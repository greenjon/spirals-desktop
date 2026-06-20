package llm.slop.spirals

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import llm.slop.spirals.cv.core.ModulatableParameter
import llm.slop.spirals.models.mandala.MandalaRatio
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Interface for renderable visual objects that consume modulatable parameters.
 */
interface VisualSource {
    /**
     * Map of parameter names to their modulatable counterparts.
     */
    val parameters: Map<String, ModulatableParameter>

    /**
     * Top-level parameters for mixing and composition.
     */
    val globalAlpha: ModulatableParameter
    val globalScale: ModulatableParameter

    /**
     * Trigger evaluation of all parameters.
     * Expected to be called at 120Hz or per frame.
     */
    fun update() {
        parameters.values.forEach { it.evaluate() }
        globalAlpha.evaluate()
        globalScale.evaluate()
    }

    /**
     * Render the visual state to the canvas.
     */
    fun render(canvas: Canvas, width: Int, height: Int)
}

/**
 * Optimized VisualSource that avoids allocations in the render loop.
 * Renders exactly one complete closed loop based on integer frequencies.
 */
class MandalaVisualSource : VisualSource {
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
        "Depth" to ModulatableParameter(0.35f),
        // Feedback Engine Parameters
        "FB Gain" to ModulatableParameter(0.0f),
        "FB Zoom" to ModulatableParameter(0.5f),   // 0.5 = 0%
        "FB Rotate" to ModulatableParameter(0.5f), // 0.5 = 0 degrees
        "FB Shift" to ModulatableParameter(0.0f),
        "FB Blur" to ModulatableParameter(0.0f)
    )

    override val globalAlpha = ModulatableParameter(1.0f)
    override val globalScale = ModulatableParameter(1.0f)

    var recipe: MandalaRatio = MandalaLibrary.MandalaRatios.first()

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val hsvBuffer = FloatArray(3)

    private val points = 2048
    // Static buffer for GPU expansion: [Phase, Side] pairs
    val expansionBuffer = FloatArray((points + 1) * 2 * 2)

    // Radial Brightness tracking
    var minR: Float = 0f
        private set
    var maxR: Float = 1f
        private set

    // Optimization flags
    var isDirty = true
    private var lastRecipeId = ""

    init {
        for (i in 0..points) {
            val phase = i.toFloat() / points.toFloat()
            // Left vertex
            expansionBuffer[i * 4 + 0] = phase
            expansionBuffer[i * 4 + 1] = -1.0f
            // Right vertex
            expansionBuffer[i * 4 + 2] = phase
            expansionBuffer[i * 4 + 3] = 1.0f
        }
    }

    override fun update() {
        super.update()

        // Optimization: Use mathematical heuristic for bounds instead of a 2048-point loop
        // maxR is the sum of absolute arm lengths (the maximum possible reach)
        val l1 = abs(parameters["L1"]?.value ?: 0f)
        val l2 = abs(parameters["L2"]?.value ?: 0f)
        val l3 = abs(parameters["L3"]?.value ?: 0f)
        val l4 = abs(parameters["L4"]?.value ?: 0f)

        maxR = max(0.001f, l1 + l2 + l3 + l4)
        minR = 0f // Stable base for depth effect

        if (recipe.id != lastRecipeId) {
            lastRecipeId = recipe.id
            isDirty = true
        }
    }

    override fun render(canvas: Canvas, width: Int, height: Int) {
        val alpha = globalAlpha.value
        if (alpha <= 0f) return

        val canvasScaleFactor = width / 2f
        val scale = (parameters["Scale"]?.value ?: 0.125f) * globalScale.value * 8.0f
        val thickness = (parameters["Thickness"]?.value ?: 0.1f) * 20f
        val hueOffset = parameters["Hue Offset"]?.value ?: 0f
        // Quantize sweep to integers for perfect loop seamlessness
        val hueSweep = ((parameters["Hue Sweep"]?.value ?: (1.0f / 9.0f)) * 9.0f).roundToInt().toFloat()
        val depth = parameters["Depth"]?.value ?: 0.35f
        val rotationDegrees = (parameters["Rotation"]?.value ?: 0f) * 360f

        val l1 = parameters["L1"]?.value ?: 0.4f
        val l2 = parameters["L2"]?.value ?: 0.3f
        val l3 = parameters["L3"]?.value ?: 0.2f
        val l4 = parameters["L4"]?.value ?: 0.1f

        paint.strokeWidth = thickness
        hsvBuffer[1] = 0.8f

        val cx = width / 2f
        val cy = height / 2f

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(rotationDegrees)
        canvas.scale(scale * canvasScaleFactor, scale * canvasScaleFactor)

        val dt = (2.0 * PI) / points
        var prevX = (l1 + l2 + l3 + l4)
        var prevY = 0f

        for (i in 1..points) {
            val t = i * dt
            val phase = i.toFloat() / points.toFloat()

            val x = (l1 * cos(t * recipe.a) + l2 * cos(t * recipe.b) + l3 * cos(t * recipe.c) + l4 * cos(t * recipe.d)).toFloat()
            val y = (l1 * sin(t * recipe.a) + l2 * sin(t * recipe.b) + l3 * sin(t * recipe.c) + l4 * sin(t * recipe.d)).toFloat()

            // Visual Parity with GPU Depth Heuristic
            val r = sqrt(x * x + y * y)
            val rNorm = (r / maxR).coerceIn(0f, 1f)
            val f = Math.pow(rNorm.toDouble(), 0.7).toFloat()
            val v = 0.85f * (1.0f - depth + depth * f)

            hsvBuffer[2] = v
            hsvBuffer[0] = ((hueOffset + phase * hueSweep) % 1.0f) * 360f
            val color = Color.HSVToColor(hsvBuffer)
            paint.color = Color.argb((alpha * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))

            canvas.drawLine(prevX, prevY, x, y, paint)
            prevX = x
            prevY = y
        }

        canvas.restore()
    }

    fun copy(): MandalaVisualSource {
        val newSource = MandalaVisualSource()
        // Deep copy parameters
        this.parameters.forEach { (key, param) ->
            newSource.parameters[key]?.baseValue = param.baseValue
            newSource.parameters[key]?.modulators?.clear()
            newSource.parameters[key]?.modulators?.addAll(param.modulators)
        }
        // Deep copy globalAlpha and globalScale
        newSource.globalAlpha.baseValue = this.globalAlpha.baseValue
        newSource.globalAlpha.modulators.clear()
        newSource.globalAlpha.modulators.addAll(this.globalAlpha.modulators)

        newSource.globalScale.baseValue = this.globalScale.baseValue
        newSource.globalScale.modulators.clear()
        newSource.globalScale.modulators.addAll(this.globalScale.modulators)

        newSource.recipe = this.recipe.copy() // Mandala4Arm is a data class, so its copy is deep enough.
        newSource.minR = this.minR
        newSource.maxR = this.maxR
        newSource.isDirty = this.isDirty
        // Note: expansionBuffer is statically initialized, no need to copy
        return newSource
    }
}