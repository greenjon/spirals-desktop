package llm.slop.spirals.rendering

import llm.slop.spirals.parameters.MeterType
import llm.slop.spirals.parameters.ModulatableParameter
import kotlinx.serialization.Serializable

@Serializable
data class ParamMeta(
    val name: String,
    val default: Float,
    val min: Float,
    val max: Float,
    val type: String = "MONOPOLAR",
    val defaultMin: Float? = null,
    val defaultMax: Float? = null
)

@Serializable
data class SourceMeta(
    val id: String,
    val name: String,
    val parameters: List<ParamMeta>,
    val feedback: Boolean = false
)

class DynamicVisualSource(
    val id: String,
    val displayName: String,
    val shader: Shader,
    override val parameters: LinkedHashMap<String, ModulatableParameter>,
    override val globalAlpha: ModulatableParameter = ModulatableParameter(1.0f),
    val hasFeedback: Boolean = false
) : VisualSource {
    var fb1: FBO? = null
    var fb2: FBO? = null
    var fbIndex: Int = 0

    fun swapFeedbackBuffers() {
        fbIndex = 1 - fbIndex
    }

    fun getCurrentHistoryFBO(): FBO? = if (fbIndex == 0) fb1 else fb2
    fun getNextHistoryFBO(): FBO? = if (fbIndex == 0) fb2 else fb1

    override fun dispose() {
        fb1?.dispose()
        fb2?.dispose()
        fb1 = null
        fb2 = null
    }

    override fun clear() {
        fb1?.clear(0f, 0f, 0f, 0f)
        fb2?.clear(0f, 0f, 0f, 0f)
    }

    override fun clone(): DynamicVisualSource {
        val clonedParams = LinkedHashMap<String, ModulatableParameter>()
        this.parameters.forEach { (name, param) ->
            clonedParams[name] = param.clone()
        }
        return DynamicVisualSource(
            id = this.id,
            displayName = this.displayName,
            shader = this.shader,
            parameters = clonedParams,
            globalAlpha = this.globalAlpha.clone(),
            hasFeedback = this.hasFeedback
        )
    }
}
