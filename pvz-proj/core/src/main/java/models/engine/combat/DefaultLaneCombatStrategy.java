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


public class DefaultLaneCombatStrategy extends LaneCombatPlantSupport implements LaneCombatStrategy {
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
        if (lane == null) throw new IllegalArgumentException("Lane cannot be null.");
        List<Zombie> livingAtStart = collectLivingZombies(lane);
        Map<Plant, Position> plantPositions = capturePlantPositions(lane);
        Set<Plant> consumedPlants = Collections.newSetFromMap(new IdentityHashMap<>());
        List<GameEvent> events = new ArrayList<>();
        updatePlants(lane, consumedPlants);
        updateZombies(lane, livingAtStart);
        boolean mowerWasReady = lane.getLawnMower().isReady();
        List<Zombie> mowerKilled = handleLaneEnd(lane, livingAtStart);
        boolean mowerTriggered = mowerWasReady && lane.getLawnMower().isTriggered();
        int plantsDestroyed = removeDeadPlants(lane, consumedPlants, plantPositions, events);
        int zombiesKilled = appendZombieDeathEvents(livingAtStart, mowerKilled, events);
        if (mowerTriggered) appendMowerEvent(lane, mowerKilled, events);
        redistributeLivingZombies(lane, livingAtStart);
        return new LaneTickResult(
                zombiesKilled, plantsDestroyed, mowerTriggered,
                hasLivingZombieAtLaneEnd(livingAtStart), events
        );
    }

    private int appendZombieDeathEvents(
            List<Zombie> livingAtStart, List<Zombie> mowerKilled, List<GameEvent> events
    ) {
        Set<Zombie> mowerKilledSet = Collections.newSetFromMap(new IdentityHashMap<>());
        mowerKilledSet.addAll(mowerKilled);
        int killed = 0;
        for (Zombie zombie : livingAtStart) {
            if (zombie.isAlive()) continue;
            killed++;
            events.add(GameEvent.zombieKilled(zombie, mowerKilledSet.contains(zombie)));
        }
        return killed;
    }

    private void appendMowerEvent(Lane lane, List<Zombie> mowerKilled, List<GameEvent> events) {
        List<String> killedNames = new ArrayList<>();
        for (Zombie zombie : mowerKilled) killedNames.add(zombie.getName());
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
                plant.tickCooldown();

                if (isLifespanExpired(plant, state)) {
                    plant.takeDamage(new Damage(plant.getMaxHp(), "lifespan"));
                    continue;
                }
                if (tile.isFrozenTerrain() || plant.isDisabled()) {
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

}
