package models.engine.combat;

import models.engine.events.GameEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LaneTickResult {
    private final int zombiesKilled;
    private final int plantsDestroyed;
    private final boolean lawnMowerTriggered;
    private final boolean brainEaten;
    private final List<GameEvent> events;

    public LaneTickResult(
            int zombiesKilled,
            int plantsDestroyed,
            boolean lawnMowerTriggered,
            boolean brainEaten
    ) {
        this(zombiesKilled, plantsDestroyed, lawnMowerTriggered, brainEaten,
                Collections.emptyList());
    }

    public LaneTickResult(
            int zombiesKilled,
            int plantsDestroyed,
            boolean lawnMowerTriggered,
            boolean brainEaten,
            List<GameEvent> events
    ) {
        if (zombiesKilled < 0 || plantsDestroyed < 0) {
            throw new IllegalArgumentException("Tick counters cannot be negative.");
        }
        this.zombiesKilled = zombiesKilled;
        this.plantsDestroyed = plantsDestroyed;
        this.lawnMowerTriggered = lawnMowerTriggered;
        this.brainEaten = brainEaten;
        this.events = events == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(events));
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

    public List<GameEvent> getEvents() {
        return events;
    }
}
