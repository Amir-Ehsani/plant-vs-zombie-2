<h1 align="center">Plants vs. Zombies 2</h1>

<p align="center">
  <strong>A desktop Plants vs. Zombies 2 remake in Java</strong>
  <br/>
  Defend four worlds, grow your collection, and duel friends in I, Zombie.
</p>

<p align="center">
  <a href="#play-the-game">Play</a> ·
  <a href="#features">Features</a> ·
  <a href="#controls">Controls</a> ·
  <a href="#build-from-source">Build</a> ·
  <a href="#project-structure">Structure</a>
</p>

---

A fan-made tower-defense built with **libGDX** and **LWJGL3**. Plant a lawn, survive chapter bosses, unlock the collection and shop, then take I, Zombie offline on the couch or online against another player.

> **Fan project.** This is an unofficial recreation. Plants vs. Zombies, its characters, art, music, and names belong to [PopCap Games](https://www.ea.com/games/plantsvszombies) and Electronic Arts. This repository is not affiliated with or endorsed by PopCap or EA.

---

## Play the game

The runnable desktop build is a **fat JAR**: game code, natives, and every asset the game loads are packed inside a single file.

1. Install **Java 17 or newer** ([Adoptium Temurin](https://adoptium.net/) is a good choice).
2. Download `client.jar` from [Releases](https://github.com/Amir-Ehsani/plant-vs-zombie-2/releases).
3. Double-click the JAR, or run:

```bash
java -jar client.jar
```

The window opens at 1280×720. Press **F11** for fullscreen.

The first launch unpacks animation data to `%USERPROFILE%\.plants-vs-zombies-2\` (Windows) or `~/.plants-vs-zombies-2/` (macOS / Linux). That can take a minute. Later launches reuse the cache.

A local copy of the same build is produced at `pvz-proj/lwjgl3/build/libs/plants-vs-zombies-failure-1.0.0.jar` after `gradlew lwjgl3:jar`.

On the login screen, check **Play offline (no server)** to register a local account and skip the multiplayer server. Adventure, collection, shop, quests, and couch minigames all work that way. Online I, Zombie and the live leaderboard need a server login.

---

## Features

| | |
| :--- | :--- |
| **Adventure** | Four chapters, plant selection, lawn combat, Plant Food, and a boss fight at the end of each world. |
| **Worlds** | Ancient Egypt graves, Ice Cave slip and freeze, Wave Beach tides, and Dark Ages night rules. |
| **Progression** | Coins, gems, collection, greenhouse, shop, quests, and **MioPoint** high scores. |
| **Minigames** | I, Zombie (couch and online), Vasebreaker, Wall-nut Bowling, Beghouled, and Zombotany. |
| **Offline play** | Local accounts from the login checkbox. No server process required. |
| **Online I, Zombie** | Authoritative TCP matchmaking. Clients send actions; both players render the same snapshot stream. |

<p align="center">
  <em>Ancient Egypt · Ice Cave · Wave Beach · Dark Ages</em>
</p>

---

## Worlds

Each chapter has its own terrain, zombies, and a level-4 boss.

| World | What changes on the lawn |
| :--- | :--- |
| **Ancient Egypt** | Graves, tomb-raising waves, and a Sphinx boss. |
| **Ice Cave** | Slippery tiles, frozen zombies, and a yeti boss. |
| **Wave Beach** | Tide columns, water tiles, and a suction boss. |
| **Dark Ages** | Graves, night planting rules, and a castle boss. |

---

## Controls

| Action | Keys |
| :--- | :--- |
| Fullscreen | `F11` |
| Place / pick up / interact | Mouse |
| Shovel | `S` |
| Plant Food | `F` |
| Pause | `P` / `Space` |
| Game speed | `1` `2` `3` |
| Cancel / leave lawn | `Esc` |

**Couch I, Zombie** (one keyboard, two players)

| Action | Keys |
| :--- | :--- |
| Plants | Mouse |
| Zombie lane | `W` `S` or `↑` `↓` |
| Choose a zombie | `1` `2` `3` `4` `5` |
| Spawn | `Space` / `Enter` |

---

## Multiplayer

Most of the game is local. Only accounts, matchmaking, the live leaderboard, and online I, Zombie go through the server.

Start the authoritative server from `pvz-proj`:

```bash
# Windows
gradlew.bat :core:runServer

# macOS / Linux
./gradlew :core:runServer
```

Default endpoint: `0.0.0.0:54555`. Override with `PVZ_SERVER_BIND` and `PVZ_SERVER_PORT`.

On the login screen, leave **Play offline** unchecked, set **Server host** / **Server port**, then register or log in. Open **Quests → Minigames → I, Zombie** (or the main-menu online banner) to challenge a player or join the random queue.

The server owns the match. Clients send commands; both players render the same snapshot stream.

Local offline accounts are stored on the machine. They are not the same as server accounts.

---

## Build from source

**Requirements**

- JDK 17 or newer on `PATH`
- Git

```bash
git clone https://github.com/Amir-Ehsani/plant-vs-zombie-2.git
cd plant-vs-zombie-2/pvz-proj

# Windows
gradlew.bat lwjgl3:run

# macOS / Linux
./gradlew lwjgl3:run
```

Create the release JAR (includes art, audio, UI, and PAM animations):

```bash
# Windows
gradlew.bat lwjgl3:jar

# macOS / Linux
./gradlew lwjgl3:jar
```

Output:

```
pvz-proj/lwjgl3/build/libs/plants-vs-zombies-failure-1.0.0.jar
```

Useful tasks (run from `pvz-proj`):

| Task | What it does |
| :--- | :--- |
| `lwjgl3:run` | Launch the desktop game |
| `lwjgl3:jar` | Fat JAR with code, natives, and in-game assets |
| `:core:runServer` | Start the multiplayer server |
| `linter` | Run Checkstyle |
| `clean` | Delete build outputs |

---

## Project structure

```
pvz-proj/
├── assets/          Game art, audio, UI, and PAM animations
├── core/            Shared game logic, screens, and the TCP server
├── lwjgl3/          Desktop launcher (LWJGL3) and fat JAR
├── config/          Checkstyle rules
└── README.md
```

The `core` module holds adventure, plants, zombies, bosses, minigames, accounts, and networking.
The `lwjgl3` module starts the window and packages a cross-platform JAR.

---

## Tech stack

- **Java 17** language level
- **[libGDX](https://libgdx.com/) 1.14.2** for rendering, input, and audio
- **LWJGL3 3.4.1** desktop backend
- **Checkstyle** for the course linter
- **Gradle Wrapper** so no global Gradle install is required

---

## Credits

- **Amir Ehsani** — design and programming · [Portfolio](https://amir-ehsani.xyz/)
- **Mohammad Khosravi** — design and programming
- **Mohsen Feyzipoor** — design and programming
- **PopCap Games / Electronic Arts** — Plants vs. Zombies, the original worlds, characters, and audio-visual identity
- **libGDX** and **LWJGL** — the desktop framework this project is built on

This project is a student / fan recreation for learning and demonstration. Please support the original game: [ea.com/games/plantsvszombies](https://www.ea.com/games/plantsvszombies)
