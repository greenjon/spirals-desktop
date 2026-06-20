package llm.slop.spirals.cv.processors

import kotlin.math.exp

/**
 * Applies attack and release smoothing to a signal.
 */
class EnvelopeFollower(
    initialUpdateRate: Float = 60f,
    var attackMs: Float = 10f,
    var releaseMs: Float = 100f
) {
    private var attackCoeff: Float = 0f
    private var releaseCoeff: Float = 0f
    private var currentValue: Float = 0f
    private var currentUpdateRate: Float = initialUpdateRate

    init {
        updateCoefficients()
    }

    fun setUpdateRate(rate: Float) {
        currentUpdateRate = rate
        updateCoefficients()
    }

    fun updateCoefficients() {
        attackCoeff = calculateCoeff(attackMs, currentUpdateRate)
        releaseCoeff = calculateCoeff(releaseMs, currentUpdateRate)
    }

    private fun calculateCoeff(ms: Float, rate: Float): Float {
        if (ms <= 0f) return 1f
        // rate is samples per second (Hz)
        return (1.0 - exp(-1.0 / (rate * ms / 1000.0))).toFloat()
    }

    /**
     * Updates the follower with a new input value.
     */
    fun update(input: Float): Float {
        val coeff = if (input > currentValue) attackCoeff else releaseCoeff
        currentValue = (1f - coeff) * currentValue + coeff * input
        return currentValue
    }

    val value: Float
        get() = currentValue
}
