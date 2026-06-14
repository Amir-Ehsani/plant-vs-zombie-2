package models.engine;

import models.level.Chapter;
import models.level.Level;
import models.level.Season;

public class GameSession {
    private GameState state;
    private Season currentSeason;
    private int totalSunAmount;
    private Chapter currentChapter;
    private Level currentLevel;
    private Board board;
    private TickManager tickManager;
    private SunManager sunManager;

    public void initSession() {

    }

    public void updateSession() {

    }
}
