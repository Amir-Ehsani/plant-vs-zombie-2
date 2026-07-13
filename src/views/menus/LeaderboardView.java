package views.menus;

import models.account.User;
import views.core.BaseView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderboardView extends BaseView {
    public LeaderboardView(String viewName) {
        super(viewName);
    }

    public LeaderboardView() {
        super("Leaderboard");
    }

    @Override
    public void display() {
        System.out.println("Leaderboard");
        System.out.println("===========");
        System.out.println("No ranking data provided.");
    }

    @Override
    public void showErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        System.out.println("ERROR: " + message);
    }

    @Override
    public void showSuccessMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        System.out.println("OK: " + message);
    }

    public void showRankings(List<User> rankedUsers) {
        System.out.print(renderRankings(rankedUsers));
    }

    public String renderRankings(List<User> users) {
        StringBuilder builder = new StringBuilder();

        builder.append("Leaderboard\n");
        builder.append("===========\n");

        if (users == null || users.isEmpty()) {
            builder.append("No users available.\n");
            return builder.toString();
        }

        List<User> rankedUsers = new ArrayList<>(users);
        rankedUsers.sort(Comparator.comparingInt(this::getScore).reversed());

        builder.append(String.format("%-6s %-20s %-20s %-10s %-10s %-10s%n", "Rank", "Username", "Nickname", "Score", "Coins", "Gems"));
        builder.append(String.format("%-6s %-20s %-20s %-10s %-10s %-10s%n", "----", "--------", "--------", "-----", "-----", "----"));

        int rank = 1;

        for (User user : rankedUsers) {
            if (user == null) {
                continue;
            }

            builder.append(String.format(
                    "%-6d %-20s %-20s %-10d %-10d %-10d%n",
                    rank,
                    safeText(user.getUsername()),
                    safeText(getStringField(user, "nickname")),
                    getScore(user),
                    getIntField(user, "coins"),
                    getIntField(user, "gems")
            ));

            rank++;
        }

        if (rank == 1) {
            builder.append("No users available.\n");
        }

        return builder.toString();
    }

    public String renderUserRank(User user, List<User> users) {
        if (user == null) {
            return "User is not available.\n";
        }

        if (users == null || users.isEmpty()) {
            return "Leaderboard is empty.\n";
        }

        List<User> rankedUsers = new ArrayList<>(users);
        rankedUsers.sort(Comparator.comparingInt(this::getScore).reversed());

        int rank = 1;

        for (User currentUser : rankedUsers) {
            if (currentUser == null) {
                continue;
            }

            if (sameUser(user, currentUser)) {
                StringBuilder builder = new StringBuilder();

                builder.append("User Rank\n");
                builder.append("=========\n");
                builder.append("Rank: ").append(rank).append("\n");
                builder.append("Username: ").append(safeText(currentUser.getUsername())).append("\n");
                builder.append("Nickname: ").append(safeText(getStringField(currentUser, "nickname"))).append("\n");
                builder.append("Score: ").append(getScore(currentUser)).append("\n");
                builder.append("Coins: ").append(getIntField(currentUser, "coins")).append("\n");
                builder.append("Gems: ").append(getIntField(currentUser, "gems")).append("\n");

                return builder.toString();
            }

            rank++;
        }

        return "User was not found in leaderboard.\n";
    }

    private boolean sameUser(User first, User second) {
        if (first == second) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        String firstUsername = first.getUsername();
        String secondUsername = second.getUsername();

        return firstUsername != null && firstUsername.equals(secondUsername);
    }

    private int getScore(User user) {
        return getIntField(user, "score");
    }

    private int getIntField(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);

            if (field == null) {
                return 0;
            }

            field.setAccessible(true);
            return field.getInt(target);
        } catch (IllegalAccessException exception) {
            return 0;
        }
    }

    private String getStringField(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);

            if (field == null) {
                return "";
            }

            field.setAccessible(true);
            Object value = field.get(target);

            if (value == null) {
                return "";
            }

            return value.toString();
        } catch (IllegalAccessException exception) {
            return "";
        }
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;

        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException exception) {
                current = current.getSuperclass();
            }
        }

        return null;
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value.trim();
    }
}