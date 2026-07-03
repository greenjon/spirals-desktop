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

    fun appendPlaylistToQueue(playlistFile: File) {
        try {
            val content = playlistFile.readText()
            val items = if (content.trim().startsWith("{")) {
                val dto = json.decodeFromString<PlaylistDto>(content)
                dto.items
            } else {
                content.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
            }
            
            items.forEach { itemName ->
                val resolved = resolveQueueItem(itemName)
                if (resolved != null) {
                    appendToQueue(resolved)
                } else {
                    logger.warn { "Queue playlist item not found: $itemName" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to append playlist to queue: ${playlistFile.absolutePath}" }
        }
    }

    private fun resolveQueueItem(name: String): File? {
        val f = File(name)
        if (f.exists() && f.isFile) return f

        val roots = listOf("presets/patches", "presets/decks")
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
        
        activeIndex = nextIndex
        val file = queue[activeIndex]
        
        // Determine which deck is inactive
        // crossfade 0.0 = Deck A, 1.0 = Deck B
        val targetIsA = mixer.crossfade.value > 0.5f
        
        logger.info { "Triggering next: ${file.name} to Deck ${if (targetIsA) "A" else "B"}" }
        
        PatchManager.loadDeckPresetAsync(file, targetIsA)
        
        // Start auto-fade to the target deck
        mixer.targetCrossfade = if (targetIsA) 0.0f else 1.0f
        mixer.isAutoFading = true
    }
    
    fun clearQueue() {
        queue.clear()
        activeIndex = -1
    }
}
