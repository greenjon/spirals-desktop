package llm.slop.spirals.audio

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Autocorrelation-based tempo estimator operating on a decimated onset-strength
 * envelope. All working memory is pre-allocated at construction time; [estimate]
 * performs zero heap allocations and is safe to call from the JACK real-time thread.
 *
 * Algorithm:
 *   1. Mean-subtract the input window (DC removal).
 *   2. Compute the normalized ACF for lags corresponding to 60–200 BPM.
 *   3. Find the lag with the maximum ACF value.
 *   4. Apply parabolic interpolation around the peak for sub-integer precision.
 *   5. Return the smoothed BPM estimate.
 *
 * @param historySize  Number of onset-strength frames in the input window (128 or 256).
 * @param envelopeFps  Frame rate of the onset envelope (callbacks/sec). Typically
 *                     sampleRate / blockSize (e.g. 44100/1024 ≈ 43.07 Hz).
 * @param minBpm       Lower BPM search limit.
 * @param maxBpm       Upper BPM search limit.
 * @param smoothing    IIR smoothing factor applied to the output BPM (0 = no smoothing,
 *                     1 = never updates). 0.15 gives a comfortable inter-update glide.
 */
class AutocorrTempoEstimator(
    historySize: Int,
    private val envelopeFps: Double,
    private val minBpm: Float = 60f,
    private val maxBpm: Float = 200f,
    private val smoothing: Float = 0.85f
) {
    /** Pre-allocated input snapshot — caller fills this before calling [estimate]. */
    val inputBuf = FloatArray(historySize)

    /** Normalized peak height of the winning ACF lag, in [0, 1]. Updated by [estimate]. */
    @Volatile var peakStrength = 0f

    /** Last smoothed BPM output. 0 before first valid estimate. */
    @Volatile var smoothedBpm = 0f

    // Pre-computed lag bounds
    private val lagMin = (envelopeFps * 60.0 / maxBpm).roundToInt().coerceAtLeast(1)
    private val lagMax = (envelopeFps * 60.0 / minBpm).roundToInt().coerceAtMost(historySize - 1)
    private val acfLen = (lagMax - lagMin + 1).coerceAtLeast(1)
    private val n = historySize

    // Pre-allocated working arrays — zero runtime allocation
    private val workBuf = FloatArray(n)
    private val acfBuf  = FloatArray(acfLen)

    /**
     * Runs the ACF tempo estimation over [inputBuf].
     * Must be called after filling [inputBuf] via [OnsetEnvelopeBuffer.copyInto].
     *
     * @return Smoothed BPM estimate, or 0f if not enough signal to estimate.
     */
    fun estimate(): Float {
        // 1. Mean-subtract to remove DC bias
        var mean = 0f
        for (i in 0 until n) mean += inputBuf[i]
        mean /= n
        for (i in 0 until n) workBuf[i] = inputBuf[i] - mean

        // 2. Compute ACF for each lag in [lagMin, lagMax]
        var maxAcf = 0f
        var bestIdx = 0
        for (k in 0 until acfLen) {
            val lag = lagMin + k
            var sum = 0f
            val limit = n - lag
            for (i in 0 until limit) {
                sum += workBuf[i] * workBuf[i + lag]
            }
            // Normalize by the number of products computed
            val normalized = sum / limit
            acfBuf[k] = normalized
            if (normalized > maxAcf) {
                maxAcf = normalized
                bestIdx = k
            }
        }

        // 3. Compute baseline (mean of acf values) for peakStrength normalization
        var acfMean = 0f
        for (k in 0 until acfLen) acfMean += acfBuf[k]
        acfMean /= acfLen

        // Peak strength: how much does the dominant peak stand above the mean?
        val acfRange = maxAcf - acfMean
        peakStrength = if (acfRange > 0f && maxAcf > 0f) {
            (acfRange / maxAcf).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Not enough signal — return previous estimate
        if (peakStrength < 0.05f) return smoothedBpm

        // 4. Parabolic interpolation for sub-integer lag precision
        val fracLag: Double = if (bestIdx in 1 until acfLen - 1) {
            val y0 = acfBuf[bestIdx - 1].toDouble()
            val y1 = acfBuf[bestIdx].toDouble()
            val y2 = acfBuf[bestIdx + 1].toDouble()
            val denom = 2.0 * (2.0 * y1 - y0 - y2)
            val intLag = (lagMin + bestIdx).toDouble()
            if (abs(denom) > 1e-9) intLag + (y0 - y2) / denom else intLag
        } else {
            (lagMin + bestIdx).toDouble()
        }

        // 5. Convert lag to BPM
        val rawBpm = (envelopeFps * 60.0 / fracLag).toFloat().coerceIn(minBpm, maxBpm)

        // 6. Gated acceptance: only update if the raw BPM is consistent with the
        //    current estimate (within ±20%) or at a clean octave relationship (2× or 0.5×).
        //    This prevents harmonic-flipping (e.g. 140 → 70 → 280) from corrupting the output
        //    even when ACF peakStrength is high on the wrong harmonic.
        val acceptedBpm: Float? = if (smoothedBpm == 0f) {
            rawBpm // Cold start — always accept first estimate
        } else {
            val ratio = rawBpm / smoothedBpm
            val inBand        = ratio in 0.80f..1.20f           // within ±20%
            val isDoubleTime  = (ratio * 0.5f) in 0.80f..1.20f // new ≈ 2× current
            val isHalfTime    = (ratio * 2.0f) in 0.80f..1.20f // new ≈ 0.5× current
            if (inBand || isDoubleTime || isHalfTime) rawBpm else null
        }

        // 7. IIR smoothing — only applied when the candidate passed the gate
        if (acceptedBpm != null) {
            smoothedBpm = if (smoothedBpm == 0f) {
                acceptedBpm
            } else {
                smoothedBpm * smoothing + acceptedBpm * (1f - smoothing)
            }
        }

        return smoothedBpm
    }

    /** Resets internal state (smoothed BPM, peak strength). */
    fun reset() {
        peakStrength = 0f
        smoothedBpm = 0f
        workBuf.fill(0f)
        acfBuf.fill(0f)
    }
}
