package models.level.wave;

import models.core.zombie.Zombie;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WaveManager {
    private static final double ZOMBIE_SPAWN_X_OFFSET = 0.75;
    private static final int NEXT_WAVE_GAP_TICKS = 50;

    private final List<Wave> waves;
    private final Random random;
    private Board board;
    private AttackPattern attackPattern;
    private int nextWaveIndex;
    private int roundRobinLane;
    private int nextWaveThresholdReachedTick;

    public WaveManager(List<Wave> waves) {
        this(waves, null, AttackPattern.RANDOM_LANES, new Random());
    }

    public WaveManager(List<Wave> waves, Board board) {
        this(waves, board, AttackPattern.RANDOM_LANES, new Random());
    }

    public WaveManager(List<Wave> waves, Board board, AttackPattern attackPattern) {
        this(waves, board, attackPattern, new Random());
    }

    public WaveManager(
        List<Wave> waves,
        Board board,
        AttackPattern attackPattern,
        Random random
    ) {
        validateWaves(waves);
        if (attackPattern == null) {
            throw new IllegalArgumentException("Attack pattern cannot be null.");
        }
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null.");
        }

        this.waves = new ArrayList<>(waves);
        this.board = board;
        this.attackPattern = attackPattern;
        this.random = random;
        this.nextWaveIndex = 0;
        this.roundRobinLane = 1;
        this.nextWaveThresholdReachedTick = -1;
    }

    public void bindBoard(Board board) {
        if (board == null) {
            throw new IllegalArgumentException("Board cannot be null.");
        }
        if (this.board != null && this.board != board && nextWaveIndex > 0) {
            throw new IllegalStateException("Cannot change the board after waves have started.");
        }
        this.board = board;
    }

    public Wave updateTicks(int currentTick) {
        validateTick(currentTick);
        return spawnNextWave(currentTick);
    }

    public boolean canSpawnNextWave(int currentTick) {
        validateTick(currentTick);
        ensureBoardIsBound();

        if (areAllWavesSpawned()) {
            return false;
        }

        Wave nextWave = waves.get(nextWaveIndex);
        if (nextWaveIndex == 0) {
            return nextWave.isReadyToSpawn(currentTick);
        }

        Wave previousWave = waves.get(nextWaveIndex - 1);
        if (!previousWave.hasLostSeventyFivePercentHealth()) {
            nextWaveThresholdReachedTick = -1;
            return false;
        }
        if (nextWaveThresholdReachedTick < 0) {
            nextWaveThresholdReachedTick = currentTick;
        }

        boolean gapElapsed = currentTick - nextWaveThresholdReachedTick >= NEXT_WAVE_GAP_TICKS;
        return gapElapsed && nextWave.isReadyToSpawn(currentTick);
    }

    public Wave spawnNextWave(int currentTick) {
        if (!canSpawnNextWave(currentTick)) {
            return null;
        }

        Wave wave = waves.get(nextWaveIndex);

        for (Zombie zombie : wave.getZombiesList()) {
            int laneNumber = chooseLane();
            spawnZombie(zombie, laneNumber);
        }

        wave.markAsSpawned();
        nextWaveIndex++;
        nextWaveThresholdReachedTick = -1;
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

    public int getNextWaveIndex() {
        return nextWaveIndex;
    }

    public Board getBoard() {
        return board;
    }

    public AttackPattern getAttackPattern() {
        return attackPattern;
    }

    public void setAttackPattern(AttackPattern attackPattern) {
        if (attackPattern == null) {
            throw new IllegalArgumentException("Attack pattern cannot be null.");
        }
        this.attackPattern = attackPattern;
    }

    public List<Wave> getWaves() {
        return Collections.unmodifiableList(waves);
    }

    private void spawnZombie(Zombie zombie, int laneNumber) {
        if (zombie == null) {
            return;
        }

        double targetX = resolveSpawnX(zombie);
        double targetY = laneNumber;
        zombie.moveBy(targetX - zombie.getX(), targetY - zombie.getY());

        Position position = new Position(board.getWidth(), laneNumber);
        Tile tile = board.getTileAt(position);
        if (tile == null) {
            throw new IllegalStateException("Zombie spawn tile does not exist.");
        }
        tile.addZombie(zombie);
    }


    private double resolveSpawnX(Zombie zombie) {
        String name = zombie.getName();
        if ("fisherman".equalsIgnoreCase(name) || "king".equalsIgnoreCase(name)) {
            return board.getWidth();
        }
        return board.getWidth() + ZOMBIE_SPAWN_X_OFFSET;
    }

    private int chooseLane() {
        if (attackPattern == AttackPattern.ROUND_ROBIN) {
            int lane = roundRobinLane;
            roundRobinLane = roundRobinLane % board.getHeight() + 1;
            return lane;
        }
        if (attackPattern == AttackPattern.BALANCED_LANES) {
            return leastPopulatedLane();
        }
        return randomLane();
    }

    private int randomLane() {
        return random.nextInt(board.getHeight()) + 1;
    }

    private int leastPopulatedLane() {
        int selectedLane = 1;
        int minimumZombies = Integer.MAX_VALUE;

        for (Lane lane : board.getLanes()) {
            int zombieCount = lane.getActiveZombieCount();
            if (zombieCount < minimumZombies) {
                minimumZombies = zombieCount;
                selectedLane = lane.getLaneId();
            }
        }
        return selectedLane;
    }

    private void ensureBoardIsBound() {
        if (board == null) {
            throw new IllegalStateException("Wave manager is not bound to a board.");
        }
    }

    private void validateTick(int currentTick) {
        if (currentTick < 0) {
            throw new IllegalArgumentException("Current tick cannot be negative.");
        }
    }

    private void validateWaves(List<Wave> source) {
        if (source == null) {
            throw new IllegalArgumentException("Waves list cannot be null.");
        }
        for (Wave wave : source) {
            if (wave == null) {
                throw new IllegalArgumentException("Waves list cannot contain null.");
            }
        }
    }
}
