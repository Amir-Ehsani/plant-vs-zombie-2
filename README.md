# Plants vs. Zombies 2

A LibGDX desktop remake of *Plants vs. Zombies 2* with adventure chapters, collection and shop systems, minigames, and optional online **I, Zombie**.

The playable game lives in [`pvz-proj/`](pvz-proj/).

## Features

- **Adventure** across Ancient Egypt, Frostbite Caves, Big Wave Beach, and Dark Ages, with plant selection, lawn combat, bosses, and plant food
- **Account progress** for coins, gems, collection, greenhouse, shop, quests, and MioPoint
- **Minigames** including I, Zombie (couch and online), Vasebreaker, Match Three, and Zombotany
- **Optional multiplayer**: authoritative TCP server for accounts, matchmaking, leaderboard, and online I, Zombie
- **Offline play** from the login screen when you do not want to start a server

## Requirements

- **Java 17** or newer
- Windows, macOS, or Linux

## Run from source

```bash
cd pvz-proj
./gradlew lwjgl3:run
```

On Windows:

```powershell
cd pvz-proj
.\gradlew.bat lwjgl3:run
```

## Offline play

On the login screen, check **Play offline (no server)**.

1. Register a local account, or log in with one you already created offline.
2. Adventure, collection, shop, quests, and couch minigames work without a server.
3. Online I, Zombie and the server leaderboard stay unavailable until you log in without that checkbox.

Local accounts are stored on the machine. They are not the same as server accounts.

## Multiplayer

Start the authoritative server from `pvz-proj`:

```bash
./gradlew :core:runServer
```

Default endpoint: `0.0.0.0:54555`. Override with `PVZ_SERVER_BIND` and `PVZ_SERVER_PORT`.

On the login screen, leave **Play offline** unchecked and set **Server host** / **Server port**. Then:

- register or log in against the server
- open **Quests → Minigames → I, Zombie** (or the main-menu online banner)
- challenge a player or join the random queue

The server owns the match. Clients send actions; both players render the same snapshot stream.

Couch I, Zombie is local: plants use the mouse, zombies use **W/S** or arrow keys for the lane, **1–5** to pick a zombie, and **Space/Enter** to spawn.

## Release JAR

Build a runnable fat JAR:

```bash
cd pvz-proj
./gradlew lwjgl3:jar
```

Output:

```
pvz-proj/lwjgl3/build/libs/plants-vs-zombies-failure-1.0.0.jar
```

Run it with Java 17+:

```bash
java -jar plants-vs-zombies-failure-1.0.0.jar
```

The first launch unpacks animation data to `%USERPROFILE%\.plants-vs-zombies-2\` (or `~/.plants-vs-zombies-2/`). That can take a minute. Later launches reuse the cache. Keep the working directory writable so local profiles can be saved.

## Controls

| Action | Input |
| --- | --- |
| Fullscreen | **F11** |
| Plant / interact | Mouse |
| Pause (in game) | In-game pause control |

## Project layout

```
pvz-proj/
  core/      Shared game logic, UI, and the TCP server
  lwjgl3/    Desktop launcher
  assets/    Art, audio, and animation data
  config/    Checkstyle rules
```

Useful Gradle tasks from `pvz-proj`:

| Task | Purpose |
| --- | --- |
| `lwjgl3:run` | Run the desktop game |
| `lwjgl3:jar` | Build the release JAR |
| `:core:runServer` | Start the multiplayer server |
| `linter` | Run Checkstyle |

```bash
./gradlew linter
```

## Scoring

MioPoint is awarded when a level ends:

`100 + (difficulty × 50) + (zombie kills × 10) + leftover sun + 500 if you win`

**Best MioPoint** is the highest single-level score, not a lifetime total.

## Networking in brief

Most of the game is local. Only online I, Zombie, server accounts, matchmaking, and the live leaderboard go through the server. Clients send commands; the server simulates the match and broadcasts snapshots.
