package models.engine;

public class BoardTickResult {
    private final int zombiesKilled;
    private final int plantsDestroyed;
    private final int lawnMowersTriggered;
    private final boolean brainEaten;

    public BoardTickResult(
            int zombiesKilled,
            int plantsDestroyed,
            int lawnMowersTriggered,
            boolean brainEaten
    ) {
        if (zombiesKilled < 0 || plantsDestroyed < 0 || lawnMowersTriggered < 0) {
            throw new IllegalArgumentException("Tick counters cannot be negative.");
        }
        this.zombiesKilled = zombiesKilled;
        this.plantsDestroyed = plantsDestroyed;
        this.lawnMowersTriggered = lawnMowersTriggered;
        this.brainEaten = brainEaten;
    }

    public static BoardTickResult empty() {
        return new BoardTickResult(0, 0, 0, false);
    }

    public int getZombiesKilled() {
        return zombiesKilled;
    }

    public int getPlantsDestroyed() {
        return plantsDestroyed;
    }

    public int getLawnMowersTriggered() {
        return lawnMowersTriggered;
    }

    public boolean isBrainEaten() {
        return brainEaten;
    }
}
