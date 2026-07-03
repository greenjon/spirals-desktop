package llm.slop.spirals.rendering

import llm.slop.spirals.parameters.ModulatableParameter
import llm.slop.spirals.parameters.MeterType

/**
 * Manages the blending of two Decks (Deck A and Deck B) into a master output FBO.
 * Provides controls for crossfade, master alpha, and blending mode.
 */
class Mixer(
    val deckA: Deck,
    val deckB: Deck,
    val deckC: Deck,
    val width: Int = 1920,
    val height: Int = 1080
) {
    // The master FBO where the blended result is rendered
    val masterFBO = FBO(width, height)

    // Blend parameters
    val crossfade = ModulatableParameter(0.0f, meterType = MeterType.BIPOLAR) // 0.0 = Deck A, 1.0 = Deck B
    val mode = ModulatableParameter(4.0f) // 0 = ADD, 1 = SCREEN, 2 = MULT, 3 = MAX, 4 = XFADE
    val masterAlpha = ModulatableParameter(1.0f) // Master output gain
    val bloom = ModulatableParameter(0.0f, minClamp = 0f, maxClamp = 1f)
    val xfadeSpeed = ModulatableParameter(0.1f, minClamp = 0.001f, maxClamp = 1.0f)

    @Volatile var targetCrossfade = 0.0f
    var isAutoFading = false

    val setlistPrev = ModulatableParameter(0.0f, minClamp = 0f, maxClamp = 1f)
    val setlistNext = ModulatableParameter(0.0f, minClamp = 0f, maxClamp = 1f)

    private var prevSetlistPrevVal = 0.0f
    private var prevSetlistNextVal = 0.0f

    /**
     * Evaluates mixer parameters.
     */
    fun update() {
        if (isAutoFading) {
            val current = crossfade.baseValue
            if (kotlin.math.abs(current - targetCrossfade) < 0.001f) {
                crossfade.baseValue = targetCrossfade
                isAutoFading = false
            } else {
                val step = xfadeSpeed.value * 0.05f // scale speed to a reasonable per-frame delta
                if (current < targetCrossfade) {
                    crossfade.baseValue = (current + step).coerceAtMost(targetCrossfade)
                } else {
                    crossfade.baseValue = (current - step).coerceAtLeast(targetCrossfade)
                }
            }
        }

        crossfade.evaluate()
        mode.evaluate()
        masterAlpha.evaluate()
        bloom.evaluate()
        xfadeSpeed.evaluate()
        setlistPrev.evaluate()
        setlistNext.evaluate()
    }

    /**
     * Evaluates if either parameter crossed the 0.5 threshold since the last frame.
     * Returns +1 if setlistNext was triggered, -1 if setlistPrev was triggered, or 0.
     */
    fun pollSetlistAdvance(): Int {
        val nextVal = setlistNext.value
        val prevVal = setlistPrev.value

        var delta = 0
        if (prevSetlistNextVal < 0.5f && nextVal >= 0.5f) {
            delta += 1
        }
        if (prevSetlistPrevVal < 0.5f && prevVal >= 0.5f) {
            delta -= 1
        }

        prevSetlistNextVal = nextVal
        prevSetlistPrevVal = prevVal
        return delta
    }

    /**
     * Disposes the master FBO.
     */
    fun dispose() {
        masterFBO.dispose()
        deckC.dispose()
    }
}
