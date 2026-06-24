# Operations & Tuning

This guide covers performance optimization, JVM tuning, diagnostic commands, and audio/graphics troubleshooting for different target operating systems.

## JVM Tuning for Low-Latency Performance

To ensure real-time rendering and low-latency audio processing without drops or frames stutters, the Java Virtual Machine must be tuned carefully.

### Z Garbage Collector (ZGC)
Use the Z Garbage Collector, which is designed for low-latency workloads with pause times under a millisecond.
Enable it using the following JVM flags:
```bash
-XX:+UseZGC -XX:MaxGCPauseMillis=2
```

### Platform-Specific Optimization Flags

#### Linux (x86_64 & arm64)
Ensure your user has real-time priority access configured in `/etc/security/limits.d/audio.conf`:
```text
@audio   -   rtprio   95
@audio   -   memlock  unlimited
```
Run with memory locking to prevent JVM heap swapping:
```bash
java -XX:+UseZGC -XX:MaxGCPauseMillis=2 -jar spirals-desktop.jar
```

#### macOS (Intel & Apple Silicon)
Set the JVM priority class higher if possible and use native libraries compiled for the specific architecture (e.g. M1/M2 native libs vs. Intel x86_64 libs) to avoid Rosetta 2 translation overhead.

#### Windows
Ensure the GPU is set to "High Performance" mode in the Windows Graphics Settings for `java.exe`. Map ASIO drivers using JACK2 to prevent Windows Audio Session API (WASAPI) latency.

---

## Audio Connectivity Diagnostics (Linux CLI)

When debugging input ports and routing, use the following tools:

### `jack_lsp`
Lists all active JACK/PipeWire ports:
```bash
jack_lsp
```
Use `jack_lsp -c` to see active connection links between ports.

### `jack_connect`
Manually wire ports together (e.g., routing system capture to spirals):
```bash
jack_connect system:capture_1 spirals:input_1
jack_connect system:capture_2 spirals:input_2
```

### `pw-link`
Use `pw-link` in PipeWire environments to inspect and establish links:
```bash
pw-link -l                       # List links
pw-link system:capture_1 spirals:input_1   # Establish connection
```

---

## Troubleshooting Guide

### 1. JVM Crash on Startup (LWJGL/OpenGL)
- **Symptoms**: The application crashes immediately upon start with a native log file (`hs_err_pid.log`).
- **Causes**:
  - OpenGL 3.3 context could not be created (unsupported driver/integrated GPU).
  - Calling OpenGL functions or polling GLFW windows off Thread 0 (violating LWJGL single-thread safety).
- **Solutions**:
  - Verify your graphics drivers are up to date.
  - Review the OpenGL logs. In development, check the stdout for lines starting with `GLDebug`.

### 2. Audio Dropouts (xruns)
- **Symptoms**: Stuttering audio, click sounds, or error logs showing `xrun` in the terminal.
- **Solutions**:
  - Increase your JACK/PipeWire buffer size (e.g. from 128 to 256 or 512 frames) or lower the sample rate (from 96kHz to 48kHz).
  - Ensure ZGC is active and check the GC logs to confirm pause times are not exceeding the audio buffer period.

### 3. MIDI CC Input Not Registering
- **Symptoms**: Rotating knobs does not trigger MIDI Learn or update CV values.
- **Solutions**:
  - Confirm the MIDI device is plugged in and recognized by the OS (`lsusb` or MIDI Control Panel).
  - Make sure another app does not have exclusive lock access to the MIDI device.
  - Check the startup logs to ensure `MidiEngine` output lists the connected device.
