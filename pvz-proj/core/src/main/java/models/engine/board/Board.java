package models.engine.board;

import models.core.plant.Plant;
import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.engine.combat.BoardTickResult;
import models.engine.combat.DefaultLaneCombatStrategy;
import models.engine.combat.LaneTickResult;
import models.engine.events.GameEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Random;


public class Board extends BoardSupport {
    public Board() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Board(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("Board width must be greater than 0.");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Board height must be greater than 0.");
        }

        this.width = width;
        this.height = height;
        this.lanes = new ArrayList<>();
        this.combatStrategy = new DefaultLaneCombatStrategy(this);
        this.lastSlipperyTileByZombie = new IdentityHashMap<>();
        this.terrainEvents = new ArrayList<>();
        this.lastTickResult = BoardTickResult.empty();
        this.totalZombiesKilled = 0;
        this.totalPlantsDestroyed = 0;
        this.brainEaten = false;
        this.graveSpawningAllowed = false;
        this.resourceHandler = null;

        initializeLanes();
    }

    protected void initializeLanes() {
        for (int y = 1; y <= height; y++) {
            Lane lane = new Lane(y, width);
            lane.setCombatStrategy(combatStrategy);
            lanes.add(lane);
        }
    }

    public BoardTickResult updateTicks() {
        int zombiesKilled = 0;
        int plantsDestroyed = 0;
        int mowersTriggered = 0;
        boolean brainWasEaten = false;
        List<GameEvent> events = new ArrayList<>();

        combatStrategy.beginBoardTick();

        for (Lane lane : lanes) {
            LaneTickResult result = lane.updateLaneTicks();
            zombiesKilled += result.getZombiesKilled();
            plantsDestroyed += result.getPlantsDestroyed();
            if (result.isLawnMowerTriggered()) {
                mowersTriggered++;
            }
            brainWasEaten = brainWasEaten || result.isBrainEaten();
            events.addAll(result.getEvents());
        }

        applySlipperyTiles();
        applyAdjacentFireToIce();

        BoardTickResult cleanup = removeDeadEntitiesInternal(false);
        zombiesKilled += cleanup.getZombiesKilled();
        plantsDestroyed += cleanup.getPlantsDestroyed();
        events.addAll(cleanup.getEvents());
        events.addAll(drainTerrainEvents());

        List<GameEvent> unsupportedPlantEvents = new ArrayList<>();
        int unsupportedPlants = removeUnsupportedWaterPlants(unsupportedPlantEvents);
        plantsDestroyed += unsupportedPlants;
        events.addAll(unsupportedPlantEvents);

        totalZombiesKilled += zombiesKilled;
        totalPlantsDestroyed += plantsDestroyed;
        brainEaten = brainEaten || brainWasEaten;
        lastTickResult = new BoardTickResult(
                zombiesKilled,
                plantsDestroyed,
                mowersTriggered,
                brainWasEaten,
                events
        );
        return lastTickResult;
    }

    public BoardTickResult applyAreaDamage(
            Position center,
            int zombieRadius,
            int zombieDamage,
            int plantRadius,
            int plantDamage
    ) {
        if (center == null) {
            throw new IllegalArgumentException("Damage center cannot be null.");
        }
        if (zombieRadius < 0 || plantRadius < 0 || zombieDamage < 0 || plantDamage < 0) {
            throw new IllegalArgumentException("Damage values and radii cannot be negative.");
        }

        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                Position position = tile.getPosition();
                int xDistance = Math.abs(position.getX() - center.getX());
                int yDistance = Math.abs(position.getY() - center.getY());

                if (xDistance <= zombieRadius && yDistance <= zombieRadius) {
                    for (Zombie zombie : new ArrayList<>(tile.getZombies())) {
                        if (isZombieOnLawn(zombie)) {
                            zombie.takeDamage(new Damage(zombieDamage, "radioactive sun"));
                        }
                    }
                }

                if (xDistance <= plantRadius && yDistance <= plantRadius) {
                    for (Plant plant : new ArrayList<>(tile.getPlants())) {
                        if (plant != null && plant.isAlive()) {
                            plant.takeDamage(new Damage(plantDamage, "radioactive sun"));
                        }
                    }
                }
            }
        }

        return removeDeadEntities();
    }

    public BoardTickResult removeDeadEntities() {
        return removeDeadEntitiesInternal(true);
    }

    private BoardTickResult removeDeadEntitiesInternal(boolean updateTotals) {
        List<GameEvent> events = new ArrayList<>();
        int plantsDestroyed = removeDeadPlants(events) + removeUnsupportedWaterPlants(events);
        int zombiesKilled = removeDeadZombies(events);
        if (updateTotals) {
            totalZombiesKilled += zombiesKilled;
            totalPlantsDestroyed += plantsDestroyed;
        }
        return new BoardTickResult(zombiesKilled, plantsDestroyed, 0, false, events);
    }

    private int removeDeadPlants(List<GameEvent> events) {
        int removed = 0;
        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                for (Plant plant : new ArrayList<>(tile.getPlants())) {
                    if (plant == null || plant.isAlive()) continue;
                    combatStrategy.handleExternalPlantDeath(plant, tile.getPosition(), events);
                    if (tile.removePlant(plant)) {
                        removed++;
                        events.add(GameEvent.plantDestroyed(plant.getName(), tile.getPosition()));
                    }
                }
            }
        }
        return removed;
    }

    private int removeDeadZombies(List<GameEvent> events) {
        int removed = 0;
        Set<Zombie> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                for (Zombie zombie : new ArrayList<>(tile.getZombies())) {
                    if (zombie == null || zombie.isAlive()) continue;
                    combatStrategy.handleExternalZombieDeath(zombie);
                    tile.removeZombie(zombie);
                    lastSlipperyTileByZombie.remove(zombie);
                    if (seen.add(zombie)) {
                        removed++;
                        events.add(GameEvent.zombieKilled(zombie, false));
                    }
                }
            }
        }
        return removed;
    }


    public BoardTickResult damageZombiesInArea(
            Position center,
            int xRadius,
            int yRadius,
            int damage,
            String damageType
    ) {
        return damageZombiesInArea(center, xRadius, yRadius, damage, damageType, "", "");
    }

    public BoardTickResult damageZombiesInArea(
            Position center,
            int xRadius,
            int yRadius,
            int damage,
            String damageType,
            String sourcePlantName,
            String sourcePlantCategory
    ) {
        if (center == null || xRadius < 0 || yRadius < 0 || damage < 0) {
            throw new IllegalArgumentException("Damage center, radii and amount are invalid.");
        }

        double xReach = xRadius + 0.5;
        double yReach = yRadius + 0.5;
        for (Zombie zombie : new ArrayList<>(getAllZombies())) {
            if (!isZombieOnLawn(zombie)) {
                continue;
            }
            double xDistance = Math.abs(zombie.getX() - center.getX());
            double yDistance = Math.abs(zombie.getY() - center.getY());
            if (xDistance <= xReach && yDistance <= yReach) {
                zombie.recordDamageSource(sourcePlantName, sourcePlantCategory, damageType);
                zombie.takeDamage(new Damage(damage, damageType));
            }
        }

        return removeDeadEntities();
    }

    public BoardTickResult damageZombiesInLane(int laneNumber, int damage, String damageType) {
        return damageZombiesInLane(laneNumber, damage, damageType, "", "");
    }

    public BoardTickResult damageZombiesInLane(
            int laneNumber,
            int damage,
            String damageType,
            String sourcePlantName,
            String sourcePlantCategory
    ) {
        Lane lane = getLaneAt(laneNumber);

        if (lane == null || damage < 0) {
            return BoardTickResult.empty();
        }

        for (Zombie zombie : new ArrayList<>(lane.getAllZombies())) {
            if (!isZombieOnLawn(zombie)) {
                continue;
            }
            zombie.recordDamageSource(sourcePlantName, sourcePlantCategory, damageType);
            zombie.takeDamage(new Damage(damage, damageType));
        }

        return removeDeadEntities();
    }

    public BoardTickResult damageAllZombies(int damage, String damageType) {
        return damageAllZombies(damage, damageType, "", "");
    }

    public BoardTickResult damageAllZombies(
            int damage,
            String damageType,
            String sourcePlantName,
            String sourcePlantCategory
    ) {
        if (damage < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }

        for (Zombie zombie : new ArrayList<>(getAllZombies())) {
            if (!isZombieOnLawn(zombie)) {
                continue;
            }
            zombie.recordDamageSource(sourcePlantName, sourcePlantCategory, damageType);
            zombie.takeDamage(new Damage(damage, damageType));
        }

        return removeDeadEntities();
    }

    public BoardTickResult damageRandomZombies(
            int hitCount,
            int damage,
            String damageType,
            java.util.Random random
    ) {
        return damageRandomZombies(hitCount, damage, damageType, random, "", "");
    }

    public BoardTickResult damageRandomZombies(
            int hitCount,
            int damage,
            String damageType,
            java.util.Random random,
            String sourcePlantName,
            String sourcePlantCategory
    ) {
        if (hitCount <= 0 || damage <= 0) {
            return BoardTickResult.empty();
        }

        java.util.Random generator = random == null ? new java.util.Random() : random;
        List<Zombie> living = new ArrayList<>();
        for (Zombie zombie : getAllZombies()) {
            if (isZombieOnLawn(zombie)) {
                living.add(zombie);
            }
        }

        for (int index = 0; index < hitCount && !living.isEmpty(); index++) {
            Zombie target = living.get(generator.nextInt(living.size()));
            target.recordDamageSource(sourcePlantName, sourcePlantCategory, damageType);
            target.takeDamage(new Damage(damage, damageType));

            if (!target.isAlive()) {
                living.remove(target);
            }
        }

        return removeDeadEntities();
    }

    public void freezeAllZombies(int ticks) {
        for (Zombie zombie : getAllZombies()) {
            if (isZombieOnLawn(zombie)) {
                combatStrategy.applyFreeze(zombie, ticks);
            }
        }
    }

    public void freezeInitialZombie(Zombie zombie) {
        combatStrategy.freezeInitialZombie(zombie);
    }

    public int getInitialZombieIceHealth(Zombie zombie) {
        return combatStrategy.getInitialZombieIceHealth(zombie);
    }

    public boolean isInitialZombieFrozen(Zombie zombie) {
        return combatStrategy.isInitialZombieFrozen(zombie);
    }

    public void applyFreeze(Zombie zombie, int ticks) {
        if (isZombieOnLawn(zombie)) {
            combatStrategy.applyFreeze(zombie, ticks);
        }
    }

    public void applyChill(Zombie zombie, int ticks) {
        if (isZombieOnLawn(zombie)) {
            combatStrategy.applyChill(zombie, ticks);
        }
    }

    public void applyPoison(Zombie zombie, int damagePerTick, int ticks) {
        if (isZombieOnLawn(zombie)) {
            combatStrategy.applyPoison(zombie, damagePerTick, ticks);
        }
    }

    public void applyButter(Zombie zombie, int ticks) {
        if (isZombieOnLawn(zombie)) {
            combatStrategy.applyButterStun(zombie, ticks);
        }
    }

    public void hypnotizeZombie(Zombie zombie) {
        combatStrategy.hypnotize(zombie);
    }

    public boolean isHypnotized(Zombie zombie) {
        return combatStrategy.isHypnotized(zombie);
    }

    public List<String> getZombieEffects(Zombie zombie) {
        return combatStrategy.getActiveEffects(zombie);
    }

    public void activatePlantFamilyBoost(String category, int ticks) {
        combatStrategy.activateFamilyBoost(category, ticks);
    }

    public boolean isPlantFamilyBoosted(String category) {
        return combatStrategy.isFamilyBoosted(category);
    }

    public int meltTerrainArea(Position center, int radius) {
        if (center == null || radius < 0) {
            return 0;
        }
        int melted = 0;
        for (int y = Math.max(1, center.getY() - radius);
             y <= Math.min(height, center.getY() + radius); y++) {
            for (int x = Math.max(1, center.getX() - radius);
                 x <= Math.min(width, center.getX() + radius); x++) {
                Tile tile = getTileAt(new Position(x, y));
                if (tile != null && tile.getTileType() == TileType.ICE
                        && tile.damageTerrain(Integer.MAX_VALUE, true)) {
                    melted++;
                }
            }
        }
        return melted;
    }

    public int meltTerrainInLane(int laneNumber) {
        Lane lane = getLaneAt(laneNumber);
        if (lane == null) {
            return 0;
        }
        int melted = 0;
        for (Tile tile : lane.getTiles()) {
            if (tile.getTileType() == TileType.ICE
                    && tile.damageTerrain(Integer.MAX_VALUE, true)) {
                melted++;
            }
        }
        return melted;
    }

    public boolean removeTerrain(Position position, TileType expectedType) {
        Tile tile = getTileAt(position);
        if (tile == null || expectedType == null) {
            return false;
        }
        TileType previousType = tile.getTileType();
        boolean matches = previousType == expectedType
                || expectedType == TileType.GRAVE && tile.isGraveTerrain();
        if (!matches) {
            return false;
        }
        tile.setTileType(TileType.NORMAL);
        recordTerrainReward(previousType, position);
        return true;
    }

    private List<GameEvent> drainTerrainEvents() {
        List<GameEvent> events = new ArrayList<>(terrainEvents);
        terrainEvents.clear();
        return events;
    }

    public BoardTickResult stabilizeTerrain() {
        List<GameEvent> events = new ArrayList<>();
        int removed = removeUnsupportedWaterPlants(events);
        events.addAll(drainTerrainEvents());
        if (removed > 0) {
            totalPlantsDestroyed += removed;
        }
        return new BoardTickResult(0, removed, 0, false, events);
    }

}
