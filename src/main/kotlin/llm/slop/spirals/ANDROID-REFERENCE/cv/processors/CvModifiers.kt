package llm.slop.spirals.cv.processors

import llm.slop.spirals.cv.CvSignal
import kotlin.math.pow

/**
 * A composite CV signal that chains Power, Gain, Offset, and Clip modifiers.
 * Formula: Clip(Gain(Power(source, exponent)) + offset, min, max)
 */
class ModifiedCv(
    private val source: CvSignal,
    private val exponent: Float = 1.0f,
    private val gain: Float = 1.0f,
    private val offset: Float = 0.0f,
    private val min: Float = 0.0f,
    private val max: Float = 1.0f
) : CvSignal {

    private val powerCv = PowerCv(source, exponent)
    private val gainCv = GainCv(powerCv, gain)
    private val offsetCv = OffsetCv(gainCv, offset)
    private val clipCv = ClipCv(offsetCv, min, max)

    override fun getValue(timeSeconds: Double): Float {
        return clipCv.getValue(timeSeconds)
    }
}

/**
 * Applies a power curve (exponent) to the source CV signal.
 * Formula: source.getValue(time).pow(exponent)
 */
class PowerCv(
    private val source: CvSignal,
    private val exponent: Float = 1.0f
) : CvSignal {
    override fun getValue(timeSeconds: Double): Float {
        val value = source.getValue(timeSeconds)
        // Ensure we don't pow a negative number unless exponent is 1
        return if (value < 0f && exponent != 1.0f) 0f else value.pow(exponent)
    }
}

/**
 * Clips the source CV signal between min and max values.
 */
class ClipCv(
    private val source: CvSignal,
    private val min: Float,
    private val max: Float
) : CvSignal {
    override fun getValue(timeSeconds: Double): Float {
        return source.getValue(timeSeconds).coerceIn(min, max)
    }
}

/**
 * Adds an offset to the source CV signal.
 */
class OffsetCv(private val source: CvSignal, private val offset: Float) : CvSignal {
    override fun getValue(timeSeconds: Double): Float = source.getValue(timeSeconds) + offset
}

/**
 * Multiplies the source CV signal by a gain factor.
 */
class GainCv(private val source: CvSignal, private val gain: Float) : CvSignal {
    override fun getValue(timeSeconds: Double): Float = source.getValue(timeSeconds) * gain
}
