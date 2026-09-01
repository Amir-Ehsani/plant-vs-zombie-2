package network.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

/**
 * Small durable outbox for the bonus network-scored mode.
 *
 * A score is tied to the username that earned it, so an offline submission can
 * never be accidentally flushed into the next account that signs in. Only the
 * highest unsent score for each username is kept because the server itself also
 * stores a personal best.
 */
final class NetworkPendingScoreStore {
    private static Path file() {
        String explicit = System.getProperty("pvz.network.pendingScores");
        if (explicit == null || explicit.isBlank()) {
            explicit = System.getenv("PVZ_NETWORK_PENDING_SCORES");
        }
        return explicit == null || explicit.isBlank()
                ? Paths.get("data", "network-pending-scores.properties")
                : Paths.get(explicit.trim());
    }

    private NetworkPendingScoreStore() { }

    static synchronized void saveMax(String username, int score) {
        String key = key(username);
        if (key == null) {
            return;
        }
        Properties properties = loadProperties();
        String scoreKey = "score." + key;
        int previous = parse(properties.getProperty(scoreKey), -1);
        int safeScore = Math.max(0, score);
        if (safeScore <= previous) {
            return;
        }
        properties.setProperty(scoreKey, String.valueOf(safeScore));
        properties.setProperty("name." + key, username.trim());
        persist(properties);
    }

    static synchronized int load(String username) {
        String key = key(username);
        if (key == null) {
            return -1;
        }
        return parse(loadProperties().getProperty("score." + key), -1);
    }

    static synchronized void clear(String username) {
        String key = key(username);
        if (key == null || !Files.exists(file())) {
            return;
        }
        Properties properties = loadProperties();
        properties.remove("score." + key);
        properties.remove("name." + key);
        if (properties.isEmpty()) {
            try { Files.deleteIfExists(file()); } catch (IOException ignored) { }
        } else {
            persist(properties);
        }
    }

    static Path fileForTests() { return file(); }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        if (!Files.exists(file())) {
            return properties;
        }
        try (InputStream input = Files.newInputStream(file())) {
            properties.load(input);
        } catch (IOException ignored) { }
        return properties;
    }

    private static void persist(Properties properties) {
        try {
            Path target = file();
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(target)) {
                properties.store(output, "PVZ pending network scored-game personal bests");
            }
        } catch (IOException ignored) {
            // The score is best-effort durability. A failed disk write must not
            // crash the game or replace the authoritative server value.
        }
    }

    private static int parse(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static String key(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
