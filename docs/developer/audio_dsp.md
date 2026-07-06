# Real-Time Audio & DSP

This section describes the real-time audio thread constraints, DSP band-splitting, and the onset
detection engine that feeds the CV registry.

## Zero-Allocation Callback Safety

The JACK audio processing thread runs in a real-time context managed by the OS audio server.

- **The Allocation Rule**: No objects may be allocated inside the callback. This means no `new`,
  no Kotlin lambdas that capture state, and no standard library collection instantiation.
- **Blocking Operations**: File I/O, database access, network requests, print statements, and mutex
  locks are strictly forbidden.
- **Pre-Allocation**: All arrays, filters, buffers, and objects used in the callback are allocated
  during `AudioEngine` object initialization, before the JACK client starts.

As of the current build, the three filter output buffers (`lowBuffer`, `midBuffer`, `highBuffer`)
are pre-allocated as `FloatArray(16384)` — the maximum JACK buffer size — so the callback never
needs to resize them.

---

## DSP Pipeline & Band Splitting

The incoming mono audio stream is processed sequentially each callback to extract CV parameters.

### Biquad IIR Filter Bank

- Class: [`BiquadFilter.kt`](file:///home/gj/projects/spirals-desktop/src/main/kotlin/llm/slop/spirals/audio/BiquadFilter.kt)
- Three biquad IIR filters run in parallel: a **low-pass** (≤150 Hz), **band-pass** (≈1000 Hz),
  and **high-pass** (≥5000 Hz), splitting the stream into bass, mid, and high frequency bands.
- IIR filters are inherently zero-allocation: state is held in a fixed set of float coefficients
  updated only on sample rate change.

### Amplitude Extractor (RMS)

- Class: [`AmplitudeExtractor.kt`](file:///home/gj/projects/spirals-desktop/src/main/kotlin/llm/slop/spirals/audio/AmplitudeExtractor.kt)
- Computes Root Mean Square amplitude over each callback block for each band:

$$RMS = \sqrt{\frac{1}{N} \sum_{i=1}^N x_i^2}$$

- The overall amplitude is computed directly from the input `FloatBuffer` before filtering.
  The per-band RMS values are computed from the filter output arrays.

### Published CV Values

After each callback, these values are pushed to `CVRegistry`:

| CV ID | Source |
|-------|--------|
| `audio_amp` | Full-band RMS × normalisation factor |
| `audio_bass` | Low-pass RMS |
| `audio_mid` | Band-pass RMS |
| `audio_high` | High-pass RMS |
| `trigger_onset` | Half-wave rectified multi-band spectral flux |
| `trigger_accent` | Peak-hold + decay envelope of onset strength |

---

## Onset Detection

Transient signals are extracted to drive trigger CV values and beat detection.

### Spectral Flux (Onset Strength)

On each callback, the engine computes the positive frame-to-frame change in RMS energy for each
band (half-wave rectified):

$$Flux_{band} = \max(0,\ RMS_{band}(t) - RMS_{band}(t-1))$$

Bands are weighted and summed into a single onset strength signal:

$$\text{onsetStrength} = Flux_{bass} \times 2.0 + Flux_{mid} \times 0.8 + Flux_{high} \times 0.3$$

This weighting favours bass/kick transients, which are the most musically relevant for beat sync.

### Silence Gate

A silence gate (`silenceThresholdDb`, default −40 dBFS) suppresses the beat flywheel during quiet
periods. When the overall RMS stays below this threshold for more than 500 ms, `SignalState`
switches to `SILENT` and the beat counter stops incrementing.

---

## Beat Flywheel

Between audio callbacks, the master beat counter `totalBeats` is incremented sample-accurately:

$$\text{beatDelta} = \frac{N_{frames}}{f_s} \times \frac{BPM_{estimated}}{60.0}$$

$$\text{totalBeats} \leftarrow \text{totalBeats} + \text{beatDelta}$$

This ensures the beat clock keeps ticking at a steady rate even during quiet sections. The BPM
source is either `manualBpm` (when `isBpmLocked = true`) or the auto-detected value from
`BeatDetector`.

Once per callback, the new `BeatAnchor(totalBeats, estimatedBpm, currentTimeNs)` is published to
`CVRegistry` via a single `AtomicReference` swap, giving the render thread a consistent snapshot
without any lock.
