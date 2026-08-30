package network.server.game;

import network.game.ActionResult;
import network.game.AuthoritativeIZombieGame;
import network.protocol.GameRole;
import network.protocol.GameSnapshot;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.server.ClientConnection;
import network.server.PvZServer;
import network.server.RequestDispatcher;
import network.server.account.ServerSessionManager;
import network.server.matchmaking.MatchTicket;
import network.server.matchmaking.MatchmakingService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns every live authoritative I, Zombie simulation on the server.
 * Clients can only submit commands; this service advances time, validates role ownership,
 * broadcasts snapshots, and decides the winner.
 */
public final class AuthoritativeGameService implements AutoCloseable {
    public static final long TICK_MILLIS = 50L;
    public static final long SNAPSHOT_INTERVAL_MILLIS = 100L;

    private final PvZServer server;
    private final MatchmakingService matchmaking;
    private final ServerSessionManager sessions;
    private final Map<String, RunningMatch> runningMatches = new ConcurrentHashMap<>();
    private final AtomicBoolean started = new AtomicBoolean();
    private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "pvz-authoritative-game");
        thread.setDaemon(true);
        return thread;
    });

    public AuthoritativeGameService(PvZServer server,
                                    MatchmakingService matchmaking,
                                    ServerSessionManager sessions) {
        this.server = server;
        this.matchmaking = matchmaking;
        this.sessions = sessions;
    }

    public void registerHandlers(RequestDispatcher dispatcher) {
        dispatcher.register(MessageType.MATCH_ACTION, this::matchAction);
        dispatcher.register(MessageType.MATCH_LEAVE, this::leaveMatch);
    }

    public void start() {
        if (!started.compareAndSet(false, true)) return;
        ticker.scheduleAtFixedRate(this::tickAll, TICK_MILLIS, TICK_MILLIS, TimeUnit.MILLISECONDS);
    }

    /** Called by matchmaking only after both clients have received MATCH_STARTED. */
    public void startMatch(MatchTicket ticket) {
        if (ticket == null) return;
        AuthoritativeIZombieGame game = new AuthoritativeIZombieGame(
                ticket.getPlantsUsername(), ticket.getZombiesUsername(), ticket.getStage());
        RunningMatch running = new RunningMatch(ticket, game);
        RunningMatch previous = runningMatches.putIfAbsent(ticket.getMatchId(), running);
        if (previous != null) return;
        broadcastSnapshot(running, game.snapshot());
        server.log("authoritative game attached to match " + ticket.getMatchId());
    }

    /** Removes simulation state when matchmaking terminates a match because a socket/session vanished. */
    public void abandonMatch(String matchId) {
        if (matchId == null) return;
        runningMatches.remove(matchId);
        server.getReactionService().clearMatch(matchId);
    }

    public AuthoritativeIZombieGame getGame(String matchId) {
        RunningMatch match = matchId == null ? null : runningMatches.get(matchId);
        return match == null ? null : match.game;
    }

    public int getRunningMatchCount() { return runningMatches.size(); }

    private NetworkMessage matchAction(ClientConnection client, NetworkMessage request) {
        String username = requireAuthenticated(client, request);
        RunningMatch running = requireRunningMatch(request.getMatchId());
        MatchTicket ticket = running.ticket;
        GameRole role = ticket.roleOf(client);
        if (role == null || ticket.roleOf(username) != role) {
            throw new IllegalArgumentException("this connection does not own a side in that match");
        }

        String action = required(request, "action", "match action is required").trim().toUpperCase();
        ActionResult result;
        switch (action) {
            case "PLACE_PLANT" -> {
                if (role != GameRole.PLANTS) throw new IllegalArgumentException("only the plant player can place plants");
                result = running.game.placePlant(username,
                        required(request, "type", "plant type is required"),
                        request.getInt("row", Integer.MIN_VALUE),
                        request.getInt("column", Integer.MIN_VALUE));
            }
            case "SPAWN_ZOMBIE" -> {
                if (role != GameRole.ZOMBIES) throw new IllegalArgumentException("only the zombie player can release zombies");
                result = running.game.spawnZombie(username,
                        required(request, "type", "zombie type is required"),
                        request.getInt("row", Integer.MIN_VALUE));
            }
            default -> throw new IllegalArgumentException("unsupported match action: " + action);
        }

        if (!result.wasSuccessful()) throw new IllegalArgumentException(result.getMessage());
        GameSnapshot snapshot = result.getSnapshot() == null ? running.game.snapshot() : result.getSnapshot();
        broadcastSnapshot(running, snapshot);
        running.lastSnapshotBroadcastAt = System.currentTimeMillis();
        return RequestDispatcher.success(request, result.getMessage())
                .matchId(ticket.getMatchId())
                .put("role", role.name())
                .snapshot(snapshot);
    }

    private NetworkMessage leaveMatch(ClientConnection client, NetworkMessage request) {
        String username = requireAuthenticated(client, request);
        RunningMatch running = requireRunningMatch(request.getMatchId());
        GameRole role = running.ticket.roleOf(client);
        if (role == null || running.ticket.roleOf(username) != role) {
            throw new IllegalArgumentException("this connection does not own a side in that match");
        }
        GameRole winner = role.opponent();
        String reason = username + " left the match";
        finishRunningMatch(running, winner, reason);
        return RequestDispatcher.success(request, "left match; opponent wins")
                .matchId(request.getMatchId())
                .put("winner", winner.name());
    }

    private void tickAll() {
        try {
            long now = System.currentTimeMillis();
            for (RunningMatch running : new ArrayList<>(runningMatches.values())) {
                if (runningMatches.get(running.ticket.getMatchId()) != running) continue;
                long previous = running.lastTickAt;
                running.lastTickAt = now;
                long delta = previous <= 0L ? TICK_MILLIS : Math.max(1L, Math.min(250L, now - previous));
                running.game.advance(delta);
                GameSnapshot snapshot = running.game.snapshot();
                if (running.game.isFinished()) {
                    finishRunningMatch(running, snapshot.getWinner(), snapshot.getFinishReason());
                } else if (now - running.lastSnapshotBroadcastAt >= SNAPSHOT_INTERVAL_MILLIS) {
                    broadcastSnapshot(running, snapshot);
                    running.lastSnapshotBroadcastAt = now;
                }
            }
        } catch (Throwable throwable) {
            server.log("authoritative game tick failed: " + throwable.getClass().getSimpleName()
                    + ": " + throwable.getMessage());
        }
    }

    private void finishRunningMatch(RunningMatch running, GameRole winner, String reason) {
        if (running == null || winner == null) return;
        String matchId = running.ticket.getMatchId();
        if (!runningMatches.remove(matchId, running)) return;
        server.getReactionService().clearMatch(matchId);
        running.game.forceFinish(winner, reason);
        GameSnapshot snapshot = running.game.snapshot();
        NetworkMessage plantsEvent = finishedEvent(running.ticket, GameRole.PLANTS, snapshot);
        NetworkMessage zombiesEvent = finishedEvent(running.ticket, GameRole.ZOMBIES, snapshot);
        sendQuietly(running.ticket.getPlantsConnection(), plantsEvent);
        sendQuietly(running.ticket.getZombiesConnection(), zombiesEvent);
        matchmaking.completeMatchFromGame(matchId);
        server.log("match " + matchId + " finished: winner=" + winner + ", reason=" + snapshot.getFinishReason());
    }

    private NetworkMessage finishedEvent(MatchTicket ticket, GameRole receiverRole, GameSnapshot snapshot) {
        GameRole winner = snapshot.getWinner();
        return NetworkMessage.of(MessageType.MATCH_FINISHED)
                .matchId(ticket.getMatchId())
                .put("winner", winner == null ? "" : winner.name())
                .put("winnerUsername", winner == null ? "" : ticket.usernameFor(winner))
                .put("receiverRole", receiverRole.name())
                .put("opponent", ticket.usernameFor(receiverRole.opponent()))
                .put("reason", snapshot.getFinishReason())
                .put("message", snapshot.getFinishReason())
                .snapshot(snapshot);
    }

    private void broadcastSnapshot(RunningMatch running, GameSnapshot snapshot) {
        NetworkMessage plantEvent = NetworkMessage.of(MessageType.MATCH_SNAPSHOT)
                .matchId(running.ticket.getMatchId()).snapshot(snapshot);
        NetworkMessage zombieEvent = NetworkMessage.of(MessageType.MATCH_SNAPSHOT)
                .matchId(running.ticket.getMatchId()).snapshot(snapshot);
        sendQuietly(running.ticket.getPlantsConnection(), plantEvent);
        sendQuietly(running.ticket.getZombiesConnection(), zombieEvent);
    }

    private String requireAuthenticated(ClientConnection client, NetworkMessage request) {
        String username = sessions.authenticate(client, request.getSessionToken());
        if (username == null) throw new IllegalArgumentException("authentication required or session expired");
        return username;
    }

    private RunningMatch requireRunningMatch(String matchId) {
        if (matchId == null || matchId.isBlank()) throw new IllegalArgumentException("match id is required");
        RunningMatch running = runningMatches.get(matchId);
        if (running == null) throw new IllegalArgumentException("match is not active");
        return running;
    }

    private static String required(NetworkMessage request, String key, String message) {
        String value = request == null ? null : request.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value;
    }

    private static void sendQuietly(ClientConnection connection, NetworkMessage message) {
        if (connection == null || !connection.isOpen()) return;
        try { connection.send(message); }
        catch (IOException ignored) { }
    }

    @Override
    public void close() {
        runningMatches.clear();
        ticker.shutdownNow();
        try { ticker.awaitTermination(1, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }

    private static final class RunningMatch {
        final MatchTicket ticket;
        final AuthoritativeIZombieGame game;
        volatile long lastTickAt = System.currentTimeMillis();
        volatile long lastSnapshotBroadcastAt = 0L;

        RunningMatch(MatchTicket ticket, AuthoritativeIZombieGame game) {
            this.ticket = ticket;
            this.game = game;
        }
    }
}
