package llm.slop.spirals.cv.processors

import kotlin.math.sqrt

/**
 * Extracts raw RMS amplitude from PCM data.
 */
class AmplitudeExtractor {
    /**
     * Calculates RMS from float PCM data (expected range -1.0 to 1.0).
     */
    fun calculateRms(pcm: FloatArray): Float {
        if (pcm.isEmpty()) return 0f
        var sum = 0f
        for (sample in pcm) {
            sum += sample * sample
        }
        return sqrt(sum / pcm.size)
    }

    /**
     * Calculates RMS from short PCM data.
     */
    fun calculateRms(pcm: ShortArray): Float {
        if (pcm.isEmpty()) return 0f
        var sum = 0.0
        for (sample in pcm) {
            val normalized = sample / 32768.0
            sum += normalized * normalized
        }
        return sqrt(sum / pcm.size).toFloat()
    }
}
