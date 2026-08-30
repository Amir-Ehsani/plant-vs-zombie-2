package network.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Immutable server-owned projection used by the graphical leaderboard. */
public final class LeaderboardRow implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final String username;
    private final String lastChapter;
    private final int lastLevel;
    private final int miniGamesWon;
    private final int dailyQuestsDone;
    private final int totalQuestsDone;
    private final Integer myPoint;

    public LeaderboardRow(String username,
                          String lastChapter,
                          int lastLevel,
                          int miniGamesWon,
                          int dailyQuestsDone,
                          int totalQuestsDone,
                          Integer myPoint) {
        this.username = Objects.requireNonNullElse(username, "");
        this.lastChapter = Objects.requireNonNullElse(lastChapter, "");
        this.lastLevel = Math.max(0, lastLevel);
        this.miniGamesWon = Math.max(0, miniGamesWon);
        this.dailyQuestsDone = Math.max(0, dailyQuestsDone);
        this.totalQuestsDone = Math.max(0, totalQuestsDone);
        this.myPoint = myPoint == null ? null : Math.max(0, myPoint);
    }

    public String getUsername() { return username; }
    public String getLastChapter() { return lastChapter; }
    public int getLastLevel() { return lastLevel; }
    public int getMiniGamesWon() { return miniGamesWon; }
    public int getDailyQuestsDone() { return dailyQuestsDone; }
    public int getTotalQuestsDone() { return totalQuestsDone; }
    public Integer getMyPoint() { return myPoint; }

    @Override
    public String toString() {
        return "LeaderboardRow{" +
                "username='" + username + '\'' +
                ", lastChapter='" + lastChapter + '\'' +
                ", lastLevel=" + lastLevel +
                ", miniGamesWon=" + miniGamesWon +
                ", dailyQuestsDone=" + dailyQuestsDone +
                ", totalQuestsDone=" + totalQuestsDone +
                ", myPoint=" + myPoint +
                '}';
    }
}
