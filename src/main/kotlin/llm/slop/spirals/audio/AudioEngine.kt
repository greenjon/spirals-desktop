package llm.slop.spirals.audio

import llm.slop.spirals.cv.CVRegistry
import llm.slop.spirals.cv.CvHistoryBuffer
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.log10
import kotlin.math.abs
import kotlin.math.floor
import mu.KotlinLogging

enum class SignalState { SILENT, SEARCHING, LOCKED }

data class DetectionConfig(
    val silenceThresholdDb: Float = -40f,
    val silenceTimeoutMs: Long = 500_000_000L, // 500ms in nanos
    val maxBpm: Float = 200f,
    val minBpm: Float = 60f,
    val pllCorrectionFactor: Double = 0.4, // How aggressively the PLL nudges phase
    // Number of JACK callbacks between ACF tempo re-estimations (~0.75 sec at 1024/44100)
    val acfUpdateIntervalFrames: Int = 32
)

/**
 * Orchestrates the audio capture client and runs the real-time DSP analysis pipeline.
 * Separates audio into bands, computes an onset-strength envelope, feeds it into an
 * autocorrelation tempo estimator every ~0.75 seconds, and drives a sample-accurate
 * beat flywheel updated to CVRegistry every callback.
 *
 * Beat detection algorithm: Autocorrelation of onset-strength envelope (ACF).
 * - Robust to syncopation, fills, dropped beats, and half/double tempo confusion
 *   (octave ambiguity is acceptable for VJ use).
 * - Requires ~3–6 seconds of audio to fill the history window before reporting tempo.
 * - Zero heap allocations inside [processAudio] after [start] is called.
 */
object AudioEngine {
    private val logger = KotlinLogging.logger {}
    private var jackClient: JackClient? = null

    // DSP filters
    private var lastSampleRate = 44100f
    private val lowPass  = BiquadFilter(BiquadFilter.Type.LOWPASS,  lastSampleRate, 150f)
    private val midPass  = BiquadFilter(BiquadFilter.Type.BANDPASS, lastSampleRate, 1000f)
    private val highPass = BiquadFilter(BiquadFilter.Type.HIGHPASS, lastSampleRate, 5000f)

    private val extractor = AmplitudeExtractor()

    // Pre-allocated buffer for oscilloscope rendering of raw input samples
    val rawHistory = CvHistoryBuffer(1024)

    // Temporary processing buffers — resized only on JACK buffer-size change (rare)
    private var lowBuffer  = FloatArray(1024)
    private var midBuffer  = FloatArray(1024)
    private var highBuffer = FloatArray(1024)

    // ── Flywheel state ──────────────────────────────────────────────────────
    private var totalSamplesProcessed = 0L
    private var totalBeats = 0.0
    @Volatile private var estimatedBpm = 120f
    @Volatile var inputGain = 1.0f

    // ── User controls ────────────────────────────────────────────────────────
    @Volatile var isBpmLocked = false
    @Volatile var manualBpm = 120f
    @Volatile var isPhaseSyncEnabled = true
    @Volatile var phaseSyncStrength = 1.0f

    /**
     * Size of the ACF history window. 128 = ~3 s (faster lock), 256 = ~6 s (more accurate).
     * Changing this at runtime triggers a restart of the ACF subsystem on the next [start] call.
     * Safe to read/write from the UI thread.
     */
    @Volatile var acfHistorySize: Int = 256
        set(value) {
            field = value
            // Rebuild ACF objects so the change takes effect immediately
            rebuildAcfObjects(lastSampleRate, 1024)
        }

    // ── State machine ────────────────────────────────────────────────────────
    val config = DetectionConfig()
    @Volatile var currentState = SignalState.SILENT
    @Volatile var confidenceScore = 0f  // mirrors tempoEstimator.peakStrength for UI display
    private var lastSignalTime = System.nanoTime()

    // ── Onset-strength tracking ──────────────────────────────────────────────
    private var prevBass = 0f
    private var prevMid  = 0f
    private var prevHigh = 0f
    private var accentLevel  = 0f
    private var localOnsetMean = 0f // fast adaptive mean for onset threshold

    // ── ACF subsystem (rebuilt when acfHistorySize or sampleRate changes) ────
    @Volatile private var envelopeBuffer = OnsetEnvelopeBuffer(acfHistorySize)
    @Volatile private var tempoEstimator = AutocorrTempoEstimator(
        historySize  = acfHistorySize,
        envelopeFps  = lastSampleRate / 1024.0,
        minBpm       = config.minBpm,
        maxBpm       = config.maxBpm
    )
    private var acfCallbackCounter = 0

