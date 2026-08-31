package network.server.matchmaking;

import network.protocol.GameRole;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.server.ClientConnection;
import network.server.PvZServer;
import network.server.RequestDispatcher;
import network.server.account.AccountRepository;
import network.server.account.ServerSessionManager;
import network.server.game.AuthoritativeGameService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stage-five I, Zombie lobby: direct challenges, accept/reject, random queue,
 * role assignment, and lightweight active-match tickets.
 *
 * The game simulation itself deliberately remains a later stage. This service
 * only decides who plays whom and which side each authenticated client owns.
 */
public final class MatchmakingService {
    public static final long CHALLENGE_TTL_MILLIS = Math.max(500L,
            Long.getLong("pvz.server.challengeTtlMillis", 60_000L));
    private static final long CLEANUP_INTERVAL_MILLIS = Math.max(100L,
            Long.getLong("pvz.server.matchmakingCleanupMillis", 1_000L));
    public static final int MIN_STAGE = 1;
    public static final int MAX_STAGE = 3;

    private final PvZServer server;
    private final AccountRepository accounts;
    private final ServerSessionManager sessions;

    private final Map<String, PendingChallenge> challengesById = new LinkedHashMap<>();
    private final Map<String, String> challengeByUser = new LinkedHashMap<>();
    private final LinkedHashMap<String, QueueEntry> randomQueue = new LinkedHashMap<>();
    private final Map<String, MatchTicket> matchesById = new LinkedHashMap<>();
    private final Map<String, String> matchByUser = new LinkedHashMap<>();
    private final AtomicLong roleFlip = new AtomicLong();
    private final AtomicBoolean janitorStarted = new AtomicBoolean();
    private final ScheduledExecutorService janitor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "pvz-matchmaking-janitor");
        thread.setDaemon(true);
        return thread;
    });
    private volatile AuthoritativeGameService gameService;

    public MatchmakingService(PvZServer server, AccountRepository accounts, ServerSessionManager sessions) {
        this.server = server;
        this.accounts = accounts;
        this.sessions = sessions;
    }

    public void setGameService(AuthoritativeGameService gameService) {
        this.gameService = gameService;
    }

    /** Starts periodic expiry/cleanup so challenges expire even if nobody clicks again. */
    public void start() {
        if (!janitorStarted.compareAndSet(false, true)) return;
        janitor.scheduleAtFixedRate(() -> {
            try { cleanupExpiredChallengesLocked(); }
            catch (Throwable throwable) {
                server.log("matchmaking cleanup failed: " + throwable.getClass().getSimpleName()
                        + ": " + throwable.getMessage());
            }
        }, CLEANUP_INTERVAL_MILLIS, CLEANUP_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void registerHandlers(RequestDispatcher dispatcher) {
        dispatcher.register(MessageType.DIRECT_CHALLENGE, this::directChallenge);
        dispatcher.register(MessageType.CHALLENGE_RESPONSE, this::challengeResponse);
        dispatcher.register(MessageType.RANDOM_QUEUE_JOIN, this::joinRandomQueue);
        dispatcher.register(MessageType.RANDOM_QUEUE_LEAVE, this::leaveRandomQueue);
    }

    private NetworkMessage directChallenge(ClientConnection client, NetworkMessage request) {
        String challenger = requireAuthenticated(client, request);
        String target = required(request, "targetUsername", "opponent username is required").trim();
        int stage = requestedStage(request);
        if (challenger.equalsIgnoreCase(target)) throw new IllegalArgumentException("you cannot challenge yourself");
        if (!accounts.exists(target)) throw new IllegalArgumentException("opponent username does not exist");

        ClientConnection targetConnection = sessions.getConnection(target);
        if (targetConnection == null || !targetConnection.isOpen()) {
            throw new IllegalArgumentException("opponent is offline");
        }

        PendingChallenge challenge;
        synchronized (this) {
            cleanupExpiredChallengesLocked();
            ensureAvailableLocked(challenger, "you are already waiting for or playing a match");
            ensureAvailableLocked(target, "opponent is already waiting for or playing a match");

            String canonicalTarget = sessions.usernameForConnection(targetConnection);
            if (canonicalTarget == null) throw new IllegalArgumentException("opponent is offline");
            challenge = new PendingChallenge(
                    UUID.randomUUID().toString(),
                    challenger,
                    client,
                    canonicalTarget,
                    targetConnection,
                    stage,
                    System.currentTimeMillis() + CHALLENGE_TTL_MILLIS);
            challengesById.put(challenge.id, challenge);
            challengeByUser.put(key(challenger), challenge.id);
            challengeByUser.put(key(canonicalTarget), challenge.id);
        }

        try {
            targetConnection.send(NetworkMessage.of(MessageType.CHALLENGE_INCOMING)
                    .put("challengeId", challenge.id)
                    .put("challenger", challenge.challenger)
                    .put("target", challenge.target)
                    .put("stage", challenge.stage)
                    .put("expiresAtEpochMillis", challenge.expiresAtEpochMillis)
                    .put("message", challenge.challenger + " challenged you to I, Zombie"));
        } catch (IOException exception) {
            synchronized (this) { removeChallengeLocked(challenge); }
            throw new IllegalArgumentException("opponent disconnected before receiving the challenge");
        }

        server.log("challenge " + challenge.id + ": " + challenger + " -> " + challenge.target
                + " (stage " + stage + ")");
        return RequestDispatcher.success(request, "challenge sent to " + challenge.target)
                .put("challengeId", challenge.id)
                .put("targetUsername", challenge.target)
                .put("stage", stage)
                .put("expiresAtEpochMillis", challenge.expiresAtEpochMillis);
    }

    private NetworkMessage challengeResponse(ClientConnection client, NetworkMessage request) {
        String responder = requireAuthenticated(client, request);
        String challengeId = required(request, "challengeId", "challenge id is required");
        boolean accepted = request.getBoolean("accepted", false);

        PendingChallenge challenge;
        synchronized (this) {
            cleanupExpiredChallengesLocked();
            challenge = challengesById.get(challengeId);
            if (challenge == null) throw new IllegalArgumentException("challenge is invalid, expired, or already answered");
            if (!challenge.target.equalsIgnoreCase(responder) || challenge.targetConnection != client) {
                throw new IllegalArgumentException("only the challenged player can answer this challenge");
            }
            removeChallengeLocked(challenge);
        }

        if (!accepted) {
            sendQuietly(challenge.challengerConnection,
                    NetworkMessage.of(MessageType.CHALLENGE_RESPONSE)
                            .put("challengeId", challenge.id)
                            .put("accepted", false)
                            .put("responder", responder)
                            .put("message", responder + " rejected your challenge"));
            return RequestDispatcher.success(request, "challenge rejected")
                    .put("challengeId", challenge.id)
                    .put("accepted", false);
        }

        if (!isCurrentAuthenticatedConnection(challenge.challenger, challenge.challengerConnection)) {
            throw new IllegalArgumentException("challenger is no longer online");
        }
        if (!isCurrentAuthenticatedConnection(challenge.target, challenge.targetConnection)) {
            throw new IllegalArgumentException("your server session changed; reopen matchmaking");
        }

        MatchTicket match = createAndStartMatch(
                challenge.challenger, challenge.challengerConnection,
                challenge.target, challenge.targetConnection,
                challenge.stage, "DIRECT");

        GameRole responderRole = match.roleOf(responder);
        return RequestDispatcher.success(request, "challenge accepted; match starting")
                .matchId(match.getMatchId())
                .put("challengeId", challenge.id)
                .put("accepted", true)
                .put("role", responderRole == null ? "" : responderRole.name())
                .put("opponent", match.opponentOf(responder))
                .put("stage", match.getStage());
    }

    private NetworkMessage joinRandomQueue(ClientConnection client, NetworkMessage request) {
        String username = requireAuthenticated(client, request);
        int stage = requestedStage(request);
        QueueEntry opponent = null;

        synchronized (this) {
            cleanupExpiredChallengesLocked();
            if (matchByUser.containsKey(key(username))) throw new IllegalArgumentException("you are already in a match");
            if (challengeByUser.containsKey(key(username))) throw new IllegalArgumentException("answer or cancel the pending challenge first");

            QueueEntry existing = randomQueue.get(key(username));
            if (existing != null) {
                if (existing.connection == client && existing.stage == stage) {
                    return RequestDispatcher.success(request, "already waiting for a random opponent")
                            .put("queued", true)
                            .put("stage", stage)
                            .put("queuePosition", queuePositionLocked(key(username)));
                }
                randomQueue.remove(key(username));
            }

            List<String> stale = new ArrayList<>();
            for (Map.Entry<String, QueueEntry> item : randomQueue.entrySet()) {
                QueueEntry candidate = item.getValue();
                if (!isCurrentAuthenticatedConnection(candidate.username, candidate.connection)) {
                    stale.add(item.getKey());
                    continue;
                }
                if (candidate.stage == stage && !candidate.username.equalsIgnoreCase(username)) {
                    opponent = candidate;
                    break;
                }
            }
            for (String staleKey : stale) randomQueue.remove(staleKey);

            if (opponent != null) randomQueue.remove(key(opponent.username));
            else {
                randomQueue.put(key(username), new QueueEntry(username, client, stage, System.currentTimeMillis()));
                return RequestDispatcher.success(request, "waiting for a random opponent")
                        .put("queued", true)
                        .put("matched", false)
                        .put("stage", stage)
                        .put("queuePosition", queuePositionLocked(key(username)));
            }
        }

        MatchTicket match;
        try {
            match = createAndStartMatch(
                    opponent.username, opponent.connection,
                    username, client,
                    stage, "RANDOM");
        } catch (IllegalArgumentException exception) {
            // If the queued peer vanished in the tiny race between selection and start,
            // keep the current authenticated player in the queue rather than failing them.
            if (isCurrentAuthenticatedConnection(username, client)) {
                synchronized (this) {
                    if (!matchByUser.containsKey(key(username)) && !challengeByUser.containsKey(key(username))) {
                        randomQueue.put(key(username), new QueueEntry(username, client, stage, System.currentTimeMillis()));
                    }
                }
                return RequestDispatcher.success(request, "opponent disconnected; still waiting in the random queue")
                        .put("queued", true).put("matched", false).put("stage", stage);
            }
            throw exception;
        }

        GameRole role = match.roleOf(username);
        return RequestDispatcher.success(request, "random opponent found; match starting")
                .matchId(match.getMatchId())
                .put("queued", false)
                .put("matched", true)
                .put("role", role == null ? "" : role.name())
                .put("opponent", match.opponentOf(username))
                .put("stage", stage);
    }

    private NetworkMessage leaveRandomQueue(ClientConnection client, NetworkMessage request) {
        String username = requireAuthenticated(client, request);
        boolean removed;
        synchronized (this) {
            QueueEntry entry = randomQueue.get(key(username));
            removed = entry != null && entry.connection == client;
            if (removed) randomQueue.remove(key(username));
        }
        return RequestDispatcher.success(request, removed ? "left the random queue" : "you were not in the random queue")
                .put("queued", false)
                .put("removed", removed);
    }

    /** Called by the account layer after a new authenticated socket becomes authoritative for a username. */
    public void sessionStarted(String username, ClientConnection client) {
        if (username == null || client == null) return;
        cleanupParticipant(username, client, true, "signed in from another client");
    }

    /** Called before an authenticated socket is forgotten by ServerSessionManager. */
    public void sessionEnded(String username, ClientConnection client, String reason) {
        if (username == null || client == null) return;
        cleanupParticipant(username, client, false, reason == null ? "left matchmaking" : reason);
    }

    /** Keeps display names coherent if a user renames while waiting in the lobby. */
    public synchronized void renameUser(String oldUsername, String newUsername, ClientConnection client) {
        String oldKey = key(oldUsername);
        String newKey = key(newUsername);
        if (oldKey.equals(newKey)) return;

        QueueEntry queue = randomQueue.remove(oldKey);
        if (queue != null && queue.connection == client) {
            randomQueue.put(newKey, new QueueEntry(newUsername, client, queue.stage, queue.joinedAtEpochMillis));
        } else if (queue != null) {
            randomQueue.put(oldKey, queue);
        }

        String challengeId = challengeByUser.remove(oldKey);
        if (challengeId != null) {
            PendingChallenge challenge = challengesById.get(challengeId);
            if (challenge != null) {
                if (challenge.challengerConnection == client) challenge.challenger = newUsername;
                if (challenge.targetConnection == client) challenge.target = newUsername;
                challengeByUser.put(newKey, challengeId);
            }
        }

        String matchId = matchByUser.remove(oldKey);
        if (matchId != null) {
            MatchTicket match = matchesById.get(matchId);
            if (match != null) match.renameUsername(oldUsername, newUsername);
            matchByUser.put(newKey, matchId);
        }
    }

    private void cleanupParticipant(String username, ClientConnection client, boolean onlyIfDifferentConnection, String reason) {
        PendingChallenge challengeToCancel = null;
        MatchTicket matchToFinish = null;
        ClientConnection opponentToNotify = null;
        GameRole opponentRole = null;
        String opponentName = null;

        synchronized (this) {
            String userKey = key(username);
            QueueEntry queue = randomQueue.get(userKey);
            if (queue != null && (!onlyIfDifferentConnection || queue.connection != client)) randomQueue.remove(userKey);
            if (queue != null && !onlyIfDifferentConnection && queue.connection == client) randomQueue.remove(userKey);

            String challengeId = challengeByUser.get(userKey);
            if (challengeId != null) {
                PendingChallenge challenge = challengesById.get(challengeId);
                boolean ownsStoredConnection = challenge != null
                        && (challenge.challengerConnection == client || challenge.targetConnection == client);
                boolean staleForNewSession = challenge != null && !ownsStoredConnection;
                if ((!onlyIfDifferentConnection && ownsStoredConnection) || (onlyIfDifferentConnection && staleForNewSession)) {
                    challengeToCancel = challenge;
                    removeChallengeLocked(challenge);
                }
            }

            String matchId = matchByUser.get(userKey);
            if (matchId != null) {
                MatchTicket match = matchesById.get(matchId);
                GameRole role = match == null ? null : match.roleOf(client);
                boolean staleForNewSession = match != null && role == null;
                if ((!onlyIfDifferentConnection && role != null) || (onlyIfDifferentConnection && staleForNewSession)) {
                    matchToFinish = match;
                    if (match != null) {
                        ClientConnection storedUserConnection = match.connectionFor(match.roleOf(username));
                        ClientConnection effectiveUserConnection = storedUserConnection == null ? client : storedUserConnection;
                        opponentToNotify = match.opponentConnection(effectiveUserConnection);
                        GameRole userRole = match.roleOf(username);
                        opponentRole = userRole == null ? null : userRole.opponent();
                        opponentName = userRole == null ? null : match.usernameFor(opponentRole);
                        removeMatchLocked(match);
                    }
                }
            }
        }

        if (challengeToCancel != null) {
            ClientConnection other = challengeToCancel.challenger.equalsIgnoreCase(username)
                    ? challengeToCancel.targetConnection : challengeToCancel.challengerConnection;
            String leavingName = username;
            sendQuietly(other, NetworkMessage.of(MessageType.CHALLENGE_RESPONSE)
                    .put("challengeId", challengeToCancel.id)
                    .put("accepted", false)
                    .put("cancelled", true)
                    .put("message", leavingName + " is no longer available; challenge cancelled"));
        }

        if (matchToFinish != null && opponentToNotify != null && opponentRole != null) {
            sendQuietly(opponentToNotify, NetworkMessage.of(MessageType.MATCH_FINISHED)
                    .matchId(matchToFinish.getMatchId())
                    .put("winner", opponentRole.name())
                    .put("reason", "Opponent " + reason)
                    .put("message", "Match ended because the opponent " + reason)
                    .put("opponent", username)
                    .put("winnerUsername", opponentName));
        }
    }

    private MatchTicket createAndStartMatch(String firstUsername, ClientConnection firstConnection,
                                            String secondUsername, ClientConnection secondConnection,
                                            int stage, String source) {
        if (!isCurrentAuthenticatedConnection(firstUsername, firstConnection)
                || !isCurrentAuthenticatedConnection(secondUsername, secondConnection)) {
            throw new IllegalArgumentException("one of the players is no longer online");
        }

        MatchTicket match;
        synchronized (this) {
            if (matchByUser.containsKey(key(firstUsername)) || matchByUser.containsKey(key(secondUsername))) {
                throw new IllegalArgumentException("one of the players is already in another match");
            }
            if (challengeByUser.containsKey(key(firstUsername)) || challengeByUser.containsKey(key(secondUsername))) {
                throw new IllegalArgumentException("one of the players has another pending challenge");
            }
            randomQueue.remove(key(firstUsername));
            randomQueue.remove(key(secondUsername));

            boolean firstPlants = (roleFlip.getAndIncrement() & 1L) == 0L;
            String matchId = UUID.randomUUID().toString();
            match = firstPlants
                    ? new MatchTicket(matchId, firstUsername, firstConnection, secondUsername, secondConnection, stage, source)
                    : new MatchTicket(matchId, secondUsername, secondConnection, firstUsername, firstConnection, stage, source);
            matchesById.put(matchId, match);
            matchByUser.put(key(match.getPlantsUsername()), matchId);
            matchByUser.put(key(match.getZombiesUsername()), matchId);
        }

        try {
            sendMatchStarted(match, GameRole.PLANTS);
            sendMatchStarted(match, GameRole.ZOMBIES);
            AuthoritativeGameService service = gameService;
            if (service != null) service.startMatch(match);
        } catch (IOException exception) {
            synchronized (this) { removeMatchLocked(match); }
            sendQuietly(match.getPlantsConnection(), NetworkMessage.of(MessageType.MATCH_FINISHED)
                    .matchId(match.getMatchId()).put("reason", "match could not start because a player disconnected"));
            sendQuietly(match.getZombiesConnection(), NetworkMessage.of(MessageType.MATCH_FINISHED)
                    .matchId(match.getMatchId()).put("reason", "match could not start because a player disconnected"));
            throw new IllegalArgumentException("match could not start because a player disconnected");
        }

        server.log("match " + match.getMatchId() + " started: " + match.getPlantsUsername() + " [PLANTS] vs "
                + match.getZombiesUsername() + " [ZOMBIES], source=" + source + ", stage=" + stage);
        return match;
    }

    private void sendMatchStarted(MatchTicket match, GameRole role) throws IOException {
        String username = match.usernameFor(role);
        String opponent = match.usernameFor(role.opponent());
        ClientConnection connection = match.connectionFor(role);
        connection.send(NetworkMessage.of(MessageType.MATCH_STARTED)
                .matchId(match.getMatchId())
                .put("role", role.name())
                .put("username", username)
                .put("opponent", opponent)
                .put("stage", match.getStage())
                .put("source", match.getSource())
                .put("message", "match started against " + opponent));
    }

    private synchronized void cleanupExpiredChallengesLocked() {
        long now = System.currentTimeMillis();
        List<PendingChallenge> expired = new ArrayList<>();
        for (PendingChallenge challenge : challengesById.values()) {
            if (challenge.expiresAtEpochMillis <= now) expired.add(challenge);
        }
        for (PendingChallenge challenge : expired) {
            removeChallengeLocked(challenge);
            sendQuietly(challenge.challengerConnection, NetworkMessage.of(MessageType.CHALLENGE_RESPONSE)
                    .put("challengeId", challenge.id)
                    .put("accepted", false)
                    .put("expired", true)
                    .put("message", "challenge to " + challenge.target + " expired"));
            sendQuietly(challenge.targetConnection, NetworkMessage.of(MessageType.CHALLENGE_RESPONSE)
                    .put("challengeId", challenge.id)
                    .put("accepted", false)
                    .put("expired", true)
                    .put("message", "challenge from " + challenge.challenger + " expired"));
        }
    }

    private void ensureAvailableLocked(String username, String message) {
        String userKey = key(username);
        if (matchByUser.containsKey(userKey) || challengeByUser.containsKey(userKey) || randomQueue.containsKey(userKey)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void removeChallengeLocked(PendingChallenge challenge) {
        if (challenge == null) return;
        challengesById.remove(challenge.id, challenge);
        challengeByUser.remove(key(challenge.challenger), challenge.id);
        challengeByUser.remove(key(challenge.target), challenge.id);
    }

    private void removeMatchLocked(MatchTicket match) {
        if (match == null) return;
        matchesById.remove(match.getMatchId(), match);
        matchByUser.remove(key(match.getPlantsUsername()), match.getMatchId());
        matchByUser.remove(key(match.getZombiesUsername()), match.getMatchId());
        AuthoritativeGameService service = gameService;
        if (service != null) service.abandonMatch(match.getMatchId());
    }

    /** Called by the authoritative game service after a normal timer/brain/forfeit finish. */
    public synchronized void completeMatchFromGame(String matchId) {
        MatchTicket match = matchId == null ? null : matchesById.get(matchId);
        if (match != null) removeMatchLocked(match);
    }

    private int queuePositionLocked(String userKey) {
        int position = 0;
        for (String key : randomQueue.keySet()) {
            position++;
            if (key.equals(userKey)) return position;
        }
        return -1;
    }

    private String requireAuthenticated(ClientConnection client, NetworkMessage request) {
        String username = sessions.authenticate(client, request.getSessionToken());
        if (username == null) throw new IllegalArgumentException("authentication required or session expired");
        return username;
    }

    private boolean isCurrentAuthenticatedConnection(String username, ClientConnection connection) {
        if (username == null || connection == null || !connection.isOpen()) return false;
        return sessions.getConnection(username) == connection;
    }

    private static int requestedStage(NetworkMessage request) {
        int stage = request.getInt("stage", 1);
        if (stage < MIN_STAGE || stage > MAX_STAGE) {
            throw new IllegalArgumentException("I, Zombie stage must be between " + MIN_STAGE + " and " + MAX_STAGE);
        }
        return stage;
    }

    private static String required(NetworkMessage request, String key, String message) {
        String value = request == null ? null : request.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value;
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static void sendQuietly(ClientConnection connection, NetworkMessage message) {
        if (connection == null || !connection.isOpen()) return;
        try { connection.send(message); }
        catch (IOException ignored) { }
    }

    public synchronized MatchTicket getMatch(String matchId) {
        return matchId == null ? null : matchesById.get(matchId);
    }

    public synchronized MatchTicket getMatchForUser(String username) {
        String matchId = matchByUser.get(key(username));
        return matchId == null ? null : matchesById.get(matchId);
    }

    public synchronized boolean isQueued(String username) { return randomQueue.containsKey(key(username)); }
    public synchronized int getQueueSize() { return randomQueue.size(); }
    public synchronized int getPendingChallengeCount() { return challengesById.size(); }
    public synchronized int getActiveMatchCount() { return matchesById.size(); }

    public synchronized void clear() {
        challengesById.clear();
        challengeByUser.clear();
        randomQueue.clear();
        matchesById.clear();
        matchByUser.clear();
    }

    /** Stops periodic cleanup and releases all lobby/matchmaking state. */
    public void close() {
        clear();
        janitor.shutdownNow();
        try { janitor.awaitTermination(1L, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }

    private static final class PendingChallenge {
        final String id;
        String challenger;
        final ClientConnection challengerConnection;
        String target;
        final ClientConnection targetConnection;
        final int stage;
        final long expiresAtEpochMillis;

        PendingChallenge(String id, String challenger, ClientConnection challengerConnection,
                         String target, ClientConnection targetConnection,
                         int stage, long expiresAtEpochMillis) {
            this.id = id;
            this.challenger = challenger;
            this.challengerConnection = challengerConnection;
            this.target = target;
            this.targetConnection = targetConnection;
            this.stage = stage;
            this.expiresAtEpochMillis = expiresAtEpochMillis;
        }
    }

    private record QueueEntry(String username, ClientConnection connection, int stage, long joinedAtEpochMillis) { }
}
