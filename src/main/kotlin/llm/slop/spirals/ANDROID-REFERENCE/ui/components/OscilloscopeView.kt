package llm.slop.spirals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import llm.slop.spirals.cv.visualizers.CvHistoryBuffer

@Composable
fun OscilloscopeView(
    history: CvHistoryBuffer,
    isUnipolar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val historySize = history.size
    val localSamples = remember(historySize) { FloatArray(historySize) }
    val sharedPath = remember { Path() }

    // Use a frame clock to trigger redraws at the display refresh rate
    var frameTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTime = it }
        }
    }

    // Use Spacer with drawBehind for zero-allocation rendering
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .drawBehind {
                // Read frameTime to invalidate the draw scope every frame
                val _trigger = frameTime
                
                val width = size.width
                val height = size.height
                
                // 1. Pull data directly from history (primitives only)
                history.copyTo(localSamples)

                // 2. Draw Baseline
                val baselineY = if (isUnipolar) height - 2f else height / 2f
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(0f, baselineY),
                    end = Offset(width, baselineY),
                    strokeWidth = 1f
                )

                if (localSamples.isEmpty()) return@drawBehind

                // 3. Rebuild the shared path
                sharedPath.reset()
                val stepX = width / (localSamples.size - 1)

                for (i in localSamples.indices) {
                    val x = i * stepX
                    val sample = localSamples[i]
                    
                    // Handle values that might exceed the 0-1 range (like audio peaks)
                    val displayValue = if (sample > 1.0f) {
                        0.5f + (sample / 16.0f) 
                    } else if (sample < 0f && isUnipolar) {
                        0f
                    } else {
                        sample
                    }

                    val y = if (isUnipolar) {
                        height - (displayValue.coerceIn(0f, 1f) * height)
                    } else {
                        val centerY = height / 2f
                        centerY - (displayValue.coerceIn(-1f, 1f) * centerY)
                    }
                    
                    if (i == 0) sharedPath.moveTo(x, y) else sharedPath.lineTo(x, y)
                }

                // 4. Final Draw (Zero object creation)
                drawPath(
                    path = sharedPath,
                    color = Color.Green,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
    )
}
