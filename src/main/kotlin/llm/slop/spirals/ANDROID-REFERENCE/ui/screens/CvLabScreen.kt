package llm.slop.spirals.ui.screens

import android.Manifest
import android.app.Activity
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import llm.slop.spirals.cv.core.ModulationRegistry
import llm.slop.spirals.cv.processors.AudioEngine
import llm.slop.spirals.cv.processors.AudioSourceManager
import llm.slop.spirals.cv.processors.AudioSourceType
import llm.slop.spirals.cv.visualizers.CvHistoryBuffer
import llm.slop.spirals.ui.components.OscilloscopeView
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun CvLabScreen(
    audioEngine: AudioEngine, 
    sourceManager: AudioSourceManager,
    audioSourceType: AudioSourceType,
    onAudioSourceTypeChange: (AudioSourceType) -> Unit,
    hasMicPermission: Boolean,
    onMicPermissionGranted: () -> Unit,
    onInternalAudioRecordCreated: (AudioRecord) -> Unit,
    onClose: () -> Unit
) {
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onMicPermissionGranted()
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val projection = sourceManager.getMediaProjection(result.resultCode, data)
                val record = sourceManager.buildAudioRecord(
                    AudioSourceType.INTERNAL,
                    44100,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT),
                    projection
                )
                if (record != null) onInternalAudioRecordCreated(record)
            }
        }
    }

    // A simple tick that forces the UI to recompose at 60Hz so the scopes update
    var frameTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            frameTick++
            delay(16)
        }
    }

    // Secondary buffer for the synthesized beat visualization
    val beatVisualBuffer = remember { CvHistoryBuffer(200) }
    LaunchedEffect(frameTick) {
        val beats = ModulationRegistry.getSynchronizedTotalBeats()
        // Synthesize a sine wave synced to the beat phase
        val value = (sin(beats * 2.0 * Math.PI).toFloat() + 1.0f) * 0.5f
        beatVisualBuffer.add(value)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CV Lab", style = MaterialTheme.typography.headlineSmall, color = AppText)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = AppText)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Input Selector
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Audio Input: ", color = AppText, style = MaterialTheme.typography.labelSmall)
            CvSegmentedButton(
                options = listOf("Mic", "Raw", "Internal"),
                selected = when(audioSourceType) {
                    AudioSourceType.MIC -> "Mic"
                    AudioSourceType.UNPROCESSED -> "Raw"
                    AudioSourceType.INTERNAL -> "Internal"
                },
                onSelect = { selected ->
                    when (selected) {
                        "Mic" -> { 
                            onAudioSourceTypeChange(AudioSourceType.MIC)
                            if (!hasMicPermission) micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) 
                        }
                        "Raw" -> {
                            onAudioSourceTypeChange(AudioSourceType.UNPROCESSED)
                            if (!hasMicPermission) micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) 
                        }
                        "Internal" -> { 
                            onAudioSourceTypeChange(AudioSourceType.INTERNAL)
                            projectionLauncher.launch(sourceManager.createProjectionIntent()) 
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BPM Display and Flashing Beat Circle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start 
        ) {
            val bpm = remember(frameTick) { ModulationRegistry.signals["bpm"] ?: 0f }
            val formattedBpm = "%.2f BPM".format(bpm)
            Text(
                text = formattedBpm,
                color = AppText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Flashing Beat Circle
            val beatPhase = remember(frameTick) { ModulationRegistry.signals["beatPhase"] ?: 0f }
            val isBeat = beatPhase < 0.1f // Flash when beat phase is near 0
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isBeat) 1f else 0f,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                label = "beatAlphaAnimation"
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(AppAccent.copy(alpha = animatedAlpha), shape = MaterialTheme.shapes.extraSmall)
            )

            Spacer(modifier = Modifier.width(16.dp)) // Added space

            val onset = remember(frameTick) { ModulationRegistry.signals["onset"] ?: 0f }
            Text(
                text = "Onset: %.2f".format(onset),
                color = AppText,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 8.dp)
            )

            val beatThreshold = remember(frameTick) { ModulationRegistry.signals["beatThreshold"] ?: 0f }
            Text(
                text = "Threshold: %.2f".format(beatThreshold),
                color = AppText,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monitor all registry signals using the centralized history
        key(frameTick) {
            DiagnosticScope("BEAT CLOCK (Synced Sine)", beatVisualBuffer)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = AppText.copy(alpha = 0.1f))

            ModulationRegistry.history["accent"]?.let { DiagnosticScope("ACCENT (Weighted Flux + Decay)", it) }
            ModulationRegistry.history["onset"]?.let { DiagnosticScope("ONSET (Raw Spikes)", it) }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = AppText.copy(alpha = 0.1f))
            
            ModulationRegistry.history["amp"]?.let { DiagnosticScope("AMP (Master Envelope)", it) }
            ModulationRegistry.history["bassFlux"]?.let { DiagnosticScope("BASS FLUX", it) }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun DiagnosticScope(label: String, buffer: CvHistoryBuffer) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = AppAccent, style = MaterialTheme.typography.labelSmall)
        OscilloscopeView(history = buffer, modifier = Modifier.height(60.dp))
    }
}

@Composable
fun CvSegmentedButton(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row {
        options.forEach { option ->
            Button(
                onClick = { onSelect(option) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == option) AppAccent else AppText.copy(alpha = 0.1f),
                    contentColor = if (selected == option) Color.White else AppText
                ),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text(option, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
