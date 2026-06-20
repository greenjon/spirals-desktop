package llm.slop.spirals.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import llm.slop.spirals.MandalaViewModel
import llm.slop.spirals.models.PatchData
import llm.slop.spirals.PatchMapper
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText

@Composable
fun OpenPatchDialog(vm: MandalaViewModel, onPatchSelected: (PatchData) -> Unit, onDismiss: () -> Unit) {
    val patches by vm.allPatches.collectAsState(initial = emptyList())
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, color = AppBackground) {
            Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.7f)) {
                Text("Saved Patches", style = MaterialTheme.typography.titleLarge, color = AppText)
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    patches.forEach { entity ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { 
                            val patchData = PatchMapper.fromJson(entity.jsonSettings)
                            if (patchData != null) onPatchSelected(patchData) 
                        }.padding(12.dp)) {
                            Text(entity.name, style = MaterialTheme.typography.bodyLarge, color = AppText)
                        }
                    }
                }
                if (patches.isEmpty()) Text("No patches saved yet.", color = AppText.copy(alpha = 0.5f))
            }
        }
    }
}
