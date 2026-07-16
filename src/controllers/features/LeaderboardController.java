package controllers.features;

import controllers.auth.AuthController;
import controllers.core.SaveManager;
import models.account.Quest;
import models.account.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LeaderboardController {
    private final AuthController authController;
    private final SaveManager saveManager;
    private String lastMessage;

    public LeaderboardController() {
        this(null);
    }

    public LeaderboardController(AuthController authController) {
        this.authController = authController;
        saveManager = new SaveManager();
        lastMessage = "";
    }

    public List<User> getRankedUsers() {
        return getRankedUsers("best-score", false);
    }

    public List<User> getRankedUsers(String column, boolean ascending) {
        if (authController != null) {
            authController.saveUsers();
        }

        List<User> users = new ArrayList<>(saveManager.loadAllUsers());
        Comparator<User> comparator = comparatorFor(column);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(user -> safeText(user.getUsername()), String.CASE_INSENSITIVE_ORDER);
        users.removeIf(user -> user == null);
        users.sort(comparator);
        success("Leaderboard sorted by " + normalizedColumn(column) + ".");
        return users;
    }

    public int getDailyQuestCount(User user) {
        return countCompletedQuests(user, true);
    }

    public int getNonDailyQuestCount(User user) {
        return countCompletedQuests(user, false);
    }

    public int getCompletedMiniGameCount(User user) {
        return 0;
    }

    public String getLastProgress(User user) {
        if (user == null) {
            return "-";
        }
        String chapter = safeText(user.getCurrentChapterName());
        if (chapter.isEmpty()) {
            chapter = "chapter -";
        }
        return chapter + ", level " + user.getPassedLevels();
    }

    public boolean isLoggedIn() {
        return authController != null && authController.isLoggedIn();
    }

    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private Comparator<User> comparatorFor(String column) {
        String normalized = normalizedColumn(column);
        if ("username".equals(normalized)) {
            return Comparator.comparing(user -> safeText(user.getUsername()), String.CASE_INSENSITIVE_ORDER);
        }
        if ("progress".equals(normalized)) {
            return Comparator.comparingInt(User::getPassedLevels);
        }
        if ("minigames".equals(normalized)) {
            return Comparator.comparingInt(this::getCompletedMiniGameCount);
        }
        if ("daily-quests".equals(normalized)) {
            return Comparator.comparingInt(this::getDailyQuestCount);
        }
        if ("quests".equals(normalized)) {
            return Comparator.comparingInt(this::getNonDailyQuestCount);
        }
        return Comparator.comparingInt(User::getBestMioPoint);
    }

    private int countCompletedQuests(User user, boolean daily) {
        if (user == null || user.getQuests() == null) {
            return 0;
        }

        int count = 0;
        for (Quest quest : user.getQuests()) {
            if (quest == null || !quest.isCompleted()) {
                continue;
            }
            boolean dailyQuest = quest.getType().toLowerCase(Locale.ROOT).contains("daily");
            if (dailyQuest == daily) {
                count++;
            }
        }
        return count;
    }

    private String normalizedColumn(String column) {
        if (column == null || column.isBlank()) {
            return "best-score";
        }
        String normalized = column.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if ("score".equals(normalized) || "miopoint".equals(normalized)) {
            return "best-score";
        }
        return normalized;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }
}
