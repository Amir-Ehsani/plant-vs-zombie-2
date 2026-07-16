package views.menus;

import controllers.core.MenuManager;
import controllers.features.LeaderboardController;
import models.account.User;
import views.core.BaseView;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeaderboardView extends BaseView {
    private static final Pattern SORT_PATTERN = Pattern.compile(
            "^menu\\s+leaderboard\\s+sort\\s+-b\\s+(\\S+)\\s+-o\\s+(asc|desc)\\s*$"
    );

    private final MenuManager menuManager;
    private final LeaderboardController controller;

    public LeaderboardView(String viewName, MenuManager menuManager, LeaderboardController controller) {
        super(viewName);
        this.menuManager = menuManager;
        this.controller = controller;
    }

    public LeaderboardView(String viewName) {
        this(viewName, null, new LeaderboardController());
    }

    public LeaderboardView() {
        this("Leaderboard Menu");
    }

    @Override
    public void display() {
        System.out.print(menuText());
        if (controller.isLoggedIn()) {
            showRankings(controller.getRankedUsers());
        }
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);
        if (!isConnected()) {
            printControllerMessage("ERROR: Leaderboard menu is not connected.");
            return;
        }
        if (!controller.isLoggedIn()) {
            printControllerMessage("ERROR: No user is logged in.");
            menuManager.enterLoginMenu();
            return;
        }

        if (handleNavigation(command) || handleLeaderboard(command)) {
            return;
        }
        controller.invalidCommand("leaderboard menu");
        printControllerMessage(controller.getLastMessage());
    }

    public String menuText() {
        return """
                Leaderboard Menu
                menu leaderboard
                menu leaderboard sort -b <column> -o <asc|desc>
                columns: username, progress, minigames, daily-quests, quests, best-score
                menu show current
                menu exit
                """;
    }

    public void showRankings(List<User> rankedUsers) {
        System.out.print(renderRankings(rankedUsers));
    }

    public String renderRankings(List<User> users) {
        StringBuilder builder = new StringBuilder("Leaderboard\n===========\n");
        if (users == null || users.isEmpty()) {
            return builder.append("No users available.\n").toString();
        }

        builder.append(String.format(
                "%-5s %-16s %-24s %-10s %-12s %-12s %-10s%n",
                "Rank", "Username", "Last progress", "MiniGames", "Daily quests", "Other quests", "Best score"
        ));
        int rank = 1;
        for (User user : users) {
            if (user != null) {
                appendUser(builder, rank++, user);
            }
        }
        return builder.toString();
    }

    public String renderUserRank(User user, List<User> users) {
        if (user == null || users == null) {
            return "User or leaderboard is not available.\n";
        }
        int rank = 1;
        for (User current : users) {
            if (current != null && current.getUsername().equals(user.getUsername())) {
                return "Rank: " + rank + "\n" + renderRankings(List.of(current));
            }
            rank++;
        }
        return "User was not found in leaderboard.\n";
    }

    private boolean handleNavigation(String command) {
        if ("menu show current".equals(command)) {
            menuManager.showCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }
        if ("menu exit".equals(command)) {
            menuManager.enterGameMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }
        return false;
    }

    private boolean handleLeaderboard(String command) {
        if ("menu leaderboard".equals(command)) {
            showRankings(controller.getRankedUsers());
            return true;
        }

        Matcher matcher = SORT_PATTERN.matcher(command);
        if (!matcher.matches()) {
            return false;
        }
        boolean ascending = "asc".equals(matcher.group(2));
        showRankings(controller.getRankedUsers(matcher.group(1), ascending));
        return true;
    }

    private void appendUser(StringBuilder builder, int rank, User user) {
        builder.append(String.format(
                "%-5d %-16s %-24s %-10d %-12d %-12d %-10d%n",
                rank,
                safeText(user.getUsername()),
                controller.getLastProgress(user),
                controller.getCompletedMiniGameCount(user),
                controller.getDailyQuestCount(user),
                controller.getNonDailyQuestCount(user),
                user.getBestMioPoint()
        ));
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private boolean isConnected() {
        return menuManager != null && controller != null;
    }
}
