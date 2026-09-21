# Plants vs. Zombies 2 (desktop)

LibGDX desktop client and optional TCP server. See the repository root [README](../README.md) for features, offline play, multiplayer, and release instructions.

## Quick start

```bash
./gradlew lwjgl3:run
```

Windows:

```powershell
.\gradlew.bat lwjgl3:run
```

## Common tasks

| Task | Command |
| --- | --- |
| Run game | `./gradlew lwjgl3:run` |
| Run server | `./gradlew :core:runServer` |
| Build JAR | `./gradlew lwjgl3:jar` |
| Lint | `./gradlew linter` |

The runnable JAR is written to `lwjgl3/build/libs/plants-vs-zombies-failure-1.0.0.jar`.

## Modules

- `core` — game logic, UI, and `PvZServer`
- `lwjgl3` — desktop launcher and fat JAR
- `assets` — packed art, audio, and PAM animations
