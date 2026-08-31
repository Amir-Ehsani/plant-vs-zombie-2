package models.engine.session;

import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.plant.PlantFood;
import models.core.plant.PlantFoodContext;
import models.core.plant.PlantActionTiming;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.BoardResourceHandler;
import models.engine.board.Lane;
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
import java.util.Comparator;
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
    private static final int SUN_PRODUCTION_PENDING = -2;

    private final List<ScheduledPlantAction> scheduledPlantActions = new ArrayList<>();

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
                totalSunCollected,
                board.getTotalZombiesKilled(),
                board.getTotalPlantsDestroyed()
        );
    }

    protected void resetScheduledPlantActions() {
        scheduledPlantActions.clear();
    }

    protected void updateScheduledPlantActions() {
        if (tickManager == null || scheduledPlantActions.isEmpty()) {
            return;
        }
        int currentTick = tickManager.getCurrentTick();
        List<ScheduledPlantAction> ready = new ArrayList<>();
        for (ScheduledPlantAction scheduled : scheduledPlantActions) {
            if (scheduled.dueTick <= currentTick) {
                ready.add(scheduled);
            }
        }
        scheduledPlantActions.removeAll(ready);
        for (ScheduledPlantAction scheduled : ready) {
            scheduled.action.run();
        }
    }

    private void schedulePlantAction(int delayTicks, Runnable action) {
        if (action == null) {
            return;
        }
        if (delayTicks <= 0 || tickManager == null) {
            action.run();
            return;
        }
        scheduledPlantActions.add(new ScheduledPlantAction(
                tickManager.getCurrentTick() + delayTicks,
                action
        ));
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
                this::schedulePlantAction,
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
        return sunBurstPositions(centerX, centerY);
    }

    private List<Position> sunBurstPositions(int centerX, int centerY) {
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

    private void spawnImmediateSunBurst(Position center, int amount) {
        if (center == null || amount <= 0) {
            return;
        }
        if (board == null || sunManager == null) {
            totalSunAmount += amount;
            totalSunProduced += amount;
            return;
        }
        int centerX = Math.max(1, Math.min(board.getWidth(), center.getX()));
        int centerY = Math.max(1, Math.min(board.getHeight(), center.getY()));
        List<Position> positions = sunBurstPositions(centerX, centerY);
        if (positions.isEmpty()) {
            totalSunAmount += amount;
            totalSunProduced += amount;
            return;
        }
        int sunCount = positions.size();
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
            int impactDelay = PlantActionTiming.sunProductionImpactTicks(plant.getName());
            plant.triggerSpecialAnimation(sunProductionClip(plant));
            nextSunProductionTick.put(plant, SUN_PRODUCTION_PENDING);
            schedulePlantAction(impactDelay, () -> {
                if (!plant.isAlive() || !board.getAllPlants().contains(plant)) {
                    nextSunProductionTick.remove(plant);
                    return;
                }
                if (spawnPlantSun(position, amount)) {
                    nextSunProductionTick.put(plant, -1);
                    pendingEvents.add(GameEvent.plantSunProduced(
                            plant.getName(),
                            position,
                            amount
                    ));
                } else {
                    nextSunProductionTick.put(
                            plant,
                            tickManager.getCurrentTick() + productionInterval(plant)
                    );
                }
            });
        }
    }

    private String sunProductionClip(Plant plant) {
        String name = normalizeName(plant == null ? null : plant.getName());
        if (name.equals("sun shroom")) {
            int growTicks = plant.getGrowTimeTicks() > 0
                    ? plant.getGrowTimeTicks()
                    : DEFAULT_SUN_SHROOM_GROW_TICKS;
            int age = plantAgeTicks.getOrDefault(plant, 0);
            if (plant.isGrowthFinished() || age >= growTicks) {
                return "special_stage3";
            }
            if (age >= Math.min(SUN_SHROOM_STAGE_TWO_TICKS, growTicks)) {
                return "special_stage2";
            }
            return "special_stage1";
        }
        return "special";
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
            int age = plantAgeTicks.getOrDefault(plant, 0);
            if (plant.isGrowthFinished() || age >= growTicks) {
                amount = MATURE_SUN_SHROOM_SUN_AMOUNT;
            } else if (age >= Math.min(SUN_SHROOM_STAGE_TWO_TICKS, growTicks)) {
                amount = 50;
            } else {
                amount = 25;
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

        String normalizedName = normalizeName(plant.getName());
        int impactDelay = PlantActionTiming.specialImpactTicks(normalizedName);
        if (impactDelay > 0) {
            schedulePlantAction(
                    impactDelay,
                    () -> executeImmediatePlantEffect(plant, position)
            );
        } else if (!executeImmediatePlantEffect(plant, position)) {
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
            activateMintEntranceEffect(name, plant, position);
            activateMint(name, plant);
            return true;
        }
        return false;
    }

    private Boolean executeResourceOrTerrainPlant(String name, Plant plant, Position position) {
        if (name.equals("gold bloom")) {
            int produced = 375 + plant.getSunProductionBonus();
            spawnImmediateSunBurst(position, produced);
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
        if (name.equals("doom shroom")) {
            recordBoardEvents(board.damageAllZombies(
                    Math.max(3000, damage), "doom shroom",
                    plant.getName(), plantCategory(plant)
            ));
            createTemporaryCrater(position);
            return true;
        }
        if (name.equals("cherry bomb") || name.equals("grapeshot")) {
            radius = 1;
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

    private void createTemporaryCrater(Position position) {
        Tile tile = board.getTileAt(position);
        if (tile == null) {
            return;
        }
        tile.setTileType(TileType.CRATER);
        schedulePlantAction(135, () -> {
            Tile current = board.getTileAt(position);
            if (current != null && current.getTileType() == TileType.CRATER) {
                current.setTileType(TileType.NORMAL);
            }
        });
    }

    private void executeGrapeshotBounces(Plant plant) {
        int bounceCount = 8 + Math.max(0, plant.getBounces());
        for (int bounce = 0; bounce < bounceCount; bounce++) {
            int delay = 2 + (int) Math.round((48.0 * bounce) / Math.max(1, bounceCount - 1));
            schedulePlantAction(delay, () -> recordBoardEvents(board.damageRandomZombies(
                    1, 200, "grapeshot bounce", random,
                    plant.getName(), plantCategory(plant)
            )));
        }
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



    private void activateMintEntranceEffect(
            String normalizedName, Plant plant, Position position
    ) {
        if (normalizedName.equals("appease mint")) {
            activateAppeaseMintVolley(plant, position);
        }
    }

    private void activateAppeaseMintVolley(Plant plant, Position position) {
        for (int volley = 0; volley < 3; volley++) {
            int delay = volley * 4;
            schedulePlantAction(delay, () -> fireAppeaseMintHugePea(plant, position));
        }
    }

    private void fireAppeaseMintHugePea(Plant plant, Position position) {
        Lane lane = board == null ? null : board.getLaneAt(position.getY());
        if (lane == null) {
            return;
        }
        List<Zombie> targets = new ArrayList<>();
        for (Zombie zombie : lane.getAllZombies()) {
            if (zombie != null && board.isZombieOnLawn(zombie) && !board.isHypnotized(zombie)
                    && zombie.getX() >= position.getX()) {
                targets.add(zombie);
            }
        }
        targets.sort(Comparator.comparingDouble(Zombie::getX));
        if (targets.isEmpty()) {
            return;
        }

        Zombie primary = targets.get(0);
        primary.recordDamageSource(plant.getName(), plantCategory(plant), "appease mint huge pea");
        primary.takeDamage(new Damage(300, "appease mint huge pea"));
        splashAppeaseMintChildren(plant, primary);
        recordBoardEvents(board.removeDeadEntities());
    }

    private void splashAppeaseMintChildren(Plant plant, Zombie impactTarget) {
        List<Zombie> nearby = new ArrayList<>();
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || zombie == impactTarget || !board.isZombieOnLawn(zombie)
                    || board.isHypnotized(zombie)) {
                continue;
            }
            if (Math.abs(zombie.getX() - impactTarget.getX()) <= 2.25
                    && Math.abs(zombie.getY() - impactTarget.getY()) <= 1.25) {
                nearby.add(zombie);
            }
        }
        nearby.sort(Comparator.comparingDouble(zombie ->
                Math.hypot(zombie.getX() - impactTarget.getX(),
                        zombie.getY() - impactTarget.getY())));
        int childCount = Math.min(6, nearby.size());
        for (int index = 0; index < childCount; index++) {
            Zombie zombie = nearby.get(index);
            zombie.recordDamageSource(plant.getName(), plantCategory(plant),
                    "appease mint child pea");
            zombie.takeDamage(new Damage(100, "appease mint child pea"));
        }
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
        PlantFoodContext context = createPlantFoodContext();
        for (Plant plant : new ArrayList<>(board.getAllPlants())) {
            if (!belongsToFamily(plant, category) || isPowerMint(plant)) {
                continue;
            }
            plant.resetCooldown();
            activateMintPlantFood(plant, context);
        }
    }

    private boolean belongsToFamily(Plant plant, String category) {
        return plant != null && plant.isAlive() && plant.getType() != null
                && normalizeName(plant.getType().getCategory()).equals(normalizeName(category));
    }

    private boolean isPowerMint(Plant plant) {
        return plant != null && normalizeName(plant.getName()).endsWith("mint");
    }

    private void activateMintPlantFood(Plant plant, PlantFoodContext context) {
        PlantFood previous = activePlantFoods.remove(plant);
        if (previous != null) {
            previous.expire();
        }
        PlantFood plantFood = new PlantFood();
        plant.usePlantFood(plantFood, context);
        if (plantFood.isActive()) {
            activePlantFoods.put(plant, plantFood);
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

    private static final class ScheduledPlantAction {
        private final int dueTick;
        private final Runnable action;

        private ScheduledPlantAction(int dueTick, Runnable action) {
            this.dueTick = dueTick;
            this.action = action;
        }
    }

}
