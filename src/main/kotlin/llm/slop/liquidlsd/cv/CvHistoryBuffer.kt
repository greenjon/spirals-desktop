package llm.slop.liquidlsd.cv

/**
 * A ring buffer to store the last N samples of a CV signal.
 * Optimized for zero-allocation access in the draw loop.
 * 
 * THREAD SAFETY WARNING: This class is designed for single-writer, single-reader scenarios.
 * Writing to [add] and reading via [getAt] or [copyTo] concurrently from different threads
 * is technically a data race on the `index` and `buffer` fields. For visualization usage (e.g.
 * oscilloscopes), this is acceptable as the consequence is at most a single-sample transient visual artifact.
 * For critical data processing, external synchronization is required.
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
