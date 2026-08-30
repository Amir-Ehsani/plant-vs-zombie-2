package network.client;

import models.account.Quest;
import models.account.User;
import network.protocol.LeaderboardRow;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.ReactionCatalog;
import network.protocol.ReactionCategory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Application-facing phase-three client. It owns one persistent TCP connection,
 * restores revocable sessions, synchronizes complete profiles, and routes
 * matchmaking/game events into queues consumed by the LibGDX screens.
 */
public final class NetworkManager implements AutoCloseable {
    private static final long REQUEST_TIMEOUT_MILLIS = 5_000L;

    private final PvZNetworkClient client = new PvZNetworkClient();
    private final NetworkProfileCodec profileCodec = new NetworkProfileCodec();
    private final ExecutorService io = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "pvz-network-io");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentLinkedQueue<NetworkMessage> challengeEvents = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<NetworkMessage> matchStartedEvents = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<NetworkMessage> matchEvents = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<NetworkMessage> systemEvents = new ConcurrentLinkedQueue<>();

    private volatile NetworkConfig config = NetworkConfig.fromEnvironment();
    private volatile String lastError = "";
    private volatile NetworkMatchContext activeMatch;
    private volatile String authenticatedUsername;

    public NetworkConfig getConfig() { return config; }
    public boolean isConnected() { return client.isConnected(); }
    public boolean isAuthenticated() { return client.isAuthenticated(); }
    public String getLastError() { return lastError; }
    public String getDisconnectReason() { return client.getDisconnectReason(); }
    public String getAuthenticatedUsername() { return authenticatedUsername; }
    public NetworkMatchContext getActiveMatch() { return activeMatch; }

    public synchronized void configure(String host, int port) {
        NetworkConfig updated = new NetworkConfig(host, port, 2_000);
        if (!updated.endpoint().equals(config.endpoint())) {
            client.close();
            authenticatedUsername = null;
            activeMatch = null;
        }
        config = updated;
    }

    public NetworkOperationResult status() {
        NetworkResponse response = call(NetworkMessage.of(MessageType.PING), false);
        if (!response.wasSuccessful()) {
            return NetworkOperationResult.error(response.getMessage());
        }
        return NetworkOperationResult.ok(response.getMessage() + " at " + config.endpoint());
    }

    public CompletableFuture<NetworkOperationResult> statusAsync() {
        return CompletableFuture.supplyAsync(this::status, io);
    }

    public NetworkAuthResult register(User user, String securityQuestion, String rawSecurityAnswer) {
        if (user == null) return NetworkAuthResult.error("registration profile is missing");
        try {
            NetworkMessage request = profileMessage(MessageType.REGISTER, user)
                    .put("passwordHash", user.getPasswordHash())
                    .put("securityQuestion", securityQuestion)
                    .put("securityAnswerHash", ClientCrypto.normalizedAnswerHash(rawSecurityAnswer));
            NetworkResponse response = call(request, false);
            return new NetworkAuthResult(response.wasSuccessful(), response.getMessage(), null);
        } catch (IOException exception) {
            return NetworkAuthResult.error(exception.getMessage());
        }
    }

    public NetworkAuthResult login(String username, String rawPassword, boolean stayLoggedIn) {
        NetworkResponse response = call(NetworkMessage.of(MessageType.LOGIN)
                .put("username", username)
                .put("passwordHash", ClientCrypto.sha256(rawPassword))
                .put("stayLoggedIn", stayLoggedIn), false);
        if (!response.wasSuccessful()) return NetworkAuthResult.error(response.getMessage());
        NetworkAuthResult result = acceptAuthenticatedProfile(response, stayLoggedIn
                ? response.get("persistentToken") : null);
        if (result.successful()) flushPendingScore(result.user());
        return result;
    }

    public NetworkAuthResult resumeSavedSession() {
        NetworkSessionStore.SavedSession saved = NetworkSessionStore.load();
        if (saved == null) return NetworkAuthResult.error("no saved server session");
        if (!ensureConnected()) return NetworkAuthResult.error(lastError);
        try {
            NetworkResponse response = NetworkResponse.from(client.request(
                    NetworkMessage.of(MessageType.RESUME_SESSION)
                            .put("username", saved.username)
                            .put("persistentToken", saved.persistentToken),
                    REQUEST_TIMEOUT_MILLIS));
            if (!response.wasSuccessful()) {
                NetworkSessionStore.clear();
                return NetworkAuthResult.error(response.getMessage());
            }
            NetworkAuthResult result = acceptAuthenticatedProfile(response, saved.persistentToken);
            if (result.successful()) flushPendingScore(result.user());
            return result;
        } catch (Exception exception) {
            lastError = "session restore failed: " + readable(exception);
            return NetworkAuthResult.error(lastError);
        }
    }

    private NetworkAuthResult acceptAuthenticatedProfile(NetworkResponse response, String persistentToken) {
        try {
            String token = response.get("sessionToken");
            if (token == null || token.isBlank()) return NetworkAuthResult.error("server omitted the session token");
            User user = profileCodec.decode(response.getResponse().getBinaryPayload());
            client.setSessionToken(token);
            authenticatedUsername = user.getUsername();
            if (persistentToken != null && !persistentToken.isBlank()) {
                NetworkSessionStore.save(user.getUsername(), persistentToken);
            } else {
                NetworkSessionStore.clear();
            }
            lastError = "";
            return new NetworkAuthResult(true, response.getMessage(), user);
        } catch (IOException exception) {
            client.setSessionToken(null);
            authenticatedUsername = null;
            return NetworkAuthResult.error("could not restore server profile: " + exception.getMessage());
        }
    }

    public NetworkOperationResult logout() {
        NetworkResponse response = client.isAuthenticated()
                ? call(NetworkMessage.of(MessageType.LOGOUT), true)
                : NetworkResponse.from(NetworkMessage.of(MessageType.SUCCESS).put("message", "already offline"));
        client.setSessionToken(null);
        authenticatedUsername = null;
        activeMatch = null;
        NetworkSessionStore.clear();
        return toResult(response);
    }

    public void clearLocalAuthentication() {
        client.setSessionToken(null);
        authenticatedUsername = null;
        activeMatch = null;
        NetworkSessionStore.clear();
    }

    public NetworkOperationResult synchronize(User user) {
        if (user == null) return NetworkOperationResult.error("profile is missing");
        if (!client.isAuthenticated()) return NetworkOperationResult.error("server session is not active");
        try {
            return toResult(call(profileMessage(MessageType.ACCOUNT_SYNC, user), true));
        } catch (IOException exception) {
            return NetworkOperationResult.error(exception.getMessage());
        }
    }

    public CompletableFuture<NetworkOperationResult> synchronizeAsync(User user) {
        if (user == null || !client.isAuthenticated()) {
            return CompletableFuture.completedFuture(NetworkOperationResult.error("server session is not active"));
        }
        return CompletableFuture.supplyAsync(() -> synchronize(user), io);
    }

    public NetworkOperationResult rename(User renamedUser, String newUsername) {
        if (renamedUser == null) return NetworkOperationResult.error("profile is missing");
        try {
            NetworkResponse response = call(profileMessage(MessageType.ACCOUNT_RENAME, renamedUser)
                    .put("newUsername", newUsername), true);
            if (response.wasSuccessful()) {
                authenticatedUsername = newUsername;
                NetworkSessionStore.SavedSession saved = NetworkSessionStore.load();
                if (saved != null) NetworkSessionStore.save(newUsername, saved.persistentToken);
            }
            return toResult(response);
        } catch (IOException exception) {
            return NetworkOperationResult.error(exception.getMessage());
        }
    }

    public NetworkOperationResult changePassword(User updatedUser, String oldPassword, String newPassword) {
        if (updatedUser == null) return NetworkOperationResult.error("profile is missing");
        try {
            NetworkResponse response = call(profileMessage(MessageType.CHANGE_PASSWORD, updatedUser)
                    .put("oldPasswordHash", ClientCrypto.sha256(oldPassword))
                    .put("newPasswordHash", ClientCrypto.sha256(newPassword)), true);
            if (response.wasSuccessful()) NetworkSessionStore.clear();
            return toResult(response);
        } catch (IOException exception) {
            return NetworkOperationResult.error(exception.getMessage());
        }
    }

    public NetworkResponse beginPasswordReset(String username, String email) {
        return call(NetworkMessage.of(MessageType.PASSWORD_RESET_LOOKUP)
                .put("username", username).put("email", email), false);
    }

    public NetworkResponse verifyPasswordReset(String resetId, String answer) {
        return call(NetworkMessage.of(MessageType.PASSWORD_RESET_VERIFY)
                .put("resetId", resetId)
                .put("securityAnswerHash", ClientCrypto.normalizedAnswerHash(answer)), false);
    }

    public NetworkOperationResult completePasswordReset(String resetId, String newPassword) {
        return toResult(call(NetworkMessage.of(MessageType.PASSWORD_RESET_COMMIT)
                .put("resetId", resetId)
                .put("newPasswordHash", ClientCrypto.sha256(newPassword)), false));
    }

    public List<LeaderboardRow> leaderboardRows(String column, boolean ascending) {
        NetworkResponse response = call(NetworkMessage.of(MessageType.LEADERBOARD_REQUEST)
                .put("column", column == null ? "score" : column)
                .put("ascending", ascending), true);
        return response.wasSuccessful() && response.getResponse() != null
                ? response.getResponse().getLeaderboardRows()
                : Collections.emptyList();
    }

    public CompletableFuture<List<LeaderboardRow>> leaderboardRowsAsync(String column, boolean ascending) {
        return CompletableFuture.supplyAsync(() -> leaderboardRows(column, ascending), io);
    }

    public NetworkOperationResult submitScoredGame(User user, int score) {
        int safeScore = Math.max(0, score);
        String username = user == null ? authenticatedUsername : user.getUsername();
        NetworkResponse response = call(NetworkMessage.of(MessageType.SCORE_SUBMIT).put("score", safeScore), true);
        if (response.wasSuccessful()) {
            if (username != null) NetworkPendingScoreStore.clear(username);
        } else if (username != null) {
            NetworkPendingScoreStore.saveMax(username, safeScore);
        }
        return toResult(response);
    }

    public void submitScoredGameAsync(User user, int score) {
        if (score <= 0) return;
        io.execute(() -> submitScoredGame(user, score));
    }

    private void flushPendingScore(User user) {
        if (user == null || user.getUsername() == null) return;
        int pending = NetworkPendingScoreStore.load(user.getUsername());
        if (pending >= 0) submitScoredGameAsync(user, pending);
    }

    public NetworkOperationResult challenge(String targetUsername, int stage) {
        return toResult(call(NetworkMessage.of(MessageType.DIRECT_CHALLENGE)
                .put("targetUsername", targetUsername)
                .put("stage", clampStage(stage)), true));
    }

    public CompletableFuture<NetworkOperationResult> challengeAsync(String targetUsername, int stage) {
        return CompletableFuture.supplyAsync(() -> challenge(targetUsername, stage), io);
    }

    public NetworkOperationResult respondToChallenge(String challengeId, boolean accepted) {
        return toResult(call(NetworkMessage.of(MessageType.CHALLENGE_RESPONSE)
                .put("challengeId", challengeId)
                .put("accepted", accepted), true));
    }

    public CompletableFuture<NetworkOperationResult> respondToChallengeAsync(String challengeId, boolean accepted) {
        return CompletableFuture.supplyAsync(() -> respondToChallenge(challengeId, accepted), io);
    }

    public NetworkOperationResult joinRandomQueue(int stage) {
        return toResult(call(NetworkMessage.of(MessageType.RANDOM_QUEUE_JOIN)
                .put("stage", clampStage(stage)), true));
    }

    public CompletableFuture<NetworkOperationResult> joinRandomQueueAsync(int stage) {
        return CompletableFuture.supplyAsync(() -> joinRandomQueue(stage), io);
    }

    public NetworkOperationResult leaveRandomQueue() {
        return toResult(call(NetworkMessage.of(MessageType.RANDOM_QUEUE_LEAVE), true));
    }

    public CompletableFuture<NetworkOperationResult> leaveRandomQueueAsync() {
        return CompletableFuture.supplyAsync(this::leaveRandomQueue, io);
    }

    public NetworkOperationResult placePlant(String matchId, String type, int row, int column) {
        return toResult(call(NetworkMessage.of(MessageType.MATCH_ACTION).matchId(matchId)
                .put("action", "PLACE_PLANT").put("type", type)
                .put("row", row).put("column", column), true));
    }

    public CompletableFuture<NetworkOperationResult> placePlantAsync(String matchId, String type, int row, int column) {
        return CompletableFuture.supplyAsync(() -> placePlant(matchId, type, row, column), io);
    }

    public NetworkOperationResult spawnZombie(String matchId, String type, int row) {
        return toResult(call(NetworkMessage.of(MessageType.MATCH_ACTION).matchId(matchId)
                .put("action", "SPAWN_ZOMBIE").put("type", type).put("row", row), true));
    }

    public CompletableFuture<NetworkOperationResult> spawnZombieAsync(String matchId, String type, int row) {
        return CompletableFuture.supplyAsync(() -> spawnZombie(matchId, type, row), io);
    }

    public NetworkOperationResult sendReaction(String matchId, ReactionCategory category, String value) {
        if (matchId == null || matchId.isBlank()) return NetworkOperationResult.error("match id is required");
        String canonical = ReactionCatalog.canonicalValue(category, value);
        if (canonical == null) return NetworkOperationResult.error("reaction is not one of the allowed presets");
        return toResult(call(NetworkMessage.of(MessageType.REACTION_SEND).matchId(matchId)
                .put("category", category.name()).put("value", canonical), true));
    }

    public CompletableFuture<NetworkOperationResult> sendReactionAsync(String matchId, ReactionCategory category, String value) {
        return CompletableFuture.supplyAsync(() -> sendReaction(matchId, category, value), io);
    }

    public NetworkOperationResult leaveMatch(String matchId) {
        return toResult(call(NetworkMessage.of(MessageType.MATCH_LEAVE).matchId(matchId), true));
    }

    public CompletableFuture<NetworkOperationResult> leaveMatchAsync(String matchId) {
        return CompletableFuture.supplyAsync(() -> leaveMatch(matchId), io);
    }

    /** Drains transport events into type-specific queues. Call once per render frame. */
    public void pumpEvents() {
        NetworkMessage event;
        while ((event = client.pollEvent()) != null) {
            switch (event.getType()) {
                case CHALLENGE_INCOMING -> challengeEvents.add(event);
                case MATCH_STARTED -> {
                    NetworkMatchContext context = NetworkMatchContext.from(event);
                    if (context != null) activeMatch = context;
                    matchStartedEvents.add(event);
                }
                case MATCH_SNAPSHOT, MATCH_FINISHED, REACTION_RECEIVED -> matchEvents.add(event);
                case SESSION_REPLACED -> {
                    clearLocalAuthentication();
                    systemEvents.add(event);
                }
                default -> systemEvents.add(event);
            }
        }
    }

    public NetworkMessage pollChallengeEvent() { return challengeEvents.poll(); }
    public NetworkMessage pollMatchStartedEvent() { return matchStartedEvents.poll(); }
    public NetworkMessage pollMatchEvent() { return matchEvents.poll(); }
    public NetworkMessage pollSystemEvent() { return systemEvents.poll(); }

    public void clearMatchEvents() {
        matchEvents.clear();
    }

    public void clearActiveMatch() {
        activeMatch = null;
        matchEvents.clear();
    }

    private NetworkMessage profileMessage(MessageType type, User user) throws IOException {
        return NetworkMessage.of(type)
                .put("username", user.getUsername())
                .put("nickname", user.getNickname())
                .put("email", user.getEmail())
                .put("coins", user.getCoins())
                .put("gems", user.getGems())
                .put("totalGamesPlayed", user.getGamesPlayed())
                .put("lastChapter", safe(user.getPersistentCurrentChapterName()))
                .put("lastLevel", user.getPersistentCurrentChapterLevel())
                .put("miniGamesWon", user.getCompletedMiniGameStageCount())
                .put("dailyQuestsDone", completedQuestCount(user, true))
                .put("totalQuestsDone", completedQuestCount(user, false) + completedQuestCount(user, true))
                .binaryPayload(profileCodec.encode(user));
    }

    private int completedQuestCount(User user, boolean daily) {
        if (user == null || user.getQuests() == null) return 0;
        int count = 0;
        for (Quest quest : user.getQuests()) {
            if (quest == null || !quest.isCompleted()) continue;
            String type = safe(quest.getType()).toLowerCase(Locale.ROOT);
            boolean dailyQuest = type.contains("daily") || type.contains("challenge");
            if (dailyQuest == daily) count++;
        }
        return count;
    }

    private synchronized boolean ensureConnected() {
        if (client.isConnected()) return true;
        try {
            client.connect(config);
            lastError = "";
            return true;
        } catch (IOException exception) {
            lastError = "cannot connect to " + config.endpoint()
                    + "; start the phase-three server first (" + readable(exception) + ")";
            return false;
        }
    }

    private NetworkResponse call(NetworkMessage request, boolean authenticated) {
        if (!ensureConnected()) return NetworkResponse.failure(lastError);
        if (authenticated && !client.isAuthenticated()) {
            NetworkAuthResult restored = resumeSavedSession();
            if (!restored.successful()) {
                return NetworkResponse.failure("login with the server before using this feature ("
                        + restored.message() + ")");
            }
        }
        try {
            NetworkResponse response = NetworkResponse.from(client.request(request, REQUEST_TIMEOUT_MILLIS));
            lastError = response.wasSuccessful() ? "" : response.getMessage();
            return response;
        } catch (Exception exception) {
            lastError = "network request failed: " + readable(exception);
            return NetworkResponse.failure(lastError);
        }
    }

    private static NetworkOperationResult toResult(NetworkResponse response) {
        if (response == null) return NetworkOperationResult.error("server did not return a response");
        return new NetworkOperationResult(response.wasSuccessful(), response.getMessage());
    }

    private static int clampStage(int stage) { return Math.max(1, Math.min(3, stage)); }
    private static String safe(String value) { return value == null ? "" : value; }

    private static String readable(Throwable throwable) {
        if (throwable == null) return "unknown error";
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    @Override
    public void close() {
        io.shutdown();
        try {
            if (!io.awaitTermination(2, TimeUnit.SECONDS)) io.shutdownNow();
        } catch (InterruptedException interrupted) {
            io.shutdownNow();
            Thread.currentThread().interrupt();
        }
        client.close();
    }
}
