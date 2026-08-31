package network.server.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Small server-side crypto helper. Raw client hashes/tokens are never persisted directly. */
final class ServerCrypto {
    private static final SecureRandom RANDOM = new SecureRandom();

    private ServerCrypto() { }

    static String randomToken(int bytes) {
        byte[] value = new byte[Math.max(16, bytes)];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    static String saltedVerifier(String salt, String clientHash) {
        if (salt == null || clientHash == null) return null;
        return sha256(salt + ":" + clientHash.trim().toLowerCase(java.util.Locale.ROOT));
    }

    static String sha256(String value) {
        if (value == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte b : hash) result.append(String.format("%02x", b));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) return false;
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    static boolean looksLikeSha256(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }
}
