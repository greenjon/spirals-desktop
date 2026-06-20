package llm.slop.spirals.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import llm.slop.spirals.MandalaViewModel
import llm.slop.spirals.MandalaVisualSource
import llm.slop.spirals.LayerType
import llm.slop.spirals.RandomSetLayerContent
import llm.slop.spirals.RandomSetGenerator
import llm.slop.spirals.display.LocalSpiralRenderer
import llm.slop.spirals.models.RandomSet
import llm.slop.spirals.models.RecipeFilter
import llm.slop.spirals.models.ArmConstraints
import llm.slop.spirals.models.RotationConstraints
import llm.slop.spirals.models.HueOffsetConstraints
import llm.slop.spirals.models.STANDARD_BEAT_VALUES
import llm.slop.spirals.ui.components.PatchManagerOverlay
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText
import llm.slop.spirals.ui.theme.AppAccent
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomSetEditorScreen(
    vm: MandalaViewModel,
    onClose: () -> Unit,
    previewContent: @Composable () -> Unit,
    visualSource: MandalaVisualSource,
    showManager: Boolean = false,
    onHideManager: () -> Unit = {}
) {
    val context = LocalContext.current
    val allRandomSets by vm.allRandomSets.collectAsState(initial = emptyList())
    
    // Initialize from layer data if available
    val navStack by vm.navStack.collectAsState()
    val layer = navStack.lastOrNull { it.type == LayerType.RANDOM_SET }
    
    var currentRSet by remember { mutableStateOf((layer?.data as? RandomSetLayerContent)?.randomSet) }
    var selectedTab by remember { mutableStateOf(0) }
    var regenerateTrigger by remember { mutableIntStateOf(0) }
    
    // Renderer configuration for video display
    val renderer = LocalSpiralRenderer.current
    DisposableEffect(renderer) {
        renderer?.visualSource = visualSource
        renderer?.mixerPatch = null  // Ensure single mandala mode
        onDispose {
            renderer?.visualSource = null
        }
    }

    // Update local state if nav data changes (e.g. from Manage overlay)
    LaunchedEffect(layer?.data) {
        (layer?.data as? RandomSetLayerContent)?.randomSet?.let {
            if (it.id != (currentRSet?.id ?: "")) {
                currentRSet = it
            }
        }
    }
    
    // Update ViewModel when RSet changes
    LaunchedEffect(currentRSet) {
        val layerIndex = navStack.indexOfFirst { it.type == LayerType.RANDOM_SET }
        if (layerIndex != -1 && currentRSet != null) {
            vm.updateLayerData(layerIndex, RandomSetLayerContent(currentRSet!!), isDirty = true)
            vm.updateLayerName(layerIndex, currentRSet!!.name)
        }
    }
    
    // Generate preview when RSet or regenerate trigger changes
    LaunchedEffect(currentRSet, regenerateTrigger) {
        if (currentRSet != null) {
            val generator = RandomSetGenerator(context)
            generator.generateFromRSet(currentRSet!!, visualSource)
            visualSource.globalAlpha.baseValue = 1f
        } else {
            visualSource.globalAlpha.baseValue = 0f
        }
    }
    
    fun selectRSet(rsetId: String) {
        val entity = allRandomSets.find { it.id == rsetId } ?: return
        val newRSet = Json.decodeFromString<RandomSet>(entity.jsonSettings)
        currentRSet = newRSet
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {
            // Preview section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(Color.Black)
            ) {
                previewContent()
                
                // Info overlay
                if (currentRSet != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Random Set Template",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = currentRSet!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    
                    // Next/Regenerate button (center-left)
                    IconButton(
                        onClick = { regenerateTrigger++ },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                            .background(AppAccent.copy(alpha = 0.3f), MaterialTheme.shapes.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Generate New",
                            tint = AppAccent
                        )
                    }
                }
            }
            
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AppBackground,
                contentColor = AppAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Recipe") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Arms") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Motion") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Color") }
                )
            }
            
            // Tab content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
            ) {
                if (currentRSet != null) {
                    when (selectedTab) {
                        0 -> RecipeTab(
                            rset = currentRSet!!,
                            onUpdate = { currentRSet = it }
                        )
                        1 -> ArmsTab(
                            rset = currentRSet!!,
                            onUpdate = { currentRSet = it }
                        )
                        2 -> MotionTab(
                            rset = currentRSet!!,
                            onUpdate = { currentRSet = it }
                        )
                        3 -> ColorTab(
                            rset = currentRSet!!,
                            onUpdate = { currentRSet = it }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Random Set loaded",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppText.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
        
        // Manager overlay
        if (showManager && layer != null) {
            PatchManagerOverlay(
                title = "Random Sets",
                patches = allRandomSets.map { it.name to it.id },
                selectedId = currentRSet?.id,
                onSelect = { id ->
                    val entity = allRandomSets.find { it.id == id }
                    entity?.let { selectRSet(it.id) }
                },
                onOpen = { id ->
                    selectRSet(id)
                    onHideManager()
                },
                onCreateNew = {
                    vm.startNewPatch(LayerType.RANDOM_SET)
                    onHideManager()
                },
                onRename = { id, newName ->
                    vm.renameSavedPatch(LayerType.RANDOM_SET, id, newName)
                },
                onClone = { id ->
                    vm.cloneSavedPatch(LayerType.RANDOM_SET, id)
                },
                onDelete = { id ->
                    vm.deleteSavedPatch(LayerType.RANDOM_SET, id)
                }
            )
        }
    }
}

@Composable
fun RecipeTab(
    rset: RandomSet,
    onUpdate: (RandomSet) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Recipe Filter",
            style = MaterialTheme.typography.titleMedium,
            color = AppText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        RecipeFilterOption("All Recipes", RecipeFilter.ALL, rset.recipeFilter) {
            onUpdate(rset.copy(recipeFilter = RecipeFilter.ALL))
        }
        
        RecipeFilterOption("Favorites Only", RecipeFilter.FAVORITES_ONLY, rset.recipeFilter) {
            onUpdate(rset.copy(recipeFilter = RecipeFilter.FAVORITES_ONLY))
        }
        
        // Petal count exact
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    onUpdate(rset.copy(
                        recipeFilter = RecipeFilter.PETALS_EXACT,
                        petalCount = rset.petalCount ?: 5
                    )) 
                }
                .padding(vertical = 8.dp)
        ) {
            RadioButton(
                selected = rset.recipeFilter == RecipeFilter.PETALS_EXACT,
                onClick = { 
                    onUpdate(rset.copy(
                        recipeFilter = RecipeFilter.PETALS_EXACT,
                        petalCount = rset.petalCount ?: 5
                    )) 
                }
            )
            Text("Specific Petal Count:", modifier = Modifier.padding(start = 8.dp), color = AppText)
            Spacer(modifier = Modifier.width(16.dp))
            if (rset.recipeFilter == RecipeFilter.PETALS_EXACT) {
                OutlinedTextField(
                    value = (rset.petalCount ?: 5).toString(),
                    onValueChange = { 
                        it.toIntOrNull()?.let { count ->
                            onUpdate(rset.copy(petalCount = count))
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AppBackground,
                        unfocusedContainerColor = AppBackground,
                        focusedTextColor = AppText,
                        unfocusedTextColor = AppText
                    )
                )
            }
        }
        
        // Petal range
        RecipeFilterOption("Petal Range", RecipeFilter.PETALS_RANGE, rset.recipeFilter) {
            onUpdate(rset.copy(
                recipeFilter = RecipeFilter.PETALS_RANGE,
                petalMin = rset.petalMin ?: 3,
                petalMax = rset.petalMax ?: 9
            ))
        }
        
        if (rset.recipeFilter == RecipeFilter.PETALS_RANGE) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 48.dp, top = 8.dp)
            ) {
                Text("Min:", color = AppText)
                OutlinedTextField(
                    value = (rset.petalMin ?: 3).toString(),
                    onValueChange = { 
                        it.toIntOrNull()?.let { min ->
                            onUpdate(rset.copy(petalMin = min))
                        }
                    },
                    modifier = Modifier
                        .width(80.dp)
                        .padding(horizontal = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AppBackground,
                        unfocusedContainerColor = AppBackground,
                        focusedTextColor = AppText,
                        unfocusedTextColor = AppText
                    )
                )
                Text("Max:", color = AppText)
                OutlinedTextField(
                    value = (rset.petalMax ?: 9).toString(),
                    onValueChange = { 
                        it.toIntOrNull()?.let { max ->
                            onUpdate(rset.copy(petalMax = max))
                        }
                    },
                    modifier = Modifier
                        .width(80.dp)
                        .padding(horizontal = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AppBackground,
                        unfocusedContainerColor = AppBackground,
                        focusedTextColor = AppText,
                        unfocusedTextColor = AppText
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rset.autoHueSweep,
                onCheckedChange = { onUpdate(rset.copy(autoHueSweep = it)) }
            )
            Text(
                text = "Auto-set Hue Sweep to petals",
                modifier = Modifier.padding(start = 8.dp),
                color = AppText
            )
        }
    }
}

