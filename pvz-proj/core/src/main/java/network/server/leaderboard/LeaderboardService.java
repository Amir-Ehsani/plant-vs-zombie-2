package network.server.leaderboard;

import network.protocol.LeaderboardRow;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.server.ClientConnection;
import network.server.RequestDispatcher;
import network.server.account.AccountRepository;
import network.server.account.ServerAccount;
import network.server.account.ServerSessionManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Authoritative phase-three leaderboard and scored-game record service.
 *
 * Leaderboard rows are always derived from the server account store. The My Point
 * column is deliberately independent from the serialized client profile: it is
 * null until SCORE_SUBMIT is received from an authenticated network-scored run.
 */
public final class LeaderboardService {
    private static final Map<String, Integer> CHAPTER_ORDER = Map.of(
            "ancient-egypt", 0,
            "frostbite-caves", 1,
            "big-wave-beach", 2,
            "dark-ages", 3
    );

    private final AccountRepository repository;
    private final ServerSessionManager sessions;

    public LeaderboardService(AccountRepository repository, ServerSessionManager sessions) {
        if (repository == null || sessions == null) {
            throw new IllegalArgumentException("leaderboard dependencies are required");
        }
        this.repository = repository;
        this.sessions = sessions;
    }

    public void registerHandlers(RequestDispatcher dispatcher) {
        dispatcher.register(MessageType.LEADERBOARD_REQUEST, this::leaderboard);
        dispatcher.register(MessageType.SCORE_SUBMIT, this::submitScore);
    }

    private NetworkMessage leaderboard(ClientConnection client, NetworkMessage request) {
        requireAuthenticatedUsername(client, request);
        String column = normalizeColumn(request.getOrDefault("column", "score"));
        boolean ascending = request.getBoolean("ascending", false);

        List<ServerAccount> accounts = repository.all();
        accounts.sort(comparator(column, ascending));

        List<LeaderboardRow> rows = new ArrayList<>(accounts.size());
        for (ServerAccount account : accounts) {
            rows.add(toRow(account));
        }

        return RequestDispatcher.success(request, "leaderboard loaded")
                .put("column", column)
                .put("ascending", ascending)
                .put("rowCount", rows.size())
                .put("generatedAtEpochMillis", System.currentTimeMillis())
                .leaderboardRows(rows);
    }

    private NetworkMessage submitScore(ClientConnection client, NetworkMessage request) {
        String username = requireAuthenticatedUsername(client, request);
        String rawScore = request.get("score");
        if (rawScore == null || rawScore.isBlank()) {
            throw new IllegalArgumentException("score is required");
        }

        final int score;
        try {
            score = Integer.parseInt(rawScore.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("score must be an integer");
        }
        if (score < 0) {
            throw new IllegalArgumentException("score cannot be negative");
        }

        AccountRepository.ScoreUpdate update = repository.updateMyPointIfHigher(username, score);
        if (update == null) {
            throw new IllegalArgumentException("account no longer exists");
        }

        return RequestDispatcher.success(request,
                        update.improved() ? "new network scored-game record saved" : "score received; personal best unchanged")
                .put("submittedScore", score)
                .put("previousBest", update.previousBest())
                .put("personalBest", update.personalBest())
                .put("improved", update.improved());
    }

    private String requireAuthenticatedUsername(ClientConnection client, NetworkMessage request) {
        String username = sessions.authenticate(client, request.getSessionToken());
        if (username == null) {
            throw new IllegalArgumentException("authentication required or session expired");
        }
        return username;
    }

    private static LeaderboardRow toRow(ServerAccount account) {
        return new LeaderboardRow(
                account.getUsername(),
                normalizedChapter(account.getLastChapter()),
                parsedLevel(account.getLastLevel()),
                account.getMiniGamesWon(),
                account.getDailyQuestsDone(),
                account.getTotalQuestsDone(),
                account.getMyPoint()
        );
    }

    private static Comparator<ServerAccount> comparator(String column, boolean ascending) {
        Comparator<ServerAccount> comparator;
        switch (column) {
            case "username" -> comparator = Comparator.comparing(
                    account -> safe(account.getUsername()), String.CASE_INSENSITIVE_ORDER);
            case "level" -> comparator = Comparator.comparingInt(LeaderboardService::progressionRank);
            case "minigames" -> comparator = Comparator.comparingInt(ServerAccount::getMiniGamesWon);
            case "daily" -> comparator = Comparator.comparingInt(ServerAccount::getDailyQuestsDone);
            case "quests" -> comparator = Comparator.comparingInt(ServerAccount::getTotalQuestsDone);
            case "other" -> comparator = Comparator.comparingInt(account -> Math.max(0,
                    account.getTotalQuestsDone() - account.getDailyQuestsDone()));
            case "score" -> {
                // Unranked users stay at the bottom in both directions instead of
                // being silently converted into a synthetic score of zero.
                return scoreComparator(ascending)
                        .thenComparing(account -> safe(account.getUsername()), String.CASE_INSENSITIVE_ORDER);
            }
            default -> throw new IllegalArgumentException("unsupported leaderboard column: " + column);
        }
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(account -> safe(account.getUsername()), String.CASE_INSENSITIVE_ORDER);
    }

    private static Comparator<ServerAccount> scoreComparator(boolean ascending) {
        return (left, right) -> {
            Integer a = left.getMyPoint();
            Integer b = right.getMyPoint();
            if (a == null && b == null) {
                return 0;
            }
            if (a == null) {
                return 1;
            }
            if (b == null) {
                return -1;
            }
            int compared = Integer.compare(a, b);
            return ascending ? compared : -compared;
        };
    }

    private static int progressionRank(ServerAccount account) {
        String chapter = normalizedChapter(account.getLastChapter());
        int chapterIndex = CHAPTER_ORDER.getOrDefault(chapter, -1);
        // Known adventure chapters sort in their actual progression order. Unknown
        // or legacy chapter names remain valid but rank before known progression.
        return chapterIndex * 10_000 + parsedLevel(account.getLastLevel());
    }

    private static String normalizeColumn(String column) {
        String normalized = column == null ? "score" : column.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            normalized = "score";
        }
        return switch (normalized) {
            case "score", "username", "level", "minigames", "daily", "quests", "other" -> normalized;
            default -> throw new IllegalArgumentException("unsupported leaderboard column: " + normalized);
        };
    }

    private static String normalizedChapter(String chapter) {
        if (chapter == null || chapter.isBlank()) {
            return "";
        }
        return chapter.trim().toLowerCase(Locale.ROOT);
    }

    private static int parsedLevel(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try { return Math.max(0, Integer.parseInt(raw.trim())); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
