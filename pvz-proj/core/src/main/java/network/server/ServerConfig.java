package network.server;

import network.client.NetworkConfig;

/** Runtime settings for the phase-three socket server. */
public final class ServerConfig {
    public static final String DEFAULT_BIND_HOST = "0.0.0.0";
    public static final int DEFAULT_PORT = NetworkConfig.DEFAULT_PORT;

    private final String bindHost;
    private final int port;
    private final int backlog;

    public ServerConfig(String bindHost, int port, int backlog) {
        this.bindHost = bindHost == null || bindHost.isBlank() ? DEFAULT_BIND_HOST : bindHost.trim();
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("server port must be between 0 and 65535");
        }
        this.port = port;
        this.backlog = Math.max(8, backlog);
    }

    public static ServerConfig fromEnvironment() {
        String host = System.getProperty("pvz.server.bind");
        if (host == null || host.isBlank()) {
            host = System.getenv("PVZ_SERVER_BIND");
        }

        String portText = System.getProperty("pvz.server.port");
        if (portText == null || portText.isBlank()) {
            portText = System.getenv("PVZ_SERVER_PORT");
        }
        int port = DEFAULT_PORT;
        try {
            if (portText != null && !portText.isBlank()) {
                port = Integer.parseInt(portText.trim());
            }
        } catch (NumberFormatException ignored) {
            port = DEFAULT_PORT;
        }
        if (port < 1 || port > 65_535) {
            port = DEFAULT_PORT;
        }
        return new ServerConfig(host, port, 64);
    }

    public String getBindHost() { return bindHost; }
    public int getPort() { return port; }
    public int getBacklog() { return backlog; }
}
