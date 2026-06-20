package llm.slop.spirals.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText

@Composable
fun PickerDialog(
    title: String,
    items: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreateNew: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = AppBackground) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = AppText)
                LazyColumn(modifier = Modifier.weight(1f, fill = false).padding(vertical = 8.dp)) {
                    items(items) { (name, id) ->
                        Text(
                            text = name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(id) }
                                .padding(vertical = 12.dp),
                            color = AppText
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    if (onCreateNew != null) {
                        Button(
                            onClick = onCreateNew,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Create New")
                        }
                    }
                }
            }
        }
    }
}
