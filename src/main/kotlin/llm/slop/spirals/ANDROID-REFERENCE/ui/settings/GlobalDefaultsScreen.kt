package llm.slop.spirals.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import llm.slop.spirals.defaults.ArmDefaults
import llm.slop.spirals.defaults.DefaultsConfig
import llm.slop.spirals.defaults.HueOffsetDefaults
import llm.slop.spirals.defaults.RotationDefaults
import llm.slop.spirals.models.STANDARD_BEAT_VALUES
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText

/**
 * Screen for configuring global default values used by randomizers
 * when specific parameters are unconfigured.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalDefaultsScreen(
    defaultsConfig: DefaultsConfig,
    onClose: () -> Unit
) {
    var armDefaults by remember { mutableStateOf(defaultsConfig.getArmDefaults()) }
    var rotationDefaults by remember { mutableStateOf(defaultsConfig.getRotationDefaults()) }
    var hueOffsetDefaults by remember { mutableStateOf(defaultsConfig.getHueOffsetDefaults()) }
    
    val formatBeatValue = { value: Float ->
        when {
            value < 1 -> "1/${(1/value).toInt()}"
            else -> value.toInt().toString()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Randomization Defaults",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppText
                )
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                ) {
                    Text("Save & Close")
                }
            }
            
            Text(
                text = "These settings control the default behavior when parameters are not explicitly configured.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppText.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Random Set Editor Section
            ExpandableSection(
                title = "Random Set Editor",
                initialExpanded = true
            ) {
                // Arms Section
                ExpandableSection(
                    title = "Arm Parameters (L1-L4)",
                    titleStyle = MaterialTheme.typography.titleMedium,
                    initialExpanded = true
                ) {
                    Text(
                        text = "When arm constraints are not configured, these settings will be used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Base Length Range
                    Text(
                        text = "Base Length Range: ${armDefaults.baseLengthMin}%-${armDefaults.baseLengthMax}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RangeSlider(
                        value = armDefaults.baseLengthMin.toFloat()..armDefaults.baseLengthMax.toFloat(),
                        onValueChange = { range ->
                            armDefaults = armDefaults.copy(
                                baseLengthMin = range.start.toInt(),
                                baseLengthMax = range.endInclusive.toInt()
                            )
                            defaultsConfig.saveArmDefaults(armDefaults)
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Movement Source Probabilities
                    Text(
                        text = "Movement Source Probabilities",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Beat: ${(armDefaults.beatProbability * 100).toInt()}%  LFO: ${(armDefaults.lfoProbability * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = armDefaults.beatProbability,
                        onValueChange = { 
                            armDefaults = armDefaults.copy(
                                beatProbability = it,
                                lfoProbability = 1 - it
                            )
                            defaultsConfig.saveArmDefaults(armDefaults)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Beat Division Range
                    val beatValues = STANDARD_BEAT_VALUES
                    val minValueIndex = beatValues.indexOfFirst { it >= armDefaults.beatDivMin }.coerceAtLeast(0)
                    val maxValueIndex = beatValues.indexOfLast { it <= armDefaults.beatDivMax }.coerceAtMost(beatValues.lastIndex)
                    
                    Text(
                        text = "Beat Division Range: ${formatBeatValue(beatValues[minValueIndex])}-${formatBeatValue(beatValues[maxValueIndex])}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    // Labels for min and max
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatBeatValue(beatValues[minValueIndex]),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatBeatValue(beatValues[maxValueIndex]),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Dual slider
                    RangeSlider(
                        value = minValueIndex.toFloat()..maxValueIndex.toFloat(),
                        onValueChange = { range ->
                            val newMin = beatValues[range.start.toInt()]
                            val newMax = beatValues[range.endInclusive.toInt()]
                            armDefaults = armDefaults.copy(
                                beatDivMin = newMin,
                                beatDivMax = newMax
                            )
                            defaultsConfig.saveArmDefaults(armDefaults)
                        },
                        valueRange = 0f..(beatValues.size - 1).toFloat(),
                        steps = beatValues.size - 2,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Waveform Probabilities
                    Text(
                        text = "Waveform Probabilities",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Text(
                        text = "Sine: ${(armDefaults.sineProbability * 100).toInt()}%  Triangle: ${(armDefaults.triangleProbability * 100).toInt()}%  Square: ${(armDefaults.squareProbability * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sine",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppText.copy(alpha = 0.7f)
                            )
                            Slider(
                                value = armDefaults.sineProbability,
                                onValueChange = { 
                                    val total = it + armDefaults.triangleProbability + armDefaults.squareProbability
                                    val normalizedValue = it / total
                                    val normalizedTriangle = armDefaults.triangleProbability / total
                                    val normalizedSquare = armDefaults.squareProbability / total
                                    
                                    armDefaults = armDefaults.copy(
                                        sineProbability = normalizedValue,
                                        triangleProbability = normalizedTriangle,
                                        squareProbability = normalizedSquare
                                    )
                                    defaultsConfig.saveArmDefaults(armDefaults)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AppAccent,
                                    activeTrackColor = AppAccent
                                )
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Triangle",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppText.copy(alpha = 0.7f)
                            )
                            Slider(
                                value = armDefaults.triangleProbability,
                                onValueChange = { 
                                    val total = armDefaults.sineProbability + it + armDefaults.squareProbability
                                    val normalizedSine = armDefaults.sineProbability / total
                                    val normalizedValue = it / total
                                    val normalizedSquare = armDefaults.squareProbability / total
                                    
                                    armDefaults = armDefaults.copy(
                                        sineProbability = normalizedSine,
                                        triangleProbability = normalizedValue,
                                        squareProbability = normalizedSquare
                                    )
                                    defaultsConfig.saveArmDefaults(armDefaults)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AppAccent,
                                    activeTrackColor = AppAccent
                                )
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Square",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppText.copy(alpha = 0.7f)
                            )
                            Slider(
                                value = armDefaults.squareProbability,
                                onValueChange = { 
                                    val total = armDefaults.sineProbability + armDefaults.triangleProbability + it
                                    val normalizedSine = armDefaults.sineProbability / total
                                    val normalizedTriangle = armDefaults.triangleProbability / total
                                    val normalizedValue = it / total
                                    
                                    armDefaults = armDefaults.copy(
                                        sineProbability = normalizedSine,
                                        triangleProbability = normalizedTriangle,
                                        squareProbability = normalizedValue
                                    )
                                    defaultsConfig.saveArmDefaults(armDefaults)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AppAccent,
                                    activeTrackColor = AppAccent
                                )
                            )
                        }
                    }
                    
                    // Weight Range
                    Text(
                        text = "Weight Range: ${armDefaults.weightMin}% to ${armDefaults.weightMax}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RangeSlider(
                        value = armDefaults.weightMin.toFloat()..armDefaults.weightMax.toFloat(),
                        onValueChange = { range ->
                            armDefaults = armDefaults.copy(
                                weightMin = range.start.toInt(),
                                weightMax = range.endInclusive.toInt()
                            )
                            defaultsConfig.saveArmDefaults(armDefaults)
                        },
                        valueRange = -100f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // LFO Time Range
                    Text(
                        text = "LFO Time Range: ${armDefaults.lfoTimeMin.toInt()}s-${armDefaults.lfoTimeMax.toInt()}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RangeSlider(
                        value = armDefaults.lfoTimeMin..armDefaults.lfoTimeMax,
                        onValueChange = { range ->
                            armDefaults = armDefaults.copy(
                                lfoTimeMin = range.start,
                                lfoTimeMax = range.endInclusive
                            )
                            defaultsConfig.saveArmDefaults(armDefaults)
                        },
                        valueRange = 1f..600f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Reset to defaults button
                    OutlinedButton(
                        onClick = { 
                            defaultsConfig.resetArmDefaults()
                            armDefaults = defaultsConfig.getArmDefaults()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset to Defaults")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Rotation Section
                ExpandableSection(
                    title = "Rotation Parameters",
                    titleStyle = MaterialTheme.typography.titleMedium,
                    initialExpanded = false
                ) {
                    Text(
                        text = "When rotation constraints are not configured, these settings will be used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Direction Probabilities
                    Text(
                        text = "Direction Probabilities",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Clockwise: ${(rotationDefaults.clockwiseProbability * 100).toInt()}%  Counter-clockwise: ${(rotationDefaults.counterClockwiseProbability * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = rotationDefaults.clockwiseProbability,
                        onValueChange = { 
                            rotationDefaults = rotationDefaults.copy(
                                clockwiseProbability = it,
                                counterClockwiseProbability = 1 - it
                            )
                            defaultsConfig.saveRotationDefaults(rotationDefaults)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Speed Source Probabilities
                    Text(
                        text = "Speed Source Probabilities",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Beat: ${(rotationDefaults.beatProbability * 100).toInt()}%  LFO: ${(rotationDefaults.lfoProbability * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = rotationDefaults.beatProbability,
                        onValueChange = { 
                            rotationDefaults = rotationDefaults.copy(
                                beatProbability = it,
                                lfoProbability = 1 - it
                            )
                            defaultsConfig.saveRotationDefaults(rotationDefaults)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Beat Division Range
                    val beatValues = STANDARD_BEAT_VALUES
                    val minValueIndex = beatValues.indexOfFirst { it >= rotationDefaults.beatDivMin }.coerceAtLeast(0)
                    val maxValueIndex = beatValues.indexOfLast { it <= rotationDefaults.beatDivMax }.coerceAtMost(beatValues.lastIndex)
                    
                    Text(
                        text = "Beat Division Range: ${formatBeatValue(beatValues[minValueIndex])}-${formatBeatValue(beatValues[maxValueIndex])}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    // Labels for min and max
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatBeatValue(beatValues[minValueIndex]),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatBeatValue(beatValues[maxValueIndex]),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Dual slider
                    RangeSlider(
                        value = minValueIndex.toFloat()..maxValueIndex.toFloat(),
                        onValueChange = { range ->
                            val newMin = beatValues[range.start.toInt()]
                            val newMax = beatValues[range.endInclusive.toInt()]
                            rotationDefaults = rotationDefaults.copy(
                                beatDivMin = newMin,
                                beatDivMax = newMax
                            )
                            defaultsConfig.saveRotationDefaults(rotationDefaults)
                        },
                        valueRange = 0f..(beatValues.size - 1).toFloat(),
                        steps = beatValues.size - 2,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // LFO Time Range
                    Text(
                        text = "LFO Time Range: ${rotationDefaults.lfoTimeMin.toInt()}s-${rotationDefaults.lfoTimeMax.toInt()}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RangeSlider(
                        value = rotationDefaults.lfoTimeMin..rotationDefaults.lfoTimeMax,
                        onValueChange = { range ->
                            rotationDefaults = rotationDefaults.copy(
                                lfoTimeMin = range.start,
                                lfoTimeMax = range.endInclusive
                            )
                            defaultsConfig.saveRotationDefaults(rotationDefaults)
                        },
                        valueRange = 1f..600f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Reset to defaults button
                    OutlinedButton(
                        onClick = { 
                            defaultsConfig.resetRotationDefaults()
                            rotationDefaults = defaultsConfig.getRotationDefaults()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset to Defaults")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Hue Offset Section
                ExpandableSection(
                    title = "Color Parameters",
                    titleStyle = MaterialTheme.typography.titleMedium,
                    initialExpanded = false
                ) {
                    Text(
                        text = "When hue offset constraints are not configured, these settings will be used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Direction Probabilities
                    Text(
                        text = "Direction Probabilities",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Forward: ${(hueOffsetDefaults.forwardProbability * 100).toInt()}%  Reverse: ${(hueOffsetDefaults.reverseProbability * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = hueOffsetDefaults.forwardProbability,
                        onValueChange = { 
                            hueOffsetDefaults = hueOffsetDefaults.copy(
                                forwardProbability = it,
                                reverseProbability = 1 - it
                            )
                            defaultsConfig.saveHueOffsetDefaults(hueOffsetDefaults)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Speed Source Probabilities
                    Text(
                        text = "Speed Source Probabilities",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Beat: ${(hueOffsetDefaults.beatProbability * 100).toInt()}%  LFO: ${(hueOffsetDefaults.lfoProbability * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = hueOffsetDefaults.beatProbability,
                        onValueChange = { 
                            hueOffsetDefaults = hueOffsetDefaults.copy(
                                beatProbability = it,
                                lfoProbability = 1 - it
                            )
                            defaultsConfig.saveHueOffsetDefaults(hueOffsetDefaults)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Beat Division Range
                    val beatValues = STANDARD_BEAT_VALUES
                    val minValueIndex = beatValues.indexOfFirst { it >= hueOffsetDefaults.beatDivMin }.coerceAtLeast(0)
                    val maxValueIndex = beatValues.indexOfLast { it <= hueOffsetDefaults.beatDivMax }.coerceAtMost(beatValues.lastIndex)
                    
                    Text(
                        text = "Beat Division Range: ${formatBeatValue(beatValues[minValueIndex])}-${formatBeatValue(beatValues[maxValueIndex])}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    // Labels for min and max
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatBeatValue(beatValues[minValueIndex]),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatBeatValue(beatValues[maxValueIndex]),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Dual slider
                    RangeSlider(
                        value = minValueIndex.toFloat()..maxValueIndex.toFloat(),
                        onValueChange = { range ->
                            val newMin = beatValues[range.start.toInt()]
                            val newMax = beatValues[range.endInclusive.toInt()]
                            hueOffsetDefaults = hueOffsetDefaults.copy(
                                beatDivMin = newMin,
                                beatDivMax = newMax
                            )
                            defaultsConfig.saveHueOffsetDefaults(hueOffsetDefaults)
                        },
                        valueRange = 0f..(beatValues.size - 1).toFloat(),
                        steps = beatValues.size - 2,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // LFO Time Range
                    Text(
                        text = "LFO Time Range: ${hueOffsetDefaults.lfoTimeMin.toInt()}s-${hueOffsetDefaults.lfoTimeMax.toInt()}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RangeSlider(
                        value = hueOffsetDefaults.lfoTimeMin..hueOffsetDefaults.lfoTimeMax,
                        onValueChange = { range ->
                            hueOffsetDefaults = hueOffsetDefaults.copy(
                                lfoTimeMin = range.start,
                                lfoTimeMax = range.endInclusive
                            )
                            defaultsConfig.saveHueOffsetDefaults(hueOffsetDefaults)
                        },
                        valueRange = 1f..600f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Reset to defaults button
                    OutlinedButton(
                        onClick = { 
                            defaultsConfig.resetHueOffsetDefaults()
                            hueOffsetDefaults = defaultsConfig.getHueOffsetDefaults()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset to Defaults")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Reset All button
                Button(
                    onClick = { 
                        defaultsConfig.resetAllDefaults()
                        armDefaults = defaultsConfig.getArmDefaults()
                        rotationDefaults = defaultsConfig.getRotationDefaults()
                        hueOffsetDefaults = defaultsConfig.getHueOffsetDefaults()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                ) {
                    Text("Reset All to Factory Defaults")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Future: Mandala Editor Defaults (coming soon)",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppText.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    initialExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = titleStyle,
                color = AppText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = AppText
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                color = AppBackground.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    content = content
                )
            }
        }
    }
}