package network.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

/** Stores only the revocable persistent token; the authoritative profile remains on the server. */
final class NetworkSessionStore {
    private NetworkSessionStore() { }

    private static Path file() {
        String explicit = System.getProperty("pvz.network.sessionFile");
        if (explicit == null || explicit.isBlank()) explicit = System.getenv("PVZ_NETWORK_SESSION_FILE");
        return explicit == null || explicit.isBlank()
                ? Paths.get("data", "network-session.properties")
                : Paths.get(explicit.trim());
    }

    static SavedSession load() {
        Path file = file();
        if (!Files.exists(file)) return null;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            String username = properties.getProperty("username", "").trim();
            String token = properties.getProperty("persistentToken", "").trim();
            return username.isEmpty() || token.isEmpty() ? null : new SavedSession(username, token);
        } catch (IOException exception) {
            return null;
        }
    }

    static void save(String username, String persistentToken) throws IOException {
        if (username == null || username.isBlank() || persistentToken == null || persistentToken.isBlank()) {
            clear();
            return;
        }
        Path file = file();
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Properties properties = new Properties();
        properties.setProperty("username", username);
        properties.setProperty("persistentToken", persistentToken);
        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(output, "PVZ revocable server session");
        }
        restrictPermissions(file);
    }

    static void clear() {
        try { Files.deleteIfExists(file()); } catch (IOException ignored) { }
    }

    static Path fileForTests() { return file(); }

    private static void restrictPermissions(Path file) {
        try {
            Set<PosixFilePermission> ownerOnly = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(file, ownerOnly);
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows/non-POSIX file systems do not expose these permissions.
        }
    }

    static final class SavedSession {
        final String username;
        final String persistentToken;
        SavedSession(String username, String persistentToken) {
            this.username = username;
            this.persistentToken = persistentToken;
        }
    }
}
