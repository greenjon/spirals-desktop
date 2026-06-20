package llm.slop.spirals.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons 
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import llm.slop.spirals.defaults.*
import llm.slop.spirals.models.STANDARD_BEAT_VALUES
import llm.slop.spirals.ui.components.KnobConfig
import llm.slop.spirals.ui.components.KnobView
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText

// Extension function to convert float to percentage
private fun Float.toPercent(): Int = (this * 100).toInt()

/**
 * A single knob for probability control
 */
@Composable
private fun ProbabilityKnob(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = AppText.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        KnobView(
            baseValue = value,
            onValueChange = onValueChange,
            onInteractionFinished = {},
            knobSize = 45.dp,
            showValue = true,
            displayTransform = { "${(it * 100).toInt()}%" }
        )
    }
}

/**
 * Screen for configuring default values used by randomization throughout the app.
 * These settings are used when creating new mandalas or when specific parameters
 * are not explicitly configured in Random Sets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandalaDefaultsScreen(
    defaultsConfig: DefaultsConfig,
    onClose: () -> Unit
) {
    // Default values state
    var armDefaults by remember { mutableStateOf(defaultsConfig.getArmDefaults()) }
    var rotationDefaults by remember { mutableStateOf(defaultsConfig.getRotationDefaults()) }
    var hueOffsetDefaults by remember { mutableStateOf(defaultsConfig.getHueOffsetDefaults()) }
    var recipeDefaults by remember { mutableStateOf(defaultsConfig.getRecipeDefaults()) }
    var feedbackDefaults by remember { mutableStateOf(defaultsConfig.getFeedbackDefaults()) }
    
    // Format beat values helper
    val formatBeatValue = { value: Float ->
        when {
            value < 1 -> "1/${(1/value).toInt()}"
            else -> value.toInt().toString()
        }
    }
    
    // Settings group options
    val RECIPES = 0
    val ARMS = 1
    val ROTATION = 2
    val COLOR = 3
    val FEEDBACK = 4
    
    // State for the currently selected settings group
    var selectedGroup by remember { mutableStateOf(RECIPES) }
    
    // Get display name for group
    val getGroupName = { group: Int ->
        when (group) {
            RECIPES -> "Recipes"
            ARMS -> "Arms"
            ROTATION -> "Rotation" 
            COLOR -> "Color"
            FEEDBACK -> "Feedback"
            else -> "Unknown"
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
            // Compact header with title and save icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mandala Defaults",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppText
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Save and Close",
                        tint = AppAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Text(
                text = "These settings control the default behavior when randomizing mandalas.",
                style = MaterialTheme.typography.bodySmall,
                color = AppText.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Group dropdown selector
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                TextField(
                    value = getGroupName(selectedGroup),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = AppBackground,
                        focusedContainerColor = AppBackground,
                        unfocusedTextColor = AppText,
                        focusedTextColor = AppText,
                        unfocusedIndicatorColor = AppText.copy(alpha = 0.3f),
                        focusedIndicatorColor = AppAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(AppBackground)
                ) {
                    listOf(RECIPES, ARMS, ROTATION, COLOR, FEEDBACK).forEach { group ->
                        DropdownMenuItem(
                            text = { Text(getGroupName(group)) },
                            onClick = {
                                selectedGroup = group
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Content based on selected group
            when (selectedGroup) {
                RECIPES -> {
                    // Recipe Settings
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Prefer Favorites
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = recipeDefaults.preferFavorites,
                                onCheckedChange = { 
                                    recipeDefaults = recipeDefaults.copy(preferFavorites = it)
                                    defaultsConfig.saveRecipeDefaults(recipeDefaults)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AppAccent
                                )
                            )
                            Text(
                                text = "Prefer favorite recipes when randomizing",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppText,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        // Petal Count Range
                        Text(
                            text = "Petal Count Range: ${recipeDefaults.minPetalCount}-${recipeDefaults.maxPetalCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                        
                        RangeSlider(
                            value = recipeDefaults.minPetalCount.toFloat()..recipeDefaults.maxPetalCount.toFloat(),
                            onValueChange = { range ->
                                recipeDefaults = recipeDefaults.copy(
                                    minPetalCount = range.start.toInt(),
                                    maxPetalCount = range.endInclusive.toInt()
                                )
                                defaultsConfig.saveRecipeDefaults(recipeDefaults)
                            },
                            valueRange = 3f..24f,
                            steps = 20,
                            colors = SliderDefaults.colors(
                                thumbColor = AppAccent,
                                activeTrackColor = AppAccent
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Auto Hue Sweep
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = recipeDefaults.autoHueSweep,
                                onCheckedChange = { 
                                    recipeDefaults = recipeDefaults.copy(autoHueSweep = it)
                                    defaultsConfig.saveRecipeDefaults(recipeDefaults)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AppAccent
                                )
                            )
                            Text(
                                text = "Auto-set Hue Sweep based on petal count",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppText,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                ARMS -> {
                    // Arms Settings
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "These settings control arm behavior when randomizing mandalas.",
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
                        
                        // Default Enable Random (RSet)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = armDefaults.defaultEnableRandom,
                                onCheckedChange = { 
                                    armDefaults = armDefaults.copy(defaultEnableRandom = it)
                                    defaultsConfig.saveArmDefaults(armDefaults)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text(
                                text = "Enable Random (S&H) by default in templates",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppText,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        // Movement Source Probabilities
                        Text(
                            text = "Movement Source Probabilities",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        // Three knobs for CV sources
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProbabilityKnob(
                                title = "Beat",
                                value = armDefaults.beatProbability,
                                onValueChange = { 
                                    // Adjust all three probabilities directly
                                    val newBeatProb = it.coerceIn(0f, 1f)
                                    
                                    // If Beat is 100%, set others to 0
                                    val (newLfoProb, newRandomProb) = if (newBeatProb >= 0.999f) {
                                        Pair(0f, 0f)
                                    } else {
                                        // Otherwise, redistribute remaining probability
                                        val remainingProb = (1.0f - newBeatProb).coerceIn(0f, 1f)
                                        val oldRatio = if (armDefaults.lfoProbability + armDefaults.randomProbability > 0) {
                                            armDefaults.lfoProbability / (armDefaults.lfoProbability + armDefaults.randomProbability)
                                        } else {
                                            0.5f // Default to equal distribution
                                        }
                                        
                                        Pair(
                                            remainingProb * oldRatio,
                                            remainingProb * (1 - oldRatio)
                                        )
                                    }
                                    
                                    // Update and save
                                    armDefaults = armDefaults.copy(
                                        beatProbability = newBeatProb,
                                        lfoProbability = newLfoProb,
                                        randomProbability = newRandomProb
                                    )
                                    defaultsConfig.saveArmDefaults(armDefaults)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            ProbabilityKnob(
                                title = "LFO",
                                value = armDefaults.lfoProbability,
                                onValueChange = { 
                                    // Adjust all three probabilities directly
                                    val newLfoProb = it.coerceIn(0f, 1f)
                                    
                                    // If LFO is 100%, set others to 0
                                    val (newBeatProb, newRandomProb) = if (newLfoProb >= 0.999f) {
                                        Pair(0f, 0f)
                                    } else {
                                        // Otherwise, redistribute remaining probability
                                        val remainingProb = (1.0f - newLfoProb).coerceIn(0f, 1f)
                                        val oldRatio = if (armDefaults.beatProbability + armDefaults.randomProbability > 0) {
                                            armDefaults.beatProbability / (armDefaults.beatProbability + armDefaults.randomProbability)
                                        } else {
                                            0.5f // Default to equal distribution
                                        }
                                        
                                        Pair(
                                            remainingProb * oldRatio,
                                            remainingProb * (1 - oldRatio)
                                        )
                                    }
                                    
                                    // Update and save
                                    armDefaults = armDefaults.copy(
                                        beatProbability = newBeatProb,
                                        lfoProbability = newLfoProb,
                                        randomProbability = newRandomProb
                                    )
                                    defaultsConfig.saveArmDefaults(armDefaults)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            ProbabilityKnob(
                                title = "Random",
                                value = armDefaults.randomProbability,
                                onValueChange = { 
                                    // Adjust all three probabilities directly
                                    val newRandomProb = it.coerceIn(0f, 1f)
                                    
                                    // If Random is 100%, set others to 0
                                    val (newBeatProb, newLfoProb) = if (newRandomProb >= 0.999f) {
                                        Pair(0f, 0f)
                                    } else {
                                        // Otherwise, redistribute remaining probability
                                        val remainingProb = (1.0f - newRandomProb).coerceIn(0f, 1f)
                                        val oldRatio = if (armDefaults.beatProbability + armDefaults.lfoProbability > 0) {
                                            armDefaults.beatProbability / (armDefaults.beatProbability + armDefaults.lfoProbability)
                                        } else {
                                            0.5f // Default to equal distribution
                                        }
                                        
                                        Pair(
                                            remainingProb * oldRatio,
                                            remainingProb * (1 - oldRatio)
                                        )
                                    }
                                    
                                    // Update and save
                                    armDefaults = armDefaults.copy(
                                        beatProbability = newBeatProb,
                                        lfoProbability = newLfoProb,
                                        randomProbability = newRandomProb
                                    )
                                    defaultsConfig.saveArmDefaults(armDefaults)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
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
                        
                        // Dual slider - removed the labels
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
                        
                        // LFO Time Range - moved up from below
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
                        
                        // Glide Range for Random
                        Text(
                            text = "Glide Range for Random: ${(armDefaults.randomGlideMin * 100).toInt()}%-${(armDefaults.randomGlideMax * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        RangeSlider(
                            value = armDefaults.randomGlideMin..armDefaults.randomGlideMax,
                            onValueChange = { range ->
                                armDefaults = armDefaults.copy(
                                    randomGlideMin = range.start,
                                    randomGlideMax = range.endInclusive
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
                        
                        // Waveform Probabilities - updated text
                        Text(
                            text = "Waveform Probabilities for Beat & LFO",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProbabilityKnob(
                                title = "Sine",
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
                                modifier = Modifier.weight(1f)
                            )
                            
                            ProbabilityKnob(
                                title = "Triangle",
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
                                modifier = Modifier.weight(1f)
                            )
                            
                            ProbabilityKnob(
                                title = "Square",
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
                                modifier = Modifier.weight(1f)
                            )
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
                        
                        // Note about waveform implementation
                        Text(
                            text = "Note: For Beat and LFO, using triangle wave with slope 0 or 100% and weight 100%",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
                
                ROTATION -> {
                    // Rotation Settings
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "These settings control rotation when randomizing mandalas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Direction Probabilities
                        Text(
                            text = "Direction Probabilities",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // CCW label on left
                            Text(
                                text = "CCW: ${(100 - (rotationDefaults.clockwiseProbability * 100).toInt())}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppText.copy(alpha = 0.7f),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            
                            // Center bipolar knob
                            KnobView(
                                baseValue = rotationDefaults.clockwiseProbability,
                                onValueChange = { 
                                    rotationDefaults = rotationDefaults.copy(
                                        clockwiseProbability = it,
                                        counterClockwiseProbability = 1 - it
                                    )
                                    defaultsConfig.saveRotationDefaults(rotationDefaults)
                                },
                                onInteractionFinished = {},
                                isBipolar = true,  // Make it bipolar with arc centered at noon
                                knobSize = 45.dp,  // Same size as other knobs
                                showValue = false, // Don't show value on knob
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            // CW label on right
                            Text(
                                text = "CW: ${(rotationDefaults.clockwiseProbability * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppText.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        // Speed Source Probabilities
                        Text(
                            text = "Speed Source Probabilities",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        // Three knobs for CV sources
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProbabilityKnob(
                                title = "Beat",
                                value = rotationDefaults.beatProbability,
                                onValueChange = { 
                                    // Adjust all three probabilities directly
                                    val newBeatProb = it.coerceIn(0f, 1f)
                                    
                                    // If Beat is 100%, set others to 0
                                    val (newLfoProb, newRandomProb) = if (newBeatProb >= 0.999f) {
                                        Pair(0f, 0f)
                                    } else {
                                        // Otherwise, redistribute remaining probability
                                        val remainingProb = (1.0f - newBeatProb).coerceIn(0f, 1f)
                                        val oldRatio = if (rotationDefaults.lfoProbability + rotationDefaults.randomProbability > 0) {
                                            rotationDefaults.lfoProbability / (rotationDefaults.lfoProbability + rotationDefaults.randomProbability)
                                        } else {
                                            0.5f // Default to equal distribution
                                        }
                                        
                                        Pair(
                                            remainingProb * oldRatio,
                                            remainingProb * (1 - oldRatio)
                                        )
                                    }
                                    
                                    // Update and save
                                    rotationDefaults = rotationDefaults.copy(
                                        beatProbability = newBeatProb,
                                        lfoProbability = newLfoProb,
                                        randomProbability = newRandomProb
                                    )
                                    defaultsConfig.saveRotationDefaults(rotationDefaults)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            ProbabilityKnob(
                                title = "LFO",
                                value = rotationDefaults.lfoProbability,
                                onValueChange = { 
                                    // Adjust all three probabilities directly
                                    val newLfoProb = it.coerceIn(0f, 1f)
                                    
                                    // If LFO is 100%, set others to 0
                                    val (newBeatProb, newRandomProb) = if (newLfoProb >= 0.999f) {
                                        Pair(0f, 0f)
                                    } else {
                                        // Otherwise, redistribute remaining probability
                                        val remainingProb = (1.0f - newLfoProb).coerceIn(0f, 1f)
                                        val oldRatio = if (rotationDefaults.beatProbability + rotationDefaults.randomProbability > 0) {
                                            rotationDefaults.beatProbability / (rotationDefaults.beatProbability + rotationDefaults.randomProbability)
                                        } else {
                                            0.5f // Default to equal distribution
                                        }
                                        
                                        Pair(
                                            remainingProb * oldRatio,
                                            remainingProb * (1 - oldRatio)
                                        )
                                    }
                                    
                                    // Update and save
                                    rotationDefaults = rotationDefaults.copy(
                                        beatProbability = newBeatProb,
                                        lfoProbability = newLfoProb,
                                        randomProbability = newRandomProb
                                    )
                                    defaultsConfig.saveRotationDefaults(rotationDefaults)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            ProbabilityKnob(
                                title = "Random",
                                value = rotationDefaults.randomProbability,
                                onValueChange = { 
                                    // Adjust all three probabilities directly
                                    val newRandomProb = it.coerceIn(0f, 1f)
                                    
                                    // If Random is 100%, set others to 0
                                    val (newBeatProb, newLfoProb) = if (newRandomProb >= 0.999f) {
                                        Pair(0f, 0f)
                                    } else {
                                        // Otherwise, redistribute remaining probability
                                        val remainingProb = (1.0f - newRandomProb).coerceIn(0f, 1f)
                                        val oldRatio = if (rotationDefaults.beatProbability + rotationDefaults.lfoProbability > 0) {
                                            rotationDefaults.beatProbability / (rotationDefaults.beatProbability + rotationDefaults.lfoProbability)
                                        } else {
                                            0.5f // Default to equal distribution
                                        }
                                        
                                        Pair(
                                            remainingProb * oldRatio,
                                            remainingProb * (1 - oldRatio)
                                        )
                                    }
                                    
                                    // Update and save
                                    rotationDefaults = rotationDefaults.copy(
                                        beatProbability = newBeatProb,
                                        lfoProbability = newLfoProb,
                                        randomProbability = newRandomProb
                                    )
                                    defaultsConfig.saveRotationDefaults(rotationDefaults)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Beat Division Range (for Beat CV)
                        val rotationBeatValues = STANDARD_BEAT_VALUES
                        val rotationMinValueIndex = rotationBeatValues.indexOfFirst { it >= rotationDefaults.beatDivMin }.coerceAtLeast(0)
                        val rotationMaxValueIndex = rotationBeatValues.indexOfLast { it <= rotationDefaults.beatDivMax }.coerceAtMost(rotationBeatValues.lastIndex)
                        
                        Text(
                            text = "Beat Division Range (for Beat CV): ${formatBeatValue(rotationBeatValues[rotationMinValueIndex])}-${formatBeatValue(rotationBeatValues[rotationMaxValueIndex])}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        // Dual slider for Beat CV - no labels
                        RangeSlider(
                            value = rotationMinValueIndex.toFloat()..rotationMaxValueIndex.toFloat(),
                            onValueChange = { range ->
                                val newMin = rotationBeatValues[range.start.toInt()]
                                val newMax = rotationBeatValues[range.endInclusive.toInt()]
                                rotationDefaults = rotationDefaults.copy(
                                    beatDivMin = newMin,
                                    beatDivMax = newMax
                                )
                                defaultsConfig.saveRotationDefaults(rotationDefaults)
                            },
                            valueRange = 0f..(rotationBeatValues.size - 1).toFloat(),
                            steps = rotationBeatValues.size - 2,
                            colors = SliderDefaults.colors(
                                thumbColor = AppAccent,
                                activeTrackColor = AppAccent
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Beat Division Range for Random CV
                        val rotationRandomBeatValues = STANDARD_BEAT_VALUES
                        val rotationRandomMinValueIndex = rotationRandomBeatValues.indexOfFirst { it >= rotationDefaults.randomBeatDivMin }.coerceAtLeast(0)
                        val rotationRandomMaxValueIndex = rotationRandomBeatValues.indexOfLast { it <= rotationDefaults.randomBeatDivMax }.coerceAtMost(rotationRandomBeatValues.lastIndex)
                        
                        Text(
                            text = "Beat Division Range (for Random CV): ${formatBeatValue(rotationRandomBeatValues[rotationRandomMinValueIndex])}-${formatBeatValue(rotationRandomBeatValues[rotationRandomMaxValueIndex])}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        // Dual slider for Random CV - no labels
                        RangeSlider(
                            value = rotationRandomMinValueIndex.toFloat()..rotationRandomMaxValueIndex.toFloat(),
                            onValueChange = { range ->
                                val newMin = rotationRandomBeatValues[range.start.toInt()]
                                val newMax = rotationRandomBeatValues[range.endInclusive.toInt()]
                                rotationDefaults = rotationDefaults.copy(
                                    randomBeatDivMin = newMin,
                                    randomBeatDivMax = newMax
                                )
                                defaultsConfig.saveRotationDefaults(rotationDefaults)
                            },
                            valueRange = 0f..(rotationRandomBeatValues.size - 1).toFloat(),
                            steps = rotationRandomBeatValues.size - 2,
                            colors = SliderDefaults.colors(
                                thumbColor = AppAccent,
                                activeTrackColor = AppAccent
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // LFO Time Range
                        Text(
                            text = "LFO Time Range (for LFO CV): ${rotationDefaults.lfoTimeMin.toInt()}s-${rotationDefaults.lfoTimeMax.toInt()}s",
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
                        
                        // Glide Range for Random
                        Text(
                            text = "Glide Range for Random CV: ${(rotationDefaults.randomGlideMin * 100).toInt()}%-${(rotationDefaults.randomGlideMax * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        RangeSlider(
                            value = rotationDefaults.randomGlideMin..rotationDefaults.randomGlideMax,
                            onValueChange = { range ->
                                rotationDefaults = rotationDefaults.copy(
                                    randomGlideMin = range.start,
                                    randomGlideMax = range.endInclusive
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
                        
                        // Note about waveform implementation
                        Text(
                            text = "Note: Rotation uses a hard-coded triangle wave with slope 0 or 100% for Beat and LFO sources, with weight 100%",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // Omitted for brevity but implementation is similar to Arms section
                    }
                }
                
                COLOR -> {
                    // Color Settings (Hue Offset)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "These settings control color cycling when randomizing mandalas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Direction Probabilities
                        Text(
                            text = "Direction Probabilities",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // REV label on left
                            Text(
                                text = "REV: ${(100 - (hueOffsetDefaults.forwardProbability * 100).toInt())}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppText.copy(alpha = 0.7f),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            
                            // Center bipolar knob
                            KnobView(
                                baseValue = hueOffsetDefaults.forwardProbability,
                                onValueChange = { 
                                    hueOffsetDefaults = hueOffsetDefaults.copy(
                                        forwardProbability = it,
                                        reverseProbability = 1 - it
                                    )
                                    defaultsConfig.saveHueOffsetDefaults(hueOffsetDefaults)
                                },
                                onInteractionFinished = {},
                                isBipolar = true,  // Make it bipolar with arc centered at noon
                                knobSize = 45.dp,  // Same size as other knobs
                                showValue = false, // Don't show value on knob
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            // FWD label on right
                            Text(
                                text = "FWD: ${(hueOffsetDefaults.forwardProbability * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppText.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        // Speed Source Probabilities
                        Text(
                            text = "Speed Source Probabilities",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        // Three knobs for CV sources - Beat, LFO, Random
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProbabilityKnob(
                                title = "Beat",
                                value = hueOffsetDefaults.beatProbability,
                                onValueChange = { 
                                    // Adjust all three probabilities directly
                                    val newBeatProb = it.coerceIn(0f, 1f)
                                    
                                    // If Beat is 100%, set others to 0
                                    val (newLfoProb, newRandomProb) = if (newBeatProb >= 0.999f) {
                                        Pair(0f, 0f)
                                    } else {
                                        // Otherwise, redistribute remaining probability
                                        val remainingProb = (1.0f - newBeatProb).coerceIn(0f, 1f)
                                        val oldRatio = if (hueOffsetDefaults.lfoProbability + hueOffsetDefaults.randomProbability > 0) {
                                            hueOffsetDefaults.lfoProbability / (hueOffsetDefaults.lfoProbability + hueOffsetDefaults.randomProbability)
                                        } else {
                                            0.5f // Default to equal distribution
                                        }
                                        
                                        Pair(
                                            remainingProb * oldRatio,
                                            remainingProb * (1 - oldRatio)
                                        )
                                    }
                                    
                                    // Update and save
                                    hueOffsetDefaults = hueOffsetDefaults.copy(
                                        beatProbability = newBeatProb,
                                        lfoProbability = newLfoProb,
                                        randomProbability = newRandomProb
                                    )
                                    defaultsConfig.saveHueOffsetDefaults(hueOffsetDefaults)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            ProbabilityKnob(
                                title = "LFO",
                                value = hueOffsetDefaults.lfoProbability,
                                onValueChange = { 
                                    // Adjust all three probabilities directly
                                    val newLfoProb = it.coerceIn(0f, 1f)
                                    
                                    // If LFO is 100%, set others to 0
                                    val (newBeatProb, newRandomProb) = if (newLfoProb >= 0.999f) {
                                        Pair(0f, 0f)
                                    } else {
                                        // Otherwise, redistribute remaining probability
                                        val remainingProb = (1.0f - newLfoProb).coerceIn(0f, 1f)
                                        val oldRatio = if (hueOffsetDefaults.beatProbability + hueOffsetDefaults.randomProbability > 0) {
                                            hueOffsetDefaults.beatProbability / (hueOffsetDefaults.beatProbability + hueOffsetDefaults.randomProbability)
                                        } else {
                                            0.5f // Default to equal distribution
                                        }
                                        
                                        Pair(
                                            remainingProb * oldRatio,
                                            remainingProb * (1 - oldRatio)
                                        )
                                    }
                                    
                                    // Update and save
                                    hueOffsetDefaults = hueOffsetDefaults.copy(
                                        beatProbability = newBeatProb,
                                        lfoProbability = newLfoProb,
                                        randomProbability = newRandomProb
                                    )
                                    defaultsConfig.saveHueOffsetDefaults(hueOffsetDefaults)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            ProbabilityKnob(
                                title = "Random",
                                value = hueOffsetDefaults.randomProbability,
                                onValueChange = { 
                                    // Adjust all three probabilities directly
                                    val newRandomProb = it.coerceIn(0f, 1f)
                                    
                                    // If Random is 100%, set others to 0
                                    val (newBeatProb, newLfoProb) = if (newRandomProb >= 0.999f) {
                                        Pair(0f, 0f)
                                    } else {
                                        // Otherwise, redistribute remaining probability
                                        val remainingProb = (1.0f - newRandomProb).coerceIn(0f, 1f)
                                        val oldRatio = if (hueOffsetDefaults.beatProbability + hueOffsetDefaults.lfoProbability > 0) {
                                            hueOffsetDefaults.beatProbability / (hueOffsetDefaults.beatProbability + hueOffsetDefaults.lfoProbability)
                                        } else {
                                            0.5f // Default to equal distribution
                                        }
                                        
                                        Pair(
                                            remainingProb * oldRatio,
                                            remainingProb * (1 - oldRatio)
                                        )
                                    }
                                    
                                    // Update and save
                                    hueOffsetDefaults = hueOffsetDefaults.copy(
                                        beatProbability = newBeatProb,
                                        lfoProbability = newLfoProb,
                                        randomProbability = newRandomProb
                                    )
                                    defaultsConfig.saveHueOffsetDefaults(hueOffsetDefaults)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Beat Division Range
                        val colorBeatValues = STANDARD_BEAT_VALUES
                        val colorMinValueIndex = colorBeatValues.indexOfFirst { it >= hueOffsetDefaults.beatDivMin }.coerceAtLeast(0)
                        val colorMaxValueIndex = colorBeatValues.indexOfLast { it <= hueOffsetDefaults.beatDivMax }.coerceAtMost(colorBeatValues.lastIndex)
                        
                        Text(
                            text = "Beat Division Range: ${formatBeatValue(colorBeatValues[colorMinValueIndex])}-${formatBeatValue(colorBeatValues[colorMaxValueIndex])}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        // Dual slider - no labels
                        RangeSlider(
                            value = colorMinValueIndex.toFloat()..colorMaxValueIndex.toFloat(),
                            onValueChange = { range ->
                                val newMin = colorBeatValues[range.start.toInt()]
                                val newMax = colorBeatValues[range.endInclusive.toInt()]
                                hueOffsetDefaults = hueOffsetDefaults.copy(
                                    beatDivMin = newMin,
                                    beatDivMax = newMax
                                )
                                defaultsConfig.saveHueOffsetDefaults(hueOffsetDefaults)
                            },
                            valueRange = 0f..(colorBeatValues.size - 1).toFloat(),
                            steps = colorBeatValues.size - 2,
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
                        
                        // Glide Range for Random
                        Text(
                            text = "Glide Range for Random: ${(hueOffsetDefaults.randomGlideMin * 100).toInt()}%-${(hueOffsetDefaults.randomGlideMax * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        RangeSlider(
                            value = hueOffsetDefaults.randomGlideMin..hueOffsetDefaults.randomGlideMax,
                            onValueChange = { range ->
                                hueOffsetDefaults = hueOffsetDefaults.copy(
                                    randomGlideMin = range.start,
                                    randomGlideMax = range.endInclusive
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
                    }
                }
                
                FEEDBACK -> {
                    // Feedback Settings
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "These settings control feedback effects when randomizing mandalas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // FB Decay Range
                        Text(
                            text = "FB Decay Range: ${(feedbackDefaults.fbDecayMin * 100).toInt()}-${(feedbackDefaults.fbDecayMax * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        RangeSlider(
                            value = feedbackDefaults.fbDecayMin..feedbackDefaults.fbDecayMax,
                            onValueChange = { range ->
                                feedbackDefaults = feedbackDefaults.copy(
                                    fbDecayMin = range.start,
                                    fbDecayMax = range.endInclusive
                                )
                                defaultsConfig.saveFeedbackDefaults(feedbackDefaults)
                            },
                            valueRange = 0f..0.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = AppAccent,
                                activeTrackColor = AppAccent
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Additional feedback parameters would go here
                        // FB Gain, FB Zoom, FB Rotate, FB Shift X/Y, FB Blur
                    }
                }
            }
            
            // Reset Button for current section
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { 
                    when (selectedGroup) {
                        RECIPES -> {
                            defaultsConfig.resetRecipeDefaults()
                            recipeDefaults = defaultsConfig.getRecipeDefaults()
                        }
                        ARMS -> {
                            defaultsConfig.resetArmDefaults()
                            armDefaults = defaultsConfig.getArmDefaults() 
                        }
                        ROTATION -> {
                            defaultsConfig.resetRotationDefaults()
                            rotationDefaults = defaultsConfig.getRotationDefaults()
                        }
                        COLOR -> {
                            defaultsConfig.resetHueOffsetDefaults()
                            hueOffsetDefaults = defaultsConfig.getHueOffsetDefaults()
                        }
                        FEEDBACK -> {
                            defaultsConfig.resetFeedbackDefaults()
                            feedbackDefaults = defaultsConfig.getFeedbackDefaults()
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Reset ${getGroupName(selectedGroup)} to Defaults")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}