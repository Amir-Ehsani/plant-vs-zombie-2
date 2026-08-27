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
    private static final double MIN_ZOMBIE_SPAWN_X_OFFSET = 4.0;
    private static final double ZOMBIE_SPAWN_X_SPREAD = 0.12;
    private static final double SPAWN_TRAILING_X_STEP = 0.10;
    private static final int MAX_TRAILING_STEPS = 6;
    private static final int ZOMBIE_SPAWN_INTERVAL_TICKS = 10;
    private static final int NEXT_WAVE_GAP_TICKS = 50;

    private final List<Wave> waves;
    private final Random random;
    private Board board;
    private AttackPattern attackPattern;
    private int nextWaveIndex;
    private int roundRobinLane;
    private int nextWaveThresholdReachedTick;
    private WaveSpawnSchedule activeSpawnSchedule;

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
        this.activeSpawnSchedule = null;
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
        spawnDueZombie(currentTick);
        return spawnNextWave(currentTick);
    }

    public boolean canSpawnNextWave(int currentTick) {
        validateTick(currentTick);
        ensureBoardIsBound();

        if (activeSpawnSchedule != null || areAllWavesSpawned()) {
            return false;
        }

        Wave nextWave = waves.get(nextWaveIndex);
        if (nextWaveIndex == 0) {
            return nextWave.isReadyToSpawn(currentTick);
        }

        Wave previousWave = waves.get(nextWaveIndex - 1);
        if (!previousWave.hasLostSeventyFivePercentHealth(this::countsAsHostileZombie)) {
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
        prepareWavePositions(wave);
        wave.markAsSpawned();
        nextWaveIndex++;
        nextWaveThresholdReachedTick = -1;
        activeSpawnSchedule = new WaveSpawnSchedule(
            wave,
            currentTick,
            ZOMBIE_SPAWN_INTERVAL_TICKS
        );
        spawnDueZombie(currentTick);
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
            if (!wave.isCleared(this::countsAsHostileZombie)) {
                return false;
            }
        }
        return true;
    }

    private boolean countsAsHostileZombie(Zombie zombie) {
        return zombie != null && (board == null || !board.isHypnotized(zombie));
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

    public int getTicksUntilNextWave(int currentTick) {
        validateTick(currentTick);
        if (activeSpawnSchedule != null || areAllWavesSpawned()) {
            return -1;
        }
        Wave nextWave = waves.get(nextWaveIndex);
        if (nextWaveIndex == 0) {
            return Math.max(0, nextWave.getDelay() - currentTick);
        }
        Wave previousWave = waves.get(nextWaveIndex - 1);
        if (!previousWave.hasLostSeventyFivePercentHealth()) {
            return -1;
        }
        int thresholdTick = nextWaveThresholdReachedTick < 0
                ? currentTick
                : nextWaveThresholdReachedTick;
        int gapRemaining = Math.max(0, NEXT_WAVE_GAP_TICKS - (currentTick - thresholdTick));
        int delayRemaining = Math.max(0, nextWave.getDelay() - currentTick);
        return Math.max(gapRemaining, delayRemaining);
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

    private void prepareWavePositions(Wave wave) {
        int spawnIndex = 0;
        for (Zombie zombie : wave.getZombiesList()) {
            if (zombie == null) {
                continue;
            }
            int laneNumber = chooseLane();
            double spawnX = resolveSpawnX(zombie, spawnIndex);
            zombie.moveBy(spawnX - zombie.getX(), laneNumber - zombie.getY());
            spawnIndex++;
        }
    }

    private void spawnDueZombie(int currentTick) {
        if (activeSpawnSchedule == null) {
            return;
        }
        Zombie zombie = activeSpawnSchedule.pollDueZombie(currentTick);
        if (zombie != null) {
            addPreparedZombieToBoard(zombie);
        }
        if (!activeSpawnSchedule.hasPendingZombies()) {
            activeSpawnSchedule = null;
        }
    }

    private void addPreparedZombieToBoard(Zombie zombie) {
        int laneNumber = Math.max(1, Math.min(board.getHeight(), (int) Math.round(zombie.getY())));
        int tileX = Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(zombie.getX())));
        Tile tile = board.getTileAt(new Position(tileX, laneNumber));
        if (tile == null) {
            throw new IllegalStateException("Zombie spawn tile does not exist.");
        }
        tile.addZombie(zombie);
    }

    private double resolveSpawnX(Zombie zombie, int spawnIndex) {
        String name = zombie.getName();
        if ("fisherman".equalsIgnoreCase(name) || "king".equalsIgnoreCase(name)) {
            return board.getWidth();
        }
        int trailingSteps = Math.min(spawnIndex, MAX_TRAILING_STEPS);
        return board.getWidth()
            + MIN_ZOMBIE_SPAWN_X_OFFSET
            + random.nextDouble() * ZOMBIE_SPAWN_X_SPREAD
            + trailingSteps * SPAWN_TRAILING_X_STEP;
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
