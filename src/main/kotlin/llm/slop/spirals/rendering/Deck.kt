package llm.slop.spirals.rendering

import llm.slop.spirals.parameters.ModulatableParameter

/**
 * Represents a single visual rendering chain (Deck).
 * Manages its own offscreen Framebuffer Objects (FBOs) for ping-pong feedback effects,
 * as well as parameters that control the feedback loop.
 */
class Deck(
    var source: VisualSource,
    val width: Int = 1920,
    val height: Int = 1080
) {
    // FBO for rendering the clean visual source output
    val cleanFBO = FBO(width, height)


    // Ping-pong feedback FBOs
    val fb1 = FBO(width, height)
    val fb2 = FBO(width, height)
    private var fbIndex = 0

    // Feedback parameters
    val fbDecay = ModulatableParameter(0.02f)
    val fbGain = ModulatableParameter(1.0f)
    val fbZoom = ModulatableParameter(0.0f) // negative is zoom out, positive is zoom in
    val fbRotate = ModulatableParameter(0.0f) // in radians
    val fbHueShift = ModulatableParameter(0.0f) // range 0..1
    val fbBlur = ModulatableParameter(0.0f) // range 0..1

    /**
     * Retrieves the current history FBO (from the last frame).
     */
    fun getCurrentHistoryFBO(): FBO = if (fbIndex == 0) fb1 else fb2

    /**
     * Retrieves the target FBO for the new feedback combination.
     */
    fun getNextHistoryFBO(): FBO = if (fbIndex == 0) fb2 else fb1

    /**
     * Swaps the feedback FBO ping-pong index.
     */
    fun swapFeedbackBuffers() {
        fbIndex = 1 - fbIndex
    }

    /**
     * Updates the underlying visual source and evaluates feedback parameters.
     */
    fun update() {
        source.update()
        fbDecay.evaluate()
        fbGain.evaluate()
        fbZoom.evaluate()
        fbRotate.evaluate()
        fbHueShift.evaluate()
        fbBlur.evaluate()
    }

    /**
     * Disposes all FBOs associated with this Deck.
     */
    fun dispose() {
        cleanFBO.dispose()
        fb1.dispose()
        fb2.dispose()
    }
}
