package network.server;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.NetworkSerialization;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** One persistent server-side socket and read loop for one client. */
public final class ClientConnection implements Runnable, AutoCloseable {
    private final PvZServer server;
    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final Object writeLock = new Object();

    private ObjectOutputStream output;
    private ObjectInputStream input;
    private volatile long lastMessageAtEpochMillis = System.currentTimeMillis();

    ClientConnection(PvZServer server, Socket socket, RequestDispatcher dispatcher) {
        this.server = Objects.requireNonNull(server, "server");
        this.socket = Objects.requireNonNull(socket, "socket");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    @Override
    public void run() {
        try {
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);

            // Both sides create/flush ObjectOutputStream first, preventing header deadlocks.
            output = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            output.flush();
            input = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            NetworkSerialization.secure(input);

            server.log("client connected: " + getRemoteAddress());
            while (open.get() && server.isRunning()) {
                Object item = input.readObject();
                lastMessageAtEpochMillis = System.currentTimeMillis();

                if (!(item instanceof NetworkMessage request)) {
                    send(NetworkMessage.of(MessageType.ERROR).put("message", "wire payload must be NetworkMessage"));
                    server.log("invalid payload from " + getRemoteAddress() + ": "
                            + (item == null ? "null" : item.getClass().getName()));
                    continue;
                }

                NetworkMessage response = dispatcher.dispatch(this, request);
                if (response != null) send(response);
            }
        } catch (EOFException exception) {
            server.log("client EOF: " + getRemoteAddress());
        } catch (SocketException exception) {
            server.log("client socket closed: " + getRemoteAddress()
                    + (exception.getMessage() == null ? "" : " (" + exception.getMessage() + ")"));
        } catch (IOException exception) {
            if (open.get() && server.isRunning()) {
                server.log("connection error from " + getRemoteAddress() + ": " + exception.getMessage());
            }
        } catch (ClassNotFoundException exception) {
            server.log("unknown serialized class from " + getRemoteAddress() + ": " + exception.getMessage());
            try {
                send(NetworkMessage.of(MessageType.ERROR).put("message", "unsupported serialized payload"));
            } catch (IOException ignored) { }
        } finally {
            close();
            server.connectionClosed(this);
        }
    }

    /** Thread-safe write used by correlated replies and unsolicited server events. */
    public void send(NetworkMessage message) throws IOException {
        if (message == null) return;
        synchronized (writeLock) {
            if (!open.get() || output == null) throw new IOException("client connection is closed");
            try {
                output.writeObject(message);
                output.reset();
                output.flush();
            } catch (IOException exception) {
                close();
                throw exception;
            }
        }
    }

    public boolean isOpen() {
        return open.get() && !socket.isClosed();
    }

    public String getRemoteAddress() {
        return socket.getRemoteSocketAddress() == null ? "unknown" : socket.getRemoteSocketAddress().toString();
    }

    public long getLastMessageAtEpochMillis() {
        return lastMessageAtEpochMillis;
    }

    @Override
    public void close() {
        if (!open.compareAndSet(true, false)) return;
        try { socket.close(); } catch (IOException ignored) { }
        try { if (input != null) input.close(); } catch (IOException ignored) { }
        synchronized (writeLock) {
            try { if (output != null) output.close(); } catch (IOException ignored) { }
        }
        input = null;
        output = null;
    }
}
