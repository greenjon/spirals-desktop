package llm.slop.spirals.database.entities

import llm.slop.spirals.cv.core.CvModulator
import llm.slop.spirals.cv.core.ModulationOperator

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
    val modulators: List<CvModulatorSetting>
)

data class CvModulatorSetting(
    val sourceId: String,
    val operator: ModulationOperator,
    val weight: Float
)
