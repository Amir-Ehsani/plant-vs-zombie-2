package models.level.wave;

import models.core.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;

final class WaveSpawnSchedule {
    private final List<Zombie> zombies;
    private final int intervalTicks;
    private int nextIndex;
    private int nextSpawnTick;

    WaveSpawnSchedule(Wave wave, int startTick, int intervalTicks) {
        if (wave == null) {
            throw new IllegalArgumentException("Wave cannot be null.");
        }
        if (startTick < 0) {
            throw new IllegalArgumentException("Start tick cannot be negative.");
        }
        if (intervalTicks <= 0) {
            throw new IllegalArgumentException("Spawn interval must be positive.");
        }
        this.zombies = new ArrayList<>(wave.getZombiesList());
        this.intervalTicks = intervalTicks;
        this.nextIndex = 0;
        this.nextSpawnTick = startTick;
    }

    Zombie pollDueZombie(int currentTick) {
        if (!hasPendingZombies() || currentTick < nextSpawnTick) {
            return null;
        }
        Zombie zombie = zombies.get(nextIndex);
        nextIndex++;
        nextSpawnTick += intervalTicks;
        return zombie;
    }

    boolean hasPendingZombies() {
        return nextIndex < zombies.size();
    }
}
