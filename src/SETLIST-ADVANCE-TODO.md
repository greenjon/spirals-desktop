Phase 3c: Setlist Advance Implementation
Add CV and global hardware controls to advance the Setlist forwards or backwards.
User Review Required
•
Is the proposed UIManager.advanceSetlist(delta) logic handling the Live Mode transitions correctly as envisioned?
Proposed Changes
Configuration & State
UITheme.kt
•
Add enum class SetlistTransitionBehavior { PROMPT, AUTO_DISCARD, AUTO_SAVE }
•
Add var setlistTransitionBehavior = SetlistTransitionBehavior.PROMPT
•
Add var setlistNextMidiCc = -1 and var setlistPrevMidiCc = -1
•
Update saveSettings() and loadSettings() to persist these new properties.
Patch-Level CV Mapping
Mixer.kt
•
Add val setlistPrev = ModulatableParameter(0.0f, maxClamp = 1.0f)
•
Add val setlistNext = ModulatableParameter(0.0f, maxClamp = 1.0f)
•
Maintain prevSetlistPrevVal and prevSetlistNextVal to detect 0.5f threshold crossings.
•
Add fun pollSetlistAdvance(): Int which evaluates the cross and returns +1, -1, or 0, then updates the previous history variables.
PatchModels.kt
•
Add val setlistNext: ParameterDto? = null and val setlistPrev: ParameterDto? = null to GlobalPatchDto.
•
Update GlobalPatchDto constructor/deserialization logic to support applying and saving these parameters.
UI & Global Logic
SetlistPanel.kt
•
Expose entries (or add a helper fun getFileOffset(current: File?, delta: Int): File?) to allow advancing.
UIManager.kt
•
Add global UI tracking in render() to intercept the hardcoded global setlistNextMidiCc / setlistPrevMidiCc from UITheme if they match the incoming MIDI polling.
•
Call mixer.pollSetlistAdvance() inside render(). Combine this delta with any global MIDI delta.
•
Add fun advanceSetlist(delta: Int) that delegates to SetlistPanel to find the target file.
•
Inside advanceSetlist, branch behavior based on UITheme.setlistTransitionBehavior:
◦
PROMPT: Use existing onLoadDirty path (displays confirmation modal).
◦
AUTO_DISCARD: Immediately performLoadFromSetlist().
◦
AUTO_SAVE: Call saveGlobalPatch() and then performLoadFromSetlist().
•
Add standard UI rows for the setlistNext and setlistPrev parameters in PatchGridPanel.kt.
SettingsPanel.kt
•
Add a new "Setlist & Live Mode" section to the modal.
•
Add an ImGui.combo to select the transition behavior.
•
Add ImGui.inputInt (or custom integer inputs) to assign the Global MIDI CCs for Next/Prev.
Verification Plan
Manual Verification
•
Test changing Global MIDI CCs in Settings. Fire those CCs and ensure they advance the setlist.
•
Test changing SetlistTransitionBehavior to AUTO_DISCARD and verify that modifying a patch, then advancing via UI/MIDI instantly drops changes and loads the next set.
•
Add an LFO to the Mixer's Setlist Next parameter and verify that when it crosses the 0.5 threshold, the patch advances.