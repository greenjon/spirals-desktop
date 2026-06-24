# Architecture Overview

This section covers the high-level system design, threading boundaries, and safety models of Spirals Desktop.

## High-Level Video Pipeline

Spirals processes real-time audio and generates framebuffers through sequential stages:

```
JACK Audio ──► AudioEngine ──► CVRegistry
                                    │  (every frame: updateAll)
                          ┌──────────┴──────────┐
                       Deck A                Deck B
                    (Mandala source)     (Mandala source)
                          │                    │
                ModulatableParams          ModulatableParams
                evaluated via CV          evaluated via CV
                          │                    │
                     cleanFBO             cleanFBO
                          │                    │
                    feedback.frag         feedback.frag
                    (ping-pong FBOs)      (ping-pong FBOs)
                          └──────────┬──────────┘
                                 Mixer.kt
                               mixer.frag
                                   │
                              masterFBO ──► screen
```

---

## Threading Boundaries

Spirals runs on two primary threads: the **OS Main Thread** and the **JACK Audio Thread**. Maintaining strict separation between them is critical for real-time safety.

### Thread 0 (OS Main / Rendering Thread)
- **Duties**: GLFW event polling, OpenGL context creation, OpenGL draw calls (rendering to Decks, Mixer, and Screen), ImGui interface drawing.
- **Rules**: All LWJGL 3 GLFW window and OpenGL context manipulations must run on Thread 0.

### Real-Time Audio Thread (JACK Callback)
- **Duties**: Periodically receives raw audio buffer chunks from the audio server, runs bandpass filtering, FFT calculations, RMS amplitude extraction, and onset detection.
- **Rules**: Must be *completely non-blocking* and *zero-allocation*.

---

## Concurrency Safety & Lock-Free Data Passing

Since the audio thread operates under strict real-time constraints and the rendering thread runs at screen refresh rates (60Hz+), lock-based synchronization between them could lead to audio glitches (xruns) or frame stuttering.

### Lock-Free Buffers
- **`CvHistoryBuffer`**: A pre-allocated, ring buffer used to pass historical CV values from the audio engine to the main thread for drawing oscilloscopes and UI monitoring.
- **Volatile References**: Thread-safe variables (`@Volatile` annotation in Kotlin) are used for variables like beat anchors and BPM estimations, enabling quick, thread-safe reads and writes.
- **Concurrent Collections**: Thread-safe structures like `ConcurrentLinkedQueue` are used to queue incoming MIDI CC events from device listeners before they are polled on the render loop.
