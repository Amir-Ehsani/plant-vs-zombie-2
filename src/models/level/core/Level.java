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

public class Level {
    private static final int DEFAULT_INITIAL_SUN = 50;

    private final int levelId;
    private final WaveManager waveManager;
    private final LevelType levelType;
    private final List<String> allowedPlants;
    private final List<String> allowedZombieNames;
    private final LevelRule levelRule;
    private final int initialSunAmount;
    private final Map<Position, TileType> terrainLayout;
    private final Map<Integer, Map<Position, TileType>> terrainChangesByTick;
    private final Map<Integer, Map<Position, TileType>> terrainChangesByWave;
    private final Map<Integer, Map<Position, String>> necromancySpawnsByWave;
    private final List<Zombie> terrainSpawnedZombies;
    private final Set<Integer> appliedTerrainTicks;
    private final Set<Integer> appliedTerrainWaves;
    private final Set<Position> lowTidePositions;
    private final ZombieFactory zombieFactory;

    private LevelStatus status;
    private Board board;
    private SeasonType seasonType;

    public Level(
            int levelId,
            WaveManager waveManager,
            LevelType levelType,
            List<String> allowedPlants,
            List<String> allowedZombieNames,
            LevelRule levelRule
    ) {
        this(
                levelId,
                waveManager,
                levelType,
                allowedPlants,
                allowedZombieNames,
                levelRule,
                DEFAULT_INITIAL_SUN
        );
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
        if (levelId <= 0) {
            throw new IllegalArgumentException("Level id must be greater than 0.");
        }
        if (waveManager == null) {
            throw new IllegalArgumentException("Wave manager cannot be null.");
        }
        if (levelType == null) {
            throw new IllegalArgumentException("Level type cannot be null.");
        }
        if (initialSunAmount < 0) {
            throw new IllegalArgumentException("Initial sun amount cannot be negative.");
        }

        this.levelId = levelId;
        this.waveManager = waveManager;
        this.levelType = levelType;
        this.allowedPlants = copyNames(allowedPlants, "Allowed plants");
        this.allowedZombieNames = copyNames(allowedZombieNames, "Allowed zombies");
        this.levelRule = resolveRule(levelType, levelRule);
        this.initialSunAmount = initialSunAmount;
        this.terrainLayout = new LinkedHashMap<>();
        this.terrainChangesByTick = new LinkedHashMap<>();
        this.terrainChangesByWave = new LinkedHashMap<>();
        this.necromancySpawnsByWave = new LinkedHashMap<>();
        this.terrainSpawnedZombies = new ArrayList<>();
        this.appliedTerrainTicks = new LinkedHashSet<>();
        this.appliedTerrainWaves = new LinkedHashSet<>();
        this.lowTidePositions = new LinkedHashSet<>();
        this.zombieFactory = new ZombieFactory();
        this.status = LevelStatus.NOT_STARTED;
        this.board = null;
        this.seasonType = null;

        validateWaveZombies();
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

                }
            }
        }
        return "Default";
    }

    private Map<Integer, Map<Position, TileType>> readOnlyNestedMap(
            Map<Integer, Map<Position, TileType>> source
    ) {
        Map<Integer, Map<Position, TileType>> copy = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<Position, TileType>> entry : source.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(
                    new LinkedHashMap<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    private void applyTerrainLayout(Board board) {
        for (Map.Entry<Position, TileType> entry : terrainLayout.entrySet()) {
            if (!board.setTileType(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException(
                        "Terrain position is outside the board: " + entry.getKey()
                );
            }
        }
    }

    private LevelRule resolveRule(LevelType type, LevelRule rule) {
        if (type == LevelType.SPECIAL && rule == null) {
            throw new IllegalArgumentException("A special level requires a level rule.");
        }
        return rule == null ? new NoSpecialRule() : rule;
    }

    private List<String> copyNames(List<String> source, String listName) {
        List<String> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }

        for (String value : source) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(listName + " cannot contain empty values.");
            }
            if (!containsIgnoreCase(copy, value)) {
                copy.add(value.trim());
            }
        }
        return copy;
    }

    private void validateWaveZombies() {
        if (allowedZombieNames.isEmpty()) {
            return;
        }

        for (Wave wave : waveManager.getWaves()) {
            for (Zombie zombie : wave.getZombiesList()) {
                if (zombie != null && !isZombieAllowed(zombie.getName())) {
                    throw new IllegalArgumentException(
                            "Zombie " + zombie.getName() + " is not allowed in level " + levelId + "."
                    );
                }
            }
        }
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        String normalizedTarget = normalize(target);
        for (String value : values) {
            if (normalize(value).equals(normalizedTarget)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}
