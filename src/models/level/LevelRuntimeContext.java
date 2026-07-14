package models.level;

import models.engine.Board;

public class LevelRuntimeContext {
    private final Board board;
    private final int currentTick;
    private final int currentSunAmount;
    private final int totalSunProduced;
    private final int totalZombiesKilled;
    private final int totalPlantsDestroyed;

    public LevelRuntimeContext(
            Board board,
            int currentTick,
            int currentSunAmount,
            int totalSunProduced,
            int totalZombiesKilled,
            int totalPlantsDestroyed
    ) {
        if (board == null) {
            throw new IllegalArgumentException("Board cannot be null.");
        }
        if (currentTick < 0 || currentSunAmount < 0 || totalSunProduced < 0
                || totalZombiesKilled < 0 || totalPlantsDestroyed < 0) {
            throw new IllegalArgumentException("Runtime counters cannot be negative.");
        }

        this.board = board;
        this.currentTick = currentTick;
        this.currentSunAmount = currentSunAmount;
        this.totalSunProduced = totalSunProduced;
        this.totalZombiesKilled = totalZombiesKilled;
        this.totalPlantsDestroyed = totalPlantsDestroyed;
    }

    public Board getBoard() {
        return board;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public int getCurrentSunAmount() {
        return currentSunAmount;
    }

    public int getTotalSunProduced() {
        return totalSunProduced;
    }

    public int getTotalZombiesKilled() {
        return totalZombiesKilled;
    }

    public int getTotalPlantsDestroyed() {
        return totalPlantsDestroyed;
    }
}
