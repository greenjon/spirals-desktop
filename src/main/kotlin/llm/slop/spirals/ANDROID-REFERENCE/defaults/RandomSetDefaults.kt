package llm.slop.spirals.defaults

import llm.slop.spirals.cv.core.Waveform
import llm.slop.spirals.models.STANDARD_BEAT_VALUES
import llm.slop.spirals.models.SpeedSource

/**
 * Data structures for storing default values for the Mandala and Random Set Editors.
 * These values are used when a parameter is "unconfigured" (no specific constraints provided).
 */

/**
 * Top-level container for all Mandala default values
 */
data class MandalaDefaults(
    val armDefaults: ArmDefaults = ArmDefaults(),
    val rotationDefaults: RotationDefaults = RotationDefaults(),
    val hueOffsetDefaults: HueOffsetDefaults = HueOffsetDefaults(),
    val recipeDefaults: RecipeDefaults = RecipeDefaults(),
    val feedbackDefaults: FeedbackDefaults = FeedbackDefaults()
)

/**
 * Default values for recipe selection
 */
data class RecipeDefaults(
    val preferFavorites: Boolean = true,  // Prefer favorites when randomizing
    val minPetalCount: Int = 3,           // Minimum petals when randomizing
    val maxPetalCount: Int = 12,          // Maximum petals when randomizing
    val autoHueSweep: Boolean = true      // Auto-set hue sweep based on petals
)

/**
 * Default values for feedback parameters
 */
data class FeedbackDefaults(
    val fbDecayMin: Float = 0.0f,
    val fbDecayMax: Float = 0.30f,
    val fbGainMin: Float = 0.85f,
    val fbGainMax: Float = 1.0f,
    val fbZoomMin: Float = 0.5f,
    val fbZoomMax: Float = 0.54f,
    val fbRotateMin: Float = 0.5f,
    val fbRotateMax: Float = 0.52f,
    val fbShiftXMin: Float = 0.0f,
    val fbShiftXMax: Float = 0.0f,
    val fbShiftYMin: Float = 0.0f,
    val fbShiftYMax: Float = 0.0f,
    val fbBlurMin: Float = 0.0f,
    val fbBlurMax: Float = 0.0f
)

/**
 * For backward compatibility - same structure as MandalaDefaults
 * Just pointing to the same defaults
 */
data class RandomSetDefaults(
    val armDefaults: ArmDefaults = ArmDefaults(),
    val rotationDefaults: RotationDefaults = RotationDefaults(),
    val hueOffsetDefaults: HueOffsetDefaults = HueOffsetDefaults()
)

/**
 * Default values for arm parameters (L1-L4)
 */
data class ArmDefaults(
    // Base length range (0-100 scale)
    val baseLengthMin: Int = 0,
    val baseLengthMax: Int = 100,
    
    // Movement source probabilities
    val beatProbability: Float = 0.4f,
    val lfoProbability: Float = 0.4f,
    val randomProbability: Float = 0.2f,
    
    // Default enablement for RSet templates
    val defaultEnableRandom: Boolean = false,
    
    // Beat division range 
    val beatDivMin: Float = STANDARD_BEAT_VALUES.first(),  // 1/16
    val beatDivMax: Float = 32f,
    
    // Waveform selection probabilities
    val sineProbability: Float = 0.33f,
    val triangleProbability: Float = 0.34f,
    val squareProbability: Float = 0.33f,
    
    // Weight/intensity range (-100 to 100)
    val weightMin: Int = -100,
    val weightMax: Int = 100,
    
    // LFO time range
    val lfoTimeMin: Float = 1.0f,
    val lfoTimeMax: Float = 60.0f,
    
    // Random glide range (0.0-1.0)
    val randomGlideMin: Float = 0.1f,
    val randomGlideMax: Float = 0.5f,

    // Phase range (0-360 degrees)
    val phaseMin: Float = 0f,
    val phaseMax: Float = 360f
) {
    /**
     * Factory method to create default values with normalized probabilities
     */
    companion object {
        fun createWithNormalizedProbabilities(
            baseLengthMin: Int = 0,
            baseLengthMax: Int = 100,
            beatProbability: Float = 0.4f,
            lfoProbability: Float = 0.4f,
            randomProbability: Float = 0.2f,
            defaultEnableRandom: Boolean = false,
            sineProbability: Float = 0.33f,
            triangleProbability: Float = 0.34f,
            squareProbability: Float = 0.33f,
            beatDivMin: Float = STANDARD_BEAT_VALUES.first(),
            beatDivMax: Float = 32f,
            weightMin: Int = -100,
            weightMax: Int = 100,
            lfoTimeMin: Float = 1.0f,
            lfoTimeMax: Float = 60.0f,
            randomGlideMin: Float = 0.1f,
            randomGlideMax: Float = 0.5f,
            phaseMin: Float = 0f,
            phaseMax: Float = 360f
        ): ArmDefaults {
            // Normalize movement source probabilities
            val totalMovementProb = beatProbability + lfoProbability + randomProbability
            val normalizedBeatProb = if (totalMovementProb > 0) beatProbability / totalMovementProb else 0.4f
            val normalizedLfoProb = if (totalMovementProb > 0) lfoProbability / totalMovementProb else 0.4f
            val normalizedRandomProb = if (totalMovementProb > 0) randomProbability / totalMovementProb else 0.2f
            
            // Normalize waveform probabilities
            val totalWaveformProb = sineProbability + triangleProbability + squareProbability
            val normalizedSineProb = sineProbability / totalWaveformProb
            val normalizedTriangleProb = triangleProbability / totalWaveformProb
            val normalizedSquareProb = squareProbability / totalWaveformProb
            
            return ArmDefaults(
                baseLengthMin = baseLengthMin,
                baseLengthMax = baseLengthMax,
                beatProbability = normalizedBeatProb,
                lfoProbability = normalizedLfoProb,
                randomProbability = normalizedRandomProb,
                defaultEnableRandom = defaultEnableRandom,
                beatDivMin = beatDivMin,
                beatDivMax = beatDivMax,
                sineProbability = normalizedSineProb,
                triangleProbability = normalizedTriangleProb,
                squareProbability = normalizedSquareProb,
                weightMin = weightMin,
                weightMax = weightMax,
                lfoTimeMin = lfoTimeMin,
                lfoTimeMax = lfoTimeMax,
                randomGlideMin = randomGlideMin,
                randomGlideMax = randomGlideMax,
                phaseMin = phaseMin,
                phaseMax = phaseMax
            )
        }
    }
    
    /**
     * Randomly select a waveform based on the configured probabilities
     */
    fun getRandomWaveform(random: kotlin.random.Random): Waveform {
        // Calculate total probability to normalize
        val totalProb = sineProbability + triangleProbability + squareProbability
        
        // If all are zero, use default fallback
        if (totalProb <= 0) {
            return Waveform.SINE
        }
        
        // Normalize probabilities
        val normalizedSine = sineProbability / totalProb
        val normalizedTriangle = triangleProbability / totalProb
        
        // Generate random value and select based on normalized probabilities
        val roll = random.nextFloat()
        
        return when {
            roll < normalizedSine -> Waveform.SINE
            roll < normalizedSine + normalizedTriangle -> Waveform.TRIANGLE
            else -> Waveform.SQUARE
        }
    }
    
    /**
     * Randomly select a movement source based on probabilities
     */
    fun getRandomMovementSource(random: kotlin.random.Random): String {
        val roll = random.nextFloat()
        return when {
            roll < beatProbability -> "beatPhase"
            roll < beatProbability + lfoProbability -> "lfo1"
            else -> "sampleAndHold"
        }
    }
}

