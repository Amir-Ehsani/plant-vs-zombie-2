package models.engine.combat;

import models.core.plant.Plant;
import models.core.plant.PlantActionTiming;
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


abstract class LaneCombatAttackSupport extends LaneCombatAbilitySupport {
    protected LaneCombatAttackSupport(Board board) {
        super(board);
    }

    protected LaneCombatAttackSupport(Board board, Random random) {
        super(board, random);
    }
    protected void performPlantAttack(
        Lane lane, Tile tile, Plant plant, PlantRuntimeState state
    ) {
        if (attackDisabledPlantObstruction(lane, plant)) {
            plant.attack();
            state.hasAttacked = true;
            return;
        }
        String name = normalizeText(plant.getName());
        if (handleLaneAttack(name, lane, plant, state)) return;
        if (handleDirectionalAttack(name, lane, plant, state)) return;
        performStandardPlantAttack(name, lane, tile, plant, state);
    }

    private boolean handleLaneAttack(
        String name, Lane lane, Plant plant, PlantRuntimeState state
    ) {
        if (name.equals("rotobaga")) {
            return handleRotobagaAttack(lane, plant, state);
        }
        if (!name.equals("threepeater")) {
            return false;
        }
        List<Integer> lanes = new ArrayList<>();
        if (plant.isBoosted() && board != null) {
            for (int laneNumber = 1; laneNumber <= board.getHeight(); laneNumber++) {
                lanes.add(laneNumber);
            }
        } else {
            lanes.add(lane.getLaneId() - 1);
            lanes.add(lane.getLaneId());
            lanes.add(lane.getLaneId() + 1);
        }
        int scheduled = scheduleLaneShots(plant, lanes, false, 1);
        if (scheduled > 0) {
            plant.prepareAttackAnimation("attack");
            finishAttack(plant, state);
        }
        return true;
    }

    private boolean handleRotobagaAttack(
        Lane lane, Plant plant, PlantRuntimeState state
    ) {
        if (board == null) {
            return true;
        }
        List<Integer> lanes = new ArrayList<>();
        int above = lane.getLaneId() - 1;
        int below = lane.getLaneId() + 1;
        if (board.getLaneAt(above) != null) {
            lanes.add(above);
            lanes.add(-above);
        }
        if (board.getLaneAt(below) != null) {
            lanes.add(below);
            lanes.add(-below);
        }
        int scheduled = scheduleLaneShots(plant, lanes, true, 1);
        if (scheduled > 0) {
            plant.prepareAttackAnimation("attack");
            finishAttack(plant, state);
        }
        return true;
    }

    private int scheduleLaneShots(
        Plant plant, List<Integer> laneDescriptors, boolean diagonal, int shotsPerDirection
    ) {
        if (board == null || laneDescriptors == null) {
            return 0;
        }
        int scheduled = 0;
        int damage = effectiveDamage(plant, diagonal ? 10 : 20);
        int repeats = Math.max(1, shotsPerDirection);
        for (int descriptor : laneDescriptors) {
            int laneNumber = Math.abs(descriptor);
            boolean behind = diagonal && descriptor < 0;
            Lane targetLane = board.getLaneAt(laneNumber);
            if (targetLane == null) {
                continue;
            }
            Zombie target = behind
                ? nearestZombieBehind(new ArrayList<>(targetLane.getAllZombies()), plant)
                : selectPrimaryTarget(plant, new ArrayList<>(targetLane.getAllZombies()));
            if (target == null) {
                continue;
            }
            for (int repeat = 0; repeat < repeats; repeat++) {
                int shotIndex = scheduled++;
                int delay = PlantActionTiming.projectileImpactTicks(
                    plant.getName(), "attack", Math.abs(target.getX() - plant.getX()), shotIndex
                );
                scheduleCombatAction(delay, () -> {
                    Zombie impactTarget = behind
                        ? nearestZombieBehind(new ArrayList<>(targetLane.getAllZombies()), plant)
                        : selectPrimaryTarget(plant, new ArrayList<>(targetLane.getAllZombies()));
                    if (impactTarget == null) {
                        return;
                    }
                    dealPlantDamage(plant, impactTarget, damage, resolveDamageType(plant), false);
                    applyOnHitEffects(plant, impactTarget);
                });
            }
        }
        return scheduled;
    }

