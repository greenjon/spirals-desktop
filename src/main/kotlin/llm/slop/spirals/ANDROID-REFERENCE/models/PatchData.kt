package llm.slop.spirals.models

import kotlinx.serialization.Serializable

@Serializable
data class PatchData(
    val name: String,
    val recipeId: String,
    val parameters: List<ParameterData>,
    val version: Int = 3 // Incremented version for LFO speed mode
)

@Serializable
data class ParameterData(
    val id: String,
    val baseValue: Float,
    val modulators: List<ModulatorData>
)

@Serializable
data class ModulatorData(
    val sourceId: String,
    val operator: String,
    val weight: Float,
    val bypassed: Boolean = false,
    val waveform: String = "SINE",
    val subdivision: Float = 1.0f,
    val phaseOffset: Float = 0.0f,
    val slope: Float = 0.5f,
    val lfoSpeedMode: String = "FAST"
)
