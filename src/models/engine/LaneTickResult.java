package models.engine;

public class LaneTickResult {
    private final int zombiesKilled;
    private final int plantsDestroyed;
    private final boolean lawnMowerTriggered;
    private final boolean brainEaten;

    public LaneTickResult(
            int zombiesKilled,
            int plantsDestroyed,
            boolean lawnMowerTriggered,
            boolean brainEaten
    ) {
        if (zombiesKilled < 0 || plantsDestroyed < 0) {
            throw new IllegalArgumentException("Tick counters cannot be negative.");
        }
        this.zombiesKilled = zombiesKilled;
        this.plantsDestroyed = plantsDestroyed;
        this.lawnMowerTriggered = lawnMowerTriggered;
        this.brainEaten = brainEaten;
    }

    public static LaneTickResult empty() {
        return new LaneTickResult(0, 0, false, false);
    }

    public int getZombiesKilled() {
        return zombiesKilled;
    }

    public int getPlantsDestroyed() {
        return plantsDestroyed;
    }

    public boolean isLawnMowerTriggered() {
        return lawnMowerTriggered;
    }

    public boolean isBrainEaten() {
        return brainEaten;
    }
}
