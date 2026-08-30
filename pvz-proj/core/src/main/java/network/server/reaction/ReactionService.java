package network.server.reaction;

import network.protocol.GameRole;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.ReactionCatalog;
import network.protocol.ReactionCategory;
import network.server.ClientConnection;
import network.server.PvZServer;
import network.server.RequestDispatcher;
import network.server.account.ServerSessionManager;
import network.server.matchmaking.MatchTicket;
import network.server.matchmaking.MatchmakingService;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative relay for the required text/emoji reactions and the
 * animated-sticker bonus. Reactions are preset, authenticated, match-scoped,
 * opponent-only, and rate limited so a modified client cannot spam arbitrary
 * content at another player.
 */
public final class ReactionService {
    private final PvZServer server;
    private final MatchmakingService matchmaking;
    private final ServerSessionManager sessions;
    private final Map<String, Long> nextAllowedAtByParticipant = new ConcurrentHashMap<>();

    public ReactionService(PvZServer server,
                           MatchmakingService matchmaking,
                           ServerSessionManager sessions) {
        this.server = server;
        this.matchmaking = matchmaking;
        this.sessions = sessions;
    }

    public void registerHandlers(RequestDispatcher dispatcher) {
        dispatcher.register(MessageType.REACTION_SEND, this::sendReaction);
    }

    private NetworkMessage sendReaction(ClientConnection client, NetworkMessage request) {
        String username = requireAuthenticated(client, request);
        String matchId = request.getMatchId();
        if (matchId == null || matchId.isBlank()) throw new IllegalArgumentException("match id is required");

        MatchTicket match = matchmaking.getMatch(matchId);
        if (match == null) throw new IllegalArgumentException("match is not active");
        GameRole role = match.roleOf(client);
        if (role == null || match.roleOf(username) != role) {
            throw new IllegalArgumentException("this connection does not own a side in that match");
        }

        ReactionCategory category = parseCategory(request.get("category"));
        String canonicalValue = ReactionCatalog.canonicalValue(category, request.get("value"));
        if (canonicalValue == null) {
            throw new IllegalArgumentException("reaction is not one of the allowed preset "
                    + category.name().toLowerCase(Locale.ROOT) + " reactions");
        }

        long now = System.currentTimeMillis();
        String participantKey = matchId + "|" + username.trim().toLowerCase(Locale.ROOT);
        long nextAllowed = nextAllowedAtByParticipant.getOrDefault(participantKey, 0L);
        if (now < nextAllowed) {
            throw new IllegalArgumentException("reaction cooldown: wait " + (nextAllowed - now) + " ms");
        }
        nextAllowedAtByParticipant.put(participantKey, now + ReactionCatalog.COOLDOWN_MILLIS);

        ClientConnection opponentConnection = match.connectionFor(role.opponent());
        if (opponentConnection == null || !opponentConnection.isOpen()) {
            nextAllowedAtByParticipant.remove(participantKey);
            throw new IllegalArgumentException("opponent is no longer connected");
        }

        String reactionId = UUID.randomUUID().toString();
        NetworkMessage event = NetworkMessage.of(MessageType.REACTION_RECEIVED)
                .matchId(matchId)
                .put("reactionId", reactionId)
                .put("sender", username)
                .put("senderRole", role.name())
                .put("recipient", match.usernameFor(role.opponent()))
                .put("category", category.name())
                .put("value", canonicalValue)
                .put("sentAtEpochMillis", now)
                .put("message", username + " sent a reaction");
        try {
            opponentConnection.send(event);
        } catch (IOException exception) {
            nextAllowedAtByParticipant.remove(participantKey);
            throw new IllegalArgumentException("opponent disconnected before receiving the reaction");
        }

        return RequestDispatcher.success(request, "reaction sent")
                .matchId(matchId)
                .put("reactionId", reactionId)
                .put("recipient", match.usernameFor(role.opponent()))
                .put("category", category.name())
                .put("value", canonicalValue)
                .put("cooldownMillis", ReactionCatalog.COOLDOWN_MILLIS)
                .put("nextAllowedAtEpochMillis", now + ReactionCatalog.COOLDOWN_MILLIS);
    }

    /** Removes small per-match rate-limit entries after a match ends. */
    public void clearMatch(String matchId) {
        if (matchId == null || matchId.isBlank()) return;
        String prefix = matchId + "|";
        nextAllowedAtByParticipant.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public int getTrackedParticipantCount() { return nextAllowedAtByParticipant.size(); }

    private String requireAuthenticated(ClientConnection client, NetworkMessage request) {
        String username = sessions.authenticate(client, request.getSessionToken());
        if (username == null) throw new IllegalArgumentException("authentication required or session expired");
        return username;
    }

    private static ReactionCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("reaction category is required");
        try { return ReactionCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("unknown reaction category"); }
    }
}
