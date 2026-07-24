package models.level.core;

import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.board.TileType;
import models.level.rules.LevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.NoSpecialRule;
import models.level.rules.SpecialLevelType;
import models.level.wave.Wave;
import models.level.wave.WaveManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;


public class Level extends LevelState {
    public Level(
            int levelId,
            WaveManager waveManager,
            LevelType levelType,
            List<String> allowedPlants,
            List<String> allowedZombieNames,
            LevelRule levelRule
    ) {
        super(levelId, waveManager, levelType, allowedPlants, allowedZombieNames, levelRule);
    }

    public Level(
            int levelId,
            WaveManager waveManager,
            LevelType levelType,
            List<String> allowedPlants,
            List<String> allowedZombieNames,
            LevelRule levelRule,
            int initialSunAmount
    ) {
        super(levelId, waveManager, levelType, allowedPlants, allowedZombieNames, levelRule, initialSunAmount);
    }

    public void startLevel(Board board, LevelRuntimeContext context) {
        if (board == null || context == null) {
            throw new IllegalArgumentException("Board and runtime context cannot be null.");
        }
        if (status == LevelStatus.RUNNING) {
            return;
        }
        if (status == LevelStatus.WON || status == LevelStatus.LOST) {
            throw new IllegalStateException("A finished level cannot be started again.");
        }

        this.board = board;
        terrainSpawnedZombies.clear();
        appliedTerrainTicks.clear();
        appliedTerrainWaves.clear();
        applyTerrainLayout(board);
        applyTerrainChangesForTick(0);
        captureLowTidePositions();
        waveManager.bindBoard(board);
        levelRule.onLevelStart(context);
        status = LevelStatus.RUNNING;
        evaluate(context);
    }

    public void startLevel() {
        Board boundBoard = waveManager.getBoard();
        if (boundBoard == null) {
            throw new IllegalStateException("The level must be started with a board.");
        }

        startLevel(boundBoard, new LevelRuntimeContext(boundBoard, 0, 0, 0, 0, 0));
    }

    public Wave updateTicks(LevelRuntimeContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Runtime context cannot be null.");
        }
        if (status != LevelStatus.RUNNING) {
            return null;
        }

        applyTerrainChangesForTick(context.getCurrentTick());
        levelRule.onTick(context);
        evaluate(context);

        if (status != LevelStatus.RUNNING || !areZombieWavesStarted()) {
            return null;
        }

