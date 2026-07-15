package models.engine.session;

import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.plant.PlantFood;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.sun.SunManager;
import models.engine.time.TickManager;
import models.level.core.Level;
import models.level.rules.LevelRuntimeContext;
import models.level.core.Season;
import models.level.wave.Wave;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

public class GameSession {
    private static final int DEFAULT_INITIAL_SUN_AMOUNT = 50;
    private static final int MAX_PLANT_FOOD = 3;
    private static final int BASE_PLANT_SUN_AMOUNT = 25;
    private static final int FALLING_SUN_TICKS = 50;

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

    public GameSession() {
        this.nextSunProductionTick = new IdentityHashMap<>();
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
        lastSpawnedWave = null;

        totalSunAmount = currentLevel == null
                ? DEFAULT_INITIAL_SUN_AMOUNT
                : currentLevel.resolveInitialSunAmount();

        try {
            tickManager.start();

            if (currentLevel != null) {
                LevelRuntimeContext context = createLevelContext();
                currentLevel.startLevel(board, context);
                lastSpawnedWave = currentLevel.updateTicks(context);
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

        sunManager.update();
        board.updateTicks();
        updatePlantSunProduction();

        if (currentLevel != null) {
            lastSpawnedWave = currentLevel.updateTicks(createLevelContext());
            updateStateFromLevel();
        } else if (board.hasBrainBeenEaten()) {
            state.setStatus(GameState.Status.LOST);
            tickManager.pause();
        }
    }

    public boolean advanceTicks(int count) {
        if (count <= 0 || !isRunning() || tickManager.isPaused()) {
            return false;
        }

        for (int i = 0; i < count; i++) {
            tickManager.advanceTicks(1);
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
        if (currentLevel != null && !currentLevel.isPlantAllowed(plantName)) {
            return false;
        }

        Plant plant;
        try {
            plant = plantFactory.createPlant(plantName, position.getX(), position.getY());
        } catch (IllegalArgumentException exception) {
            return false;
        }

        int cost = currentLevel != null && currentLevel.usesConveyorBelt()
                ? 0
                : plant.getCurrentSunCost();
        if (totalSunAmount < cost || !board.placePlant(plant, position)) {
            return false;
        }

        if (currentLevel != null && currentLevel.usesConveyorBelt()
                && !currentLevel.consumeConveyorPlant(plantName)) {
            board.removePlant(position);
            return false;
        }

        totalSunAmount -= cost;
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

        int collectedAmount = sunManager.collectSun(position);
        if (collectedAmount <= 0) {
            return false;
        }

        totalSunAmount += collectedAmount;
        Tile tile = board.getTileAt(position);
        Plant plant = tile == null ? null : tile.getCurrentPlant();
        if (isSunProducer(plant)) {
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
        if (!isRunning() || position == null || amount <= 0) {
            return false;
        }
        if (currentLevel != null && !currentLevel.allowsSkySun()) {
            return false;
        }

        sunManager.spawnSun(position, amount, FALLING_SUN_TICKS);
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
}
