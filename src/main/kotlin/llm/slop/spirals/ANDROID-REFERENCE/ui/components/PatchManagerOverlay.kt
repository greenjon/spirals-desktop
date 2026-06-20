package llm.slop.spirals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatchManagerOverlay(
    title: String,
    patches: List<Pair<String, String>>, // Name, ID or JSON
    selectedId: String?,
    onSelect: (String) -> Unit, // Preview instantly
    onOpen: ((String) -> Unit)? = null, // Open and close overlay
    onCreateNew: () -> Unit, // Create new item
    onRename: (id: String, newName: String) -> Unit,
    onClone: (String) -> Unit,
    onDelete: (String) -> Unit,
    // Navigation for Sets/Shows
    navigationLabel: String? = null, // e.g., "Mandala" or "Mixer"
    navigationIndex: Int? = null,
    navigationTotal: Int? = null,
    onNavigatePrev: (() -> Unit)? = null,
    onNavigateNext: (() -> Unit)? = null
) {
    var showLongPressMenu by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .background(AppBackground)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = AppText)
                Button(
                    onClick = onCreateNew,
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create New")
                }
            }

            // Navigation controls (only for Sets/Shows)
            if (navigationLabel != null && navigationIndex != null && navigationTotal != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onNavigatePrev?.invoke() },
                        enabled = navigationIndex > 0 && onNavigatePrev != null,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("◀ Prev", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = "$navigationLabel ${navigationIndex + 1}/$navigationTotal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppText
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    OutlinedButton(
                        onClick = { onNavigateNext?.invoke() },
                        enabled = navigationIndex < navigationTotal - 1 && onNavigateNext != null,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Next ▶", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (patches.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No saved items found.", color = AppText.copy(alpha = 0.5f))
                }
            } else {
                FlowRow(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    patches.forEach { (name, id) ->
                        val isSelected = id == selectedId
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected) AppAccent.copy(alpha = 0.2f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) AppAccent else AppText.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .pointerInput(id) {
                                    detectTapGestures(
                                        onTap = { onSelect(id) },
                                        onLongPress = { showLongPressMenu = id }
                                    )
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) AppAccent else AppText,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { showLongPressMenu = id },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = AppText.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Long Press Menu
    showLongPressMenu?.let { id ->
        val name = patches.find { it.second == id }?.first ?: ""
        AlertDialog(
            onDismissRequest = { showLongPressMenu = null },
            title = { Text(name, color = AppText, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    // Open option (if provided)
                    onOpen?.let { openCallback ->
                        ListItem(
                            headlineContent = { Text("Open", color = AppAccent) },
                            modifier = Modifier.clickable { 
                                // Pass the id for consistent unique identification
                                openCallback(id)
                                showLongPressMenu = null
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(color = AppText.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                    
                    ListItem(
                        headlineContent = { Text("Rename", color = AppText) },
                        modifier = Modifier.clickable { 
                            showRenameDialog = id
                            showLongPressMenu = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Clone", color = AppText) },
                        modifier = Modifier.clickable { 
                            onClone(id)
                            showLongPressMenu = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    HorizontalDivider(color = AppText.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    
                    ListItem(
                        headlineContent = { Text("Delete", color = Color.Red) },
                        modifier = Modifier.clickable { 
                            showDeleteConfirm = id
                            showLongPressMenu = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            },
            confirmButton = {},
            containerColor = AppBackground
        )
    }

    // Rename Dialog
    showRenameDialog?.let { id ->
        var newName by remember { mutableStateOf(patches.find { it.second == id }?.first ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename", color = AppText) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        focusedTextColor = AppText,
                        unfocusedTextColor = AppText,
                        cursorColor = AppAccent
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    onRename(id, newName)
                    showRenameDialog = null 
                }) {
                    Text("OK", color = AppAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel", color = AppText)
                }
            },
            containerColor = AppBackground
        )
    }

    // Delete Confirmation
    showDeleteConfirm?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Patch?", color = AppText) },
            text = { Text("This action cannot be undone.", color = AppText) },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete(id)
                    showDeleteConfirm = null 
                }) {
                    Text("DELETE", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("CANCEL", color = AppText)
                }
            },
            containerColor = AppBackground
        )
    }
}
