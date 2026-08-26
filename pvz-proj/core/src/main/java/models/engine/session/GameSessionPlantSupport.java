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


abstract class GameSessionPlantSupport extends GameSessionEventSupport {
    protected GameSessionPlantSupport() {
        super();
    }

    protected GameSessionPlantSupport(Random random) {
        super(random);
    }

    protected void updateStateFromLevel() {
        if (currentLevel.checkLoseCondition()) {
            state.setStatus(GameState.Status.LOST);
            tickManager.pause();
        } else if (currentLevel.checkWinCondition()) {
            state.setStatus(GameState.Status.WON);
            tickManager.pause();
        }
    }

    protected LevelRuntimeContext createLevelContext() {
        return new LevelRuntimeContext(
                board,
                tickManager.getCurrentTick(),
                totalSunAmount,
                totalSunProduced,
                board.getTotalZombiesKilled(),
                board.getTotalPlantsDestroyed()
        );
    }

    protected void updateFallingSuns() {
        for (Sun sun : sunManager.update()) {
            pendingEvents.add(GameEvent.skySunLanded(
                    sun.getType(),
                    sun.getPosition()
            ));
        }
    }

    protected PlantFoodContext createPlantFoodContext() {
        return new PlantFoodContext(
                board,
                plantFactory,
                random,
                amount -> {
                    if (amount > 0) {
                        totalSunAmount += amount;
                        totalSunProduced += amount;
                    }
                },
                this::spawnPlantFoodSunBurst,
                this::recordBoardEvents,
                plant -> {
                    if (plant != null) {
                        plantAgeTicks.put(plant, 0);
                        scheduleSunProduction(plant);
                    }
                },
                plant -> {
                    if (plant != null) {
                        plantAgeTicks.put(plant, 0);
                    }
                }
        );
    }

    private void spawnPlantFoodSunBurst(Plant source, int amount) {
        if (source == null || amount <= 0 || board == null || sunManager == null) {
            return;
        }
        List<Position> positions = plantFoodSunPositions(source);
        if (positions.isEmpty()) {
            return;
        }
        int sunCount = Math.min(positions.size(), Math.max(1, (amount + 24) / 25));
        int fullQuanta = amount / 25;
        int remainder = amount % 25;
        int baseQuanta = fullQuanta / sunCount;
        int extraQuanta = fullQuanta % sunCount;
        for (int index = 0; index < sunCount; index++) {
            int sunAmount = baseQuanta * 25;
            if (index < extraQuanta) {
                sunAmount += 25;
            }
            if (index == 0) {
                sunAmount += remainder;
            }
            if (sunAmount > 0) {
                sunManager.spawnLooseSun(positions.get(index), sunAmount);
            }
        }
        totalSunProduced += amount;
    }

    private List<Position> plantFoodSunPositions(Plant source) {
        int centerX = Math.max(1, Math.min(board.getWidth(), (int) Math.round(source.getX())));
        int centerY = Math.max(1, Math.min(board.getHeight(), (int) Math.round(source.getY())));
        int[][] offsets = {
            {-1, 0}, {1, 0}, {0, 1}, {0, -1},
            {-1, 1}, {1, 1}, {-1, -1}, {1, -1}, {0, 0}
        };
        List<Position> positions = new ArrayList<>();
        for (int[] offset : offsets) {
            int x = centerX + offset[0];
            int y = centerY + offset[1];
            if (x >= 1 && x <= board.getWidth() && y >= 1 && y <= board.getHeight()) {
                positions.add(new Position(x, y));
            }
        }
        return positions;
    }

    protected void updatePlantFoodEffects() {
        for (Map.Entry<Plant, PlantFood> entry : new ArrayList<>(activePlantFoods.entrySet())) {
            Plant plant = entry.getKey();
            PlantFood food = entry.getValue();
            if (plant == null || !plant.isAlive() || food == null) {
                activePlantFoods.remove(plant);
                continue;
            }
            food.tick();
            if (food.isExpired()) {
                activePlantFoods.remove(plant);
            }
        }
    }

    protected Plant findSunProducerWaitingAt(Tile tile) {
        if (tile == null) {
            return null;
        }
        Plant fallback = null;
        for (Plant plant : tile.getPlants()) {
            if (!isSunProducer(plant)) {
                continue;
            }
            if (Integer.valueOf(-1).equals(nextSunProductionTick.get(plant))) {
                return plant;
            }
            if (fallback == null) {
                fallback = plant;
            }
        }
        return fallback;
    }

