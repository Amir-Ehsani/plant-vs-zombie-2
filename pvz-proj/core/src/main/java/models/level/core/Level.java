package models.level.core;

import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.core.plant.Plant;
import models.engine.events.GameEvent;
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
        board.setGraveSpawningAllowed(
                seasonType == SeasonType.ANCIENT_EGYPT || seasonType == SeasonType.DARK_AGES
        );
        terrainSpawnedZombies.clear();
        chapterEvents.clear();
        activeNecromancyGravePositions.clear();
        appliedTerrainTicks.clear();
        appliedTerrainWaves.clear();
        applyTerrainLayout(board);
        applyTerrainChangesForTick(0);
        captureLowTidePositions();
        waveManager.bindBoard(board);
        spawnInitialTerrainZombies();
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
            applySeasonalZombieTraits(spawnedWave);
            applyChapterFeaturesForWave(waveNumber, spawnedWave);
            applyTerrainChangesForWave(waveNumber);
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

        if (waveManager.areAllWavesCleared() && !hasLivingTerrainZombie()) {
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
        return seasonType != SeasonType.DARK_AGES && levelRule.allowsSkySun();
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

    public void scheduleInitialTerrainZombie(Position position, String zombieName) {
        ensureConfigurable();
        if (position == null || zombieName == null || zombieName.isBlank()) {
            throw new IllegalArgumentException("Initial terrain zombie configuration is invalid.");
        }
        initialTerrainZombieSpawns.put(position, zombieName.trim());
    }

    public void setHighTideWaterColumns(int columns) {
        ensureConfigurable();
        highTideWaterColumns = Math.max(1, Math.min(4, columns));
    }

    public Map<Integer, Map<Position, TileType>> getTerrainChangesByTick() {
        return readOnlyNestedMap(terrainChangesByTick);
    }

    public Map<Integer, Map<Position, TileType>> getTerrainChangesByWave() {
        return readOnlyNestedMap(terrainChangesByWave);
    }

    public Set<Position> getActiveNecromancyGravePositions() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(activeNecromancyGravePositions));
    }

    public Set<Position> getLowTidePositions() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(lowTidePositions));
    }

    public List<Zombie> drainTerrainSpawnedZombies() {
        List<Zombie> result = new ArrayList<>(terrainSpawnedZombies);
        terrainSpawnedZombies.clear();
        return Collections.unmodifiableList(result);
    }

    public List<GameEvent> drainChapterEvents() {
        List<GameEvent> result = new ArrayList<>(chapterEvents);
        chapterEvents.clear();
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

    private void applyChapterFeaturesForWave(int waveNumber, Wave wave) {
        if (seasonType == SeasonType.ANCIENT_EGYPT
                && waveNumber == waveManager.getTotalWaves()) {
            applyFinalWaveSandstorm(wave);
        } else if (seasonType == SeasonType.FROSTBITE_CAVES) {
            applyFrostWind(waveNumber);
        } else if (seasonType == SeasonType.BIG_WAVE_BEACH) {
            applyAutomaticTideForWave(waveNumber);
        } else if (seasonType == SeasonType.DARK_AGES) {
            growDarkAgesGraves(waveNumber);
        }
        spawnNecromancyZombies(waveNumber);
    }

    private void applySeasonalZombieTraits(Wave wave) {
        if (seasonType != SeasonType.FROSTBITE_CAVES || wave == null) {
            return;
        }
        for (Zombie zombie : wave.getZombiesList()) {
            if (zombie != null) {
                zombie.setSeasonalIceImmune(true);
            }
        }
    }

    private void applyFinalWaveSandstorm(Wave wave) {
        if (wave == null) {
            return;
        }
        for (Zombie zombie : wave.getZombiesList()) {
            if (zombie == null || !zombie.isAlive()) {
                continue;
            }
            int lane = Math.max(1, Math.min(board.getHeight(), (int) Math.round(zombie.getY())));
            int minimumColumn = Math.max(board.getWidth() - 4, rightmostPlantColumn(lane) + 1);
            int maximumColumn = board.getWidth();
            if (minimumColumn > maximumColumn) {
                minimumColumn = maximumColumn;
            }
            int targetColumn = minimumColumn + chapterRandom.nextInt(maximumColumn - minimumColumn + 1);
            double targetX = targetColumn - 0.15 + chapterRandom.nextDouble() * 0.30;
            Tile sourceTile = board.getTileContainingZombie(zombie);
            zombie.moveBy(targetX - zombie.getX(), lane - zombie.getY());
            int tileX = Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(zombie.getX())));
            Tile targetTile = board.getTileAt(new Position(tileX, lane));
            if (sourceTile != null && targetTile != null && sourceTile != targetTile) {
                sourceTile.removeZombie(zombie);
                targetTile.addZombie(zombie);
            }
            chapterEvents.add(GameEvent.chapterEffect(
                    "A sandstorm carried " + zombie.getName() + " to column " + targetColumn
                            + " in lane " + lane + "."
            ));
        }
    }

    private int rightmostPlantColumn(int laneNumber) {
        int rightmost = 0;
        for (Plant plant : board.getAllPlants()) {
            if (plant != null && plant.isAlive() && (int) Math.round(plant.getY()) == laneNumber) {
                rightmost = Math.max(rightmost, (int) Math.ceil(plant.getX()));
            }
        }
        return rightmost;
    }

    private void applyFrostWind(int waveNumber) {
        int firstLane = chapterRandom.nextInt(board.getHeight()) + 1;
        int secondLane = chapterRandom.nextInt(board.getHeight()) + 1;
        freezePlantsInLane(firstLane);
        if (waveNumber == waveManager.getTotalWaves() && secondLane != firstLane) {
            freezePlantsInLane(secondLane);
        }
        String lanes = secondLane == firstLane || waveNumber != waveManager.getTotalWaves()
                ? Integer.toString(firstLane) : firstLane + " and " + secondLane;
        chapterEvents.add(GameEvent.chapterEffect(
                "An icy wind increased the freeze level of plants in lane(s) " + lanes + "."
        ));
    }

    private void freezePlantsInLane(int laneNumber) {
        for (Plant plant : board.getAllPlants()) {
            if (plant != null && plant.isAlive()
                    && (int) Math.round(plant.getY()) == laneNumber
                    && !normalize(plant.getType().getTags()).contains("fire")) {
                plant.addIceHit();
            }
        }
    }

    private void applyAutomaticTideForWave(int waveNumber) {
        if (lowTidePositions.isEmpty()) {
            return;
        }
        boolean highTide = waveNumber % 2 == 1;
        int firstFloodedColumn = board.getWidth() - 1 - highTideWaterColumns;
        for (Position position : lowTidePositions) {
            TileType type = highTide && position.getX() >= firstFloodedColumn
                    ? TileType.WATER : TileType.LOW_TIDE;
            board.setTileType(position, type);
        }
        chapterEvents.add(GameEvent.chapterEffect(highTide
                ? "The tide rose and flooded the marked beach columns."
                : "The tide receded and exposed the low-tide tiles."));
        if (highTide) {
            spawnLowBeachZombie();
        }
    }

    private void spawnLowBeachZombie() {
        List<Position> flooded = new ArrayList<>();
        for (Position position : lowTidePositions) {
            Tile tile = board.getTileAt(position);
            if (tile != null && tile.getTileType() == TileType.WATER && !tile.hasZombies()) {
                flooded.add(position);
            }
        }
        if (flooded.isEmpty() || chapterRandom.nextInt(100) >= 35) {
            return;
        }
        Position position = flooded.get(chapterRandom.nextInt(flooded.size()));
        spawnTerrainZombie(position, resolveDefaultTerrainZombie());
        chapterEvents.add(GameEvent.chapterEffect(
                "A zombie emerged from the flooded low beach at " + position + "."
        ));
    }

    private void growDarkAgesGraves(int waveNumber) {
        List<Tile> candidates = new ArrayList<>();
        for (int y = 1; y <= board.getHeight(); y++) {
            for (int x = 2; x < board.getWidth(); x++) {
                Tile tile = board.getTileAt(new Position(x, y));
                if (tile != null && tile.getTileType() == TileType.NORMAL
                        && !tile.hasPlant() && !tile.hasZombies()) {
                    candidates.add(tile);
                }
            }
        }
        Collections.shuffle(candidates, chapterRandom);
        int count = Math.min(candidates.size(), 2 + chapterRandom.nextInt(2));
        for (int index = 0; index < count; index++) {
            candidates.get(index).setTileType(randomDarkAgesGraveType());
        }
        rebuildActiveNecromancyGraves();
        if (count > 0) {
            chapterEvents.add(GameEvent.chapterEffect(
                    count + " new grave(s) rose from the dark ground."
            ));
        }
    }

    private TileType randomDarkAgesGraveType() {
        int roll = chapterRandom.nextInt(10);
        if (roll == 0) {
            return TileType.PLANT_FOOD_GRAVE;
        }
        if (roll <= 2) {
            return TileType.SUN_GRAVE;
        }
        return TileType.GRAVE;
    }

    private void spawnNecromancyZombies(int waveNumber) {
        Map<Position, String> configured = necromancySpawnsByWave.get(waveNumber);
        if (configured != null) {
            for (Map.Entry<Position, String> entry : configured.entrySet()) {
                spawnTerrainZombie(entry.getKey(), entry.getValue());
            }
            return;
        }

        if (seasonType != SeasonType.DARK_AGES || board == null) {
            return;
        }
        if (activeNecromancyGravePositions.isEmpty()) {
            return;
        }
        String zombieName = resolveDefaultTerrainZombie();
        int spawned = 0;
        for (Position position : new ArrayList<>(activeNecromancyGravePositions)) {
            Tile tile = board.getTileAt(position);
            if (tile == null || tile.getTileType() != TileType.GRAVE) {
                activeNecromancyGravePositions.remove(position);
                continue;
            }
            spawnTerrainZombie(position, zombieName);
            spawned++;
        }
        if (spawned > 0) {
            chapterEvents.add(GameEvent.chapterEffect(
                    "Necromancy raised zombies from the cursed graves."
            ));
        }
    }

    private void rebuildActiveNecromancyGraves() {
        activeNecromancyGravePositions.clear();
        if (board == null || seasonType != SeasonType.DARK_AGES) {
            return;
        }
        for (int y = 1; y <= board.getHeight(); y++) {
            for (int x = 2; x < board.getWidth(); x++) {
                Position position = new Position(x, y);
                Tile tile = board.getTileAt(position);
                if (tile != null && tile.getTileType() == TileType.GRAVE && chapterRandom.nextInt(10) == 0) {
                    activeNecromancyGravePositions.add(position);
                }
            }
        }
    }

    private void spawnInitialTerrainZombies() {
        for (Map.Entry<Position, String> entry : initialTerrainZombieSpawns.entrySet()) {
            Zombie zombie = spawnTerrainZombie(entry.getKey(), entry.getValue());
            if (zombie != null && seasonType == SeasonType.FROSTBITE_CAVES) {
                board.freezeInitialZombie(zombie);
            }
        }
        if (!initialTerrainZombieSpawns.isEmpty()) {
            chapterEvents.add(GameEvent.chapterEffect(
                    "Frozen zombies are trapped inside the ice blocks."
            ));
        }
    }

    private Zombie spawnTerrainZombie(Position position, String zombieName) {
        if (board == null || position == null || zombieName == null) {
            return null;
        }
        if (!board.isValidPosition(position)) {
            return null;
        }
        try {
            Zombie zombie = zombieFactory.createZombie(
                    zombieName,
                    position.getX(),
                    position.getY()
            );
            if (seasonType == SeasonType.FROSTBITE_CAVES) {
                zombie.setSeasonalIceImmune(true);
            }
            board.getTileAt(position).addZombie(zombie);
            terrainSpawnedZombies.add(zombie);
            return zombie;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String resolveDefaultTerrainZombie() {
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

    private boolean hasLivingTerrainZombie() {
        if (board == null) {
            return false;
        }
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie != null && zombie.isAlive()) {
                return true;
            }
        }
        return false;
    }

}
