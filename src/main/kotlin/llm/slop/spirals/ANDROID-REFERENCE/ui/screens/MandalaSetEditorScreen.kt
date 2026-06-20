package llm.slop.spirals.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.json.Json
import llm.slop.spirals.*
import llm.slop.spirals.models.set.MandalaSet
import llm.slop.spirals.models.PatchData
import llm.slop.spirals.models.set.SelectionPolicy
import llm.slop.spirals.ui.components.RecipePickerDialog
import llm.slop.spirals.ui.components.MandalaPicker
import llm.slop.spirals.ui.components.PatchManagerOverlay
import llm.slop.spirals.ui.components.SetChipList
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandalaSetEditorScreen(
    vm: MandalaViewModel = viewModel(),
    onClose: () -> Unit,
    onNavigateToMixerEditor: () -> Unit,
    onShowCvLab: () -> Unit,
    previewContent: @Composable () -> Unit,
    visualSource: MandalaVisualSource,
    showManager: Boolean = false,
    onHideManager: () -> Unit = {}
) {
    val allSets by vm.allSets.collectAsState(initial = emptyList())
    val allPatches by vm.allPatches.collectAsState(initial = emptyList())

    // Initialize from layer data if available
    val navStack by vm.navStack.collectAsState()
    val layer = navStack.lastOrNull { it.type == LayerType.SET }

    var currentSet by remember { mutableStateOf((layer?.data as? SetLayerContent)?.set) }
    var focusedMandalaId by remember { mutableStateOf<String?>(null) }
    var showMandalaPicker by remember { mutableStateOf(false) }

    var showOpenDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    // Update local state if nav data changes (e.g. from Manage overlay)
    LaunchedEffect(layer?.data) {
        (layer?.data as? SetLayerContent)?.set?.let {
            if (it.id != (currentSet?.id ?: "")) {
                currentSet = it
                focusedMandalaId = it.orderedMandalaIds.firstOrNull()
            }
        }
    }

    // Logic to update the preview when the selected mandala in the set changes
    LaunchedEffect(currentSet, focusedMandalaId, allPatches) {
        if (currentSet == null || focusedMandalaId == null) {
            visualSource.globalAlpha.baseValue = 0f
        } else {
            val patchEntity = allPatches.find { it.name == focusedMandalaId }
            patchEntity?.let { entity ->
                val patchData = PatchMapper.fromJson(entity.jsonSettings)
                patchData?.let { data ->
                    PatchMapper.applyToVisualSource(data, visualSource)
                    visualSource.globalAlpha.baseValue = 1f
                }
            }
        }
    }

    fun selectSet(setId: String) {
        val entity = allSets.find { it.id == setId } ?: return
        val newSet = MandalaSet(
            id = entity.id,
            name = entity.name,
            orderedMandalaIds = Json.decodeFromString(entity.jsonOrderedMandalaIds),
            selectionPolicy = SelectionPolicy.valueOf(entity.selectionPolicy)
        )
        currentSet = newSet
        focusedMandalaId = newSet.orderedMandalaIds.firstOrNull()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Internal Header removed - breadcrumbs handle it

                // Always show preview window area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    previewContent()
                }

                Spacer(modifier = Modifier.height(16.dp))

                val activeSet = currentSet
                if (activeSet == null) {
                    Column {
                        Text("Existing Sets:", style = MaterialTheme.typography.titleMedium, color = AppText)
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            allSets.forEach { setEntity ->
                                Text(
                                    text = setEntity.name,
                                    color = AppAccent,
                                    modifier = Modifier.clickable { selectSet(setEntity.id) }.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Set Editor View
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { showMandalaPicker = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Mandala")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Policy Selector Dropdown
                            var policyExpanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { policyExpanded = true },
                                    shape = MaterialTheme.shapes.extraSmall,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(activeSet.selectionPolicy.name, style = MaterialTheme.typography.labelSmall, color = AppText)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AppText)
                                }
                                DropdownMenu(
                                    expanded = policyExpanded,
                                    onDismissRequest = { policyExpanded = false },
                                    containerColor = AppBackground
                                ) {
                                    SelectionPolicy.entries.forEach { policy ->
                                        DropdownMenuItem(
                                            text = { Text(policy.name, style = MaterialTheme.typography.labelSmall) },
                                            onClick = {
                                                currentSet = activeSet.copy(selectionPolicy = policy)
                                                policyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // For mandala sets, the mandala name is used as both display text and ID
                        val mandalaItems = remember(activeSet.orderedMandalaIds) {
                            activeSet.orderedMandalaIds.map { mandalaName -> mandalaName to mandalaName }
                        }

                        SetChipList(
                            chipItems = mandalaItems,
                            onChipTapped = { focusedMandalaId = it },
                            onChipReordered = { newList ->
                                // newList contains mandala names in the new order
                                currentSet = activeSet.copy(orderedMandalaIds = newList.toMutableList())
                            },
                            onChipDeleted = { mandalaId ->
                                val newList = activeSet.orderedMandalaIds.toMutableList()
                                newList.remove(mandalaId)
                                currentSet = activeSet.copy(orderedMandalaIds = newList)
                                if (focusedMandalaId == mandalaId) {
                                    focusedMandalaId = newList.firstOrNull()
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showManager) {
            val mandalaIds = currentSet?.orderedMandalaIds ?: emptyList()
            val currentMandalaIndex = mandalaIds.indexOf(focusedMandalaId).takeIf { it >= 0 } ?: 0

            PatchManagerOverlay(
                title = "Manage Sets",
                patches = allSets.map { it.name to it.id },
                selectedId = currentSet?.id,
                onSelect = { id ->
                    // Preview instantly on tap
                    selectSet(id)
                    val set = allSets.find { it.id == id }
                    if (set != null) {
                        val idx = navStack.indexOfLast { it.type == LayerType.SET }
                        if (idx != -1) vm.updateLayerName(idx, set.name)
                    }
                },
                onOpen = { id ->
                    // Open and close overlay
                    selectSet(id)
                    val set = allSets.find { it.id == id }
                    if (set != null) {
                        val idx = navStack.indexOfLast { it.type == LayerType.SET }
                        if (idx != -1) vm.updateLayerName(idx, set.name)
                        onHideManager()
                    }
                },
                onCreateNew = {
                    vm.startNewPatch(LayerType.SET)
                    onHideManager()
                },
                onRename = { id, newName ->
                    vm.renameSavedPatch(LayerType.SET, id, newName)
                },
                onClone = { id ->
                    vm.cloneSavedPatch(LayerType.SET, id)
                },
                onDelete = { id ->
                    vm.deleteSavedPatch(LayerType.SET, id)
                },
                // Navigation through mandalas in the set
                navigationLabel = if (mandalaIds.isNotEmpty()) "Mandala" else null,
                navigationIndex = if (mandalaIds.isNotEmpty()) currentMandalaIndex else null,
                navigationTotal = if (mandalaIds.isNotEmpty()) mandalaIds.size else null,
                onNavigatePrev = if (mandalaIds.isNotEmpty() && currentMandalaIndex > 0) {
                    { focusedMandalaId = mandalaIds[currentMandalaIndex - 1] }
                } else null,
                onNavigateNext = if (mandalaIds.isNotEmpty() && currentMandalaIndex < mandalaIds.size - 1) {
                    { focusedMandalaId = mandalaIds[currentMandalaIndex + 1] }
                } else null
            )
        }
    }

    if (showMandalaPicker) {
        ModalBottomSheet(onDismissRequest = { showMandalaPicker = false }) {
            MandalaPicker(
                patches = allPatches,
                onPatchSelected = { patchName ->
                    focusedMandalaId = patchName
                },
                onPatchAdded = { patchName ->
                    currentSet = currentSet?.copy(
                        orderedMandalaIds = (currentSet?.orderedMandalaIds ?: mutableListOf()).toMutableList().apply { add(patchName) }
                    )
                    focusedMandalaId = patchName
                },
                onCreateNew = {
                    vm.createAndPushLayer(LayerType.MANDALA)
                    showMandalaPicker = false
                }
            )
        }
    }

    if (showOpenDialog) {
        // This is a placeholder, as RecipePickerDialog is not a direct replacement for a set picker
        // A new component `SetPickerDialog` would be needed. For now, I'm commenting it out to fix the build.
        /*
        RecipePickerDialog(
            title = "Open Mixer Patch",
            items = allSets.map { it.name to it.id },
            onSelect = { id: String ->
                selectSet(id)
                showOpenDialog = false
            },
            onDismiss = { showOpenDialog = false }
        )
        */
    }

    if (showRenameDialog) {
        var name by remember { mutableStateOf(currentSet?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Set", color = AppText) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedTextColor = AppText,
                        focusedTextColor = AppText,
                        cursorColor = AppAccent
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    currentSet = currentSet?.copy(name = name)
                    showRenameDialog = false
                }) {
                    Text("RENAME", color = AppAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("CANCEL", color = AppText)
                }
            },
            containerColor = AppBackground
        )
    }

    // Capture work-in-progress back to VM
    LaunchedEffect(currentSet) {
        val stack = vm.navStack.value
        val index = stack.indexOfLast { it.type == LayerType.SET }
        val setToUpdate = currentSet
        if (index != -1 && setToUpdate != null) {
            vm.updateLayerData(index, SetLayerContent(setToUpdate))
        }
    }
}