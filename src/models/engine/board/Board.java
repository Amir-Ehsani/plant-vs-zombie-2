package models.engine.board;

import models.core.plant.Plant;
import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.engine.combat.BoardTickResult;
import models.engine.combat.LaneTickResult;
import models.engine.events.GameEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board {
    private static final int DEFAULT_WIDTH = 9;
    private static final int DEFAULT_HEIGHT = 5;

    private final int width;
    private final int height;
    private final List<Lane> lanes;
    private BoardTickResult lastTickResult;
    private int totalZombiesKilled;
    private int totalPlantsDestroyed;
    private boolean brainEaten;

    public Board() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Board(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("Board width must be greater than 0.");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Board height must be greater than 0.");
        }

        this.width = width;
        this.height = height;
        this.lanes = new ArrayList<>();
        this.lastTickResult = BoardTickResult.empty();
        this.totalZombiesKilled = 0;
        this.totalPlantsDestroyed = 0;
        this.brainEaten = false;

        initializeLanes();
    }

    private void initializeLanes() {
        for (int y = 1; y <= height; y++) {
            lanes.add(new Lane(y, width));
        }
    }

    public BoardTickResult updateTicks() {
        int zombiesKilled = 0;
        int plantsDestroyed = 0;
        int mowersTriggered = 0;
        boolean brainWasEaten = false;
        List<GameEvent> events = new ArrayList<>();

        for (Lane lane : lanes) {
            LaneTickResult result = lane.updateLaneTicks();
            zombiesKilled += result.getZombiesKilled();
            plantsDestroyed += result.getPlantsDestroyed();
            if (result.isLawnMowerTriggered()) {
                mowersTriggered++;
            }
            brainWasEaten = brainWasEaten || result.isBrainEaten();
            events.addAll(result.getEvents());
        }

        totalZombiesKilled += zombiesKilled;
        totalPlantsDestroyed += plantsDestroyed;
        brainEaten = brainEaten || brainWasEaten;
        lastTickResult = new BoardTickResult(
                zombiesKilled,
                plantsDestroyed,
                mowersTriggered,
                brainWasEaten,
                events
        );
        return lastTickResult;
    }

    public BoardTickResult applyAreaDamage(
            Position center,
            int zombieRadius,
            int zombieDamage,
            int plantRadius,
            int plantDamage
    ) {
        if (center == null) {
            throw new IllegalArgumentException("Damage center cannot be null.");
        }
        if (zombieRadius < 0 || plantRadius < 0 || zombieDamage < 0 || plantDamage < 0) {
            throw new IllegalArgumentException("Damage values and radii cannot be negative.");
        }

        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                Position position = tile.getPosition();
                int xDistance = Math.abs(position.getX() - center.getX());
                int yDistance = Math.abs(position.getY() - center.getY());

                if (xDistance <= zombieRadius && yDistance <= zombieRadius) {
                    for (Zombie zombie : new ArrayList<>(tile.getZombies())) {
                        if (zombie != null && zombie.isAlive()) {
                            zombie.takeDamage(new Damage(zombieDamage, "radioactive sun"));
                        }
                    }
                }

                Plant plant = tile.getCurrentPlant();
                if (plant != null && plant.isAlive()
                        && xDistance <= plantRadius && yDistance <= plantRadius) {
                    plant.takeDamage(new Damage(plantDamage, "radioactive sun"));
                }
            }
        }

        return removeDeadEntities();
    }

    public BoardTickResult removeDeadEntities() {
        int zombiesKilled = 0;
        int plantsDestroyed = 0;
        List<GameEvent> events = new ArrayList<>();

        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                Plant plant = tile.getCurrentPlant();
                if (plant != null && !plant.isAlive()) {
                    tile.removePlant();
                    plantsDestroyed++;
                    events.add(GameEvent.plantDestroyed(
                            plant.getName(),
                            tile.getPosition()
                    ));
                }

                for (Zombie zombie : new ArrayList<>(tile.getZombies())) {
                    if (zombie != null && !zombie.isAlive()) {
                        tile.removeZombie(zombie);
                        zombiesKilled++;
                        events.add(GameEvent.zombieKilled(zombie, false));
                    }
                }
            }
        }

        totalZombiesKilled += zombiesKilled;
        totalPlantsDestroyed += plantsDestroyed;

        return new BoardTickResult(
                zombiesKilled,
                plantsDestroyed,
                0,
                false,
                events
        );
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<Lane> getLanes() {
        return Collections.unmodifiableList(lanes);
    }

    public Lane getLaneAt(int y) {
        if (y <= 0 || y > height) {
            return null;
        }
        return lanes.get(y - 1);
    }

    public Tile getTileAt(Position position) {
        if (position == null) {
            return null;
        }

        Lane lane = getLaneAt(position.getY());
        if (lane == null) {
            return null;
        }
        return lane.getTileAt(position.getX());
    }

    public boolean isValidPosition(Position position) {
        return getTileAt(position) != null;
    }

    public boolean canPlacePlant(Position position) {
        Tile tile = getTileAt(position);
        return tile != null && tile.isPlantable() && !tile.hasPlant();
    }

    public boolean placePlant(Plant plant, Position position) {
        if (plant == null || !canPlacePlant(position)) {
            return false;
        }

        Tile tile = getTileAt(position);
        tile.placePlant(plant);
        return true;
    }

    public Plant removePlant(Position position) {
        Tile tile = getTileAt(position);
        if (tile == null || !tile.hasPlant()) {
            return null;
        }
        return tile.removePlant();
    }

    public List<Zombie> getAllZombies() {
        List<Zombie> zombies = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Zombie zombie : lane.getAllZombies()) {
                if (!zombies.contains(zombie)) {
                    zombies.add(zombie);
                }
            }
        }
        return Collections.unmodifiableList(zombies);
    }

    public List<Plant> getAllPlants() {
        List<Plant> plants = new ArrayList<>();
        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                Plant plant = tile.getCurrentPlant();
                if (plant != null && plant.isAlive()) {
                    plants.add(plant);
                }
            }
        }
        return Collections.unmodifiableList(plants);
    }

    public int getActiveZombieCount() {
        return getAllZombies().size();
    }

    public int getPlantCount() {
        return getAllPlants().size();
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

    public int destroyAllZombies() {
        int destroyed = 0;
        for (Lane lane : lanes) {
            for (Tile tile : lane.getTiles()) {
                for (Zombie zombie : new ArrayList<>(tile.getZombies())) {
                    if (zombie != null && zombie.isAlive()) {
                        zombie.kill();
                        destroyed++;
                    }
                }
                tile.clearZombies();
            }
        }
        totalZombiesKilled += destroyed;
        return destroyed;
    }

    public BoardTickResult getLastTickResult() {
        return lastTickResult;
    }
}
