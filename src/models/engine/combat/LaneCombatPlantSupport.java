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
        if (state.ageTicks < armTicks) return true;
        Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
        if (target != null && Math.abs(target.getX() - plant.getX()) <= 0.75) {
            int damage = effectiveDamage(plant, name.equals("primal potato mine") ? 2400 : 1800);
            damageArea(new Position((int) Math.round(plant.getX()), lane.getLaneId()),
                    1, 1, damage, "mine", plant, null);
            consumePlant(tile, plant, consumedPlants);
        }
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
        Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
        if (target == null || Math.abs(target.getX() - plant.getX()) > 1.25) return;
        int limit = plant.canCrushTwice() ? 2 : 1;
        for (Zombie zombie : closestTargets(plant, collectCandidateZombies(plant, lane), limit, true)) {
            zombie.recordDamageSource(plant.getName(), plantCategory(plant), "crush");
            zombie.kill();
            state.crushCount++;
        }
        consumePlant(tile, plant, consumedPlants);
    }

    private void handleTangleKelp(
            Lane lane, Tile tile, Plant plant, Set<Plant> consumedPlants
    ) {
        List<Zombie> targets = closestTargets(plant, collectCandidateZombies(plant, lane),
                Math.max(1, plant.getTargetCount()), true);
        int killed = 0;
        for (Zombie target : targets) {
            if (Math.abs(target.getX() - plant.getX()) > 0.75) continue;
            target.recordDamageSource(plant.getName(), plantCategory(plant), "drag");
            target.kill();
            killed++;
        }
        if (killed > 0) consumePlant(tile, plant, consumedPlants);
    }

    private void handleIcebergLettuce(
            Lane lane, Tile tile, Plant plant, Set<Plant> consumedPlants
    ) {
        Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
        if (target != null && Math.abs(target.getX() - plant.getX()) <= 0.9) {
            applyFreeze(target, Math.max(DEFAULT_FREEZE_TICKS, plant.getFreezeDurationTicks()));
            consumePlant(tile, plant, consumedPlants);
        }
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
            default -> null;
        };
    }

    private boolean runMagnetShroom(Plant plant, Lane lane) {
        if (plant.getCooldownRemaining() == 0) {
            Zombie target = findArmoredTarget(plant, lane);
            if (target != null && target.getArmor() != null) {
                target.getArmor().reduceDamage(Integer.MAX_VALUE);
                plant.attack();
            }
        }
        return true;
    }

    private boolean runCaulipower(Plant plant, Lane lane) {
        if (plant.getCooldownRemaining() == 0) {
            Zombie target = strongestTarget(collectCandidateZombies(plant, lane), false);
            if (target != null) {
                hypnotize(target);
                plant.attack();
            }
        }
        return true;
    }

    private boolean runElectricBlueberry(Plant plant, Lane lane) {
        if (plant.getCooldownRemaining() == 0) {
            Zombie target = strongestTarget(collectCandidateZombies(plant, lane), false);
            if (target != null) {
                target.recordDamageSource(plant.getName(), plantCategory(plant), "electric");
                target.kill();
                plant.attack();
            }
        }
        return true;
    }

    private boolean runChomper(Plant plant, Lane lane, PlantRuntimeState state) {
        if (plant.getCooldownRemaining() == 0) {
            Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
            if (target != null && Math.abs(target.getX() - plant.getX()) <= MELEE_RANGE) {
                target.recordDamageSource(plant.getName(), plantCategory(plant), "chomp");
                target.kill();
                state.digestTicks = plant.getDigestTimeTicks() > 0
                        ? plant.getDigestTimeTicks() : DEFAULT_CHOMPER_DIGEST_TICKS;
                plant.attack();
            }
        }
        return true;
    }

    private boolean runPhatBeet(Plant plant, Lane lane) {
        if (plant.getCooldownRemaining() == 0) {
            int damage = effectiveDamage(plant, 15);
            damageArea(new Position((int) Math.round(plant.getX()), lane.getLaneId()),
                    1, 1, damage, "sonic", plant, null);
            plant.attack();
        }
        return true;
    }

    private String plantCategory(Plant plant) {
        return plant.getType() == null ? "" : plant.getType().getCategory();
    }

}
