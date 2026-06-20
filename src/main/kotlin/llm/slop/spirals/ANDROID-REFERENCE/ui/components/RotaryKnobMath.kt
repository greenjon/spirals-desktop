package llm.slop.spirals.ui.components

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow

data class KnobConfig(
    val rMin: Float = 0.001f,  // Fine resolution
    val rMax: Float = 0.01f,   // Coarse resolution
    val isBipolar: Boolean = false
)

object RotaryKnobMath {
    /**
     * Computes the new value for a rotary knob based on vertical drag delta and velocity.
     * Returns a Pair of (new value, updated smoothed velocity).
     */
    fun computeNewValue(
        deltaY: Float,
        deltaTimeSec: Float, 
        currentValue: Float,
        prevVelocity: Float,
        config: KnobConfig
    ): Pair<Float, Float> {
        // 1. Velocity Calculation (Cap deltaTime to prevent spikes)
        val cappedDt = deltaTimeSec.coerceIn(0.008f, 0.033f)
        val rawVelocity = abs(deltaY) / cappedDt
        val smoothedVelocity = (prevVelocity * 0.7f) + (rawVelocity * 0.3f)
        
        // 2. Resolution Calculation
        // vn: normalized velocity [0..1] based on expected drag speed range [50..2500 px/s]
        val vHigh = 2500f
        val vn = ((smoothedVelocity - 50f) / (vHigh - 50f)).coerceIn(0f, 1f)
        val vs = vn.pow(3.0f) // Gamma 3.0 for natural "acceleration"
        var resolution = config.rMin + vs * (config.rMax - config.rMin)
        
        // Range Scaling (Bipolar is twice as wide as Unipolar)
        if (config.isBipolar) resolution *= 2.0f 
        
        // 3. Primary Movement
        var value = currentValue + (-deltaY * resolution)
        
        // 4. Bipolar Soft Detent (Magnet effect at center 0.5)
        if (config.isBipolar) {
            val detentWidth = 0.05f
            val detentStrength = 0.4f * (1.0f - vs) // Weaken detent as user moves faster
            val centeredValue = value - 0.5f // Adjust to put detent at 0.5
            val falloff = exp(-(centeredValue * centeredValue) / (detentWidth * detentWidth))
            value += -centeredValue * falloff * detentStrength // Pull toward center (0.5)
        }
        
        // For bipolar knobs, keep the value in 0-1 range, but with special detent at 0.5f (center)
        val finalValue = value.coerceIn(0f, 1f)
        
        return Pair(finalValue, smoothedVelocity)
    }
}
