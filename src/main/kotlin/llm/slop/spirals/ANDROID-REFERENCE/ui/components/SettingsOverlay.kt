package llm.slop.spirals.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import llm.slop.spirals.StartupMode
import llm.slop.spirals.defaults.DefaultsConfig
import llm.slop.spirals.display.ExternalDisplayCoordinator
import llm.slop.spirals.ui.settings.MandalaDefaultsScreen
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText

@Composable
fun SettingsOverlay(
    currentMode: StartupMode,
    onModeChange: (StartupMode) -> Unit,
    onClose: () -> Unit,
    displayCoordinator: ExternalDisplayCoordinator
) {
    val context = LocalContext.current
    val defaultsConfig = remember { DefaultsConfig.getInstance(context) }
    var showGlobalDefaults by remember { mutableStateOf(false) }

    if (showGlobalDefaults) {
        Dialog(
            onDismissRequest = { showGlobalDefaults = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = AppBackground,
                border = BorderStroke(1.dp, AppText.copy(alpha = 0.1f))
            ) {
                MandalaDefaultsScreen(
                    defaultsConfig = defaultsConfig,
                    onClose = { showGlobalDefaults = false }
                )
            }
        }
    } else {
        Dialog(onDismissRequest = onClose) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.medium,
                color = AppBackground,
                border = BorderStroke(1.dp, AppText.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Settings", style = MaterialTheme.typography.headlineSmall, color = AppText)
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = AppText)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // HDMI Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("HDMI Output", style = MaterialTheme.typography.titleMedium, color = AppText)
                            Text("Enable external display", style = MaterialTheme.typography.bodySmall, color = AppText.copy(alpha = 0.6f))
                        }
                        var hdmiEnabled by remember { mutableStateOf(defaultsConfig.isHdmiEnabled()) }
                        Switch(
                            checked = hdmiEnabled,
                            onCheckedChange = {
                                hdmiEnabled = it
                                defaultsConfig.setHdmiEnabled(it)
                                displayCoordinator.updatePresentation()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = AppAccent)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = AppText.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Start app with...", style = MaterialTheme.typography.titleMedium, color = AppText)

                    Spacer(modifier = Modifier.height(12.dp))

                    StartupMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onModeChange(mode) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentMode == mode,
                                onClick = { onModeChange(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = AppAccent, unselectedColor = AppText.copy(alpha = 0.6f))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = when(mode) {
                                    StartupMode.LAST_WORKSPACE -> "Last Workspace"
                                    StartupMode.MIXER -> "Mixer Editor"
                                    StartupMode.SET -> "Set Editor"
                                    StartupMode.MANDALA -> "Mandala Editor"
                                    StartupMode.SHOW -> "Show Editor"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = AppText.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showGlobalDefaults = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mandala Defaults")
                    }
                }
            }
        }
    }
}
