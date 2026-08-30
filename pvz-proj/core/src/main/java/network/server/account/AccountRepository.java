package network.server.account;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Thread-safe persistent repository for authoritative server accounts. */
public final class AccountRepository {
    private static final int STORE_VERSION = 1;
    private static final long MAX_STORE_BYTES = 128L * 1024L * 1024L;

    private final Path file;
    private final Map<String, ServerAccount> accounts = new LinkedHashMap<>();

    public AccountRepository(Path file) {
        this.file = file == null ? defaultFile() : file.toAbsolutePath().normalize();
        load();
    }

    public static Path defaultFile() {
        String explicit = System.getProperty("pvz.server.accounts");
        if (explicit == null || explicit.isBlank()) explicit = System.getenv("PVZ_SERVER_ACCOUNTS");
        if (explicit != null && !explicit.isBlank()) return Paths.get(explicit.trim());
        return Paths.get("data", "server", "accounts.ser");
    }

    public synchronized boolean create(ServerAccount account) {
        if (account == null) throw new IllegalArgumentException("account is required");
        String key = key(account.getUsername());
        if (key == null || accounts.containsKey(key)) return false;
        accounts.put(key, account.copy());
        persist();
        return true;
    }

    public synchronized ServerAccount find(String username) {
        String key = key(username);
        ServerAccount account = key == null ? null : accounts.get(key);
        return account == null ? null : account.copy();
    }

    public synchronized boolean exists(String username) {
        String key = key(username);
        return key != null && accounts.containsKey(key);
    }

    public synchronized void save(ServerAccount account) {
        if (account == null || key(account.getUsername()) == null) throw new IllegalArgumentException("account is required");
        String key = key(account.getUsername());
        if (!accounts.containsKey(key)) throw new IllegalArgumentException("account no longer exists");
        accounts.put(key, account.copy());
        persist();
    }

    public synchronized boolean rename(String oldUsername, ServerAccount renamed) {
        String oldKey = key(oldUsername);
        String newKey = renamed == null ? null : key(renamed.getUsername());
        if (oldKey == null || newKey == null || !accounts.containsKey(oldKey)) return false;
        if (!oldKey.equals(newKey) && accounts.containsKey(newKey)) return false;
        accounts.remove(oldKey);
        accounts.put(newKey, renamed.copy());
        persist();
        return true;
    }

    public synchronized List<ServerAccount> all() {
        List<ServerAccount> copy = new ArrayList<>(accounts.size());
        for (ServerAccount account : accounts.values()) copy.add(account.copy());
        return copy;
    }

    /** Number of authoritative accounts currently stored on this server. */
    public synchronized int count() { return accounts.size(); }

    /** Atomically keeps only the highest score submitted through the network scored-game path. */
    public synchronized ScoreUpdate updateMyPointIfHigher(String username, int score) {
        String accountKey = key(username);
        ServerAccount account = accountKey == null ? null : accounts.get(accountKey);
        if (account == null) return null;

        int safeScore = Math.max(0, score);
        Integer previous = account.getMyPoint();
        boolean improved = previous == null || safeScore > previous;
        if (improved) {
            account.setMyPoint(safeScore);
            accounts.put(accountKey, account.copy());
            persist();
        }
        return new ScoreUpdate(previous, improved ? safeScore : previous, improved);
    }

    public Path getFile() { return file; }

    /** Immutable result of an atomic scored-game record update. */
    public record ScoreUpdate(Integer previousBest, Integer personalBest, boolean improved) { }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            if (Files.size(file) > MAX_STORE_BYTES) throw new IOException("account store is too large");
            try (ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                input.setObjectInputFilter(info -> {
                    if (info.depth() > 24 || info.references() > 500_000 || info.streamBytes() > MAX_STORE_BYTES) {
                        return ObjectInputFilter.Status.REJECTED;
                    }
                    Class<?> type = info.serialClass();
                    if (type == null) return ObjectInputFilter.Status.UNDECIDED;
                    while (type.isArray()) type = type.getComponentType();
                    if (type.isPrimitive()) return ObjectInputFilter.Status.ALLOWED;
                    String name = type.getName();
                    if (name.equals(ServerAccount.class.getName()) || name.equals(AccountStore.class.getName())
                            || name.startsWith("java.lang.") || name.startsWith("java.util.")) {
                        return ObjectInputFilter.Status.ALLOWED;
                    }
                    return ObjectInputFilter.Status.REJECTED;
                });
                Object restored = input.readObject();
                if (!(restored instanceof AccountStore store) || store.version != STORE_VERSION) {
                    throw new IOException("unsupported account store format");
                }
                accounts.clear();
                for (ServerAccount account : store.accounts) {
                    if (account != null && key(account.getUsername()) != null) {
                        accounts.put(key(account.getUsername()), account.copy());
                    }
                }
            }
        } catch (IOException | ClassNotFoundException | RuntimeException exception) {
            throw new IllegalStateException("could not load server accounts from " + file + ": " + exception.getMessage(), exception);
        }
    }

    private void persist() {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            AccountStore store = new AccountStore();
            for (ServerAccount account : accounts.values()) store.accounts.add(account.copy());
            try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
                output.writeObject(store);
                output.flush();
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("could not persist server accounts: " + exception.getMessage(), exception);
        }
    }

    private static String key(String username) {
        if (username == null) return null;
        String trimmed = username.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static final class AccountStore implements java.io.Serializable {
        @java.io.Serial private static final long serialVersionUID = 1L;
        int version = STORE_VERSION;
        List<ServerAccount> accounts = new ArrayList<>();
    }
}
