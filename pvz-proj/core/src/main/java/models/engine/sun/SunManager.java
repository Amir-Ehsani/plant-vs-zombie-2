package models.engine.sun;

import models.engine.board.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SunManager {
    private final List<Sun> suns;

    public SunManager() {
        this.suns = new ArrayList<>();
    }

    public Sun spawnSun(Position position, int sunAmount, int timeLeft) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null.");
        }
        if (sunAmount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        Sun sun = new Sun(position, sunAmount, timeLeft);
        suns.add(sun);
        return sun;
    }

    public Sun spawnSkySun(Position position, SunType type, int fallingTicks) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Sun type cannot be null.");
        }
        if (fallingTicks <= 0) {
            throw new IllegalArgumentException("Falling ticks must be positive.");
        }

        Sun sun = Sun.skySun(position, type, fallingTicks);
        suns.add(sun);
        return sun;
    }

    public Sun spawnGroundSun(Position position, int sunAmount, int groundLifetimeTicks) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null.");
        }
        if (sunAmount <= 0 || groundLifetimeTicks <= 0) {
            throw new IllegalArgumentException("Amount and lifetime must be positive.");
        }

        Sun sun = Sun.groundSun(position, sunAmount, groundLifetimeTicks);
        suns.add(sun);
        return sun;
    }

    public Sun spawnPermanentSun(Position position, int sunAmount) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null.");
        }
        if (sunAmount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        Sun sun = Sun.plantSun(position, sunAmount);
        suns.add(sun);
        return sun;
    }

    public Sun collectSunObject(Position position) {
        if (position == null) {
            return null;
        }

        Iterator<Sun> iterator = suns.iterator();
        while (iterator.hasNext()) {
            Sun sun = iterator.next();
            if (sun.getPosition().equals(position)) {
                iterator.remove();
                return sun;
            }
        }

        return null;
    }

    public int collectSun(Position position) {
        Sun sun = collectSunObject(position);
        return sun == null ? 0 : sun.getSunAmount();
    }

    public List<Sun> update() {
        List<Sun> landedSuns = new ArrayList<>();
        Iterator<Sun> iterator = suns.iterator();

        while (iterator.hasNext()) {
            Sun sun = iterator.next();
            if (sun.tick()) {
                landedSuns.add(sun);
            }
            if (sun.isExpired()) {
                iterator.remove();
            }
        }

        return Collections.unmodifiableList(landedSuns);
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

    public int stealLooseSuns() {
        int amount = 0;
        Iterator<Sun> iterator = suns.iterator();
        while (iterator.hasNext()) {
            Sun sun = iterator.next();
            if (!sun.isFalling()) {
                amount += sun.getSunAmount();
                iterator.remove();
            }
        }
        return amount;
    }

    public void clearSuns() {
        suns.clear();
    }
}
