package llm.slop.spirals.patches

import llm.slop.spirals.rendering.Mixer
import llm.slop.spirals.models.PlaylistDto
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages the volatile RAM Play Queue (Phase 1).
 */
object PlayQueueManager {
    private val logger = KotlinLogging.logger {}
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    val queue = CopyOnWriteArrayList<File>()
    
    @Volatile
    var isAutoVJEnabled = false

    @Volatile
    var activeIndex = -1
        private set

    fun parsePlaylist(playlistFile: File): List<File> {
        return try {
            val content = playlistFile.readText()
            val items = if (content.trim().startsWith("{")) {
                val dto = json.decodeFromString<PlaylistDto>(content)
                dto.items
            } else {
                content.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
            }
            
            items.mapNotNull { itemName ->
                val resolved = resolveQueueItem(itemName)
                if (resolved == null) {
                    logger.warn { "Queue playlist item not found: $itemName" }
                }
                resolved
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse playlist: ${playlistFile.absolutePath}" }
            emptyList()
        }
    }

    fun appendPlaylistToQueue(playlistFile: File) {
        val files = parsePlaylist(playlistFile)
        queue.addAll(files)
        logger.info { "Appended playlist to queue: ${playlistFile.name} (${files.size} items)" }
    }

    fun playNow(file: File, mixer: Mixer) {
        clearQueue()
        appendToQueue(file)
        triggerNext(mixer)
    }

    fun playPlaylistNow(playlistFile: File, mixer: Mixer) {
        clearQueue()
        val files = parsePlaylist(playlistFile)
        if (files.isNotEmpty()) {
            queue.addAll(files)
            logger.info { "Replaced queue with playlist: ${playlistFile.name} (${files.size} items)" }
            triggerNext(mixer)
        }
    }

    fun insertAfterCurrent(file: File) {
        val insertIndex = if (activeIndex in queue.indices) activeIndex + 1 else 0
        if (insertIndex <= queue.size) {
            queue.add(insertIndex, file)
            logger.info { "Inserted after current (at $insertIndex): ${file.name}" }
        } else {
            appendToQueue(file)
        }
    }

    fun insertPlaylistAfterCurrent(playlistFile: File) {
        val files = parsePlaylist(playlistFile)
        if (files.isNotEmpty()) {
            val insertIndex = if (activeIndex in queue.indices) activeIndex + 1 else 0
            if (insertIndex <= queue.size) {
                queue.addAll(insertIndex, files)
                logger.info { "Inserted playlist after current (at $insertIndex): ${playlistFile.name} (${files.size} items)" }
            } else {
                queue.addAll(files)
                logger.info { "Appended playlist to queue: ${playlistFile.name} (${files.size} items)" }
            }
        }
    }

    private fun resolveQueueItem(name: String): File? {
        val f = File(name)
        if (f.exists() && f.isFile) return f

        val roots = listOf("presets/patches")
        for (root in roots) {
            val rf = File(root, name)
            if (rf.exists() && rf.isFile) return rf
        }

        // Try extensions
        val extensions = listOf(".lsd", ".json", ".patch")
        for (ext in extensions) {
            val nameWithExt = if (name.endsWith(ext, ignoreCase = true)) name else "$name$ext"
            val fExt = File(nameWithExt)
            if (fExt.exists() && fExt.isFile) return fExt

            for (root in roots) {
                val rfExt = File(root, nameWithExt)
                if (rfExt.exists() && rfExt.isFile) return rfExt
            }
        }
        return null
    }

    fun appendToQueue(file: File) {
        queue.add(file)
        logger.info { "Appended to queue: ${file.name}" }
    }

    fun removeFromQueue(index: Int) {
        if (index in queue.indices) {
            val removed = queue.removeAt(index)
            logger.info { "Removed from queue: ${removed.name}" }
            if (index <= activeIndex) {
                activeIndex--
            }
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        if (from in queue.indices && to in queue.indices) {
            val item = queue.removeAt(from)
            queue.add(to, item)
            
            // Adjust activeIndex if it was moved
            if (activeIndex == from) {
                activeIndex = to
            } else if (from < activeIndex && to >= activeIndex) {
                activeIndex--
            } else if (from > activeIndex && to <= activeIndex) {
                activeIndex++
            }
        }
    }

    fun triggerNext(mixer: Mixer) {
        if (queue.isEmpty()) return
        
        val nextIndex = activeIndex + 1
        if (nextIndex >= queue.size) {
            logger.info { "End of queue reached." }
            return
        }
        
        // Determine which deck is inactive
        // crossfade -1.0 = Deck A, 1.0 = Deck B
        val targetIsA = mixer.crossfade.value > 0.0f
        val targetDeck = if (targetIsA) mixer.deckA else mixer.deckB
        
        val isDirty = PatchManager.isDeckDirty(targetDeck, mixer)
        if (isDirty) {
            when (llm.slop.spirals.ui.UITheme.autoVjDirtyBehavior) {
                llm.slop.spirals.ui.UITheme.AutoVjDirtyBehavior.SKIP -> {
                    logger.info { "AutoVJ: Skipping next item because target deck is dirty" }
                    return
                }
                llm.slop.spirals.ui.UITheme.AutoVjDirtyBehavior.AUTO_SAVE -> {
                    val activeName = if (targetIsA) PatchManager.activePresetA else PatchManager.activePresetB
                    val saveName = activeName ?: "AutoVJ_${if (targetIsA) "A" else "B"}_${System.currentTimeMillis()}"
                    logger.info { "AutoVJ: Autosaving dirty deck to $saveName" }
                    PatchManager.saveDeckPresetAsync(File("presets/patches/$saveName.lsd"), targetDeck, saveName)
                    // We can't wait for async save here easily, but we've captured the state
                }
                llm.slop.spirals.ui.UITheme.AutoVjDirtyBehavior.AUTO_DISCARD -> {
                    logger.info { "AutoVJ: Discarding changes on dirty deck" }
                }
            }
        }

        activeIndex = nextIndex
        val file = queue[activeIndex]
        
        logger.info { "Triggering next: ${file.name} to Deck ${if (targetIsA) "A" else "B"}" }
        
        PatchManager.loadDeckPresetAsync(file, targetIsA)
        
        // Start auto-fade to the target deck
        mixer.targetCrossfade = if (targetIsA) -1.0f else 1.0f
        mixer.isAutoFading = true
    }
    
    fun clearQueue() {
        queue.clear()
        activeIndex = -1
    }

    fun restoreSessionQueue(files: List<File>, activeIdx: Int, autoVJ: Boolean) {
        queue.clear()
        queue.addAll(files)
        activeIndex = activeIdx
        isAutoVJEnabled = autoVJ
    }

    fun triggerPrevious(mixer: Mixer) {
        if (queue.isEmpty()) return
        
        val prevIndex = activeIndex - 1
        if (prevIndex < 0) {
            logger.info { "Start of queue reached." }
            return
        }
        
        // Determine which deck is inactive
        // crossfade -1.0 = Deck A, 1.0 = Deck B
        val targetIsA = mixer.crossfade.value > 0.0f
        val targetDeck = if (targetIsA) mixer.deckA else mixer.deckB
        
        val isDirty = PatchManager.isDeckDirty(targetDeck, mixer)
        if (isDirty) {
            when (llm.slop.spirals.ui.UITheme.autoVjDirtyBehavior) {
                llm.slop.spirals.ui.UITheme.AutoVjDirtyBehavior.SKIP -> {
                    logger.info { "AutoVJ: Skipping prev item because target deck is dirty" }
                    return
                }
                llm.slop.spirals.ui.UITheme.AutoVjDirtyBehavior.AUTO_SAVE -> {
                    val activeName = if (targetIsA) PatchManager.activePresetA else PatchManager.activePresetB
                    val saveName = activeName ?: "AutoVJ_${if (targetIsA) "A" else "B"}_${System.currentTimeMillis()}"
                    logger.info { "AutoVJ: Autosaving dirty deck to $saveName" }
                    PatchManager.saveDeckPresetAsync(File("presets/patches/$saveName.lsd"), targetDeck, saveName)
                }
                llm.slop.spirals.ui.UITheme.AutoVjDirtyBehavior.AUTO_DISCARD -> {
                    logger.info { "AutoVJ: Discarding changes on dirty deck" }
                }
            }
        }

        activeIndex = prevIndex
        val file = queue[activeIndex]
        
        logger.info { "Triggering previous: ${file.name} to Deck ${if (targetIsA) "A" else "B"}" }
        
        PatchManager.loadDeckPresetAsync(file, targetIsA)
        
        // Start auto-fade to the target deck
        mixer.targetCrossfade = if (targetIsA) -1.0f else 1.0f
        mixer.isAutoFading = true
    }
}
