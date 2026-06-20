package llm.slop.spirals.cv.visualizers

/**
 * A ring buffer to store the last N samples of a CV signal.
 * Optimized for zero-allocation access in the draw loop.
 */
class CvHistoryBuffer(val size: Int) {
    private val buffer = FloatArray(size)
    
    @Volatile
    private var index = 0

    fun add(value: Float) {
        buffer[index] = value
        index = (index + 1) % size
    }

    /**
     * Gets a sample at a specific chronological index (0 = oldest, size-1 = newest).
     */
    fun getAt(i: Int): Float {
        return buffer[(index + i) % size]
    }

    /**
     * Copies the samples in chronological order into the target array.
     */
    fun copyTo(target: FloatArray) {
        val count = size.coerceAtMost(target.size)
        val currentIndex = index // Read once
        for (i in 0 until count) {
            target[i] = buffer[(currentIndex + i) % size]
        }
    }

    /**
     * Returns the samples in chronological order.
     * Warning: This allocates a new array.
     */
    fun getSamples(): FloatArray {
        val result = FloatArray(size)
        copyTo(result)
        return result
    }
}
