package llm.slop.spirals.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * A custom modifier that implements rotary knob logic via vertical dragging.
 * Uses rememberUpdatedState to ensure that stale lambdas don't cause state resetting.
 */
fun Modifier.knobInput(
    value: Float,
    config: KnobConfig,
    onValueChange: (Float) -> Unit,
    onInteractionFinished: () -> Unit = {}
): Modifier = composed {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnInteractionFinished by rememberUpdatedState(onInteractionFinished)

    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val startY = down.position.y
            var lastY = down.position.y
            var lastTime = System.currentTimeMillis()
            var smoothedVelocity = 0f
            var currentValue = value
            var isDragging = false
            val deadzone = 10f

            while (true) {
                val event: PointerEvent = awaitPointerEvent()
                val type = event.type

                if (type == PointerEventType.Move) {
                    val currentY = event.changes.first().position.y
                    val currentTime = System.currentTimeMillis()
                    
                    if (!isDragging) {
                        if (abs(currentY - startY) > deadzone) {
                            isDragging = true
                            // Implement 10px vertical slop subtract offset to prevent jump
                            val sign = if (currentY > startY) 1 else -1
                            lastY = startY + (deadzone * sign)
                            lastTime = currentTime
                        }
                    }

                    if (isDragging) {
                        val deltaY = currentY - lastY
                        val deltaTimeSec = (currentTime - lastTime) / 1000f
                        
                        val (nextValue, newVelocity) = RotaryKnobMath.computeNewValue(
                            deltaY = deltaY,
                            deltaTimeSec = deltaTimeSec,
                            currentValue = currentValue,
                            prevVelocity = smoothedVelocity,
                            config = config
                        )
                        
                        currentValue = nextValue
                        smoothedVelocity = newVelocity
                        currentOnValueChange(nextValue)
                        
                        lastY = currentY
                        lastTime = currentTime
                    }
                    event.changes.first().consume()
                } else if (type == PointerEventType.Release || type == PointerEventType.Exit) {
                    currentOnInteractionFinished()
                    break
                }
            }
        }
    }
}
