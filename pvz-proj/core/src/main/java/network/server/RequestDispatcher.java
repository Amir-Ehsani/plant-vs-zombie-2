package network.server;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Routes phase-three wire requests to focused server services. PING also acts as
 * a lightweight health/diagnostic endpoint for the graphical network lobby.
 */
public final class RequestDispatcher {
    private final Map<MessageType, RequestHandler> handlers = new EnumMap<>(MessageType.class);
    private final PvZServer server;

    RequestDispatcher(PvZServer server) {
        this.server = Objects.requireNonNull(server, "server");
        register(MessageType.PING, this::handlePing);
    }

    public synchronized void register(MessageType type, RequestHandler handler) {
        if (type == null || handler == null) {
            throw new IllegalArgumentException("type and handler are required");
        }
        handlers.put(type, handler);
    }

    public synchronized void unregister(MessageType type) {
        if (type != null && type != MessageType.PING) {
            handlers.remove(type);
        }
    }

    public NetworkMessage dispatch(ClientConnection client, NetworkMessage request) {
        if (request == null) {
            return error(null, "empty request");
        }
        if (request.getProtocolVersion() != NetworkMessage.PROTOCOL_VERSION) {
            return error(request, "protocol version mismatch: server=" + NetworkMessage.PROTOCOL_VERSION
                    + ", client=" + request.getProtocolVersion());
        }
        if (request.getReplyTo() != null) {
            return error(request, "clients must send requests/events, not reply messages");
        }

        RequestHandler handler;
        synchronized (this) {
            handler = handlers.get(request.getType());
        }
        if (handler == null) {
            return error(request, "message type is not implemented as a client request: " + request.getType());
        }

        try {
            NetworkMessage response = handler.handle(client, request);
            if (response == null) {
                return success(request, "operation completed");
            }
            if (response.getReplyTo() == null) {
                response.replyTo(request.getRequestId());
            }
            if (response.getMatchId() == null && request.getMatchId() != null) {
                response.matchId(request.getMatchId());
            }
            return response;
        } catch (IllegalArgumentException exception) {
            return error(request, exception.getMessage() == null ? "invalid request" : exception.getMessage());
        } catch (Exception exception) {
            server.log("request " + request.getType() + " failed for " + client.getRemoteAddress()
                    + ": " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return error(request, "server could not process the request");
        }
    }

    private NetworkMessage handlePing(ClientConnection client, NetworkMessage request) {
        return success(request, "pong")
                .put("protocolVersion", NetworkMessage.PROTOCOL_VERSION)
                .put("phase3Ready", true)
                .put("serverTimeEpochMillis", System.currentTimeMillis())
                .put("serverUptimeMillis", server.getUptimeMillis())
                .put("connectedClients", server.getConnectedClientCount())
                .put("onlineUsers", server.getSessionManager().onlineCount())
                .put("registeredAccounts", server.getAccountRepository().count())
                .put("randomQueueSize", server.getMatchmakingService().getQueueSize())
                .put("pendingChallenges", server.getMatchmakingService().getPendingChallengeCount())
                .put("activeMatches", server.getMatchmakingService().getActiveMatchCount())
                .put("authoritativeGames", server.getAuthoritativeGameService().getRunningMatchCount())
                .put("remoteAddress", client.getRemoteAddress());
    }

    public static NetworkMessage success(NetworkMessage request, String message) {
        return NetworkMessage.reply(MessageType.SUCCESS, request)
                .put("message", message == null || message.isBlank() ? "operation completed" : message);
    }

    public static NetworkMessage error(NetworkMessage request, String message) {
        return NetworkMessage.reply(MessageType.ERROR, request)
                .put("message", message == null || message.isBlank() ? "server rejected the request" : message);
    }
}
