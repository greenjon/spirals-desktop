package llm.slop.spirals.ui.components

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import llm.slop.spirals.*
import llm.slop.spirals.models.*
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText
import llm.slop.spirals.cv.core.ModulatableParameter
import llm.slop.spirals.display.SpiralRenderer
import llm.slop.spirals.display.SharedEGLContextFactory
import llm.slop.spirals.ui.components.ModulatorRow
import llm.slop.spirals.display.LocalSpiralRenderer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.roundToInt
import llm.slop.spirals.database.entities.MandalaSetEntity
import llm.slop.spirals.database.entities.RandomSetEntity

@Composable
fun SpiralPreview(sourceId: String, mainRenderer: SpiralRenderer?, modifier: Modifier = Modifier) {
    if (mainRenderer == null) {
        Box(modifier = modifier.background(Color.Black))
        return
    }

    val view = remember { mutableStateOf<GLSurfaceView?>(null) }
    var viewportWidth by remember { mutableIntStateOf(0) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var blitHelper: SimpleBlitHelper? by remember { mutableStateOf(null) }
    
    // Wait a bit for main context to be established
    var isReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100) // Give main renderer time to initialize
        isReady = true
    }
    
    if (!isReady) {
        Box(modifier = modifier.background(Color.Black))
        return
    }

    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(3)
                // Bridge Context
                setEGLContextFactory(SharedEGLContextFactory())
                setRenderer(object : GLSurfaceView.Renderer {
                    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                        // Create blit resources for this context
                        blitHelper = SimpleBlitHelper()
                    }
                    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                        viewportWidth = width
                        viewportHeight = height
                    }
                    override fun onDrawFrame(gl: GL10?) {
                        // Set viewport for this view
                        android.opengl.GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
                        android.opengl.GLES30.glClearColor(0f, 0f, 0f, 1f)
                        android.opengl.GLES30.glClear(android.opengl.GLES30.GL_COLOR_BUFFER_BIT)
                        
                        // Sample from main renderer's master pool
                        val textureId = mainRenderer.getTextureForSource(sourceId)
                        if (textureId != 0 && blitHelper != null) {
                            blitHelper?.drawTexture(textureId)
                        }
                    }
                })
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                view.value = this
            }
        },
        modifier = modifier,
        update = { _ -> }
    )

    LaunchedEffect(sourceId) {
        while (true) {
            delay(33) // Throttled sample
            view.value?.requestRender()
        }
    }
}

/**
 * Updated StripPreview using the throttled architecture with Shared Context.
 */
@Composable
fun StripPreview(monitorSource: String, patch: MixerPatch, mainRenderer: SpiralRenderer?) {
    if (mainRenderer == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }
    
    val view = remember { mutableStateOf<GLSurfaceView?>(null) }
    var viewportWidth by remember { mutableIntStateOf(0) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var blitHelper: SimpleBlitHelper? by remember { mutableStateOf(null) }
    
    // Wait a bit for main context to be established
    var isReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100) // Give main renderer time to initialize
        isReady = true
    }
    
    if (!isReady) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }
    
    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(3)
                // Bridge Context
                setEGLContextFactory(SharedEGLContextFactory())
                setRenderer(object : GLSurfaceView.Renderer {
                    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                        // Create blit resources for this context
                        blitHelper = SimpleBlitHelper()
                    }
                    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                        viewportWidth = width
                        viewportHeight = height
                    }
                    override fun onDrawFrame(gl: GL10?) {
                        // Set viewport for this view
                        android.opengl.GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
                        android.opengl.GLES30.glClearColor(0f, 0f, 0f, 1f)
                        android.opengl.GLES30.glClear(android.opengl.GLES30.GL_COLOR_BUFFER_BIT)
                        
                        // Sample from main renderer's master pool
                        val textureId = mainRenderer.getTextureForSource(monitorSource)
                        if (textureId != 0 && blitHelper != null) {
                            blitHelper?.drawTexture(textureId)
                        }
                    }
                })
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                view.value = this
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { _ -> }
    )

    LaunchedEffect(monitorSource) {
        while (true) {
            delay(33) // Throttled sample (30 FPS)
            view.value?.requestRender()
        }
    }
}

