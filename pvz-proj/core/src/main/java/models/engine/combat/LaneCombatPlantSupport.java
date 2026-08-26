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


abstract class LaneCombatPlantSupport extends LaneCombatAttackSupport {
    protected LaneCombatPlantSupport(Board board) {
        super(board);
    }

    protected LaneCombatPlantSupport(Board board, Random random) {
        super(board, random);
    }

    protected boolean handlePassiveOrSpecialPlant(
            Lane lane,
            Tile tile,
            Plant plant,
            PlantRuntimeState state,
            Set<Plant> consumedPlants
    ) {
        String name = normalizeText(plant.getName());
        Boolean handled = handleMinePlant(name, lane, tile, plant, state, consumedPlants);
        if (handled == null) handled = handleInstantPlant(name, lane, tile, plant, state, consumedPlants);
        if (handled == null) handled = handleCooldownSpecialPlant(name, lane, tile, plant, state);
        if (handled != null) return handled;
        String category = normalizeCategory(plant);
        return category.equals("sun producer") || category.equals("wall nut")
                || category.equals("modifier") || name.equals("lily pad") || name.equals("torchwood");
    }

    private Boolean handleMinePlant(
            String name, Lane lane, Tile tile, Plant plant,
            PlantRuntimeState state, Set<Plant> consumedPlants
    ) {
        if (!name.equals("potato mine") && !name.equals("primal potato mine")) return null;
        int armTicks = plant.getArmTimeTicks() > 0 ? plant.getArmTimeTicks()
                : name.equals("primal potato mine")
                ? DEFAULT_PRIMAL_POTATO_ARM_TICKS : DEFAULT_POTATO_ARM_TICKS;
        if (state.ageTicks < armTicks || state.actionPending) return true;
        Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
        if (target == null || Math.abs(target.getX() - plant.getX()) > 0.75) {
            return true;
        }
        state.actionPending = true;
        plant.triggerSpecialAnimation("attack");
        int delay = PlantActionTiming.meleeImpactTicks(name, "attack");
        int damage = effectiveDamage(plant, name.equals("primal potato mine") ? 2400 : 1800);
        scheduleCombatAction(delay, () -> {
            if (!plant.isAlive()) {
                return;
            }
            damageArea(new Position((int) Math.round(plant.getX()), lane.getLaneId()),
                    1, 1, damage, "mine explosion", plant, null);
            tile.removePlant(plant);
            plantStates.remove(plant);
        });
        return true;
    }

    private Boolean handleInstantPlant(
            String name, Lane lane, Tile tile, Plant plant,
            PlantRuntimeState state, Set<Plant> consumedPlants
    ) {
        return switch (name) {
            case "squash" -> {
                handleSquash(lane, tile, plant, state, consumedPlants);
                yield true;
            }
            case "tangle kelp" -> {
                handleTangleKelp(lane, tile, plant, consumedPlants);
                yield true;
            }
            case "iceberg lettuce" -> {
                handleIcebergLettuce(lane, tile, plant, consumedPlants);
                yield true;
            }
            default -> null;
        };
    }

    private void handleSquash(
            Lane lane, Tile tile, Plant plant, PlantRuntimeState state, Set<Plant> consumedPlants
    ) {
        if (state.actionPending) return;
        Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
        if (target == null || Math.abs(target.getX() - plant.getX()) > 1.25) return;
        state.actionPending = true;
        String clip = target.getX() >= plant.getX() ? "jump_up_right" : "jump_up_left";
        plant.triggerSpecialAnimation(clip);
        int delay = PlantActionTiming.meleeImpactTicks("squash", clip);
        int limit = plant.canCrushTwice() ? 2 : 1;
        scheduleCombatAction(delay, () -> {
            for (Zombie zombie : closestTargets(
                    plant, collectCandidateZombies(plant, lane), limit, true)) {
                zombie.recordDamageSource(plant.getName(), plantCategory(plant), "crush");
                zombie.kill();
                state.crushCount++;
            }
            tile.removePlant(plant);
            plantStates.remove(plant);
        });
    }

