package models.engine.combat;

import models.core.plant.Plant;
import models.core.projectile.Damage;
import models.core.zombie.Zombie;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DefaultLaneCombatStrategy implements LaneCombatStrategy {
    private static final double MELEE_RANGE = 1.0;

    @Override
    public LaneTickResult updateLane(Lane lane) {
        if (lane == null) {
            throw new IllegalArgumentException("Lane cannot be null.");
        }

        List<Zombie> livingAtStart = collectLivingZombies(lane);
        Map<Plant, Position> plantPositions = capturePlantPositions(lane);
        Set<Plant> consumedPlants = Collections.newSetFromMap(new IdentityHashMap<>());
        List<GameEvent> events = new ArrayList<>();

        updatePlants(lane, livingAtStart, consumedPlants);
        updateZombies(lane, livingAtStart);

        boolean mowerWasReady = lane.getLawnMower().isReady();
        List<Zombie> mowerKilled = handleLaneEnd(lane, livingAtStart);
        boolean mowerTriggered = mowerWasReady && lane.getLawnMower().isTriggered();
        boolean brainEaten = hasLivingZombieAtLaneEnd(livingAtStart);

        Set<Zombie> mowerKilledSet = Collections.newSetFromMap(new IdentityHashMap<>());
        mowerKilledSet.addAll(mowerKilled);

        int zombiesKilled = 0;
        for (Zombie zombie : livingAtStart) {
            if (!zombie.isAlive()) {
                zombiesKilled++;
                events.add(GameEvent.zombieKilled(zombie, mowerKilledSet.contains(zombie)));
            }
        }

        if (mowerTriggered) {
            List<String> killedNames = new ArrayList<>();
            for (Zombie zombie : mowerKilled) {
                killedNames.add(zombie.getName());
            }
            events.add(GameEvent.lawnMowerTriggered(lane.getLaneId(), killedNames));
        }

        int plantsDestroyed = removeDeadPlants(
                lane,
                consumedPlants,
                plantPositions,
                events
        );
        redistributeLivingZombies(lane, livingAtStart);

        return new LaneTickResult(
                zombiesKilled,
                plantsDestroyed,
                mowerTriggered,
                brainEaten,
                events
        );
    }

    private Map<Plant, Position> capturePlantPositions(Lane lane) {
        Map<Plant, Position> positions = new IdentityHashMap<>();
        for (Tile tile : lane.getTiles()) {
            for (Plant plant : tile.getPlants()) {
                if (plant != null) {
                    positions.put(plant, tile.getPosition());
                }
            }
        }
        return positions;
    }

    private void updatePlants(
            Lane lane,
            List<Zombie> zombies,
            Set<Plant> consumedPlants
    ) {
        for (Tile tile : lane.getTiles()) {
            if (tile.isFrozenTerrain()) {
                continue;
            }

            for (Plant plant : new ArrayList<>(tile.getPlants())) {
                if (plant == null || !plant.isAlive()) {
                    continue;
                }

                plant.tickCooldown();
                Zombie target = findTarget(plant, zombies);
                if (target == null) {
                    continue;
                }

                Tile blockingTerrain = findBlockingTerrain(lane, plant, target);
                if (blockingTerrain != null) {
                    int damage = Math.max(1, plant.getAttackDamage());
                    blockingTerrain.damageTerrain(damage, isFirePlant(plant));
                    plant.attack();
                } else {
                    plant.attack(target);
                    applyAdditionalPlantDamage(plant, target, zombies);
                }

                if (isOneUseExplosive(plant) && tile.removePlant(plant)) {
                    consumedPlants.add(plant);
                }
            }
        }
    }

    private void updateZombies(Lane lane, List<Zombie> zombies) {
        List<Zombie> ordered = new ArrayList<>(zombies);
        ordered.sort(Comparator.comparingDouble(Zombie::getX));

        for (Zombie zombie : ordered) {
            if (!zombie.isAlive()) {
                continue;
            }

            Tile currentTile = tileForZombie(lane, zombie);
            if (currentTile != null && currentTile.isFrozenTerrain()) {
                continue;
            }

            zombie.executeAbility();
            if (!zombie.isAlive()) {
                continue;
            }

            currentTile = tileForZombie(lane, zombie);
            Plant plant = currentTile == null ? null : currentTile.getCurrentPlant();

            if (plant != null && plant.isAlive()) {
                zombie.attack(plant);
                if (plant.getReflectDamage() > 0 && zombie.isAlive()) {
                    zombie.takeDamage(new Damage(plant.getReflectDamage(), "reflected"));
                }
            } else {
                zombie.move();
            }
        }
    }

    private Zombie findTarget(Plant plant, List<Zombie> zombies) {
        Zombie selected = null;
        double selectedDistance = Double.MAX_VALUE;
        boolean melee = normalizeCategory(plant).equals("melee");

        for (Zombie zombie : zombies) {
            if (!zombie.isAlive() || zombie.getX() < plant.getX()) {
                continue;
            }

            double distance = zombie.getX() - plant.getX();
            if (melee && distance > MELEE_RANGE) {
                continue;
            }
            if (distance < selectedDistance) {
                selected = zombie;
                selectedDistance = distance;
            }
        }
        return selected;
    }

    private Tile findBlockingTerrain(Lane lane, Plant plant, Zombie target) {
        int startX = Math.max(1, (int) Math.floor(plant.getX()) + 1);
        int endX = Math.min(lane.getWidth(), (int) Math.ceil(target.getX()));
        boolean directHorizontalShot = normalizeCategory(plant).equals("shooter");

        for (int x = startX; x <= endX; x++) {
            Tile tile = lane.getTileAt(x);
            if (tile == null || !tile.hasDamageableTerrain()) {
                continue;
            }
            if (tile.getTileType() == TileType.ICE || directHorizontalShot) {
                return tile;
            }
        }
        return null;
    }

    private void applyAdditionalPlantDamage(
            Plant plant,
            Zombie primaryTarget,
            List<Zombie> zombies
    ) {
        int targetLimit = Math.max(1, plant.getTargetCount() + plant.getPierceCount());
        if (targetLimit <= 1 || plant.getAttackDamage() <= 0) {
            return;
        }

        List<Zombie> ordered = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (zombie != null && zombie.isAlive() && zombie != primaryTarget
                    && zombie.getX() >= plant.getX()) {
                ordered.add(zombie);
            }
        }
        ordered.sort(Comparator.comparingDouble(Zombie::getX));

        int remainingTargets = targetLimit - 1;
        int damage = plant.isBoosted()
                ? plant.getAttackDamage() * 2
                : plant.getAttackDamage();
        for (Zombie zombie : ordered) {
            if (remainingTargets <= 0) {
                break;
            }
            zombie.takeDamage(new Damage(damage, "multi-target"));
            remainingTargets--;
        }
    }

    private List<Zombie> handleLaneEnd(Lane lane, List<Zombie> zombies) {
        if (!hasLivingZombieAtLaneEnd(zombies)) {
            return Collections.emptyList();
        }

        LawnMower mower = lane.getLawnMower();
        if (!mower.isReady()) {
            return Collections.emptyList();
        }

        return mower.destroyZombies(zombies);
    }

    private boolean hasLivingZombieAtLaneEnd(List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (zombie.isAlive() && zombie.getX() <= 0) {
                return true;
            }
        }
        return false;
    }

    private int removeDeadPlants(
            Lane lane,
            Set<Plant> consumedPlants,
            Map<Plant, Position> plantPositions,
            List<GameEvent> events
    ) {
        int destroyed = 0;
        for (Tile tile : lane.getTiles()) {
            for (Plant plant : new ArrayList<>(tile.getPlants())) {
                if (plant == null || plant.isAlive() || !tile.removePlant(plant)) {
                    continue;
                }
                if (!consumedPlants.contains(plant)) {
                    destroyed++;
                    Position position = plantPositions.get(plant);
                    if (position == null) {
                        position = tile.getPosition();
                    }
                    events.add(GameEvent.plantDestroyed(plant.getName(), position));
                }
            }
        }
        return destroyed;
    }

    private void redistributeLivingZombies(Lane lane, List<Zombie> zombies) {
        for (Tile tile : lane.getTiles()) {
            tile.clearZombies();
        }

        for (Zombie zombie : zombies) {
            if (!zombie.isAlive() || zombie.getX() <= 0) {
                continue;
            }
            Tile tile = tileForZombie(lane, zombie);
            if (tile != null) {
                tile.addZombie(zombie);
            }
        }
    }

    private Tile tileForZombie(Lane lane, Zombie zombie) {
        int x = (int) Math.ceil(zombie.getX());
        x = Math.max(1, Math.min(lane.getWidth(), x));
        return lane.getTileAt(x);
    }

    private List<Zombie> collectLivingZombies(Lane lane) {
        List<Zombie> zombies = new ArrayList<>();
        Set<Zombie> seen = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Tile tile : lane.getTiles()) {
            for (Zombie zombie : tile.getZombies()) {
                if (zombie != null && zombie.isAlive() && seen.add(zombie)) {
                    zombies.add(zombie);
                }
            }
        }
        return zombies;
    }

    private boolean isOneUseExplosive(Plant plant) {
        return normalizeCategory(plant).equals("explosive")
                && plant.getAttackDamage() > 0;
    }

    private boolean isFirePlant(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return false;
        }
        String tags = normalizeText(plant.getType().getTags());
        if (tags.contains("fire")) {
            return true;
        }
        String name = normalizeText(plant.getName());
        return name.contains("fire")
                || name.contains("pepper")
                || name.contains("jalapeno")
                || name.contains("torchwood")
                || name.contains("wasabi")
                || name.contains("hot potato");
    }

    private String normalizeCategory(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return "";
        }
        return normalizeText(plant.getType().getCategory());
    }

    private String normalizeText(String value) {
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
