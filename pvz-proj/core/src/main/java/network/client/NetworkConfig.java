package network.client;

/** Host/port configuration shared by account and multiplayer operations. */
public final class NetworkConfig {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 54_555;

    private final String host;
    private final int port;
    private final int connectTimeoutMillis;

    public NetworkConfig(String host, int port, int connectTimeoutMillis) {
        this.host = host == null || host.isBlank() ? DEFAULT_HOST : host.trim();
        this.port = port < 1 || port > 65_535 ? DEFAULT_PORT : port;
        this.connectTimeoutMillis = Math.max(250, connectTimeoutMillis);
    }

    public static NetworkConfig fromEnvironment() {
        String host = System.getProperty("pvz.server.host");
        if (host == null || host.isBlank()) host = System.getenv("PVZ_SERVER_HOST");
        String portText = System.getProperty("pvz.server.port");
        if (portText == null || portText.isBlank()) portText = System.getenv("PVZ_SERVER_PORT");
        int port = DEFAULT_PORT;
        try { if (portText != null) port = Integer.parseInt(portText.trim()); }
        catch (NumberFormatException ignored) { }
        return new NetworkConfig(host, port, 2_000);
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public String endpoint() { return host + ":" + port; }
}
