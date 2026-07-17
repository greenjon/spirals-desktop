# Testing Patterns

**Analysis Date:** 2026-07-07

## Test Framework

**Runner:**
- Kotlin test via `testImplementation(kotlin("test"))` in `build.gradle.kts`.
- Gradle wrapper: Gradle 8.5 from `gradle/wrapper/gradle-wrapper.properties`.
- Kotlin JVM plugin: 2.0.21 in `build.gradle.kts`.
- Config: `build.gradle.kts`. No separate `junit`, `vitest`, `jest`, or coverage config file detected.

**Assertion Library:**
- `kotlin.test` assertions: `assertEquals`, `assertTrue`, `assertFalse`, `assertNull`, `assertNotNull`.
- MockK 1.13.8 for mocks, singleton mocks, static extension mocks, relaxed mocks, and verification.

**Run Commands:**
```bash
./gradlew test              # Run all tests on Unix-like shells
.\gradlew.bat test          # Run all tests on Windows PowerShell/cmd
./gradlew --no-daemon test  # Use when the local Gradle daemon cannot bind its lock-listener socket
```

## Test File Organization

**Location:**
- Tests are under `src/test/kotlin/llm/slop/liquidlsd/` and mirror production package areas.
- Patch/persistence tests live in `src/test/kotlin/llm/slop/liquidlsd/patches/`.
- Rendering utility tests live in `src/test/kotlin/llm/slop/liquidlsd/rendering/`.
- UI/resource inspection tests live in `src/test/kotlin/llm/slop/liquidlsd/ui/`.

**Naming:**
- Use `*Test.kt` filenames and `*Test` classes: `SessionStateTest`, `PlayQueueManagerTest`, `DirtyStateTest`, `DeckUtilityTest`, `FontInspectorTest`.
- Test methods currently use `test...` names, for example `testSequentialNoRepeatForward`, `testDeckDirtyState`, and `testSessionStateDtoSerializationAndBackwardCompatibility`.

**Structure:**
```
src/test/kotlin/llm/slop/liquidlsd/
├── patches/
│   ├── DirtyStateTest.kt
│   ├── PlayQueueManagerTest.kt
│   └── SessionStateTest.kt
├── rendering/
│   └── DeckUtilityTest.kt
└── ui/
    └── FontInspectorTest.kt
```

## Test Structure

**Suite Organization:**
```kotlin
class PlayQueueManagerTest {
    private val mixer = mockk<Mixer>(relaxed = true)

    @BeforeTest
    fun setUp() {
        mockkObject(PatchManager)
        every { PatchManager.isDeckDirty(any(), any()) } returns false
        every { PatchManager.loadDeckPresetAsync(any(), any()) } returns Unit
        PlayQueueManager.clearQueue()
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(PatchManager)
    }

    @Test
    fun testSequentialNoRepeatForward() {
        PlayQueueManager.isRepeatEnabled = false
        PlayQueueManager.isShuffleEnabled = false
        PlayQueueManager.appendToQueue(File("presets/patches/patch1.lsd"))

        PlayQueueManager.triggerNext(mixer)

        assertEquals(0, PlayQueueManager.activeIndex)
    }
}
```

**Patterns:**
- Keep setup local to each test class with `@BeforeTest`; reset singleton state before assertions. See `src/test/kotlin/llm/slop/liquidlsd/patches/DirtyStateTest.kt` and `src/test/kotlin/llm/slop/liquidlsd/patches/PlayQueueManagerTest.kt`.
- Use `@AfterTest` to unmock MockK object mocks. `src/test/kotlin/llm/slop/liquidlsd/patches/PlayQueueManagerTest.kt` unmockkObject restores `PatchManager`.
- Prefer direct state assertions after command calls: queue index assertions in `src/test/kotlin/llm/slop/liquidlsd/patches/PlayQueueManagerTest.kt`; preset cache assertions in `src/test/kotlin/llm/slop/liquidlsd/rendering/DeckUtilityTest.kt`.
- Use real DTO values where equality semantics matter: `src/test/kotlin/llm/slop/liquidlsd/patches/DirtyStateTest.kt`.
- Use serialization round trips to verify backward-compatible DTO defaults: `src/test/kotlin/llm/slop/liquidlsd/patches/SessionStateTest.kt`.

## Mocking

**Framework:** MockK

**Patterns:**
```kotlin
mockkObject(PatchManager)
every { PatchManager.isDeckDirty(any(), any()) } returns false
every { PatchManager.loadDeckPresetAsync(any(), any()) } returns Unit

val mixer = mockk<Mixer>()
val deckA = mockk<Deck>()
every { mixer.deckA } returns deckA

mockkStatic("llm.slop.liquidlsd.models.PatchModelsKt")
every { deckA.toDto(any(), any()) } returns dtoA
verify { deckA.toDto(any()) }
```

**What to Mock:**
- Mock rendering graph objects (`Mixer`, `Deck`) when testing patch/deck orchestration without OpenGL. See `src/test/kotlin/llm/slop/liquidlsd/rendering/DeckUtilityTest.kt`.
- Mock singleton managers when a test needs to isolate global side effects. See `PatchManager` object mocking in `src/test/kotlin/llm/slop/liquidlsd/patches/PlayQueueManagerTest.kt`.
- Mock extension functions from `PatchModelsKt` when asserting orchestration logic that depends on `toDto`/`applyDto`. See `src/test/kotlin/llm/slop/liquidlsd/patches/DirtyStateTest.kt`.

