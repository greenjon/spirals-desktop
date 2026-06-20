package llm.slop.spirals.ui

import imgui.ImGui
import imgui.flag.ImGuiConfigFlags
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import llm.slop.spirals.rendering.FBO
import llm.slop.spirals.rendering.Mandala
import mu.KotlinLogging
import org.lwjgl.opengl.GL33.*

private val logger = KotlinLogging.logger {}

class UIManager(private val windowHandle: Long) {
    private val imguiGlfw = ImGuiImplGlfw()
    private val imguiGl3 = ImGuiImplGl3()

    // Persistent state for UI controls
    private val crossfade = floatArrayOf(0.5f)

    init {
        ImGui.createContext()
        val io = ImGui.getIO()
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard)
        io.iniFilename = null // Disable imgui.ini

        imguiGlfw.init(windowHandle, true)
        imguiGl3.init("#version 330 core")

        logger.info { "UIManager initialized" }
    }

    fun render(testFBO: FBO, mandala: Mandala) {
        imguiGlfw.newFrame()
        ImGui.newFrame()

        // Setup UI Windows
        drawMenuBar()
        ImGui.showDemoWindow()
        drawControlWindow(testFBO, mandala)

        // Rendering
        ImGui.render()
        imguiGl3.renderDrawData(ImGui.getDrawData())
    }

    private fun drawMenuBar() {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("New Patch")) { logger.info { "New patch" } }
                if (ImGui.menuItem("Open Patch...")) { logger.info { "Open patch" } }
                if (ImGui.menuItem("Save Patch")) { logger.info { "Save patch" } }
                ImGui.separator()
                if (ImGui.menuItem("Exit")) {
                    // We'll handle exit in Main
                }
                ImGui.endMenu()
            }
            if (ImGui.beginMenu("View")) {
                if (ImGui.menuItem("CV Sources")) { }
                if (ImGui.menuItem("Parameter Grid")) { }
                ImGui.endMenu()
            }
            ImGui.endMainMenuBar()
        }
    }

    private fun drawControlWindow(testFBO: FBO, mandala: Mandala) {
        ImGui.begin("Spirals Control")
        ImGui.text("Welcome to Spirals Desktop!")
        ImGui.separator()

        if (ImGui.collapsingHeader("Status")) {
            ImGui.text("OpenGL: ${glGetString(GL_VERSION)}")
            ImGui.text("Renderer: ${glGetString(GL_RENDERER)}")
            ImGui.text("FPS: %.1f".format(ImGui.getIO().framerate))
            ImGui.separator()
            ImGui.text("FBO Test:")
            ImGui.text("  ID: ${testFBO.framebufferId}")
            ImGui.text("  Size: ${testFBO.width}x${testFBO.height}")
            ImGui.text("  Texture: ${testFBO.texture}")
        }

        if (ImGui.collapsingHeader("Mixer")) {
            ImGui.text("Deck A / Deck B")
            if (ImGui.sliderFloat("Crossfade", crossfade, 0f, 1f)) {
                logger.debug { "Crossfade: ${crossfade[0]}" }
            }
        }

        if (ImGui.collapsingHeader("Mandala Parameters")) {
            val l1 = floatArrayOf(mandala.parameters["L1"]?.baseValue ?: 0f)
            if (ImGui.sliderFloat("L1", l1, -1f, 1f)) mandala.parameters["L1"]?.set(l1[0])

            val l2 = floatArrayOf(mandala.parameters["L2"]?.baseValue ?: 0f)
            if (ImGui.sliderFloat("L2", l2, -1f, 1f)) mandala.parameters["L2"]?.set(l2[0])

            val l3 = floatArrayOf(mandala.parameters["L3"]?.baseValue ?: 0f)
            if (ImGui.sliderFloat("L3", l3, -1f, 1f)) mandala.parameters["L3"]?.set(l3[0])

            val l4 = floatArrayOf(mandala.parameters["L4"]?.baseValue ?: 0f)
            if (ImGui.sliderFloat("L4", l4, -1f, 1f)) mandala.parameters["L4"]?.set(l4[0])

            val scale = floatArrayOf(mandala.parameters["Scale"]?.baseValue ?: 0f)
            if (ImGui.sliderFloat("Scale", scale, 0f, 1f)) mandala.parameters["Scale"]?.set(scale[0])

            val rotation = floatArrayOf(mandala.parameters["Rotation"]?.baseValue ?: 0f)
            if (ImGui.sliderFloat("Rotation", rotation, 0f, 1f)) mandala.parameters["Rotation"]?.set(rotation[0])

            val thickness = floatArrayOf(mandala.parameters["Thickness"]?.baseValue ?: 0f)
            if (ImGui.sliderFloat("Thickness", thickness, 0.001f, 0.5f)) mandala.parameters["Thickness"]?.set(thickness[0])

            val hueOffset = floatArrayOf(mandala.parameters["Hue Offset"]?.baseValue ?: 0f)
            if (ImGui.sliderFloat("Hue Offset", hueOffset, 0f, 1f)) mandala.parameters["Hue Offset"]?.set(hueOffset[0])

            val hueSweep = floatArrayOf(mandala.parameters["Hue Sweep"]?.baseValue ?: 0f)
            if (ImGui.sliderFloat("Hue Sweep", hueSweep, -2f, 2f)) mandala.parameters["Hue Sweep"]?.set(hueSweep[0])

            val depth = floatArrayOf(mandala.parameters["Depth"]?.baseValue ?: 0f)
            if (ImGui.sliderFloat("Depth", depth, 0f, 1f)) mandala.parameters["Depth"]?.set(depth[0])

            ImGui.separator()
            ImGui.text("Recipe (Freqs): a=%d, b=%d, c=%d, d=%d".format(mandala.recipe.a, mandala.recipe.b, mandala.recipe.c, mandala.recipe.d))
        }

        ImGui.end()
    }

    fun dispose() {
        imguiGl3.dispose()
        imguiGlfw.dispose()
        ImGui.destroyContext()
        logger.info { "UIManager disposed" }
    }
}
