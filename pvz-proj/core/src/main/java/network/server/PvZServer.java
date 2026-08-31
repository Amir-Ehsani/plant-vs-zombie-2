package network.server;

import network.server.account.AccountRepository;
import network.server.account.AccountService;
import network.server.account.ServerSessionManager;
import network.server.leaderboard.LeaderboardService;
import network.server.game.AuthoritativeGameService;
import network.server.matchmaking.MatchmakingService;
import network.server.reaction.ReactionService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Persistent multi-client TCP server for phase three.
 *
 * Owns transport, authoritative accounts/sessions, the live server leaderboard,
 * matchmaking, and the headless authoritative I, Zombie simulation.
 */
public final class PvZServer implements AutoCloseable {
    private final ServerConfig config;
    private final AtomicBoolean running = new AtomicBoolean();
    private final Set<ClientConnection> connections = ConcurrentHashMap.newKeySet();
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "pvz-server-client");
        thread.setDaemon(true);
        return thread;
    });
    private final CountDownLatch stopped = new CountDownLatch(1);
    private final RequestDispatcher dispatcher;
    private final AccountRepository accountRepository;
    private final ServerSessionManager sessionManager;
    private final AccountService accountService;
    private final LeaderboardService leaderboardService;
    private final MatchmakingService matchmakingService;
    private final AuthoritativeGameService authoritativeGameService;
    private final ReactionService reactionService;

    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;
    private volatile long startedAtEpochMillis;

    public PvZServer() {
        this(ServerConfig.fromEnvironment(), AccountRepository.defaultFile());
    }

    public PvZServer(int port) {
        this(new ServerConfig(ServerConfig.DEFAULT_BIND_HOST, port, 64), AccountRepository.defaultFile());
    }

    /** Test/development constructor that keeps account persistence isolated to the supplied file. */
    public PvZServer(int port, Path accountFile) {
        this(new ServerConfig(ServerConfig.DEFAULT_BIND_HOST, port, 64), accountFile);
    }

    public PvZServer(ServerConfig config) {
        this(config, AccountRepository.defaultFile());
    }

    public PvZServer(ServerConfig config, Path accountFile) {
        this.config = config == null ? ServerConfig.fromEnvironment() : config;
        this.dispatcher = new RequestDispatcher(this);
        this.accountRepository = new AccountRepository(accountFile);
        this.sessionManager = new ServerSessionManager(this);
        this.accountService = new AccountService(this, accountRepository, sessionManager);
        this.accountService.registerHandlers(dispatcher);
        this.leaderboardService = new LeaderboardService(accountRepository, sessionManager);
        this.leaderboardService.registerHandlers(dispatcher);
        this.matchmakingService = new MatchmakingService(this, accountRepository, sessionManager);
        this.matchmakingService.registerHandlers(dispatcher);
        this.authoritativeGameService = new AuthoritativeGameService(this, matchmakingService, sessionManager);
        this.authoritativeGameService.registerHandlers(dispatcher);
        this.matchmakingService.setGameService(authoritativeGameService);
        this.reactionService = new ReactionService(this, matchmakingService, sessionManager);
        this.reactionService.registerHandlers(dispatcher);
    }

    /** Starts accepting clients in a dedicated daemon thread. Safe to call once. */
    public synchronized void start() throws IOException {
        if (running.get()) return;
        if (stopped.getCount() == 0) throw new IOException("server instance cannot be restarted after shutdown");

        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new java.net.InetSocketAddress(InetAddress.getByName(config.getBindHost()),
                config.getPort()), config.getBacklog());
        serverSocket = socket;
        startedAtEpochMillis = System.currentTimeMillis();
        running.set(true);
        matchmakingService.start();
        authoritativeGameService.start();

        acceptThread = new Thread(this::acceptLoop, "pvz-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        log("server listening on " + config.getBindHost() + ":" + getPort());
    }

    /** Starts if necessary and blocks until close() is called. */
    public void serveForever() throws IOException, InterruptedException {
        start();
        stopped.await();
    }

    private void acceptLoop() {
        try {
            while (running.get()) {
                Socket socket = serverSocket.accept();
                ClientConnection connection = new ClientConnection(this, socket, dispatcher);
                connections.add(connection);
                try {
                    clientExecutor.execute(connection);
                } catch (RuntimeException exception) {
                    connections.remove(connection);
                    connection.close();
                    if (running.get()) log("could not start client handler: " + exception.getMessage());
                }
            }
        } catch (SocketException exception) {
            if (running.get()) log("server socket error: " + exception.getMessage());
        } catch (IOException exception) {
            if (running.get()) log("accept loop failed: " + exception.getMessage());
        } finally {
            if (running.get()) close();
        }
    }

    void connectionClosed(ClientConnection connection) {
        if (connection != null) {
            String username = sessionManager.usernameForConnection(connection);
            if (username != null) matchmakingService.sessionEnded(username, connection, "disconnected");
            sessionManager.connectionClosed(connection);
        }
        if (connection != null && connections.remove(connection)) {
            log("client disconnected: " + connection.getRemoteAddress());
        }
    }

    public RequestDispatcher getDispatcher() {
        return dispatcher;
    }

    public AccountRepository getAccountRepository() { return accountRepository; }
    public ServerSessionManager getSessionManager() { return sessionManager; }
    public AccountService getAccountService() { return accountService; }
    public LeaderboardService getLeaderboardService() { return leaderboardService; }
    public MatchmakingService getMatchmakingService() { return matchmakingService; }
    public AuthoritativeGameService getAuthoritativeGameService() { return authoritativeGameService; }
    public ReactionService getReactionService() { return reactionService; }

    public boolean isRunning() {
        return running.get();
    }

    /** Bound port; useful when tests construct the server with port 0. */
    public int getPort() {
        ServerSocket socket = serverSocket;
        return socket == null ? config.getPort() : socket.getLocalPort();
    }

    public int getConnectedClientCount() {
        int count = 0;
        for (ClientConnection connection : connections) if (connection.isOpen()) count++;
        return count;
    }

    public long getUptimeMillis() {
        return startedAtEpochMillis <= 0L ? 0L : Math.max(0L, System.currentTimeMillis() - startedAtEpochMillis);
    }

    public void log(String message) {
        System.out.println("[PVZ-SERVER] " + (message == null ? "" : message));
    }

    @Override
    public synchronized void close() {
        if (!running.compareAndSet(true, false)) return;

        ServerSocket socket = serverSocket;
        serverSocket = null;
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }

        for (ClientConnection connection : connections.toArray(ClientConnection[]::new)) {
            connection.close();
        }
        connections.clear();
        authoritativeGameService.close();
        matchmakingService.close();
        sessionManager.clearAll();

        clientExecutor.shutdownNow();
        try { clientExecutor.awaitTermination(2, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }

        stopped.countDown();
        log("server stopped");
    }

    public static void main(String[] args) throws Exception {
        ServerConfig base = ServerConfig.fromEnvironment();
        int port = base.getPort();
        if (args != null && args.length > 0) {
            try {
                int candidate = Integer.parseInt(args[0]);
                if (candidate >= 1 && candidate <= 65_535) port = candidate;
            } catch (NumberFormatException ignored) { }
        }

        PvZServer server = new PvZServer(new ServerConfig(base.getBindHost(), port, base.getBacklog()));
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "pvz-server-shutdown"));
        server.serveForever();
    }
}
