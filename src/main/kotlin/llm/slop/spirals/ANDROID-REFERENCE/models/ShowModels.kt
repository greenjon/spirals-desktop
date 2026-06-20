package llm.slop.spirals.models

import kotlinx.serialization.Serializable
import java.util.UUID

enum class TransitionType {
    NONE, IMPLODE_EXPLODE, EXPLODE_EXPLODE, IMPLODE_IMPLODE
}

@Serializable
data class ShowPatch(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val randomSetIds: List<String> = emptyList(),
    val prevTrigger: ModulatableParameterData = ModulatableParameterData(0.0f),
    val nextTrigger: ModulatableParameterData = ModulatableParameterData(0.0f),
    val randomTrigger: ModulatableParameterData = ModulatableParameterData(0.0f),
    val generateTrigger: ModulatableParameterData = ModulatableParameterData(0.0f),
    val transitionType: TransitionType = TransitionType.NONE,
    val transitionDurationBeats: Float = 0.0f,
    val transitionFadeOutPercent: Float = 0.5f,
    val transitionFadeInPercent: Float = 0.5f,
    val feedbackAmount: ModulatableParameterData = ModulatableParameterData(0.001f),
    val backgroundHue: ModulatableParameterData = ModulatableParameterData(0.0f),
    val backgroundBrightness: ModulatableParameterData = ModulatableParameterData(0.0f)
)
