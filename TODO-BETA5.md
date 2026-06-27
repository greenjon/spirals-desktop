# Spirals Desktop - TODO BETA 5

This file consolidates all remaining tasks from previous `TODO.md`, `TODO-PATCH.md`, `TODO-VIDEO.md`, `ROADMAP.md`, `DOCS-TODO.md`, and `SETLIST-ADVANCE-TODO.md` files.

---

## 1. UI & Interaction Improvements
- [ ] **Subtle Panel Backgrounds for Subgroups**: In the Patch Grid, record the cursor position before a subgroup (like "Background" or "Feedback") starts and after it ends, then draw a subtle rounded rectangle behind the entire block using `dl.addRectFilled()`.
- [ ] **Grid Cell Traversal**: Enable grid cell traversal with arrow keys.
- [ ] **Cell Deletion Key**: Enable using the `DELETE` key to remove a patch modulator from the selected cell.
- [ ] **Undo Support**: Add `CTRL-Z` to undo a paste or delete action in the grid.
- [x] **Intuitive LFO Times**: Make LFO times more intuitive in the Cell Config panel.

## 2. DSP & Audio Analysis
- [ ] **Center Raw Audio**: Center the raw audio waveform in the sound analysis/audio monitor panel.
- [ ] **Stabilize Beat Phase**: Improve synchronization and stabilization of the beat phase.
- [ ] **Refine Audio Processing**: Continue optimizing real-time audio processing for efficiency and safety.

## 3. Hardware & External Integrations
- [x] **MIDI Hardware Hotplugging**: Implement MIDI hardware hotplugging with Threaded Polling to dynamically recover connected controllers.
- [ ] **Edge Cases for MIDI in Patches**: Investigate and fix potential edge cases when saving/loading patches containing MIDI assignments.

## 4. Performance & Diagnostics
- [ ] **OpenGL Profiling**: Implement driver debugging and OpenGL profiling (e.g. using `glGetDebugMessageLog` and `GLDebug` callback checks).
- [ ] **Shader Optimization**: Optimize shader compilation and runtime performance.

## 5. Build, Documentation & Distribution
- [ ] **Build Documentation & MkDocs**: Complete any remaining sections in the MkDocs documentation structure under `docs/`.
- [ ] **Inline Documentation**: Add comprehensive inline code comments across core Kotlin rendering and CV modules.
- [x] **Linux Distribution Preparation**: Package and prepare the workstation application for standard Linux distribution (packaging dependencies, launch scripts, etc.).
