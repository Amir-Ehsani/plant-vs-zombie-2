package network.client;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;

public final class NetworkResponse {
    private final boolean successful;
    private final String message;
    private final NetworkMessage response;

    private NetworkResponse(boolean successful, String message, NetworkMessage response) {
        this.successful = successful;
        this.message = message;
        this.response = response;
    }

    public static NetworkResponse from(NetworkMessage response) {
        boolean success = response != null && response.getType() != MessageType.ERROR;
        String message = response == null ? "server did not return a response"
                : response.getOrDefault("message", success ? "operation completed" : "server rejected the request");
        return new NetworkResponse(success, message, response);
    }

    public static NetworkResponse failure(String message) {
        return new NetworkResponse(false, message, null);
    }

    public boolean wasSuccessful() { return successful; }
    public String getMessage() { return message; }
    public NetworkMessage getResponse() { return response; }
    public String get(String key) { return response == null ? null : response.get(key); }
}
