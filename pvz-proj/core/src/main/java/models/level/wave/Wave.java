package models.level.wave;


import models.core.zombie.Zombie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class Wave {
    private static final double NEXT_WAVE_HEALTH_THRESHOLD = 0.25;

    private final int waveNumber;
    private final int delay;
    private final List<Zombie> zombiesList;
    private final int initialTotalHealth;

    private boolean spawned;


    public Wave(int waveNumber, int delay, List<Zombie> zombiesList) {
        if (waveNumber <= 0) {
            throw new IllegalArgumentException("Wave number must be greater than 0.");
        }
        if (delay < 0) {
            throw new IllegalArgumentException("Wave delay cannot be negative.");
        }
        if (zombiesList == null) {
            throw new IllegalArgumentException("Zombie list cannot be null.");
        }

        this.waveNumber = waveNumber;
        this.delay = delay;
        this.zombiesList = new ArrayList<>(zombiesList);
        this.initialTotalHealth = calculateInitialTotalHealth();

        this.spawned = false;
    }

    private int calculateInitialTotalHealth() {
        int totalHealth = 0;

        for (Zombie zombie : zombiesList) {
            if (zombie != null) {
                totalHealth += zombie.getMaxHp();
            }
        }
        return totalHealth;
    }

    public boolean isReadyToSpawn(int currentTick) {
        return !spawned && currentTick >= delay;
    }

    public void markAsSpawned() {
        spawned = true;
    }

    public boolean isSpawned() {
        return spawned;
    }

    public boolean hasLostSeventyFivePercentHealth() {
        return hasLostSeventyFivePercentHealth(zombie -> true);
    }

    public boolean hasLostSeventyFivePercentHealth(Predicate<Zombie> countsAsHostile) {
        if (!spawned || initialTotalHealth == 0) {
            return false;
        }

        Predicate<Zombie> predicate = countsAsHostile == null ? zombie -> true : countsAsHostile;
        int totalHealth = 0;
        for (Zombie zombie : zombiesList) {
            if (zombie != null && zombie.isAlive() && predicate.test(zombie)) {
                totalHealth += zombie.getHp();
            }
        }

        return (double) totalHealth / initialTotalHealth
                <= NEXT_WAVE_HEALTH_THRESHOLD;
    }

    public boolean isCleared() {
        return isCleared(zombie -> true);
    }

    public boolean isCleared(Predicate<Zombie> countsAsHostile) {
        if (!spawned) {
            return false;
        }

        Predicate<Zombie> predicate = countsAsHostile == null ? zombie -> true : countsAsHostile;
        for (Zombie zombie : zombiesList) {
            if (zombie != null && zombie.isAlive() && predicate.test(zombie)) {
                return false;
            }
        }

        return true;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public int getDelay() {
        return delay;
    }

    public int getInitialTotalHealth() {
        return initialTotalHealth;
    }

    public int getTotalWaveCost() {
        int totalCost = 0;
        for (Zombie zombie : zombiesList) {
            if (zombie != null && zombie.getType() != null) {
                totalCost += zombie.getType().getWaveCost();
            }
        }
        return totalCost;
    }

    public List<Zombie> getZombiesList() {
        return Collections.unmodifiableList(zombiesList);
    }


}
