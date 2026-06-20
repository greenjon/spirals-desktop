package llm.slop.spirals.defaults

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import llm.slop.spirals.models.STANDARD_BEAT_VALUES
import llm.slop.spirals.models.SpeedSource

/**
 * Manages default settings for randomization and editors.
 * 
 * This class provides access to user-configurable defaults that are used 
 * when specific constraints aren't provided. Settings are persisted using
 * SharedPreferences and provide factory defaults as fallbacks.
 */
class DefaultsConfig(context: Context) {

    companion object {
        private const val PREFS_NAME = "spirals_defaults_config"
        
        // Prefix constants for organizing preferences
        private const val PREFIX_MANDALA = "defaults_mandala_"
        private const val PREFIX_ARM = "${PREFIX_MANDALA}arm_"
        private const val PREFIX_ROTATION = "${PREFIX_MANDALA}rotation_"
        private const val PREFIX_HUE = "${PREFIX_MANDALA}hue_"
        private const val PREFIX_RECIPE = "${PREFIX_MANDALA}recipe_"
        private const val PREFIX_FEEDBACK = "${PREFIX_MANDALA}feedback_"
        private const val PREFIX_HDMI = "defaults_hdmi_"
        
        // Legacy prefix for backward compatibility
        private const val PREFIX_RANDOMSET = PREFIX_MANDALA
        
        // Singleton instance
        @Volatile
        private var instance: DefaultsConfig? = null
        
        fun getInstance(context: Context): DefaultsConfig {
            return instance ?: synchronized(this) {
                instance ?: DefaultsConfig(context).also { instance = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // HDMI Output Settings
    
    fun isHdmiEnabled(): Boolean {
        return prefs.getBoolean("${PREFIX_HDMI}enabled", true)
    }
    
    fun setHdmiEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("${PREFIX_HDMI}enabled", enabled) }
    }
    
    // Random Set Defaults - Arms
    
    fun getArmDefaults(): ArmDefaults {
        return ArmDefaults(
            baseLengthMin = prefs.getInt("${PREFIX_ARM}base_length_min", 0),
            baseLengthMax = prefs.getInt("${PREFIX_ARM}base_length_max", 100),
            beatProbability = prefs.getFloat("${PREFIX_ARM}beat_probability", 0.4f),
            lfoProbability = prefs.getFloat("${PREFIX_ARM}lfo_probability", 0.4f),
            randomProbability = prefs.getFloat("${PREFIX_ARM}random_probability", 0.2f),
            defaultEnableRandom = prefs.getBoolean("${PREFIX_ARM}default_enable_random", false),
            beatDivMin = prefs.getFloat("${PREFIX_ARM}beat_div_min", STANDARD_BEAT_VALUES.first()),
            beatDivMax = prefs.getFloat("${PREFIX_ARM}beat_div_max", 32f),
            sineProbability = prefs.getFloat("${PREFIX_ARM}sine_probability", 0.33f),
            triangleProbability = prefs.getFloat("${PREFIX_ARM}triangle_probability", 0.34f),
            squareProbability = prefs.getFloat("${PREFIX_ARM}square_probability", 0.33f),
            weightMin = prefs.getInt("${PREFIX_ARM}weight_min", -100),
            weightMax = prefs.getInt("${PREFIX_ARM}weight_max", 100),
            lfoTimeMin = prefs.getFloat("${PREFIX_ARM}lfo_time_min", 1.0f),
            lfoTimeMax = prefs.getFloat("${PREFIX_ARM}lfo_time_max", 60.0f),
            randomGlideMin = prefs.getFloat("${PREFIX_ARM}random_glide_min", 0.1f),
            randomGlideMax = prefs.getFloat("${PREFIX_ARM}random_glide_max", 0.5f),
            phaseMin = prefs.getFloat("${PREFIX_ARM}phase_min", 0f),
            phaseMax = prefs.getFloat("${PREFIX_ARM}phase_max", 360f)
        )
    }
    
    fun saveArmDefaults(defaults: ArmDefaults) {
        prefs.edit {
            putInt("${PREFIX_ARM}base_length_min", defaults.baseLengthMin)
            putInt("${PREFIX_ARM}base_length_max", defaults.baseLengthMax)
            putFloat("${PREFIX_ARM}beat_probability", defaults.beatProbability)
            putFloat("${PREFIX_ARM}lfo_probability", defaults.lfoProbability)
            putFloat("${PREFIX_ARM}random_probability", defaults.randomProbability)
            putBoolean("${PREFIX_ARM}default_enable_random", defaults.defaultEnableRandom)
            putFloat("${PREFIX_ARM}beat_div_min", defaults.beatDivMin)
            putFloat("${PREFIX_ARM}beat_div_max", defaults.beatDivMax)
            putFloat("${PREFIX_ARM}sine_probability", defaults.sineProbability)
            putFloat("${PREFIX_ARM}triangle_probability", defaults.triangleProbability)
            putFloat("${PREFIX_ARM}square_probability", defaults.squareProbability)
            putInt("${PREFIX_ARM}weight_min", defaults.weightMin)
            putInt("${PREFIX_ARM}weight_max", defaults.weightMax)
            putFloat("${PREFIX_ARM}lfo_time_min", defaults.lfoTimeMin)
            putFloat("${PREFIX_ARM}lfo_time_max", defaults.lfoTimeMax)
            putFloat("${PREFIX_ARM}random_glide_min", defaults.randomGlideMin)
            putFloat("${PREFIX_ARM}random_glide_max", defaults.randomGlideMax)
            putFloat("${PREFIX_ARM}phase_min", defaults.phaseMin)
            putFloat("${PREFIX_ARM}phase_max", defaults.phaseMax)
        }
    }
    
    // Random Set Defaults - Rotation
    
    fun getRotationDefaults(): RotationDefaults {
        return RotationDefaults(
            clockwiseProbability = prefs.getFloat("${PREFIX_ROTATION}clockwise_probability", 0.5f),
            counterClockwiseProbability = prefs.getFloat("${PREFIX_ROTATION}counter_clockwise_probability", 0.5f),
            beatProbability = prefs.getFloat("${PREFIX_ROTATION}beat_probability", 0.6f),
            lfoProbability = prefs.getFloat("${PREFIX_ROTATION}lfo_probability", 0.2f),
            randomProbability = prefs.getFloat("${PREFIX_ROTATION}random_probability", 0.2f),
            beatDivMin = prefs.getFloat("${PREFIX_ROTATION}beat_div_min", 4f),
            beatDivMax = prefs.getFloat("${PREFIX_ROTATION}beat_div_max", 128f),
            randomBeatDivMin = prefs.getFloat("${PREFIX_ROTATION}random_beat_div_min", 4f),
            randomBeatDivMax = prefs.getFloat("${PREFIX_ROTATION}random_beat_div_max", 64f),
            lfoTimeMin = prefs.getFloat("${PREFIX_ROTATION}lfo_time_min", 5.0f),
            lfoTimeMax = prefs.getFloat("${PREFIX_ROTATION}lfo_time_max", 30.0f),
            randomGlideMin = prefs.getFloat("${PREFIX_ROTATION}random_glide_min", 0.1f),
            randomGlideMax = prefs.getFloat("${PREFIX_ROTATION}random_glide_max", 0.5f)
        )
    }
    
    fun saveRotationDefaults(defaults: RotationDefaults) {
        prefs.edit {
            putFloat("${PREFIX_ROTATION}clockwise_probability", defaults.clockwiseProbability)
            putFloat("${PREFIX_ROTATION}counter_clockwise_probability", defaults.counterClockwiseProbability)
            putFloat("${PREFIX_ROTATION}beat_probability", defaults.beatProbability)
            putFloat("${PREFIX_ROTATION}lfo_probability", defaults.lfoProbability)
            putFloat("${PREFIX_ROTATION}random_probability", defaults.randomProbability)
            putFloat("${PREFIX_ROTATION}beat_div_min", defaults.beatDivMin)
            putFloat("${PREFIX_ROTATION}beat_div_max", defaults.beatDivMax)
            putFloat("${PREFIX_ROTATION}random_beat_div_min", defaults.randomBeatDivMin)
            putFloat("${PREFIX_ROTATION}random_beat_div_max", defaults.randomBeatDivMax)
            putFloat("${PREFIX_ROTATION}lfo_time_min", defaults.lfoTimeMin)
            putFloat("${PREFIX_ROTATION}lfo_time_max", defaults.lfoTimeMax)
            putFloat("${PREFIX_ROTATION}random_glide_min", defaults.randomGlideMin)
            putFloat("${PREFIX_ROTATION}random_glide_max", defaults.randomGlideMax)
        }
    }
    
    // Random Set Defaults - Hue Offset
    
    fun getHueOffsetDefaults(): HueOffsetDefaults {
        return HueOffsetDefaults(
            forwardProbability = prefs.getFloat("${PREFIX_HUE}forward_probability", 0.5f),
            reverseProbability = prefs.getFloat("${PREFIX_HUE}reverse_probability", 0.5f),
            beatProbability = prefs.getFloat("${PREFIX_HUE}beat_probability", 0.6f),
            lfoProbability = prefs.getFloat("${PREFIX_HUE}lfo_probability", 0.2f),
            randomProbability = prefs.getFloat("${PREFIX_HUE}random_probability", 0.2f),
            beatDivMin = prefs.getFloat("${PREFIX_HUE}beat_div_min", 4f),
            beatDivMax = prefs.getFloat("${PREFIX_HUE}beat_div_max", 16f),
            lfoTimeMin = prefs.getFloat("${PREFIX_HUE}lfo_time_min", 10.0f),
            lfoTimeMax = prefs.getFloat("${PREFIX_HUE}lfo_time_max", 60.0f),
            randomGlideMin = prefs.getFloat("${PREFIX_HUE}random_glide_min", 0.1f),
            randomGlideMax = prefs.getFloat("${PREFIX_HUE}random_glide_max", 0.5f)
        )
    }
    
    fun saveHueOffsetDefaults(defaults: HueOffsetDefaults) {
        prefs.edit {
            putFloat("${PREFIX_HUE}forward_probability", defaults.forwardProbability)
            putFloat("${PREFIX_HUE}reverse_probability", defaults.reverseProbability)
            putFloat("${PREFIX_HUE}beat_probability", defaults.beatProbability)
            putFloat("${PREFIX_HUE}lfo_probability", defaults.lfoProbability)
            putFloat("${PREFIX_HUE}random_probability", defaults.randomProbability)
            putFloat("${PREFIX_HUE}beat_div_min", defaults.beatDivMin)
            putFloat("${PREFIX_HUE}beat_div_max", defaults.beatDivMax)
            putFloat("${PREFIX_HUE}lfo_time_min", defaults.lfoTimeMin)
            putFloat("${PREFIX_HUE}lfo_time_max", defaults.lfoTimeMax)
            putFloat("${PREFIX_HUE}random_glide_min", defaults.randomGlideMin)
            putFloat("${PREFIX_HUE}random_glide_max", defaults.randomGlideMax)
        }
    }
    
    // Recipe Defaults
    
    fun getRecipeDefaults(): RecipeDefaults {
        return RecipeDefaults(
            preferFavorites = prefs.getBoolean("${PREFIX_RECIPE}prefer_favorites", true),
            minPetalCount = prefs.getInt("${PREFIX_RECIPE}min_petal_count", 3),
            maxPetalCount = prefs.getInt("${PREFIX_RECIPE}max_petal_count", 12),
            autoHueSweep = prefs.getBoolean("${PREFIX_RECIPE}auto_hue_sweep", true)
        )
    }
    
    fun saveRecipeDefaults(defaults: RecipeDefaults) {
        prefs.edit {
            putBoolean("${PREFIX_RECIPE}prefer_favorites", defaults.preferFavorites)
            putInt("${PREFIX_RECIPE}min_petal_count", defaults.minPetalCount)
            putInt("${PREFIX_RECIPE}max_petal_count", defaults.maxPetalCount)
            putBoolean("${PREFIX_RECIPE}auto_hue_sweep", defaults.autoHueSweep)
        }
    }
    
    // Feedback Defaults
    
    fun getFeedbackDefaults(): FeedbackDefaults {
        return FeedbackDefaults(
            fbDecayMin = prefs.getFloat("${PREFIX_FEEDBACK}fb_decay_min", 0.0f),
            fbDecayMax = prefs.getFloat("${PREFIX_FEEDBACK}fb_decay_max", 0.30f),
            fbGainMin = prefs.getFloat("${PREFIX_FEEDBACK}fb_gain_min", 0.85f),
            fbGainMax = prefs.getFloat("${PREFIX_FEEDBACK}fb_gain_max", 1.0f),
            fbZoomMin = prefs.getFloat("${PREFIX_FEEDBACK}fb_zoom_min", 0.5f),
            fbZoomMax = prefs.getFloat("${PREFIX_FEEDBACK}fb_zoom_max", 0.54f),
            fbRotateMin = prefs.getFloat("${PREFIX_FEEDBACK}fb_rotate_min", 0.5f),
            fbRotateMax = prefs.getFloat("${PREFIX_FEEDBACK}fb_rotate_max", 0.52f),
            fbShiftXMin = prefs.getFloat("${PREFIX_FEEDBACK}fb_shift_x_min", 0.0f),
            fbShiftXMax = prefs.getFloat("${PREFIX_FEEDBACK}fb_shift_x_max", 0.0f),
            fbShiftYMin = prefs.getFloat("${PREFIX_FEEDBACK}fb_shift_y_min", 0.0f),
            fbShiftYMax = prefs.getFloat("${PREFIX_FEEDBACK}fb_shift_y_max", 0.0f),
            fbBlurMin = prefs.getFloat("${PREFIX_FEEDBACK}fb_blur_min", 0.0f),
            fbBlurMax = prefs.getFloat("${PREFIX_FEEDBACK}fb_blur_max", 0.0f)
        )
    }
    
    fun saveFeedbackDefaults(defaults: FeedbackDefaults) {
        prefs.edit {
            putFloat("${PREFIX_FEEDBACK}fb_decay_min", defaults.fbDecayMin)
            putFloat("${PREFIX_FEEDBACK}fb_decay_max", defaults.fbDecayMax)
            putFloat("${PREFIX_FEEDBACK}fb_gain_min", defaults.fbGainMin)
            putFloat("${PREFIX_FEEDBACK}fb_gain_max", defaults.fbGainMax)
            putFloat("${PREFIX_FEEDBACK}fb_zoom_min", defaults.fbZoomMin)
            putFloat("${PREFIX_FEEDBACK}fb_zoom_max", defaults.fbZoomMax)
            putFloat("${PREFIX_FEEDBACK}fb_rotate_min", defaults.fbRotateMin)
            putFloat("${PREFIX_FEEDBACK}fb_rotate_max", defaults.fbRotateMax)
            putFloat("${PREFIX_FEEDBACK}fb_shift_x_min", defaults.fbShiftXMin)
            putFloat("${PREFIX_FEEDBACK}fb_shift_x_max", defaults.fbShiftXMax)
            putFloat("${PREFIX_FEEDBACK}fb_shift_y_min", defaults.fbShiftYMin)
            putFloat("${PREFIX_FEEDBACK}fb_shift_y_max", defaults.fbShiftYMax)
            putFloat("${PREFIX_FEEDBACK}fb_blur_min", defaults.fbBlurMin)
            putFloat("${PREFIX_FEEDBACK}fb_blur_max", defaults.fbBlurMax)
        }
    }

    // Composite Default Objects
    
    fun getMandalaDefaults(): MandalaDefaults {
        return MandalaDefaults(
            armDefaults = getArmDefaults(),
            rotationDefaults = getRotationDefaults(),
            hueOffsetDefaults = getHueOffsetDefaults(),
            recipeDefaults = getRecipeDefaults(),
            feedbackDefaults = getFeedbackDefaults()
        )
    }
    
    fun saveMandalaDefaults(defaults: MandalaDefaults) {
        saveArmDefaults(defaults.armDefaults)
        saveRotationDefaults(defaults.rotationDefaults)
        saveHueOffsetDefaults(defaults.hueOffsetDefaults)
        saveRecipeDefaults(defaults.recipeDefaults)
        saveFeedbackDefaults(defaults.feedbackDefaults)
    }
    
    // For backward compatibility
    fun getRandomSetDefaults(): RandomSetDefaults {
        return RandomSetDefaults(
            armDefaults = getArmDefaults(),
            rotationDefaults = getRotationDefaults(),
            hueOffsetDefaults = getHueOffsetDefaults()
        )
    }
    
    fun saveRandomSetDefaults(defaults: RandomSetDefaults) {
        saveArmDefaults(defaults.armDefaults)
        saveRotationDefaults(defaults.rotationDefaults)
        saveHueOffsetDefaults(defaults.hueOffsetDefaults)
    }
    
    // Reset to factory defaults
    
    fun resetArmDefaults() {
        saveArmDefaults(ArmDefaults())
    }
    
    fun resetRotationDefaults() {
        saveRotationDefaults(RotationDefaults())
    }
    
    fun resetHueOffsetDefaults() {
        saveHueOffsetDefaults(HueOffsetDefaults())
    }
    
    fun resetRecipeDefaults() {
        saveRecipeDefaults(RecipeDefaults())
    }
    
    fun resetFeedbackDefaults() {
        saveFeedbackDefaults(FeedbackDefaults())
    }
    
    fun resetAllDefaults() {
        resetArmDefaults()
        resetRotationDefaults()
        resetHueOffsetDefaults()
        resetRecipeDefaults()
        resetFeedbackDefaults()
    }
}