    private boolean handleDirectionalAttack(
        String name, Lane lane, Plant plant, PlantRuntimeState state
    ) {
        if (name.equals("starfruit")) {
            return handleStarfruitAttack(plant, state);
        }
        if (name.equals("bonk choy")) {
            return handleBonkChoyAttack(lane, plant, state);
        }
        if (name.equals("wasabi whip")) {
            return handleWasabiWhipAttack(lane, plant, state);
        }
        if (!name.equals("split pea")) return false;
        List<Zombie> candidates = collectCandidateZombies(plant, lane);
        Zombie front = nearestZombie(candidates, plant, false);
        Zombie back = nearestZombieBehind(candidates, plant);
        int damage = effectiveDamage(plant, 20);
        int shotIndex = 0;
        if (front != null) {
            scheduleDirectionalProjectile(plant, lane, false, damage, shotIndex++);
        }
        if (back != null) {
            scheduleDirectionalProjectile(plant, lane, true, damage, shotIndex++);
            scheduleDirectionalProjectile(plant, lane, true, damage, shotIndex);
        }
        if (front != null || back != null) {
            String clip = back != null && front == null ? "attack2" : "attack";
            plant.prepareAttackAnimation(clip);
            finishAttack(plant, state);
        }
        return true;
    }

    private boolean handleStarfruitAttack(Plant plant, PlantRuntimeState state) {
        List<Zombie> targets = starfruitDirectionalTargets(plant);
        if (targets.isEmpty()) {
            return true;
        }
        int damage = effectiveDamage(plant, 20);
        for (int index = 0; index < targets.size(); index++) {
            Zombie target = targets.get(index);
            int delay = PlantActionTiming.projectileImpactTicks(
                plant.getName(), "attack", Math.abs(target.getX() - plant.getX()), index
            );
            scheduleCombatAction(delay, () -> {
                if (target.isAlive() && !isHypnotized(target)) {
                    dealPlantDamage(plant, target, damage, "star", false);
                }
            });
        }
        plant.prepareAttackAnimation("attack");
        finishAttack(plant, state);
        return true;
    }

    private List<Zombie> starfruitDirectionalTargets(Plant plant) {
        Zombie forward = null;
        Zombie upperForward = null;
        Zombie lowerForward = null;
        Zombie upperBack = null;
        Zombie lowerBack = null;
        double forwardDistance = Double.MAX_VALUE;
        double upperForwardDistance = Double.MAX_VALUE;
        double lowerForwardDistance = Double.MAX_VALUE;
        double upperBackDistance = Double.MAX_VALUE;
        double lowerBackDistance = Double.MAX_VALUE;
        for (Zombie zombie : allLivingEnemyZombies()) {
            double dx = zombie.getX() - plant.getX();
            int dy = (int) Math.round(zombie.getY() - plant.getY());
            double distance = Math.hypot(dx, dy);
            if (dy == 0 && dx >= 0 && distance < forwardDistance) {
                forward = zombie;
                forwardDistance = distance;
            } else if (dy < 0 && dx >= 0 && distance < upperForwardDistance) {
                upperForward = zombie;
                upperForwardDistance = distance;
            } else if (dy > 0 && dx >= 0 && distance < lowerForwardDistance) {
                lowerForward = zombie;
                lowerForwardDistance = distance;
            } else if (dy < 0 && dx < 0 && distance < upperBackDistance) {
                upperBack = zombie;
                upperBackDistance = distance;
            } else if (dy > 0 && dx < 0 && distance < lowerBackDistance) {
                lowerBack = zombie;
                lowerBackDistance = distance;
            }
        }
        List<Zombie> result = new ArrayList<>();
        addIfPresent(result, forward);
        addIfPresent(result, upperForward);
        addIfPresent(result, lowerForward);
        addIfPresent(result, upperBack);
        addIfPresent(result, lowerBack);
        return result;
    }

    private void addIfPresent(List<Zombie> targets, Zombie zombie) {
        if (zombie != null && !targets.contains(zombie)) {
            targets.add(zombie);
        }
    }

