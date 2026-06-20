package llm.slop.spirals.models

import llm.slop.spirals.cv.core.CvModulator

/**
 * Data model for a complete Mandala configuration (Patch).
 */
data class MandalaPatch(
    val name: String,
    val recipeId: String, // ID from MandalaRatio
    val parameterSettings: Map<String, ParameterSetting>
)

data class ParameterSetting(
    val baseValue: Float,
    val modulators: List<CvModulator>
)