@Composable
fun RecipeFilterOption(
    label: String,
    filter: RecipeFilter,
    currentFilter: RecipeFilter,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp)
    ) {
        RadioButton(
            selected = currentFilter == filter,
            onClick = onSelect
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            color = AppText
        )
    }
}

@Composable
fun ArmsTab(
    rset: RandomSet,
    onUpdate: (RandomSet) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Arm Constraints",
            style = MaterialTheme.typography.titleMedium,
            color = AppText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Configure L1-L4 (outer to inner arms). Leave unconfigured to use defaults.",
            style = MaterialTheme.typography.bodySmall,
            color = AppText.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Link all arms to L1",
                style = MaterialTheme.typography.bodyMedium,
                color = AppText,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = rset.linkArms,
                onCheckedChange = { linked ->
                    if (linked) {
                        onUpdate(rset.copy(
                            linkArms = true,
                            l2Constraints = rset.l1Constraints,
                            l3Constraints = rset.l1Constraints,
                            l4Constraints = rset.l1Constraints
                        ))
                    } else {
                        onUpdate(rset.copy(linkArms = false))
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = AppAccent, checkedTrackColor = AppAccent.copy(alpha = 0.5f))
            )
        }

        if (rset.linkArms) {
            ArmConstraintSection(
                title = "L1 (Master Control)",
                constraints = rset.l1Constraints,
                onUpdate = { constraints ->
                    onUpdate(rset.copy(
                        l1Constraints = constraints,
                        l2Constraints = constraints,
                        l3Constraints = constraints,
                        l4Constraints = constraints
                    ))
                },
                onClear = { 
                    onUpdate(rset.copy(
                        l1Constraints = null,
                        l2Constraints = null,
                        l3Constraints = null,
                        l4Constraints = null
                    ))
                }
            )
        } else {
            ArmConstraintSection(
                title = "L1 (Outer Arm)",
                constraints = rset.l1Constraints,
                onUpdate = { onUpdate(rset.copy(l1Constraints = it)) },
                onClear = { onUpdate(rset.copy(l1Constraints = null)) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ArmConstraintSection(
                title = "L2",
                constraints = rset.l2Constraints,
                onUpdate = { onUpdate(rset.copy(l2Constraints = it)) },
                onClear = { onUpdate(rset.copy(l2Constraints = null)) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ArmConstraintSection(
                title = "L3",
                constraints = rset.l3Constraints,
                onUpdate = { onUpdate(rset.copy(l3Constraints = it)) },
                onClear = { onUpdate(rset.copy(l3Constraints = null)) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ArmConstraintSection(
                title = "L4 (Inner Arm)",
                constraints = rset.l4Constraints,
                onUpdate = { onUpdate(rset.copy(l4Constraints = it)) },
                onClear = { onUpdate(rset.copy(l4Constraints = null)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmConstraintSection(
    title: String,
    constraints: ArmConstraints?,
    onUpdate: (ArmConstraints) -> Unit,
    onClear: () -> Unit
) {
    var expanded by remember { mutableStateOf(constraints != null) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (constraints != null) AppAccent.copy(alpha = 0.1f) else AppBackground.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AppText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppText,
                    modifier = Modifier.weight(1f)
                )
                if (constraints != null) {
                    Text(
                        text = "✓ configured",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppAccent,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                } else {
                    Text(
                        text = "using defaults",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppText.copy(alpha = 0.5f),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Get defaults from settings when no constraints are provided
                val context = LocalContext.current
                val defaultsConfig = llm.slop.spirals.defaults.DefaultsConfig.getInstance(context)
                val defaults = defaultsConfig.getArmDefaults()
                val currentConstraints = if (constraints != null) {
                    constraints
                } else {
                    // Convert from defaults to ArmConstraints
                    ArmConstraints(
                        baseLengthMin = defaults.baseLengthMin,
                        baseLengthMax = defaults.baseLengthMax,
                        enableBeat = defaults.beatProbability > 0,
                        enableLfo = defaults.lfoProbability > 0,
                        enableRandom = defaults.defaultEnableRandom,
                        allowSine = defaults.sineProbability > 0,
                        allowTriangle = defaults.triangleProbability > 0,
                        allowSquare = defaults.squareProbability > 0,
                        beatDivMin = defaults.beatDivMin,
                        beatDivMax = defaults.beatDivMax,
                        weightMin = defaults.weightMin,
                        weightMax = defaults.weightMax,
                        lfoTimeMin = defaults.lfoTimeMin,
                        lfoTimeMax = defaults.lfoTimeMax,
                        randomGlideMin = defaults.randomGlideMin,
                        randomGlideMax = defaults.randomGlideMax,
                        phaseMin = defaults.phaseMin,
                        phaseMax = defaults.phaseMax
                    )
                }
                
                // Base Length Range
                Text(
                    text = "Base Length: ${currentConstraints.baseLengthMin}-${currentConstraints.baseLengthMax}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppText,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                RangeSlider(
                    value = currentConstraints.baseLengthMin.toFloat()..currentConstraints.baseLengthMax.toFloat(),
                    onValueChange = { range ->
                        onUpdate(currentConstraints.copy(
                            baseLengthMin = range.start.toInt(),
                            baseLengthMax = range.endInclusive.toInt()
                        ))
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = AppAccent,
                        activeTrackColor = AppAccent
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Movement sources
                Text(
                    text = "Movement Sources:",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppText
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = currentConstraints.enableBeat,
                            onCheckedChange = { onUpdate(currentConstraints.copy(enableBeat = it)) },
                            colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                        )
                        Text("Beat", color = AppText, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = currentConstraints.enableLfo,
                            onCheckedChange = { onUpdate(currentConstraints.copy(enableLfo = it)) },
                            colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                        )
                        Text("LFO", color = AppText, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = currentConstraints.enableRandom,
                            onCheckedChange = { onUpdate(currentConstraints.copy(enableRandom = it)) },
                            colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                        )
                        Text("Random", color = AppText, style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                // Beat division controls (shown when Beat OR Random is enabled)
                if (currentConstraints.enableBeat || currentConstraints.enableRandom) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val beatValues = STANDARD_BEAT_VALUES
                    val minValueIndex = beatValues.indexOfFirst { it >= currentConstraints.beatDivMin }.coerceAtLeast(0)
                    val maxValueIndex = beatValues.indexOfLast { it <= currentConstraints.beatDivMax }.coerceAtMost(beatValues.lastIndex)
                    
                    val formatBeatValue = { value: Float ->
                        when {
                            value < 1 -> "1/${(1/value).toInt()}"
                            else -> value.toInt().toString()
                        }
                    }
                    
                    Text(
                        text = "Beat Division: ${formatBeatValue(beatValues[minValueIndex])}-${formatBeatValue(beatValues[maxValueIndex])}",
                        style = MaterialTheme.typography.bodySmall,
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
                            onUpdate(currentConstraints.copy(
                                beatDivMin = newMin,
                                beatDivMax = newMax
                            ))
                        },
                        valueRange = 0f..(beatValues.size - 1).toFloat(),
                        steps = beatValues.size - 2,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        )
                    )
                }
                
                // Random Glide controls (only shown when Random is enabled)
                if (currentConstraints.enableRandom) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Random Glide: ${String.format("%.2f", currentConstraints.randomGlideMin)}-${String.format("%.2f", currentConstraints.randomGlideMax)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RangeSlider(
                        value = currentConstraints.randomGlideMin..currentConstraints.randomGlideMax,
                        onValueChange = { range ->
                            onUpdate(currentConstraints.copy(
                                randomGlideMin = range.start,
                                randomGlideMax = range.endInclusive
                            ))
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        )
                    )
                }
                
                // LFO time controls (only shown when LFO is enabled)
                if (currentConstraints.enableLfo) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "LFO Time: ${currentConstraints.lfoTimeMin.toInt()}s-${currentConstraints.lfoTimeMax.toInt()}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RangeSlider(
                        value = currentConstraints.lfoTimeMin..currentConstraints.lfoTimeMax,
                        onValueChange = { range ->
                            onUpdate(currentConstraints.copy(
                                lfoTimeMin = range.start,
                                lfoTimeMax = range.endInclusive
                            ))
                        },
                        valueRange = 1f..900f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        )
                    )
                }

                // Phase Range Control
                if (currentConstraints.enableBeat || currentConstraints.enableLfo || currentConstraints.enableRandom) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Phase Range: ${currentConstraints.phaseMin.toInt()}° - ${currentConstraints.phaseMax.toInt()}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RangeSlider(
                        value = currentConstraints.phaseMin..currentConstraints.phaseMax,
                        onValueChange = { range ->
                            onUpdate(currentConstraints.copy(
                                phaseMin = range.start,
                                phaseMax = range.endInclusive
                            ))
                        },
                        valueRange = 0f..270f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Waveforms (hide if only Random is enabled)
                if (currentConstraints.enableBeat || currentConstraints.enableLfo) {
                    Text(
                        text = "Waveforms:",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = currentConstraints.allowSine,
                                onCheckedChange = { onUpdate(currentConstraints.copy(allowSine = it)) },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("Sine", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = currentConstraints.allowTriangle,
                                onCheckedChange = { onUpdate(currentConstraints.copy(allowTriangle = it)) },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("Triangle", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = currentConstraints.allowSquare,
                                onCheckedChange = { onUpdate(currentConstraints.copy(allowSquare = it)) },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("Square", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Weight Range
                Text(
                    text = "Weight: ${currentConstraints.weightMin} to ${currentConstraints.weightMax}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppText,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                RangeSlider(
                    value = currentConstraints.weightMin.toFloat()..currentConstraints.weightMax.toFloat(),
                    onValueChange = { range ->
                        onUpdate(currentConstraints.copy(
                            weightMin = range.start.toInt(),
                            weightMax = range.endInclusive.toInt()
                        ))
                    },
                    valueRange = -100f..100f,
                    colors = SliderDefaults.colors(
                            thumbColor = AppAccent,
                            activeTrackColor = AppAccent
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (constraints == null) {
                        // Get defaults outside of the onClick handler
                        val context = LocalContext.current
                        val defaultsConfig = llm.slop.spirals.defaults.DefaultsConfig.getInstance(context)
                        val defaults = defaultsConfig.getArmDefaults()
                        
                        Button(
                            onClick = {
                                // Convert to ArmConstraints
                                val armConstraints = ArmConstraints(
                                    baseLengthMin = defaults.baseLengthMin,
                                    baseLengthMax = defaults.baseLengthMax,
                                    enableBeat = defaults.beatProbability > 0,
                                    enableLfo = defaults.lfoProbability > 0,
                                    enableRandom = defaults.defaultEnableRandom,
                                    allowSine = defaults.sineProbability > 0,
                                    allowTriangle = defaults.triangleProbability > 0,
                                    allowSquare = defaults.squareProbability > 0,
                                    beatDivMin = defaults.beatDivMin,
                                    beatDivMax = defaults.beatDivMax,
                                    weightMin = defaults.weightMin,
                                    weightMax = defaults.weightMax,
                                    lfoTimeMin = defaults.lfoTimeMin,
                                    lfoTimeMax = defaults.lfoTimeMax,
                                    randomGlideMin = defaults.randomGlideMin,
                                    randomGlideMax = defaults.randomGlideMax,
                                    phaseMin = defaults.phaseMin,
                                    phaseMax = defaults.phaseMax
                                )
                                
                                onUpdate(armConstraints)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                        ) {
                            Text("Enable Constraints", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onClear,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Use Defaults", style = MaterialTheme.typography.labelSmall, color = AppText)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionTab(
    rset: RandomSet,
    onUpdate: (RandomSet) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Rotation Constraints",
            style = MaterialTheme.typography.titleMedium,
            color = AppText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Configure smooth rotation for generated mandalas.",
            style = MaterialTheme.typography.bodySmall,
            color = AppText.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val currentConstraints = rset.rotationConstraints
        val isEnabled = currentConstraints != null
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) AppAccent.copy(alpha = 0.1f) else AppBackground.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnabled) "✓ Rotation Enabled" else "Rotation Disabled",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isEnabled) AppAccent else AppText.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )
                    // Get defaults outside of the onCheckedChange handler
                    val context = LocalContext.current
                    val defaultsConfig = llm.slop.spirals.defaults.DefaultsConfig.getInstance(context)
                    val defaults = defaultsConfig.getRotationDefaults()
                    
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                // Convert to RotationConstraints
                                val rotationConstraints = RotationConstraints(
                                    enableClockwise = defaults.clockwiseProbability > 0,
                                    enableCounterClockwise = defaults.counterClockwiseProbability > 0,
                                    enableBeat = defaults.beatProbability > 0,
                                    enableLfo = defaults.lfoProbability > 0,
                                    enableRandom = defaults.randomProbability > 0,
                                    beatDivMin = defaults.beatDivMin,
                                    beatDivMax = defaults.beatDivMax,
                                    lfoTimeMin = defaults.lfoTimeMin,
                                    lfoTimeMax = defaults.lfoTimeMax,
                                    randomGlideMin = defaults.randomGlideMin,
                                    randomGlideMax = defaults.randomGlideMax
                                )
                                
                                onUpdate(rset.copy(rotationConstraints = rotationConstraints))
                            } else {
                                onUpdate(rset.copy(rotationConstraints = null))
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AppAccent, checkedTrackColor = AppAccent.copy(alpha = 0.5f))
                    )
                }
                
                if (isEnabled) {
                    val constraints = currentConstraints!!
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Direction
                    Text(
                        text = "Direction:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = constraints.enableClockwise,
                                onCheckedChange = { 
                                    onUpdate(rset.copy(rotationConstraints = constraints.copy(enableClockwise = it)))
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("Clockwise", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = constraints.enableCounterClockwise,
                                onCheckedChange = { 
                                    onUpdate(rset.copy(rotationConstraints = constraints.copy(enableCounterClockwise = it)))
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("Counter-CW", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Movement sources
                    Text(
                        text = "Movement Sources:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = constraints.enableBeat,
                                onCheckedChange = { onUpdate(rset.copy(rotationConstraints = constraints.copy(enableBeat = it))) },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("Beat", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = constraints.enableLfo,
                                onCheckedChange = { onUpdate(rset.copy(rotationConstraints = constraints.copy(enableLfo = it))) },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("LFO", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = constraints.enableRandom,
                                onCheckedChange = { onUpdate(rset.copy(rotationConstraints = constraints.copy(enableRandom = it))) },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("Random", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    // Beat division controls (shown when Beat OR Random is enabled)
                    if (constraints.enableBeat || constraints.enableRandom) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val beatValues = STANDARD_BEAT_VALUES
                        val minValueIndex = beatValues.indexOfFirst { it >= constraints.beatDivMin }.coerceAtLeast(0)
                        val maxValueIndex = beatValues.indexOfLast { it <= constraints.beatDivMax }.coerceAtMost(beatValues.lastIndex)
                        
                        val formatBeatValue = { value: Float ->
                            when {
                                value < 1 -> "1/${(1/value).toInt()}"
                                else -> value.toInt().toString()
                            }
                        }
                        
                        Text(
                            text = "Beat Division: ${formatBeatValue(beatValues[minValueIndex])}-${formatBeatValue(beatValues[maxValueIndex])}",
                            style = MaterialTheme.typography.bodySmall,
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
                                onUpdate(rset.copy(rotationConstraints = constraints.copy(
                                    beatDivMin = newMin,
                                    beatDivMax = newMax
                                )))
                            },
                            valueRange = 0f..(beatValues.size - 1).toFloat(),
                            steps = beatValues.size - 2,
                            colors = SliderDefaults.colors(
                                thumbColor = AppAccent,
                                activeTrackColor = AppAccent
                            )
                        )
                    }
                    
                    // Random Glide controls (only shown when Random is enabled)
                    if (constraints.enableRandom) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Random Glide: ${String.format("%.2f", constraints.randomGlideMin)}-${String.format("%.2f", constraints.randomGlideMax)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        RangeSlider(
                            value = constraints.randomGlideMin..constraints.randomGlideMax,
                            onValueChange = { range ->
                                onUpdate(rset.copy(rotationConstraints = constraints.copy(
                                    randomGlideMin = range.start,
                                    randomGlideMax = range.endInclusive
                                )))
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = AppAccent,
                                activeTrackColor = AppAccent
                            )
                        )
                    }
                    
                    // LFO time controls (only shown when LFO is enabled)
                    if (constraints.enableLfo) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "LFO Time: ${constraints.lfoTimeMin.toInt()}s-${constraints.lfoTimeMax.toInt()}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        RangeSlider(
                            value = constraints.lfoTimeMin..constraints.lfoTimeMax,
                            onValueChange = { range ->
                                onUpdate(rset.copy(rotationConstraints = constraints.copy(
                                    lfoTimeMin = range.start,
                                    lfoTimeMax = range.endInclusive
                                )))
                            },
                            valueRange = 1f..900f,
                            colors = SliderDefaults.colors(
                                thumbColor = AppAccent,
                                activeTrackColor = AppAccent
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorTab(
    rset: RandomSet,
    onUpdate: (RandomSet) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Hue Offset Constraints",
            style = MaterialTheme.typography.titleMedium,
            color = AppText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Configure color cycling (rainbow shift) for generated mandalas.",
            style = MaterialTheme.typography.bodySmall,
            color = AppText.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val currentConstraints = rset.hueOffsetConstraints
        val isEnabled = currentConstraints != null
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) AppAccent.copy(alpha = 0.1f) else AppBackground.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnabled) "✓ Color Cycling Enabled" else "Color Cycling Disabled",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isEnabled) AppAccent else AppText.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )
                    // Get defaults outside of the onCheckedChange handler
                    val context = LocalContext.current
                    val defaultsConfig = llm.slop.spirals.defaults.DefaultsConfig.getInstance(context)
                    val defaults = defaultsConfig.getHueOffsetDefaults()
                    
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                // Convert to HueOffsetConstraints
                                val hueOffsetConstraints = HueOffsetConstraints(
                                    enableForward = defaults.forwardProbability > 0,
                                    enableReverse = defaults.reverseProbability > 0,
                                    enableBeat = defaults.beatProbability > 0,
                                    enableLfo = defaults.lfoProbability > 0,
                                    enableRandom = defaults.randomProbability > 0,
                                    beatDivMin = defaults.beatDivMin,
                                    beatDivMax = defaults.beatDivMax,
                                    lfoTimeMin = defaults.lfoTimeMin,
                                    lfoTimeMax = defaults.lfoTimeMax,
                                    randomGlideMin = defaults.randomGlideMin,
                                    randomGlideMax = defaults.randomGlideMax
                                )
                                
                                onUpdate(rset.copy(hueOffsetConstraints = hueOffsetConstraints))
                            } else {
                                onUpdate(rset.copy(hueOffsetConstraints = null))
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AppAccent, checkedTrackColor = AppAccent.copy(alpha = 0.5f))
                    )
                }
                
                if (isEnabled) {
                    val constraints = currentConstraints!!
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Direction
                    Text(
                        text = "Direction:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = constraints.enableForward,
                                onCheckedChange = { 
                                    onUpdate(rset.copy(hueOffsetConstraints = constraints.copy(enableForward = it)))
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("Forward", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = constraints.enableReverse,
                                onCheckedChange = { 
                                    onUpdate(rset.copy(hueOffsetConstraints = constraints.copy(enableReverse = it)))
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("Reverse", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Movement sources
                    Text(
                        text = "Movement Sources:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = constraints.enableBeat,
                                onCheckedChange = { onUpdate(rset.copy(hueOffsetConstraints = constraints.copy(enableBeat = it))) },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("Beat", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = constraints.enableLfo,
                                onCheckedChange = { onUpdate(rset.copy(hueOffsetConstraints = constraints.copy(enableLfo = it))) },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("LFO", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = constraints.enableRandom,
                                onCheckedChange = { onUpdate(rset.copy(hueOffsetConstraints = constraints.copy(enableRandom = it))) },
                                colors = CheckboxDefaults.colors(checkedColor = AppAccent)
                            )
                            Text("Random", color = AppText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    // Beat division controls (shown when Beat OR Random is enabled)
                    if (constraints.enableBeat || constraints.enableRandom) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val beatValues = STANDARD_BEAT_VALUES
                        val minValueIndex = beatValues.indexOfFirst { it >= constraints.beatDivMin }.coerceAtLeast(0)
                        val maxValueIndex = beatValues.indexOfLast { it <= constraints.beatDivMax }.coerceAtMost(beatValues.lastIndex)
                        
                        val formatBeatValue = { value: Float ->
                            when {
                                value < 1 -> "1/${(1/value).toInt()}"
                                else -> value.toInt().toString()
                            }
                        }
                        
                        Text(
                            text = "Beat Division: ${formatBeatValue(beatValues[minValueIndex])}-${formatBeatValue(beatValues[maxValueIndex])}",
                            style = MaterialTheme.typography.bodySmall,
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
                                onUpdate(rset.copy(hueOffsetConstraints = constraints.copy(
                                    beatDivMin = newMin,
                                    beatDivMax = newMax
                                )))
                            },
                            valueRange = 0f..(beatValues.size - 1).toFloat(),
                                steps = beatValues.size - 2,
                            colors = SliderDefaults.colors(
                                thumbColor = AppAccent,
                                activeTrackColor = AppAccent
                            )
                        )
                    }
                    
                    // Random Glide controls (only shown when Random is enabled)
                    if (constraints.enableRandom) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Random Glide: ${String.format("%.2f", constraints.randomGlideMin)}-${String.format("%.2f", constraints.randomGlideMax)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        RangeSlider(
                            value = constraints.randomGlideMin..constraints.randomGlideMax,
                            onValueChange = { range ->
                                onUpdate(rset.copy(hueOffsetConstraints = constraints.copy(
                                    randomGlideMin = range.start,
                                    randomGlideMax = range.endInclusive
                                )))
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = AppAccent,
                                activeTrackColor = AppAccent
                            )
                        )
                    }
                    
                    // LFO time controls (only shown when LFO is enabled)
                    if (constraints.enableLfo) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "LFO Time: ${constraints.lfoTimeMin.toInt()}s-${constraints.lfoTimeMax.toInt()}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        RangeSlider(
                            value = constraints.lfoTimeMin..constraints.lfoTimeMax,
                            onValueChange = { range ->
                                onUpdate(rset.copy(hueOffsetConstraints = constraints.copy(
                                    lfoTimeMin = range.start,
                                    lfoTimeMax = range.endInclusive
                                )))
                            },
                            valueRange = 1f..600f,
                            colors = SliderDefaults.colors(
                                thumbColor = AppAccent,
                                activeTrackColor = AppAccent
                            )
                        )
                    }
                }
            }
        }
    }
}
