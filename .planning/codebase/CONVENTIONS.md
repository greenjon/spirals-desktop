# Coding Conventions

**Analysis Date:** 2026-07-07

## Naming Patterns

**Files:**
- Use one primary Kotlin type per file with PascalCase filenames: `src/main/kotlin/llm/slop/liquidlsd/ui/UIManager.kt`, `src/main/kotlin/llm/slop/liquidlsd/rendering/Renderer.kt`, `src/main/kotlin/llm/slop/liquidlsd/audio/JackClient.kt`.
- Use domain package directories under `src/main/kotlin/llm/slop/liquidlsd/`: `audio`, `cv`, `midi`, `models`, `parameters`, `patches`, `rendering`, `ui`, and `utils`.
- Put serialization models and conversion extensions in `src/main/kotlin/llm/slop/liquidlsd/models/PatchModels.kt`.
- Put ImGui panels and UI helper objects in `src/main/kotlin/llm/slop/liquidlsd/ui/`, for example `src/main/kotlin/llm/slop/liquidlsd/ui/AssetBrowserPanel.kt`, `src/main/kotlin/llm/slop/liquidlsd/ui/SettingsPanel.kt`, and `src/main/kotlin/llm/slop/liquidlsd/ui/CustomRangeSlider.kt`.
- Put OpenGL/rendering resources in `src/main/resources/shaders/` and font resources in `src/main/resources/fonts/`.
- Test filenames use `*Test.kt` and mirror the production package: `src/test/kotlin/llm/slop/liquidlsd/patches/PlayQueueManagerTest.kt`, `src/test/kotlin/llm/slop/liquidlsd/rendering/DeckUtilityTest.kt`, `src/test/kotlin/llm/slop/liquidlsd/ui/FontInspectorTest.kt`.

**Functions:**
- Use lowerCamelCase for functions and methods: `triggerNext`, `loadDeckPresetAsync`, `applyPendingPatches`, `renderMixer`, `calculateWaveform`.
- Use verb-first names for mutating or effectful operations: `saveSession`, `loadSession`, `appendToQueue`, `removeFromQueue`, `initializeShuffle`.
- Use `toDto` and `applyDto` extension functions for DTO conversion and restoration in `src/main/kotlin/llm/slop/liquidlsd/models/PatchModels.kt`.
- Use private helper functions for scoped implementation details: `getExternalMonitor`, `createSecondaryWindow`, and `destroySecondaryWindow` in `src/main/kotlin/llm/slop/liquidlsd/Main.kt`; `emptyDeckDto` and `handleDirtyDeck` in `src/main/kotlin/llm/slop/liquidlsd/patches/PatchManager.kt` and `src/main/kotlin/llm/slop/liquidlsd/patches/PlayQueueManager.kt`.

**Variables:**
- Use lowerCamelCase for local values and properties: `activePresetA`, `cachedDtoA`, `pendingFontSize`, `targetCrossfade`, `feedbackShader`.
- Use boolean names with `is`, `has`, or capability language: `isAutoVJEnabled`, `isShuffleEnabled`, `isDisposed`, `hasFeedback`.
- Use concise graphics/audio local names only where the surrounding context is dense and established: `w`, `h`, `sw`, `sh` in `src/main/kotlin/llm/slop/liquidlsd/Main.kt`; `aVal`, `bVal`, `cVal`, `dVal` in `src/main/kotlin/llm/slop/liquidlsd/rendering/Renderer.kt`.
- Use nullable cached state explicitly when domain state can be absent: `cachedGlobalDto`, `defaultGlobalPatchDto`, `activePresetC`, `lastBgVideoEnabled`.

**Types:**
- Use PascalCase for classes, objects, interfaces, data classes, and enums: `PatchManager`, `PlayQueueManager`, `ModulatableParameter`, `VisualSource`, `DeckPatchDto`, `MeterType`.
- DTO types end with `Dto`: `ModulatorDto`, `ParameterDto`, `DeckPatchDto`, `SessionStateDto`, `PlaylistDto`.
- Enum constants use uppercase with underscores: `MONOPOLAR`, `BIPOLAR`, `AUTO_DISCARD`, `PAGE_UP_DOWN`.
- Singleton managers use Kotlin `object`: `src/main/kotlin/llm/slop/liquidlsd/patches/PatchManager.kt`, `src/main/kotlin/llm/slop/liquidlsd/midi/MidiEngine.kt`, `src/main/kotlin/llm/slop/liquidlsd/cv/CVRegistry.kt`, `src/main/kotlin/llm/slop/liquidlsd/ui/UITheme.kt`.

## Code Style