@Composable
fun SourceStrip(
    index: Int,
    patch: MixerPatch,
    onPatchChange: (MixerPatch) -> Unit,
    mainRenderer: SpiralRenderer?,
    onPickSet: () -> Unit,
    onPickMandala: () -> Unit,
    onPickRandomSet: () -> Unit = {},
    onRandomSetNext: () -> Unit = {},
    allSets: List<MandalaSetEntity>,
    allRandomSets: List<RandomSetEntity> = emptyList(),
    identity: String,
    onOffAlignment: Alignment,
    focusedId: String,
    onFocusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val slot = patch.slots[index]
    val slotId = "S${index + 1}"
    val prevNextId = "PN${index + 1}"
    val hueId = "H${index + 1}"
    val satId = "S${index + 1}"

    Column(
        modifier = modifier.padding(1.dp).wrapContentHeight().border(1.dp, AppText.copy(alpha = 0.1f)).padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Source $identity",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AppText,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 0.dp)
        )

        Box(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
            .background(Color.Black)
            .border(1.dp, AppText.copy(alpha = 0.2f))
            .clickable {
                val newSlots = patch.slots.toMutableList()
                newSlots[index] = slot.copy(enabled = !slot.enabled)
                onPatchChange(patch.copy(slots = newSlots))
            }
        ) {
            if (slot.enabled) {
                if (slot.isPopulated()) {
                    StripPreview(monitorSource = "${index + 1}", patch = patch, mainRenderer = mainRenderer)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No source loaded", color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp, textAlign = TextAlign.Center)
                    }
                }
                
                Text(
                    text = "ON",
                    modifier = Modifier.align(onOffAlignment).padding(horizontal = 2.dp, vertical = 0.dp),
                    style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, shadow = Shadow(color = Color.Black, blurRadius = 3f))
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OFF",
                        style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, shadow = Shadow(color = Color.Black, blurRadius = 3f))
                    )
                    Text(
                        text = "Tap to enable",
                        style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Normal, shadow = Shadow(color = Color.Black, blurRadius = 3f))
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        var typeExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.clickable { onFocusChange(slotId); typeExpanded = true }) {
            Text(
                text = when(slot.sourceType) {
                    VideoSourceType.MANDALA -> "Mandala"
                    VideoSourceType.MANDALA_SET -> "Mandala Set"
                    VideoSourceType.RANDOM_SET -> "Random Set"
                    VideoSourceType.COLOR -> "Color"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (focusedId == slotId) AppAccent else AppText.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(2.dp)
            )
            DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }, containerColor = AppBackground) {
                VideoSourceType.entries.forEach { type ->
                    DropdownMenuItem(text = { Text(type.name.replace("_", " "), fontSize = 11.sp) }, onClick = {
                        val newSlots = patch.slots.toMutableList()
                        newSlots[index] = slot.copy(sourceType = type)
                        onPatchChange(patch.copy(slots = newSlots))
                        typeExpanded = false
                    })
                }
            }
        }
        
        when(slot.sourceType) {
            VideoSourceType.MANDALA, VideoSourceType.MANDALA_SET, VideoSourceType.RANDOM_SET -> {
                val displayName = when (slot.sourceType) {
                    VideoSourceType.MANDALA_SET -> allSets.find { it.id == slot.mandalaSetId }?.name ?: "Pick Set"
                    VideoSourceType.RANDOM_SET -> allRandomSets.find { it.id == slot.randomSetId }?.name ?: "Pick RSet"
                    VideoSourceType.MANDALA -> slot.selectedMandalaId ?: "Pick Man"
                    else -> "Pick"
                }
                
                Button(
                    onClick = { 
                        when (slot.sourceType) {
                            VideoSourceType.MANDALA_SET -> onPickSet()
                            VideoSourceType.RANDOM_SET -> onPickRandomSet()
                            VideoSourceType.MANDALA -> onPickMandala()
                            else -> {}
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppText.copy(alpha = 0.1f)),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(displayName, style = MaterialTheme.typography.labelSmall, color = AppText, maxLines = 1, textAlign = TextAlign.Center, fontSize = 8.sp)
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onFocusChange(prevNextId) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val arrowColor = if (focusedId == prevNextId) AppAccent else AppText
                    IconButton(
                        onClick = {
                            onFocusChange(prevNextId)
                            when (slot.sourceType) {
                                VideoSourceType.MANDALA_SET -> {
                                    val set = allSets.find { it.id == slot.mandalaSetId }
                                    set?.let {
                                        val ids = try { Json.decodeFromString<List<String>>(it.jsonOrderedMandalaIds) } catch(e: Exception) { emptyList() }
                                        if (ids.isNotEmpty()) {
                                            val nextIdx = if (slot.currentIndex.baseValue <= 0) ids.size - 1 else slot.currentIndex.baseValue.toInt() - 1
                                            val newSlots = patch.slots.toMutableList()
                                            newSlots[index] = slot.copy(currentIndex = slot.currentIndex.copy(baseValue = nextIdx.toFloat()))
                                            onPatchChange(patch.copy(slots = newSlots))
                                        }
                                    }
                                }
                                VideoSourceType.RANDOM_SET -> {
                                    onRandomSetNext()
                                }
                                else -> {}
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = slot.sourceType == VideoSourceType.MANDALA_SET || slot.sourceType == VideoSourceType.RANDOM_SET
                    ) { 
                        val enabled = slot.sourceType == VideoSourceType.MANDALA_SET || slot.sourceType == VideoSourceType.RANDOM_SET
                        Icon(Icons.Default.KeyboardArrowLeft, null, tint = if (enabled) arrowColor else arrowColor.copy(alpha = 0.2f), modifier = Modifier.size(32.dp)) 
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            onFocusChange(prevNextId)
                            when (slot.sourceType) {
                                VideoSourceType.MANDALA_SET -> {
                                    val set = allSets.find { it.id == slot.mandalaSetId }
                                    set?.let {
                                        val ids = try { Json.decodeFromString<List<String>>(it.jsonOrderedMandalaIds) } catch(e: Exception) { emptyList() }
                                        if (ids.isNotEmpty()) {
                                            val nextIdx = (slot.currentIndex.baseValue.toInt() + 1) % ids.size
                                            val newSlots = patch.slots.toMutableList()
                                            newSlots[index] = slot.copy(currentIndex = slot.currentIndex.copy(baseValue = nextIdx.toFloat()))
                                            onPatchChange(patch.copy(slots = newSlots))
                                        }
                                    }
                                }
                                VideoSourceType.RANDOM_SET -> {
                                    onRandomSetNext()
                                }
                                else -> {}
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = slot.sourceType == VideoSourceType.MANDALA_SET || slot.sourceType == VideoSourceType.RANDOM_SET
                    ) { 
                        val enabled = slot.sourceType == VideoSourceType.MANDALA_SET || slot.sourceType == VideoSourceType.RANDOM_SET
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = if (enabled) arrowColor else arrowColor.copy(alpha = 0.2f), modifier = Modifier.size(32.dp)) 
                    }
                }
            }
            VideoSourceType.COLOR -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onFocusChange(hueId) }) {
                        Text("HUE", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = if (focusedId == hueId) AppAccent else AppText)
                        val mainRenderer = LocalSpiralRenderer.current
                        val modulatedHue = mainRenderer?.getMixerParam(hueId)?.value ?: slot.hue.baseValue
                        KnobView(
                            baseValue = slot.hue.baseValue,
                            modulatedValue = modulatedHue,
                            onValueChange = { newValue ->
                                onFocusChange(hueId)
                                val newSlots = patch.slots.toMutableList()
                                newSlots[index] = slot.copy(hue = slot.hue.copy(baseValue = newValue))
                                onPatchChange(patch.copy(slots = newSlots))
                            },
                            onInteractionFinished = {},
                            knobSize = 36.dp,
                            showValue = true,
                            focused = focusedId == hueId
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onFocusChange(satId) }) {
                        Text("SAT", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = if (focusedId == satId) AppAccent else AppText)
                        val mainRenderer = LocalSpiralRenderer.current
                        val modulatedSat = mainRenderer?.getMixerParam(satId)?.value ?: slot.saturation.baseValue
                        KnobView(
                            baseValue = slot.saturation.baseValue,
                            modulatedValue = modulatedSat,
                            onValueChange = { newValue ->
                                onFocusChange(satId)
                                val newSlots = patch.slots.toMutableList()
                                newSlots[index] = slot.copy(saturation = slot.saturation.copy(baseValue = newValue))
                                onPatchChange(patch.copy(slots = newSlots))
                            },
                            onInteractionFinished = {},
                            knobSize = 36.dp,
                            showValue = true,
                            focused = focusedId == satId
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MonitorStrip(
    group: String,
    patch: MixerPatch,
    onPatchChange: (MixerPatch) -> Unit,
    mainRenderer: SpiralRenderer?,
    hasToggle: Boolean,
    viewSet1A2: Boolean,
    onToggleViewSet: (Boolean) -> Unit,
    focusedId: String,
    onFocusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupData = when(group) {
        "A" -> patch.mixerA
        "B" -> patch.mixerB
        else -> patch.mixerF
    }
    
    val headerText = when(group) {
        "A" -> "Mixer A"
        "B" -> "Mixer B"
        else -> "Final Mixer"
    }

    val modeId = "M${group}_MODE"
    val balId = "M${group}_BAL"
    val finalGainId = "MF_GAIN"
    
    Column(
        modifier = modifier.padding(1.dp).wrapContentHeight().border(1.dp, AppText.copy(alpha = 0.1f)).padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = headerText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AppText,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 0.dp)
        )

        Box(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
            .background(Color.Black)
            .border(1.dp, AppText.copy(alpha = 0.2f))
            .clickable {
                if (group == "A") onToggleViewSet(false)
                else if (group == "B") onToggleViewSet(true)
            }
        ) {
            StripPreview(monitorSource = group, patch = patch, mainRenderer = mainRenderer)

            if (hasToggle) {
                Row(
                    modifier = Modifier.align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "1/2",
                        style = TextStyle(color = if (viewSet1A2) AppAccent else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, shadow = Shadow(color = Color.Black, blurRadius = 3f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "3/4",
                        style = TextStyle(color = if (!viewSet1A2) AppAccent else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, shadow = Shadow(color = Color.Black, blurRadius = 3f))
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        var modeExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.clickable { onFocusChange(modeId) }) {
            val modeEntries = MixerMode.entries
            val currentModeIndex = groupData.mode.baseValue.toInt().coerceIn(0, modeEntries.size - 1)
            Text(
                text = modeEntries[currentModeIndex].name,
                style = MaterialTheme.typography.labelSmall,
                color = if (focusedId == modeId) AppAccent else AppText.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onFocusChange(modeId); modeExpanded = true }
                    .padding(2.dp)
            )
            DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }, containerColor = AppBackground) {
                MixerMode.entries.forEachIndexed { index, m ->
                    DropdownMenuItem(text = { Text(m.name, fontSize = 11.sp) }, onClick = {
                        val newGroup = groupData.copy(mode = groupData.mode.copy(baseValue = index.toFloat()))
                        onPatchChange(updateGroup(patch, group, newGroup))
                        modeExpanded = false
                    })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onFocusChange(balId) }) {
            Text("BAL", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = if (focusedId == balId) AppAccent else AppText)
            val mainRenderer = LocalSpiralRenderer.current
            val modulatedBal = mainRenderer?.getMixerParam(balId)?.value ?: groupData.balance.baseValue
            KnobView(
                baseValue = groupData.balance.baseValue,
                modulatedValue = modulatedBal,
                onValueChange = { newValue ->
                    onFocusChange(balId)
                    val newGroup = groupData.copy(balance = groupData.balance.copy(baseValue = newValue))
                    onPatchChange(updateGroup(patch, group, newGroup))
                },
                onInteractionFinished = {},
                isBipolar = true,
                knobSize = 44.dp,
                showValue = true,
                focused = focusedId == balId,
                displayTransform = { 
                    // Map 0-1 to -100 to 100 for display
                    val v = ((it - 0.5f) * 200).roundToInt()
                    val (leftLabel, rightLabel) = if (group == "F") "A" to "B" else "L" to "R"
                    when {
                        v < 0 -> "$leftLabel${abs(v)}"
                        v > 0 -> "$rightLabel$v"
                        else -> "0"
                    }
                }
            )
        }
        
        if (group == "F") {
            Spacer(modifier = Modifier.height(4.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onFocusChange(finalGainId) }) {
                val mainRenderer = LocalSpiralRenderer.current
                val modulatedGain = mainRenderer?.getMixerParam(finalGainId)?.value ?: patch.finalGain.baseValue
                KnobView(
                    baseValue = patch.finalGain.baseValue,
                    modulatedValue = modulatedGain,
                    onValueChange = { newValue ->
                        onFocusChange(finalGainId)
                        onPatchChange(patch.copy(finalGain = patch.finalGain.copy(baseValue = newValue)))
                    },
                    onInteractionFinished = {},
                    knobSize = 44.dp,
                    showValue = true,
                    focused = focusedId == finalGainId
                )
                Text("GAIN", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = if (focusedId == finalGainId) AppAccent else AppText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MixerCvEditor(
    patch: MixerPatch,
    focusedId: String,
    onPatchUpdate: (MixerPatch) -> Unit
) {
    val scrollState = rememberScrollState()
    
    val focusedParamData = remember(patch, focusedId) {
        when {
            focusedId.startsWith("PN") -> {
                val idx = (focusedId.last().digitToIntOrNull() ?: 1) - 1
                if (idx in 0..3) patch.slots[idx].currentIndex else null
            }
            focusedId.startsWith("H") -> {
                val idx = (focusedId.last().digitToIntOrNull() ?: 1) - 1
                if (idx in 0..3) patch.slots[idx].hue else null
            }
            focusedId.startsWith("S") && !focusedId.startsWith("S_") && focusedId.length <= 2 -> {
                val idx = (focusedId.last().digitToIntOrNull() ?: 1) - 1
                if (idx in 0..3) patch.slots[idx].saturation else null
            }
            focusedId.startsWith("MA_") -> {
                when(focusedId.removePrefix("MA_")) {
                    "MODE" -> patch.mixerA.mode
                    "BAL" -> patch.mixerA.balance
                    else -> null
                }
            }
            focusedId.startsWith("MB_") -> {
                when(focusedId.removePrefix("MB_")) {
                    "MODE" -> patch.mixerB.mode
                    "BAL" -> patch.mixerB.balance
                    else -> null
                }
            }
            focusedId.startsWith("MF_") -> {
                val sub = focusedId.removePrefix("MF_")
                when (sub) {
                    "MODE" -> patch.mixerF.mode
                    "BAL" -> patch.mixerF.balance
                    "GAIN" -> patch.finalGain
                    "FB_DECAY" -> patch.effects.fbDecay
                    "FB_GAIN" -> patch.effects.fbGain
                    "FB_ZOOM" -> patch.effects.fbZoom
                    "FB_ROTATE" -> patch.effects.fbRotate
                    "FB_SHIFT" -> patch.effects.fbShift
                    "FB_BLUR" -> patch.effects.fbBlur
                    "TRAILS" -> patch.effects.trails
                    "SNAP_COUNT" -> patch.effects.snapCount
                    "SNAP_MODE" -> patch.effects.snapMode
                    "SNAP_BLEND" -> patch.effects.snapBlend
                    "SNAP_TRIG" -> patch.effects.snapTrigger
                    else -> null
                }
            }
            else -> null
        }
    }

    if (focusedParamData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a parameter to patch CV", color = AppText.copy(alpha = 0.5f))
        }
        return
    }

    val tempParam = remember(focusedId, focusedParamData) {
        ModulatableParameter(baseValue = focusedParamData.baseValue).apply {
            modulators.addAll(focusedParamData.modulators)
        }
    }
    
    var refreshCount by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
            key(focusedId, refreshCount) {
                tempParam.modulators.forEachIndexed { index, mod ->
                    ModulatorRow(
                        mod = mod,
                        onUpdate = { updatedMod ->
                            tempParam.modulators[index] = updatedMod
                            syncMixerParam(patch, focusedId, tempParam, onPatchUpdate)
                        },
                        onInteractionFinished = { syncMixerParam(patch, focusedId, tempParam, onPatchUpdate) },
                        onRemove = {
                            tempParam.modulators.removeAt(index)
                            refreshCount++
                            syncMixerParam(patch, focusedId, tempParam, onPatchUpdate)
                        }
                    )
                    HorizontalDivider(color = AppText.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                }

                ModulatorRow(
                    mod = null,
                    onUpdate = { newMod ->
                        tempParam.modulators.add(newMod)
                        refreshCount++
                        syncMixerParam(patch, focusedId, tempParam, onPatchUpdate)
                    },
                    onInteractionFinished = { syncMixerParam(patch, focusedId, tempParam, onPatchUpdate) },
                    onRemove = {}
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

private fun syncMixerParam(patch: MixerPatch, id: String, param: ModulatableParameter, onUpdate: (MixerPatch) -> Unit) {
    val data = ModulatableParameterData(baseValue = param.baseValue, modulators = param.modulators.toList())
    val newPatch = when {
        id.startsWith("PN") -> {
            val idx = (id.last().digitToIntOrNull() ?: 1) - 1
            val newSlots = patch.slots.toMutableList()
            newSlots[idx] = newSlots[idx].copy(currentIndex = data)
            patch.copy(slots = newSlots)
        }
        id.startsWith("H") -> {
            val idx = (id.last().digitToIntOrNull() ?: 1) - 1
            val newSlots = patch.slots.toMutableList()
            newSlots[idx] = newSlots[idx].copy(hue = data)
            patch.copy(slots = newSlots)
        }
        id.startsWith("S") && !id.startsWith("S_") && id.length <= 2 -> {
            val idx = (id.last().digitToIntOrNull() ?: 1) - 1
            val newSlots = patch.slots.toMutableList()
            newSlots[idx] = newSlots[idx].copy(saturation = data)
            patch.copy(slots = newSlots)
        }
        id.startsWith("MA_") -> {
            val group = patch.mixerA
            val newGroup = when(id.removePrefix("MA_")) {
                "MODE" -> group.copy(mode = data)
                "BAL" -> group.copy(balance = data)
                else -> group
            }
            patch.copy(mixerA = newGroup)
        }
        id.startsWith("MB_") -> {
            val group = patch.mixerB
            val newGroup = when(id.removePrefix("MB_")) {
                "MODE" -> group.copy(mode = data)
                "BAL" -> group.copy(balance = data)
                else -> group
            }
            patch.copy(mixerB = newGroup)
        }
        id.startsWith("MF_") -> {
            val sub = id.removePrefix("MF_")
            when (sub) {
                "MODE" -> patch.copy(mixerF = patch.mixerF.copy(mode = data))
                "BAL" -> patch.copy(mixerF = patch.mixerF.copy(balance = data))
                "GAIN" -> patch.copy(finalGain = data)
                "FB_DECAY" -> patch.copy(effects = patch.effects.copy(fbDecay = data))
                "FB_GAIN" -> patch.copy(effects = patch.effects.copy(fbGain = data))
                "FB_ZOOM" -> patch.copy(effects = patch.effects.copy(fbZoom = data))
                "FB_ROTATE" -> patch.copy(effects = patch.effects.copy(fbRotate = data))
                "FB_SHIFT" -> patch.copy(effects = patch.effects.copy(fbShift = data))
                "FB_BLUR" -> patch.copy(effects = patch.effects.copy(fbBlur = data))
                "TRAILS" -> patch.copy(effects = patch.effects.copy(trails = data))
                "SNAP_COUNT" -> patch.copy(effects = patch.effects.copy(snapCount = data))
                "SNAP_MODE" -> patch.copy(effects = patch.effects.copy(snapMode = data))
                "SNAP_BLEND" -> patch.copy(effects = patch.effects.copy(snapBlend = data))
                "SNAP_TRIG" -> patch.copy(effects = patch.effects.copy(snapTrigger = data))
                else -> patch
            }
        }
        else -> patch
    }
    onUpdate(newPatch)
}

private fun updateGroup(patch: MixerPatch, group: String, data: MixerGroupData): MixerPatch {
    return when(group) {
        "A" -> patch.copy(mixerA = data)
        "B" -> patch.copy(mixerB = data)
        else -> patch.copy(mixerF = data)
    }
}

/**
 * Helper class to blit textures in secondary GL contexts.
 * Each context needs its own shader program and VAO since these aren't shared.
 */
class SimpleBlitHelper {
    private val program: Int
    private val vao: Int
    private val vbo: Int
    private val uTextureLocation: Int

    init {
        // Simple passthrough vertex shader
        val vertexShader = """
            #version 300 es
            in vec2 aPosition;
            out vec2 vTexCoord;
            void main() {
                vTexCoord = (aPosition + 1.0) * 0.5;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """.trimIndent()

        // Simple texture fragment shader
        val fragmentShader = """
            #version 300 es
            precision mediump float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;
            void main() {
                fragColor = texture(uTexture, vTexCoord);
            }
        """.trimIndent()

        // Compile shaders
        val vShader = android.opengl.GLES30.glCreateShader(android.opengl.GLES30.GL_VERTEX_SHADER)
        android.opengl.GLES30.glShaderSource(vShader, vertexShader)
        android.opengl.GLES30.glCompileShader(vShader)

        val fShader = android.opengl.GLES30.glCreateShader(android.opengl.GLES30.GL_FRAGMENT_SHADER)
        android.opengl.GLES30.glShaderSource(fShader, fragmentShader)
        android.opengl.GLES30.glCompileShader(fShader)

        // Link program
        program = android.opengl.GLES30.glCreateProgram()
        android.opengl.GLES30.glAttachShader(program, vShader)
        android.opengl.GLES30.glAttachShader(program, fShader)
        android.opengl.GLES30.glLinkProgram(program)
        android.opengl.GLES30.glDeleteShader(vShader)
        android.opengl.GLES30.glDeleteShader(fShader)

        uTextureLocation = android.opengl.GLES30.glGetUniformLocation(program, "uTexture")

        // Create fullscreen quad
        val quad = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        val quadBuffer = java.nio.ByteBuffer.allocateDirect(quad.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        quadBuffer.put(quad)
        quadBuffer.position(0)

        val vaoArr = IntArray(1)
        android.opengl.GLES30.glGenVertexArrays(1, vaoArr, 0)
        vao = vaoArr[0]

        val vboArr = IntArray(1)
        android.opengl.GLES30.glGenBuffers(1, vboArr, 0)
        vbo = vboArr[0]

        android.opengl.GLES30.glBindVertexArray(vao)
        android.opengl.GLES30.glBindBuffer(android.opengl.GLES30.GL_ARRAY_BUFFER, vbo)
        android.opengl.GLES30.glBufferData(
            android.opengl.GLES30.GL_ARRAY_BUFFER,
            quad.size * 4,
            quadBuffer,
            android.opengl.GLES30.GL_STATIC_DRAW
        )
        android.opengl.GLES30.glEnableVertexAttribArray(0)
        android.opengl.GLES30.glVertexAttribPointer(0, 2, android.opengl.GLES30.GL_FLOAT, false, 0, 0)
    }

    fun drawTexture(textureId: Int) {
        android.opengl.GLES30.glUseProgram(program)
        android.opengl.GLES30.glBindVertexArray(vao)
        android.opengl.GLES30.glActiveTexture(android.opengl.GLES30.GL_TEXTURE0)
        android.opengl.GLES30.glBindTexture(android.opengl.GLES30.GL_TEXTURE_2D, textureId)
        android.opengl.GLES30.glUniform1i(uTextureLocation, 0)
        android.opengl.GLES30.glDrawArrays(android.opengl.GLES30.GL_TRIANGLE_STRIP, 0, 4)
    }
}
