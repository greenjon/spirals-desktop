package llm.slop.spirals.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import llm.slop.spirals.database.entities.MandalaPatchEntity
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppText

@Composable
fun MandalaPicker(
    patches: List<MandalaPatchEntity>,
    onPatchSelected: (String) -> Unit, // For previewing
    onPatchAdded: (String) -> Unit, // For adding to the set
    onCreateNew: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Add Mandala to Set", color = AppText, modifier = Modifier.padding(bottom = 12.dp))
            if (onCreateNew != null) {
                IconButton(onClick = onCreateNew) {
                    Icon(Icons.Default.Add, contentDescription = "Create New", tint = AppAccent)
                }
            }
        }

        if (onCreateNew != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreateNew() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = AppAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Create new...", style = MaterialTheme.typography.bodyLarge, color = AppAccent)
            }
            HorizontalDivider(color = AppText.copy(alpha = 0.1f))
        }
        
        patches.forEach { patch ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPatchSelected(patch.name) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(patch.name, color = AppText, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { onPatchAdded(patch.name) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add to set")
                }
            }
        }
        if (patches.isEmpty() && onCreateNew == null) {
            Text("No mandalas saved yet.", color = AppText.copy(alpha = 0.5f), modifier = Modifier.padding(12.dp))
        }
    }
}
