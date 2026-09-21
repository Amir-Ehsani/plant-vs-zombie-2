package controllers.features;

import controllers.auth.AuthController;
import controllers.core.SaveManager;
import models.account.Quest;
import models.account.User;
import network.client.NetworkManager;
import network.protocol.LeaderboardRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LeaderboardController {
    public record LeaderboardEntry(
            String username,
            String progress,
            int miniGames,
            int dailyQuests,
            int otherQuests,
            Integer myPoint
    ) { }

    private final AuthController authController;
    private final NetworkManager networkManager;
    private final SaveManager saveManager;
    private String lastMessage;

    public LeaderboardController() { this(null, null); }
    public LeaderboardController(AuthController authController) { this(authController, null); }

    public LeaderboardController(AuthController authController, NetworkManager networkManager) {
        this.authController = authController;
        this.networkManager = networkManager;
        saveManager = new SaveManager();
        lastMessage = "";
    }

    public List<LeaderboardEntry> getEntries(String column, boolean ascending) {
        if (shouldUseServerLeaderboard()) {
            if (!networkManager.isAuthenticated()) {
                fail("Server leaderboard requires an active server session.");
                return List.of();
            }
            List<LeaderboardRow> rows = networkManager.leaderboardRows(serverColumn(column), ascending);
            if (!networkManager.getLastError().isBlank()) {
                fail(networkManager.getLastError());
                return List.of();
            }
            List<LeaderboardEntry> entries = new ArrayList<>();
            for (LeaderboardRow row : rows) {
                int other = Math.max(0, row.getTotalQuestsDone() - row.getDailyQuestsDone());
                String chapter = safeText(row.getLastChapter());
                String progress = chapter.isEmpty() || row.getLastLevel() <= 0
                        ? "-"
                        : chapter + ", level " + row.getLastLevel();
                entries.add(new LeaderboardEntry(row.getUsername(), progress, row.getMiniGamesWon(),
                        row.getDailyQuestsDone(), other, row.getMyPoint()));
            }
            success("Server leaderboard loaded.");
            return entries;
        }

        List<User> users = getRankedUsers(column, ascending);
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (User user : users) {
            entries.add(new LeaderboardEntry(user.getUsername(), getLastProgress(user),
                    getCompletedMiniGameCount(user), getDailyQuestCount(user),
                    getNonDailyQuestCount(user), user.getBestMioPoint()));
        }
        return entries;
    }

    private boolean shouldUseServerLeaderboard() {
        if (networkManager == null) {
            return false;
        }
        return authController == null || authController.isNetworkBacked();
    }

    public List<User> getRankedUsers() { return getRankedUsers("best-score", false); }

    public List<User> getRankedUsers(String column, boolean ascending) {
        if (authController != null) {
            authController.saveUsers();
        }
        List<User> users = new ArrayList<>(saveManager.loadAllUsers());
        users.removeIf(user -> user == null);
        Comparator<User> comparator = comparatorFor(column);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(user -> safeText(user.getUsername()), String.CASE_INSENSITIVE_ORDER);
        users.sort(comparator);
        success("Leaderboard sorted by " + normalizedColumn(column) + ".");
        return users;
    }

    public int getDailyQuestCount(User user) { return countCompletedQuests(user, true); }
    public int getNonDailyQuestCount(User user) { return countCompletedQuests(user, false); }
    public int getCompletedMiniGameCount(User user) { return user == null ? 0 : user.getCompletedMiniGameStageCount(); }

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

    public boolean isLoggedIn() { return authController != null && authController.isLoggedIn(); }
    public void invalidCommand(String menuName) { fail("Invalid command in " + menuName + "."); }
    public String getLastMessage() { return lastMessage; }
    public boolean wasSuccessful() { return lastMessage != null && lastMessage.startsWith("OK:"); }

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
            String type = quest.getType().toLowerCase(Locale.ROOT);
            boolean dailyQuest = type.contains("daily") || type.contains("challenge");
            if (dailyQuest == daily) {
                count++;
            }
        }
        return count;
    }

    private String serverColumn(String column) {
        String normalized = normalizedColumn(column);
        return switch (normalized) {
            case "best-score" -> "score";
            case "progress" -> "level";
            case "daily-quests" -> "daily";
            case "quests" -> "other";
            default -> normalized;
        };
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

    private String safeText(String value) { return value == null ? "" : value.trim(); }
    private void success(String message) { lastMessage = "OK: " + message; }
    private void fail(String message) { lastMessage = "ERROR: " + message; }
}