        Wave spawnedWave = waveManager.updateTicks(context.getCurrentTick());
        if (spawnedWave != null) {
            int waveNumber = spawnedWave.getWaveNumber();
            applyAutomaticTideForWave(waveNumber);
            // Explicit level configuration has priority over the automatic
            // seasonal tide for the same wave.
            applyTerrainChangesForWave(waveNumber);
            spawnNecromancyZombies(waveNumber);
        }
        return spawnedWave;
    }

    public void evaluate(LevelRuntimeContext context) {
        if (context == null || status != LevelStatus.RUNNING) {
            return;
        }

        if (context.getBoard().hasBrainBeenEaten()
                || levelRule.isLoseConditionMet(context)) {
            status = LevelStatus.LOST;
            return;
        }

        if (levelRule.usesCustomWinCondition()) {
            if (levelRule.isWinConditionMet(context)) {
                status = LevelStatus.WON;
            }
            return;
        }

        if (waveManager.areAllWavesCleared()) {
            status = LevelStatus.WON;
        }
    }

    public boolean checkWinCondition() {
        return status == LevelStatus.WON;
    }

    public boolean checkWinCondition(LevelRuntimeContext context) {
        evaluate(context);
        return checkWinCondition();
    }

    public boolean checkLoseCondition() {
        return status == LevelStatus.LOST;
    }

    public boolean checkLoseCondition(LevelRuntimeContext context) {
        evaluate(context);
        return checkLoseCondition();
    }

    public void applySpecialRules() {
        if (status == LevelStatus.NOT_STARTED) {
            throw new IllegalStateException("Level must be started before applying its rules.");
        }
    }

    public boolean isPlantAllowed(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return false;
        }

        boolean allowedByLevel = allowedPlants.isEmpty()
                || containsIgnoreCase(allowedPlants, plantName);
        return allowedByLevel && levelRule.isPlantAllowed(plantName);
    }

    public boolean isZombieAllowed(String zombieName) {
        return allowedZombieNames.isEmpty()
                || containsIgnoreCase(allowedZombieNames, zombieName);
    }

    public void onPlantUsed(String plantName) {
        levelRule.onPlantUsed(plantName);
    }

    public int resolveInitialSunAmount() {
        return levelRule.resolveInitialSunAmount(initialSunAmount);
    }

    public boolean allowsSkySun() {
        return levelRule.allowsSkySun();
    }

    public boolean ignoresPlantRecharge() {
        return levelRule.ignoresPlantRecharge();
    }

    public boolean usesConveyorBelt() {
        return levelRule.usesConveyorBelt();
    }

    public List<String> getConveyorPlants() {
        return levelRule.getConveyorPlants();
    }

    public boolean consumeConveyorPlant(String plantName) {
        return levelRule.consumeConveyorPlant(plantName);
    }

    public boolean areZombieWavesStarted() {
        if (levelRule.startsZombieWavesAutomatically()) {
            return true;
        }
        return levelRule.areZombieWavesStarted();
    }

    public boolean startZombieWaves() {
        return levelRule.startZombieWaves();
    }


    public void setTerrainTile(Position position, TileType tileType) {
        if (status != LevelStatus.NOT_STARTED) {
            throw new IllegalStateException("Terrain cannot be changed after the level starts.");
        }
        if (position == null || tileType == null) {
            throw new IllegalArgumentException("Terrain position and type cannot be null.");
        }
        terrainLayout.put(position, tileType);
    }

    public void setTerrainLayout(Map<Position, TileType> terrainLayout) {
        if (status != LevelStatus.NOT_STARTED) {
            throw new IllegalStateException("Terrain cannot be changed after the level starts.");
        }
        this.terrainLayout.clear();
        if (terrainLayout == null) {
            return;
        }
        for (Map.Entry<Position, TileType> entry : terrainLayout.entrySet()) {
            setTerrainTile(entry.getKey(), entry.getValue());
        }
    }

    public Map<Position, TileType> getTerrainLayout() {
        return Collections.unmodifiableMap(terrainLayout);
    }

    public void scheduleTerrainChangeAtTick(
            int tick,
            Position position,
            TileType tileType
    ) {
        ensureConfigurable();
        if (tick < 0 || position == null || tileType == null) {
            throw new IllegalArgumentException("Terrain tick, position and type are invalid.");
        }
        terrainChangesByTick
                .computeIfAbsent(tick, ignored -> new LinkedHashMap<>())
                .put(position, tileType);
    }

    public void scheduleTerrainChangeAtWave(
            int waveNumber,
            Position position,
            TileType tileType
    ) {
        ensureConfigurable();
        if (waveNumber <= 0 || position == null || tileType == null) {
            throw new IllegalArgumentException("Terrain wave, position and type are invalid.");
        }
        terrainChangesByWave
                .computeIfAbsent(waveNumber, ignored -> new LinkedHashMap<>())
                .put(position, tileType);
    }

    public void scheduleNecromancySpawn(
            int waveNumber,
            Position position,
            String zombieName
    ) {
        ensureConfigurable();
        if (waveNumber <= 0 || position == null
                || zombieName == null || zombieName.isBlank()) {
            throw new IllegalArgumentException("Necromancy spawn configuration is invalid.");
        }
        if (!isZombieAllowed(zombieName)) {
            throw new IllegalArgumentException("Zombie is not allowed in this level: " + zombieName);
        }
        necromancySpawnsByWave
                .computeIfAbsent(waveNumber, ignored -> new LinkedHashMap<>())
                .put(position, zombieName.trim());
    }

    public Map<Integer, Map<Position, TileType>> getTerrainChangesByTick() {
        return readOnlyNestedMap(terrainChangesByTick);
    }

    public Map<Integer, Map<Position, TileType>> getTerrainChangesByWave() {
        return readOnlyNestedMap(terrainChangesByWave);
    }

    public List<Zombie> drainTerrainSpawnedZombies() {
        List<Zombie> result = new ArrayList<>(terrainSpawnedZombies);
        terrainSpawnedZombies.clear();
        return Collections.unmodifiableList(result);
    }

    void bindSeasonType(SeasonType seasonType) {
        if (status != LevelStatus.NOT_STARTED) {
            throw new IllegalStateException("Season cannot be changed after the level starts.");
        }
        this.seasonType = seasonType;
    }

    public SeasonType getSeasonType() {
        return seasonType;
    }

    public int getLevelId() {
        return levelId;
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    public LevelType getLevelType() {
        return levelType;
    }

    public SpecialLevelType getSpecialLevelType() {
        return levelRule.getType();
    }

    public LevelRule getLevelRule() {
        return levelRule;
    }

    public List<String> getAllowedPlants() {
        return Collections.unmodifiableList(allowedPlants);
    }

    public List<String> getAllowedZombieNames() {
        return Collections.unmodifiableList(allowedZombieNames);
    }

    public LevelStatus getStatus() {
        return status;
    }

    public Board getBoard() {
        return board;
    }


    private void ensureConfigurable() {
        if (status != LevelStatus.NOT_STARTED) {
            throw new IllegalStateException("Level runtime configuration is already locked.");
        }
    }

    private void captureLowTidePositions() {
        lowTidePositions.clear();
        if (board == null) {
            return;
        }
        for (int y = 1; y <= board.getHeight(); y++) {
            for (int x = 1; x <= board.getWidth(); x++) {
                Position position = new Position(x, y);
                if (board.getTileAt(position).getTileType() == TileType.LOW_TIDE) {
                    lowTidePositions.add(position);
                }
            }
        }
    }

    private void applyTerrainChangesForTick(int tick) {
        if (!appliedTerrainTicks.add(tick)) {
            return;
        }
        applyTerrainChanges(terrainChangesByTick.get(tick));
    }

    private void applyTerrainChangesForWave(int waveNumber) {
        if (!appliedTerrainWaves.add(waveNumber)) {
            return;
        }
        applyTerrainChanges(terrainChangesByWave.get(waveNumber));
    }

    private void applyTerrainChanges(Map<Position, TileType> changes) {
        if (changes == null || board == null) {
            return;
        }
        for (Map.Entry<Position, TileType> entry : changes.entrySet()) {
            if (!board.setTileType(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException(
                        "Terrain position is outside the board: " + entry.getKey()
                );
            }
        }
    }

    private void applyAutomaticTideForWave(int waveNumber) {
        if (seasonType != SeasonType.BIG_WAVE_BEACH || lowTidePositions.isEmpty()) {
            return;
        }
        // The board starts in low tide. Even waves raise the water and odd waves
        // lower it again. Explicit scheduled changes are applied before this rule.
        TileType type = waveNumber % 2 == 0 ? TileType.WATER : TileType.LOW_TIDE;
        for (Position position : lowTidePositions) {
            board.setTileType(position, type);
        }
    }

    private void spawnNecromancyZombies(int waveNumber) {
        Map<Position, String> configured = necromancySpawnsByWave.get(waveNumber);
        if (configured != null) {
            for (Map.Entry<Position, String> entry : configured.entrySet()) {
                spawnNecromancyZombie(entry.getKey(), entry.getValue());
            }
            return;
        }

        if (seasonType != SeasonType.DARK_AGES || board == null) {
            return;
        }
        String zombieName = resolveDefaultNecromancyZombie();
        for (Position position : necromancyPositions()) {
            spawnNecromancyZombie(position, zombieName);
        }
    }

    private List<Position> necromancyPositions() {
        List<Position> positions = new ArrayList<>();
        if (board == null) {
            return positions;
        }
        for (int y = 1; y <= board.getHeight(); y++) {
            for (int x = 1; x <= board.getWidth(); x++) {
                Position position = new Position(x, y);
                if (board.getTileAt(position).getTileType() == TileType.NECROMANCY) {
                    positions.add(position);
                }
            }
        }
        return positions;
    }

    private void spawnNecromancyZombie(Position position, String zombieName) {
        if (board == null || position == null || zombieName == null) {
            return;
        }
        if (!board.isValidPosition(position)
                || board.getTileAt(position).getTileType() != TileType.NECROMANCY) {
            return;
        }
        try {
            Zombie zombie = zombieFactory.createZombie(
                    zombieName,
                    position.getX(),
                    position.getY()
            );
            board.getTileAt(position).addZombie(zombie);
            terrainSpawnedZombies.add(zombie);
        } catch (IllegalArgumentException ignored) {
            // Invalid external configuration is ignored at runtime; constructor
            // validation still protects regular wave zombies.
        }
    }

    private String resolveDefaultNecromancyZombie() {
        for (String name : allowedZombieNames) {
            String normalized = normalize(name);
            if (!normalized.contains("gargantuar") && !normalized.contains("king")) {
                try {
                    zombieFactory.createZombie(name, 1, 1);
                    return name;
                } catch (IllegalArgumentException ignored) {
                    // Try the next allowed type.
                }
            }
        }
        return "Default";
    }

}
