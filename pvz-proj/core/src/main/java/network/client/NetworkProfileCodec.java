package network.client;

import controllers.core.SaveManager;
import models.account.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class NetworkProfileCodec {
    private static final int MAX_PROFILE_BYTES = 6 * 1024 * 1024;
    private final SaveManager saveManager = new SaveManager();

    byte[] encode(User user) throws IOException {
        if (user == null) {
            throw new IOException("profile is missing");
        }
        byte[] bytes = saveManager.exportUser(user).getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > MAX_PROFILE_BYTES) {
            throw new IOException("profile is too large for server synchronization");
        }
        return bytes;
    }

    User decode(byte[] payload) throws IOException {
        if (payload == null || payload.length == 0 || payload.length > MAX_PROFILE_BYTES) {
            throw new IOException("server returned an invalid profile");
        }
        User user = saveManager.importUser(new String(payload, StandardCharsets.UTF_8));
        if (user == null) {
            throw new IOException("server profile could not be decoded");
        }
        return user;
    }
}
