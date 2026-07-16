package models.engine.session;

import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.plant.PlantFood;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.combat.BoardTickResult;
import models.engine.events.GameEvent;
import models.engine.events.GameEventType;
import models.engine.sun.Sun;
import models.engine.sun.SunManager;
import models.engine.sun.SunType;
import models.engine.time.TickManager;
import models.level.core.Level;
import models.level.core.Season;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.impl.LockedPlantsRule;
import models.level.wave.Wave;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GameSession {
    private static final int DEFAULT_INITIAL_SUN_AMOUNT = 50;
    private static final int MAX_PLANT_FOOD = 3;
    private static final int BASE_PLANT_SUN_AMOUNT = 25;
    private static final int FALLING_SUN_TICKS = 50;
    private static final int TICKS_PER_SECOND = 10;
    private static final int NORMAL_SKY_SUN_PERCENT = 80;
    private static final int SPECIAL_SKY_SUN_PERCENT = 15;
    private static final int GLOWING_ZOMBIE_PERCENT = 5;
    private static final int RADIOACTIVE_ZOMBIE_DAMAGE = 150;
    private static final int RADIOACTIVE_PLANT_DAMAGE = 80;
    private static final int RADIOACTIVE_ZOMBIE_RADIUS = 2;
    private static final int RADIOACTIVE_PLANT_RADIUS = 1;

    private GameState state;
    private Season currentSeason;
    private int totalSunAmount;
    private int plantFoodCount;
    private int totalSunProduced;
    private Level currentLevel;
    private Board board;
    private TickManager tickManager;
    private SunManager sunManager;
    private PlantFactory plantFactory;
    private ZombieFactory zombieFactory;
    private Wave lastSpawnedWave;
    private final Map<Plant, Integer> nextSunProductionTick;
    private final Map<String, Integer> plantRechargeUntilTick;
    private final Set<String> selectedPlantNames;
    private final List<GameEvent> pendingEvents;
    private final Map<Zombie, Boolean> glowingZombies;
    private final Random random;
    private boolean plantRechargeDisabled;
    private int nextSkySunTick;
    private int lastAdvancedTickCount;

    public GameSession() {
        this(new Random());
    }

    public GameSession(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null.");
        }
        this.random = random;
        this.nextSunProductionTick = new IdentityHashMap<>();
        this.plantRechargeUntilTick = new HashMap<>();
        this.selectedPlantNames = new LinkedHashSet<>();
        this.pendingEvents = new ArrayList<>();
        this.glowingZombies = new IdentityHashMap<>();
    }

    public void initSession() {
        if (isRunning()) {
            throw new IllegalStateException("Game session is already running.");
        }

        state = new GameState();
        plantFoodCount = 0;
        totalSunProduced = 0;
        board = new Board();
        tickManager = new TickManager();
        sunManager = new SunManager();
        plantFactory = new PlantFactory();
        zombieFactory = new ZombieFactory();
        nextSunProductionTick.clear();
        plantRechargeUntilTick.clear();
        pendingEvents.clear();
        glowingZombies.clear();
        plantRechargeDisabled = false;
        lastSpawnedWave = null;
        lastAdvancedTickCount = 0;

        totalSunAmount = currentLevel == null
                ? DEFAULT_INITIAL_SUN_AMOUNT
                : currentLevel.resolveInitialSunAmount();

        try {
            synchronizeLockedPlantSelection();
            tickManager.start();
            scheduleNextSkySun();

            if (currentLevel != null) {
                LevelRuntimeContext context = createLevelContext();
                currentLevel.startLevel(board, context);
                lastSpawnedWave = currentLevel.updateTicks(context);
                recordWaveEvents(lastSpawnedWave);
            }

            state.setStatus(GameState.Status.RUNNING);
            if (currentLevel != null) {
                updateStateFromLevel();
            }
        } catch (RuntimeException exception) {
            tickManager.pause();
            state.setStatus(GameState.Status.NOT_STARTED);
            throw exception;
        }
    }

    public void updateSession() {
        if (!isRunning() || tickManager.isPaused()) {
            return;
        }

        updateFallingSuns();
        BoardTickResult boardResult = board.updateTicks();
        recordBoardEvents(boardResult);
        updatePlantSunProduction();
        updateSkySunProduction();

        if (currentLevel != null) {
            lastSpawnedWave = currentLevel.updateTicks(createLevelContext());
            recordWaveEvents(lastSpawnedWave);
            updateStateFromLevel();
        } else if (board.hasBrainBeenEaten()) {
            state.setStatus(GameState.Status.LOST);
            tickManager.pause();
        }
    }

    public boolean advanceTicks(int count) {
        if (count <= 0 || !isRunning() || tickManager.isPaused()) {
            lastAdvancedTickCount = 0;
            return false;
        }

        lastAdvancedTickCount = 0;
        for (int i = 0; i < count; i++) {
            tickManager.advanceTicks(1);
            lastAdvancedTickCount++;
            updateSession();
            if (!isRunning()) {
                break;
            }
        }
        return true;
    }

    public boolean plant(String plantName, Position position) {
        if (!isRunning() || plantName == null || position == null) {
            return false;
        }

        PlantType type = getPlantType(plantName);
        if (type == null || !isPlantSelected(plantName)) {
            return false;
        }
        if (currentLevel != null && !currentLevel.isPlantAllowed(plantName)) {
            return false;
        }
        if (getPlantRechargeRemainingTicks(plantName) > 0) {
            return false;
        }

        Plant plant;
        try {
            plant = plantFactory.createPlant(type, position.getX(), position.getY());
        } catch (IllegalArgumentException exception) {
            return false;
        }

        int cost = resolvePlantCost(type);
        if (totalSunAmount < cost || !board.placePlant(plant, position)) {
            return false;
        }

        if (currentLevel != null && currentLevel.usesConveyorBelt()
                && !currentLevel.consumeConveyorPlant(plantName)) {
            board.removePlant(position);
            return false;
        }

        totalSunAmount -= cost;
        startPlantRecharge(type);
        scheduleSunProduction(plant);
        if (currentLevel != null) {
            currentLevel.onPlantUsed(plantName);
        }
        return true;
    }

    public boolean pluck(Position position) {
        if (!isRunning() || position == null) {
            return false;
        }

        Plant removedPlant = board.removePlant(position);
        if (removedPlant == null) {
            return false;
        }
        nextSunProductionTick.remove(removedPlant);
        return true;
    }

    public boolean feedPlant(Position position) {
        if (!isRunning() || plantFoodCount <= 0 || position == null) {
            return false;
        }

        Tile tile = board.getTileAt(position);
        if (tile == null || !tile.hasPlant()) {
            return false;
        }

        Plant plant = tile.getCurrentPlant();
        plant.usePlantFood(new PlantFood());
        plantFoodCount--;
        return true;
    }

    public boolean collectSun(Position position) {
        if (!isRunning() || position == null) {
            return false;
        }

        Sun sun = sunManager.collectSunObject(position);
        if (sun == null) {
            return false;
        }

        if (sun.isRadioactiveAndFalling()) {
            BoardTickResult result = board.applyAreaDamage(
                    position,
                    RADIOACTIVE_ZOMBIE_RADIUS,
                    RADIOACTIVE_ZOMBIE_DAMAGE,
                    RADIOACTIVE_PLANT_RADIUS,
                    RADIOACTIVE_PLANT_DAMAGE
            );
            pendingEvents.add(GameEvent.radioactiveSunExploded(
                    position,
                    result.getZombiesKilled(),
                    result.getPlantsDestroyed()
            ));
            recordBoardEvents(result);
            if (currentLevel != null) {
                currentLevel.evaluate(createLevelContext());
                updateStateFromLevel();
            }
            return true;
        }

        totalSunAmount += sun.getSunAmount();
        Tile tile = board.getTileAt(position);
        Plant plant = tile == null ? null : tile.getCurrentPlant();
        if (sun.isProducedByPlant() && isSunProducer(plant)) {
            scheduleSunProduction(plant);
        }
        return true;
    }

    public boolean spawnPlantSun(Position position, int amount) {
        if (!isRunning() || position == null || amount <= 0) {
            return false;
        }
        if (sunManager.hasSunAt(position)) {
            return false;
        }

        sunManager.spawnPermanentSun(position, amount);
        totalSunProduced += amount;
        return true;
    }

    public boolean spawnSkySun(Position position, int amount) {
        if (amount <= 0) {
            return false;
        }
        SunType type = amount == SunType.SPECIAL.getCollectionAmount()
                ? SunType.SPECIAL
                : SunType.NORMAL;
        return spawnSkySun(position, type);
    }

    public boolean spawnSkySun(Position position, SunType type) {
        if (!isRunning() || position == null || type == null) {
            return false;
        }
        if (currentLevel != null && !currentLevel.allowsSkySun()) {
            return false;
        }

        sunManager.spawnSkySun(position, type, FALLING_SUN_TICKS);
        pendingEvents.add(GameEvent.skySunDropping(type, position));
        return true;
    }

    public boolean startZombieWaves() {
        return currentLevel != null
                && isRunning()
                && currentLevel.startZombieWaves();
    }

    public boolean spawnZombie(String zombieName, Position position) {
        if (!isRunning() || zombieName == null || position == null) {
            return false;
        }

        Tile tile = board.getTileAt(position);
        if (tile == null) {
            return false;
        }

        try {
            Zombie zombie = zombieFactory.createZombie(
                    zombieName,
                    position.getX(),
                    position.getY()
            );
            registerSpawnedZombie(zombie);
            tile.addZombie(zombie);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public int releaseNuke() {
        if (!isRunning()) {
            return 0;
        }
        return board.destroyAllZombies();
    }

    public void addSun(int amount) {
        if (amount > 0) {
            totalSunAmount += amount;
        }
    }

    public boolean addPlantFood() {
        if (plantFoodCount >= MAX_PLANT_FOOD) {
            return false;
        }
        plantFoodCount++;
        return true;
    }

    public boolean removePlantCooldowns() {
        if (!isRunning()) {
            return false;
        }
        plantRechargeDisabled = true;
        plantRechargeUntilTick.clear();
        return true;
    }

    public int getPlantRechargeRemainingTicks(String plantName) {
        if (plantName == null || tickManager == null || isPlantRechargeIgnored()) {
            return 0;
        }

        int availableAt = plantRechargeUntilTick.getOrDefault(normalizeName(plantName), 0);
        return Math.max(0, availableAt - tickManager.getCurrentTick());
    }

    public boolean isPlantRechargeDisabled() {
        return plantRechargeDisabled;
    }

    public List<PlantRechargeStatus> getPlantRechargeStatuses() {
        List<PlantRechargeStatus> statuses = new ArrayList<>();

        for (PlantType type : resolveStatusPlantTypes()) {
            boolean selected = isPlantSelected(type.getName());
            boolean allowedByLevel = currentLevel == null
                    || currentLevel.isPlantAllowed(type.getName());
            int cost = resolvePlantCost(type);
            boolean enoughSun = totalSunAmount >= cost;
            int remainingTicks = getPlantRechargeRemainingTicks(type.getName());
            boolean plantable = selected
                    && allowedByLevel
                    && enoughSun
                    && remainingTicks == 0;

            statuses.add(new PlantRechargeStatus(
                    type.getName(),
                    cost,
                    remainingTicks,
                    plantable,
                    selected,
                    allowedByLevel,
                    enoughSun
            ));
        }

        return Collections.unmodifiableList(statuses);
    }

    public PlantType getPlantType(String plantName) {
        if (plantName == null) {
            return null;
        }
        PlantRegistry registry = plantFactory == null
                ? DefaultPlantRegistry.getInstance()
                : plantFactory.getPlantRegistry();
        return registry.getByName(plantName);
    }

    public int getPlantCost(String plantName) {
        PlantType type = getPlantType(plantName);
        return type == null ? -1 : resolvePlantCost(type);
    }

    public boolean isPlantSelected(String plantName) {
        if (plantName == null) {
            return false;
        }
        if (selectedPlantNames.isEmpty()) {
            return true;
        }
        return containsName(selectedPlantNames, plantName);
    }

    public void setSelectedPlantNames(Collection<String> plantNames) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot change selected plants while the game is running.");
        }

        LinkedHashSet<String> resolvedNames = new LinkedHashSet<>();
        if (plantNames != null) {
            for (String plantName : plantNames) {
                PlantType type = DefaultPlantRegistry.getInstance().getByName(plantName);
                if (type == null) {
                    throw new IllegalArgumentException("Unknown plant type: " + plantName);
                }
                resolvedNames.add(type.getName());
            }
        }

        validateLockedPlantSelection(resolvedNames);
        selectedPlantNames.clear();
        selectedPlantNames.addAll(resolvedNames);
    }

    public boolean addSelectedPlant(String plantName) {
        if (isRunning()) {
            return false;
        }
        PlantType type = DefaultPlantRegistry.getInstance().getByName(plantName);
        if (type == null || containsName(selectedPlantNames, type.getName())) {
            return false;
        }

        if (currentLevel != null && currentLevel.getLevelRule() instanceof LockedPlantsRule) {
            LockedPlantsRule rule = (LockedPlantsRule) currentLevel.getLevelRule();
            if (!rule.selectPlant(type.getName())) {
                return false;
            }
        }

        selectedPlantNames.add(type.getName());
        return true;
    }

    public boolean removeSelectedPlant(String plantName) {
        if (isRunning() || plantName == null) {
            return false;
        }
        String selectedName = findName(selectedPlantNames, plantName);
        if (selectedName == null) {
            return false;
        }

        if (currentLevel != null && currentLevel.getLevelRule() instanceof LockedPlantsRule) {
            LockedPlantsRule rule = (LockedPlantsRule) currentLevel.getLevelRule();
            if (!rule.deselectPlant(selectedName)) {
                return false;
            }
        }

        selectedPlantNames.remove(selectedName);
        return true;
    }

    public Set<String> getSelectedPlantNames() {
        return Collections.unmodifiableSet(selectedPlantNames);
    }

    public List<GameEvent> drainEvents() {
        List<GameEvent> events = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public void clearPendingEvents() {
        pendingEvents.clear();
    }

    public int getLastAdvancedTickCount() {
        return lastAdvancedTickCount;
    }

    public boolean isRunning() {
        return state != null && state.isRunning();
    }

    public GameState getState() {
        return state;
    }

    public Season getCurrentSeason() {
        return currentSeason;
    }

    public void setCurrentSeason(Season currentSeason) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot change season while the game is running.");
        }
        this.currentSeason = currentSeason;
    }

    public Level getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Level currentLevel) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot change level while the game is running.");
        }
        this.currentLevel = currentLevel;
        selectedPlantNames.clear();
        plantRechargeUntilTick.clear();
    }

    public int getTotalSunAmount() {
        return totalSunAmount;
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public int getTotalSunProduced() {
        return totalSunProduced;
    }

    public int getTotalZombiesKilled() {
        return board == null ? 0 : board.getTotalZombiesKilled();
    }

    public int getTotalPlantsDestroyed() {
        return board == null ? 0 : board.getTotalPlantsDestroyed();
    }

    public Board getBoard() {
        return board;
    }

    public TickManager getTickManager() {
        return tickManager;
    }

    public SunManager getSunManager() {
        return sunManager;
    }

    public Wave getLastSpawnedWave() {
        return lastSpawnedWave;
    }

    private void updateStateFromLevel() {
        if (currentLevel.checkLoseCondition()) {
            state.setStatus(GameState.Status.LOST);
            tickManager.pause();
        } else if (currentLevel.checkWinCondition()) {
            state.setStatus(GameState.Status.WON);
            tickManager.pause();
        }
    }

    private LevelRuntimeContext createLevelContext() {
        return new LevelRuntimeContext(
                board,
                tickManager.getCurrentTick(),
                totalSunAmount,
                totalSunProduced,
                board.getTotalZombiesKilled(),
                board.getTotalPlantsDestroyed()
        );
    }

    private void updateFallingSuns() {
        for (Sun sun : sunManager.update()) {
            pendingEvents.add(GameEvent.skySunLanded(
                    sun.getType(),
                    sun.getPosition()
            ));
        }
    }

    private void updatePlantSunProduction() {
        nextSunProductionTick.keySet().removeIf(plant -> !plant.isAlive());

        for (Plant plant : board.getAllPlants()) {
            if (!isSunProducer(plant)) {
                continue;
            }
            nextSunProductionTick.putIfAbsent(
                    plant,
                    tickManager.getCurrentTick() + productionInterval(plant)
            );

            int nextTick = nextSunProductionTick.get(plant);
            if (nextTick < 0 || tickManager.getCurrentTick() < nextTick) {
                continue;
            }

            Position position = new Position((int) plant.getX(), (int) plant.getY());
            int amount = BASE_PLANT_SUN_AMOUNT + plant.getSunProductionBonus();
            if (spawnPlantSun(position, amount)) {
                nextSunProductionTick.put(plant, -1);
                pendingEvents.add(GameEvent.plantSunProduced(
                        plant.getName(),
                        position,
                        amount
                ));
            }
        }
    }

    private void scheduleSunProduction(Plant plant) {
        if (isSunProducer(plant)) {
            nextSunProductionTick.put(
                    plant,
                    tickManager.getCurrentTick() + productionInterval(plant)
            );
        }
    }

    private int productionInterval(Plant plant) {
        return Math.max(1, plant.getProductionTimeTicks());
    }

    private boolean isSunProducer(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return false;
        }
        String category = plant.getType().getCategory();
        return category != null
                && category.trim().toLowerCase(Locale.ROOT).equals("sun producer");
    }

    private void updateSkySunProduction() {
        if (!skySunIsAllowed()) {
            nextSkySunTick = -1;
            return;
        }
        if (nextSkySunTick < 0) {
            scheduleNextSkySun();
            return;
        }
        if (tickManager.getCurrentTick() < nextSkySunTick) {
            return;
        }

        Position position = randomSunPosition();
        SunType type = randomSkySunType();
        spawnSkySun(position, type);
        scheduleNextSkySun();
    }

    private void scheduleNextSkySun() {
        if (tickManager == null || !skySunIsAllowed()) {
            nextSkySunTick = -1;
            return;
        }

        double elapsedSeconds = tickManager.getCurrentTick() / (double) TICKS_PER_SECOND;
        double intervalSeconds = Math.max(6.0 + 0.05 * elapsedSeconds, 12.0);
        int intervalTicks = Math.max(1, (int) Math.ceil(intervalSeconds * TICKS_PER_SECOND));
        nextSkySunTick = tickManager.getCurrentTick() + intervalTicks;
    }

    private boolean skySunIsAllowed() {
        return currentLevel == null || currentLevel.allowsSkySun();
    }

    private Position randomSunPosition() {
        Position fallback = new Position(
                random.nextInt(board.getWidth()) + 1,
                random.nextInt(board.getHeight()) + 1
        );

        for (int attempt = 0; attempt < board.getWidth() * board.getHeight(); attempt++) {
            Position candidate = new Position(
                    random.nextInt(board.getWidth()) + 1,
                    random.nextInt(board.getHeight()) + 1
            );
            if (!sunManager.hasSunAt(candidate)) {
                return candidate;
            }
        }
        return fallback;
    }

    private SunType randomSkySunType() {
        int roll = random.nextInt(100);
        if (roll < NORMAL_SKY_SUN_PERCENT) {
            return SunType.NORMAL;
        }
        if (roll < NORMAL_SKY_SUN_PERCENT + SPECIAL_SKY_SUN_PERCENT) {
            return SunType.SPECIAL;
        }
        return SunType.RADIOACTIVE;
    }

    private void recordWaveEvents(Wave wave) {
        if (wave == null) {
            return;
        }

        boolean finalWave = currentLevel != null
                && wave.getWaveNumber() == currentLevel.getWaveManager().getTotalWaves();
        pendingEvents.add(GameEvent.waveStarted(wave.getWaveNumber(), finalWave));

        for (Zombie zombie : wave.getZombiesList()) {
            if (zombie != null) {
                registerSpawnedZombie(zombie);
                pendingEvents.add(GameEvent.zombieSpawned(
                        zombie,
                        wave.getWaveNumber()
                ));
            }
        }
    }

    private void registerSpawnedZombie(Zombie zombie) {
        boolean glowing = random.nextInt(100) < GLOWING_ZOMBIE_PERCENT;
        glowingZombies.put(zombie, glowing);
        zombie.setGlowing(glowing);
    }

    private void recordBoardEvents(BoardTickResult result) {
        if (result == null) {
            return;
        }

        pendingEvents.addAll(result.getEvents());

        for (GameEvent event : result.getEvents()) {
            if (event.getType() == GameEventType.ZOMBIE_KILLED) {
                handleZombieDeath(event.getZombie());
            }
        }
    }

    private void handleZombieDeath(Zombie zombie) {
        if (zombie == null) {
            return;
        }

        if (Boolean.TRUE.equals(glowingZombies.remove(zombie))
                && plantFoodCount < MAX_PLANT_FOOD) {
            plantFoodCount++;
            pendingEvents.add(GameEvent.plantFoodDropped(plantFoodCount));
        }

        if (zombie.hasDroppedReward()) {
            pendingEvents.add(GameEvent.rewardDropped(zombie.getDroppedRewardType()));
        }
    }

    private void startPlantRecharge(PlantType type) {
        if (type == null || isPlantRechargeIgnored()
                || currentLevel != null && currentLevel.usesConveyorBelt()) {
            return;
        }

        int rechargeTicks = Math.max(0, type.getRecharge());
        if (rechargeTicks == 0) {
            plantRechargeUntilTick.remove(normalizeName(type.getName()));
            return;
        }

        plantRechargeUntilTick.put(
                normalizeName(type.getName()),
                tickManager.getCurrentTick() + rechargeTicks
        );
    }

    private boolean isPlantRechargeIgnored() {
        return plantRechargeDisabled
                || currentLevel != null && currentLevel.ignoresPlantRecharge()
                || currentLevel != null && currentLevel.usesConveyorBelt();
    }

    private int resolvePlantCost(PlantType type) {
        if (type == null) {
            return 0;
        }
        return currentLevel != null && currentLevel.usesConveyorBelt()
                ? 0
                : type.getSunCost();
    }

    private List<PlantType> resolveStatusPlantTypes() {
        PlantRegistry registry = plantFactory == null
                ? DefaultPlantRegistry.getInstance()
                : plantFactory.getPlantRegistry();
        List<PlantType> types = new ArrayList<>();

        if (!selectedPlantNames.isEmpty()) {
            for (String plantName : selectedPlantNames) {
                PlantType type = registry.getByName(plantName);
                if (type != null) {
                    types.add(type);
                }
            }
            return types;
        }

        if (currentLevel != null && currentLevel.usesConveyorBelt()) {
            Set<String> seen = new LinkedHashSet<>();
            for (String plantName : currentLevel.getConveyorPlants()) {
                PlantType type = registry.getByName(plantName);
                if (type != null && seen.add(normalizeName(type.getName()))) {
                    types.add(type);
                }
            }
            return types;
        }

        if (currentLevel != null && !currentLevel.getAllowedPlants().isEmpty()) {
            for (String plantName : currentLevel.getAllowedPlants()) {
                PlantType type = registry.getByName(plantName);
                if (type != null) {
                    types.add(type);
                }
            }
            return types;
        }

        types.addAll(registry.getAllPlantTypes());
        return types;
    }

    private void validateLockedPlantSelection(Collection<String> names) {
        if (currentLevel == null
                || !(currentLevel.getLevelRule() instanceof LockedPlantsRule)) {
            return;
        }

        LockedPlantsRule rule = (LockedPlantsRule) currentLevel.getLevelRule();
        rule.resetSelections();
        for (String name : names) {
            if (!rule.selectPlant(name)) {
                rule.resetSelections();
                throw new IllegalArgumentException(
                        "Plant is locked by the current level: " + name
                );
            }
        }
    }

    private void synchronizeLockedPlantSelection() {
        validateLockedPlantSelection(selectedPlantNames);
    }

    private boolean containsName(Collection<String> values, String target) {
        return findName(values, target) != null;
    }

    private String findName(Collection<String> values, String target) {
        String normalizedTarget = normalizeName(target);
        for (String value : values) {
            if (normalizeName(value).equals(normalizedTarget)) {
                return value;
            }
        }
        return null;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}
