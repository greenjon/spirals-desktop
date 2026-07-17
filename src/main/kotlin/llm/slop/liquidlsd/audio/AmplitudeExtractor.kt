package llm.slop.liquidlsd.audio

import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * Utility class to calculate RMS (Root Mean Square) amplitude.
 * Optimized to read directly from NIO FloatBuffers without array allocation.
 */
class AmplitudeExtractor {
    /**
     * Calculates the RMS amplitude of a segment of a FloatBuffer.
     */
    fun calculateRms(buffer: FloatBuffer, length: Int): Float {
        if (length <= 0) return 0f
        var sum = 0f
        val startPos = buffer.position()
        for (i in 0 until length) {
            val sample = buffer.get(startPos + i)
            sum += sample * sample
        }
        return sqrt(sum / length)
    }

    /**
     * Calculates the RMS amplitude of a FloatArray.
     */
    fun calculateRms(array: FloatArray, length: Int): Float {
        if (length <= 0 || array.isEmpty()) return 0f
        var sum = 0f
        val count = length.coerceAtMost(array.size)
        for (i in 0 until count) {
            val sample = array[i]
            sum += sample * sample
        }
        return sqrt(sum / count)
    }
}
