# Core Concepts

Spirals Desktop is structured around a few primary visual generation and mixing systems. Understanding these concepts will help you build complex, audio-reactive live performances.

## Mandala Synthesis Engine

The core generative visual source in Spirals is the **Mandala**. It uses mathematical formulas to draw complex, symmetrical geometric shapes.

### Lobes & Geometry
- **Lobe Count (Petals)**: Dictates the rotational symmetry of the mandala (how many arms or repeating patterns are generated).
- **Ratios & Libraries**: The system includes a curated library of about 300 classic mandala ratio presets that determine how overlapping lines intersect and form intricate patterns.

### Color Cycles & Palettes
- The mandala's colors dynamically cycle through color space based on parametric rates.
- Color cycles can be modulated by external audio CVs to change palette characteristics on musical accents.

### 3D Harmonograph Option
- **3D Z-Axis**: Extrudes the 2D mandala into 3D using a two-component damped sine equation:
  $$Z(t) = A_5 \sin(f_5 t + p_5) e^{-d_5 t} + A_6 \sin(f_6 t + p_6) e^{-d_6 t}$$
  providing a physical harmonograph decay and complex spatial geometry.
- **3D Controls**: Allows setting parameters for Z amplitudes (`Z Amp 1`, `Z Amp 2`), frequencies (`Z Freq 1`, `Z Freq 2`), damping (`Z Damp 1`, `Z Damp 2`), and phase offsets (`Z Phase 1`, `Z Phase 2`).
- **Yaw, Pitch & Roll**: Supports 3D rotation of the mandala in space. Like all parameters, these can be modulated by CV sources (e.g. Bass CV modulating Yaw/Pitch).
- **Perspective Projection**: Slide between a flat Orthographic view (`3D Persp` = 0) and an immersive, deep Perspective view (`3D Persp` = 1).

### Feedback Loop (Ping-Pong FBOs)
- Each deck uses a dual Framebuffer Object (FBO) feedback loop.
- The output of the current frame is slightly scaled, rotated, blurred, or shifted in hue, and then blended back into the background of the next frame.
- This creates long-exposure trails, fluid organic movement, and standard video-feedback zoom effects.

---

## Dual-Deck Mixer

Spirals follows a traditional DJ/VJ layout, featuring two independent decks that feed into a central mixer.

### Deck A & Deck B
- Each deck acts as an independent generator running its own Visual Source (currently a Mandala).
- Decks possess individual settings for feedback parameters (Decay, Gain, Zoom, Rotate, Hue Shift, Blur, Chroma Offset, and Feedback Mode).

### Blending Modes
- The central Mixer blends the outputs of Deck A and Deck B using select blending equations: Additive blend (`ADD`), Screen blend (`SCREEN`), Multiply blend (`MULT`), Maximum/Lighten (`MAX`), or standard crossfade (`XFADE`).

### Crossfader Controls
- The crossfader (`crossfade`) slider controls the interpolation weight between Deck A and Deck B.
- Like all parameters, the crossfader can be modulated by a CV source (for example, audio-derived `bass` or an `LFO`) to alternate decks automatically in sync with the beat.
