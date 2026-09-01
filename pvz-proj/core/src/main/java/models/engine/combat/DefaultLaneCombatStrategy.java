package models.engine.combat;

import models.core.plant.Plant;
import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.events.GameEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class DefaultLaneCombatStrategy extends LaneCombatPlantSupport implements LaneCombatStrategy {
    private static final int OCTOPUS_BREAK_STEP_TICKS = 3 * TICKS_PER_SECOND;
    public DefaultLaneCombatStrategy() {
        super(null);
    }

    public DefaultLaneCombatStrategy(Board board) {
        super(board);
    }

    public DefaultLaneCombatStrategy(Board board, Random random) {
        super(board, random);
    }

    @Override
    public LaneTickResult updateLane(Lane lane) {
        if (lane == null) {
            throw new IllegalArgumentException("Lane cannot be null.");
        }
        List<Zombie> zombiesAtStart = collectZombiesAtTickStart(lane);
        List<Zombie> livingAtStart = new ArrayList<>();
        for (Zombie zombie : zombiesAtStart) {
            if (zombie.isAlive()) {
                livingAtStart.add(zombie);
            }
        }
        Map<Plant, Position> plantPositions = capturePlantPositions(lane);
        Set<Plant> consumedPlants = Collections.newSetFromMap(new IdentityHashMap<>());
        List<GameEvent> events = new ArrayList<>();
        updatePlants(lane, consumedPlants);
        updateZombies(lane, livingAtStart);
        boolean mowerWasReady = lane.getLawnMower().isReady();
        List<Zombie> mowerKilled = handleLaneEnd(lane, livingAtStart);
        boolean mowerTriggered = mowerWasReady && lane.getLawnMower().isTriggered();
        int plantsDestroyed = removeDeadPlants(lane, consumedPlants, plantPositions, events);
        int zombiesKilled = appendZombieDeathEvents(zombiesAtStart, mowerKilled, events);
        if (mowerTriggered) {
            appendMowerEvent(lane, mowerKilled, events);
        }
        redistributeLivingZombies(lane, livingAtStart);
        return new LaneTickResult(
                zombiesKilled, plantsDestroyed, mowerTriggered,
                hasLivingZombieAtLaneEnd(livingAtStart), events
        );
    }

    private List<Zombie> collectZombiesAtTickStart(Lane lane) {
        List<Zombie> zombies = new ArrayList<>();
        Set<Zombie> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Tile tile : lane.getTiles()) {
            for (Zombie zombie : tile.getZombies()) {
                if (zombie != null && seen.add(zombie)) {
                    zombies.add(zombie);
                }
            }
        }
        return zombies;
    }

    private int appendZombieDeathEvents(
            List<Zombie> zombiesAtStart, List<Zombie> mowerKilled, List<GameEvent> events
    ) {
        Set<Zombie> mowerKilledSet = Collections.newSetFromMap(new IdentityHashMap<>());
        mowerKilledSet.addAll(mowerKilled);
        int killed = 0;
        for (Zombie zombie : zombiesAtStart) {
            if (zombie.isAlive()) {
                continue;
            }
            killed++;
            events.add(GameEvent.zombieKilled(zombie, mowerKilledSet.contains(zombie)));
        }
        return killed;
    }

    private void appendMowerEvent(Lane lane, List<Zombie> mowerKilled, List<GameEvent> events) {
        List<String> killedNames = new ArrayList<>();
        for (Zombie zombie : mowerKilled) {
            killedNames.add(zombie.getName());
        }
        events.add(GameEvent.lawnMowerTriggered(lane.getLaneId(), killedNames));
    }

    public void handleExternalZombieDeath(Zombie zombie) {
        if (zombie != null && !zombie.isAlive()) {
            handleSpecialZombieDeath(zombie, stateOf(zombie));
        }
    }

    public void handleExternalPlantDeath(Plant plant, Position position, List<GameEvent> events) {
        if (plant == null || position == null) {
            return;
        }
        triggerPlantDeathEffect(plant, position, events == null ? new ArrayList<>() : events);
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

    private void updatePlants(Lane lane, Set<Plant> consumedPlants) {
        for (Tile tile : lane.getTiles()) {
            for (Plant plant : new ArrayList<>(tile.getPlants())) {
                if (plant == null || !plant.isAlive()) {
                    continue;
                }

                PlantRuntimeState state = plantStateOf(plant);
                state.ageTicks++;
                state.bowlingBlueRechargeTicks = Math.max(0, state.bowlingBlueRechargeTicks - 1);
                state.bowlingOrangeRechargeTicks = Math.max(0, state.bowlingOrangeRechargeTicks - 1);
                plant.tickCooldown();
                updateOctopusCover(plant, state);
                if (plant.consumeExplosiveArmorBreak()) {
                    damageArea(tile.getPosition(), 1, 1,
                            Math.max(1800, plant.getExplodeDamage()),
                            "armor explosion", plant, null);
                    plant.triggerSpecialAnimation("attack");
                }
                applyPassivePlantEnvironment(plant, state);

                if (isLifespanExpired(plant, state)) {
                    plant.takeDamage(new Damage(plant.getMaxHp(), "lifespan"));
                    continue;
                }
                if (tile.isFrozenTerrain()) {
                    continue;
                }
                if (plant.isFrozenByZombie()) {
                    plant.damageIce(2, false);
                    continue;
                }
                if (plant.isDisabled()) {
                    continue;
                }
                if (state.digestTicks > 0) {
                    state.digestTicks--;
                    continue;
                }

                if (handlePassiveOrSpecialPlant(lane, tile, plant, state, consumedPlants)) {
                    continue;
                }
                if (plant.getCooldownRemaining() > 0 || !canPlantAttack(plant)) {
                    continue;
                }

                performPlantAttack(lane, tile, plant, state);
            }
        }
    }

    private void updateOctopusCover(Plant plant, PlantRuntimeState state) {
        if (!plant.isCoveredByOctopus()) {
            state.octopusBreakTicks = 0;
            return;
        }
        state.octopusBreakTicks++;
        if (state.octopusBreakTicks >= OCTOPUS_BREAK_STEP_TICKS) {
            state.octopusBreakTicks = 0;
            plant.damageOctopus();
        }
    }

    private void applyPassivePlantEnvironment(Plant source, PlantRuntimeState state) {
        if (board == null || source == null || !isFirePlant(source)
                || state.ageTicks % TICKS_PER_SECOND != 0) {
            return;
        }
        int radius = Math.max(1, source.getWarmthRadius());
        int centerX = (int) Math.round(source.getX());
        int centerY = (int) Math.round(source.getY());
        board.meltTerrainArea(new Position(centerX, centerY), radius);
        for (int lane = Math.max(1, centerY - radius);
                lane <= Math.min(board.getHeight(), centerY + radius); lane++) {
            Lane candidateLane = board.getLaneAt(lane);
            if (candidateLane == null) {
                continue;
            }
            for (int x = Math.max(1, centerX - radius);
                    x <= Math.min(board.getWidth(), centerX + radius); x++) {
                Tile tile = candidateLane.getTileAt(x);
                if (tile == null) {
                    continue;
                }
                for (Plant neighbor : tile.getPlants()) {
                    if (neighbor != null && neighbor != source && neighbor.getIceHits() > 0) {
                        neighbor.removeIceHit();
                    }
                }
            }
        }
    }

}