    private void handleTangleKelp(
            Lane lane, Tile tile, Plant plant, Set<Plant> consumedPlants
    ) {
        PlantRuntimeState state = plantStateOf(plant);
        if (state.actionPending) return;
        List<Zombie> targets = closestTargets(plant, collectCandidateZombies(plant, lane),
                Math.max(1, plant.getTargetCount()), true);
        Zombie target = targets.isEmpty() ? null : targets.get(0);
        if (target == null || Math.abs(target.getX() - plant.getX()) > 0.75) return;
        state.actionPending = true;
        plant.triggerSpecialAnimation("attack_submerge");
        int delay = PlantActionTiming.meleeImpactTicks("tangle kelp", "attack_submerge");
        scheduleCombatAction(delay, () -> {
            int killed = 0;
            for (Zombie current : closestTargets(
                    plant, collectCandidateZombies(plant, lane),
                    Math.max(1, plant.getTargetCount()), true)) {
                if (Math.abs(current.getX() - plant.getX()) > 0.85) continue;
                current.recordDamageSource(plant.getName(), plantCategory(plant), "drag");
                current.kill();
                killed++;
            }
            if (killed > 0) {
                tile.removePlant(plant);
                plantStates.remove(plant);
            } else {
                state.actionPending = false;
            }
        });
    }

    private void handleIcebergLettuce(
            Lane lane, Tile tile, Plant plant, Set<Plant> consumedPlants
    ) {
        PlantRuntimeState state = plantStateOf(plant);
        if (state.actionPending) return;
        Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
        if (target == null || Math.abs(target.getX() - plant.getX()) > 0.9) return;
        state.actionPending = true;
        plant.triggerSpecialAnimation("attack");
        int delay = PlantActionTiming.meleeImpactTicks("iceberg lettuce", "attack");
        scheduleCombatAction(delay, () -> {
            Zombie impactTarget = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
            if (impactTarget != null && Math.abs(impactTarget.getX() - plant.getX()) <= 0.95) {
                applyFreeze(impactTarget,
                        Math.max(DEFAULT_FREEZE_TICKS, plant.getFreezeDurationTicks()));
                tile.removePlant(plant);
                plantStates.remove(plant);
            } else {
                state.actionPending = false;
            }
        });
    }

    private Boolean handleCooldownSpecialPlant(
            String name, Lane lane, Tile tile, Plant plant, PlantRuntimeState state
    ) {
        return switch (name) {
            case "magnet shroom" -> runMagnetShroom(plant, lane);
            case "caulipower" -> runCaulipower(plant, lane);
            case "electric blueberry" -> runElectricBlueberry(plant, lane);
            case "chomper" -> runChomper(plant, lane, state);
            case "phat beet" -> runPhatBeet(plant, lane);
            case "kiwibeast" -> runKiwibeast(plant, lane, state);
            default -> null;
        };
    }

    private boolean runMagnetShroom(Plant plant, Lane lane) {
        if (plant.getCooldownRemaining() != 0) return true;
        Zombie target = findArmoredTarget(plant, lane);
        if (target == null || target.getArmor() == null) return true;
        plant.prepareAttackAnimation("special");
        plant.attack();
        scheduleCombatAction(PlantActionTiming.specialImpactTicks("magnet shroom"), () -> {
            if (target.isAlive() && target.getArmor() != null) {
                target.getArmor().reduceDamage(Integer.MAX_VALUE);
                plant.triggerSpecialAnimation("catch");
            }
        });
        return true;
    }

    private boolean runCaulipower(Plant plant, Lane lane) {
        if (plant.getCooldownRemaining() != 0) return true;
        Zombie target = randomLivingTarget();
        if (target == null) return true;
        plant.prepareAttackAnimation("attack");
        plant.attack();
        scheduleCombatAction(PlantActionTiming.specialImpactTicks("caulipower"), () -> {
            if (target.isAlive()) hypnotize(target);
        });
        return true;
    }

