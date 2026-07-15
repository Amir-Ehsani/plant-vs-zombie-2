package models.level.core;

import models.core.zombie.Zombie;
import models.engine.board.Board;
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

public class Level {
    private static final int DEFAULT_INITIAL_SUN = 50;

    private final int levelId;
    private final WaveManager waveManager;
    private final LevelType levelType;
    private final List<String> allowedPlants;
    private final List<String> allowedZombieNames;
    private final LevelRule levelRule;
    private final int initialSunAmount;

    private LevelStatus status;
    private Board board;

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
        this.status = LevelStatus.NOT_STARTED;
        this.board = null;

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

        levelRule.onTick(context);
        evaluate(context);

        if (status != LevelStatus.RUNNING || !areZombieWavesStarted()) {
            return null;
        }

        return waveManager.updateTicks(context.getCurrentTick());
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
