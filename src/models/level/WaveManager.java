package models.level;

import models.core.zombie.Zombie;
import models.engine.Board;
import models.engine.Position;
import models.engine.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WaveManager {
    private final List<Wave> waves;
    private final Board board;
    private final Random random;
    private int nextWaveIndex;

    public WaveManager(List<Wave> waves, Board board) {
        if (waves == null) {
            throw new IllegalArgumentException("Waves list cannot be null.");
        }
        if (board == null) {
            throw new IllegalArgumentException("Board cannot be null.");
        }

        for (Wave wave : waves) {
            if (wave == null) {
                throw new IllegalArgumentException("Waves list cannot contain null.");
            }
        }

        this.waves = new ArrayList<>(waves);
        this.board = board;
        this.random = new Random();
        this.nextWaveIndex = 0;
    }

    public Wave updateTicks(int currentTick) {
        if (currentTick < 0) {
            throw new IllegalArgumentException("Current tick cannot be negative.");
        }

        return spawnNextWave(currentTick);
    }

    public boolean canSpawnNextWave(int currentTick) {
        if (currentTick < 0) {
            throw new IllegalArgumentException("Current tick cannot be negative.");
        }

        if (areAllWavesSpawned()) {
            return false;
        }

        Wave nextWave = waves.get(nextWaveIndex);

        if (!nextWave.isReadyToSpawn(currentTick)) {
            return false;
        }

        if (nextWaveIndex == 0) {
            return true;
        }

        Wave previousWave = waves.get(nextWaveIndex - 1);
        return previousWave.hasLostSeventyFivePercentHealth();
    }

    public Wave spawnNextWave(int currentTick) {
        if (!canSpawnNextWave(currentTick)) {
            return null;
        }

        Wave wave = waves.get(nextWaveIndex);

        for (Zombie zombie : wave.getZombiesList()) {
            if (zombie == null) {
                continue;
            }

            int randomLane = random.nextInt(board.getHeight()) + 1;
            Position position = new Position(board.getWidth(), randomLane);
            Tile tile = board.getTileAt(position);

            if (tile == null) {
                throw new IllegalStateException("Zombie spawn tile does not exist.");
            }

            tile.addZombie(zombie);
        }

        wave.markAsSpawned();
        nextWaveIndex++;

        return wave;
    }

    public Wave getCurrentWave() {
        if (nextWaveIndex == 0) {
            return null;
        }

        return waves.get(nextWaveIndex - 1);
    }

    public Wave getNextWave() {
        if (areAllWavesSpawned()) {
            return null;
        }

        return waves.get(nextWaveIndex);
    }

    public boolean areAllWavesSpawned() {
        return nextWaveIndex >= waves.size();
    }

    public boolean areAllWavesCleared() {
        if (!areAllWavesSpawned()) {
            return false;
        }

        for (Wave wave : waves) {
            if (!wave.isCleared()) {
                return false;
            }
        }

        return true;
    }

    public int getCurrentWaveNumber() {
        Wave currentWave = getCurrentWave();
        return currentWave == null ? 0 : currentWave.getWaveNumber();
    }

    public int getTotalWaves() {
        return waves.size();
    }

    public List<Wave> getWaves() {
        return Collections.unmodifiableList(waves);
    }
}