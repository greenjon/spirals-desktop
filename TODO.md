# Spirals Desktop - TODO

## Upcoming Tasks

- [ ] **Sound Analysis Panel:**
  - Remove Bass Flux from the audio monitor.
  - Center the raw audio in the audio monitor.
  - Add a beat phase o-scope to the audio monitor.
  - Allow the audio monitor window to grow up to 90% of the vertical space.
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
  - Make all the deck controls read-only (to avoid confusion).
  - Add deck buttons to quickly disable/enable effects.
  - Add deck buttons to quickly mute/solo decks.
  - Fix intermittent bug with bypassing effects.
  - Enable grid cell traversal with arrow keys.
  - Enable using the DELETE key to remove the patch from the cell.
  - Add CTRL-Z to undo a paste or delete action.
  - Make LFO times more intuitive.
  
  Unorganized thoughts:
  - parameters should mostly either be 0 to 1 or -1 to 1 in their final value. SOme might be 0-3, or other things.
  - all the CV should follow the final value.
  - we need knobs that are only positive, and ones that are +/-, and knobs that are 360 degrees.
  - where we have a slider to set a value, add a number box that is a display and also allows typing into it.
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