    // ── Last onset timestamp for phase sync ─────────────────────────────────
    private var lastOnsetSamples = 0L
    private var isFirstOnsetAfterSilence = true

    fun getEstimatedBpm(): Float = estimatedBpm
    fun isActive(): Boolean = jackClient?.isConnected == true

    fun setBpmDirectly(bpm: Float) {
        estimatedBpm = bpm
        CVRegistry.updateBeatAnchor(totalBeats, bpm, System.nanoTime())
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Rebuilds the ACF envelope buffer and tempo estimator with the current
     * [acfHistorySize] and the given [sampleRate] / [blockSize].
     * Must be called from a non-RT thread (allocates).
     */
    private fun rebuildAcfObjects(sampleRate: Float, blockSize: Int) {
        val fps = sampleRate / blockSize.toDouble()
        envelopeBuffer = OnsetEnvelopeBuffer(acfHistorySize)
        tempoEstimator = AutocorrTempoEstimator(
            historySize  = acfHistorySize,
            envelopeFps  = fps,
            minBpm       = config.minBpm,
            maxBpm       = config.maxBpm
        )
        acfCallbackCounter = 0
        logger.info { "ACF subsystem rebuilt: historySize=$acfHistorySize fps=%.2f".format(fps) }
    }

    /**
     * Starts the Audio Engine and JACK client connection.
     */
    fun start() {
        // Reset flywheel
        totalSamplesProcessed = 0L
        totalBeats = 0.0
        estimatedBpm = if (isBpmLocked) manualBpm else 120f
        currentState = SignalState.SILENT
        confidenceScore = 0f
        lastSignalTime = System.nanoTime()

        // Reset onset trackers
        prevBass = 0f
        prevMid  = 0f
        prevHigh = 0f
        accentLevel = 0f
        localOnsetMean = 0f
        lastOnsetSamples = 0L
        isFirstOnsetAfterSilence = true

        // Rebuild ACF objects (allocation is fine here — we are not yet in the RT callback)
        rebuildAcfObjects(lastSampleRate, 1024)

        jackClient = JackClient("spirals-desktop") { buffer, nframes, sampleRate ->
            processAudio(buffer, nframes, sampleRate)
        }
        jackClient?.start()
    }

    /**
     * Attempts to reconnect to JACK if not currently active.
     * Safe to call from a background thread.
     */
    fun tryReconnect() {
        if (isActive()) return
        logger.info { "Watchdog attempting JACK reconnection..." }
        stop()
        start()
    }

    /**
     * Processes a new block of audio samples from JACK. Runs on the real-time audio thread.
     * ZERO ALLOCATIONS — all buffers are pre-allocated in [start] or at object init.
     */
    private fun processAudio(buffer: FloatBuffer, nframes: Int, sampleRate: Float) {
        val currentTime = System.nanoTime()
        totalSamplesProcessed += nframes

        // 1. Dynamic sample rate adjustment (rare)
        if (sampleRate != lastSampleRate) {
            lowPass.sampleRate  = sampleRate; lowPass.updateCoefficients()
            midPass.sampleRate  = sampleRate; midPass.updateCoefficients()
            highPass.sampleRate = sampleRate; highPass.updateCoefficients()
            lastSampleRate = sampleRate
            // ACF rebuild deferred — do it outside the callback to avoid alloc on RT thread
            // The estimator will continue with its current envelopeFps until the next start()
        }

        // 2. Resize temp buffers only on JACK buffer-size change (rare)
        if (lowBuffer.size < nframes) {
            lowBuffer  = FloatArray(nframes)
            midBuffer  = FloatArray(nframes)
            highBuffer = FloatArray(nframes)
        }

        // 3. Filter bank + raw history
        val startPos = buffer.position()
        val gain = inputGain
        for (i in 0 until nframes) {
            val sample = buffer.get(startPos + i) * gain
            rawHistory.add(sample)
            lowBuffer[i]  = lowPass.process(sample)
            midBuffer[i]  = midPass.process(sample)
            highBuffer[i] = highPass.process(sample)
        }

        // 4. RMS amplitudes per band
        val amp  = extractor.calculateRms(buffer, nframes) * gain
        val bass = extractor.calculateRms(lowBuffer,  nframes)
        val mid  = extractor.calculateRms(midBuffer,  nframes)
        val high = extractor.calculateRms(highBuffer, nframes)

        // 5. Onset-strength function: half-wave rectified multi-band spectral flux
        //    Weights favour bass/kick (×2) over mid (×0.8) and high (×0.3)
        val bassFlux = max(0f, bass - prevBass)
        val midFlux  = max(0f, mid  - prevMid)
        val highFlux = max(0f, high - prevHigh)
        val onsetStrength = bassFlux * 2.0f + midFlux * 0.8f + highFlux * 0.3f

        prevBass = bass
        prevMid  = mid
        prevHigh = high

        // Fast adaptive local mean (τ ≈ 20 callbacks ≈ ~0.5 s) for onset thresholding
        localOnsetMean = localOnsetMean * 0.95f + onsetStrength * 0.05f
        val isOnset = onsetStrength > localOnsetMean * 1.5f && onsetStrength > 1e-5f

        // Accent envelope (peak-hold + decay) — published as CV
        if (onsetStrength > accentLevel) {
            accentLevel = onsetStrength
        } else {
            accentLevel *= 0.88f
        }

        // Normalized onset for CV output (0–2 range)
        val onsetNormalized = (onsetStrength / 0.05f).coerceIn(0f, 2f)

        // 6. Silence gate
        val currentRmsDb = 20f * log10(amp + 1e-6f)
        if (currentRmsDb < config.silenceThresholdDb) {
            if (currentTime - lastSignalTime > config.silenceTimeoutMs) {
                currentState = SignalState.SILENT
                confidenceScore = 0f
                isFirstOnsetAfterSilence = true
                envelopeBuffer.reset()
                tempoEstimator.reset()
                acfCallbackCounter = 0
            }
        } else {
            lastSignalTime = currentTime
            if (currentState == SignalState.SILENT) {
                currentState = SignalState.SEARCHING
            }
        }

        // 7. Tick the flywheel (sample-accurate)
        val deltaTimeSec = nframes.toDouble() / sampleRate.toDouble()
        if (currentState != SignalState.SILENT) {
            totalBeats += deltaTimeSec * (estimatedBpm / 60.0)
        }

        // 8. Feed onset envelope and run ACF every ACF_UPDATE_INTERVAL callbacks
        if (currentState != SignalState.SILENT) {
            envelopeBuffer.add(onsetStrength)
            acfCallbackCounter++

            if (acfCallbackCounter >= config.acfUpdateIntervalFrames && envelopeBuffer.isFull) {
                acfCallbackCounter = 0
                envelopeBuffer.copyInto(tempoEstimator.inputBuf)
                val newBpm = tempoEstimator.estimate()

                if (newBpm > 0f && !isBpmLocked) {
                    estimatedBpm = newBpm.coerceIn(config.minBpm, config.maxBpm)
                }

                // Map ACF peak strength to confidence and state
                confidenceScore = tempoEstimator.peakStrength
                currentState = when {
                    tempoEstimator.peakStrength >= 0.4f -> SignalState.LOCKED
                    tempoEstimator.peakStrength >= 0.1f -> SignalState.SEARCHING
                    else -> currentState // preserve existing state
                }
            }
        }

        // 9. Phase sync: nudge the flywheel whenever an onset lands
        if (currentState != SignalState.SILENT && isOnset && isPhaseSyncEnabled && !isBpmLocked) {
            if (isFirstOnsetAfterSilence) {
                isFirstOnsetAfterSilence = false
                lastOnsetSamples = totalSamplesProcessed
            } else {
                val currentPhase = totalBeats - floor(totalBeats)
                val phaseError = if (currentPhase > 0.5) currentPhase - 1.0 else currentPhase
                // Only nudge if the onset is close to an expected beat boundary (within 30%)
                if (abs(phaseError) < 0.30) {
                    totalBeats -= phaseError * config.pllCorrectionFactor * phaseSyncStrength
                }
                lastOnsetSamples = totalSamplesProcessed
            }
        }

        // 10. Manual BPM lock override
        if (isBpmLocked) {
            estimatedBpm = manualBpm
        }

        // 11. Publish to CV Registry
        CVRegistry.updateBeatAnchor(totalBeats, estimatedBpm, currentTime)
        CVRegistry.updatePushedValue("amp",    (amp  / 0.1f).coerceIn(0f, 2f))
        CVRegistry.updatePushedValue("bass",   (bass / 0.1f).coerceIn(0f, 2f))
        CVRegistry.updatePushedValue("mid",    (mid  / 0.1f).coerceIn(0f, 2f))
        CVRegistry.updatePushedValue("high",   (high / 0.1f).coerceIn(0f, 2f))
        CVRegistry.updatePushedValue("onset",  onsetNormalized)
        CVRegistry.updatePushedValue("accent", accentLevel)
    }

    /**
     * Stops the Audio Engine and releases resources.
     */
    fun stop() {
        jackClient?.stop()
        jackClient = null
    }
}
