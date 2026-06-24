# Audio Beat Synchronization & Stability

This document details the mechanics of the beat-tracking and Control Voltage (CV) synchronization system in Spirals Desktop. It outlines how audio beats are detected, how the tempo is estimated, and how the system remains stable despite fluctuations in the input signal's BPM.

---

## Architecture Overview

Beat synchronization bridges the real-time audio thread, the control voltage registry, and the rendering thread.

* **Audio Callback Thread**: Performs transient detection, filters out noise, estimates BPM, and increments a master beat counter.
* **Control Voltage Registry**: Dynamically interpolates the beat clock with sub-millisecond precision for rendering frame rate decoupling.
* **Render Thread**: Evaluates visual generators (e.g. mandalas, LFOs) synced to the phase of the beat clock.

---

## Phase Sync Pipeline

The beat synchronization flow consists of the following steps:

### 1. Transient / Onset Detection
In [AudioEngine.processAudio](file:///home/gj/projects/spirals-desktop/src/main/kotlin/llm/slop/spirals/audio/AudioEngine.kt#L66), incoming mono audio is split into three frequency bands using [BiquadFilter](file:///home/gj/projects/spirals-desktop/src/main/kotlin/llm/slop/spirals/audio/BiquadFilter.kt). The engine computes the **Spectral Flux** (the positive frame-to-frame change in RMS energy) for each band:

* Low band (bass): $Flux_{bass} = \max(0, RMS_{bass} - RMS_{bass, prev})$
* Mid band (vocals/instruments): $Flux_{mid} = \max(0, RMS_{mid} - RMS_{mid, prev})$
* High band (hi-hats/percussion): $Flux_{high} = \max(0, RMS_{high} - RMS_{high, prev})$

The bands are mixed into a single `onsetRaw` metric, which is normalized to `onsetNormalized` in the range $[0.0, 2.0]$:

$$\text{onsetRaw} = (Flux_{bass} \times 1.0) + (Flux_{mid} \times 0.6) + (Flux_{high} \times 0.3)$$

### 2. BPM Estimation
When `onsetNormalized` crosses a dynamic `beatThreshold`, a beat trigger candidate is recorded. The engine computes the interval (in nanoseconds) since the last detected beat:

$$\text{interval} = \text{currentTime} - \text{lastBeatTime}$$

To filter out drum fills, syncopated beats, and false positives, the engine maintains a sliding window of the last 8 valid beat intervals (between 40 and 200 BPM) in the `beatIntervals` collection. The **estimated BPM** is calculated using the **median** of these intervals:

$$\text{estimatedBpm} = \frac{60,000,000,000}{\text{medianInterval}}$$

Using the median instead of the mean makes the estimation highly robust to isolated timing outliers.

### 3. Flywheel Ticking
In between physical audio transients, a master beat counter `totalBeats` is incremented on every audio callback using a steady tempo flywheel:

$$\text{beatDelta} = \text{deltaTimeSec} \times \frac{\text{estimatedBpm}}{60.0}$$
$$\text{totalBeats} \leftarrow \text{totalBeats} + \text{beatDelta}$$

This ensures that even during quiet sections or periods of missing transients, the beat clock keeps ticking steadily at the last known tempo.

### 4. Gated Phase Realignment
To sync the accumulated beat phase with the actual audio stream, the phase is aligned on strong transients (`onsetNormalized > 1.4f`). The current phase is checked:

$$\text{currentPhase} = \text{totalBeats} \pmod{1.0}$$

If the phase is not already near the beat boundaries (i.e. $\text{currentPhase} \ge 0.1$ and $\text{currentPhase} \le 0.9$), the engine snaps `totalBeats` to the nearest integer:

$$\text{totalBeats} \leftarrow \text{round}(\text{totalBeats})$$

This gating prevents minor timing micro-variations from constantly shifting the clock phase.

### 5. High-Precision CV Sync
Once per render frame, [CVRegistry.updateAll](file:///home/gj/projects/spirals-desktop/src/main/kotlin/llm/slop/spirals/cv/CVRegistry.kt#L111) is called, which queries [CVRegistry.getSynchronizedTotalBeats](file:///home/gj/projects/spirals-desktop/src/main/kotlin/llm/slop/spirals/cv/CVRegistry.kt#L58-L63). This function interpolates the beat clock with sub-millisecond precision using the time elapsed since the last audio thread callback:

$$\text{synchronizedBeats} = \text{anchorBeats} + (\text{currentTime} - \text{anchorTimeNs}) \times \frac{\text{anchorBpm}}{60.0 \times 10^9}$$

The [BeatClock](file:///home/gj/projects/spirals-desktop/src/main/kotlin/llm/slop/spirals/cv/BeatClock.kt) source exposes this synchronized phase:

$$\text{value} = \text{synchronizedBeats} \pmod{1.0}$$

---

## Stability Tuning

For audio tracks with unstable rhythm or variable BPMs, several mechanisms can be tuned or added to stabilize the beat:

### Dynamic Thresholding
The detection threshold `beatThreshold` is updated continuously to adapt to track dynamics:
```kotlin
beatThreshold = (beatThreshold * 0.95f) + (onsetNormalized * 0.05f)
```
This ensures the engine remains sensitive during quiet parts and avoids double-triggering during loud, complex segments.

### Further Improvements for High BPM Variance
If the estimated BPM continues to experience jitter, developer enhancements include:
1. **Exponential Moving Average (EMA) BPM Smoothing**:
   Filter the estimated BPM to slow down tempo changes:
   ```kotlin
   estimatedBpm = (estimatedBpm * 0.95f) + (newEstimatedBpm * 0.05f)
   ```
2. **Phase-Locked Loop (PLL)**:
   Rather than abruptly snapping the phase using the `round()` function, adjust the flywheel speed slightly based on phase error:
   ```kotlin
   val error = targetPhase - currentPhase
   estimatedBpm += error * Gain
   ```
3. **Increasing the Median Window**:
   Change `maxIntervals` in [AudioEngine](file:///home/gj/projects/spirals-desktop/src/main/kotlin/llm/slop/spirals/audio/AudioEngine.kt#L42) to a higher value (e.g. 16 or 32) to require longer sustained changes before shifting the tempo.
4. **Manual BPM Override**:
   Allow locking the BPM to a fixed value to completely disable auto-tracking while maintaining phase-snapping.
