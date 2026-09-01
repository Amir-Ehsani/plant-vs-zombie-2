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
import models.engine.events.GameEvent;
import models.engine.sun.SunManager;
import models.engine.time.TickManager;
import models.level.core.Level;
import models.level.core.Season;
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

abstract class GameSessionState {
    protected static final int DEFAULT_INITIAL_SUN_AMOUNT = 50;
    protected static final int MAX_PLANT_FOOD = 3;
    protected static final int BASE_PLANT_SUN_AMOUNT = 50;
    protected static final int TWIN_SUNFLOWER_SUN_AMOUNT = 100;
    protected static final int PRIMAL_SUNFLOWER_SUN_AMOUNT = 75;
    protected static final int MATURE_SUN_SHROOM_SUN_AMOUNT = 75;
    protected static final int SUN_SHROOM_STAGE_TWO_TICKS = 24 * 10;
    protected static final int DEFAULT_SUN_SHROOM_GROW_TICKS = 72 * 10;
    protected static final int DOUBLE_SUN_CHANCE_PERCENT = 25;
    protected static final int FALLING_SUN_TICKS = 50;
    protected static final int TICKS_PER_SECOND = 10;
    protected static final int NORMAL_SKY_SUN_PERCENT = 80;
    protected static final int SPECIAL_SKY_SUN_PERCENT = 15;
    protected static final int GLOWING_ZOMBIE_PERCENT = 5;
    protected static final int RADIOACTIVE_ZOMBIE_DAMAGE = 150;
    protected static final int RADIOACTIVE_PLANT_DAMAGE = 80;
    protected static final int RADIOACTIVE_ZOMBIE_RADIUS = 2;
    protected static final int RADIOACTIVE_PLANT_RADIUS = 1;
    protected static final int TERRAIN_REWARD_LIFETIME_TICKS = 3 * TICKS_PER_SECOND;

    protected GameState state;
    protected Season currentSeason;
    protected int totalSunAmount;
    protected int plantFoodCount;
    protected int initialPlantFoodCount;
    protected int totalSunProduced;
    protected int totalSunCollected;
    protected Level currentLevel;
    protected Board board;
    protected TickManager tickManager;
    protected SunManager sunManager;
    protected PlantFactory plantFactory;
    protected ZombieFactory zombieFactory;
    protected Wave lastSpawnedWave;
    protected final Map<Plant, Integer> nextSunProductionTick;
    protected final Map<Plant, Integer> plantAgeTicks;
    protected final Map<Plant, PlantFood> activePlantFoods;
    protected final Map<String, Integer> plantRechargeUntilTick;
    protected final Set<String> selectedPlantNames;
    protected final List<GameEvent> pendingEvents;
    protected final List<GroundRewardDrop> groundRewardDrops;
    protected final Map<Zombie, Boolean> glowingZombies;
    protected final List<PlantFoodDrop> plantFoodDrops;
    protected final Random random;
    protected boolean plantRechargeDisabled;
    protected int nextSkySunTick;
    protected int nextGroundRewardId;
    protected int lastAdvancedTickCount;

    protected GameSessionState() {
        this(new Random());
    }

    protected GameSessionState(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null.");
        }
        this.random = random;
        this.nextSunProductionTick = new IdentityHashMap<>();
        this.plantAgeTicks = new IdentityHashMap<>();
        this.activePlantFoods = new IdentityHashMap<>();
        this.plantRechargeUntilTick = new HashMap<>();
        this.selectedPlantNames = new LinkedHashSet<>();
        this.pendingEvents = new ArrayList<>();
        this.groundRewardDrops = new ArrayList<>();
        this.glowingZombies = new IdentityHashMap<>();
        this.plantFoodDrops = new ArrayList<>();
        this.nextGroundRewardId = 1;
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
                    || currentLevel.isPlantAllowed(type.getName())
                    || selected;
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

    public void setInitialPlantFoodCount(int count) {
        if (isRunning()) {
            throw new IllegalStateException("Cannot change initial Plant Food while the game is running.");
        }
        initialPlantFoodCount = Math.max(0, Math.min(MAX_PLANT_FOOD, count));
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public List<PlantFoodDrop> getPlantFoodDrops() {
        return Collections.unmodifiableList(new ArrayList<>(plantFoodDrops));
    }

    public int getTotalSunProduced() {
        return totalSunProduced;
    }

    public int getTotalSunCollected() {
        return totalSunCollected;
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

    public List<GroundRewardDrop> getGroundRewardDrops() {
        return Collections.unmodifiableList(groundRewardDrops);
    }

    public Wave getLastSpawnedWave() {
        return lastSpawnedWave;
    }

    protected boolean isPlantRechargeIgnored() {
        return plantRechargeDisabled
                || currentLevel != null && currentLevel.ignoresPlantRecharge()
                || currentLevel != null && currentLevel.usesConveyorBelt();
    }

    protected int resolvePlantCost(PlantType type) {
        if (type == null) {
            return 0;
        }
        return currentLevel != null && currentLevel.usesConveyorBelt()
                ? 0
                : type.getSunCost();
    }

    protected List<PlantType> resolveStatusPlantTypes() {
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

    protected void validateLockedPlantSelection(Collection<String> names) {
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

    protected void synchronizeLockedPlantSelection() {
        validateLockedPlantSelection(selectedPlantNames);
    }

    protected boolean containsName(Collection<String> values, String target) {
        return findName(values, target) != null;
    }

    protected String findName(Collection<String> values, String target) {
        String normalizedTarget = normalizeName(target);
        for (String value : values) {
            if (normalizeName(value).equals(normalizedTarget)) {
                return value;
            }
        }
        return null;
    }

    protected String normalizeName(String value) {
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