    private void scheduleDirectionalProjectile(
        Plant plant, Lane lane, boolean behind, int damage, int shotIndex
    ) {
        List<Zombie> candidates = new ArrayList<>(lane.getAllZombies());
        Zombie target = behind ? nearestZombieBehind(candidates, plant)
            : nearestZombie(candidates, plant, false);
        if (target == null) {
            return;
        }
        int delay = PlantActionTiming.projectileImpactTicks(
            plant.getName(), behind ? "attack2" : "attack",
            Math.abs(target.getX() - plant.getX()), shotIndex
        );
        scheduleCombatAction(delay, () -> {
            List<Zombie> current = new ArrayList<>(lane.getAllZombies());
            Zombie impactTarget = behind ? nearestZombieBehind(current, plant)
                : nearestZombie(current, plant, false);
            if (impactTarget != null) {
                dealPlantDamage(plant, impactTarget, damage, "pea", false);
                applyOnHitEffects(plant, impactTarget);
            }
        });
    }

    private boolean handleBonkChoyAttack(
        Lane lane, Plant plant, PlantRuntimeState state
    ) {
        List<Zombie> candidates = collectCandidateZombies(plant, lane);
        Zombie front = nearestZombie(candidates, plant, false);
        Zombie back = nearestZombieBehind(candidates, plant);
        double range = resolveMaximumRange(plant);
        if (front != null && Math.abs(front.getX() - plant.getX()) > range) front = null;
        if (back != null && Math.abs(back.getX() - plant.getX()) > range) back = null;
        if (front == null && back == null) {
            return true;
        }
        String clip = resolveBonkAttackClip(front, back);
        int delay = PlantActionTiming.meleeImpactTicks(plant.getName(), clip);
        int damage = effectiveDamage(plant, 15);
        scheduleCombatAction(delay, () -> {
            List<Zombie> current = collectCandidateZombies(plant, lane);
            Zombie currentFront = nearestZombie(current, plant, false);
            Zombie currentBack = nearestZombieBehind(current, plant);
            if (currentFront != null && Math.abs(currentFront.getX() - plant.getX()) <= range) {
                dealPlantDamage(plant, currentFront, damage, "melee", false);
            }
            if (currentBack != null && currentBack != currentFront
                && Math.abs(currentBack.getX() - plant.getX()) <= range) {
                dealPlantDamage(plant, currentBack, damage, "melee", false);
            }
        });
        plant.prepareAttackAnimation(clip);
        finishAttack(plant, state);
        return true;
    }

    private boolean handleWasabiWhipAttack(
        Lane lane, Plant plant, PlantRuntimeState state
    ) {
        List<Zombie> candidates = collectCandidateZombies(plant, lane);
        Zombie front = nearestZombie(candidates, plant, false);
        Zombie back = nearestZombieBehind(candidates, plant);
        double range = resolveMaximumRange(plant);
        if (front != null && Math.abs(front.getX() - plant.getX()) > range) front = null;
        if (back != null && Math.abs(back.getX() - plant.getX()) > range) back = null;
        if (front == null && back == null) {
            return true;
        }
        String clip = front != null && back != null ? "attack3" : back != null ? "attack2" : "attack";
        int damage = effectiveDamage(plant, 30);
        int delay = PlantActionTiming.meleeImpactTicks(plant.getName(), clip);
        scheduleCombatAction(delay, () -> {
            List<Zombie> current = collectCandidateZombies(plant, lane);
            Zombie currentFront = nearestZombie(current, plant, false);
            Zombie currentBack = nearestZombieBehind(current, plant);
            if (currentFront != null && Math.abs(currentFront.getX() - plant.getX()) <= range) {
                dealPlantDamage(plant, currentFront, damage, "fire whip", true);
            }
            if (currentBack != null && currentBack != currentFront
                && Math.abs(currentBack.getX() - plant.getX()) <= range) {
                dealPlantDamage(plant, currentBack, damage, "fire whip", true);
            }
            meltNearbyTerrain("wasabi whip", plant);
        });
        plant.prepareAttackAnimation(clip);
        finishAttack(plant, state);
        return true;
    }

    private String resolveBonkAttackClip(Zombie front, Zombie back) {
        if (front != null && back != null && back != front) {
            return "attack3";
        }
        if (back != null) {
            return "attack2";
        }
        return "attack";
    }

