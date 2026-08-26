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


abstract class LaneCombatAbilitySupport extends LaneCombatTargetSupport {
    protected LaneCombatAbilitySupport(Board board) {
        super(board);
    }

    protected LaneCombatAbilitySupport(Board board, Random random) {
        super(board, random);
    }

    protected void runSpecialZombieAbility(
            Lane lane,
            Zombie zombie,
            ZombieRuntimeState state
    ) {
        String name = normalizeText(zombie.getName());
        if (name.equals("gargantuar")) {
            handleGargantuar(lane, zombie, state);
        } else if (name.equals("turquoise")) {
            handleTurquoise(lane, zombie, state);
        } else if (name.equals("prospector")) {
            handleProspector(zombie, state);
        } else if (name.equals("piano")) {
            handlePiano(lane, zombie, state);
        } else if (name.equals("ra")) {
            handleRa(zombie, state);
        } else if (name.equals("tomb raiser")) {
            handleTombRaiser(lane, state);
        } else if (name.equals("hunter")) {
            handleHunter(lane, zombie, state);
        } else if (name.equals("troglobite")) {
            handleTroglobite(lane, zombie, state);
        } else if (name.equals("fisherman")) {
            handleFisherman(lane, zombie, state);
        } else if (name.equals("octopus")) {
            handleOctopus(lane, zombie, state);
        } else if (name.equals("wizard")) {
            handleWizard(lane, zombie, state);
        } else if (name.equals("king")) {
            handleKing(lane, zombie, state);
        } else if (name.equals("juggler") && state.jugglerSpinTicks > 0) {
            state.jugglerSpinTicks--;
        }
    }

    protected void handleGargantuar(Lane lane, Zombie zombie, ZombieRuntimeState state) {
        if (state.gargantuarImpThrown || zombie.getHp() > zombie.getMaxHp() / 2) {
            return;
        }
        state.gargantuarImpThrown = true;
        spawnZombie("Imp", 3, lane.getLaneId());
    }

    protected void handleTurquoise(Lane lane, Zombie zombie, ZombieRuntimeState state) {
        Plant target = nearestPlantInLane(lane, zombie.getX(), 4.0, false);
        if (target == null) {
            state.turquoiseChannelTicks = 0;
            return;
        }
        state.turquoiseChannelTicks++;
        if (state.turquoiseChannelTicks % TICKS_PER_SECOND == 0 && board != null) {
            int stolen = board.stealStoredSun(25);
            zombie.addStolenSun(stolen);
        }
        if (state.turquoiseChannelTicks < 5 * TICKS_PER_SECOND) {
            return;
        }
        double left = zombie.getX() - 4.0;
        for (Tile tile : lane.getTiles()) {
            if (tile.getPosition().getX() >= left
                    && tile.getPosition().getX() < zombie.getX()) {
                for (Plant plant : new ArrayList<>(tile.getPlants())) {
                    plant.kill();
                }
            }
        }
        state.turquoiseChannelTicks = 0;
    }

    protected void handleProspector(Zombie zombie, ZombieRuntimeState state) {
        if (state.prospectorReversed || state.prospectorDynamiteExtinguished
                || state.ageTicks < 10 * TICKS_PER_SECOND) {
            return;
        }
        state.prospectorReversed = true;
        zombie.moveTo(1, zombie.getY());
    }

    protected void handlePiano(Lane lane, Zombie zombie, ZombieRuntimeState state) {
        if (board == null || state.ageTicks % (5 * TICKS_PER_SECOND) != 0) {
            return;
        }
        for (Zombie candidate : new ArrayList<>(board.getAllZombies())) {
            if (candidate != zombie && candidate.isAlive()) {
                board.shiftZombieToAdjacentLane(candidate, random);
            }
        }
    }

    protected void handleRa(Zombie zombie, ZombieRuntimeState state) {
        if (board == null || state.ageTicks % TICKS_PER_SECOND != 0) {
            return;
        }
        zombie.addStolenSun(board.stealLooseSuns());
    }

