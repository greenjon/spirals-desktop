package llm.slop.spirals.ui

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiWindowFlags
import llm.slop.spirals.patches.PlaylistManager
import mu.KotlinLogging
import java.io.File

/**
 * UI for creating and editing playlists (.lsdset) (Phase 2).
 */
object PlaylistPanel {
    private val logger = KotlinLogging.logger {}
    private val fileBrowser = ImGuiFileBrowser("playlistFileBrowser")
    private val deckBrowser = ImGuiFileBrowser("playlistDeckBrowser")

    private var pendingOpen = false

    fun open() {
        pendingOpen = true
    }

    fun openWithQueue(queue: List<File>) {
        logger.info { "Opening Playlist Editor with ${queue.size} items from queue" }
        PlaylistManager.initializeFromQueue(queue)
        pendingOpen = true
    }

    fun draw() {
        if (pendingOpen) {
            logger.info { "PlaylistPanel: opening popup $POPUP_ID" }
            ImGui.openPopup(POPUP_ID)
            pendingOpen = false
        }

        ImGui.setNextWindowSize(600f, 600f, imgui.flag.ImGuiCond.Appearing)
        ImGui.setNextWindowPos(
            ImGui.getIO().displaySizeX * 0.5f,
            ImGui.getIO().displaySizeY * 0.5f,
            imgui.flag.ImGuiCond.Appearing,
            0.5f, 0.5f
        )

        val flags = ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove
        val isOpen = ImGui.beginPopupModal(POPUP_ID, flags)
        if (!isOpen) return

        val fileName = PlaylistManager.currentPlaylistFile?.name ?: "Untitled Playlist"
        val title = if (PlaylistManager.isDirty) "$fileName *" else fileName
        UITheme.h3(title)

        ImGui.sameLine(ImGui.getContentRegionAvailX() - 320f)
        if (ImGui.button("New##newPlaylist")) {
            PlaylistManager.createNew()
        }
        ImGui.sameLine()
        if (ImGui.button("Load...##loadPlaylist")) {
            fileBrowser.open(
                ImGuiFileBrowser.Mode.LOAD,
                startDir = File("presets/playlists").canonicalFile,
                extensions = listOf(".lsdset")
            )
        }
        ImGui.sameLine()
        if (ImGui.button("Save##savePlaylist")) {
            val current = PlaylistManager.currentPlaylistFile
            if (current != null) {
                PlaylistManager.savePlaylist(current)
            } else {
                fileBrowser.open(
                    ImGuiFileBrowser.Mode.SAVE,
                    startDir = File("presets/playlists").canonicalFile,
                    extensions = listOf(".lsdset")
                )
            }
        }
        ImGui.sameLine()
        if (ImGui.button("Save As...##saveAsPlaylist")) {
            fileBrowser.open(
                ImGuiFileBrowser.Mode.SAVE,
                startDir = File("presets/playlists").canonicalFile,
                extensions = listOf(".lsdset"),
                initialName = PlaylistManager.currentPlaylistFile?.nameWithoutExtension ?: ""
            )
        }

        ImGui.separator()
        ImGui.spacing()

        // Playlist contents
        val listH = ImGui.getContentRegionAvailY() - 50f
        if (ImGui.beginChild("##playlistItems", 0f, listH, true)) {
            if (PlaylistManager.activePlaylist.isEmpty()) {
                ImGui.textDisabled("Playlist is empty.")
            } else {
                var moveFrom = -1
                var moveTo = -1
                
                PlaylistManager.activePlaylist.forEachIndexed { index, file ->
                    val label = "${index + 1}. ${file.nameWithoutExtension}"
                    ImGui.selectable("$label##playlistItem_$index", false)

                    // Drag and Drop
                    if (ImGui.beginDragDropSource()) {
                        ImGui.setDragDropPayload("PLAYLIST_ITEM", index as Any)
                        ImGui.text("Moving $label")
                        ImGui.endDragDropSource()
                    }

                    if (ImGui.beginDragDropTarget()) {
                        val payload = ImGui.acceptDragDropPayload<Int>("PLAYLIST_ITEM")
                        if (payload != null) {
                            moveFrom = payload
                            moveTo = index
                        }
                        ImGui.endDragDropTarget()
                    }

                    // Right click menu
                    if (ImGui.beginPopupContextItem("playlist_item_menu_$index")) {
                        if (ImGui.menuItem("Remove")) {
                            PlaylistManager.removeFromPlaylist(index)
                        }
                        ImGui.endPopup()
                    }
                }
                
                if (moveFrom != -1 && moveTo != -1) {
                    PlaylistManager.moveItem(moveFrom, moveTo)
                }
            }
        }
        ImGui.endChild()

        ImGui.spacing()
        if (ImGui.button("Add Patch...##addPatchToPlaylist", 120f, 0f)) {
            deckBrowser.open(
                ImGuiFileBrowser.Mode.LOAD,
                startDir = File("presets/decks").canonicalFile,
                extensions = listOf(".lsd", ".json")
            )
        }
        ImGui.sameLine()
        if (ImGui.button("PUSH TO PLAY QUEUE", 180f, 0f)) {
            PlaylistManager.pushToPlayQueue()
        }
        ImGui.sameLine(ImGui.getContentRegionAvailX() - 90f)
        if (ImGui.button("Close##closePlaylist", 90f, 0f)) {
            ImGui.closeCurrentPopup()
        }

        fileBrowser.draw { file ->
            if (fileBrowser.mode == ImGuiFileBrowser.Mode.LOAD) {
                PlaylistManager.loadPlaylist(file)
            } else {
                PlaylistManager.savePlaylist(file)
            }
        }

        deckBrowser.draw { file ->
            PlaylistManager.addToPlaylist(file)
        }

        ImGui.endPopup()
    }

    private const val POPUP_ID = "Playlist Editor###playlistPanel"
}
