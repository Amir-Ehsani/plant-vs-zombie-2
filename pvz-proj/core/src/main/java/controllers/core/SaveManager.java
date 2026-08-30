package controllers.core;

import models.account.Collection;
import models.account.Greenhouse;
import models.account.News;
import models.account.PlantData;
import models.account.Quest;
import models.account.User;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class SaveManager extends SaveManagerWriter {
    public void saveAllUsers(List<User> users) {
        try {
            Files.createDirectories(SAVE_DIRECTORY);
            Files.writeString(USERS_FILE, usersToJson(users), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public List<User> loadAllUsers() {
        if (!Files.exists(USERS_FILE)) {
            return new ArrayList<>();
        }

        try {
            String json = Files.readString(USERS_FILE, StandardCharsets.UTF_8);
            Object parsed = new JsonParser(json).parse();

            if (!(parsed instanceof List<?> parsedUsers)) {
                return new ArrayList<>();
            }

            List<User> users = new ArrayList<>();

            for (Object parsedUser : parsedUsers) {
                User user = mapToUser(parsedUser);

                if (user != null) {
                    users.add(user);
                }
            }

            return users;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }
    /** Serializes one complete user profile for server-side phase-three synchronization. */
    public String exportUser(User user) {
        return user == null ? "" : userToJson(user);
    }

    /** Restores one complete user profile received from the phase-three server. */
    public User importUser(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Object parsed = new JsonParser(json).parse();
            return mapToUser(parsed);
        } catch (RuntimeException exception) {
            return null;
        }
    }

}
