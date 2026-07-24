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


abstract class LevelState {
    protected static final int DEFAULT_INITIAL_SUN = 50;

    protected final int levelId;
    protected final WaveManager waveManager;
    protected final LevelType levelType;
    protected final List<String> allowedPlants;
    protected final List<String> allowedZombieNames;
    protected final LevelRule levelRule;
    protected final int initialSunAmount;
    protected final Map<Position, TileType> terrainLayout;
    protected final Map<Integer, Map<Position, TileType>> terrainChangesByTick;
    protected final Map<Integer, Map<Position, TileType>> terrainChangesByWave;
    protected final Map<Integer, Map<Position, String>> necromancySpawnsByWave;
    protected final List<Zombie> terrainSpawnedZombies;
    protected final Set<Integer> appliedTerrainTicks;
    protected final Set<Integer> appliedTerrainWaves;
    protected final Set<Position> lowTidePositions;
    protected final ZombieFactory zombieFactory;

    protected LevelStatus status;
    protected Board board;
    protected SeasonType seasonType;

    protected LevelState(
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

    protected LevelState(
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

    protected Map<Integer, Map<Position, TileType>> readOnlyNestedMap(
            Map<Integer, Map<Position, TileType>> source
    ) {
        Map<Integer, Map<Position, TileType>> copy = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<Position, TileType>> entry : source.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(
                    new LinkedHashMap<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    protected void applyTerrainLayout(Board board) {
        for (Map.Entry<Position, TileType> entry : terrainLayout.entrySet()) {
            if (!board.setTileType(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException(
                        "Terrain position is outside the board: " + entry.getKey()
                );
            }
        }
    }

    protected LevelRule resolveRule(LevelType type, LevelRule rule) {
        if (type == LevelType.SPECIAL && rule == null) {
            throw new IllegalArgumentException("A special level requires a level rule.");
        }
        return rule == null ? new NoSpecialRule() : rule;
    }

    protected List<String> copyNames(List<String> source, String listName) {
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

    protected void validateWaveZombies() {
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

    protected boolean containsIgnoreCase(List<String> values, String target) {
        String normalizedTarget = normalize(target);
        for (String value : values) {
            if (normalize(value).equals(normalizedTarget)) {
                return true;
            }
        }
        return false;
    }

    protected String normalize(String value) {
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

    public abstract boolean isZombieAllowed(String zombieName);
}