**What NOT to Mock:**
- Do not mock DTO equality when testing dirty-state semantics. Use real `ParameterDto` and `DeckPatchDto` objects as in `src/test/kotlin/llm/slop/liquidlsd/patches/DirtyStateTest.kt`.
- Do not instantiate real OpenGL, GLFW, ImGui, JACK, or audio engine flows in unit tests unless the test is explicitly a native/resource smoke test. Most current tests avoid `src/main/kotlin/llm/slop/liquidlsd/Main.kt`, `src/main/kotlin/llm/slop/liquidlsd/rendering/Renderer.kt`, and `src/main/kotlin/llm/slop/liquidlsd/audio/AudioEngine.kt`.
- Do not rely on real preset files for queue unit tests; current queue tests use `File("presets/patches/patch1.lsd")` as value objects without reading them.

## Fixtures and Factories

**Test Data:**
```kotlin
val dummyParam = ParameterDto(
    baseValue = 0.5f,
    baseMin = 0.0f,
    baseMax = 1.0f,
    randomizeBase = false,
    modulators = emptyList()
)

val cachedDeckDto = DeckPatchDto(
    name = "Test",
    visualSourceType = "Mandala",
    parameters = mapOf("Lobes" to realInitial),
    feedbackParameters = emptyMap(),
    globalAlpha = globalAlpha
)
```

**Location:**
- Fixtures are inline in test methods. No shared fixture/factory directory detected.
- Large JSON compatibility fixture strings are embedded directly in `src/test/kotlin/llm/slop/liquidlsd/patches/SessionStateTest.kt`.
- Resource fixtures used by tests are production resources under `src/main/resources/fonts/`, especially `src/main/resources/fonts/lucide.ttf` for `src/test/kotlin/llm/slop/liquidlsd/ui/FontInspectorTest.kt`.

## Coverage

**Requirements:** None enforced. No JaCoCo, Kover, or coverage threshold configuration detected in `build.gradle.kts`.

**View Coverage:**
```bash
# Not configured
```

## Test Types

**Unit Tests:**
- Primary test type. Tests cover DTO compatibility, dirty-state equality, queue sequencing/shuffle behavior, and deck copy/move/swap orchestration.
- Important unit test files: `src/test/kotlin/llm/slop/liquidlsd/patches/SessionStateTest.kt`, `src/test/kotlin/llm/slop/liquidlsd/patches/DirtyStateTest.kt`, `src/test/kotlin/llm/slop/liquidlsd/patches/PlayQueueManagerTest.kt`, `src/test/kotlin/llm/slop/liquidlsd/rendering/DeckUtilityTest.kt`.

**Integration Tests:**
- Lightweight resource/native smoke testing exists in `src/test/kotlin/llm/slop/liquidlsd/ui/FontInspectorTest.kt`, which loads `src/main/resources/fonts/lucide.ttf` and uses STB TrueType to inspect glyphs.
- No separate integration-test source set detected.

**E2E Tests:**
- Not used. No UI automation, application launch, OpenGL rendering, JACK/audio, or full workflow E2E framework detected.

## Common Patterns

**Async Testing:**
```kotlin
mockkObject(PatchManager)
every { PatchManager.loadDeckPresetAsync(any(), any()) } returns Unit

PlayQueueManager.triggerNext(mixer)

assertEquals(0, PlayQueueManager.activeIndex)
```
- Current tests avoid waiting on `CompletableFuture` background work by mocking async boundaries such as `PatchManager.loadDeckPresetAsync` in `src/test/kotlin/llm/slop/liquidlsd/patches/PlayQueueManagerTest.kt`.
- When adding tests for `PatchManager.loadDeckPresetAsync` or save methods in `src/main/kotlin/llm/slop/liquidlsd/patches/PatchManager.kt`, expose deterministic synchronization or test the queued DTO application separately through `applyPendingPatches`.

**Error Testing:**
```kotlin
val decodedOld = json.decodeFromString<SessionStateDto>(oldJsonStr)

assertNull(decodedOld.bloom)
assertFalse(decodedOld.isRepeatEnabled)
assertFalse(decodedOld.isShuffleEnabled)
```
- Existing tests focus on fallback/default behavior rather than expecting thrown exceptions.
- Add explicit error-path tests around JSON parsing, missing preset files, and resource failures when changing `src/main/kotlin/llm/slop/liquidlsd/patches/PatchManager.kt`, `src/main/kotlin/llm/slop/liquidlsd/ui/FileSystemManager.kt`, or `src/main/kotlin/llm/slop/liquidlsd/rendering/Shader.kt`.

## Verification Notes

- `.\gradlew.bat test` failed before test execution in this environment because the Gradle daemon could not bind its file-lock listener socket.
- `.\gradlew.bat --no-daemon test` completed successfully on 2026-07-07.
- Use `--no-daemon` for local verification when daemon startup reports `java.net.BindException: Address already in use: Cannot bind`.

---

*Testing analysis: 2026-07-07*