    protected void handleTombRaiser(Lane lane, ZombieRuntimeState state) {
        if (state.ageTicks % (3 * TICKS_PER_SECOND) != 0) {
            return;
        }
        List<Tile> available = new ArrayList<>();
        List<Lane> candidateLanes = board == null ? List.of(lane) : board.getLanes();
        for (Lane candidateLane : candidateLanes) {
            for (Tile tile : candidateLane.getTiles()) {
                if (!tile.hasPlant() && !tile.hasZombies()
                        && tile.getTileType() == TileType.NORMAL) {
                    available.add(tile);
                }
            }
        }
        Collections.shuffle(available, random);
        for (int index = 0; index < Math.min(2, available.size()); index++) {
            available.get(index).setTileType(TileType.GRAVE);
        }
    }

    protected void handleHunter(Lane lane, Zombie zombie, ZombieRuntimeState state) {
        if (state.ageTicks % (5 * TICKS_PER_SECOND) != 0) {
            return;
        }
        Plant target = nearestPlantInLane(lane, zombie.getX(), Double.MAX_VALUE, false);
        if (target != null) {
            target.addIceHit();
        }
    }

    protected void handleTroglobite(Lane lane, Zombie zombie, ZombieRuntimeState state) {
        int currentX = clampX(lane, zombie);
        Tile front = lane.getTileAt(currentX - 1);
        Tile destination = lane.getTileAt(currentX - 2);
        if (front == null || destination == null || front.getTileType() != TileType.ICE) {
            return;
        }
        for (Plant plant : new ArrayList<>(destination.getPlants())) {
            plant.kill();
        }
        for (Zombie candidate : new ArrayList<>(destination.getZombies())) {
            if (candidate != zombie && isHypnotized(candidate)) {
                candidate.kill();
            }
        }
        front.setTileType(TileType.NORMAL);
        destination.setTileType(TileType.ICE);
    }

    protected void handleFisherman(Lane lane, Zombie zombie, ZombieRuntimeState state) {
        if (state.ageTicks == 1) {
            zombie.moveTo(lane.getWidth(), lane.getLaneId());
        }
        if (state.ageTicks % (5 * TICKS_PER_SECOND) != 0) {
            return;
        }
        Plant target = nearestPlantInLane(lane, zombie.getX(), Double.MAX_VALUE, false);
        if (target == null) {
            return;
        }
        if (Math.abs(zombie.getX() - target.getX()) <= 1.0) {
            target.kill();
            return;
        }
        int targetX = Math.min(lane.getWidth(), (int) Math.round(target.getX()) + 1);
        Tile destination = lane.getTileAt(targetX);
        if (destination != null && !destination.hasPlant() && board != null) {
            Position source = new Position((int) Math.round(target.getX()), lane.getLaneId());
            Position destinationPosition = new Position(targetX, lane.getLaneId());
            board.movePlant(source, destinationPosition, target);
        }
    }

    protected void handleOctopus(Lane lane, Zombie zombie, ZombieRuntimeState state) {
        if (state.ageTicks % (5 * TICKS_PER_SECOND) != 0) {
            return;
        }
        Plant target = nearestPlantInLane(lane, zombie.getX(), Double.MAX_VALUE, true);
        if (target != null) {
            target.addOctopus();
        }
    }

    protected void handleWizard(Lane lane, Zombie zombie, ZombieRuntimeState state) {
        if (board == null || state.ageTicks % (5 * TICKS_PER_SECOND) != 0) {
            return;
        }
        List<Plant> candidates = new ArrayList<>();
        for (Plant plant : board.getAllPlants()) {
            if (plant.isAlive() && !plant.isTransformedToCat()) {
                candidates.add(plant);
            }
        }
        if (!candidates.isEmpty()) {
            candidates.get(random.nextInt(candidates.size())).transformToCat(zombie);
        }
    }

