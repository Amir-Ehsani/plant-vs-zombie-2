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


abstract class LaneCombatTargetSupport extends LaneCombatTerrainSupport {
    protected LaneCombatTargetSupport(Board board) {
        super(board);
    }

    protected LaneCombatTargetSupport(Board board, Random random) {
        super(board, random);
    }

    protected double scaledBaseSpeed(Zombie zombie) {
        if (zombie == null || zombie.getType() == null) {
            return 0;
        }
        return zombie.getType().getSpeed() * GLOBAL_ZOMBIE_SPEED_SCALE;
    }

    protected void spawnZombie(String zombieName, double x, int laneNumber) {
        if (board == null) {
            return;
        }
        try {
            Zombie spawned = new ZombieFactory().createZombie(zombieName, x, laneNumber);
            Lane lane = board.getLaneAt(laneNumber);
            if (lane == null) {
                return;
            }
            Tile tile = lane.getTileAt(Math.max(1, Math.min(lane.getWidth(), (int) Math.ceil(x))));
            if (tile != null) {
                tile.addZombie(spawned);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    protected Plant nearestPlantInLane(
            Lane lane,
            double zombieX,
            double maximumDistance,
            boolean excludeDisabled
    ) {
        Plant result = null;
        double minimum = Double.MAX_VALUE;
        for (Tile tile : lane.getTiles()) {
            for (Plant plant : tile.getPlants()) {
                if (!plant.isAlive() || plant.isTransformedToCat()
                        || excludeDisabled && plant.isDisabled()) {
                    continue;
                }
                double distance = Math.abs(zombieX - plant.getX());
                if (distance <= maximumDistance && distance < minimum) {
                    minimum = distance;
                    result = plant;
                }
            }
        }
        return result;
    }

    protected boolean attackDisabledPlantObstruction(Lane lane, Plant attacker) {
        if (lane == null || attacker == null || normalizeCategory(attacker).equals("lobber")) {
            return false;
        }
        Plant target = null;
        double minimum = Double.MAX_VALUE;
        for (Tile tile : lane.getTiles()) {
            for (Plant plant : tile.getPlants()) {
                if (plant == attacker || !plant.isAlive() || !plant.isDisabled()
                        || plant.getX() < attacker.getX()) {
                    continue;
                }
                double distance = plant.getX() - attacker.getX();
                if (distance <= resolveMaximumRange(attacker) && distance < minimum) {
                    target = plant;
                    minimum = distance;
                }
            }
        }
        if (target == null) {
            return false;
        }
        if (target.isCoveredByOctopus()) {
            target.damageOctopus();
        } else if (target.isFrozenByZombie()) {
            target.removeIceHit();
        }
        return true;
    }

    protected boolean isJugglerSpinning(ZombieRuntimeState state) {
        return state != null && state.jugglerSpinTicks > 0;
    }

    protected boolean isReflectableProjectile(Plant source, String damageType) {
        if (source == null) {
            return false;
        }
        String category = normalizeCategory(source);
        String type = normalizeText(damageType);
        return (category.equals("shooter") || category.equals("strike through"))
                && !type.contains("fire")
                && !type.contains("explosive")
                && !type.contains("splash");
    }

    protected boolean isIceDamage(String value) {
        String normalized = normalizeText(value);
        return normalized.contains("ice") || normalized.contains("frozen")
                || normalized.contains("snow") || normalized.contains("chill");
    }

    protected void maybeAttractToSweetPotato(
            Lane currentLane,
            Zombie zombie,
            ZombieRuntimeState state
    ) {
        if (board == null || state.pendingLaneShift != 0) {
            return;
        }
        for (int delta : new int[]{-1, 1}) {
            Lane adjacent = board.getLaneAt(currentLane.getLaneId() + delta);
            if (adjacent == null) {
                continue;
            }
            for (Tile tile : adjacent.getTiles()) {
                if (!tile.hasPlantNamed("Sweet Potato")) {
                    continue;
                }
                if (Math.abs(tile.getPosition().getX() - zombie.getX()) <= 2.0) {
                    state.pendingLaneShift = delta;
                    return;
                }
            }
        }
    }

    protected void scheduleGarlicLaneShift(
            Lane lane,
            Zombie zombie,
            ZombieRuntimeState state
    ) {
        if (board == null || state.pendingLaneShift != 0) {
            return;
        }
        boolean upAvailable = board.getLaneAt(lane.getLaneId() - 1) != null;
        boolean downAvailable = board.getLaneAt(lane.getLaneId() + 1) != null;
        if (!upAvailable && !downAvailable) {
            return;
        }
        if (upAvailable && downAvailable) {
            state.pendingLaneShift = random.nextBoolean() ? -1 : 1;
        } else {
            state.pendingLaneShift = upAvailable ? -1 : 1;
        }
    }

    protected boolean fliesOverPlant(Zombie zombie, Plant plant) {
        String zombieName = normalizeText(zombie == null ? null : zombie.getName());
        if (!zombieName.contains("dodo")) {
            return false;
        }
        String plantName = normalizeText(plant == null ? null : plant.getName());
        if (plantName.equals("tall nut")) {
            return false;
        }
        String category = normalizeCategory(plant);
        String tags = plant == null || plant.getType() == null
                ? "" : normalizeText(plant.getType().getTags());
        return category.contains("wall")
                || category.contains("defensive")
                || category.equals("explosive")
                || plant.getMaxHp() >= 1000
                || tags.contains("trap")
                || tags.contains("move zombie")
                || tags.contains("explosive");
    }

    protected Zombie selectPrimaryTarget(Plant plant, List<Zombie> candidates) {
        if (plant == null) {
            return null;
        }
        if (plant.hasTargetPriorityUp()
                || normalizeText(plant.getName()).equals("electric blueberry")) {
            return strongestTarget(candidates, false);
        }
        return nearestZombie(candidates, plant, false);
    }

    protected Zombie nearestZombie(List<Zombie> candidates, Plant plant, boolean allowSameTileOnly) {
        Zombie selected = null;
        double selectedDistance = Double.MAX_VALUE;
        String name = normalizeText(plant.getName());
        boolean canShootBehind = name.equals("starfruit") || name.equals("split pea");
        double maximumRange = resolveMaximumRange(plant);

        for (Zombie zombie : candidates) {
            if (zombie == null || !zombie.isAlive() || isHypnotized(zombie)) {
                continue;
            }
            double signedDistance = zombie.getX() - plant.getX();
            if (!canShootBehind && signedDistance < 0 && !occupiesSameTile(plant, zombie)) {
                continue;
            }
            double distance = Math.abs(signedDistance);
            if (allowSameTileOnly && distance > 1.25) {
                continue;
            }
            if (distance > maximumRange) {
                continue;
            }
            if (distance < selectedDistance) {
                selected = zombie;
                selectedDistance = distance;
            }
        }
        return selected;
    }

    protected boolean occupiesSameTile(Plant plant, Zombie zombie) {
        if (plant == null || zombie == null) {
            return false;
        }
        int plantColumn = (int) Math.round(plant.getX());
        int zombieColumn = (int) Math.ceil(zombie.getX());
        int plantRow = (int) Math.round(plant.getY());
        int zombieRow = (int) Math.round(zombie.getY());
        return plantColumn == zombieColumn && plantRow == zombieRow;
    }

    protected Zombie nearestZombieBehind(List<Zombie> candidates, Plant plant) {
        Zombie selected = null;
        double selectedDistance = Double.MAX_VALUE;
        for (Zombie zombie : candidates) {
            if (zombie == null || !zombie.isAlive() || isHypnotized(zombie)
                    || zombie.getX() >= plant.getX()) {
                continue;
            }
            double distance = plant.getX() - zombie.getX();
            if (distance < selectedDistance) {
                selected = zombie;
                selectedDistance = distance;
            }
        }
        return selected;
    }

    protected Zombie strongestTarget(List<Zombie> candidates, boolean includeHypnotized) {
        Zombie selected = null;
        int strongest = Integer.MIN_VALUE;
        for (Zombie zombie : candidates) {
            if (zombie == null || !zombie.isAlive()
                    || (!includeHypnotized && isHypnotized(zombie))) {
                continue;
            }
            int strength = zombie.getHp()
                    + (zombie.getArmor() == null ? 0 : zombie.getArmor().getHp());
            if (strength > strongest) {
                strongest = strength;
                selected = zombie;
            }
        }
        return selected;
    }

    protected List<Zombie> closestTargets(
            Plant plant,
            List<Zombie> candidates,
            int limit,
            boolean onlyNear
    ) {
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : candidates) {
            if (zombie == null || !zombie.isAlive() || isHypnotized(zombie)) {
                continue;
            }
            if (onlyNear && Math.abs(zombie.getX() - plant.getX()) > 1.25) {
                continue;
            }
            result.add(zombie);
        }
        result.sort(Comparator.comparingDouble(zombie -> Math.abs(zombie.getX() - plant.getX())));
        if (result.size() > limit) {
            return new ArrayList<>(result.subList(0, limit));
        }
        return result;
    }

    protected List<Zombie> collectCandidateZombies(Plant plant, Lane ownLane) {
        String name = normalizeText(plant.getName());
        String category = normalizeCategory(plant);
        if (board != null && (category.equals("homing")
                || name.equals("cat tail")
                || name.equals("electric blueberry")
                || name.equals("caulipower")
                || name.equals("starfruit"))) {
            return new ArrayList<>(board.getAllZombies());
        }
        return new ArrayList<>(ownLane.getAllZombies());
    }

    protected List<Zombie> allLivingEnemyZombies() {
        if (board == null) {
            return Collections.emptyList();
        }
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie != null && zombie.isAlive() && !isHypnotized(zombie)) {
                result.add(zombie);
            }
        }
        return result;
    }

