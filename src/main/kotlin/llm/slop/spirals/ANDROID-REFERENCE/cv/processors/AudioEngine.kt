package llm.slop.spirals.cv.processors

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.util.Log
import llm.slop.spirals.cv.core.ModulationRegistry
import kotlinx.coroutines.*
import kotlin.math.max

/**
 * The core analysis engine. Splits audio into bands and updates the CvRegistry.
 * Implements Spectral Flux (onset detection) and Automatic BPM Detection.
 */
class AudioEngine(context: Context) {
    private val appContext = context.applicationContext
    val sourceManager = AudioSourceManager(appContext)
    
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
    
    private var audioRecord: AudioRecord? = null
    private var job: Job? = null
    
    private val lowPass = BiquadFilter(BiquadFilter.Type.LOWPASS, 44100f, 150f)
    private val midPass = BiquadFilter(BiquadFilter.Type.BANDPASS, 44100f, 1000f)
    private val highPass = BiquadFilter(BiquadFilter.Type.HIGHPASS, 44100f, 5000f)
    
    private val extractor = AmplitudeExtractor()
    
    // Master Clock State
    private var lastFrameTime = System.nanoTime()
    private var totalBeats = 0.0 // Use Double for precision
    private var estimatedBpm = 120f

    // BPM Estimation State
    private var lastBeatTime = 0L
    private val beatIntervals = mutableListOf<Long>()
    private val maxIntervals = 8
    private val minIntervalNs = 300_000_000L // 200 BPM max
    private val maxIntervalNs = 1_500_000_000L // 40 BPM min

    @Volatile
    var debugLastRms: Float = 0f
        private set

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, record: AudioRecord?) {
        if (record == null) return
        stop()
        audioRecord = record
        
        lastBeatTime = System.nanoTime() // Initialize beat timer

        try {
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) return
            audioRecord?.startRecording()

            job = scope.launch(Dispatchers.Default) {
                val minBufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val readBufferSize = (minBufferSizeInBytes / 4).coerceAtLeast(512)
                val audioData = FloatArray(readBufferSize)
                
                val lowData = FloatArray(readBufferSize)
                val midData = FloatArray(readBufferSize)
                val highData = FloatArray(readBufferSize)

                var prevBass = 0f
                var prevMid = 0f
                var prevHigh = 0f
                var accentLevel = 0f
                var beatThreshold = 0.5f 
                var lastOnsetNormalized = 0f

                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(audioData, 0, audioData.size, AudioRecord.READ_BLOCKING) ?: 0
                    if (read > 0) {
                        for (i in 0 until read) {
                            lowData[i] = lowPass.process(audioData[i])
                            midData[i] = midPass.process(audioData[i])
                            highData[i] = highPass.process(audioData[i])
                        }

                        val amp = extractor.calculateRms(audioData.copyOfRange(0, read))
                        val bass = extractor.calculateRms(lowData.copyOfRange(0, read))
                        val mid = extractor.calculateRms(midData.copyOfRange(0, read))
                        val high = extractor.calculateRms(highData.copyOfRange(0, read))

                        val bassFlux = max(0f, bass - prevBass)
                        val midFlux = max(0f, mid - prevMid)
                        val highFlux = max(0f, high - prevHigh)
                        val onsetRaw = (bassFlux * 1.0f) + (midFlux * 0.6f) + (highFlux * 0.3f)

                        prevBass = bass
                        prevMid = mid
                        prevHigh = high

                        val onsetNormalized = (onsetRaw / 0.05f).coerceIn(0f, 2f)
                        if (onsetNormalized > accentLevel) {
                            accentLevel = onsetNormalized
                        } else {
                            accentLevel *= 0.88f
                        }

                        // Automatic BPM & Phase Sync
                        val currentTime = System.nanoTime()
                        if (onsetNormalized > beatThreshold && lastOnsetNormalized <= beatThreshold) {
                            val interval = currentTime - lastBeatTime
                            if (interval in minIntervalNs..maxIntervalNs) {
                                beatIntervals.add(interval)
                                if (beatIntervals.size > maxIntervals) beatIntervals.removeAt(0)
                                val medianInterval = beatIntervals.sorted()[beatIntervals.size / 2]
                                estimatedBpm = 60_000_000_000f / medianInterval
                                
                                // SMART SYNC: Only force reset if it's a "Strong" transient 
                                // AND we are significantly out of phase (> 10%)
                                if (onsetNormalized > 1.4f) {
                                    val currentPhase = totalBeats % 1.0
                                    val isNearBeat = currentPhase < 0.1 || currentPhase > 0.9
                                    if (!isNearBeat) {
                                        // Align totalBeats to the nearest integer
                                        totalBeats = Math.round(totalBeats).toDouble()
                                    }
                                }
                            }
                            lastBeatTime = currentTime
                        }
                        
                        // MODIFIED: Replaced the problematic beatThreshold calculation
                        beatThreshold = (beatThreshold * 0.95f) + (onsetNormalized * 0.05f) 
                        beatThreshold = beatThreshold.coerceAtLeast(0.1f) // Ensure threshold doesn't drop too low

                        lastOnsetNormalized = onsetNormalized

                        // Flywheel
                        val deltaTimeSec = (currentTime - lastFrameTime) / 1_000_000_000.0
                        lastFrameTime = currentTime
                        val beatDelta = deltaTimeSec * (estimatedBpm / 60.0)
                        totalBeats += beatDelta

                        // Update Registry with the Anchor point
                        // This provides the source of truth for high-precision interpolation in the renderer
                        ModulationRegistry.updateBeatAnchor(totalBeats, estimatedBpm, currentTime)
                        ModulationRegistry.update("beatPhase", (totalBeats % 1.0).toFloat())
                        ModulationRegistry.update("beatThreshold", beatThreshold) 

                        val ref = 0.1f
                        ModulationRegistry.update("amp", (amp / ref).coerceIn(0f, 2f))
                        ModulationRegistry.update("bass", (bass / ref).coerceIn(0f, 2f))
                        ModulationRegistry.update("mid", (mid / ref).coerceIn(0f, 2f))
                        ModulationRegistry.update("high", (high / ref).coerceIn(0f, 2f))
                        ModulationRegistry.update("bassFlux", (bassFlux / 0.05f).coerceIn(0f, 2f))
                        ModulationRegistry.update("onset", onsetNormalized)
                        ModulationRegistry.update("accent", accentLevel)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error", e)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null
    }
}