    protected void handleKing(Lane lane, Zombie zombie, ZombieRuntimeState state) {
        if (state.ageTicks == 1) {
            zombie.moveTo(lane.getWidth(), lane.getLaneId());
        }
        if (state.ageTicks % (2 * TICKS_PER_SECOND) != 0) {
            return;
        }
        Zombie candidate = null;
        double minimum = Double.MAX_VALUE;
        for (Zombie other : lane.getAllZombies()) {
            if (other == zombie || !other.isAlive()
                    || !normalizeText(other.getName()).equals("default")) {
                continue;
            }
            double distance = Math.abs(other.getX() - zombie.getX());
            if (distance < minimum) {
                minimum = distance;
                candidate = other;
            }
        }
        if (candidate == null) {
            return;
        }
        ZombieFactory factory = new ZombieFactory();
        ZombieType knight = factory.getZombieRegistry().getZombieTypeByName("Knight");
        if (knight != null) {
            Zombie sample = factory.createZombie(knight, candidate.getX(), candidate.getY());
            candidate.transformTo(knight, sample.getArmor());
        }
    }

    protected void handleFrontObjectState(
            Lane lane,
            Zombie zombie,
            ZombieRuntimeState state
    ) {
        String name = normalizeText(zombie.getName());
        if (!name.equals("barrel roller") && !name.equals("arcade")) {
            return;
        }
        if (!state.frontObjectObserved) {
            state.frontObjectObserved = true;
            return;
        }
        if (!state.frontObjectBrokenHandled && !zombie.hasArmor()) {
            state.frontObjectBrokenHandled = true;
            if (name.equals("barrel roller")) {
                spawnZombie("Imp", zombie.getX(), lane.getLaneId());
                spawnZombie("Imp", zombie.getX(), lane.getLaneId());
            }
        }
    }

