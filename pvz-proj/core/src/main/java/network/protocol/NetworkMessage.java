package network.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Single envelope used for requests, correlated responses, and unsolicited events.
 *
 * Request: requestId is generated automatically and replyTo is null.
 * Response: replyTo points at the originating requestId.
 * Event: replyTo is null and the message is routed through NetworkEventRouter.
 */
public final class NetworkMessage implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    public static final int PROTOCOL_VERSION = 1;

    private final int protocolVersion;
    private final MessageType type;
    private final String requestId;
    private String replyTo;
    private String sessionToken;
    private String matchId;
    private final Map<String, String> data;
    private byte[] binaryPayload;
    private GameSnapshot snapshot;
    private List<LeaderboardRow> leaderboardRows;
    private final long createdAtEpochMillis;

    private NetworkMessage(MessageType type) {
        this.protocolVersion = PROTOCOL_VERSION;
        this.type = Objects.requireNonNull(type, "message type");
        this.requestId = UUID.randomUUID().toString();
        this.data = new LinkedHashMap<>();
        this.leaderboardRows = new ArrayList<>();
        this.createdAtEpochMillis = System.currentTimeMillis();
    }

    public static NetworkMessage of(MessageType type) {
        return new NetworkMessage(type);
    }

    /** Convenience factory for server replies. */
    public static NetworkMessage reply(MessageType type, NetworkMessage request) {
        NetworkMessage response = of(type);
        if (request != null) {
            response.replyTo(request.getRequestId());
            response.matchId(request.getMatchId());
        }
        return response;
    }

    public NetworkMessage put(String key, Object value) {
        if (key == null || key.isBlank()) return this;
        if (value == null) data.remove(key);
        else data.put(key, String.valueOf(value));
        return this;
    }

    public String get(String key) {
        return key == null ? null : data.get(key);
    }

    public String getOrDefault(String key, String fallback) {
        String value = get(key);
        return value == null ? fallback : value;
    }

    public int getInt(String key, int fallback) {
        try { return Integer.parseInt(get(key)); }
        catch (RuntimeException ignored) { return fallback; }
    }

    public long getLong(String key, long fallback) {
        try { return Long.parseLong(get(key)); }
        catch (RuntimeException ignored) { return fallback; }
    }

    public boolean getBoolean(String key, boolean fallback) {
        String value = get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public NetworkMessage replyTo(String requestId) {
        this.replyTo = blankToNull(requestId);
        return this;
    }

    public NetworkMessage sessionToken(String sessionToken) {
        this.sessionToken = blankToNull(sessionToken);
        return this;
    }

    public NetworkMessage matchId(String matchId) {
        this.matchId = blankToNull(matchId);
        return this;
    }

    public NetworkMessage binaryPayload(byte[] binaryPayload) {
        this.binaryPayload = binaryPayload == null ? null : binaryPayload.clone();
        return this;
    }

    public NetworkMessage snapshot(GameSnapshot snapshot) {
        this.snapshot = snapshot;
        return this;
    }

    public NetworkMessage leaderboardRows(List<LeaderboardRow> rows) {
        this.leaderboardRows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        return this;
    }

    public int getProtocolVersion() { return protocolVersion; }
    public MessageType getType() { return type; }
    public String getRequestId() { return requestId; }
    public String getReplyTo() { return replyTo; }
    public String getSessionToken() { return sessionToken; }
    public String getMatchId() { return matchId; }
    public Map<String, String> getData() { return Collections.unmodifiableMap(data); }
    public byte[] getBinaryPayload() { return binaryPayload == null ? null : binaryPayload.clone(); }
    public GameSnapshot getSnapshot() { return snapshot; }
    public List<LeaderboardRow> getLeaderboardRows() { return Collections.unmodifiableList(leaderboardRows); }
    public long getCreatedAtEpochMillis() { return createdAtEpochMillis; }

    public boolean isReply() { return replyTo != null; }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public String toString() {
        return "NetworkMessage{" +
                "v=" + protocolVersion +
                ", type=" + type +
                ", requestId='" + requestId + '\'' +
                ", replyTo='" + replyTo + '\'' +
                ", matchId='" + matchId + '\'' +
                ", data=" + data +
                '}';
    }
}
