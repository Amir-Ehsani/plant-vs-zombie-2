package network.server.account;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Short-lived, one-time password reset challenges. */
final class PasswordResetManager {
    private static final long RESET_TTL_MILLIS = 10L * 60L * 1000L;
    private static final int MAX_VERIFY_ATTEMPTS = 3;
    private final Map<String, ResetState> resets = new HashMap<>();

    synchronized String create(String username) {
        prune();
        String id = ServerCrypto.randomToken(24);
        resets.put(id, new ResetState(username, System.currentTimeMillis() + RESET_TTL_MILLIS));
        return id;
    }

    synchronized String usernameForVerification(String resetId) {
        ResetState state = valid(resetId);
        if (state == null || state.attempts >= MAX_VERIFY_ATTEMPTS) {
            return null;
        }
        return state.username;
    }

    synchronized boolean markAttempt(String resetId, boolean successful) {
        ResetState state = valid(resetId);
        if (state == null) {
            return false;
        }
        state.attempts++;
        if (successful) {
            state.verified = true;
        }
        if (!successful && state.attempts >= MAX_VERIFY_ATTEMPTS) {
            resets.remove(resetId);
        }
        return successful;
    }

    synchronized String consumeVerified(String resetId) {
        ResetState state = valid(resetId);
        if (state == null || !state.verified) {
            return null;
        }
        resets.remove(resetId);
        return state.username;
    }

    private ResetState valid(String resetId) {
        prune();
        if (resetId == null || resetId.isBlank()) {
            return null;
        }
        return resets.get(resetId);
    }

    private void prune() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, ResetState>> iterator = resets.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt < now) {
                iterator.remove();
            }
        }
    }

    private static final class ResetState {
        final String username;
        final long expiresAt;
        int attempts;
        boolean verified;

        ResetState(String username, long expiresAt) {
            this.username = username;
            this.expiresAt = expiresAt;
        }
    }
}
