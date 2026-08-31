package models.engine.session;

import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.plant.PlantFood;
import models.core.plant.PlantFoodContext;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.BoardResourceHandler;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
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


public class GameSession extends GameSessionPlantSupport {
    public GameSession() {
        super();
    }

    public GameSession(Random random) {
        super(random);
    }

    public void initSession() {
        if (isRunning()) throw new IllegalStateException("Game session is already running.");
        initializeRuntimeComponents();
        resetRuntimeCollections();
        totalSunAmount = currentLevel == null
                ? DEFAULT_INITIAL_SUN_AMOUNT : currentLevel.resolveInitialSunAmount();
        try {
            startRuntime();
        } catch (RuntimeException exception) {
            tickManager.pause();
            state.setStatus(GameState.Status.NOT_STARTED);
            throw exception;
        }
    }

    private void initializeRuntimeComponents() {
        state = new GameState();
        plantFoodCount = Math.max(0, Math.min(MAX_PLANT_FOOD, initialPlantFoodCount));
        initialPlantFoodCount = 0;
        totalSunProduced = 0;
        totalSunCollected = 0;
        board = new Board();
        tickManager = new TickManager();
        sunManager = new SunManager();
        board.setResourceHandler(createResourceHandler());
        plantFactory = new PlantFactory();
        zombieFactory = new ZombieFactory();
    }

    private BoardResourceHandler createResourceHandler() {
        return new BoardResourceHandler() {
            public int stealStoredSun(int amount) {
                int stolen = Math.max(0, Math.min(totalSunAmount, amount));
                totalSunAmount -= stolen;
                return stolen;
            }
            public int stealLooseSuns() {
                return sunManager.stealLooseSuns();
            }
            public void restoreSun(int amount) {
                if (amount > 0) totalSunAmount += amount;
            }
        };
    }

    private void resetRuntimeCollections() {
        nextSunProductionTick.clear();
        plantAgeTicks.clear();
        activePlantFoods.clear();
        resetScheduledPlantActions();
        plantRechargeUntilTick.clear();
        pendingEvents.clear();
        groundRewardDrops.clear();
        nextGroundRewardId = 1;
        glowingZombies.clear();
        plantFoodDrops.clear();
        plantRechargeDisabled = false;
        lastSpawnedWave = null;
        lastAdvancedTickCount = 0;
    }

    private void startRuntime() {
        synchronizeLockedPlantSelection();
        tickManager.start();
        scheduleNextSkySun();
        if (currentLevel != null) startCurrentLevel();
        state.setStatus(GameState.Status.RUNNING);
        if (currentLevel != null) updateStateFromLevel();
    }

    private void startCurrentLevel() {
        LevelRuntimeContext context = createLevelContext();
        currentLevel.startLevel(board, context);
        lastSpawnedWave = currentLevel.updateTicks(context);
        recordWaveEvents(lastSpawnedWave);
        recordTerrainSpawnEvents(currentLevel.drainTerrainSpawnedZombies());
        recordBoardEvents(board.stabilizeTerrain());
    }


    public void updateSession() {
        if (!isRunning() || tickManager.isPaused()) {
            return;
        }

        updateFallingSuns();
        updateGroundRewardDrops();
        updatePlantFoodEffects();
        updateScheduledPlantActions();
        BoardTickResult boardResult = board.updateTicks();
        recordBoardEvents(boardResult);
        updatePlantSunProduction();
        updateSkySunProduction();

        if (currentLevel != null) {
            lastSpawnedWave = currentLevel.updateTicks(createLevelContext());
            recordWaveEvents(lastSpawnedWave);
            recordTerrainSpawnEvents(currentLevel.drainTerrainSpawnedZombies());
            recordBoardEvents(board.stabilizeTerrain());
            updateStateFromLevel();
        } else if (board.hasBrainBeenEaten()) {
            state.setStatus(GameState.Status.LOST);
            tickManager.pause();
        }
    }

    private void updateGroundRewardDrops() {
        groundRewardDrops.removeIf(GroundRewardDrop::tick);
    }