**Formatting:**
- Tool used: Not detected. No `.editorconfig`, KtLint, Detekt, Spotless, or IntelliJ formatter config is present.
- Key settings: Infer Kotlin defaults from source. Use 4-space indentation, braces on the same line, trailing commas only where already useful in multiline calls, and explicit line breaks for long constructor or function arguments.
- Keep package declarations first, then imports, then top-level logger or declarations: `src/main/kotlin/llm/slop/liquidlsd/Main.kt`, `src/main/kotlin/llm/slop/liquidlsd/audio/JackClient.kt`, `src/main/kotlin/llm/slop/liquidlsd/rendering/Shader.kt`.
- Avoid adding formatting-only churn. Existing files contain extra blank lines and mixed import grouping, especially `src/main/kotlin/llm/slop/liquidlsd/ui/UIManager.kt`; keep new code clean without reformatting unrelated sections.

**Linting:**
- Tool used: Not detected.
- Key rules: Rely on Kotlin compiler checks from `build.gradle.kts` and focused tests under `src/test/kotlin/`.
- Suppress deprecation locally where legacy MIDI mapping fields are intentionally preserved: `@Suppress("DEPRECATION")` in `src/main/kotlin/llm/slop/liquidlsd/models/PatchModels.kt` and `src/main/kotlin/llm/slop/liquidlsd/parameters/ModulatableParameter.kt`.

## Import Organization

**Order:**
1. Package declaration.
2. External library imports such as `imgui.*`, `kotlinx.serialization.*`, `mu.KotlinLogging`, `org.lwjgl.*`, and `io.mockk.*`.
3. Java standard library imports such as `java.io.File`, `java.nio.FloatBuffer`, and `java.util.concurrent.*`.
4. Project imports under `llm.slop.liquidlsd.*`.
5. Kotlin standard imports such as `kotlin.math.roundToInt` and `kotlin.test.*`.

**Path Aliases:**
- Not applicable. Kotlin imports use fully qualified package paths rooted at `llm.slop.liquidlsd`.
- Do not introduce custom Gradle source-set aliases or path alias mechanisms. Add code under `src/main/kotlin/llm/slop/liquidlsd/<domain>/` and import by package.

## Error Handling

**Patterns:**
- Fail fast for unrecoverable startup and graphics initialization problems with `RuntimeException`: `src/main/kotlin/llm/slop/liquidlsd/Main.kt`, `src/main/kotlin/llm/slop/liquidlsd/rendering/Shader.kt`, `src/main/kotlin/llm/slop/liquidlsd/rendering/VisualSourceRegistry.kt`.
- Use `try`/`catch` around file I/O, JSON parsing, shader loading, native APIs, and background tasks, then log through KotlinLogging: `src/main/kotlin/llm/slop/liquidlsd/patches/PatchManager.kt`, `src/main/kotlin/llm/slop/liquidlsd/ui/FileSystemManager.kt`, `src/main/kotlin/llm/slop/liquidlsd/ui/PlaylistManager.kt`.
- Return safe defaults for recoverable user/data failures: `emptyList()` from playlist parsing in `src/main/kotlin/llm/slop/liquidlsd/patches/PlayQueueManager.kt`, `null` from file helpers in `src/main/kotlin/llm/slop/liquidlsd/ui/FileSystemManager.kt`, and `false` from validation-style operations.
- Use optional fallback paths for backwards compatibility, for example `.lsd` to `.json` fallback in `src/main/kotlin/llm/slop/liquidlsd/patches/PatchManager.kt`.
- For native resource allocation, use `try`/`finally` to free memory, as in `MemoryUtil.memAllocFloat` and `MemoryUtil.memFree` in `src/main/kotlin/llm/slop/liquidlsd/rendering/Renderer.kt`.
- In JACK callbacks, catch `Throwable` inside the callback, store it in atomics, and log from a scheduled background logger instead of logging directly in the real-time callback: `src/main/kotlin/llm/slop/liquidlsd/audio/JackClient.kt`.

## Logging

**Framework:** KotlinLogging with Logback

**Patterns:**
- Define `private val logger = KotlinLogging.logger {}` at file scope or as a private class/object property: `src/main/kotlin/llm/slop/liquidlsd/Main.kt`, `src/main/kotlin/llm/slop/liquidlsd/patches/PatchManager.kt`, `src/main/kotlin/llm/slop/liquidlsd/ui/UITheme.kt`.
- Use lazy logging lambdas: `logger.info { "..." }`, `logger.debug { "..." }`, `logger.warn { "..." }`, `logger.error(e) { "..." }`.
- Use `info` for lifecycle and user-visible state changes, `debug` for frequent diagnostics such as FPS and scan counts, `warn` for recoverable missing optional services, and `error` when an operation fails.
- Avoid `println` in production code. Current `println` usage is limited to Gradle packaging tasks in `build.gradle.kts` and diagnostic test output in `src/test/kotlin/llm/slop/liquidlsd/ui/FontInspectorTest.kt`.
- Never log inside the JACK process callback. Use the deferred atomic error pattern in `src/main/kotlin/llm/slop/liquidlsd/audio/JackClient.kt`.

