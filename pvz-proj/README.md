# Plants vs. Zombies 2 (desktop)

LibGDX desktop client and optional TCP server. See the repository root [README](../README.md) for play instructions, features, controls, and credits.

## Quick start

```bash
# Windows
gradlew.bat lwjgl3:run

# macOS / Linux
./gradlew lwjgl3:run
```

## Common tasks

| Task | Command |
| :--- | :--- |
| Run game | `./gradlew lwjgl3:run` |
| Run server | `./gradlew :core:runServer` |
| Build JAR | `./gradlew lwjgl3:jar` |
| Lint | `./gradlew linter` |

The runnable JAR is written to `lwjgl3/build/libs/plants-vs-zombies-failure-1.0.0.jar`.
