# Spirals Desktop - TODO

## Upcoming Tasks

- [ ] **Sound Analysis Panel:**
 x - Remove Bass Flux from the audio monitor.
  - Center the raw audio in the audio monitor.
x  - Add a beat phase o-scope to the audio monitor.
x  - Allow the audio monitor window to grow up to 90% of the vertical space.
  - Stabilize the Beat phase.
- [ ] **MIDI Hardware Hotplugging:**
  - Implement MIDI hardware hotplugging with Threaded Polling.
- [ ] **Build Documentation:**
  - Write and organize MkDocs documentation.
  - Use Gemini 3.5 Flash (Medium) for boilerplate and structure.
  - Use Gemini 3.1 Pro (High) for the complicated, deep technical sections.
- [ ] **Base Column Copy/Paste:**
  - Make copy/paste work in the Base parameter column.
- [ ] **Consider edge cases for storing MIDI data in patches:**
  - What can go wrong? Because, it will.
- [ ] **Deck and Parameter Grid Improvements:**
x  - Make all the deck controls read-only (to avoid confusion).
  - Fix intermittent bug with bypassing effects.
  - Enable grid cell traversal with arrow keys.
  - Enable using the DELETE key to remove the patch from the cell.
  - Add CTRL-Z to undo a paste or delete action.
  - Make LFO times more intuitive.
  
  Unorganized thoughts:
    - beside the number box, have a pair of buttons to increment/decrement the value
  - make the entire area of the double buttons also change the cursor to a double dong, and have click-draging increment/decrement the value
  - in final, we should be able to set the base value, 
  - and also offset (DC, basically) the value of the CVs
  - and also gain
  - if we do that than we don't need the base column
  - should we have a little "fine tune" bar beside the long bars?
  - right now, when we modulate a wave, it's additive. should we do freq/ampl/phase?
  - should beat and lfo be combined, to allow them to modulate each other?
  - 


