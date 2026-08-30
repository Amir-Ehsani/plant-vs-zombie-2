package network.client;

import network.protocol.GameRole;
import network.protocol.NetworkMessage;

/** Immutable description of the local side of one live network I, Zombie match. */
public record NetworkMatchContext(String matchId, String opponent, GameRole role, int stage, String source) {
    public static NetworkMatchContext from(NetworkMessage message) {
        if (message == null || message.getMatchId() == null || message.getMatchId().isBlank()) {
            return null;
        }
        GameRole role;
        try {
            role = GameRole.valueOf(message.getOrDefault("role", "PLANTS"));
        } catch (IllegalArgumentException ignored) {
            role = GameRole.PLANTS;
        }
        return new NetworkMatchContext(
                message.getMatchId(),
                message.getOrDefault("opponent", "opponent"),
                role,
                Math.max(1, Math.min(3, message.getInt("stage", 1))),
                message.getOrDefault("source", "NETWORK")
        );
    }
}
