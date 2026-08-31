# Phase 3 Networking

This project includes a TCP server/client implementation for Phase 3.

## Run the server

From the project directory:

```bash
./gradlew :core:runServer
```

The default endpoint is `0.0.0.0:54555`. Override it with `PVZ_SERVER_BIND` and
`PVZ_SERVER_PORT` (server) or with the Server Host / Server Port fields on Login (client).
Server account data is persisted in `data/server/accounts.ser` on the server machine.

## Account flow

Registration, login, stay-logged-in sessions, password recovery, profile synchronization,
username/password changes, currencies and progress are sent through the server. A full user
profile is restored after login, so account data is independent from the client machine.

## Network I, Zombie

Open Quests -> Minigames -> I, Zombie. The lobby supports:

- challenge an online username and wait for accept/reject;
- random matchmaking queue;
- local Couch Play bonus.

The server owns the match clock, resources, brains, entities, projectile simulation, cooldowns,
win/loss state and snapshots. The plant client can only place plants; the zombie client can only
spawn zombies. Both clients render the same server snapshot stream.

The online screen includes the three preset text reactions, three emojis and three animated
sticker reactions. Reactions are validated and rate-limited by the server.

## Couch Play

Plants are controlled with the mouse. Zombies use W/S or Up/Down to select a lane, 1-5 to
select a zombie, and Space/Enter to release it.

## Leaderboard and network score

Leaderboard rows are loaded from the server. `My Point` stays unranked (`-`) until a score is
actually submitted by a network-backed run. New scores only replace a previous record when they
are higher. Failed score submissions are kept in a small local pending-score outbox and retried
on a later authenticated connection.
