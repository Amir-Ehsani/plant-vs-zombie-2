package network.server.account;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.server.ClientConnection;
import network.server.PvZServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Ephemeral authenticated sessions, bound to the socket that created them. */
public final class ServerSessionManager {
    private final PvZServer server;
    private final Map<String, Session> byToken = new HashMap<>();
    private final Map<String, Session> byUsername = new HashMap<>();
    private final Map<ClientConnection, Session> byConnection = new HashMap<>();

    public ServerSessionManager(PvZServer server) {
        this.server = server;
    }

    public String createSession(String username, ClientConnection client) {
        if (username == null || username.isBlank() || client == null) {
            throw new IllegalArgumentException("session identity is required");
        }
        Session replaced;
        Session previousConnection;
        Session created;
        synchronized (this) {
            previousConnection = byConnection.remove(client);
            if (previousConnection != null) {
                removeInternal(previousConnection);
            }

            replaced = byUsername.get(key(username));
            if (replaced != null) {
                removeInternal(replaced);
            }

            String token = ServerCrypto.randomToken(32);
            created = new Session(token, username.trim(), client);
            byToken.put(token, created);
            byUsername.put(key(username), created);
            byConnection.put(client, created);
        }

        if (replaced != null && replaced.client != client) {
            try {
                replaced.client.send(NetworkMessage.of(MessageType.SESSION_REPLACED)
                        .put("message", "this account was signed in from another client"));
            } catch (IOException ignored) { }
        }
        return created.token;
    }

    public synchronized String authenticate(ClientConnection client, String token) {
        if (client == null || token == null || token.isBlank()) {
            return null;
        }
        Session session = byToken.get(token);
        if (session == null || session.client != client || !client.isOpen()) {
            return null;
        }
        return session.username;
    }

    public synchronized boolean isOnline(String username) {
        Session session = byUsername.get(key(username));
        return session != null && session.client.isOpen();
    }

    public synchronized ClientConnection getConnection(String username) {
        Session session = byUsername.get(key(username));
        return session == null || !session.client.isOpen() ? null : session.client;
    }

    /** Returns the authenticated username currently bound to this exact socket. */
    public synchronized String usernameForConnection(ClientConnection client) {
        Session session = client == null ? null : byConnection.get(client);
        return session == null ? null : session.username;
    }

    public synchronized void logout(ClientConnection client, String token) {
        if (client == null || token == null) {
            return;
        }
        Session session = byToken.get(token);
        if (session != null && session.client == client) {
            removeInternal(session);
        }
    }

    public synchronized void connectionClosed(ClientConnection client) {
        Session session = byConnection.get(client);
        if (session != null) {
            removeInternal(session);
        }
    }

    public void invalidateUser(String username, String reason) {
        Session removed;
        synchronized (this) {
            removed = byUsername.get(key(username));
            if (removed != null) {
                removeInternal(removed);
            }
        }
        if (removed != null && removed.client.isOpen()) {
            try {
                removed.client.send(NetworkMessage.of(MessageType.SESSION_REPLACED)
                        .put("message", reason == null || reason.isBlank() ? "server session was revoked" : reason));
            } catch (IOException ignored) { }
        }
    }

    public synchronized void renameUser(String oldUsername, String newUsername) {
        Session session = byUsername.remove(key(oldUsername));
        if (session == null) {
            return;
        }
        session.username = newUsername;
        byUsername.put(key(newUsername), session);
    }

    public synchronized int onlineCount() { return byUsername.size(); }

    public synchronized void clearAll() {
        byToken.clear();
        byUsername.clear();
        byConnection.clear();
    }

    private void removeInternal(Session session) {
        byToken.remove(session.token, session);
        byUsername.remove(key(session.username), session);
        byConnection.remove(session.client, session);
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Session {
        final String token;
        String username;
        final ClientConnection client;

        Session(String token, String username, ClientConnection client) {
            this.token = token;
            this.username = username;
            this.client = client;
        }
    }
}