    protected void handleSpecialZombieDeath(Zombie zombie, ZombieRuntimeState state) {
        if (zombie == null || state == null || state.deathHandled) {
            return;
        }
        state.deathHandled = true;
        String name = normalizeText(zombie.getName());
        if (name.equals("wizard") && board != null) {
            for (Plant plant : board.getAllPlants()) {
                if (plant.getTransformedByWizard() == zombie) {
                    plant.restoreFromCat(zombie);
                }
            }
        }
        if ((name.equals("barrel roller") || name.equals("arcade"))
                && zombie.hasArmor() && board != null) {
            Position position = new Position(
                    Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(zombie.getX()))),
                    Math.max(1, Math.min(board.getHeight(), (int) Math.round(zombie.getY())))
            );
            Tile tile = board.getTileAt(position);
            if (tile != null && tile.getTileType() == TileType.NORMAL) {
                tile.setTileType(name.equals("barrel roller") ? TileType.BARREL : TileType.ARCADE);
            }
        }
    }

    protected void handleZombiePlantCollision(
            Lane lane,
            Zombie zombie,
            ZombieRuntimeState state,
            Plant plant
    ) {
        String zombieName = normalizeText(zombie.getName());
        if (zombieName.equals("gargantuar")
                || zombieName.equals("piano")
                || zombieName.equals("arcade") && zombie.hasArmor()
                || zombieName.equals("explorer") && state.torchLit) {
            plant.kill();
        } else if (zombieName.equals("allstar") && state.allstarCharging) {
            plant.kill();
            state.allstarCharging = false;
            zombie.setCurrentSpeed(scaledBaseSpeed(zombie) * 0.25);
        } else if (zombieName.equals("wizard")) {
            plant.transformToCat(zombie);
        } else {
            double eatMultiplier = (zombieName.equals("news paper")
                    || zombieName.equals("newspaper"))
                    && !zombie.hasArmor() ? 2.0 : 1.0;
            int hpBefore = plant.getHp() + plant.getArmorHp();
            zombie.attack(plant, eatMultiplier);
            String bittenPlantName = normalizeText(plant.getName());
            int hpAfter = plant.getHp() + plant.getArmorHp();
            if (bittenPlantName.equals("sun bean") && hpAfter < hpBefore && board != null) {
                board.restoreSun(5 + Math.max(0, plant.getSunDropBonus()));
            }
        }

        String plantName = normalizeText(plant.getName());
        int reflectedDamage = plant.getReflectDamage();
        if (plantName.equals("endurian")) {
            reflectedDamage = Math.max(20, reflectedDamage);
        }
        if (reflectedDamage > 0 && zombie.isAlive()) {
            zombie.recordDamageSource(
                    plant.getName(),
                    plant.getType() == null ? "" : plant.getType().getCategory(),
                    "reflected"
            );
            zombie.takeDamage(new Damage(reflectedDamage, "reflected"));
        }

        if (plantName.equals("hypno shroom") && zombie.isAlive()) {
            if (plant.hasPlantFoodHypnoGargantuar()) {
                ZombieFactory factory = new ZombieFactory();
                ZombieType gargantuar = factory.getZombieRegistry().getZombieTypeByName("Gargantuar");
                if (gargantuar != null) {
                    zombie.transformTo(gargantuar, null);
                }
            }
            hypnotize(zombie);
            plant.kill();
        } else if (plantName.equals("garlic") && zombie.isAlive()) {
            scheduleGarlicLaneShift(lane, zombie, state);
        }
    }

    protected boolean handleAllstarZombieCollision(
            Lane lane,
            Zombie zombie,
            ZombieRuntimeState state
    ) {
        if (!normalizeText(zombie.getName()).equals("allstar") || !state.allstarCharging) {
            return false;
        }
        for (Zombie other : lane.getAllZombies()) {
            if (other != zombie && other.isAlive() && isHypnotized(other)
                    && Math.abs(other.getX() - zombie.getX()) <= MELEE_RANGE) {
                other.kill();
                state.allstarCharging = false;
                zombie.setCurrentSpeed(scaledBaseSpeed(zombie) * 0.25);
                return true;
            }
        }
        return false;
    }

    protected boolean handleHeavyZombieCollision(Lane lane, Zombie zombie) {
        String name = normalizeText(zombie.getName());
        boolean destructive = name.equals("piano")
                || name.equals("arcade") && zombie.hasArmor();
        if (!destructive) {
            return false;
        }
        for (Zombie other : lane.getAllZombies()) {
            if (other != zombie && other.isAlive() && isHypnotized(other)
                    && Math.abs(other.getX() - zombie.getX()) <= MELEE_RANGE) {
                other.kill();
                return true;
            }
        }
        return false;
    }

    protected void updateSnorkelState(Zombie zombie, Tile tile, Plant plant) {
        if (!normalizeText(zombie.getName()).equals("snorkel")) {
            return;
        }
        boolean water = tile != null && tile.getTileType() == TileType.WATER;
        zombie.setSubmerged(water && plant == null);
    }

    protected void moveZombieByAbility(Zombie zombie, ZombieRuntimeState state) {
        String name = normalizeText(zombie.getName());
        if (name.equals("fisherman") || name.equals("king")) {
            return;
        }
        if (state.prospectorReversed) {
            zombie.moveBy(zombie.getCurrentSpeed(), 0);
        } else if (name.equals("prospector")
                && !state.prospectorDynamiteExtinguished
                && state.ageTicks < 10 * TICKS_PER_SECOND
                && zombie.getX() - zombie.getCurrentSpeed() <= 1.0) {
            zombie.moveTo(1.0, zombie.getY());
        } else {
            zombie.move();
        }
    }

    protected double resolveAbilitySpeed(Zombie zombie, ZombieRuntimeState state) {
        String name = normalizeText(zombie.getName());
        double base = scaledBaseSpeed(zombie);
        if (name.equals("turquoise") && state.turquoiseChannelTicks > 0) {
            return 0;
        }
        if (name.equals("allstar") && state.allstarCharging) {
            return base * 3.0;
        }
        if (name.equals("allstar") && !state.allstarCharging) {
            return base * 0.25;
        }
        if ((name.equals("news paper") || name.equals("newspaper")) && !zombie.hasArmor()) {
            return base * 2.0;
        }
        if (name.equals("juggler") && state.jugglerSpinTicks > 0) {
            return base * 1.5;
        }
        return base;
    }

}
