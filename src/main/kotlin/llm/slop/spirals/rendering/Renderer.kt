package llm.slop.spirals.rendering

import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer

/**
 * Main OpenGL renderer class.
 * Handles loading of the mandala shader, VAO/VBO initialization, and rendering.
 */
class Renderer {
    private val mandalaShader: Shader
    private var mandalaVAO: Int = 0
    private var mandalaVBO: Int = 0
    private var isDisposed = false

    init {
        // Load the mandala vertex and fragment shaders
        mandalaShader = Shader.fromResources("shaders/mandala.vert", "shaders/mandala.frag")

        // Initialize VAO and VBO for Mandala geometry
        val expansionBuffer = Mandala.expansionBuffer
        val buffer: FloatBuffer = MemoryUtil.memAllocFloat(expansionBuffer.size)
        buffer.put(expansionBuffer).flip()

        try {
            mandalaVAO = glGenVertexArrays()
            mandalaVBO = glGenBuffers()

            glBindVertexArray(mandalaVAO)
            glBindBuffer(GL_ARRAY_BUFFER, mandalaVBO)
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)

            // Location 0: vec2 [phase, side]
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.SIZE_BYTES, 0)
            glEnableVertexAttribArray(0)

            glBindBuffer(GL_ARRAY_BUFFER, 0)
            glBindVertexArray(0)
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    /**
     * Renders a Mandala to the specified FBO.
     */
    fun render(mandala: Mandala, targetFBO: FBO) {
        targetFBO.bind()

        // Clear the framebuffer's color buffer (fully transparent black background)
        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT)

        mandalaShader.bind()

        // Set arm length parameters (L1 to L4)
        val p = mandala.parameters
        mandalaShader.setUniform("uL1", p["L1"]?.value ?: 0f)
        mandalaShader.setUniform("uL2", p["L2"]?.value ?: 0f)
        mandalaShader.setUniform("uL3", p["L3"]?.value ?: 0f)
        mandalaShader.setUniform("uL4", p["L4"]?.value ?: 0f)

        // Set arm frequency parameters (a to d)
        mandalaShader.setUniform("uA", mandala.recipe.a.toFloat())
        mandalaShader.setUniform("uB", mandala.recipe.b.toFloat())
        mandalaShader.setUniform("uC", mandala.recipe.c.toFloat())
        mandalaShader.setUniform("uD", mandala.recipe.d.toFloat())

        // Calculate and set global transformation uniforms
        val scale = (p["Scale"]?.value ?: 0.125f) * mandala.globalScale.value * 8.0f
        val rotation = (p["Rotation"]?.value ?: 0f) * 2.0f * Math.PI.toFloat()
        val thickness = p["Thickness"]?.value ?: 0.1f
        val aspect = targetFBO.width.toFloat() / targetFBO.height.toFloat()

        mandalaShader.setUniform("uGlobalScale", scale)
        mandalaShader.setUniform("uGlobalRotation", rotation)
        mandalaShader.setUniform("uThickness", thickness)
        mandalaShader.setUniform("uAspectRatio", aspect)

        // Set color-related uniforms
        val hueOffset = p["Hue Offset"]?.value ?: 0f
        val hueSweep = ((p["Hue Sweep"]?.value ?: (1.0f / 9.0f)) * 9.0f).toInt().toFloat()
        val depth = p["Depth"]?.value ?: 0.35f

        mandalaShader.setUniform("uHueOffset", hueOffset)
        mandalaShader.setUniform("uHueSweep", hueSweep)
        mandalaShader.setUniform("uDepth", depth)
        mandalaShader.setUniform("uMaxR", mandala.maxR)
        mandalaShader.setUniform("uAlpha", mandala.globalAlpha.value)

        // Render the mandala ribbon
        glBindVertexArray(mandalaVAO)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, (Mandala.POINTS + 1) * 2)

        glBindVertexArray(0)
        mandalaShader.unbind()
        targetFBO.unbind()
    }

    /**
     * Clean up OpenGL resources.
     */
    fun dispose() {
        if (!isDisposed) {
            glDeleteBuffers(mandalaVBO)
            glDeleteVertexArrays(mandalaVAO)
            mandalaShader.dispose()
            isDisposed = true
        }
    }
}