    private void performStandardPlantAttack(
        String name, Lane lane, Tile tile, Plant plant, PlantRuntimeState state
    ) {
        List<Zombie> candidates = collectCandidateZombies(plant, lane);
        Zombie target = selectPrimaryTarget(plant, candidates);
        if (target == null || !isChargeReady(name, plant, state)) return;
        if (name.equals("bowling bulb")) {
            prepareBowlingBulbShot(state);
        }
        int damage = effectiveDamage(plant, resolveBaseDamage(plant, state, tile));
        boolean peaProjectile = isPeaProjectilePlant(name, plant);
        Plant torchwood = peaProjectile ? findTorchwoodBetween(plant, target, lane) : null;
        boolean fireDamage = isFirePlant(plant) || torchwood != null;
        if (torchwood != null) {
            damage *= torchwood.hasBlueFlame() ? 3 : 2;
        }

        boolean butterShot = name.equals("kernel pult")
            && random.nextInt(100) < Math.min(100, 25 + plant.getButterChancePercent());
        String attackClip = resolveAttackClip(name, state, tile, butterShot);
        int shotCount = resolveShotCount(plant, tile);
        int damagePerShot = damage;
        boolean fireAtImpact = fireDamage;
        for (int shot = 0; shot < shotCount; shot++) {
            int shotIndex = shot;
            int delay = PlantActionTiming.projectileImpactTicks(
                plant.getName(), attackClip, Math.abs(target.getX() - plant.getX()), shotIndex
            );
            scheduleCombatAction(delay, () -> applyStandardProjectileImpact(
                name, lane, plant, damagePerShot, fireAtImpact, butterShot, shotIndex
            ));
        }
        plant.prepareAttackAnimation(attackClip);
        state.shotCycle++;
        finishAttack(plant, state);
    }

    private void prepareBowlingBulbShot(PlantRuntimeState state) {
        if (state.bowlingOrangeRechargeTicks <= 0) {
            state.bowlingShotTier = 3;
            state.bowlingOrangeRechargeTicks = 10 * TICKS_PER_SECOND;
            return;
        }
        if (state.bowlingBlueRechargeTicks <= 0) {
            state.bowlingShotTier = 2;
            state.bowlingBlueRechargeTicks = 5 * TICKS_PER_SECOND;
            return;
        }
        state.bowlingShotTier = 1;
    }

    private String resolveAttackClip(
        String name, PlantRuntimeState state, Tile tile, boolean butterShot
    ) {
        if (name.equals("pea pod") && tile != null) {
            int heads = 0;
            for (Plant layer : tile.getPlants()) {
                if (normalizeText(layer.getName()).equals("pea pod")) {
                    heads++;
                }
            }
            return heads <= 1 ? "attack" : "attack " + Math.min(5, heads);
        }
        if (name.equals("kernel pult")) {
            return butterShot ? "attack2" : "attack";
        }
        if (name.equals("bowling bulb")) {
            return state.bowlingShotTier >= 3 ? "special3"
                    : state.bowlingShotTier == 2 ? "special2" : "special";
        }
        if (name.equals("fume shroom")) {
            return "special";
        }
        if (name.equals("kiwibeast")) {
            if (state.ageTicks >= 72 * TICKS_PER_SECOND) return "attack_stage3";
            if (state.ageTicks >= 24 * TICKS_PER_SECOND) return "attack_stage2";
            return "attack_stage1";
        }
        return "attack";
    }

    private void applyStandardProjectileImpact(
        String name,
        Lane lane,
        Plant plant,
        int damage,
        boolean fireDamage,
        boolean butterShot,
        int shotIndex
    ) {
        List<Zombie> candidates = collectCandidateZombies(plant, lane);
        Zombie target = selectPrimaryTarget(plant, candidates);
        if (target == null) {
            return;
        }
        Tile blockingTerrain = findBlockingTerrain(lane, plant, target);
        if (blockingTerrain != null) {
            if (board == null) {
                blockingTerrain.damageTerrain(damage, fireDamage);
            } else {
                board.damageTerrain(blockingTerrain.getPosition(), damage, fireDamage);
            }
            return;
        }
        String damageType = name.equals("goo peashooter")
            ? "poison armor bypass" : fireDamage ? "fire" : resolveDamageType(plant);
        dealPlantDamage(plant, target, damage, damageType, fireDamage);
        if (butterShot && name.equals("kernel pult") && target.isAlive()) {
            applyButterStun(target, DEFAULT_BUTTER_TICKS);
        }
        applyOnHitEffects(plant, target);
        applySplashDamage(plant, target, damage, fireDamage);
        if (name.equals("bowling bulb")) {
            applyBowlingBulbBounces(plant, target, damage);
        } else {
            applyExtraTargets(plant, target, candidates, damage, fireDamage);
        }
        meltNearbyTerrain(name, plant);
    }

