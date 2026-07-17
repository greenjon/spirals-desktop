package llm.slop.liquidlsd.cv

/**
 * Interface representing a Control Voltage (CV) modulation source.
 */
interface CVSource {
    val id: String

    /**
     * Updates the internal state of the CV source. Called once per frame.
     * @param totalBeats Synchronized total beats from the beat clock.
     * @param elapsedSeconds Time elapsed in seconds since application start.
     */
    fun update(totalBeats: Double, elapsedSeconds: Double)

    /**
     * Returns the current evaluated value of this CV source.
     */
    val value: Float
}

/**
 * A basic, mutable CV source wrapper.
 * Useful for signals that are pushed from other threads/engines (like audio analysis).
 */
class MutableCVSource(
    override val id: String,
    @Volatile override var value: Float = 0f
) : CVSource {
    override fun update(totalBeats: Double, elapsedSeconds: Double) {
        // Pushed externally, no self-tick required
    }
}
