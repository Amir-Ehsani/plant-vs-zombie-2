package models.engine.combat;

import models.core.plant.Plant;
import models.core.projectile.Damage;
import models.core.zombie.Armor;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.core.zombie.ZombieType;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.engine.events.GameEvent;
import models.entities.LawnMower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;


abstract class LaneCombatTerrainSupport extends LaneCombatState {
    protected LaneCombatTerrainSupport(Board board) {
        super(board);
    }

    protected LaneCombatTerrainSupport(Board board, Random random) {
        super(board, random);
    }


    protected Plant findTorchwoodBetween(Plant plant, Zombie target, Lane lane) {
        if (lane == null || plant == null || target == null || !isPeaPlant(plant)) {
            return null;
        }
        int startX = (int) Math.floor(plant.getX()) + 1;
        int endX = (int) Math.ceil(target.getX()) - 1;
        for (int x = startX; x <= endX; x++) {
            Tile tile = lane.getTileAt(x);
            if (tile == null) {
                continue;
            }
            for (Plant candidate : tile.getPlants()) {
                if (normalizeText(candidate.getName()).equals("torchwood")) {
                    return candidate;
                }
            }
        }
        return null;
    }

    protected boolean isPeaPlant(Plant plant) {
        String name = normalizeText(plant == null ? null : plant.getName());
        return name.equals("peashooter")
            || name.equals("repeater")
            || name.equals("threepeater")
            || name.equals("split pea")
            || name.equals("pea pod")
            || name.equals("mega gatling pea");
    }

    protected Tile findBlockingTerrain(Lane lane, Plant plant, Zombie target) {
        int startX = Math.max(1, (int) Math.floor(plant.getX()) + 1);
        String category = normalizeCategory(plant);
        boolean ignoresTerrain = category.equals("lobber")
            || category.equals("homing")
            || category.equals("strike through");
        if (ignoresTerrain) {
            return null;
        }

        // Terrain only blocks a direct shot while it is physically closer to the
        // shooter than the zombie. Using ceil(targetX) used to keep selecting a
        // grave after a zombie had already walked in front of that grave.
        double targetX = target.getX();
        for (int x = startX; x <= lane.getWidth(); x++) {
            if (x >= targetX) {
                break;
            }
            Tile tile = lane.getTileAt(x);
            if (tile != null && tile.hasDamageableTerrain()) {
                return tile;
            }
        }
        return null;
    }

    protected List<Zombie> handleLaneEnd(Lane lane, List<Zombie> zombies) {
        LawnMower mower = lane.getLawnMower();
        if (mower.isReady() && hasLivingZombieAtLaneEnd(zombies)) {
            mower.trigger();
        }
        if (!mower.isMoving()) {
            return Collections.emptyList();
        }
        return mower.advanceAndDestroy(zombies);
    }

    protected boolean hasLivingZombieAtLaneEnd(List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (zombie.isAlive() && zombie.getX() <= 0) {
                return true;
            }
        }
        return false;
    }

    protected int removeDeadPlants(
        Lane lane,
        Set<Plant> consumedPlants,
        Map<Plant, Position> plantPositions,
        List<GameEvent> events
    ) {
        int destroyed = 0;
        for (Tile tile : lane.getTiles()) {
            for (Plant plant : new ArrayList<>(tile.getPlants())) {
                if (plant == null || plant.isAlive()) {
                    continue;
                }
                Position position = plantPositions.getOrDefault(plant, tile.getPosition());
                triggerPlantDeathEffect(plant, position, events);
                if (!tile.removePlant(plant)) {
                    continue;
                }
                plantStates.remove(plant);
                if (!consumedPlants.contains(plant)) {
                    destroyed++;
                    events.add(GameEvent.plantDestroyed(plant.getName(), position));
                }
            }
        }
        return destroyed;
    }

    protected void triggerPlantDeathEffect(Plant plant, Position position, List<GameEvent> events) {
        PlantRuntimeState state = plantStateOf(plant);
        if (state.deathEffectHandled) {
            return;
        }
        state.deathEffectHandled = true;
        String name = normalizeText(plant.getName());

        if (name.equals("explode o nut") || plant.getExplodeDamage() > 0) {
            int damage = Math.max(1800, plant.getExplodeDamage());
            damageArea(position, 1, 1, damage, "death explosion", plant, null);
        } else if (plant.hasAoeOnDeath()) {
            int damage = Math.max(20, plant.getAttackDamage());
            damageArea(position, 1, 1, damage, "death area", plant, null);
        }
    }

    protected void redistributeLivingZombies(Lane lane, List<Zombie> zombies) {
        List<Zombie> all = new ArrayList<>(zombies);
        for (Tile tile : lane.getTiles()) {
            for (Zombie zombie : tile.getZombies()) {
                if (zombie != null && !all.contains(zombie)) {
                    all.add(zombie);
                }
            }
            tile.clearZombies();
        }

        for (Zombie zombie : all) {
            if (!zombie.isAlive() || zombie.getX() <= 0) {
                continue;
            }
            ZombieRuntimeState state = stateOf(zombie);
            if (state.pendingLaneShift != 0 && board != null) {
                int targetLaneNumber = lane.getLaneId() + state.pendingLaneShift;
                Lane targetLane = board.getLaneAt(targetLaneNumber);
                if (targetLane != null) {
                    zombie.moveBy(0, state.pendingLaneShift);
                }
                state.pendingLaneShift = 0;
            }
            int targetLaneNumber = board == null
                ? lane.getLaneId()
                : Math.max(1, Math.min(board.getHeight(), (int) Math.round(zombie.getY())));
            Lane targetLane = board == null ? lane : board.getLaneAt(targetLaneNumber);
            if (targetLane == null) {
                continue;
            }
            Tile targetTile = targetLane.getTileAt(clampX(targetLane, zombie));
            if (targetTile != null) {
                targetTile.addZombie(zombie);
            }
        }
    }

}
