package llm.slop.spirals.rendering

import kotlinx.serialization.Serializable

/** Standard beat subdivision values: 1/16 through 256 beats. */
val STANDARD_BEAT_VALUES = listOf(0.0625f, 0.125f, 0.25f, 0.5f, 1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f)

@Serializable
enum class RecipeFilter { ALL, PETALS_EXACT, PETALS_RANGE, SPECIFIC_IDS }

@Serializable
data class ArmConstraints(
    val baseLengthMin: Int = 0,
    val baseLengthMax: Int = 100,
    val enableBeat: Boolean = false,
    val enableLfo: Boolean = false,
    val enableRandom: Boolean = false,
    val allowSine: Boolean = true,
    val allowTriangle: Boolean = true,
    val allowSquare: Boolean = false,
    val weightMin: Int = -100,
    val weightMax: Int = 100,
    val beatDivMin: Float = 0.0625f,
    val beatDivMax: Float = 32.0f,
    val lfoTimeMin: Float = 1.0f,
    val lfoTimeMax: Float = 60.0f,
    val randomGlideMin: Float = 0.1f,
    val randomGlideMax: Float = 0.5f,
    val phaseMin: Float = 0f,
    val phaseMax: Float = 360f
)

@Serializable
data class RotationConstraints(
    val enableClockwise: Boolean = true,
    val enableCounterClockwise: Boolean = true,
    val enableBeat: Boolean = true,
    val enableLfo: Boolean = false,
    val enableRandom: Boolean = false,
    val beatDivMin: Float = 4f,
    val beatDivMax: Float = 256f,
    val lfoTimeMin: Float = 5.0f,
    val lfoTimeMax: Float = 30.0f,
    val randomGlideMin: Float = 0.1f,
    val randomGlideMax: Float = 0.5f
)

@Serializable
data class HueOffsetConstraints(
    val enableForward: Boolean = true,
    val enableReverse: Boolean = true,
    val enableBeat: Boolean = true,
    val enableLfo: Boolean = false,
    val enableRandom: Boolean = false,
    val beatDivMin: Float = 4f,
    val beatDivMax: Float = 16f,
    val lfoTimeMin: Float = 10.0f,
    val lfoTimeMax: Float = 60.0f,
    val randomGlideMin: Float = 0.1f,
    val randomGlideMax: Float = 0.5f
)

/** Top-level template that drives RecipeRandomizer. */
@Serializable
data class RandomSet(
    val name: String,
    val recipeFilter: RecipeFilter = RecipeFilter.ALL,
    val petalCount: Int? = null,
    val petalMin: Int? = null,
    val petalMax: Int? = null,
    val specificRecipeIds: List<String>? = null,
    val autoHueSweep: Boolean = true,
    val linkArms: Boolean = false,
    val l1Constraints: ArmConstraints? = null,
    val l2Constraints: ArmConstraints? = null,
    val l3Constraints: ArmConstraints? = null,
    val l4Constraints: ArmConstraints? = null,
    val rotationConstraints: RotationConstraints? = null,
    val hueOffsetConstraints: HueOffsetConstraints? = null
)
