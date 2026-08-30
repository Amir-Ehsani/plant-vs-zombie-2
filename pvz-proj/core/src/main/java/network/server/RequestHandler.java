package network.server;

import network.protocol.NetworkMessage;

/** Handles one validated client request and returns a correlated server response. */
@FunctionalInterface
public interface RequestHandler {
    NetworkMessage handle(ClientConnection client, NetworkMessage request) throws Exception;
}