/**
 * Default values for rotation parameters
 */
data class RotationDefaults(
    // Direction probabilities
    val clockwiseProbability: Float = 0.5f,
    val counterClockwiseProbability: Float = 0.5f,
    
    // Speed source probabilities
    val beatProbability: Float = 0.6f,
    val lfoProbability: Float = 0.2f,
    val randomProbability: Float = 0.2f,
    
    // Beat division range (for Beat CV source)
    val beatDivMin: Float = 4f,
    val beatDivMax: Float = 128f,
    
    // Beat division range for Random CV source
    val randomBeatDivMin: Float = 4f,
    val randomBeatDivMax: Float = 64f,
    
    // LFO time range
    val lfoTimeMin: Float = 5.0f,
    val lfoTimeMax: Float = 30.0f,
    
    // Random glide range (0.0-1.0)
    val randomGlideMin: Float = 0.1f,
    val randomGlideMax: Float = 0.5f
) {
    /**
     * Randomly select a direction (slope) based on probabilities
     */
    fun getRandomDirection(random: kotlin.random.Random): Float {
        val normalizedCw = clockwiseProbability / (clockwiseProbability + counterClockwiseProbability)
        return if (random.nextFloat() < normalizedCw) 0f else 1f
    }
    
    /**
     * Randomly select a speed source based on probabilities
     */
    fun getRandomSpeedSource(random: kotlin.random.Random): SpeedSource {
        val totalProb = beatProbability + lfoProbability + randomProbability
        val normalizedBeat = beatProbability / totalProb
        val normalizedLfo = lfoProbability / totalProb
        
        val roll = random.nextFloat()
        return when {
            roll < normalizedBeat -> SpeedSource.BEAT
            roll < normalizedBeat + normalizedLfo -> SpeedSource.LFO
            else -> SpeedSource.RANDOM
        }
    }
}

/**
 * Default values for hue offset (color cycling) parameters
 */
data class HueOffsetDefaults(
    // Direction probabilities
    val forwardProbability: Float = 0.5f,
    val reverseProbability: Float = 0.5f,
    
    // Speed source probabilities
    val beatProbability: Float = 0.6f,
    val lfoProbability: Float = 0.2f,
    val randomProbability: Float = 0.2f,
    
    // Beat division range
    val beatDivMin: Float = 4f,
    val beatDivMax: Float = 16f,
    
    // LFO time range
    val lfoTimeMin: Float = 10.0f,
    val lfoTimeMax: Float = 60.0f,
    
    // Random glide range (0.0-1.0)
    val randomGlideMin: Float = 0.1f,
    val randomGlideMax: Float = 0.5f
) {
    /**
     * Randomly select a direction (slope) based on probabilities
     */
    fun getRandomDirection(random: kotlin.random.Random): Float {
        val normalizedForward = forwardProbability / (forwardProbability + reverseProbability)
        return if (random.nextFloat() < normalizedForward) 1f else 0f
    }
    
    /**
     * Randomly select a speed source based on probabilities
     */
    fun getRandomSpeedSource(random: kotlin.random.Random): SpeedSource {
        val totalProb = beatProbability + lfoProbability + randomProbability
        val normalizedBeat = beatProbability / totalProb
        val normalizedLfo = lfoProbability / totalProb
        
        val roll = random.nextFloat()
        return when {
            roll < normalizedBeat -> SpeedSource.BEAT
            roll < normalizedBeat + normalizedLfo -> SpeedSource.LFO
            else -> SpeedSource.RANDOM
        }
    }
}
