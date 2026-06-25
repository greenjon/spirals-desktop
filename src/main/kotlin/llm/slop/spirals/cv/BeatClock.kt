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

/**
 * CV source representing a sine wave derived from the beat.
 * Oscillates between 0.0 and 2.0 to match other CV signals.
 */
class BeatSine(override val id: String = "beatSine") : CVSource {
    private var _value = 0f
    override val value: Float get() = _value

    override fun update(totalBeats: Double, elapsedSeconds: Double) {
        _value = (1.0 + kotlin.math.sin(totalBeats * 2.0 * kotlin.math.PI)).toFloat()
    }
}
