# Real-Time Audio & DSP

This section describes the real-time audio thread constraints, DSP band-splitting, and the onset/beat detection engine.

## Zero-Allocation Callback Safety

The JACK audio processing thread runs in a real-time thread context.

- **The Allocation Rule**: No objects may be allocated inside the callback (`new`, Kotlin lambdas that capture state, standard library collection instantiation, etc.).
- **Blocking Operations**: File I/O, database access, network requests, print statements, and mutex locks are strictly forbidden.
- **Pre-Allocation**: All arrays, filters, buffers, and objects used in the callback must be allocated in advance during the initialization phase.

---

## DSP Pipeline & Band Splitting

The incoming mono or stereo audio stream is processed sequentially to extract CV parameters:

### Biquad IIR Filter
- Class: `BiquadFilter.kt`
- Implements a low-latency, zero-allocation infinite impulse response (IIR) filter.
- Split frequencies are set to isolate Low (Bass), Mid, and High frequency bands.

### FFT Processing
- Class: `DSP.kt`
- Performs short-time Fourier Transform on pre-allocated float arrays.
- Computes spectral flux (rate of change of frequency bins) for the low end.

### Amplitude Extractor (RMS)
- Class: `AmplitudeExtractor.kt`
- Computes Root Mean Square (RMS) values over short windows:
  $$RMS = \sqrt{\frac{1}{N} \sum_{i=1}^N x_i^2}$$
- These values are mapped directly to `amp`, `bass`, `mid`, and `high` CV registers.

---

## Onset & BPM Estimation

Transient signals are extracted to synchronize visual shifts with the musical tempo.

- **Spectral Flux**: Measures increases in frequency bin energy between consecutive frames.
- **Onset Detection**: Triggers when spectral flux exceeds a dynamic, rolling threshold.
- **BPM Sync**: Updates the central `BeatClock` using JACK sync anchors when available, or interpolates the tempo based on the timing of consecutive onsets.