    private boolean isPeaProjectilePlant(String name, Plant plant) {
        if (name == null || plant == null) {
            return false;
        }
        String tags = plant.getType() == null ? "" : normalizeText(plant.getType().getTags());
        return tags.contains("pea")
                || name.equals("peashooter")
                || name.equals("repeater")
                || name.equals("threepeater")
                || name.equals("split pea")
                || name.equals("pea pod")
                || name.equals("snow pea")
                || name.equals("fire peashooter")
                || name.equals("goo peashooter")
                || name.equals("mega gatling pea");
    }

    private boolean isChargeReady(String name, Plant plant, PlantRuntimeState state) {
        return !name.equals("citron") || state.hasAttacked
            || state.ageTicks >= Math.max(1, plant.getChargeTimeTicks());
    }

    private void meltNearbyTerrain(String name, Plant plant) {
        if ((!name.equals("pepper pult") && !name.equals("wasabi whip")
            && plant.getWarmthRadius() <= 0) || board == null) return;
        int radius = Math.max(1, plant.getWarmthRadius());
        board.meltTerrainArea(new Position(
            (int) Math.round(plant.getX()), (int) Math.round(plant.getY())), radius);
    }

    private void finishAttack(Plant plant, PlantRuntimeState state) {
        plant.attack();
        state.hasAttacked = true;
    }

