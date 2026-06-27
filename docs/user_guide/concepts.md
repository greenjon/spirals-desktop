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
