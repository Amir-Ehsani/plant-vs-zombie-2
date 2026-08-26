package models.core.plant;

import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.combat.BoardTickResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public class PlantFoodContext {
    private final Board board;
    private final PlantFactory plantFactory;
    private final Random random;
    private final IntConsumer sunAdder;
    private final BiConsumer<Plant, Integer> sunBurstSpawner;
    private final BiConsumer<Integer, Runnable> delayedActionScheduler;
    private final Consumer<BoardTickResult> resultRecorder;
    private final Consumer<Plant> plantPlacedHandler;
    private final Consumer<Plant> plantAgeResetHandler;

    public PlantFoodContext(
            Board board,
            PlantFactory plantFactory,
            Random random,
            IntConsumer sunAdder,
            Consumer<BoardTickResult> resultRecorder,
            Consumer<Plant> plantPlacedHandler,
            Consumer<Plant> plantAgeResetHandler
    ) {
        this(
                board,
                plantFactory,
                random,
                sunAdder,
                null,
                null,
                resultRecorder,
                plantPlacedHandler,
                plantAgeResetHandler
        );
    }

    public PlantFoodContext(
            Board board,
            PlantFactory plantFactory,
            Random random,
            IntConsumer sunAdder,
            BiConsumer<Plant, Integer> sunBurstSpawner,
            Consumer<BoardTickResult> resultRecorder,
            Consumer<Plant> plantPlacedHandler,
            Consumer<Plant> plantAgeResetHandler
    ) {
        this(
                board,
                plantFactory,
                random,
                sunAdder,
                sunBurstSpawner,
                null,
                resultRecorder,
                plantPlacedHandler,
                plantAgeResetHandler
        );
    }

    public PlantFoodContext(
            Board board,
            PlantFactory plantFactory,
            Random random,
            IntConsumer sunAdder,
            BiConsumer<Plant, Integer> sunBurstSpawner,
            BiConsumer<Integer, Runnable> delayedActionScheduler,
            Consumer<BoardTickResult> resultRecorder,
            Consumer<Plant> plantPlacedHandler,
            Consumer<Plant> plantAgeResetHandler
    ) {
        this.board = board;
        this.plantFactory = plantFactory;
        this.random = random == null ? new Random() : random;
        this.sunAdder = sunAdder;
        this.sunBurstSpawner = sunBurstSpawner;
        this.delayedActionScheduler = delayedActionScheduler;
        this.resultRecorder = resultRecorder;
        this.plantPlacedHandler = plantPlacedHandler;
        this.plantAgeResetHandler = plantAgeResetHandler;
    }

    public Board getBoard() {
        return board;
    }

    public void addSun(int amount) {
        if (amount > 0 && sunAdder != null) {
            sunAdder.accept(amount);
        }
    }

    public void spawnSunBurst(Plant source, int amount) {
        if (source == null || amount <= 0) {
            return;
        }
        if (sunBurstSpawner != null) {
            sunBurstSpawner.accept(source, amount);
            return;
        }
        addSun(amount);
    }

    public void damageLane(Plant source, int damage, String damageType) {
        if (board == null || source == null || damage <= 0) {
            return;
        }
        record(board.damageZombiesInLane(
                laneOf(source),
                damage,
                damageType,
                source.getName(),
                source.getType() == null ? "" : source.getType().getCategory()
        ));
    }

    public void damageArea(Plant source, int xRadius, int yRadius, int damage, String damageType) {
        if (board == null || source == null || damage <= 0) {
            return;
        }
        record(board.damageZombiesInArea(
                positionOf(source),
                Math.max(0, xRadius),
                Math.max(0, yRadius),
                damage,
                damageType,
                source.getName(),
                source.getType() == null ? "" : source.getType().getCategory()
        ));
    }

    public void damageAreaDelayed(
            Plant source,
            int xRadius,
            int yRadius,
            int damage,
            String damageType,
            int delayTicks
    ) {
        if (source == null || damage <= 0) {
            return;
        }
        schedule(delayTicks, () -> {
            if (source.isAlive()) {
                damageArea(source, xRadius, yRadius, damage, damageType);
            }
        });
    }

    private void schedule(int delayTicks, Runnable action) {
        if (action == null) {
            return;
        }
        if (delayTicks <= 0 || delayedActionScheduler == null) {
            action.run();
            return;
        }
        delayedActionScheduler.accept(delayTicks, action);
    }

    public void damageRandom(Plant source, int count, int damage, String damageType) {
        if (board == null || source == null || count <= 0 || damage <= 0) {
            return;
        }
        record(board.damageRandomZombies(
                count,
                damage,
                damageType,
                random,
                source.getName(),
                source.getType() == null ? "" : source.getType().getCategory()
        ));
    }

    public void killRandom(Plant source, int count, String damageType) {
        if (board == null || source == null || count <= 0) {
            return;
        }
        List<Zombie> zombies = livingEnemies();
        Collections.shuffle(zombies, random);
        int limit = Math.min(count, zombies.size());
        for (int index = 0; index < limit; index++) {
            Zombie zombie = zombies.get(index);
            zombie.recordDamageSource(
                    source.getName(),
                    source.getType() == null ? "" : source.getType().getCategory(),
                    damageType
            );
            zombie.kill();
        }
        record(board.removeDeadEntities());
    }

    public void hypnotizeRandom(int count) {
        if (board == null || count <= 0) {
            return;
        }
        List<Zombie> zombies = livingEnemies();
        Collections.shuffle(zombies, random);
        for (int index = 0; index < Math.min(count, zombies.size()); index++) {
            board.hypnotizeZombie(zombies.get(index));
        }
    }

    public void freezeLane(Plant source, int ticks) {
        if (board == null || source == null || ticks <= 0) {
            return;
        }
        Lane lane = board.getLaneAt(laneOf(source));
        if (lane == null) {
            return;
        }
        for (Zombie zombie : lane.getAllZombies()) {
            board.applyFreeze(zombie, ticks);
        }
    }

    public void freezeAll(int ticks) {
        if (board != null && ticks > 0) {
            board.freezeAllZombies(ticks);
        }
    }

    public void butterAll(int ticks) {
        if (board == null || ticks <= 0) {
            return;
        }
        for (Zombie zombie : board.getAllZombies()) {
            board.applyButter(zombie, ticks);
        }
    }

    public void poisonLane(Plant source, int damagePerTick, int ticks) {
        if (board == null || source == null || damagePerTick <= 0 || ticks <= 0) {
            return;
        }
        Lane lane = board.getLaneAt(laneOf(source));
        if (lane == null) {
            return;
        }
        for (Zombie zombie : lane.getAllZombies()) {
            board.applyPoison(zombie, damagePerTick, ticks);
        }
    }

    public void pushLane(Plant source, double distance) {
        if (board == null || source == null || distance <= 0) {
            return;
        }
        Lane lane = board.getLaneAt(laneOf(source));
        if (lane == null) {
            return;
        }
        for (Zombie zombie : lane.getAllZombies()) {
            if (zombie != null && zombie.isAlive()) {
                zombie.moveBy(distance, 0);
            }
        }
    }

    public void shiftLaneZombies(Plant source) {
        if (board == null || source == null) {
            return;
        }
        Lane lane = board.getLaneAt(laneOf(source));
        if (lane == null) {
            return;
        }
        for (Zombie zombie : new ArrayList<>(lane.getAllZombies())) {
            board.shiftZombieToAdjacentLane(zombie, random);
        }
    }

    public void attractNearbyZombies(Plant source) {
        if (board == null || source == null) {
            return;
        }
        int targetLane = laneOf(source);
        for (Zombie zombie : new ArrayList<>(board.getAllZombies())) {
            if (zombie == null || !zombie.isAlive()) {
                continue;
            }
            int zombieLane = Math.max(1, Math.min(board.getHeight(), (int) Math.round(zombie.getY())));
            if (Math.abs(zombieLane - targetLane) == 1) {
                board.moveZombieToLane(zombie, targetLane);
            }
        }
    }

    public void removeArmorFromRandom(int count) {
        if (board == null || count <= 0) {
            return;
        }
        List<Zombie> armored = new ArrayList<>();
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie != null && zombie.isAlive() && zombie.hasArmor()) {
                armored.add(zombie);
            }
        }
        armored.sort(Comparator.comparingInt(zombie -> -zombie.getArmor().getHp()));
        for (int index = 0; index < Math.min(count, armored.size()); index++) {
            armored.get(index).getArmor().reduceDamage(Integer.MAX_VALUE);
        }
    }

    public void clonePlant(Plant source, int count) {
        if (board == null || plantFactory == null || source == null || count <= 0) {
            return;
        }
        List<Position> empty = emptyPositions();
        Collections.shuffle(empty, random);
        int created = 0;
        for (Position position : empty) {
            if (created >= count) {
                break;
            }
            Plant clone = plantFactory.createPlant(
                    source.getType(),
                    position.getX(),
                    position.getY()
            );
            clone.setLevel(source.getLevel());
            if (board.placePlant(clone, position)) {
                created++;
                if (plantPlacedHandler != null) {
                    plantPlacedHandler.accept(clone);
                }
            }
        }
    }

    public void cloneLilyPads(int count) {
        if (board == null || plantFactory == null || count <= 0) {
            return;
        }
        PlantType lilyPad = plantFactory.getPlantRegistry().getByName("Lily Pad");
        if (lilyPad == null) {
            return;
        }
        List<Position> positions = new ArrayList<>();
        for (Lane lane : board.getLanes()) {
            for (Tile tile : lane.getTiles()) {
                if (tile.getTileType().name().equals("WATER") && !tile.hasPlant()) {
                    positions.add(tile.getPosition());
                }
            }
        }
        Collections.shuffle(positions, random);
        for (int index = 0; index < Math.min(count, positions.size()); index++) {
            Position position = positions.get(index);
            Plant plant = plantFactory.createPlant(lilyPad, position.getX(), position.getY());
            if (board.placePlant(plant, position) && plantPlacedHandler != null) {
                plantPlacedHandler.accept(plant);
            }
        }
    }

    public void resetPlantAges(String plantName) {
        if (board == null || plantName == null || plantAgeResetHandler == null) {
            return;
        }
        for (Plant plant : board.getAllPlants()) {
            if (normalize(plant.getName()).equals(normalize(plantName))) {
                plantAgeResetHandler.accept(plant);
            }
        }
    }

    public int countPlantLayers(Plant source) {
        if (board == null || source == null) {
            return 1;
        }
        Tile tile = board.getTileAt(positionOf(source));
        if (tile == null) {
            return 1;
        }
        int count = 0;
        for (Plant plant : tile.getPlants()) {
            if (normalize(plant.getName()).equals(normalize(source.getName()))) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    public void damagePlant(Plant plant, int amount, String type) {
        if (plant != null && amount > 0) {
            plant.takeDamage(new Damage(amount, type));
        }
    }

    private List<Zombie> livingEnemies() {
        List<Zombie> result = new ArrayList<>();
        if (board == null) {
            return result;
        }
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie != null && zombie.isAlive() && !board.isHypnotized(zombie)) {
                result.add(zombie);
            }
        }
        return result;
    }

    private List<Position> emptyPositions() {
        List<Position> positions = new ArrayList<>();
        if (board == null) {
            return positions;
        }
        for (Lane lane : board.getLanes()) {
            for (Tile tile : lane.getTiles()) {
                if (!tile.hasPlant() && tile.isPlantable()) {
                    positions.add(tile.getPosition());
                }
            }
        }
        return positions;
    }

    private Position positionOf(Plant plant) {
        int x = board == null ? Math.max(1, (int) Math.round(plant.getX()))
                : Math.max(1, Math.min(board.getWidth(), (int) Math.round(plant.getX())));
        int y = board == null ? Math.max(1, (int) Math.round(plant.getY()))
                : Math.max(1, Math.min(board.getHeight(), (int) Math.round(plant.getY())));
        return new Position(x, y);
    }

    private int laneOf(Plant plant) {
        return positionOf(plant).getY();
    }

    private void record(BoardTickResult result) {
        if (resultRecorder != null && result != null) {
            resultRecorder.accept(result);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase()
                .replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }
}
