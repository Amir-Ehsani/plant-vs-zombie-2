package models.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SunManager {
    private static final int PERMANENT_SUN_TIME = Integer.MAX_VALUE;

    private final List<Sun> suns;

    public SunManager() {
        this.suns = new ArrayList<>();
    }

    public void spawnSun(Position position, int sunAmount, int timeLeft) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null.");
        }
        if (sunAmount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        Sun sun = new Sun(position, sunAmount, timeLeft);
        suns.add(sun);
    }

    public void spawnPermanentSun(Position position, int sunAmount) {
        spawnSun(position, sunAmount, PERMANENT_SUN_TIME);
    }

    public int collectSun(Position position) {
        if (position == null) {
            return 0;
        }

        Iterator<Sun> iterator = suns.iterator();
        while (iterator.hasNext()) {
            Sun sun = iterator.next();

            if (sun.getPosition().equals(position)) {
                int collectedAmount = sun.getSunAmount();
                iterator.remove();
                return collectedAmount;
            }
        }

        return 0;
    }

    public void update() {
        Iterator<Sun> iterator = suns.iterator();

        while (iterator.hasNext()) {
            Sun sun = iterator.next();
            sun.tick();

            if (sun.isExpired()) {
                iterator.remove();
            }
        }
    }

    public List<Sun> getSuns() {
        return Collections.unmodifiableList(suns);
    }

    public boolean hasSunAt(Position position) {
        if (position == null) {
            return false;
        }

        for (Sun sun : suns) {
            if (sun.getPosition().equals(position)) {
                return true;
            }
        }

        return false;
    }

    public void clearSuns() {
        suns.clear();
    }
}