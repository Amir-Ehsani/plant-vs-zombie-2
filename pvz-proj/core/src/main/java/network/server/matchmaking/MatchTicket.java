package network.server.matchmaking;

import network.protocol.GameRole;
import network.server.ClientConnection;

import java.util.Objects;

/**
 * Lightweight authoritative ownership record created by matchmaking.
 * Stage six attaches an AuthoritativeIZombieGame to this ticket without changing
 * the lobby protocol or role assignment established here.
 */
public final class MatchTicket {
    private final String matchId;
    private String plantsUsername;
    private final ClientConnection plantsConnection;
    private String zombiesUsername;
    private final ClientConnection zombiesConnection;
    private final int stage;
    private final String source;
    private final long createdAtEpochMillis;

    MatchTicket(String matchId,
                String plantsUsername,
                ClientConnection plantsConnection,
                String zombiesUsername,
                ClientConnection zombiesConnection,
                int stage,
                String source) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.plantsUsername = Objects.requireNonNull(plantsUsername, "plantsUsername");
        this.plantsConnection = Objects.requireNonNull(plantsConnection, "plantsConnection");
        this.zombiesUsername = Objects.requireNonNull(zombiesUsername, "zombiesUsername");
        this.zombiesConnection = Objects.requireNonNull(zombiesConnection, "zombiesConnection");
        this.stage = stage;
        this.source = source == null ? "UNKNOWN" : source;
        this.createdAtEpochMillis = System.currentTimeMillis();
    }

    public String getMatchId() { return matchId; }
    public String getPlantsUsername() { return plantsUsername; }
    public ClientConnection getPlantsConnection() { return plantsConnection; }
    public String getZombiesUsername() { return zombiesUsername; }
    public ClientConnection getZombiesConnection() { return zombiesConnection; }
    public int getStage() { return stage; }
    public String getSource() { return source; }
    public long getCreatedAtEpochMillis() { return createdAtEpochMillis; }

    public GameRole roleOf(String username) {
        if (username == null) {
            return null;
        }
        if (plantsUsername.equalsIgnoreCase(username)) {
            return GameRole.PLANTS;
        }
        if (zombiesUsername.equalsIgnoreCase(username)) {
            return GameRole.ZOMBIES;
        }
        return null;
    }

    public GameRole roleOf(ClientConnection connection) {
        if (connection == plantsConnection) {
            return GameRole.PLANTS;
        }
        if (connection == zombiesConnection) {
            return GameRole.ZOMBIES;
        }
        return null;
    }

    public String opponentOf(String username) {
        GameRole role = roleOf(username);
        if (role == GameRole.PLANTS) {
            return zombiesUsername;
        }
        if (role == GameRole.ZOMBIES) {
            return plantsUsername;
        }
        return null;
    }

    public ClientConnection opponentConnection(ClientConnection connection) {
        if (connection == plantsConnection) {
            return zombiesConnection;
        }
        if (connection == zombiesConnection) {
            return plantsConnection;
        }
        return null;
    }

    public String usernameFor(GameRole role) {
        return role == GameRole.PLANTS ? plantsUsername : zombiesUsername;
    }

    public ClientConnection connectionFor(GameRole role) {
        return role == GameRole.PLANTS ? plantsConnection : zombiesConnection;
    }

    void renameUsername(String oldUsername, String newUsername) {
        if (oldUsername == null || newUsername == null) {
            return;
        }
        if (plantsUsername.equalsIgnoreCase(oldUsername)) {
            plantsUsername = newUsername;
        }
        if (zombiesUsername.equalsIgnoreCase(oldUsername)) {
            zombiesUsername = newUsername;
        }
    }
}
