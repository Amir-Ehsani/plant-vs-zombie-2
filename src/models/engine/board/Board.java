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

public class Board {
    private static final int DEFAULT_WIDTH = 9;
    private static final int DEFAULT_HEIGHT = 5;
    private static final int TICKS_PER_SECOND = 10;
    private static final int ADJACENT_FIRE_MELT_PER_SECOND = 60;
    private static final int ADJACENT_FIRE_MELT_PER_TICK =
            Math.max(1, ADJACENT_FIRE_MELT_PER_SECOND / TICKS_PER_SECOND);

    private final int width;
    private final int height;
    private final List<Lane> lanes;
    private final DefaultLaneCombatStrategy combatStrategy;
    private final Map<Zombie, Position> lastSlipperyTileByZombie;
    private BoardTickResult lastTickResult;
    private int totalZombiesKilled;
    private int totalPlantsDestroyed;
    private boolean brainEaten;

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
        this.lastTickResult = BoardTickResult.empty();
        this.totalZombiesKilled = 0;
        this.totalPlantsDestroyed = 0;
        this.brainEaten = false;

        initializeLanes();
    }

    private void initializeLanes() {
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
                        if (zombie != null && zombie.isAlive()) {
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
        int zombiesKilled = 0;
        int plantsDestroyed = 0;
        List<GameEvent> events = new ArrayList<>();

        // Plant death effects may kill zombies on another tile or lane. Remove all
        // dead plants first, execute every death effect, and only then collect dead
        // zombies. This guarantees that an explosion is accounted for in the same
        // cleanup pass instead of one tick later.
        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                for (Plant plant : new ArrayList<>(tile.getPlants())) {
                    if (plant == null || plant.isAlive()) {
                        continue;
                    }
                    combatStrategy.handleExternalPlantDeath(plant, tile.getPosition(), events);
                    if (tile.removePlant(plant)) {
                        plantsDestroyed++;
                        events.add(GameEvent.plantDestroyed(
                                plant.getName(),
                                tile.getPosition()
                        ));
                    }
                }
            }
        }

        Set<Zombie> removedZombies = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                for (Zombie zombie : new ArrayList<>(tile.getZombies())) {
                    if (zombie == null || zombie.isAlive()) {
                        continue;
                    }
                    tile.removeZombie(zombie);
                    lastSlipperyTileByZombie.remove(zombie);
                    if (removedZombies.add(zombie)) {
                        zombiesKilled++;
                        events.add(GameEvent.zombieKilled(zombie, false));
                    }
                }
            }
        }

        plantsDestroyed += removeUnsupportedWaterPlants(events);
        if (updateTotals) {
            totalZombiesKilled += zombiesKilled;
            totalPlantsDestroyed += plantsDestroyed;
        }

        return new BoardTickResult(
                zombiesKilled,
                plantsDestroyed,
                0,
                false,
                events
        );
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<Lane> getLanes() {
        return Collections.unmodifiableList(lanes);
    }

    public Lane getLaneAt(int y) {
        if (y <= 0 || y > height) {
            return null;
        }
        return lanes.get(y - 1);
    }

    public Tile getTileAt(Position position) {
        if (position == null) {
            return null;
        }

        Lane lane = getLaneAt(position.getY());
        if (lane == null) {
            return null;
        }
        return lane.getTileAt(position.getX());
    }

    public boolean isValidPosition(Position position) {
        return getTileAt(position) != null;
    }

    /**
     * Compatibility check for callers that do not yet have a Plant instance.
     * Water and stacking require the overload that receives the plant.
     */
    public boolean canPlacePlant(Position position) {
        Tile tile = getTileAt(position);
        return tile != null && tile.isPlantable() && !tile.hasPlant();
    }

    public boolean canPlacePlant(Plant plant, Position position) {
        Tile tile = getTileAt(position);
        return tile != null && tile.canPlacePlant(plant);
    }

    public boolean placePlant(Plant plant, Position position) {
        if (plant == null || !canPlacePlant(plant, position)) {
            return false;
        }

        Tile tile = getTileAt(position);
        tile.placePlant(plant);
        return true;
    }

    /** Removes the uppermost plant at the position. */
    public Plant removePlant(Position position) {
        Tile tile = getTileAt(position);
        if (tile == null || !tile.hasPlant()) {
            return null;
        }
        return tile.removePlant();
    }

    /** Removes one exact plant instance without disturbing another layer. */
    public boolean removePlant(Position position, Plant plant) {
        Tile tile = getTileAt(position);
        return tile != null && tile.removePlant(plant);
    }

    public boolean setTileType(Position position, TileType tileType) {
        Tile tile = getTileAt(position);
        if (tile == null || tileType == null) {
            return false;
        }
        tile.setTileType(tileType);
        return true;
    }

    public boolean damageTerrain(Position position, int damage, boolean fireDamage) {
        Tile tile = getTileAt(position);
        return tile != null && tile.damageTerrain(damage, fireDamage);
    }

    public BoardTickResult damageZombiesInArea(
            Position center,
            int xRadius,
            int yRadius,
            int damage,
            String damageType
    ) {
        if (center == null || xRadius < 0 || yRadius < 0 || damage < 0) {
            throw new IllegalArgumentException("Damage center, radii and amount are invalid.");
        }
        for (Zombie zombie : new ArrayList<>(getAllZombies())) {
            int x = Math.max(1, Math.min(width, (int) Math.ceil(zombie.getX())));
            int y = Math.max(1, Math.min(height, (int) Math.round(zombie.getY())));
            if (Math.abs(x - center.getX()) <= xRadius
                    && Math.abs(y - center.getY()) <= yRadius) {
                zombie.takeDamage(new Damage(damage, damageType));
            }
        }
        return removeDeadEntities();
    }

    public BoardTickResult damageZombiesInLane(int laneNumber, int damage, String damageType) {
        Lane lane = getLaneAt(laneNumber);
        if (lane == null || damage < 0) {
            return BoardTickResult.empty();
        }
        for (Zombie zombie : new ArrayList<>(lane.getAllZombies())) {
            zombie.takeDamage(new Damage(damage, damageType));
        }
        return removeDeadEntities();
    }

    public BoardTickResult damageAllZombies(int damage, String damageType) {
        if (damage < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        for (Zombie zombie : new ArrayList<>(getAllZombies())) {
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
        if (hitCount <= 0 || damage <= 0) {
            return BoardTickResult.empty();
        }
        java.util.Random generator = random == null ? new java.util.Random() : random;
        List<Zombie> living = new ArrayList<>(getAllZombies());
        for (int index = 0; index < hitCount && !living.isEmpty(); index++) {
            Zombie target = living.get(generator.nextInt(living.size()));
            target.takeDamage(new Damage(damage, damageType));
            if (!target.isAlive()) {
                living.remove(target);
            }
        }
        return removeDeadEntities();
    }

    public void freezeAllZombies(int ticks) {
        for (Zombie zombie : getAllZombies()) {
            combatStrategy.applyFreeze(zombie, ticks);
        }
    }

    public void applyFreeze(Zombie zombie, int ticks) {
        combatStrategy.applyFreeze(zombie, ticks);
    }

    public void applyChill(Zombie zombie, int ticks) {
        combatStrategy.applyChill(zombie, ticks);
    }

    public void applyPoison(Zombie zombie, int damagePerTick, int ticks) {
        combatStrategy.applyPoison(zombie, damagePerTick, ticks);
    }

    public void hypnotizeZombie(Zombie zombie) {
        combatStrategy.hypnotize(zombie);
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
        if (tile == null || expectedType == null || tile.getTileType() != expectedType) {
            return false;
        }
        tile.setTileType(TileType.NORMAL);
        return true;
    }

    public BoardTickResult stabilizeTerrain() {
        List<GameEvent> events = new ArrayList<>();
        int removed = removeUnsupportedWaterPlants(events);
        if (removed > 0) {
            totalPlantsDestroyed += removed;
        }
        return new BoardTickResult(0, removed, 0, false, events);
    }

    public Tile getTileContainingZombie(Zombie zombie) {
        return findTileContainingZombie(zombie);
    }

    public List<Zombie> getAllZombies() {
        List<Zombie> zombies = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Zombie zombie : lane.getAllZombies()) {
                if (!zombies.contains(zombie)) {
                    zombies.add(zombie);
                }
            }
        }
        return Collections.unmodifiableList(zombies);
    }

    public List<Plant> getAllPlants() {
        List<Plant> plants = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                for (Plant plant : tile.getPlants()) {
                    if (plant != null && plant.isAlive()) {
                        plants.add(plant);
                    }
                }
            }
        }
        return Collections.unmodifiableList(plants);
    }

    public int getActiveZombieCount() {
        return getAllZombies().size();
    }

    public int getPlantCount() {
        return getAllPlants().size();
    }

    public int getTotalZombiesKilled() {
        return totalZombiesKilled;
    }

    public int getTotalPlantsDestroyed() {
        return totalPlantsDestroyed;
    }

    public boolean hasBrainBeenEaten() {
        return brainEaten;
    }

    public int destroyAllZombies() {
        int destroyed = 0;
        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                for (Zombie zombie : new ArrayList<>(tile.getZombies())) {
                    if (zombie != null && zombie.isAlive()) {
                        zombie.kill();
                        destroyed++;
                    }
                    lastSlipperyTileByZombie.remove(zombie);
                }
                tile.clearZombies();
            }
        }
        totalZombiesKilled += destroyed;
        return destroyed;
    }

    public BoardTickResult getLastTickResult() {
        return lastTickResult;
    }

    private void applySlipperyTiles() {
        for (Zombie zombie : new ArrayList<>(getAllZombies())) {
            Tile currentTile = findTileContainingZombie(zombie);
            if (currentTile == null || !zombie.isAlive()) {
                lastSlipperyTileByZombie.remove(zombie);
                continue;
            }

            TileType type = currentTile.getTileType();
            if ((type != TileType.SLIPPERY_UP && type != TileType.SLIPPERY_DOWN)
                    || ignoresSlipperyTile(zombie)) {
                lastSlipperyTileByZombie.remove(zombie);
                continue;
            }

            Position currentPosition = currentTile.getPosition();
            Position previousTrigger = lastSlipperyTileByZombie.get(zombie);
            if (currentPosition.equals(previousTrigger)) {
                continue;
            }

            int laneDelta = type == TileType.SLIPPERY_UP ? -1 : 1;
            int targetLaneNumber = currentPosition.getY() + laneDelta;
            Lane targetLane = getLaneAt(targetLaneNumber);
            if (targetLane == null) {
                lastSlipperyTileByZombie.put(zombie, currentPosition);
                continue;
            }

            int targetX = Math.max(1, Math.min(width, (int) Math.ceil(zombie.getX())));
            Tile targetTile = targetLane.getTileAt(targetX);
            if (targetTile == null) {
                continue;
            }

            currentTile.removeZombie(zombie);
            zombie.moveBy(0, laneDelta);
            targetTile.addZombie(zombie);
            lastSlipperyTileByZombie.put(zombie, currentPosition);
        }
    }

    private Tile findTileContainingZombie(Zombie zombie) {
        if (zombie == null) {
            return null;
        }
        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                if (tile.getZombies().contains(zombie)) {
                    return tile;
                }
            }
        }
        return null;
    }

    private boolean ignoresSlipperyTile(Zombie zombie) {
        String name = normalize(zombie == null ? null : zombie.getName());
        String id = zombie == null || zombie.getType() == null
                ? ""
                : normalize(zombie.getType().getId());
        return name.contains("dodo") || id.contains("dodo");
    }

    private void applyAdjacentFireToIce() {
        for (Lane lane : lanes) {
            for (Tile iceTile : lane.getTiles()) {
                if (!iceTile.isFrozenTerrain()) {
                    continue;
                }
                if (hasAdjacentFirePlant(iceTile.getPosition())) {
                    iceTile.damageTerrain(ADJACENT_FIRE_MELT_PER_TICK, false);
                }
            }
        }
    }

    private boolean hasAdjacentFirePlant(Position center) {
        for (int y = Math.max(1, center.getY() - 1);
             y <= Math.min(height, center.getY() + 1);
             y++) {
            for (int x = Math.max(1, center.getX() - 1);
                 x <= Math.min(width, center.getX() + 1);
                 x++) {
                if (x == center.getX() && y == center.getY()) {
                    continue;
                }
                Tile tile = getTileAt(new Position(x, y));
                if (tile == null || tile.isFrozenTerrain()) {
                    continue;
                }
                for (Plant plant : tile.getPlants()) {
                    if (plant != null && plant.isAlive() && isFirePlant(plant)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isFirePlant(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return false;
        }
        String tags = normalize(plant.getType().getTags());
        if (tags.contains("fire")) {
            return true;
        }
        String name = normalize(plant.getName());
        return name.contains("fire")
                || name.contains("pepper")
                || name.contains("jalapeno")
                || name.contains("torchwood")
                || name.contains("wasabi")
                || name.contains("hot potato");
    }

    private int removeUnsupportedWaterPlants(List<GameEvent> events) {
        int removedCount = 0;
        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                for (Plant plant : tile.removeUnsupportedWaterPlants()) {
                    removedCount++;
                    events.add(GameEvent.plantDestroyed(plant.getName(), tile.getPosition()));
                }
            }
        }
        return removedCount;
    }

    private String normalize(String value) {
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
