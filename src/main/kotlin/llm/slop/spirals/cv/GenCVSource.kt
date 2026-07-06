package llm.slop.spirals.cv

/**
 * CV source representing a unified generator source (Gen).
 * 
 * NOTE: The actual modulation value for generators (gen1/gen2) is evaluated dynamically
 * per-parameter in [llm.slop.spirals.cv.evaluateModulator].
 * 
 * This registered source class exists primarily as a placeholder in the registry
 * so that a history buffer slot is allocated for visual history/oscilloscope reference.
 * The value is set to 0f here as the history buffer is updated/rendered separately.
 */
class GenCVSource(override val id: String) : CVSource {
    private var _value = 0f
    override val value: Float get() = _value

    override fun update(totalBeats: Double, elapsedSeconds: Double) {
        _value = 0f
    }
}
