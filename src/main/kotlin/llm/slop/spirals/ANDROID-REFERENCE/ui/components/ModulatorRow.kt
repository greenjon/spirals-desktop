package llm.slop.spirals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import llm.slop.spirals.R
import llm.slop.spirals.cv.core.CvModulator
import llm.slop.spirals.cv.core.LfoSpeedMode
import llm.slop.spirals.cv.core.ModulationOperator
import llm.slop.spirals.cv.core.ModulationRegistry
import llm.slop.spirals.cv.core.Waveform
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText
import kotlin.math.sin

@Composable
fun ModulatorRow(
    mod: CvModulator?,
    onUpdate: (CvModulator) -> Unit,
    onInteractionFinished: () -> Unit,
    onRemove: () -> Unit
) {
    val isNew = mod == null
    var sourceId by remember(mod) { mutableStateOf(mod?.sourceId ?: "none") }
    var operator by remember(mod) { mutableStateOf(mod?.operator ?: ModulationOperator.ADD) }
    var weight by remember(mod) { mutableFloatStateOf(mod?.weight ?: 0f) }
    var bypassed by remember(mod) { mutableStateOf(mod?.bypassed ?: false) }

    // Beat/LFO fields
    var waveform by remember(mod) { mutableStateOf(mod?.waveform ?: Waveform.SINE) }
    var subdivision by remember(mod) { mutableFloatStateOf(mod?.subdivision ?: 1.0f) }
    var phaseOffset by remember(mod) { mutableFloatStateOf(mod?.phaseOffset ?: 0.0f) }
    var slope by remember(mod) { mutableFloatStateOf(mod?.slope ?: 0.5f) }
    var lfoSpeedMode by remember(mod) { mutableStateOf(mod?.lfoSpeedMode ?: LfoSpeedMode.FAST) }

    val isBeat = sourceId == "beatPhase"
    val isLfo = sourceId == "lfo"
    val isSampleAndHold = sourceId == "sampleAndHold"
    val hasAdvancedControls = isBeat || isLfo || isSampleAndHold
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var pulseValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(sourceId, weight, waveform, subdivision, phaseOffset, slope, lfoSpeedMode) {
        if (sourceId != "none") {
            while(true) {
                val rawCv = when (sourceId) {
                    "beatPhase" -> {
                        val beats = ModulationRegistry.getSynchronizedTotalBeats()
                        val localPhase = ((beats / subdivision.toDouble()) + phaseOffset.toDouble()) % 1.0
                        val positivePhase = if (localPhase < 0) (localPhase + 1.0) else localPhase
                        calculatePreviewWave(waveform, positivePhase, slope)
                    }
                    "lfo" -> {
                        val seconds = ModulationRegistry.getElapsedRealtimeSec()
                        val period = when (lfoSpeedMode) {
                            LfoSpeedMode.FAST -> subdivision * 10.0
                            LfoSpeedMode.MEDIUM -> subdivision * 900.0
                            LfoSpeedMode.SLOW -> subdivision * 86400.0
                        }.coerceAtLeast(0.001)
                        val localPhase = ((seconds / period) + phaseOffset.toDouble()) % 1.0
                        val positivePhase = if (localPhase < 0) (localPhase + 1.0) else localPhase
                        calculatePreviewWave(waveform, positivePhase, slope)
                    }
                    "sampleAndHold" -> {
                        val beats = ModulationRegistry.getSynchronizedTotalBeats()
                        // S&H handles its internal phase and seeds via phaseOffset
                        ModulationRegistry.sampleAndHold.getValue(
                            totalBeats = beats,
                            subdivision = subdivision.toDouble(),
                            phaseOffset = phaseOffset,
                            slope = slope
                        )
                    }
                    else -> ModulationRegistry.get(sourceId)
                }
                pulseValue = rawCv * weight
                delay(16)
            }
        } else {
            pulseValue = 0f
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Modulator", color = AppText) },
            text = { Text("Are you sure you want to remove this modulation source?", color = AppText) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onRemove()
                }) {
                    Text("Remove", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = AppText)
                }
            },
            containerColor = AppBackground
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (bypassed) Modifier.alpha(0.5f) else Modifier)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT PART: CV Name + Controls Below + Wave/Beat
                Column(modifier = Modifier.weight(1.6f)) {
                    // Top Row: CV Name + Add/Mul
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var sourceExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.width(64.dp)) {
                            Text(
                                text = when(sourceId) {
                                    "beatPhase" -> "BEAT"
                                    "sampleAndHold" -> "RANDOM"
                                    else -> sourceId.uppercase()
                                },
                                modifier = Modifier.clickable { sourceExpanded = true }.padding(vertical = 4.dp, horizontal = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppText,
                                maxLines = 1
                            )
                            DropdownMenu(expanded = sourceExpanded, onDismissRequest = { sourceExpanded = false }) {
                                listOf("none", "amp", "bass", "mid", "high", "accent", "beatPhase", "lfo", "sampleAndHold").forEach { s ->
                                    DropdownMenuItem(text = { Text(
                                        when(s) {
                                            "beatPhase" -> "BEAT"
                                            "sampleAndHold" -> "RANDOM"
                                            else -> s.uppercase()
                                        })
                                    }, onClick = {
                                        sourceId = s
                                        // Force Square wave for Sample & Hold
                                        val wave = if (s == "sampleAndHold") Waveform.SQUARE else waveform
                                        // Always update waveform variable for UI consistency
                                        if (s == "sampleAndHold") {
                                            waveform = Waveform.SQUARE
                                        }
                                        if (s != "none") { onUpdate(CvModulator(s, operator, weight, bypassed, wave, subdivision, phaseOffset, slope, lfoSpeedMode)); onInteractionFinished() }
                                        sourceExpanded = false
                                    })
                                }
                            }
                        }

                        if (sourceId != "none") {
                            TextButton(
                                onClick = {
                                    val newOp = if (operator == ModulationOperator.ADD) ModulationOperator.MUL else ModulationOperator.ADD
                                    operator = newOp
                                    if (!isNew) { onUpdate(CvModulator(sourceId, newOp, weight, bypassed, waveform, subdivision, phaseOffset, slope, lfoSpeedMode)); onInteractionFinished() }
                                },
                                modifier = Modifier.height(32.dp).padding(horizontal = 4.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (operator == ModulationOperator.ADD) "ADD" else "MUL", color = AppAccent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Bottom Row: On/Off + Wave/Beat
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isNew) {
                            Surface(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .height(20.dp)
                                    .width(32.dp)
                                    .clickable {
                                        bypassed = !bypassed
                                        onUpdate(CvModulator(sourceId, operator, weight, bypassed, waveform, subdivision, phaseOffset, slope, lfoSpeedMode))
                                        onInteractionFinished()
                                    },
                                color = AppBackground,
                                shape = MaterialTheme.shapes.extraSmall,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (bypassed) AppText else AppAccent)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (bypassed) "OFF" else "ON",
                                        color = AppText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                    )
                                }
                            }
                        } else {
                            if (sourceId == "none") {
                                Spacer(modifier = Modifier.width(36.dp))
                            }
                        }

                        if (hasAdvancedControls) {
                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = {
                                    // Only allow changing waveform if not sampleAndHold
                                    if (!isSampleAndHold) {
                                        val nextWave = Waveform.entries[(waveform.ordinal + 1) % Waveform.entries.size]
                                        waveform = nextWave
                                        onUpdate(CvModulator(sourceId, operator, weight, bypassed, nextWave, subdivision, phaseOffset, slope, lfoSpeedMode))
                                        onInteractionFinished()
                                    }
                                },
                                modifier = Modifier.size(28.dp),
                                enabled = !isSampleAndHold
                            ) {
                                Icon(
                                    painter = painterResource(id = when(waveform) {
                                        Waveform.SINE -> R.drawable.ic_wave_sine
                                        Waveform.TRIANGLE -> R.drawable.ic_wave_triangle
                                        Waveform.SQUARE -> R.drawable.ic_wave_square
                                    }),
                                    contentDescription = "Waveform",
                                    tint = if (isSampleAndHold) AppAccent.copy(alpha = 0.5f) else AppAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (isBeat || isSampleAndHold) {
                                var subExpanded by remember { mutableStateOf(false) }
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.widthIn(min = 36.dp)) {
                                    val subText = when(subdivision) {
                                        0.0625f -> "1/16"
                                        0.125f -> "1/8"
                                        0.25f -> "1/4"
                                        0.5f -> "1/2"
                                        else -> subdivision.toInt().toString()
                                    }
                                    Text(
                                        text = subText,
                                        modifier = Modifier
                                            .clickable { subExpanded = true }
                                            .padding(horizontal = 4.dp),
                                        color = AppAccent,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false
                                    )

                                    DropdownMenu(expanded = subExpanded, onDismissRequest = { subExpanded = false }) {
                                        listOf(0.0625f, 0.125f, 0.25f, 0.5f, 1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f).forEach { sub ->
                                            DropdownMenuItem(text = {
                                                Text(when(sub) {
                                                    0.0625f -> "1/16"
                                                    0.125f -> "1/8"
                                                    0.25f -> "1/4"
                                                    0.5f -> "1/2"
                                                    else -> sub.toInt().toString()
                                                })
                                            }, onClick = {
                                                subdivision = sub
                                                onUpdate(CvModulator(sourceId, operator, weight, bypassed, waveform, sub, phaseOffset, slope, lfoSpeedMode))
                                                onInteractionFinished()
                                                subExpanded = false
                                            })
                                        }
                                    }
                                }
                            } else if (isLfo) {
                                val speedLabel = when(lfoSpeedMode) {
                                    LfoSpeedMode.SLOW -> "Slow"
                                    LfoSpeedMode.MEDIUM -> "Med"
                                    LfoSpeedMode.FAST -> "Fast"
                                }
                                Text(
                                    text = speedLabel,
                                    modifier = Modifier
                                        .clickable {
                                            val nextMode = LfoSpeedMode.entries[(lfoSpeedMode.ordinal + 1) % LfoSpeedMode.entries.size]
                                            lfoSpeedMode = nextMode
                                            onUpdate(CvModulator(sourceId, operator, weight, bypassed, waveform, subdivision, phaseOffset, slope, nextMode))
                                            onInteractionFinished()
                                        }
                                        .padding(horizontal = 4.dp),
                                    color = AppAccent,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                // RIGHT PART: Knobs
                if (sourceId != "none") {
                    // Weight Knob
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        KnobView(
                            baseValue = weight,
                            onValueChange = { newValue ->
                                weight = newValue
                                if (!isNew) onUpdate(CvModulator(sourceId, operator, newValue, bypassed, waveform, subdivision, phaseOffset, slope, lfoSpeedMode))
                            },
                            onInteractionFinished = onInteractionFinished,
                            isBipolar = true,
                            focused = true,
                            knobSize = 44.dp,
                            showValue = true
                        )
                        Text("Weight", style = MaterialTheme.typography.labelSmall, color = AppText)
                    }

                    if (hasAdvancedControls) {
                        // Period/Subdivision Knob
                        if (isLfo) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                KnobView(
                                    baseValue = subdivision,
                                    onValueChange = { newValue ->
                                        subdivision = newValue
                                        if (!isNew) onUpdate(CvModulator(sourceId, operator, weight, bypassed, waveform, newValue, phaseOffset, slope, lfoSpeedMode))
                                    },
                                    onInteractionFinished = onInteractionFinished,
                                    isBipolar = false,
                                    focused = true,
                                    knobSize = 44.dp,
                                    showValue = false,
                                    displayTransform = { it.toString() }
                                )
                                val periodLabel = when (lfoSpeedMode) {
                                    LfoSpeedMode.FAST -> "%.3fs".format(subdivision * 10.0)
                                    LfoSpeedMode.MEDIUM -> {
                                        val totalSec = (subdivision * 900.0).toInt()
                                        "%02dm:%02ds".format(totalSec / 60, totalSec % 60)
                                    }
                                    LfoSpeedMode.SLOW -> {
                                        val totalMin = (subdivision * 1440.0).toInt()
                                        "%02dh:%02dm".format(totalMin / 60, totalMin % 60)
                                    }
                                }
                                Text(text = periodLabel, style = MaterialTheme.typography.labelSmall, color = AppText)
                            }
                        }

                        // Phase Knob
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            KnobView(
                                baseValue = phaseOffset,
                                onValueChange = { newValue ->
                                    phaseOffset = newValue
                                    if (!isNew) onUpdate(CvModulator(sourceId, operator, weight, bypassed, waveform, subdivision, newValue, slope, lfoSpeedMode))
                                },
                                onInteractionFinished = onInteractionFinished,
                                isBipolar = false,
                                focused = true,
                                knobSize = 44.dp,
                                showValue = true
                            )
                            Text("Phase", style = MaterialTheme.typography.labelSmall, color = AppText)
                        }

                        // Slope/Duty Knob
                        val hasSecondSlider = waveform == Waveform.TRIANGLE || waveform == Waveform.SQUARE || isSampleAndHold
                        if (hasSecondSlider) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                KnobView(
                                    baseValue = slope,
                                    onValueChange = { newValue ->
                                        slope = newValue
                                        if (!isNew) onUpdate(CvModulator(sourceId, operator, weight, bypassed, waveform, subdivision, phaseOffset, newValue, lfoSpeedMode))
                                    },
                                    onInteractionFinished = onInteractionFinished,
                                    isBipolar = false,
                                    focused = true,
                                    knobSize = 44.dp,
                                    showValue = true
                                )
                                Text(
                                    text = when {
                                        isSampleAndHold -> "Glide"
                                        waveform == Waveform.TRIANGLE -> "Slope"
                                        else -> "Duty"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppText
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(2.9f))
                    }
                } else {
                    Spacer(modifier = Modifier.weight(3.9f))
                }
            }

            if (sourceId != "none") {
                Box(modifier = Modifier.padding(top = 4.dp).fillMaxWidth().height(1.dp).background(AppText.copy(alpha = 0.2f))) {
                    val displayValue = pulseValue.coerceIn(-1f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(kotlin.math.abs(displayValue))
                            .fillMaxHeight()
                            .align(if (displayValue >= 0) Alignment.CenterStart else Alignment.CenterEnd)
                            .background(if (displayValue >= 0) AppAccent else Color.Red.copy(alpha = 0.7f))
                    )
                }
            }
        }

        if (!isNew) {
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = AppText.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun calculatePreviewWave(waveform: Waveform, phase: Double, slope: Float): Float {
    return when(waveform) {
        Waveform.SINE -> (sin(phase * 2.0 * Math.PI).toFloat() * 0.5f) + 0.5f
        Waveform.TRIANGLE -> {
            val s = slope.toDouble()
            if (s <= 0.001) (1.0 - phase).toFloat()
            else if (s >= 0.999) phase.toFloat()
            else if (phase < s) (phase / s).toFloat()
            else ((1.0 - phase) / (1.0 - s)).toFloat()
        }
        Waveform.SQUARE -> if (phase < slope) 1.0f else 0.0f
    }
}
