package llm.slop.spirals.ui

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiWindowFlags
import llm.slop.spirals.patches.PlayQueueManager
import llm.slop.spirals.rendering.Mixer
import java.io.File

/**
 * UI for the volatile RAM Play Queue (Phase 1).
 */
object PlayQueuePanel {
    
    private val fileBrowser = ImGuiFileBrowser("queueFileBrowser")

    fun drawPopups() {
        fileBrowser.draw { file ->
            PlayQueueManager.appendToQueue(file)
        }
    }
    
    fun draw(mixer: Mixer) {
        UITheme.h3("Play Queue")
        ImGui.sameLine(ImGui.getContentRegionAvailX() - 80f)
        if (ImGui.button("Add...##addQueue")) {
            fileBrowser.open(
                ImGuiFileBrowser.Mode.LOAD,
                startDir = File("presets/decks").canonicalFile,
                extensions = listOf(".lsd", ".json")
            )
        }
        
        ImGui.spacing()
        
        // Next Trigger Button
        val canTrigger = PlayQueueManager.activeIndex + 1 < PlayQueueManager.queue.size
        if (!canTrigger) {
            ImGui.beginDisabled()
        }
        
        if (ImGui.button("TRIGGER NEXT", ImGui.getContentRegionAvailX(), 40f)) {
            PlayQueueManager.triggerNext(mixer)
        }
        
        if (!canTrigger) {
            ImGui.endDisabled()
        }
        
        ImGui.spacing()
        ImGui.separator()
        ImGui.spacing()
        
        // Queue List
        val listH = ImGui.getContentRegionAvailY() - 30f
        if (ImGui.beginChild("##queueList", 0f, listH, true)) {
            if (PlayQueueManager.queue.isEmpty()) {
                ImGui.textDisabled("Queue is empty.")
                ImGui.textDisabled("Add .lsd patches to begin.")
            } else {
                var moveFrom = -1
                var moveTo = -1
                
                PlayQueueManager.queue.forEachIndexed { index, file ->
                    val isActive = index == PlayQueueManager.activeIndex
                    val label = "${index + 1}. ${file.nameWithoutExtension}"
                    
                    if (isActive) {
                        ImGui.pushStyleColor(ImGuiCol.Text, 0.4f, 1.0f, 0.8f, 1.0f) // Mint green for active
                    }
                    
                    val selected = ImGui.selectable("$label##queue_$index", false)
                    
                    if (isActive) {
                        ImGui.popStyleColor()
                        ImGui.sameLine(5f)
                        ImGui.text("→")
                    }
                    
                    // Drag and Drop for reordering
                    if (ImGui.beginDragDropSource()) {
                        ImGui.setDragDropPayload("QUEUE_ITEM", index as Any)
                        ImGui.text("Moving $label")
                        ImGui.endDragDropSource()
                    }
                    
                    if (ImGui.beginDragDropTarget()) {
                        val payload = ImGui.acceptDragDropPayload<Int>("QUEUE_ITEM")
                        if (payload != null) {
                            moveFrom = payload
                            moveTo = index
                        }
                        ImGui.endDragDropTarget()
                    }
                    
                    // Right-click menu
                    if (ImGui.beginPopupContextItem("queue_item_menu_$index")) {
                        if (ImGui.menuItem("Remove")) {
                            PlayQueueManager.removeFromQueue(index)
                        }
                        ImGui.endPopup()
                    }
                    
                    if (selected) {
                        // Optional: trigger this specific index? 
                        // For now just highlight.
                    }
                }
                
                if (moveFrom != -1 && moveTo != -1) {
                    PlayQueueManager.moveQueueItem(moveFrom, moveTo)
                }
            }
        }
        ImGui.endChild()
        
        if (PlayQueueManager.queue.isNotEmpty()) {
            if (ImGui.button("Save Queue as Playlist...", ImGui.getContentRegionAvailX(), 0f)) {
                PlaylistPanel.openWithQueue(PlayQueueManager.queue)
            }
            ImGui.spacing()
            if (ImGui.button("Clear Queue", ImGui.getContentRegionAvailX(), 0f)) {
                PlayQueueManager.clearQueue()
            }
        }
    }
}
