package network.client;

import models.account.User;

/** Authentication result plus the authoritative profile returned by the server. */
public record NetworkAuthResult(boolean successful, String message, User user) {
    public static NetworkAuthResult error(String message) {
        return new NetworkAuthResult(false, message == null ? "authentication failed" : message, null);
    }
}
