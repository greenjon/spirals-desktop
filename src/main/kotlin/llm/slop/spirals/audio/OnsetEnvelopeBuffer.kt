package llm.slop.spirals.audio

/**
 * A pre-allocated circular ring buffer that accumulates one onset-strength float
 * per audio callback block. Provides a fixed-length, oldest-to-newest ordered
 * snapshot for autocorrelation without any runtime allocation.
 *
 * @param capacity Number of envelope frames to store (e.g. 128 or 256).
 */
class OnsetEnvelopeBuffer(val capacity: Int) {

    private val buf = FloatArray(capacity)
    private var writeHead = 0
    private var count = 0

    /** Returns true once the buffer has been filled at least once. */
    val isFull: Boolean get() = count >= capacity

    /**
     * Adds one onset-strength sample. Overwrites the oldest entry when full.
     * Called once per JACK audio callback — must be allocation-free.
     */
    fun add(value: Float) {
        buf[writeHead] = value
        writeHead = (writeHead + 1) % capacity
        if (count < capacity) count++
    }

    /**
     * Copies the contents into [dest] in chronological order (oldest first).
     * [dest] must have length >= [capacity].
     * Called from the JACK thread before ACF estimation — allocation-free.
     */
    fun copyInto(dest: FloatArray) {
        if (count < capacity) {
            // Buffer not yet full: copy what we have, zero-pad the rest
            val available = count
            System.arraycopy(buf, 0, dest, 0, available)
            dest.fill(0f, available, capacity)
        } else {
            // Oldest sample is at writeHead, wraps around
            val tail = capacity - writeHead
            System.arraycopy(buf, writeHead, dest, 0, tail)
            System.arraycopy(buf, 0, dest, tail, writeHead)
        }
    }

    /** Resets the buffer to empty. */
    fun reset() {
        buf.fill(0f)
        writeHead = 0
        count = 0
    }
}
