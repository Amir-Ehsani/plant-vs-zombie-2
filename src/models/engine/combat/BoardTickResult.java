package models.engine.combat;

import models.engine.events.GameEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoardTickResult {
    private final int zombiesKilled;
    private final int plantsDestroyed;
    private final int lawnMowersTriggered;
    private final boolean brainEaten;
    private final List<GameEvent> events;

    public BoardTickResult(
            int zombiesKilled,
            int plantsDestroyed,
            int lawnMowersTriggered,
            boolean brainEaten
    ) {
        this(zombiesKilled, plantsDestroyed, lawnMowersTriggered, brainEaten,
                Collections.emptyList());
    }

    public BoardTickResult(
            int zombiesKilled,
            int plantsDestroyed,
            int lawnMowersTriggered,
            boolean brainEaten,
            List<GameEvent> events
    ) {
        if (zombiesKilled < 0 || plantsDestroyed < 0 || lawnMowersTriggered < 0) {
            throw new IllegalArgumentException("Tick counters cannot be negative.");
        }
        this.zombiesKilled = zombiesKilled;
        this.plantsDestroyed = plantsDestroyed;
        this.lawnMowersTriggered = lawnMowersTriggered;
        this.brainEaten = brainEaten;
        this.events = events == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(events));
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

    public List<GameEvent> getEvents() {
        return events;
    }
}
