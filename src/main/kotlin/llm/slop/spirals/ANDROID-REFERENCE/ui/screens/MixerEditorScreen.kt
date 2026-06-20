package llm.slop.spirals.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import llm.slop.spirals.*
import llm.slop.spirals.models.*
import llm.slop.spirals.display.LocalSpiralRenderer
import llm.slop.spirals.ui.components.* // Keep existing wildcard import
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixerEditorScreen(
    vm: MandalaViewModel = viewModel(),
    onClose: () -> Unit,
    onNavigateToSetEditor: (Boolean) -> Unit,
    onNavigateToMandalaEditor: (Boolean) -> Unit,
    onShowCvLab: () -> Unit,
    previewContent: @Composable () -> Unit,
    showManager: Boolean = false,
    onHideManager: () -> Unit = {}
) {
    val allMixerPatches by vm.allMixerPatches.collectAsState(initial = emptyList())
    val allSets by vm.allSets.collectAsState(initial = emptyList())
    val allPatches by vm.allPatches.collectAsState(initial = emptyList())
    val allRandomSets by vm.allRandomSets.collectAsState(initial = emptyList())

    val navStack by vm.navStack.collectAsState()
    val layer = navStack.lastOrNull { it.type == LayerType.MIXER }

    var currentPatch by remember {
        mutableStateOf((layer?.data as? MixerLayerContent)?.mixer ?: MixerPatch(name = "New Mixer", slots = List(4) { MixerSlotData() }))
    }

    LaunchedEffect(layer?.data) {
        (layer?.data as? MixerLayerContent)?.mixer?.let {
            if (it.id != currentPatch.id) {
                currentPatch = it
            }
        }
    }

    var monitorSource by remember { mutableStateOf("F") }
    var focusedParameterId by remember { mutableStateOf("PN1") }

    var showOpenDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSetPickerForSlot by remember { mutableStateOf<Int?>(null) }
    var showMandalaPickerForSlot by remember { mutableStateOf<Int?>(null) }
    var showRandomSetPickerForSlot by remember { mutableStateOf<Int?>(null) }

    // Generation triggers for random sets (one per slot)
    var randomSetTriggers by remember { mutableStateOf(List(4) { 0 }) }

    val mainRenderer = LocalSpiralRenderer.current

    var frameTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            frameTick++
            delay(16)
        }
    }

    LaunchedEffect(currentPatch) {
        val index = navStack.indexOfLast { it.type == LayerType.MIXER }
        if (index != -1) {
            vm.updateLayerData(index, MixerLayerContent(currentPatch))
        }
    }

    LaunchedEffect(mainRenderer, currentPatch, monitorSource, allSets, allPatches, allRandomSets, randomSetTriggers) {
        if (mainRenderer == null) return@LaunchedEffect
        mainRenderer.mixerPatch = currentPatch
        mainRenderer.monitorSource = monitorSource

        currentPatch.slots.forEachIndexed { index, slot ->
            val source = mainRenderer.getSlotSource(index)

            when (slot.sourceType) {
                VideoSourceType.MANDALA_SET -> {
                    val setEntity = allSets.find { it.id == slot.mandalaSetId }
                    setEntity?.let { se ->
                        val orderedIds = try {
                            Json.decodeFromString<List<String>>(se.jsonOrderedMandalaIds)
                        } catch (e: Exception) {
                            emptyList()
                        }
                        if (orderedIds.isNotEmpty()) {
                            val safeIndex = slot.currentIndex.baseValue.toInt().coerceIn(0, orderedIds.size - 1)
                            val patchEntity = allPatches.find { it.name == orderedIds[safeIndex] }
                            patchEntity?.let { pe ->
                                val patchData = PatchMapper.fromJson(pe.jsonSettings)
                                patchData?.let { pd ->
                                    PatchMapper.applyToVisualSource(pd, source)
                                }
                            }
                        }
                    }
                }
                VideoSourceType.MANDALA -> {
                    val patchEntity = allPatches.find { it.name == slot.selectedMandalaId }
                    patchEntity?.let { pe ->
                        val patchData = PatchMapper.fromJson(pe.jsonSettings)
                        patchData?.let { pd ->
                            PatchMapper.applyToVisualSource(pd, source)
                        }
                    }
                }
                VideoSourceType.RANDOM_SET -> {
                    val rsetEntity = allRandomSets.find { it.id == slot.randomSetId }
                    rsetEntity?.let { re ->
                        val randomSet = Json.decodeFromString<RandomSet>(re.jsonSettings)
                        val generator = RandomSetGenerator(vm.getApplication())
                        // Only regenerate when trigger changes (randomSetTriggers[index] is in dependencies)
                        generator.generateFromRSet(randomSet, source)
                    }
                }
                VideoSourceType.COLOR -> {
                    // Color source handled elsewhere
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .aspectRatio(16 / 9f)
                    .background(Color.Black)
                    .border(1.dp, AppText.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                previewContent()

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("1", "2", "3", "4", "A", "B", "F").forEach { src ->
                        val isSelected = monitorSource == src
                        Surface(
                            color = if (isSelected) AppAccent else AppBackground.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.clickable { monitorSource = src }
                        ) {
                            Text(
                                text = src,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else AppText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(60.dp).border(1.dp, AppText.copy(alpha = 0.1f))) {
                key(frameTick) {
                    val targetParam = mainRenderer?.getMixerParam(focusedParameterId)
                    if (targetParam != null) {
                        OscilloscopeView(history = targetParam.history, modifier = Modifier.fillMaxSize())
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 4.dp)
            ) {
                Column(modifier = Modifier.weight(3f)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SourceStrip(0, currentPatch, { currentPatch = it }, mainRenderer, { showSetPickerForSlot = 0 }, { showMandalaPickerForSlot = 0 }, { showRandomSetPickerForSlot = 0 }, { randomSetTriggers = randomSetTriggers.toMutableList().apply { this[0]++ } }, allSets, allRandomSets, "1", Alignment.TopEnd, focusedParameterId, { focusedParameterId = it }, Modifier.weight(1f))
                        MonitorStrip("A", currentPatch, { currentPatch = it }, mainRenderer, false, true, {}, focusedParameterId, { focusedParameterId = it }, Modifier.weight(1f))
                        SourceStrip(1, currentPatch, { currentPatch = it }, mainRenderer, { showSetPickerForSlot = 1 }, { showMandalaPickerForSlot = 1 }, { showRandomSetPickerForSlot = 1 }, { randomSetTriggers = randomSetTriggers.toMutableList().apply { this[1]++ } }, allSets, allRandomSets, "2", Alignment.TopStart, focusedParameterId, { focusedParameterId = it }, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SourceStrip(2, currentPatch, { currentPatch = it }, mainRenderer, { showSetPickerForSlot = 2 }, { showMandalaPickerForSlot = 2 }, { showRandomSetPickerForSlot = 2 }, { randomSetTriggers = randomSetTriggers.toMutableList().apply { this[2]++ } }, allSets, allRandomSets, "3", Alignment.TopEnd, focusedParameterId, { focusedParameterId = it }, Modifier.weight(1f))
                        MonitorStrip("B", currentPatch, { currentPatch = it }, mainRenderer, false, true, {}, focusedParameterId, { focusedParameterId = it }, Modifier.weight(1f))
                        SourceStrip(3, currentPatch, { currentPatch = it }, mainRenderer, { showSetPickerForSlot = 3 }, { showMandalaPickerForSlot = 3 }, { showRandomSetPickerForSlot = 3 }, { randomSetTriggers = randomSetTriggers.toMutableList().apply { this[3]++ } }, allSets, allRandomSets, "4", Alignment.TopStart, focusedParameterId, { focusedParameterId = it }, Modifier.weight(1f))
                    }
                }
                MonitorStrip("F", currentPatch, { currentPatch = it }, mainRenderer, false, true, {}, focusedParameterId, { focusedParameterId = it }, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                MixerCvEditor(
                    patch = currentPatch,
                    focusedId = focusedParameterId,
                    onPatchUpdate = { currentPatch = it }
                )
            }
        }

        if (showManager) {
            PatchManagerOverlay(
                title = "Manage Mixers",
                patches = allMixerPatches.map { it.name to it.id },
                selectedId = currentPatch.id,
                onSelect = { id ->
                    val entity = allMixerPatches.find { it.id == id }
                    if (entity != null) {
                        try {
                            val selected = Json.decodeFromString<MixerPatch>(entity.jsonSettings)
                            currentPatch = selected
                            val idx = navStack.indexOfLast { it.type == LayerType.MIXER }
                            if (idx != -1) vm.updateLayerName(idx, selected.name)
                        } catch (e: Exception) {}
                    }
                },
                onOpen = { id ->
                    val entity = allMixerPatches.find { it.id == id }
                    if (entity != null) {
                        try {
                            val selected = Json.decodeFromString<MixerPatch>(entity.jsonSettings)
                            currentPatch = selected
                            val idx = navStack.indexOfLast { it.type == LayerType.MIXER }
                            if (idx != -1) vm.updateLayerName(idx, selected.name)
                            onHideManager()
                        } catch (e: Exception) {}
                    }
                },
                onCreateNew = {
                    vm.startNewPatch(LayerType.MIXER)
                    onHideManager()
                },
                onRename = { id, newName ->
                    vm.renameSavedPatch(LayerType.MIXER, id, newName)
                },
                onClone = { id ->
                    vm.cloneSavedPatch(LayerType.MIXER, id)
                },
                onDelete = { id ->
                    vm.deleteSavedPatch(LayerType.MIXER, id)
                }
            )
        }
    }

    showSetPickerForSlot?.let { idx: Int ->
        PickerDialog(
            title = "Select Mandala Set",
            items = allSets.map { it.name to it.id },
            onSelect = { id: String ->
                val newSlots = currentPatch.slots.toMutableList()
                newSlots[idx] = newSlots[idx].copy(mandalaSetId = id, currentIndex = ModulatableParameterData(0f), sourceType = VideoSourceType.MANDALA_SET)
                currentPatch = currentPatch.copy(slots = newSlots)
                showSetPickerForSlot = null
            },
            onDismiss = { showSetPickerForSlot = null },
            onCreateNew = {
                vm.createAndPushLayer(LayerType.SET, parentSlotIndex = idx)
                showSetPickerForSlot = null
            }
        )
    }

    showMandalaPickerForSlot?.let { idx: Int ->
        PickerDialog(
            title = "Select Mandala",
            items = allPatches.map { it.name to it.name },
            onSelect = { id: String ->
                val newSlots = currentPatch.slots.toMutableList()
                newSlots[idx] = newSlots[idx].copy(selectedMandalaId = id, sourceType = VideoSourceType.MANDALA)
                currentPatch = currentPatch.copy(slots = newSlots)
                showMandalaPickerForSlot = null
            },
            onDismiss = { showMandalaPickerForSlot = null },
            onCreateNew = {
                vm.createAndPushLayer(LayerType.MANDALA, parentSlotIndex = idx)
                showMandalaPickerForSlot = null
            }
        )
    }

    showRandomSetPickerForSlot?.let { idx: Int ->
        PickerDialog(
            title = "Select Random Set",
            items = allRandomSets.map { it.name to it.id },
            onSelect = { id: String ->
                val newSlots = currentPatch.slots.toMutableList()
                newSlots[idx] = newSlots[idx].copy(randomSetId = id, sourceType = VideoSourceType.RANDOM_SET)
                currentPatch = currentPatch.copy(slots = newSlots)
                showRandomSetPickerForSlot = null
            },
            onDismiss = { showRandomSetPickerForSlot = null },
            onCreateNew = {
                vm.createAndPushLayer(LayerType.RANDOM_SET, parentSlotIndex = idx)
                showRandomSetPickerForSlot = null
            }
        )
    }

    if (showOpenDialog) {
        PickerDialog(
            title = "Open Mixer Patch",
            items = allMixerPatches.map { it.name to it.jsonSettings },
            onSelect = { json: String ->
                try {
                    currentPatch = Json.decodeFromString(json)
                } catch (e: Exception) { }
                showOpenDialog = false
            },
            onDismiss = { showOpenDialog = false }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(currentPatch.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Mixer") },
            text = { TextField(value = newName, onValueChange = { newName = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { currentPatch = currentPatch.copy(name = newName); showRenameDialog = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } },
            containerColor = AppBackground, titleContentColor = AppText, textContentColor = AppText
        )
    }
}
