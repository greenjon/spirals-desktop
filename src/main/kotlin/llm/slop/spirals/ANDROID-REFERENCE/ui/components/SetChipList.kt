package llm.slop.spirals.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SetChipList(
    chipItems: List<Pair<String, String>>,
    onChipTapped: (String) -> Unit,
    onChipReordered: (List<String>) -> Unit,
    onChipDeleted: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chipItems.forEachIndexed { index, chipItem ->
            val (displayName, chipId) = chipItem
            
            Chip(
                displayName = displayName,
                chipId = chipId,
                onChipTapped = onChipTapped,
                onChipDeleted = onChipDeleted,
                onMoveUp = {
                    if (index > 0) {
                        val reordered = chipItems.toMutableList()
                        val item = reordered.removeAt(index)
                        reordered.add(index - 1, item)
                        onChipReordered(reordered.map { it.second })
                    }
                },
                onMoveDown = {
                    if (index < chipItems.size - 1) {
                        val reordered = chipItems.toMutableList()
                        val item = reordered.removeAt(index)
                        reordered.add(index + 1, item)
                        onChipReordered(reordered.map { it.second })
                    }
                },
                canMoveUp = index > 0,
                canMoveDown = index < chipItems.size - 1
            )
        }
    }
}

@Composable
private fun Chip(
    displayName: String,
    chipId: String,
    onChipTapped: (String) -> Unit,
    onChipDeleted: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    val chipBackground = Color(0xFFEEE8D5)
    val chipTextColor = Color(0xFF360B00)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChipTapped(chipId) },
        colors = CardDefaults.cardColors(containerColor = chipBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp).fillMaxWidth()
        ) {
            Text(
                text = displayName,
                color = chipTextColor,
                modifier = Modifier.padding(vertical = 8.dp).weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = chipTextColor.copy(alpha = 0.6f))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (canMoveUp) {
                        DropdownMenuItem(
                            text = { Text("Move Up") },
                            onClick = {
                                onMoveUp()
                                menuExpanded = false
                            }
                        )
                    }
                    if (canMoveDown) {
                        DropdownMenuItem(
                            text = { Text("Move Down") },
                            onClick = {
                                onMoveDown()
                                menuExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onChipDeleted(chipId)
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}