## Comments

**When to Comment:**
- Comment around native, rendering, audio, or compatibility logic where the reason is not obvious: `src/main/kotlin/llm/slop/liquidlsd/rendering/Renderer.kt`, `src/main/kotlin/llm/slop/liquidlsd/patches/PatchManager.kt`, `src/main/kotlin/llm/slop/liquidlsd/audio/JackClient.kt`.
- Use comments to preserve migration/backward-compatibility intent for DTOs and session formats: `src/main/kotlin/llm/slop/liquidlsd/models/PatchModels.kt`.
- Use comments sparingly in UI code to mark non-obvious ImGui frame timing or ID-stack constraints: `src/main/kotlin/llm/slop/liquidlsd/ui/UIManager.kt`.
- Do not add generic restatement comments for simple property assignments or obvious branches.

**JSDoc/TSDoc:**
- Use Kotlin KDoc for public or important domain types and methods: `ModulatableParameter` in `src/main/kotlin/llm/slop/liquidlsd/parameters/ModulatableParameter.kt`, `JackClient` in `src/main/kotlin/llm/slop/liquidlsd/audio/JackClient.kt`, and `Renderer` in `src/main/kotlin/llm/slop/liquidlsd/rendering/Renderer.kt`.
- Prefer short KDoc with parameter/return notes only where they guide usage, for example `handleDirtyDeck` in `src/main/kotlin/llm/slop/liquidlsd/patches/PlayQueueManager.kt`.

## Function Design

**Size:** Keep new functions focused and move repeated UI/rendering behavior into helpers or small panel objects. Existing large files such as `src/main/kotlin/llm/slop/liquidlsd/ui/AssetBrowserPanel.kt`, `src/main/kotlin/llm/slop/liquidlsd/ui/UIManager.kt`, and `src/main/kotlin/llm/slop/liquidlsd/rendering/Renderer.kt` are operational hubs; add new behavior to narrower collaborators when possible.

**Parameters:** Prefer explicit primitive/domain parameters over maps for regular functions. Use DTOs for persistence boundaries: `DeckPatchDto`, `GlobalPatchDto`, and `SessionStateDto` in `src/main/kotlin/llm/slop/liquidlsd/models/PatchModels.kt`.

**Return Values:** Return `Unit` for command-style mutations, booleans for proceed/skip decisions, nullable values for optional lookup, and DTOs for snapshots. Examples: `handleDirtyDeck(): Boolean`, `getExternalMonitor(): Long?`, `Deck.toDto(): DeckPatchDto`.

## Module Design

**Exports:** Kotlin files expose package-visible public types by default. Use `private` aggressively for implementation helpers and mutable internals: `private val logger`, `private fun emptyDeckDto`, `private fun renderMandala`, and private callback state.

**Barrel Files:** Not used. Import concrete files/classes by package path; do not add index/barrel modules.

**State and Threading Constraints:**
- Keep OpenGL and GLFW operations on the main thread in `src/main/kotlin/llm/slop/liquidlsd/Main.kt`, `src/main/kotlin/llm/slop/liquidlsd/rendering/Renderer.kt`, and `src/main/kotlin/llm/slop/liquidlsd/rendering/Geometry.kt`.
- Pass background-loaded patch data through thread-safe queues and apply it on the main thread: `ConcurrentLinkedQueue` fields and `applyPendingPatches` in `src/main/kotlin/llm/slop/liquidlsd/patches/PatchManager.kt`.
- Use lock-free/concurrent collections for shared queue and CV state: `CopyOnWriteArrayList` in `src/main/kotlin/llm/slop/liquidlsd/patches/PlayQueueManager.kt` and `src/main/kotlin/llm/slop/liquidlsd/parameters/ModulatableParameter.kt`; `ConcurrentHashMap.newKeySet` in `src/main/kotlin/llm/slop/liquidlsd/patches/PlayQueueManager.kt`.
- For ImGui native allocations, use scoped LWJGL memory management and free native objects. Existing style cleanup is explicit in `src/main/kotlin/llm/slop/liquidlsd/ui/UIManager.kt`; OpenGL buffers are freed in `src/main/kotlin/llm/slop/liquidlsd/rendering/Renderer.kt`.
- Do not allocate, block, perform file/network I/O, or log inside JACK audio callbacks. Pre-allocate state outside the callback and report errors through atomics as in `src/main/kotlin/llm/slop/liquidlsd/audio/JackClient.kt`.

---

*Convention analysis: 2026-07-07*