    private boolean runElectricBlueberry(Plant plant, Lane lane) {
        if (plant.getCooldownRemaining() != 0) return true;
        Zombie target = randomLivingTarget();
        if (target == null) return true;
        plant.prepareAttackAnimation("attack");
        plant.attack();
        scheduleCombatAction(PlantActionTiming.specialImpactTicks("electric blueberry"), () -> {
            if (target.isAlive()) {
                target.recordDamageSource(plant.getName(), plantCategory(plant), "electric");
                target.kill();
            }
        });
        return true;
    }

    private boolean runChomper(Plant plant, Lane lane, PlantRuntimeState state) {
        if (plant.getCooldownRemaining() != 0 || state.actionPending) return true;
        Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
        if (target == null || Math.abs(target.getX() - plant.getX()) > MELEE_RANGE) return true;
        state.actionPending = true;
        plant.prepareAttackAnimation("bite");
        plant.attack();
        int delay = PlantActionTiming.meleeImpactTicks("chomper", "bite");
        scheduleCombatAction(delay, () -> {
            if (target.isAlive()) {
                target.recordDamageSource(plant.getName(), plantCategory(plant), "chomp");
                target.kill();
                state.digestTicks = plant.getDigestTimeTicks() > 0
                        ? plant.getDigestTimeTicks() : DEFAULT_CHOMPER_DIGEST_TICKS;
                plant.triggerSpecialAnimation("special");
            }
            state.actionPending = false;
        });
        return true;
    }

    private boolean runPhatBeet(Plant plant, Lane lane) {
        if (plant.getCooldownRemaining() != 0) return true;
        boolean hasTarget = false;
        for (Zombie zombie : board == null ? List.<Zombie>of() : board.getAllZombies()) {
            if (zombie != null && zombie.isAlive()
                    && Math.abs(zombie.getX() - plant.getX()) <= 1.5
                    && Math.abs(zombie.getY() - plant.getY()) <= 1.5) {
                hasTarget = true;
                break;
            }
        }
        if (!hasTarget) return true;
        int damage = effectiveDamage(plant, 15);
        plant.prepareAttackAnimation("attack");
        plant.attack();
        scheduleCombatAction(PlantActionTiming.meleeImpactTicks("phat beet", "attack"), () ->
                damageArea(new Position((int) Math.round(plant.getX()), lane.getLaneId()),
                        1, 1, damage, "sonic", plant, null));
        return true;
    }

    private boolean runKiwibeast(Plant plant, Lane lane, PlantRuntimeState state) {
        if (plant.getCooldownRemaining() != 0) return true;
        int stage = state.ageTicks >= 72 * TICKS_PER_SECOND ? 3
                : state.ageTicks >= 24 * TICKS_PER_SECOND ? 2 : 1;
        int radius = stage >= 3 ? 2 : 1;
        boolean hasTarget = false;
        if (board != null) {
            for (Zombie zombie : board.getAllZombies()) {
                if (zombie != null && zombie.isAlive()
                        && Math.abs(zombie.getX() - plant.getX()) <= radius + 0.5
                        && Math.abs(zombie.getY() - plant.getY()) <= radius + 0.5) {
                    hasTarget = true;
                    break;
                }
            }
        }
        if (!hasTarget) return true;
        String clip = "attack_stage" + stage;
        int damage = effectiveDamage(plant, 15 * stage);
        plant.prepareAttackAnimation(clip);
        plant.attack();
        scheduleCombatAction(PlantActionTiming.meleeImpactTicks("kiwibeast", clip), () ->
                damageArea(new Position((int) Math.round(plant.getX()), lane.getLaneId()),
                        radius, radius, damage, "kiwibeast pulse", plant, null));
        return true;
    }

    private Zombie randomLivingTarget() {
        if (board == null) {
            return null;
        }
        List<Zombie> candidates = new ArrayList<>();
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie != null && zombie.isAlive() && !isHypnotized(zombie)) {
                candidates.add(zombie);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private String plantCategory(Plant plant) {
        return plant.getType() == null ? "" : plant.getType().getCategory();
    }

}
