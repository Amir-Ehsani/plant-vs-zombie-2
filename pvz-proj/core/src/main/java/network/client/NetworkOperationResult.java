package network.client;

/** Small immutable result used by graphical network operations. */
public record NetworkOperationResult(boolean successful, String message) {
    public static NetworkOperationResult ok(String message) {
        return new NetworkOperationResult(true, message == null ? "operation completed" : message);
    }

    public static NetworkOperationResult error(String message) {
        return new NetworkOperationResult(false, message == null ? "network operation failed" : message);
    }
}