    public boolean collectGroundReward(int rewardId) {
        java.util.Iterator<GroundRewardDrop> iterator = groundRewardDrops.iterator();
        while (iterator.hasNext()) {
            GroundRewardDrop drop = iterator.next();
            if (drop.getId() != rewardId) {
                continue;
            }
            if (drop.getType().equals("plant_food")) {
                plantFoodCount = Math.min(MAX_PLANT_FOOD, plantFoodCount + drop.getAmount());
            }
            iterator.remove();
            return true;
        }
        return false;
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
        if (totalSunAmount < cost) {
            return false;
        }

        return placePreparedPlant(plant, type, type.getName(), position, cost);
    }

    public boolean plantImitater(String copiedPlantName, Position position) {
        if (!isRunning() || copiedPlantName == null || position == null) {
            return false;
        }

        PlantType imitaterType = getPlantType("Imitater");
        PlantType copiedType = getPlantType(copiedPlantName);
        if (imitaterType == null || copiedType == null
                || normalizeName(copiedType.getName()).equals("imitater")) {
            return false;
        }
        if (!isPlantSelected(imitaterType.getName())
                || !isPlantSelected(copiedType.getName())) {
            return false;
        }
        if (currentLevel != null
                && (!currentLevel.isPlantAllowed(imitaterType.getName())
                || !currentLevel.isPlantAllowed(copiedType.getName()))) {
            return false;
        }
        if (getPlantRechargeRemainingTicks(imitaterType.getName()) > 0) {
            return false;
        }

        Plant copiedPlant;
        try {
            copiedPlant = plantFactory.createPlant(
                    copiedType,
                    position.getX(),
                    position.getY()
            );
        } catch (IllegalArgumentException exception) {
            return false;
        }

        int cost = resolvePlantCost(imitaterType);
        if (totalSunAmount < cost) {
            return false;
        }
        return placePreparedPlant(
                copiedPlant,
                imitaterType,
                imitaterType.getName(),
                position,
                cost
        );
    }

    private boolean placePreparedPlant(
            Plant plant,
            PlantType rechargeType,
            String consumedSeedName,
            Position position,
            int cost
    ) {
        if (plant == null || rechargeType == null || consumedSeedName == null) {
            return false;
        }

        if (isImmediatePlant(plant)) {
            return useImmediatePlant(
                    plant,
                    rechargeType,
                    consumedSeedName,
                    position,
                    cost
            );
        }

        if (!board.placePlant(plant, position)) {
            return false;
        }

        if (currentLevel != null && currentLevel.usesConveyorBelt()
                && !currentLevel.consumeConveyorPlant(consumedSeedName)) {
            board.removePlant(position, plant);
            return false;
        }

        totalSunAmount -= cost;
        startPlantRecharge(rechargeType);
        plantAgeTicks.put(plant, 0);
        scheduleSunProduction(plant);
        if (currentLevel != null) {
            currentLevel.onPlantUsed(consumedSeedName);
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
        plantAgeTicks.remove(removedPlant);
        activePlantFoods.remove(removedPlant);
        return true;
    }

    public boolean feedPlant(Position position) {
        return activatePlantFood(position, true);
    }

    public boolean activatePlantFood(Position position, boolean consumeInventory) {
        if (!isRunning() || position == null) {
            return false;
        }
        if (consumeInventory && plantFoodCount <= 0) {
            return false;
        }

        Tile tile = board.getTileAt(position);
        if (tile == null || !tile.hasPlant()) {
            return false;
        }

        Plant plant = tile.getCurrentPlant();
        if (plant == null || !plant.isAlive()) {
            return false;
        }
        PlantFood previous = activePlantFoods.remove(plant);
        if (previous != null) {
            previous.expire();
        }
        PlantFood plantFood = new PlantFood();
        plant.usePlantFood(plantFood, createPlantFoodContext());
        if (plantFood.isActive()) {
            activePlantFoods.put(plant, plantFood);
        }
        if (consumeInventory) {
            plantFoodCount--;
        }
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
        totalSunCollected += sun.getSunAmount();
        Tile tile = board.getTileAt(position);
        Plant plant = findSunProducerWaitingAt(tile);
        if (sun.isProducedByPlant() && plant != null) {
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

    public boolean collectPlantFoodDrop(PlantFoodDrop drop) {
        if (!isRunning() || drop == null || plantFoodCount >= MAX_PLANT_FOOD) {
            return false;
        }
        if (!plantFoodDrops.remove(drop)) {
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

}
