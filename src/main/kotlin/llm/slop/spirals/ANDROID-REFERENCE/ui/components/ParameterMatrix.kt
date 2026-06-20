package llm.slop.spirals.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import llm.slop.spirals.cv.core.ModulatableParameter
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppText
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MandalaParameterMatrix(
    labels: List<String>,
    parameters: List<ModulatableParameter>,
    focusedParameterId: String?,
    onFocusRequest: (String) -> Unit,
    onInteractionFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Frame tick to force recomposition so the live modulated arcs animate
    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTick = it }
        }
    }

    // Row 1: Primary Geometry (Arms + Scale)
    val row1Ids = listOf("L1", "L2", "L3", "L4", "Scale")
    val row1Params = remember(labels, parameters) {
        row1Ids.mapNotNull { id ->
            val idx = labels.indexOf(id)
            if (idx != -1) id to parameters[idx] else null
        }
    }

    // Row 2: Secondary Geometry & Color
    val row2Ids = listOf("Rotation", "Thickness", "Hue Offset", "Hue Sweep", "Depth")
    val row2Params = remember(labels, parameters) {
        row2Ids.mapNotNull { id ->
            val idx = labels.indexOf(id)
            if (idx != -1) id to parameters[idx] else null
        }
    }

    // Row 3: Snapshot & Trails
    val row3Ids = listOf("Trails", "Snap Count", "Snap Mode", "Snap Blend", "Snap Trigger")
    val row3Params = remember(labels, parameters) {
        row3Ids.mapNotNull { id ->
            val idx = labels.indexOf(id)
            if (idx != -1) id to parameters[idx] else null
        }
    }

    // Row 4: Feedback Engine
    val row4Ids = listOf("FB Decay", "FB Gain", "FB Zoom", "FB Rotate", "FB Shift", "FB Blur")
    val row4Params = remember(labels, parameters) {
        row4Ids.mapNotNull { id ->
            val idx = labels.indexOf(id)
            if (idx != -1) id to parameters[idx] else null
        }
    }

    // Capture any parameters not explicitly grouped above
    val handledIds = row1Ids + row2Ids + row3Ids + row4Ids
    val extraParams = remember(labels, parameters) {
        labels.indices
            .filter { !handledIds.contains(labels[it]) }
            .map { labels[it] to parameters[it] }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ROW 1
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row1Params.forEach { (id, param) ->
                KnobCell(
                    id = id,
                    param = param,
                    isFocused = id == focusedParameterId,
                    onFocusRequest = onFocusRequest,
                    onInteractionFinished = onInteractionFinished,
                    labelAbove = true,
                    tick = frameTick
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ROW 2
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row2Params.forEach { (id, param) ->
                KnobCell(
                    id = id,
                    param = param,
                    isFocused = id == focusedParameterId,
                    onFocusRequest = onFocusRequest,
                    onInteractionFinished = onInteractionFinished,
                    labelAbove = false,
                    tick = frameTick
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ROW 3
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row3Params.forEach { (id, param) ->
                KnobCell(
                    id = id,
                    param = param,
                    isFocused = id == focusedParameterId,
                    onFocusRequest = onFocusRequest,
                    onInteractionFinished = onInteractionFinished,
                    labelAbove = false,
                    tick = frameTick
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ROW 4: Feedback
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row4Params.forEach { (id, param) ->
                KnobCell(
                    id = id,
                    param = param,
                    isFocused = id == focusedParameterId,
                    onFocusRequest = onFocusRequest,
                    onInteractionFinished = onInteractionFinished,
                    labelAbove = false,
                    tick = frameTick
                )
            }
        }

        if (extraParams.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                extraParams.forEach { (id, param) ->
                    KnobCell(
                        id = id,
                        param = param,
                        isFocused = id == focusedParameterId,
                        onFocusRequest = onFocusRequest,
                        onInteractionFinished = onInteractionFinished,
                        labelAbove = false,
                        tick = frameTick
                    )
                }
            }
        }
    }
}

@Composable
private fun KnobCell(
    id: String,
    param: ModulatableParameter,
    isFocused: Boolean,
    onFocusRequest: (String) -> Unit,
    onInteractionFinished: () -> Unit,
    labelAbove: Boolean,
    tick: Long,
    modifier: Modifier = Modifier
) {
    var localBaseValue by remember(param) { mutableFloatStateOf(param.baseValue) }
    val currentOnFocusRequest by rememberUpdatedState(onFocusRequest)

    // Sync if model changes (e.g. preset load)
    LaunchedEffect(param.baseValue) {
        localBaseValue = param.baseValue
    }

    // Capture the live value. Reading param.value here, inside a Composable 
    // that receives 'tick', ensures this block is sensitive to the frame updates.
    val liveModulatedValue = param.value

    Box(
        modifier = modifier
            .padding(2.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { currentOnFocusRequest(id) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (labelAbove) {
                Text(
                    text = id,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFocused) AppAccent else AppText,
                    maxLines = 1
                )
            }
            KnobView(
                baseValue = localBaseValue,
                modulatedValue = liveModulatedValue,
                onValueChange = { newValue ->
                    localBaseValue = newValue
                    param.baseValue = newValue
                    currentOnFocusRequest(id)
                },
                onInteractionFinished = onInteractionFinished,
                modifier = Modifier.padding(vertical = 1.dp),
                isBipolar = false,
                focused = isFocused,
                knobSize = 44.dp,
                showValue = true,
                tick = tick,
                displayTransform = { v -> 
                    // Use the passed baseValue (v) for the text display
                    when (id) {
                        "Hue Sweep" -> (v * 9.0f).roundToInt().toString()
                        "Scale" -> "%.2f".format(v * 8.0f)
                        "Snap Count" -> (v * 14f + 2f).roundToInt().toString()
                        "Snap Mode" -> if (v < 0.5f) "BHND" else "ABOV"
                        "Snap Blend" -> if (v < 0.5f) "NORM" else "ADD"
                        "FB Zoom" -> "%.1f%%".format((v - 0.5f) * 10f)
                        "FB Rotate" -> "%.1f°".format((v - 0.5f) * 10f)
                        "FB Shift" -> "%.0f°".format(v * 360f)
                        else -> (v * 100f).roundToInt().toString()
                    }
                }
            )
            if (!labelAbove) {
                Text(
                    text = id,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFocused) AppAccent else AppText,
                    maxLines = 1
                )
            }
        }
    }
}
