# TODO: Fullscreen and Multi-Monitor Video Output Support

This document outlines the tasks and architectural approach for implementing main window fullscreen/clean mode toggling ("f" key) and automatic/toggled external monitor/projector output ("Spacebar" key).

---

## Task List

### 1. GLFW Keyboard Event Integration
- [ ] Register a custom GLFW key callback on the primary window in [Main.kt](file:///home/gj/projects/spirals-desktop/src/main/kotlin/llm/slop/spirals/Main.kt).
- [ ] Implement callback chaining to preserve ImGui input handling:
  ```kotlin
  val imguiKeyCallback = glfwSetKeyCallback(window) { winHandle, key, scancode, action, mods ->
      if (action == GLFW_PRESS) {
          when (key) {
              GLFW_KEY_F -> toggleMainWindowCleanMode()
              GLFW_KEY_SPACE -> toggleSecondaryMonitorWindow()
          }
      }
      // Chain to the previous callback (ImGui's) to prevent breaking UI inputs
  }
  ```

### 2. Main Window "Clean Mode" (F Key)
- [ ] Add a `var cleanModeEnabled: Boolean` flag in [UIManager.kt](file:///home/gj/projects/spirals-desktop/src/main/kotlin/llm/slop/spirals/ui/UIManager.kt) or a global state manager.
- [ ] When `cleanModeEnabled` is `true`:
  - Force `UITheme.backgroundVideoEnabled = true` to render the master video output.
  - Skip rendering any ImGui control panels (e.g. Patch Grid, Cell Config, Mixer/Monitor).
  - (Optional) Either hide the Main Menu Bar completely, or auto-hide it to maximize clean real estate.
- [ ] When `cleanModeEnabled` is toggled off, restore the previous `backgroundVideoEnabled` state and ImGui layout rendering.

### 3. External Monitor Auto-Detection & Creation
- [ ] During initialization in [Main.kt](file:///home/gj/projects/spirals-desktop/src/main/kotlin/llm/slop/spirals/Main.kt), query connected monitors:
  ```kotlin
  val monitors = glfwGetMonitors() // PointerBuffer of monitor handles
  val primaryMonitor = glfwGetPrimaryMonitor()
  ```
- [ ] Implement a detection utility to find the first available non-primary monitor.
- [ ] If an external monitor is detected on startup:
  - Automatically create the secondary GLFW window on that monitor.
  - Retrieve the monitor's video mode using `glfwGetVideoMode(externalMonitor)`.
  - Initialize the secondary window in fullscreen mode with a shared OpenGL context:
    ```kotlin
    val secondaryWindow = glfwCreateWindow(
        mode.width(), mode.height(), "Spirals Output", externalMonitor, primaryWindowHandle
    )
    ```

### 4. Secondary Window Toggle (Spacebar Key)
- [ ] Track the secondary window handle `var secondaryWindow: Long? = null` in the main loop context.
- [ ] When `Spacebar` is pressed:
  - If `secondaryWindow` is open (non-null), destroy it safely:
    ```kotlin
    glfwDestroyWindow(windowHandle)
    secondaryWindow = null
    ```
  - If `secondaryWindow` is closed (null), detect an external monitor and open the secondary window fullscreen. If no external monitor is found, open a borderless/windowed secondary preview window on the primary screen.

### 5. Dual-Window Render Loop Coordination
- [ ] Coordinate the frame execution inside the main render loop on the primary OS thread (Thread 0):
  - **Phase 1: Update & Render to FBOs**: Run deck updates and composite the final mix to `mixer.masterFBO` (using the primary window context).
  - **Phase 2: Render Main Window**: Render background video blit (if enabled) and the UI/panels, then call `glfwSwapBuffers(primaryWindow)`.
  - **Phase 3: Render Secondary Window**:
    - If `secondaryWindow != null`:
      - Switch OpenGL context: `glfwMakeContextCurrent(secondaryWindow)`.
      - Set viewport to match secondary window size.
      - Bind `blitShader` and `mixer.masterFBO.texture`.
      - Draw the fullscreen quad (no UI).
      - Call `glfwSwapBuffers(secondaryWindow)`.
      - Restore primary context: `glfwMakeContextCurrent(primaryWindow)`.

### 6. Safety, Threading & Resource Cleanup
- [ ] Ensure all GLFW window creation/destruction and OpenGL context switching (`glfwMakeContextCurrent`) is run strictly on Thread 0.
- [ ] In the application shutdown hook, verify the secondary window is closed and destroyed to prevent memory leaks:
  ```kotlin
  secondaryWindow?.let { 
      glfwDestroyWindow(it) 
  }
  ```

---

## Verification & Testing Plan

### 1. Single Monitor Testing
- Verify pressing `f` toggles the UI visibility on and off, displaying only the master video output on the main window.
- Verify pressing `Spacebar` opens/closes a secondary windowed output (since no secondary monitor is present).

### 2. Multi-Monitor / Projector Testing
- Connect an external display or projector.
- Launch the application:
  - Verify that the secondary fullscreen output automatically spawns on the external display without any user interaction.
  - Verify that the primary monitor displays only the control UI panels.
- Press `Spacebar`:
  - Verify that the external output closes cleanly.
- Press `Spacebar` again:
  - Verify that the external output re-opens fullscreen on the projector/external display.
