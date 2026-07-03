package llm.slop.spirals.ui

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import mu.KotlinLogging
import java.io.File

/**
 * Quick-load Setlist panel (Phase 3b).
 *
 * Reads all `.json` files from `presets/global/` and displays them as a
 * clickable list sorted alphabetically (numeric prefixes give the VJ full
 * ordering control, e.g. `01_Intro.json`, `02_Build.json`).
 *
 * Clicking a row fires [onLoad] with the chosen [File].  The caller is
 * responsible for the "unsaved changes" guard -- [onLoad] is only invoked
 * after the guard has been satisfied (or there are no unsaved changes).
 *
 * The panel is rendered as a collapsible `ImGui.beginPopupModal` triggered
 * from the menu bar "Setlist" item.
 */
object SetlistPanel {

    private val logger = KotlinLogging.logger {}

    private val presetsDir = File("presets/global")

    // -- State -----------------------------------------------------------------

    private var pendingOpen = false
    private var entries: List<File> = emptyList()

    // -- Public API ------------------------------------------------------------

    /** Schedule the panel to open on the next [draw] call. */
    fun open() {
        scan()
        pendingOpen = true
    }

    /** Find a file in entries relative to the current file by a delta offset, clamping to list bounds. */
    fun getFileOffset(current: File?, delta: Int): File? {
        scan()
        if (entries.isEmpty()) return null
        val currentCanonical = current?.canonicalPath
        val currentIndex = entries.indexOfFirst { it.canonicalPath == currentCanonical }
        if (currentIndex == -1) {
            // Start from the first file if the current file is not active or not in the setlist
            return entries.first()
        }
        val targetIndex = (currentIndex + delta).coerceIn(0, entries.lastIndex)
        return entries[targetIndex]
    }

    /**
     * Draw the setlist modal.  Must be called every frame from the render thread.
     *
     * @param currentFile   The currently loaded project file (highlighted in the list).
     * @param isDirty       Whether the current project has unsaved changes.
     * @param onLoad        Called with the chosen [File] when the user clicks a row
     *                      and there are no unsaved changes, or after the caller has
     *                      handled the unsaved-changes guard.
     * @param onLoadDirty   Called with the chosen [File] when the user clicks a row
     *                      but [isDirty] is true -- the caller should show the
     *                      "unsaved changes" confirm popup and then call [onLoad].
     */
    fun draw(
        currentFile: File?,
        isDirty: Boolean,
        onLoad: (File) -> Unit,
        onLoadDirty: (File) -> Unit
    ) {
        if (pendingOpen) {
            ImGui.openPopup(POPUP_ID)
            pendingOpen = false
        }

        ImGui.setNextWindowSize(400f, 480f, imgui.flag.ImGuiCond.Appearing)
        ImGui.setNextWindowPos(
            ImGui.getIO().displaySizeX * 0.5f,
            ImGui.getIO().displaySizeY * 0.5f,
            imgui.flag.ImGuiCond.Appearing,
            0.5f, 0.5f
        )

        val flags = ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove
        if (!ImGui.beginPopupModal(POPUP_ID, flags)) return

        // Header row
        UITheme.h3("Setlist")
        ImGui.sameLine(ImGui.getContentRegionAvailX() - 70f)
        if (ImGui.button("Refresh##setlistRefresh")) {
            scan()
        }

        ImGui.spacing()
        ImGui.textDisabled("presets/global/  --  click a project to load it")
        ImGui.separator()

        // Scrollable list
        val listH = 340f
        ImGui.beginChild("##setlistEntries", ImGui.getContentRegionAvailX(), listH, false)

        if (entries.isEmpty()) {
            ImGui.textDisabled("No .json files found in presets/global/")
            ImGui.textDisabled("Save a project there to build your setlist.")
        } else {
            for (file in entries) {
                val isActive = file.canonicalPath == currentFile?.canonicalPath
                val label = if (isActive && isDirty) "${file.nameWithoutExtension} *"
                            else file.nameWithoutExtension

                if (ImGui.selectable(label, isActive)) {
                    if (isDirty) {
                        onLoadDirty(file)
                        ImGui.closeCurrentPopup()
                    } else {
                        onLoad(file)
                        ImGui.closeCurrentPopup()
                    }
                }
            }
        }

        ImGui.endChild()

        ImGui.separator()
        ImGui.spacing()

        if (ImGui.button("Close##setlistClose", 90f, 0f)) {
            ImGui.closeCurrentPopup()
        }

        ImGui.endPopup()
    }

    // -- Private helpers -------------------------------------------------------

    private const val POPUP_ID = "Setlist###setlistPanel"

    /**
     * Reads `presets/global/` and rebuilds [entries] sorted alphabetically.
     * Numeric prefixes (`01_`, `02_`, ...) give the VJ full ordering control.
     */
    private fun scan() {
        if (!presetsDir.exists()) presetsDir.mkdirs()
        entries = (presetsDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray())
            .sortedBy { it.name.lowercase() }
        logger.debug { "Setlist scanned: ${entries.size} projects in ${presetsDir.path}" }
    }
}
