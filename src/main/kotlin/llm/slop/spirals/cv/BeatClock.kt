package llm.slop.spirals.cv

/**
 * CV source representing a basic beat-synced clock.
 * Exposes a normalized phase (0.0 to 1.0) of the current beat cycle.
 */
class BeatClock(override val id: String = "beatPhase") : CVSource {
    private var _value = 0f
    override val value: Float get() = _value

    override fun update(totalBeats: Double, elapsedSeconds: Double) {
        val phase = totalBeats % 1.0
        _value = (if (phase < 0.0) phase + 1.0 else phase).toFloat()
    }
}
