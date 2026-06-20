package llm.slop.spirals.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import llm.slop.spirals.*
import llm.slop.spirals.cv.core.CvModulator
import llm.slop.spirals.cv.core.ModulationOperator
import llm.slop.spirals.cv.core.Waveform
import llm.slop.spirals.models.PatchData
import llm.slop.spirals.models.STANDARD_BEAT_VALUES
import llm.slop.spirals.models.SpeedSource
import llm.slop.spirals.display.LocalSpiralRenderer
import llm.slop.spirals.ui.screens.InstrumentEditorScreen
import llm.slop.spirals.ui.components.MandalaParameterMatrix
import llm.slop.spirals.ui.components.OscilloscopeView
import llm.slop.spirals.ui.components.PatchManagerOverlay
import llm.slop.spirals.ui.components.RecipePickerDialog
import llm.slop.spirals.ui.components.RecipeSortMode
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText

@Composable
fun MandalaEditorScreen(
    vm: MandalaViewModel,
    visualSource: MandalaVisualSource,
    isDirty: Boolean,
    lastLoadedPatch: PatchData?,
    onPatchLoaded: (PatchData) -> Unit,
    onInteraction: () -> Unit,
    onNavigateToSetEditor: () -> Unit,
    onNavigateToMixerEditor: () -> Unit,
    onShowCvLab: () -> Unit,
    previewContent: @Composable () -> Unit,
    showHeader: Boolean = true,
    showManager: Boolean = false,
    onHideManager: () -> Unit = {}
) {
    var focusedParameterId by remember { mutableStateOf("L1") }
    var recipeExpanded by remember { mutableStateOf(false) }

    // Get the current layer name from the nav stack (handles renames)
    val navStack by vm.navStack.collectAsState()
    val currentLayer = navStack.lastOrNull { it.type == LayerType.MANDALA }
    val patchName = currentLayer?.name ?: lastLoadedPatch?.name ?: "New Patch"

    val allPatches by vm.allPatches.collectAsState(initial = emptyList())
    var recipeSortMode by remember { mutableStateOf(RecipeSortMode.PETALS) }

    val renderer = LocalSpiralRenderer.current
    DisposableEffect(renderer) {
        renderer?.visualSource = visualSource
        renderer?.mixerPatch = null
        // Reset alpha when entering editor
        visualSource.globalAlpha.baseValue = 1f
        onDispose {
            renderer?.visualSource = null
        }
    }

    // Apply loaded patch to visual source when it changes
    LaunchedEffect(lastLoadedPatch) {
        lastLoadedPatch?.let {
            PatchMapper.applyToVisualSource(it, visualSource)
        }
    }

    // Keep ViewModel updated with current work-in-progress for cascade saving
    LaunchedEffect(patchName, visualSource.recipe, visualSource.parameters.values.map { it.value }) {
        val patchData = PatchMapper.fromVisualSource(patchName, visualSource)
        val stack = vm.navStack.value
        val index = stack.indexOfLast { it.type == LayerType.MANDALA }
        if (index != -1 && stack[index].data != null) { // Only update if we have actual data
            val realDirty = PatchMapper.isDirty(visualSource, lastLoadedPatch)
            vm.updateLayerData(index, MandalaLayerContent(patchData), isDirty = realDirty)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                Column(modifier = Modifier.wrapContentHeight().fillMaxWidth().padding(horizontal = 8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16 / 9f).background(Color.Black).border(1.dp, AppText.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        previewContent()

                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                            Surface(color = AppBackground.copy(alpha = 0.7f), shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.clickable { recipeExpanded = true }) {
                                Text(text = "${visualSource.recipe.a}, ${visualSource.recipe.b}, ${visualSource.recipe.c}, ${visualSource.recipe.d} (${visualSource.recipe.petals}P)", style = MaterialTheme.typography.labelSmall, color = AppAccent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp) )
                            }
                        }

                        // Randomize button (center-left)
                        IconButton(
                            onClick = {
                                randomizeMandala(visualSource, vm)
                                onInteraction()
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(8.dp)
                                .size(48.dp)
                                .background(AppBackground.copy(alpha = 0.7f), MaterialTheme.shapes.small)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Randomize",
                                tint = AppAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Star/Trash buttons on the right side
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val context = LocalContext.current
                            val tagManager = remember { RecipeTagManager(context) }
                            var refreshTrigger by remember { mutableIntStateOf(0) }
                            val isFavorite = remember(visualSource.recipe.id, refreshTrigger) {
                                tagManager.isFavorite(visualSource.recipe.id)
                            }
                            val isTrash = remember(visualSource.recipe.id, refreshTrigger) {
                                tagManager.isTrash(visualSource.recipe.id)
                            }

                            IconButton(
                                onClick = {
                                    tagManager.toggleFavorite(visualSource.recipe.id)
                                    refreshTrigger++
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(AppBackground.copy(alpha = 0.7f), MaterialTheme.shapes.small)
                            ) {
                                Icon(
                                    if (isFavorite) Icons.Filled.Star else Icons.Default.Star,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (isFavorite) androidx.compose.ui.graphics.Color(0xFFFFD700) else AppText.copy(alpha = 0.5f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    tagManager.toggleTrash(visualSource.recipe.id)
                                    refreshTrigger++
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(AppBackground.copy(alpha = 0.7f), MaterialTheme.shapes.small)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Toggle Trash",
                                    tint = if (isTrash) androidx.compose.ui.graphics.Color.Red else AppText.copy(alpha = 0.5f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Navigation arrows (lower right)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val context = LocalContext.current
                            val tagManager = remember { RecipeTagManager(context) }
                            val sortedRecipes = remember(recipeSortMode) {
                                val favorites = tagManager.getFavorites()
                                val trash = tagManager.getTrash()
                                when (recipeSortMode) {
                                    RecipeSortMode.PETALS -> MandalaLibrary.MandalaRatios.sortedBy { it.petals }
                                    RecipeSortMode.FAVORITES -> {
                                        val faves = MandalaLibrary.MandalaRatios.filter { favorites.contains(it.id) }
                                            .sortedWith(compareBy({ it.petals }, { it.id }))
                                        val rest = MandalaLibrary.MandalaRatios.filter { !favorites.contains(it.id) }
                                            .sortedWith(compareBy({ it.petals }, { it.id }))
                                        faves + rest
                                    }
                                    RecipeSortMode.TO_DELETE -> {
                                        val trashItems = MandalaLibrary.MandalaRatios.filter { trash.contains(it.id) }
                                            .sortedBy { it.id }
                                        val rest = MandalaLibrary.MandalaRatios.filter { !trash.contains(it.id) }
                                            .sortedBy { it.id }
                                        trashItems + rest
                                    }
                                    RecipeSortMode.SHAPE_RATIO -> MandalaLibrary.MandalaRatios.sortedBy { it.shapeRatio }
                                    RecipeSortMode.MULTIPLICITY -> MandalaLibrary.MandalaRatios.sortedBy { it.multiplicityClass }
                                    RecipeSortMode.FREQ_COUNT -> MandalaLibrary.MandalaRatios.sortedBy { it.independentFreqCount }
                                    RecipeSortMode.HIERARCHY -> MandalaLibrary.MandalaRatios.sortedBy { it.hierarchyDepth }
                                    RecipeSortMode.DOMINANCE -> MandalaLibrary.MandalaRatios.sortedBy { it.dominanceRatio }
                                    RecipeSortMode.RADIAL_VAR -> MandalaLibrary.MandalaRatios.sortedBy { it.radialVariance }
                                }
                            }
                            val currentIndex = sortedRecipes.indexOfFirst { it.id == visualSource.recipe.id }

                            IconButton(
                                onClick = {
                                    if (currentIndex > 0) {
                                        visualSource.recipe = sortedRecipes[currentIndex - 1]
                                        onInteraction()
                                    }
                                },
                                enabled = currentIndex > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Previous Recipe",
                                    tint = if (currentIndex > 0) AppAccent else AppText.copy(alpha = 0.3f)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (currentIndex < sortedRecipes.size - 1) {
                                        visualSource.recipe = sortedRecipes[currentIndex + 1]
                                        onInteraction()
                                    }
                                },
                                enabled = currentIndex < sortedRecipes.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Next Recipe",
                                    tint = if (currentIndex < sortedRecipes.size - 1) AppAccent else AppText.copy(alpha = 0.3f)
                                )
                            }
                        }

                        if (recipeExpanded) {
                            RecipePickerDialog(
                                currentRecipe = visualSource.recipe,
                                initialSortMode = recipeSortMode,
                                onRecipeSelected = { ratio ->
                                    visualSource.recipe = ratio
                                    onInteraction()
                                    recipeExpanded = false
                                },
                                onSortModeChanged = { mode ->
                                    recipeSortMode = mode
                                },
                                onDismiss = { recipeExpanded = false }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    var monitorTick by remember { mutableIntStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            monitorTick++
                            delay(16)
                        }
                    }

                    val focusedParam = remember(focusedParameterId, visualSource) {
                        visualSource.parameters[focusedParameterId] ?: visualSource.globalAlpha
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(60.dp).border(1.dp, AppText.copy(alpha = 0.1f))) {
                        key(monitorTick) {
                            OscilloscopeView(history = focusedParam.history, modifier = Modifier.fillMaxSize())
                        }
                        Surface(color = AppBackground.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.TopStart).padding(4.dp), shape = MaterialTheme.shapes.extraSmall) {
                            Text(text = focusedParameterId, style = MaterialTheme.typography.labelSmall, color = AppAccent, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    MandalaParameterMatrix(labels = visualSource.parameters.keys.toList(), parameters = visualSource.parameters.values.toList(), focusedParameterId = focusedParameterId, onFocusRequest = { focusedParameterId = it }, onInteractionFinished = onInteraction)
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    InstrumentEditorScreen(source = visualSource, vm = vm, focusedId = focusedParameterId, onFocusChange = { focusedParameterId = it }, onInteractionFinished = onInteraction)
                }
            }
        }

        if (showManager) {
            PatchManagerOverlay(
                title = "Manage Mandalas",
                patches = allPatches.map { it.name to it.name },
                selectedId = patchName,
                onSelect = { id ->
                    // Preview instantly on tap
                    val entity = allPatches.find { it.name == id }
                    entity?.let {
                        val data = PatchMapper.fromJson(it.jsonSettings)
                        if (data != null) {
                            PatchMapper.applyToVisualSource(data, visualSource)
                            vm.setCurrentPatch(data)
                            // Update layer name in stack
                            val stack = vm.navStack.value
                            val idx = stack.indexOfLast { it.type == LayerType.MANDALA }
                            if (idx != -1) {
                                vm.updateLayerData(idx, MandalaLayerContent(data))
                                vm.updateLayerName(idx, data.name)
                            }
                        }
                    }
                },
                onOpen = { id ->
                    // Open and close overlay
                    val entity = allPatches.find { it.name == id }
                    entity?.let {
                        val data = PatchMapper.fromJson(it.jsonSettings)
                        if (data != null) {
                            PatchMapper.applyToVisualSource(data, visualSource)
                            vm.setCurrentPatch(data)
                            val stack = vm.navStack.value
                            val idx = stack.indexOfLast { it.type == LayerType.MANDALA }
                            if (idx != -1) {
                                vm.updateLayerData(idx, MandalaLayerContent(data))
                                vm.updateLayerName(idx, data.name)
                            }
                            onHideManager()
                        }
                    }
                },
                onCreateNew = {
                    vm.startNewPatch(LayerType.MANDALA)
                    onHideManager()
                },
                onRename = { id, newName ->
                    vm.renameSavedPatch(LayerType.MANDALA, id, newName)
                },
                onClone = { id ->
                    vm.cloneSavedPatch(LayerType.MANDALA, id)
                },
                onDelete = { id ->
                    vm.deleteSavedPatch(LayerType.MANDALA, id)
                }
            )
        }
    }
}

private fun randomizeMandala(visualSource: MandalaVisualSource, vm: MandalaViewModel) {
    val random = kotlin.random.Random.Default
    val context = vm.getApplication<android.app.Application>().applicationContext
    val defaultsConfig = llm.slop.spirals.defaults.DefaultsConfig.getInstance(context)

    // Get all defaults
    val mandalaDefaults = defaultsConfig.getMandalaDefaults()
    val recipeDefaults = mandalaDefaults.recipeDefaults
    val armDefaults = mandalaDefaults.armDefaults
    val rotationDefaults = mandalaDefaults.rotationDefaults
    val hueOffsetDefaults = mandalaDefaults.hueOffsetDefaults

    // 1. Random recipe - respecting recipe defaults
    val allRecipes = MandalaLibrary.MandalaRatios
    val filteredRecipes = if (recipeDefaults.preferFavorites) {
        // Try to use favorites if available
        val tagManager = RecipeTagManager(context)
        val favorites = tagManager.getFavorites()

        if (favorites.isNotEmpty()) {
            // Use favorites but filter by petal range
            val favoriteRecipes = allRecipes.filter { it.id in favorites }
            favoriteRecipes.filter {
                it.petals in recipeDefaults.minPetalCount..recipeDefaults.maxPetalCount
            }
        } else {
            // No favorites, just filter by petal range
            allRecipes.filter {
                it.petals in recipeDefaults.minPetalCount..recipeDefaults.maxPetalCount
            }
        }
    } else {
        // Just filter by petal range
        allRecipes.filter {
            it.petals in recipeDefaults.minPetalCount..recipeDefaults.maxPetalCount
        }
    }
    // Use the filtered recipes, or fall back to all recipes if filtering produced empty result
    visualSource.recipe = if (filteredRecipes.isNotEmpty()) {
        filteredRecipes.random(random)
    } else {
        allRecipes.random(random)
    }

    // 2. Hue Sweep = petals (scaled by 9.0 in render code) if auto enabled
    visualSource.parameters["Hue Sweep"]?.let { param ->
        if (recipeDefaults.autoHueSweep) {
            param.baseValue = visualSource.recipe.petals / 9.0f
        } else {
            param.baseValue = random.nextFloat() * 0.5f // Random value between 0-0.5
        }
        param.modulators.clear()
    }

    // 3. L1-L4 (arm lengths)
    listOf("L1", "L2", "L3", "L4").forEach { paramName ->
        visualSource.parameters[paramName]?.let { param ->
            // Base length from range
            val baseLength = random.nextInt(armDefaults.baseLengthMin, armDefaults.baseLengthMax + 1) / 100f
            param.baseValue = baseLength
            param.modulators.clear()

            // Select source based on probabilities
            val sourceRoll = random.nextFloat()
            val sourceId = when {
                sourceRoll < armDefaults.beatProbability -> "beatPhase"
                sourceRoll < armDefaults.beatProbability + armDefaults.lfoProbability -> "lfo1"
                else -> "sampleAndHold"
            }

            // Select waveform based on probabilities
            val waveform = armDefaults.getRandomWaveform(random)

            // Weight/intensity from range
            val weight = random.nextInt(armDefaults.weightMin, armDefaults.weightMax + 1) / 100f

            // Beat division or LFO time based on source
            val subdivision = when (sourceId) {
                "beatPhase" -> {
                    // Beat division from STANDARD_BEAT_VALUES
                    val validValues = STANDARD_BEAT_VALUES.filter {
                        it in armDefaults.beatDivMin..armDefaults.beatDivMax
                    }
                    if (validValues.isNotEmpty()) {
                        validValues.random(random)
                    } else {
                        // Fallback
                        4f
                    }
                }
                "sampleAndHold" -> {
                    // Random CV should also use beat divisions, not arbitrary time
                    val validValues = STANDARD_BEAT_VALUES.filter {
                        it in armDefaults.beatDivMin..armDefaults.beatDivMax
                    }
                    if (validValues.isNotEmpty()) {
                        validValues.random(random)
                    } else {
                        // Fallback
                        4f
                    }
                }
                else -> {
                    // LFO time
                    random.nextInt(armDefaults.lfoTimeMin.toInt(), armDefaults.lfoTimeMax.toInt() + 1).toFloat()
                }
            }

            // Random phase offset
            val phaseOffset = random.nextFloat()

            param.modulators.add(
                CvModulator(
                    sourceId = sourceId,
                    operator = ModulationOperator.ADD,
                    waveform = waveform,
                    slope = 0.5f,
                    weight = weight,
                    phaseOffset = phaseOffset,
                    subdivision = subdivision
                )
            )
        }
    }

    // 4. Rotation
    visualSource.parameters["Rotation"]?.let { param ->
        param.baseValue = 0f
        param.modulators.clear()

        // Direction based on probabilities
        val slope = rotationDefaults.getRandomDirection(random)

        // Source based on probabilities
        val speedSource = rotationDefaults.getRandomSpeedSource(random)
        val sourceId = when(speedSource) {
            SpeedSource.BEAT -> "beatPhase"
            SpeedSource.LFO -> "lfo1"
            SpeedSource.RANDOM -> "sampleAndHold"
        }

        // Subdivision based on source
        val subdivision = when (sourceId) {
            "beatPhase" -> {
                // Beat division from STANDARD_BEAT_VALUES
                val validValues = STANDARD_BEAT_VALUES.filter {
                    it in rotationDefaults.beatDivMin..rotationDefaults.beatDivMax
                }
                if (validValues.isNotEmpty()) {
                    validValues.random(random)
                } else {
                    // Fallback
                    4f
                }
            }
            "sampleAndHold" -> {
                // Random CV should also use beat divisions, not arbitrary time
                val validValues = STANDARD_BEAT_VALUES.filter {
                    it in rotationDefaults.beatDivMin..rotationDefaults.beatDivMax
                }
                if (validValues.isNotEmpty()) {
                    validValues.random(random)
                } else {
                    // Fallback
                    4f
                }
            }
            else -> {
                // LFO time
                random.nextInt(rotationDefaults.lfoTimeMin.toInt(), rotationDefaults.lfoTimeMax.toInt() + 1).toFloat()
            }
        }

        param.modulators.add(
            CvModulator(
                sourceId = sourceId,
                operator = ModulationOperator.ADD,
                waveform = Waveform.TRIANGLE,
                slope = slope,
                weight = 1.0f,
                phaseOffset = random.nextFloat(),
                subdivision = subdivision
            )
        )
    }

    // 5. Hue Offset
    visualSource.parameters["Hue Offset"]?.let { param ->
        param.baseValue = 0f
        param.modulators.clear()

        // Direction based on probabilities
        val slope = hueOffsetDefaults.getRandomDirection(random)

        // Source based on probabilities
        val speedSource = hueOffsetDefaults.getRandomSpeedSource(random)
        val sourceId = when(speedSource) {
            SpeedSource.BEAT -> "beatPhase"
            SpeedSource.LFO -> "lfo1"
            SpeedSource.RANDOM -> "sampleAndHold"
        }

        // Subdivision based on source
        val subdivision = when (sourceId) {
            "beatPhase" -> {
                // Beat division from STANDARD_BEAT_VALUES
                val validValues = STANDARD_BEAT_VALUES.filter {
                    it in hueOffsetDefaults.beatDivMin..hueOffsetDefaults.beatDivMax
                }
                if (validValues.isNotEmpty()) {
                    validValues.random(random)
                } else {
                    // Fallback
                    4f
                }
            }
            "sampleAndHold" -> {
                // Random CV should also use beat divisions, not arbitrary time
                val validValues = STANDARD_BEAT_VALUES.filter {
                    it in hueOffsetDefaults.beatDivMin..hueOffsetDefaults.beatDivMax
                }
                if (validValues.isNotEmpty()) {
                    validValues.random(random)
                } else {
                    // Fallback
                    4f
                }
            }
            else -> {
                // LFO time
                random.nextInt(hueOffsetDefaults.lfoTimeMin.toInt(), hueOffsetDefaults.lfoTimeMax.toInt() + 1).toFloat()
            }
        }

        param.modulators.add(
            CvModulator(
                sourceId = sourceId,
                operator = ModulationOperator.ADD,
                waveform = Waveform.TRIANGLE,
                slope = slope,
                weight = 1.0f,
                phaseOffset = random.nextFloat(),
                subdivision = subdivision
            )
        )
    }
}
