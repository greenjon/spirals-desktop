# Presets & Project Management V2

This document outlines the planned improvements for loading, saving, and managing presets and
global projects in Spirals Desktop.

---

## The Problem

The current system has two distinct pain points, ordered by severity:

### 1. Global Projects — Native OS File Dialogs (Highest Priority)
`loadGlobalPatchWithDialog()` and `saveGlobalPatch()` both call `java.awt.FileDialog`, which
spawns a native OS window on top of the OpenGL context. This is the most disruptive issue:
it breaks immersion completely, causes a jarring visual context switch every time the user
loads or saves a live set, and is unacceptably slow for a live performance instrument.

### 2. Deck Presets — Flat `ImGui.combo` Dropdown (Scalability)
The deck preset selector uses a flat `ImGui.combo` list seeded from `presets/decks/*.json`.
For a small library (≤10 patches) this is fine. As the library grows into the dozens, a
flat dropdown becomes hard to navigate quickly under performance pressure. There is also no
way to group or label patches by mood, style, or set section.

Because VJs typically have dozens — not thousands — of structural patches, we do not need
a full library management system with smart playlists and nested crates. We need something
lightweight, immersive, and fast.

---

## The Solution

An entirely ImGui-native approach using:
- A searchable popup browser with tag filtering (deck presets)
- A custom ImGui file browser replacing all `java.awt.FileDialog` calls
- A quick-load setlist panel for live project switching

---

## 3-Phase Implementation Plan

### Phase 1 — Global ImGui File Browser *(Most Disruptive Fix)*

**Goal:** Eliminate all `java.awt.FileDialog` calls. This is ordered first because it fixes
the highest-severity regression and is self-contained.

**Scope:**
- Build a reusable `ImGuiFileBrowser` component rendered as an `ImGui.beginPopupModal`.
- The modal blocks interaction while open (preventing accidental parameter changes).
- Directory navigation uses `java.io.File` calls on the main thread (not the audio thread),
  which is safe and does not require any async machinery.
- The browser opens locked to `presets/global/` by default for Save operations (no need
  to navigate away) and opens the last-used directory for Load operations.

**Replaces:**
- `UIManager.loadGlobalPatchWithDialog()` — the `java.awt.FileDialog(LOAD)` call.
- `UIManager.saveGlobalPatch()` — the `java.awt.FileDialog(SAVE)` call.

**UI Requirements:**
- Path breadcrumb at the top (clickable to navigate up).
- Scrollable file list showing `.json` files only.
- Text input at the bottom for filename (pre-populated with the current project name on Save).
- `Save` / `Open` and `Cancel` buttons. `Cancel` closes the modal with no side effects.
- Existing "unsaved changes" confirmation guard (`drawConfirmPopup`) must remain intact —
  the file browser opens *after* the user has confirmed discarding unsaved changes, not before.

---

### Phase 2 — Deck Preset Browser (Search & Tags)

**Goal:** Replace the `ImGui.combo` in `drawDeckHeader()` with a searchable popup that
scales comfortably to dozens of presets.

#### 2a. Data Model — Tags Field

Add a `tags` field to `DeckPatchDto`:

```kotlin
@Serializable
data class DeckPatchDto(
    val version: Int = 1,
    val name: String,
    val tags: List<String> = emptyList(), // NEW — defaults to empty list
    ...
)
```

**Backward compatibility:** `PatchManager` already configures `Json { ignoreUnknownKeys = true }`.
Existing preset files (e.g., `presets/decks/test.json`) will deserialize cleanly with
`tags = emptyList()` — no migration script is required.

#### 2b. Preset Browser Popup

Replace the `ImGui.combo("##preset_$label", ...)` call with an `ImGui.openPopup` trigger
and an `ImGui.beginPopup` panel:

- **Search bar:** Text input at the top filtering preset names by substring (case-insensitive).
- **Tag filter row:** A row of toggleable pill buttons for all tags that exist in the loaded
  preset files (e.g., `[Ambient]`, `[Strobe]`, `[Geo]`, `[Drone]`). Multiple tags can be
  active simultaneously (AND filter). Tags derived from filenames at startup.
- **Preset list:** Scrollable list of filtered results. Active preset is highlighted.
  Dirty indicator (`*`) is preserved on the active row label.
- **Clicking a preset:** Calls `loadDeckPreset()` and closes the popup.

#### 2c. Save / "Save As" Dialog

The existing Save and Save As flows in `drawDeckSaveMenu()` already use a text input in
an `ImGui.beginPopupModal`. Update this modal to include:
- A `Tags:` text input (comma-separated, e.g., `ambient, geo`).
- The entered tags are parsed, trimmed, and stored in `DeckPatchDto.tags` on save.

**Dirty-state guard:** The existing `*` suffix indicator and the `drawConfirmDeckSavePopup`
modal must carry forward into the new browser UI without regression.

---

### Phase 3 — Setlist & Quick-Load *(Live Performance)*

**Goal:** Allow a VJ to switch between pre-arranged global projects during a live set
without touching a file browser.

#### 3a. Setlist Directory Convention

Designate `presets/global/` (which currently exists but is empty) as the canonical home
for both ad-hoc projects and live sets. No second directory is needed.

Live sets should use a numeric prefix naming convention for natural sort order:
```
presets/global/
  01_Intro.json
  02_Build.json
  03_Drop.json
  04_Outro.json
```

#### 3b. Quick-Load Panel

Add a collapsible "Setlist" section to the top menu bar or a dedicated `SetlistPanel`:
- Reads all `.json` files from `presets/global/` at startup and on a manual Refresh button.
- Displays them sorted alphabetically (numeric prefix gives the VJ full ordering control).
- Clicking a row triggers the same "unsaved changes" guard as the menu Load action, then
  calls `PatchManager.loadGlobalPatchAsync()`.

#### 3c. MIDI / Keyboard "Next / Previous Project"

Allow advancing the setlist without looking at the UI.

> **Dependency note:** This feature requires a general-purpose *action-binding* system
> (mapping MIDI CCs or keyboard shortcuts to named actions) that does not yet exist in the
> codebase. Implementation of Phase 3c should be deferred until an action-binding layer is
> designed, or scoped as a minimal "setlist-only" MIDI binding added directly to
> `MIDIManager` as a stopgap.

Minimum viable implementation (stopgap):
- Two reserved MIDI CC slots (configurable in `spirals-settings.properties`):
  `setlist.midi.next` and `setlist.midi.prev`.
- `MIDIManager` checks these CCs on each message and fires a
  `SetlistManager.advanceProject(+1)` / `advanceProject(-1)` call.
- `SetlistManager` maintains the current index and queues the load via
  `PatchManager.loadGlobalPatchAsync()`.

Full action-binding integration can replace the stopgap in a later pass.