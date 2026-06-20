package llm.slop.spirals.ui

import imgui.ImGui
import imgui.flag.ImGuiConfigFlags
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import imgui.type.ImInt
import llm.slop.spirals.rendering.Deck
import llm.slop.spirals.rendering.Mandala
import llm.slop.spirals.rendering.Mixer
import mu.KotlinLogging
import org.lwjgl.opengl.GL33.*

/**
 * Manages the ImGui overlay for desktop control.
 */
class UIManager(private val windowHandle: Long) {
    private val logger = KotlinLogging.logger {}
    private val imguiGlfw = ImGuiImplGlfw()
    private val imguiGl3 = ImGuiImplGl3()

    init {
        logger.info { "Initializing ImGui..." }
        ImGui.createContext()
        val io = ImGui.getIO()
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard)

        // Initialize platform and renderer bindings
        imguiGlfw.init(windowHandle, true)
        imguiGl3.init("#version 150")
        logger.info { "UIManager initialized" }
    }

    /**
     * Renders the control UI for the video Mixer and Decks.
     */
    fun render(mixer: Mixer) {
        imguiGlfw.newFrame()
        ImGui.newFrame()

        // Setup UI Windows
        drawMenuBar()
        ImGui.showDemoWindow()
        drawControlWindow(mixer)

        // Rendering
        ImGui.render()
        imguiGl3.renderDrawData(ImGui.getDrawData())
    }

    private fun drawMenuBar() {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Exit")) {
                    // We can close via GLFW but for now just log
                    logger.info { "Exit menu item clicked" }
                }
                ImGui.endMenu()
            }
            ImGui.endMainMenuBar()
        }
    }

    private fun drawControlWindow(mixer: Mixer) {
        ImGui.begin("Spirals Control")
        ImGui.text("VJ Mixer Panel")
        ImGui.separator()

        if (ImGui.collapsingHeader("System Status")) {
            ImGui.text("OpenGL: ${glGetString(GL_VERSION)}")
            ImGui.text("Renderer: ${glGetString(GL_RENDERER)}")
            ImGui.text("FPS: %.1f".format(ImGui.getIO().framerate))
            ImGui.separator()
            ImGui.text("Master FBO:")
            ImGui.text("  ID: ${mixer.masterFBO.framebufferId}")
            ImGui.text("  Size: ${mixer.masterFBO.width}x${mixer.masterFBO.height}")
            ImGui.text("  Texture: ${mixer.masterFBO.texture}")
        }

        // Mixer controls
        if (ImGui.collapsingHeader("Mixer Section")) {
            // Crossfade
            val xfade = floatArrayOf(mixer.crossfade.baseValue)
            if (ImGui.sliderFloat("Crossfade (A -> B)", xfade, 0f, 1f)) {
                mixer.crossfade.set(xfade[0])
            }

            // Blend Mode dropdown
            val modes = arrayOf("ADD", "SCREEN", "MULT", "MAX", "XFADE")
            val currentMode = ImInt(mixer.mode.baseValue.toInt())
            if (ImGui.combo("Blend Mode", currentMode, modes)) {
                mixer.mode.set(currentMode.get().toFloat())
            }

            // Master Alpha
            val alpha = floatArrayOf(mixer.masterAlpha.baseValue)
            if (ImGui.sliderFloat("Master Gain", alpha, 0f, 1f)) {
                mixer.masterAlpha.set(alpha[0])
            }
        }

        // Decks controls
        drawDeckControls("Deck A (Source & Feedback)", mixer.deckA)
        drawDeckControls("Deck B (Source & Feedback)", mixer.deckB)

        ImGui.end()
    }

    private fun drawDeckControls(label: String, deck: Deck) {
        ImGui.pushID(label)
        if (ImGui.collapsingHeader(label)) {
            val mandala = deck.source as? Mandala
            if (mandala != null) {
                if (ImGui.treeNode("Mandala Parameters")) {
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

                    ImGui.text("Recipe (Freqs): a=%d, b=%d, c=%d, d=%d".format(mandala.recipe.a, mandala.recipe.b, mandala.recipe.c, mandala.recipe.d))
                    ImGui.treePop()
                }
            }

            if (ImGui.treeNode("Feedback Settings")) {
                val decay = floatArrayOf(deck.fbDecay.baseValue)
                if (ImGui.sliderFloat("Decay", decay, 0.0f, 0.2f)) deck.fbDecay.set(decay[0])

                val gain = floatArrayOf(deck.fbGain.baseValue)
                if (ImGui.sliderFloat("Gain", gain, 0.9f, 1.1f)) deck.fbGain.set(gain[0])

                val zoom = floatArrayOf(deck.fbZoom.baseValue)
                if (ImGui.sliderFloat("Zoom (Rotate/Scale)", zoom, -0.1f, 0.1f)) deck.fbZoom.set(zoom[0])

                val rotate = floatArrayOf(deck.fbRotate.baseValue)
                if (ImGui.sliderFloat("Rotate Speed", rotate, -0.1f, 0.1f)) deck.fbRotate.set(rotate[0])

                val hueShift = floatArrayOf(deck.fbHueShift.baseValue)
                if (ImGui.sliderFloat("Hue Shift Rate", hueShift, -0.1f, 0.1f)) deck.fbHueShift.set(hueShift[0])

                val blur = floatArrayOf(deck.fbBlur.baseValue)
                if (ImGui.sliderFloat("Feedback Blur", blur, 0.0f, 0.2f)) deck.fbBlur.set(blur[0])

                ImGui.treePop()
            }
        }
        ImGui.popID()
    }

    /**
     * Cleans up ImGui platform/renderer bindings.
     */
    fun dispose() {
        logger.info { "Disposing ImGui resources..." }
        imguiGl3.dispose()
        imguiGlfw.dispose()
        ImGui.destroyContext()
    }
}
