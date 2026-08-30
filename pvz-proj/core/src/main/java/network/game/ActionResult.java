package network.game;

import network.protocol.GameSnapshot;

/** Result of one authoritative I, Zombie command. */
public final class ActionResult {
    private final boolean successful;
    private final String message;
    private final GameSnapshot snapshot;

    private ActionResult(boolean successful, String message, GameSnapshot snapshot) {
        this.successful = successful;
        this.message = message == null ? "" : message;
        this.snapshot = snapshot;
    }

    public static ActionResult success(String message, GameSnapshot snapshot) {
        return new ActionResult(true, message, snapshot);
    }

    public static ActionResult failure(String message, GameSnapshot snapshot) {
        return new ActionResult(false, message, snapshot);
    }

    public boolean wasSuccessful() { return successful; }
    public boolean isSuccessful() { return successful; }
    public String getMessage() { return message; }
    public GameSnapshot getSnapshot() { return snapshot; }
}
