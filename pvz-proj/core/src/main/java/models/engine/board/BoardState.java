package models.engine.board;

import models.core.plant.Plant;
import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.engine.events.GameEvent;
import models.engine.combat.BoardTickResult;
import models.engine.combat.DefaultLaneCombatStrategy;
import models.engine.combat.LaneTickResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Random;


abstract class BoardState {
    protected static final int DEFAULT_WIDTH = 9;
    protected static final int DEFAULT_HEIGHT = 5;
    protected static final int TICKS_PER_SECOND = 10;
    protected static final int ADJACENT_FIRE_MELT_PER_SECOND = 60;
    protected static final int ADJACENT_FIRE_MELT_PER_TICK =
            Math.max(1, ADJACENT_FIRE_MELT_PER_SECOND / TICKS_PER_SECOND);

    protected int width;
    protected int height;
    protected List<Lane> lanes;
    protected DefaultLaneCombatStrategy combatStrategy;
    protected Map<Zombie, Position> lastSlipperyTileByZombie;
    protected BoardTickResult lastTickResult;
    protected int totalZombiesKilled;
    protected int totalPlantsDestroyed;
    protected boolean brainEaten;
    protected boolean graveSpawningAllowed;
    protected BoardResourceHandler resourceHandler;
    protected List<GameEvent> terrainEvents;


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

    public Plant removePlant(Position position) {
        Tile tile = getTileAt(position);
        if (tile == null || !tile.hasPlant()) {
            return null;
        }
        return tile.removePlant();
    }

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
        if (tile == null) {
            return false;
        }
        TileType previousType = tile.getTileType();
        boolean destroyed = tile.damageTerrain(damage, fireDamage);
        if (destroyed) {
            recordTerrainReward(previousType, position);
        }
        return destroyed;
    }

    protected void recordTerrainReward(TileType previousType, Position position) {
        if (previousType == TileType.SUN_GRAVE) {
            terrainEvents.add(GameEvent.rewardDropped("sun", 50, position));
        } else if (previousType == TileType.PLANT_FOOD_GRAVE) {
            terrainEvents.add(GameEvent.rewardDropped("plant_food", 1, position));
        }
    }
}
