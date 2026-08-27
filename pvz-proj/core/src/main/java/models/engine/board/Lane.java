package models.engine.board;

import models.core.zombie.Zombie;
import models.engine.combat.DefaultLaneCombatStrategy;
import models.engine.combat.LaneCombatStrategy;
import models.engine.combat.LaneTickResult;
import models.entities.LawnMower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Lane {
    private final int laneId;
    private final List<Tile> tiles;
    private final LawnMower lawnMower;
    private LaneCombatStrategy combatStrategy;
    private LaneTickResult lastTickResult;
    private int totalZombiesKilled;
    private int totalPlantsDestroyed;
    private boolean brainEaten;
    private boolean continueAfterBrainEaten;

    public Lane(int laneId, int width) {
        if (laneId <= 0) {
            throw new IllegalArgumentException("Lane id must be greater than 0.");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be greater than 0.");
        }

        this.laneId = laneId;
        this.tiles = new ArrayList<>();
        this.lawnMower = new LawnMower(laneId);
        this.combatStrategy = new DefaultLaneCombatStrategy();
        this.lastTickResult = LaneTickResult.empty();
        this.totalZombiesKilled = 0;
        this.totalPlantsDestroyed = 0;
        this.brainEaten = false;
        this.continueAfterBrainEaten = false;

        initializeTiles(width);
    }

    private void initializeTiles(int width) {
        for (int x = 1; x <= width; x++) {
            Position position = new Position(x, laneId);
            tiles.add(new Tile(position));
        }
    }

    public LaneTickResult updateLaneTicks() {
        if (brainEaten && !continueAfterBrainEaten) {
            return lastTickResult;
        }

        lastTickResult = combatStrategy.updateLane(this);
        totalZombiesKilled += lastTickResult.getZombiesKilled();
        totalPlantsDestroyed += lastTickResult.getPlantsDestroyed();
        brainEaten = brainEaten || lastTickResult.isBrainEaten();
        return lastTickResult;
    }

    public int getLaneId() {
        return laneId;
    }

    public int getWidth() {
        return tiles.size();
    }

    public List<Tile> getTiles() {
        return Collections.unmodifiableList(tiles);
    }

    public Tile getTileAt(int x) {
        if (x <= 0 || x > tiles.size()) {
            return null;
        }
        return tiles.get(x - 1);
    }

    public LawnMower getLawnMower() {
        return lawnMower;
    }

    public LaneTickResult getLastTickResult() {
        return lastTickResult;
    }

    public int getTotalZombiesKilled() {
        return totalZombiesKilled;
    }

    public int getTotalPlantsDestroyed() {
        return totalPlantsDestroyed;
    }

    public boolean hasBrainBeenEaten() {
        return brainEaten;
    }


    public void setContinueAfterBrainEaten(boolean continueAfterBrainEaten) {
        this.continueAfterBrainEaten = continueAfterBrainEaten;
    }

    public int getActiveZombieCount() {
        return getAllZombies().size();
    }

    public List<Zombie> getAllZombies() {
        List<Zombie> zombies = new ArrayList<>();
        for (Tile tile : tiles) {
            for (Zombie zombie : tile.getZombies()) {
                if (zombie != null && zombie.isAlive() && !zombies.contains(zombie)) {
                    zombies.add(zombie);
                }
            }
        }
        return Collections.unmodifiableList(zombies);
    }

    public void setCombatStrategy(LaneCombatStrategy combatStrategy) {
        if (combatStrategy == null) {
            throw new IllegalArgumentException("Combat strategy cannot be null.");
        }
        this.combatStrategy = combatStrategy;
    }
}
