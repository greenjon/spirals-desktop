package llm.slop.spirals.cv.sources

import kotlin.math.floor
import kotlin.random.Random
import llm.slop.spirals.cv.CvSignal

/**
 * A CV signal that generates a new random value on a clock trigger and glides to it.
 * This simulates a classic "Sample and Hold" synthesizer module.
 * 
 * This implementation is "stateless" - it calculates current and previous values
 * based solely on the beat count and modulator settings, ensuring that multiple 
 * parameters using the same SampleAndHold instance get independent results
 * if their subdivisions or phase offsets differ.
 */
class SampleAndHoldCv : CvSignal {
    
    /**
     * Gets a deterministic random value for a specific beat cycle.
     */
    private fun getRandomValueForCycle(cycleIndex: Int, seed: Int = 0): Float {
        // Create a deterministic random generator seeded by the cycle index and unique seed
        val rng = Random(cycleIndex + seed)
        return rng.nextFloat()
    }

    /**
     * Gets the current and previous values based on the cycle position and subdivision.
     */
    private fun getValuesForPosition(cyclePosition: Double, subdivision: Double, phaseOffset: Float): Pair<Float, Float> {
        val currentCycle = floor(cyclePosition).toInt()
        val previousCycle = currentCycle - 1
        
        // Use both subdivision and phaseOffset in the seed to ensure independence
        val seed = subdivision.hashCode() xor phaseOffset.hashCode()
        
        val currentValue = getRandomValueForCycle(currentCycle, seed)
        val previousValue = getRandomValueForCycle(previousCycle, seed)
        
        return Pair(currentValue, previousValue)
    }

    /**
     * Calculates the interpolated value for the current position.
     * 
     * @param totalBeats Total beats elapsed
     * @param subdivision Beats per cycle
     * @param phaseOffset Shift in the sampling point (0.0 to 1.0)
     * @param slope Duration of the glide phase (0.0 to 1.0)
     */
    fun getValue(totalBeats: Double, subdivision: Double, phaseOffset: Float, slope: Float): Float {
        // Calculate where we are in the sequence of cycles
        val cyclePosition = (totalBeats / subdivision) + phaseOffset
        
        // Calculate the phase within the current cycle (0.0 to 1.0)
        val phase = cyclePosition % 1.0
        val positivePhase = if (phase < 0) phase + 1.0 else phase
        
        // Get random values for the current and previous cycles
        val (currentValue, previousValue) = getValuesForPosition(cyclePosition, subdivision, phaseOffset)
        
        // Calculate glide interpolation factor
        val glideAmount = if (positivePhase < slope) {
            // During glide phase
            (positivePhase / slope).toFloat().coerceIn(0f, 1f)
        } else {
            // During hold phase
            1.0f
        }
        
        // Interpolate between previous and current values
        return previousValue + (currentValue - previousValue) * glideAmount
    }

    /**
     * Interface method from CvSignal. Not used for the complex S&H logic.
     */
    override fun getValue(timeSeconds: Double): Float {
        return 0.5f
    }
}