    protected void updatePlantSunProduction() {
        List<Plant> currentPlants = board.getAllPlants();
        Set<Plant> plantsOnBoard = Collections.newSetFromMap(new IdentityHashMap<>());
        plantsOnBoard.addAll(currentPlants);

        nextSunProductionTick.keySet().removeIf(
                plant -> plant == null || !plant.isAlive() || !plantsOnBoard.contains(plant)
        );
        plantAgeTicks.keySet().removeIf(
                plant -> plant == null || !plant.isAlive() || !plantsOnBoard.contains(plant)
        );

        for (Plant plant : currentPlants) {
            plantAgeTicks.merge(plant, 1, Integer::sum);
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
            int amount = resolvePlantSunAmount(plant);
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

    protected int resolvePlantSunAmount(Plant plant) {
        String name = normalizeName(plant == null ? null : plant.getName());
        int amount = BASE_PLANT_SUN_AMOUNT;

        if (name.equals("twin sunflower")) {
            amount = TWIN_SUNFLOWER_SUN_AMOUNT;
        } else if (name.equals("primal sunflower")) {
            amount = PRIMAL_SUNFLOWER_SUN_AMOUNT;
        } else if (name.equals("sun shroom")) {
            int growTicks = plant.getGrowTimeTicks() > 0
                    ? plant.getGrowTimeTicks()
                    : DEFAULT_SUN_SHROOM_GROW_TICKS;
            if (plantAgeTicks.getOrDefault(plant, 0) >= growTicks) {
                amount = MATURE_SUN_SHROOM_SUN_AMOUNT;
            }
        }

        amount += Math.max(0, plant.getSunProductionBonus());
        if (plant.isBoosted()) {
            amount *= 2;
        }
        if (board.isPlantFamilyBoosted("sun producer")) {
            amount *= 2;
        }
        if (plant.hasDoubleSunChance()
                && random.nextInt(100) < DOUBLE_SUN_CHANCE_PERCENT) {
            amount *= 2;
        }
        return Math.max(1, amount);
    }

    protected void scheduleSunProduction(Plant plant) {
        if (isSunProducer(plant)) {
            nextSunProductionTick.put(
                    plant,
                    tickManager.getCurrentTick() + productionInterval(plant)
            );
        }
    }

    protected int productionInterval(Plant plant) {
        return Math.max(1, plant.getProductionTimeTicks());
    }

    protected boolean isSunProducer(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return false;
        }
        String category = plant.getType().getCategory();
        return category != null
                && category.trim().toLowerCase(Locale.ROOT).equals("sun producer");
    }

    protected boolean isImmediatePlant(Plant plant) {
        String name = normalizeName(plant == null ? null : plant.getName());
        return name.equals("gold bloom")
                || name.equals("cherry bomb")
                || name.equals("grapeshot")
                || name.equals("jalapeno")
                || name.equals("doom shroom")
                || name.equals("ice shroom")
                || name.equals("hot potato")
                || name.equals("grave buster")
                || name.endsWith(" mint");
    }

    protected boolean useImmediatePlant(
            Plant plant,
            PlantType rechargeType,
            String consumedSeedName,
            Position position,
            int cost
    ) {
        Tile tile = board.getTileAt(position);
        if (tile == null || !tile.canPlacePlant(plant)) {
            return false;
        }

        if (currentLevel != null && currentLevel.usesConveyorBelt()
                && !currentLevel.consumeConveyorPlant(consumedSeedName)) {
            return false;
        }

        if (!executeImmediatePlantEffect(plant, position)) {
            return false;
        }

        totalSunAmount -= cost;
        startPlantRecharge(rechargeType);
        if (currentLevel != null) {
            currentLevel.onPlantUsed(consumedSeedName);
            currentLevel.evaluate(createLevelContext());
            updateStateFromLevel();
        }
        return true;
    }

    protected boolean executeImmediatePlantEffect(Plant plant, Position position) {
        String name = normalizeName(plant.getName());
        int damage = Math.max(1, plant.getAttackDamage());
        Boolean handled = executeResourceOrTerrainPlant(name, plant, position);
        if (handled == null) handled = executeAreaDamagePlant(name, plant, position, damage);
        if (handled == null) handled = executeLaneOrGlobalPlant(name, plant, position, damage);
        if (handled != null) return handled;
        if (name.endsWith(" mint")) {
            activateMint(name, plant);
            return true;
        }
        return false;
    }

    private Boolean executeResourceOrTerrainPlant(String name, Plant plant, Position position) {
        if (name.equals("gold bloom")) {
            int produced = 375 + plant.getSunProductionBonus();
            totalSunAmount += produced;
            totalSunProduced += produced;
            return true;
        }
        if (name.equals("grave buster")) return board.removeTerrain(position, TileType.GRAVE);
        if (!name.equals("hot potato")) return null;
        if (board.getTileAt(position).getTileType() != TileType.ICE) return false;
        board.meltTerrainArea(position, plant.hasMeltAreaThreeByThree() ? 1 : 0);
        return true;
    }

    private Boolean executeAreaDamagePlant(
            String name, Plant plant, Position position, int damage
    ) {
        int radius;
        String damageType;
        if (name.equals("cherry bomb") || name.equals("grapeshot")) {
            radius = 1;
            damageType = name;
        } else if (name.equals("doom shroom")) {
            radius = 2;
            damageType = name;
        } else {
            return null;
        }
        recordBoardEvents(board.damageZombiesInArea(
                position, radius, radius, Math.max(1800, damage), damageType,
                plant.getName(), plantCategory(plant)
        ));
        if (name.equals("grapeshot")) executeGrapeshotBounces(plant);
        return true;
    }

    private void executeGrapeshotBounces(Plant plant) {
        recordBoardEvents(board.damageRandomZombies(
                8 + Math.max(0, plant.getBounces()), 200, "grapeshot bounce", random,
                plant.getName(), plantCategory(plant)
        ));
    }

    private Boolean executeLaneOrGlobalPlant(
            String name, Plant plant, Position position, int damage
    ) {
        if (name.equals("jalapeno")) {
            recordBoardEvents(board.damageZombiesInLane(
                    position.getY(), Math.max(1800, damage), "jalapeno",
                    plant.getName(), plantCategory(plant)
            ));
            board.meltTerrainInLane(position.getY());
            return true;
        }
        if (!name.equals("ice shroom")) return null;
        recordBoardEvents(board.damageAllZombies(
                50, "ice shroom", plant.getName(), plantCategory(plant)
        ));
        board.freezeAllZombies(Math.max(50, plant.getFreezeDurationTicks()));
        return true;
    }

    private String plantCategory(Plant plant) {
        return plant.getType() == null ? "" : plant.getType().getCategory();
    }


    protected void activateMint(String normalizedName, Plant plant) {
        int duration = Math.max(100, plant.getDurationTicks());
        if (normalizedName.equals("enlighten mint")) {
            activateFamily("sun producer", duration);
        } else if (normalizedName.equals("appease mint")) {
            activateFamily("shooter", duration);
        } else if (normalizedName.equals("arma mint")) {
            activateFamily("lobber", duration);
        } else if (normalizedName.equals("bombard mint")) {
            activateFamily("explosive", duration);
        } else if (normalizedName.equals("enforce mint")) {
            activateFamily("melee", duration);
        } else if (normalizedName.equals("reinforce mint")) {
            activateFamily("wall nut", duration);
        } else if (normalizedName.equals("enchant mint")) {
            activateFamily("modifier", duration);
            activateFamily("homing", duration);
        } else if (normalizedName.equals("pierce mint")) {
            activateFamily("strike through", duration);
        } else if (normalizedName.equals("cattail mint")) {
            activateFamily("homing", duration);
        }
    }

    protected void activateFamily(String category, int duration) {
        board.activatePlantFamilyBoost(category, duration);
        resetFamilyRecharge(category);
        for (Plant plant : board.getAllPlants()) {
            if (plant.getType() != null
                    && normalizeName(plant.getType().getCategory()).equals(normalizeName(category))) {
                plant.resetCooldown();
            }
        }
    }

    protected void resetFamilyRecharge(String category) {
        PlantRegistry registry = plantFactory == null
                ? DefaultPlantRegistry.getInstance()
                : plantFactory.getPlantRegistry();
        for (PlantType type : registry.getAllPlantTypes()) {
            if (normalizeName(type.getCategory()).equals(normalizeName(category))) {
                plantRechargeUntilTick.remove(normalizeName(type.getName()));
            }
        }
    }

}
