# OpenGL Rendering Pipeline

This section details the OpenGL graphics rendering pipeline, framebuffer architecture, shader systems, and mandala generation math.

## Framebuffer Object (FBO) Ping-Pong Architecture

To perform feedback effects (decay, zoom, rotation, blur), the system uses ping-pong framebuffers:

- **FBO Class**: `FBO.kt` wraps OpenGL Framebuffer creation, texture attachments, and viewport setup.
- **Ping-Pong Loop**:
  1. Render the current visual source (the clean mandala geometry) to `cleanFBO`.
  2. Bind the target `feedbackFBO`.
  3. Draw a fullscreen quad rendering `feedback.frag`. Pass the previous frame's feedback texture, the `cleanFBO` texture, and modulation parameters (Zoom, Rotate, Decay) as uniforms.
  4. Swap/ping-pong the read and write feedback textures.
  5. The output is blended and composited in `Mixer.kt` via `mixer.frag` to `masterFBO`.

---

## Shader compilation & uniform binding

Shaders are compiled dynamically from files located in `src/main/resources/shaders/`.

- **Shader Class**: `Shader.kt` compiles vertex and fragment GLSL shaders, links them into a program, and validates for linking errors.
- **OpenGL Debug Context**: The development configuration maintains `GLFW_OPENGL_DEBUG_CONTEXT` active, printing warnings and errors via the `GLDebug` callback immediately.
- **Uniform Mapping**: Uniforms (floats, vectors) are bound by locating uniform locations with `glGetUniformLocation` and uploading evaluated parameters once per frame.

---

## Mandala Geometry Math

The mandala's vertex rendering uses polar equations mapped to Cartesian coordinates:

- Polar equations define the radius $r(\theta)$ based on coefficients from the selected `MandalaRatio`:
  $$r(\theta) = f(\theta, a, b, c, d)$$
- Convert polar points to Cartesian coordinates for the shader pipeline:
  $$x = r(\theta) \cos(\theta)$$
  $$y = r(\theta) \sin(\theta)$$
- These points form vertices of a **ribbon** — each vertex carries a `(phase, side)` attribute pair,
  where `side` is `+1` or `-1`, expanding the curve into a band with finite width.
  The ribbon is rendered as a `GL_TRIANGLE_STRIP`.

---

## Dynamic Visual Sources

In addition to the built-in `Mandala` source, the rendering engine supports fully pluggable
dynamic visual sources loaded at runtime from `presets/sources/`.

- **`VisualSourceRegistry`**: Scans `presets/sources/` on startup and compiles each subfolder's
  `shader.frag` against the shared vertex shader.
- **`DynamicVisualSource`**: Wraps a compiled `Shader` and a `LinkedHashMap` of `ModulatableParameter`
  objects. Parameters are injected as `uniform float u<Name>` each frame.
- **Shader Ownership**: Master instances in the registry own their `Shader` object (`ownsShader = true`).
  Clones assigned to Decks share the shader reference but do not dispose it.
- **Error Fallback**: If a custom shader fails to compile, a red checkerboard fallback shader is used
  so the rest of the application keeps running.
