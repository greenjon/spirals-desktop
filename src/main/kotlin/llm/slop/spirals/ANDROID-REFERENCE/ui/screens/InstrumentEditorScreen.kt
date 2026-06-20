package llm.slop.spirals.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import llm.slop.spirals.MandalaVisualSource
import llm.slop.spirals.MandalaViewModel
import llm.slop.spirals.ui.components.ModulatorRow
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText

@Composable
fun InstrumentEditorScreen(
    source: MandalaVisualSource,
    vm: MandalaViewModel,
    focusedId: String,
    onFocusChange: (String) -> Unit,
    onInteractionFinished: () -> Unit
) {
    val scrollState = rememberScrollState()
    val focusedParam = source.parameters[focusedId] ?: source.globalAlpha
    
    var refreshCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
            key(focusedId, refreshCount) {
                focusedParam.modulators.forEachIndexed { index, mod ->
                    ModulatorRow(
                        mod = mod,
                        onUpdate = { updatedMod ->
                            focusedParam.modulators[index] = updatedMod
                        },
                        onInteractionFinished = onInteractionFinished,
                        onRemove = { 
                            focusedParam.modulators.removeAt(index)
                            refreshCount++
                            onInteractionFinished()
                        }
                    )
                    HorizontalDivider(color = AppText.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                }

                ModulatorRow(
                    mod = null,
                    onUpdate = { newMod ->
                        focusedParam.modulators.add(newMod)
                        refreshCount++
                        onInteractionFinished()
                    },
                    onInteractionFinished = onInteractionFinished,
                    onRemove = {}
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
