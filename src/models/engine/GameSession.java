package models.engine;

import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.plant.PlantFood;
import models.level.Level;
import models.level.Season;

public class GameSession {
    private static final int INITIAL_SUN_AMOUNT = 50;
    private static final int MAX_PLANT_FOOD = 3;

    private GameState state;
    private Season currentSeason;
    private int totalSunAmount;
    private int plantFoodCount;
    private Level currentLevel;
    private Board board;
    private TickManager tickManager;
    private SunManager sunManager;
    private PlantFactory plantFactory;

    public void initSession() {
        state = new GameState();
        state.setStatus(GameState.Status.RUNNING);

        totalSunAmount = INITIAL_SUN_AMOUNT;
        plantFoodCount = 0;

        board = new Board();
        tickManager = new TickManager();
        sunManager = new SunManager();
        plantFactory = new PlantFactory();

        if (currentLevel != null) {
            currentLevel.startLevel();
            currentLevel.applySpecialRules();
        }

        tickManager.start();
    }

    public void updateSession() {
        if (!isRunning()) {
            return;
        }

        sunManager.update();

        for (Lane lane : board.getLanes()) {
            lane.updateLaneTicks();
        }

        if (currentLevel != null && currentLevel.checkWinCondition()) {
            state.setStatus(GameState.Status.WON);
            tickManager.pause();
        }
    }

    public boolean advanceTicks(int count) {
        if (count <= 0 || !isRunning()) {
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

        Plant plant;
        try {
            plant = plantFactory.createPlant(plantName, position.getX(), position.getY());
        } catch (IllegalArgumentException exception) {
            return false;
        }

        int cost = plant.getType().getSunCost();
        if (totalSunAmount < cost) {
            return false;
        }

        boolean planted = board.placePlant(plant, position);
        if (!planted) {
            return false;
        }

        totalSunAmount -= cost;
        return true;
    }

    public boolean pluck(Position position) {
        if (!isRunning() || position == null) {
            return false;
        }

        return board.removePlant(position) != null;
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
        return true;
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
        this.currentSeason = currentSeason;
    }

    public Level getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Level currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getTotalSunAmount() {
        return totalSunAmount;
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
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
}