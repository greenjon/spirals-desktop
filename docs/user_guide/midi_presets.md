# Presets & MIDI Mapping

Spirals Desktop contains patch saving mechanisms and external MIDI controller integration.

## Preset & Patch Management

Patches store the complete state of the workstation, including all parameter values, feedback settings, and active CV modulations.

### Serialization Format
- Patches are saved as JSON files (`.json`) using the `kotlinx.serialization` library.
- By default, presets are saved in the `presets/` folder in the project root.
- The default patch file `default.json` is loaded automatically on startup.

### Copy & Paste (Base Column)
- You can copy the base parameter settings of one deck and paste them onto the other.
- Use the right-click menu or keyboard shortcuts within the Base column of the Patch Grid to copy/paste parameter structures.

---

## MIDI Controller Mapping

You can control parameter base values and grid cell modulations using external hardware controllers.

### MIDI Learn Mode
1. **Activate Learn Mode**: In the UI, click the MIDI button next to a target parameter slider or grid cell.
2. **Send CC Message**: Rotate a knob, move a slider, or press a button on your connected MIDI controller.
3. **Map Confirmation**: The system will detect the MIDI Control Change (CC) message, read the channel and CC ID, and bind it to the target automatically.

### Where MIDI Assignments are Saved
- **Direct Integration**: MIDI mapping configurations are stored directly inside the visual patch file.
  - When you map a MIDI CC to a parameter's base value, the `mappedMidiId` field (e.g., `midi_cc_0_42`) is stored inside the `ParameterDto` structure.
  - When you map a MIDI CC to modulate a cell, the mapping is saved as a `CvModulator` with its `sourceId` set to the midi ID.
- **Portability**: Because the MIDI map is stored within the patch, saving the patch automatically preserves your MIDI mappings. Loading the patch restores the MIDI assignments.
- *Note: A dedicated, standalone MIDI map preset manager (to swap MIDI mappings independent of the visual patches) is planned for a future update.*