    protected int resolveBaseDamage(Plant plant, PlantRuntimeState state, Tile tile) {
        String name = normalizeText(plant.getName());
        if (name.equals("citron")) {
            return 400;
        }
        if (name.equals("bowling bulb")) {
            return state.bowlingShotTier >= 3 ? 180 : state.bowlingShotTier == 2 ? 120 : 40;
        }
        if (name.equals("cabbage pult") || name.equals("kernel pult")) {
            return 40;
        }
        if (name.equals("melon pult") || name.equals("winter melon")) {
            return 80;
        }
        if (name.equals("pepper pult")) {
            return 50;
        }
        if (name.equals("wasabi whip")) {
            return 30;
        }
        if (name.equals("kiwibeast")) {
            double healthRatio = plant.getMaxHp() == 0 ? 1.0
                : plant.getHp() / (double) plant.getMaxHp();
            int size = healthRatio <= 0.33 ? Math.max(3, plant.getMaxSize())
                : healthRatio <= 0.66 ? Math.max(2, plant.getMaxSize())
                : 1;
            return 15 * size;
        }
        if (name.equals("bonk choy")) {
            return 15;
        }
        if (name.equals("goo peashooter") || name.equals("snow pea")
            || name.equals("fire peashooter") || name.equals("mega gatling pea")
            || name.equals("repeater") || name.equals("pea pod")) {
            return 20;
        }
        return Math.max(0, plant.getAttackDamage());
    }
    protected int resolveShotCount(Plant plant, Tile tile) {
        String name = normalizeText(plant.getName());
        if (name.equals("repeater")) {
            return 2;
        }
        if (name.equals("mega gatling pea")) {
            return 4;
        }
        if (name.equals("pea pod") && tile != null) {
            int count = 0;
            for (Plant layer : tile.getPlants()) {
                if (normalizeText(layer.getName()).equals("pea pod")) {
                    count++;
                }
            }
            return Math.max(1, count);
        }
        return 1;
    }
    protected void applyOnHitEffects(Plant plant, Zombie target) {
        if (plant == null || target == null || !target.isAlive()) {
            return;
        }
        String name = normalizeText(plant.getName());

        if (name.equals("snow pea") || name.equals("winter melon")) {
            applyChill(target, Math.max(DEFAULT_CHILL_TICKS, plant.getChillDurationTicks()));
        }
        if (name.equals("goo peashooter") || plant.getDamagePerTick() > 0) {
            int damage = Math.max(5, plant.getDamagePerTick());
            applyPoison(target, damage, DEFAULT_POISON_TICKS, plant);
        }
    }
    protected void applySplashDamage(
        Plant plant,
        Zombie primary,
        int primaryDamage,
        boolean fireDamage
    ) {
        int splash = plant.getAreaDamage();
        String name = normalizeText(plant.getName());
        if (name.equals("melon pult") || name.equals("winter melon")) {
            splash = Math.max(splash, primaryDamage / 2);
        }
        if (splash <= 0 || board == null) {
            return;
        }
        Position center = new Position(
            Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(primary.getX()))),
            Math.max(1, Math.min(board.getHeight(), (int) Math.round(primary.getY())))
        );
        damageArea(center, 1, 1, splash,
            fireDamage ? "fire splash" : "splash", plant, primary);
    }
    private void applyBowlingBulbBounces(Plant plant, Zombie primary, int damage) {
        if (board == null || primary == null) {
            return;
        }
        Zombie previous = primary;
        int lane = (int) Math.round(primary.getY());
        for (int bounce = 0; bounce < 2; bounce++) {
            Zombie next = nearestBowlingTarget(previous, lane, bounce);
            if (next == null) {
                break;
            }
            dealPlantDamage(plant, next, damage, "bowling bounce", false);
            previous = next;
            lane = (int) Math.round(next.getY());
        }
    }

    private Zombie nearestBowlingTarget(Zombie previous, int lane, int bounceIndex) {
        int preferred = bounceIndex % 2 == 0 ? lane - 1 : lane + 1;
        Zombie target = nearestLivingInLane(preferred, previous);
        if (target != null) {
            return target;
        }
        int alternate = bounceIndex % 2 == 0 ? lane + 1 : lane - 1;
        return nearestLivingInLane(alternate, previous);
    }

    private Zombie nearestLivingInLane(int laneNumber, Zombie previous) {
        Lane lane = board.getLaneAt(laneNumber);
        if (lane == null) {
            return null;
        }
        Zombie selected = null;
        double best = Double.MAX_VALUE;
        for (Zombie zombie : lane.getAllZombies()) {
            if (zombie == null || !zombie.isAlive() || isHypnotized(zombie) || zombie == previous) {
                continue;
            }
            double distance = Math.abs(zombie.getX() - previous.getX());
            if (distance < best) {
                best = distance;
                selected = zombie;
            }
        }
        return selected;
    }

    protected void applyExtraTargets(
        Plant plant,
        Zombie primary,
        List<Zombie> candidates,
        int damage,
        boolean fireDamage
    ) {
        int additional = plant.hasPlantFoodUnlimitedPierce()
            ? Integer.MAX_VALUE
            : Math.max(0, plant.getTargetCount() - 1)
            + Math.max(0, plant.getPierceCount())
            + Math.max(0, plant.getBounces());
        String category = normalizeCategory(plant);
        String plantName = normalizeText(plant.getName());
        if (plantName.equals("fume shroom")) {
            additional = Integer.MAX_VALUE;
        } else if (category.equals("strike through") && additional == 0) {
            additional = 2;
        }
        if (additional <= 0) {
            return;
        }

        List<Zombie> ordered = new ArrayList<>();
        for (Zombie zombie : candidates) {
            if (zombie != null && zombie != primary && zombie.isAlive()
                && !isHypnotized(zombie)
                && Math.abs(zombie.getX() - plant.getX()) <= resolveMaximumRange(plant)) {
                ordered.add(zombie);
            }
        }
        ordered.sort(Comparator.comparingDouble(zombie -> Math.abs(zombie.getX() - primary.getX())));

        for (int index = 0; index < ordered.size() && index < additional; index++) {
            Zombie zombie = ordered.get(index);
            int appliedDamage = index < plant.getBounces()
                ? Math.max(1, damage / 2)
                : damage;
            dealPlantDamage(plant, zombie, appliedDamage,
                fireDamage ? "fire" : "multi target", fireDamage);
            applyOnHitEffects(plant, zombie);
        }
    }
    protected void damageArea(
        Position center,
        int xRadius,
        int yRadius,
        int damage,
        String damageType,
        Plant source,
        Zombie excluded
    ) {
        if (board == null || center == null || damage <= 0) {
            return;
        }
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || zombie == excluded || !zombie.isAlive() || isHypnotized(zombie)) {
                continue;
            }
            int x = (int) Math.ceil(zombie.getX());
            int y = (int) Math.round(zombie.getY());
            if (Math.abs(x - center.getX()) <= xRadius
                && Math.abs(y - center.getY()) <= yRadius) {
                dealPlantDamage(source, zombie, damage, damageType, damageType.contains("fire"));
            }
        }
    }
    protected void dealPlantDamage(
        Plant source, Zombie target, int damage, String damageType, boolean fireDamage
    ) {
        if (!canDamageTarget(target, damage)) return;
        int adjusted = adjustedPlantDamage(source, damage);
        ZombieRuntimeState targetState = stateOf(target);
        if (isAttackBlockedOrReflected(source, target, targetState, damageType, adjusted)) return;
        recordPlantDamageSource(source, target, damageType);
        target.takeDamage(new Damage(adjusted, damageType));
        if (!target.isAlive()) handleSpecialZombieDeath(target, targetState);
        if (fireDamage) damageTerrainAtTarget(target, adjusted);
    }
    private boolean canDamageTarget(Zombie target, int damage) {
        return target != null && target.isAlive() && damage > 0 && !isHypnotized(target);
    }
    private int adjustedPlantDamage(Plant source, int damage) {
        if (source != null && isFamilyBoosted(source.getType().getCategory())) return damage * 2;
        return damage;
    }
    private boolean isAttackBlockedOrReflected(
        Plant source,
        Zombie target,
        ZombieRuntimeState state,
        String damageType,
        int damage
    ) {
        String targetName = normalizeText(target.getName());
        if (!targetName.equals("juggler") || !isReflectableProjectile(source, damageType)) return false;
        state.jugglerSpinTicks = 2 * TICKS_PER_SECOND;
        if (source != null) {
            source.takeDamage(new Damage(damage, "reflected projectile"));
            if (isIceDamage(resolveDamageType(source))
                || normalizeText(source.getName()).contains("snow")) source.addIceHit();
        }
        return true;
    }
    private void recordPlantDamageSource(Plant source, Zombie target, String damageType) {
        if (source == null) return;
        target.recordDamageSource(source.getName(),
            source.getType() == null ? "" : source.getType().getCategory(), damageType);
    }
    private void damageTerrainAtTarget(Zombie target, int damage) {
        if (board == null) return;
        Position position = new Position(
            Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(target.getX()))),
            Math.max(1, Math.min(board.getHeight(), (int) Math.round(target.getY())))
        );
        board.damageTerrain(position, damage, true);
    }
    protected void updateZombies(Lane lane, List<Zombie> zombies) {
        List<Zombie> ordered = new ArrayList<>(zombies);
        ordered.sort(Comparator.comparingDouble(Zombie::getX));
        for (Zombie zombie : ordered) updateZombie(lane, zombie);
    }
    private void updateZombie(Lane lane, Zombie zombie) {
        if (!zombie.isAlive() || !processedZombiesThisBoardTick.add(zombie)) return;
        ZombieRuntimeState state = stateOf(zombie);
        state.ageTicks++;
        observeZombieDamage(zombie, state);
        handleFrontObjectState(lane, zombie, state);
        if (!applyZombieStatusEffects(lane, zombie, state)) return;
        if (zombie.getX() > lane.getWidth()) {
            updateZombieSpeed(zombie, state);
            moveZombieByAbility(zombie, state);
            return;
        }
        runSpecialZombieAbility(lane, zombie, state);
        if (!zombie.isAlive()) {
            handleSpecialZombieDeath(zombie, state);
            return;
        }
        updateZombieSpeed(zombie, state);
        if (state.hypnotized) {
            updateHypnotizedZombie(lane, zombie, state);
            return;
        }
        moveOrAttackWithZombie(lane, zombie, state);
    }
    private boolean applyZombieStatusEffects(
        Lane lane, Zombie zombie, ZombieRuntimeState state
    ) {
        if (state.poisonTicks > 0) {
            zombie.recordDamageSource(
                state.poisonSourcePlantName, state.poisonSourcePlantCategory, "poison");
            zombie.takeDamage(new Damage(state.poisonDamage, "poison"));
            state.poisonTicks--;
            if (!zombie.isAlive()) {
                handleSpecialZombieDeath(zombie, state);
                return false;
            }
        }
        Tile tile = tileForZombie(lane, zombie);
        if (tile != null && tile.isFrozenTerrain() && !zombie.isIceImmune()) {
            zombie.setCurrentSpeed(0);
            return false;
        }
        if (state.frozenTicks > 0) {
            state.frozenTicks--;
            zombie.setCurrentSpeed(0);
            return false;
        }
        if (state.butterTicks > 0) {
            state.butterTicks--;
            zombie.setCurrentSpeed(0);
            return false;
        }
        if (state.electricStrikeTicks > 0) {
            state.electricStrikeTicks--;
        }
        return true;
    }
    private void updateZombieSpeed(Zombie zombie, ZombieRuntimeState state) {
        double multiplier = state.chilledTicks > 0 ? 0.5 : 1.0;
        if (state.chilledTicks > 0) state.chilledTicks--;
        zombie.setCurrentSpeed(resolveAbilitySpeed(zombie, state) * multiplier);
    }
    private void moveOrAttackWithZombie(Lane lane, Zombie zombie, ZombieRuntimeState state) {
        maybeAttractToSweetPotato(lane, zombie, state);
        Tile tile = tileForZombie(lane, zombie);
        Plant plant = tile == null ? null : tile.getCurrentPlant();
        if (plant != null && plant.isTransformedToCat()) plant = null;
        updateSnorkelState(zombie, tile, plant);
        if (handleAllstarZombieCollision(lane, zombie, state)
            || handleHeavyZombieCollision(lane, zombie)
            || handleHypnotizedZombieCollision(lane, zombie, state)) return;
        if (plant != null && plant.isAlive() && !fliesOverPlant(zombie, plant)) {
            handleZombiePlantCollision(lane, zombie, state, plant);
        } else {
            moveZombieByAbility(zombie, state);
        }
    }
    private boolean handleHypnotizedZombieCollision(
            Lane lane, Zombie zombie, ZombieRuntimeState state
    ) {
        Zombie target = nearestHypnotizedZombie(lane, zombie);
        if (target == null) {
            state.hostileDuelTarget = null;
            state.hostileDuelBiteTicks = 0;
            return false;
        }
        if (state.hostileDuelTarget != target) {
            state.hostileDuelTarget = target;
            state.hostileDuelBiteTicks = HYPNOTIZED_BITE_WINDUP_TICKS;
        }
        if (state.hostileDuelBiteTicks > 0) {
            state.hostileDuelBiteTicks--;
            return true;
        }
        target.takeDamage(new Damage(
                zombie.getType().getDamagePerTick(),
                "zombie duel bite"
        ));
        state.hostileDuelBiteTicks = HYPNOTIZED_BITE_INTERVAL_TICKS;
        return true;
    }

    private Zombie nearestHypnotizedZombie(Lane lane, Zombie zombie) {
        Zombie target = null;
        double minimum = Double.MAX_VALUE;
        for (Zombie candidate : lane.getAllZombies()) {
            if (candidate == zombie || !candidate.isAlive() || !isHypnotized(candidate)) {
                continue;
            }
            double distance = Math.abs(candidate.getX() - zombie.getX());
            if (distance <= MELEE_RANGE && distance < minimum) {
                minimum = distance;
                target = candidate;
            }
        }
        return target;
    }

    protected void updateHypnotizedZombie(
            Lane lane, Zombie zombie, ZombieRuntimeState state
    ) {
        Zombie target = null;
        double minimum = Double.MAX_VALUE;
        for (Zombie candidate : lane.getAllZombies()) {
            if (candidate == zombie || !candidate.isAlive() || isHypnotized(candidate)
                    || candidate.getX() < zombie.getX()) {
                continue;
            }
            double distance = candidate.getX() - zombie.getX();
            if (distance < minimum) {
                minimum = distance;
                target = candidate;
            }
        }
        if (target == null || minimum > MELEE_RANGE) {
            state.hypnotizedTarget = null;
            state.hypnotizedBiteTicks = 0;
            zombie.moveBy(zombie.getCurrentSpeed(), 0);
            return;
        }
        if (state.hypnotizedTarget != target) {
            state.hypnotizedTarget = target;
            state.hypnotizedBiteTicks = HYPNOTIZED_BITE_WINDUP_TICKS;
        }
        if (state.hypnotizedBiteTicks > 0) {
            state.hypnotizedBiteTicks--;
            return;
        }
        target.takeDamage(new Damage(
                zombie.getType().getDamagePerTick(),
                "hypnotized bite"
        ));
        state.hypnotizedBiteTicks = HYPNOTIZED_BITE_INTERVAL_TICKS;
    }
    protected void observeZombieDamage(Zombie zombie, ZombieRuntimeState state) {
        if (zombie.getDamageRevision() == state.lastDamageRevision) {
            return;
        }
        state.lastDamageRevision = zombie.getDamageRevision();
        String type = normalizeText(zombie.getLastDamageType());
        String name = normalizeText(zombie.getName());
        if (name.equals("prospector") && isIceDamage(type)) {
            state.prospectorDynamiteExtinguished = true;
        }
        if (name.equals("explorer")) {
            if (isIceDamage(type)) {
                state.torchLit = false;
            } else if (type.contains("fire") || type.contains("flame") || type.contains("burn")) {
                state.torchLit = true;
            }
        }
    }

}
