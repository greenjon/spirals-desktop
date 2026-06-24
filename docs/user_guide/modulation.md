# CV Modulation

The heart of Spirals Desktop is its Control Voltage (CV) modulation matrix. This system allows you to route audio characteristics or generator signals to modulate any rendering parameter.

## The Patch Grid

The Patch Grid is a visual modulation matrix displayed in the left panel of the application.

- **Rows**: The parameters available for modulation (grouped hierarchically: Mixer, Deck A, Deck B, along with subcategories like Geometry, Color, and Feedback).
- **Columns**: The active CV sources.
- **Grid Cells**: The intersection points where you can create a modulator linking a CV source to a parameter.
  - Active cells display a faint circle containing an animated dot indicating the current real-time value of the CV signal.
  - Bypassed cells are rendered in muted grey.

---

## CV Sources

CV sources generate values in the range of `0.0` to `1.0`. They are divided into two main categories:

### Audio-Derived Sources (Extracted from JACK Input)
- **`amp`**: Overall root-mean-square (RMS) amplitude of the incoming audio signal.
- **`bass`**: RMS amplitude in the low-frequency band.
- **`mid`**: RMS amplitude in the mid-frequency band.
- **`high`**: RMS amplitude in the high-frequency band.
- **`bassFlux`**: The rate of change in the bass frequency spectrum, useful for detecting bass transients.
- **`onset`**: A brief impulse trigger generated on detected audio transients.
- **`accent`**: A transient trigger generated on strong musical accents.
- **`bpm`**: The estimated tempo of the input track.

### Generator Sources (Calculated in Real-Time)
- **`beatPhase`**: A ramp signal going from `0.0` to `1.0` linearly over the course of each beat, synchronized with the audio engine's beat clock.
- **`lfo`**: A continuous oscillator (e.g. sine, triangle, square, sawtooth) running at a designated time-based speed.
- **`sampleAndHold`**: A random value generator that steps to a new value at beat subdivisions, with optional glide smoothing between steps.

---

## Modulators & Operator Math

When you select a grid cell, the **Cell Config Panel** (middle panel) opens. Here, you can configure how the CV source affects the target parameter.

### Modulator Attributes
- **Bypass/Active Toggle**: Instantly enable or disable this specific modulation routing.
- **Weight (Weight Slider)**: Controls the strength and direction of the modulation.
- **Operator**: Defines how the modulation value is combined with the parameter's base value:
  - **ADD**: The modulation value is added directly:
    $$result = baseValue + (cv * weight)$$
  - **MUL**: The parameter's base value is scaled:
    $$result = baseValue * (1.0 + (cv * weight))$$
  - **SCALE**: The parameter's base value is scaled relative to the weight:
    $$result = baseValue * (1.0 - weight + (cv * weight))$$

*The final evaluated parameter value is always clamped to its defined limits (usually `0.0` to `1.0`).*

---

## Oscilloscopes & Monitors

Monitoring real-time values is essential to dialing in responsive patches.

- **Cell-Specific Oscilloscope**: Inside the Cell Config panel, a scrolling waveform display shows the history of the selected CV source and how it affects the parameter's evaluated output.
- **Sound Analysis Panel**: (Plan under active development) A dedicated window displaying the raw audio waveform, frequency band splits, and calculated BPM estimate.
