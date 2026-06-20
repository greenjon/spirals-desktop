package llm.slop.spirals.rendering

import llm.slop.spirals.parameters.ModulatableParameter
import kotlin.math.abs
import kotlin.math.max

/**
 * Data class representing the frequency ratios (recipe) for a 4-arm mandala.
 */
data class Mandala4Arm(
    val id: String,
    val a: Int,
    val b: Int,
    val c: Int,
    val d: Int,
    val petals: Int = 3,
    val shapeRatio: Float = 4.0f,
    val multiplicityClass: Int = 2,
    val independentFreqCount: Int = 3,
    val twoFoldLikely: Boolean = true,
    val hierarchyDepth: Int = 0,
    val dominanceRatio: Float = 4.0f,
    val radialVariance: Float = 10.8f
)

typealias MandalaRatio = Mandala4Arm

/**
 * Encapsulates the state and parameters of a Mandala.
 * Implements VisualSource for desktop rendering.
 */
class Mandala(
    var recipe: MandalaRatio
) : VisualSource {

    override val parameters = linkedMapOf(
        "L1" to ModulatableParameter(0.4f),
        "L2" to ModulatableParameter(0.3f),
        "L3" to ModulatableParameter(0.2f),
        "L4" to ModulatableParameter(0.1f),
        "Scale" to ModulatableParameter(0.125f),
        "Rotation" to ModulatableParameter(0.0f),
        "Thickness" to ModulatableParameter(0.1f),
        "Hue Offset" to ModulatableParameter(0.0f),
        "Hue Sweep" to ModulatableParameter(1.0f / 9.0f),
        "Depth" to ModulatableParameter(0.35f)
    )

    override val globalAlpha = ModulatableParameter(1.0f)
    override val globalScale = ModulatableParameter(1.0f)

    var minR: Float = 0f
        private set
    var maxR: Float = 1f
        private set

    override fun update() {
        super.update()

        // Calculate max possible reach of the arms to normalize distance in the shader
        val l1 = abs(parameters["L1"]?.value ?: 0f)
        val l2 = abs(parameters["L2"]?.value ?: 0f)
        val l3 = abs(parameters["L3"]?.value ?: 0f)
        val l4 = abs(parameters["L4"]?.value ?: 0f)

        maxR = max(0.001f, l1 + l2 + l3 + l4)
        minR = 0f // Stable base for depth/brightness effect
    }

    companion object {
        const val POINTS = 2048

        /**
         * Static buffer for GPU expansion containing [Phase, Side] pairs.
         * Used to generate a ribbon geometry of points along the parameterized curve.
         */
        val expansionBuffer: FloatArray by lazy {
            val buffer = FloatArray((POINTS + 1) * 2 * 2)
            for (i in 0..POINTS) {
                val phase = i.toFloat() / POINTS.toFloat()
                // Left vertex
                buffer[i * 4 + 0] = phase
                buffer[i * 4 + 1] = -1.0f
                // Right vertex
                buffer[i * 4 + 2] = phase
                buffer[i * 4 + 3] = 1.0f
            }
            buffer
        }
    }
}
