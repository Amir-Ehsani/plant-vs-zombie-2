package network.client;

import network.protocol.NetworkMessage;
import network.protocol.NetworkSerialization;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One persistent client/server connection with correlated requests and queued
 * unsolicited server events.
 *
 * Stage 10 hardening notes:
 * - a timed-out request tears down the transport instead of leaving a zombie
 *   socket that still reports itself as connected;
 * - pending requests are failed immediately when the client is closed;
 * - the unsolicited event queue is bounded so a paused/minimized client cannot
 *   grow memory forever while snapshots are being broadcast;
 * - last-read/last-write timestamps are exposed for connection diagnostics.
 */
public final class PvZNetworkClient implements AutoCloseable {
    private static final int MAX_QUEUED_EVENTS = 2_048;

    private final Map<String, CompletableFuture<NetworkMessage>> pending = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<NetworkMessage> events = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queuedEventCount = new AtomicInteger();
    private final AtomicBoolean connected = new AtomicBoolean();
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Thread readerThread;
    private volatile String sessionToken;
    private volatile String disconnectReason = "not connected";
    private volatile long lastReceivedAtEpochMillis;
    private volatile long lastSentAtEpochMillis;

    public synchronized void connect(NetworkConfig config) throws IOException {
        if (config == null) throw new IOException("network configuration is missing");
        if (connected.get() && socket != null && socket.isConnected() && !socket.isClosed()) return;

        closeTransport();
        clearEvents();
        Socket candidate = new Socket();
        try {
            candidate.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getConnectTimeoutMillis());
            candidate.setTcpNoDelay(true);
            candidate.setKeepAlive(true);
            ObjectOutputStream outgoing = new ObjectOutputStream(new BufferedOutputStream(candidate.getOutputStream()));
            outgoing.flush();
            ObjectInputStream incoming = new ObjectInputStream(new BufferedInputStream(candidate.getInputStream()));
            NetworkSerialization.secure(incoming);
            socket = candidate;
            output = outgoing;
            input = incoming;
            disconnectReason = "";
            long now = System.currentTimeMillis();
            lastReceivedAtEpochMillis = now;
            lastSentAtEpochMillis = now;
            connected.set(true);
            readerThread = new Thread(this::readerLoop, "pvz-network-reader");
            readerThread.setDaemon(true);
            readerThread.start();
        } catch (IOException | RuntimeException exception) {
            try { candidate.close(); } catch (IOException ignored) { }
            closeTransport();
            connected.set(false);
            disconnectReason = "connection failed: " + readable(exception);
            throw exception;
        }
    }

    public NetworkMessage request(NetworkMessage request, long timeoutMillis) throws Exception {
        if (request == null) throw new IOException("network request is missing");
        if (!connected.get()) throw new IOException("not connected to server");
        if (sessionToken != null && request.getSessionToken() == null) request.sessionToken(sessionToken);
        CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
        CompletableFuture<NetworkMessage> previous = pending.putIfAbsent(request.getRequestId(), future);
        if (previous != null) throw new IOException("duplicate request id");
        try {
            send(request);
            return future.get(Math.max(250L, timeoutMillis), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            // A TCP socket can remain in ESTABLISHED state long after the remote process
            // or network path has disappeared. Treat an unanswered correlated request as
            // a dead transport so the next high-level operation can reconnect cleanly.
            disconnected("server response timed out");
            throw new IOException("server response timed out", exception);
        } finally {
            pending.remove(request.getRequestId(), future);
        }
    }

    public synchronized void send(NetworkMessage message) throws IOException {
        if (message == null) return;
        if (!connected.get() || output == null) throw new IOException("not connected to server");
        if (sessionToken != null && message.getSessionToken() == null) message.sessionToken(sessionToken);
        try {
            output.writeObject(message);
            output.flush();
            output.reset();
            lastSentAtEpochMillis = System.currentTimeMillis();
        } catch (IOException exception) {
            disconnected("connection write failed: " + readable(exception));
            throw exception;
        }
    }

    private void readerLoop() {
        try {
            while (connected.get()) {
                Object item = input.readObject();
                lastReceivedAtEpochMillis = System.currentTimeMillis();
                if (!(item instanceof NetworkMessage message)) continue;
                String replyTo = message.getReplyTo();
                CompletableFuture<NetworkMessage> future = replyTo == null ? null : pending.remove(replyTo);
                if (future != null) future.complete(message);
                else enqueueEvent(message);
            }
        } catch (EOFException exception) {
            disconnected("server closed the connection");
        } catch (IOException | ClassNotFoundException exception) {
            disconnected("connection lost: " + readable(exception));
        }
    }

    private void enqueueEvent(NetworkMessage message) {
        if (message == null) return;
        events.add(message);
        int size = queuedEventCount.incrementAndGet();
        while (size > MAX_QUEUED_EVENTS) {
            NetworkMessage dropped = events.poll();
            if (dropped == null) {
                queuedEventCount.set(0);
                break;
            }
            size = queuedEventCount.decrementAndGet();
        }
    }

    private void disconnected(String reason) {
        disconnectReason = reason == null || reason.isBlank() ? "connection lost" : reason;
        if (!connected.compareAndSet(true, false)) return;
        sessionToken = null;
        failPending(new IOException(disconnectReason));
        closeTransport();
    }

    private void failPending(IOException failure) {
        for (CompletableFuture<NetworkMessage> future : pending.values()) future.completeExceptionally(failure);
        pending.clear();
    }

    public NetworkMessage pollEvent() {
        NetworkMessage message = events.poll();
        if (message != null) queuedEventCount.updateAndGet(value -> Math.max(0, value - 1));
        return message;
    }

    public List<NetworkMessage> drainEvents() {
        List<NetworkMessage> drained = new ArrayList<>();
        NetworkMessage message;
        while ((message = pollEvent()) != null) drained.add(message);
        return drained;
    }

    public void clearEvents() {
        events.clear();
        queuedEventCount.set(0);
    }

    public boolean isConnected() { return connected.get(); }
    public boolean isAuthenticated() { return connected.get() && sessionToken != null; }
    public String getDisconnectReason() { return disconnectReason; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = blankToNull(sessionToken); }
    public long getLastReceivedAtEpochMillis() { return lastReceivedAtEpochMillis; }
    public long getLastSentAtEpochMillis() { return lastSentAtEpochMillis; }
    public int getQueuedEventCount() { return queuedEventCount.get(); }

    @Override
    public synchronized void close() {
        boolean wasConnected = connected.getAndSet(false);
        sessionToken = null;
        disconnectReason = wasConnected ? "client closed" : disconnectReason;
        failPending(new IOException("client closed"));
        closeTransport();
    }

    private synchronized void closeTransport() {
        try { if (input != null) input.close(); } catch (IOException ignored) { }
        try { if (output != null) output.close(); } catch (IOException ignored) { }
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
        input = null;
        output = null;
        socket = null;
        readerThread = null;
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String readable(Throwable throwable) {
        if (throwable == null) return "unknown error";
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
