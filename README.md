# Spirals Desktop

Open source audio-reactive visual performance software for VJs.

## Status
🚧 Early development - Project structure established

## Tech Stack
- Kotlin/JVM
- LWJGL 3 (OpenGL)
- ImGui (UI)
- JACK Audio

## Roadmap & Active Tasks
- See `ROADMAP.md` for long-term project planning and completed milestones.
- See `TODO.md` for the list of active tasks and features currently under development.

## Building

```bash
./gradlew build
```

## Running

```bash
./gradlew run
```

## License
MIT
```

---

## **Your Project Structure Should Look Like:**

```
spirals-desktop/
├── .gitignore
├── LICENSE
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   └── wrapper/
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── llm/slop/spirals/
│       │       ├── Main.kt
│       │       ├── audio/
│       │       ├── cv/
│       │       ├── midi/
│       │       ├── parameters/
│       │       ├── rendering/
│       │       ├── ui/
│       │       └── patches/
│       └── resources/
│           ├── shaders/
│           │   ├── blit.vert
│           │   └── blit.frag
│           ├── patches/
│           │   └── default.json
│           └── logback.xml
└── build/
```

---

## **Now Test Everything!**

```bash
# In VS Code terminal:
./gradlew run