    protected Zombie findArmoredTarget(Plant plant, Lane lane) {
        for (Zombie zombie : collectCandidateZombies(plant, lane)) {
            Armor armor = zombie.getArmor();
            if (armor == null || armor.isBroken()) {
                continue;
            }
            String type = normalizeText(armor.getArmorType());
            if (type.equals("metallic") || normalizeText(armor.getName()).contains("bucket")
                    || normalizeText(armor.getName()).contains("crown")
                    || normalizeText(armor.getName()).contains("shoulder")) {
                return zombie;
            }
        }
        return null;
    }

    protected double resolveMaximumRange(Plant plant) {
        String name = normalizeText(plant.getName());
        int bonus = Math.max(0, plant.getRange() - 1);
        if (normalizeCategory(plant).equals("melee")) {
            return MELEE_RANGE + bonus;
        }
        if (name.equals("puff shroom") || name.equals("sea shroom")) {
            return 3 + bonus;
        }
        if (name.equals("wasabi whip")) {
            return 2 + bonus;
        }
        if (name.equals("fume shroom")) {
            return 4 + bonus;
        }
        return Double.MAX_VALUE;
    }

    protected boolean canPlantAttack(Plant plant) {
        String category = normalizeCategory(plant);
        return category.equals("shooter")
                || category.equals("strike through")
                || category.equals("homing")
                || category.equals("lobber")
                || category.equals("melee");
    }

    protected boolean isLifespanExpired(Plant plant, PlantRuntimeState state) {
        int configured = plant.getLifespanTicks();
        String name = normalizeText(plant.getName());
        if (configured <= 0 && (name.equals("puff shroom") || name.equals("sea shroom"))) {
            configured = DEFAULT_SHROOM_LIFESPAN_TICKS;
        }
        return configured > 0 && state.ageTicks >= configured;
    }

    protected boolean hasTorchwoodBetween(Plant plant, Zombie target, Lane lane) {
        return findTorchwoodBetween(plant, target, lane) != null;
    }
}
