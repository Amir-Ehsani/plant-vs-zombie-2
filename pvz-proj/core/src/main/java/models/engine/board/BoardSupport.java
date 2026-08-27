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


abstract class BoardSupport extends BoardState {
    public void setGraveSpawningAllowed(boolean allowed) {
        graveSpawningAllowed = allowed;
    }

    public boolean isGraveSpawningAllowed() {
        return graveSpawningAllowed;
    }

    public void setResourceHandler(BoardResourceHandler resourceHandler) {
        this.resourceHandler = resourceHandler;
    }

    public int stealStoredSun(int amount) {
        if (resourceHandler == null || amount <= 0) {
            return 0;
        }
        return Math.max(0, resourceHandler.stealStoredSun(amount));
    }

    public int stealLooseSuns() {
        return resourceHandler == null ? 0 : Math.max(0, resourceHandler.stealLooseSuns());
    }

    public void restoreSun(int amount) {
        if (resourceHandler != null && amount > 0) {
            resourceHandler.restoreSun(amount);
        }
    }

    public boolean moveZombieToLane(Zombie zombie, int targetLaneNumber) {
        if (zombie == null || !zombie.isAlive()) {
            return false;
        }
        Lane targetLane = getLaneAt(targetLaneNumber);
        Tile sourceTile = findTileContainingZombie(zombie);
        if (targetLane == null || sourceTile == null) {
            return false;
        }
        Tile targetTile = targetLane.getTileAt(Math.max(1, Math.min(width, (int) Math.ceil(zombie.getX()))));
        if (targetTile == null) {
            return false;
        }
        sourceTile.removeZombie(zombie);
        zombie.moveBy(0, targetLaneNumber - zombie.getY());
        targetTile.addZombie(zombie);
        return true;
    }

    public boolean shiftZombieToAdjacentLane(Zombie zombie, Random random) {
        if (zombie == null) {
            return false;
        }
        int currentLane = Math.max(1, Math.min(height, (int) Math.round(zombie.getY())));
        boolean up = getLaneAt(currentLane - 1) != null;
        boolean down = getLaneAt(currentLane + 1) != null;
        if (!up && !down) {
            return false;
        }
        int target;
        if (up && down) {
            target = random != null && random.nextBoolean() ? currentLane - 1 : currentLane + 1;
        } else {
            target = up ? currentLane - 1 : currentLane + 1;
        }
        return moveZombieToLane(zombie, target);
    }

    public boolean movePlant(Position source, Position target, Plant plant) {
        if (source == null || target == null || plant == null) {
            return false;
        }
        Tile sourceTile = getTileAt(source);
        Tile targetTile = getTileAt(target);
        if (!canMovePlant(sourceTile, targetTile, plant)) {
            return false;
        }
        if (!sourceTile.removePlant(plant)) {
            return false;
        }
        targetTile.placePlant(plant);
        plant.moveTo(target.getX(), target.getY());
        return true;
    }

    private boolean canMovePlant(Tile sourceTile, Tile targetTile, Plant plant) {
        return sourceTile != null
                && targetTile != null
                && sourceTile.hasPlant(plant)
                && targetTile.canPlacePlant(plant);
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

    protected void applySlipperyTiles() {
        for (Zombie zombie : new ArrayList<>(getAllZombies())) {
            Tile currentTile = findTileContainingZombie(zombie);
            if (currentTile == null || !zombie.isAlive()) {
                lastSlipperyTileByZombie.remove(zombie);
                continue;
            }

            TileType type = currentTile.getTileType();
            if ((type != TileType.SLIPPERY_UP && type != TileType.SLIPPERY_DOWN)
                    || ignoresSlipperyTile(zombie)
                    || combatStrategy.isInitialZombieFrozen(zombie)) {
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

            int targetX = Math.max(1, Math.min(width, (int) Math.ceil(zombie.getX()) - 1));
            Tile targetTile = targetLane.getTileAt(targetX);
            if (targetTile == null) {
                continue;
            }

            currentTile.removeZombie(zombie);
            zombie.moveBy(-1, laneDelta);
            targetTile.addZombie(zombie);
            lastSlipperyTileByZombie.put(zombie, currentPosition);
        }
    }

    protected Tile findTileContainingZombie(Zombie zombie) {
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

    protected boolean ignoresSlipperyTile(Zombie zombie) {
        String name = normalize(zombie == null ? null : zombie.getName());
        String id = zombie == null || zombie.getType() == null
                ? ""
                : normalize(zombie.getType().getId());
        return name.contains("dodo") || id.contains("dodo");
    }

    protected void applyAdjacentFireToIce() {
        for (Lane lane : lanes) {
            for (Tile iceTile : lane.getTiles()) {
                if (iceTile.isFrozenTerrain() && hasAdjacentFirePlant(iceTile.getPosition())) {
                    iceTile.damageTerrain(ADJACENT_FIRE_MELT_PER_TICK, false);
                }
                for (Plant plant : iceTile.getPlants()) {
                    if (plant != null && plant.isAlive() && plant.isFrozenByZombie()
                            && hasAdjacentFirePlant(iceTile.getPosition())) {
                        plant.damageIce(ADJACENT_FIRE_MELT_PER_TICK, false);
                    }
                }
            }
        }
    }

    protected boolean hasAdjacentFirePlant(Position center) {
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

    protected boolean isFirePlant(Plant plant) {
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

    protected int removeUnsupportedWaterPlants(List<GameEvent> events) {
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

    protected String normalize(String value) {
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
