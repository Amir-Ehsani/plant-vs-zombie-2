package network.game;

import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.minigame.IZombieGame;
import network.protocol.EntityState;
import network.protocol.GameRole;
import network.protocol.GameSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Server-authoritative online I, Zombie built on the same Board/Plant/Zombie combat core
 * used by normal adventure levels. Networking owns commands, economy and snapshots only;
 * movement, attacks, armor, abilities and damage all come from the shared game core.
 */
public final class AuthoritativeIZombieGame {
    public static final int ROWS = 5;
    public static final int COLUMNS = 9;
    public static final int LAST_PLANT_COLUMN = IZombieGame.RED_LINE_COLUMN - 1;
    public static final long DEFAULT_MATCH_DURATION_MILLIS = 120_000L;

    private static final long CORE_TICK_MILLIS = 100L;
    private static final long PASSIVE_INCOME_INTERVAL_MILLIS = 5_000L;
    private static final int PLANT_PASSIVE_INCOME = 25;
    private static final int ZOMBIE_PASSIVE_INCOME = 0;
    private static final long SUNFLOWER_INCOME_INTERVAL_MILLIS = 7_000L;
    private static final int SUNFLOWER_INCOME = 50;
    private static final int SUNFLOWER_EAT_REWARD = 200;

    private static final Map<String, Integer> PLANT_COSTS = createPlantCosts();
    private static final Map<String, Integer> ZOMBIE_COSTS = createZombieCosts();

    private final String plantsUsername;
    private final String zombiesUsername;
    private final int stage;
    private final long matchDurationMillis;
    private final Board board;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Map<String, Long> plantReadyAt;
    private final Map<String, Long> zombieReadyAt;
    private final Set<String> previousSunflowerIds;

    private long elapsedMillis;
    private long lastPassiveIncomeAt;
    private long lastSunflowerIncomeAt;
    private long boardTickRemainderMillis;
    private long sequence;
    private int plantSun;
    private int zombieSun;
    private GameRole winner;
    private String finishReason;

    public AuthoritativeIZombieGame(String plantsUsername, String zombiesUsername) {
        this(plantsUsername, zombiesUsername, 1, DEFAULT_MATCH_DURATION_MILLIS);
    }

    public AuthoritativeIZombieGame(String plantsUsername, String zombiesUsername, int stage) {
        this(plantsUsername, zombiesUsername, stage, DEFAULT_MATCH_DURATION_MILLIS);
    }

    /** Public primarily so deterministic tests and Couch Play can use shorter custom rounds. */
    public AuthoritativeIZombieGame(
            String plantsUsername,
            String zombiesUsername,
            int stage,
            long durationMillis
    ) {
        this.plantsUsername = requireName(plantsUsername, "plants username");
        this.zombiesUsername = requireName(zombiesUsername, "zombies username");
        if (this.plantsUsername.equalsIgnoreCase(this.zombiesUsername)) {
            throw new IllegalArgumentException("players must be different users");
        }
        this.stage = 1; // Online I, Zombie intentionally has no stage selection.
        this.matchDurationMillis = Math.max(1_000L, durationMillis);
        this.board = new Board(COLUMNS, ROWS);
        this.plantFactory = new PlantFactory();
        this.zombieFactory = new ZombieFactory();
        this.plantReadyAt = new LinkedHashMap<>();
        this.zombieReadyAt = new LinkedHashMap<>();
        this.previousSunflowerIds = new HashSet<>();
        this.elapsedMillis = 0L;
        this.lastPassiveIncomeAt = 0L;
        this.lastSunflowerIncomeAt = 0L;
        this.boardTickRemainderMillis = 0L;
        this.sequence = 1L;
        this.plantSun = IZombieGame.INITIAL_SUN;
        this.zombieSun = IZombieGame.INITIAL_SUN;
        this.winner = null;
        this.finishReason = "";
        configureLevelOneBaseBoard();
    }

    public static Map<String, Integer> plantCosts() {
        return PLANT_COSTS;
    }

    public static Map<String, Integer> zombieCosts() {
        return ZOMBIE_COSTS;
    }

    /** Compatibility for Couch Play. Online matchmaking itself always uses the single stage-one base. */
    public static Map<String, Integer> plantCostsForStage(int requestedStage) {
        return PLANT_COSTS;
    }

    /** Compatibility for Couch Play. Online matchmaking itself always uses the single stage-one base. */
    public static Map<String, Integer> zombieCostsForStage(int requestedStage) {
        return ZOMBIE_COSTS;
    }

    public synchronized ActionResult placePlant(String actorUsername, String type, int row, int column) {
        if (isFinished()) {
            return ActionResult.failure("match is already finished", snapshot());
        }
        if (!plantsUsername.equalsIgnoreCase(requireName(actorUsername, "actor username"))) {
            return ActionResult.failure("only the plant player can place plants", snapshot());
        }
        if (!validRow(row) || column < 0 || column > LAST_PLANT_COLUMN) {
            return ActionResult.failure("plant position is outside the plant side", snapshot());
        }

        String key = normalize(type);
        Integer cost = PLANT_COSTS.get(key);
        PlantType plantType = plantType(key);
        if (cost == null || plantType == null) {
            return ActionResult.failure("plant type is not available", snapshot());
        }
        if (plantSun < cost) {
            return ActionResult.failure("not enough sun", snapshot());
        }
        long cooldown = remainingCooldown(plantReadyAt, key);
        if (cooldown > 0L) {
            return ActionResult.failure("plant packet is recharging", snapshot());
        }

        Position position = new Position(column + 1, row + 1);
        Plant plant;
        try {
            plant = plantFactory.createPlant(plantType, position.getX(), position.getY());
        } catch (RuntimeException exception) {
            return ActionResult.failure("plant could not be created", snapshot());
        }
        if (!board.placePlant(plant, position)) {
            return ActionResult.failure("that tile cannot accept this plant", snapshot());
        }

        plantSun -= cost;
        plantReadyAt.put(key, elapsedMillis + Math.max(CORE_TICK_MILLIS, plantType.getRecharge() * CORE_TICK_MILLIS));
        if (isSunflower(plant)) {
            previousSunflowerIds.add(plant.getId());
        }
        sequence++;
        return ActionResult.success(plant.getName() + " planted", snapshot());
    }

    public synchronized ActionResult spawnZombie(String actorUsername, String type, int row) {
        if (isFinished()) {
            return ActionResult.failure("match is already finished", snapshot());
        }
        if (!zombiesUsername.equalsIgnoreCase(requireName(actorUsername, "actor username"))) {
            return ActionResult.failure("only the zombie player can release zombies", snapshot());
        }
        if (!validRow(row)) {
            return ActionResult.failure("zombie lane is invalid", snapshot());
        }

        String key = normalizeZombie(type);
        Integer cost = ZOMBIE_COSTS.get(key);
        if (cost == null) {
            return ActionResult.failure("zombie type is not available", snapshot());
        }
        if (zombieSun < cost) {
            return ActionResult.failure("not enough sun", snapshot());
        }
        long cooldown = remainingCooldown(zombieReadyAt, key);
        if (cooldown > 0L) {
            return ActionResult.failure("zombie packet is recharging", snapshot());
        }

        String zombieName = zombieName(key);
        Zombie zombie;
        try {
            zombie = zombieFactory.createZombie(zombieName, board.getWidth() + 3.15, row + 1);
        } catch (RuntimeException exception) {
            return ActionResult.failure("zombie could not be created", snapshot());
        }
        Tile entrance = board.getTileAt(new Position(board.getWidth(), row + 1));
        if (entrance == null) {
            return ActionResult.failure("zombie lane is invalid", snapshot());
        }
        entrance.addZombie(zombie);

        zombieSun -= cost;
        zombieReadyAt.put(key, elapsedMillis + zombieRechargeMillis(cost));
        sequence++;
        return ActionResult.success(zombie.getName() + " released", snapshot());
    }

    public synchronized void advance(long deltaMillis) {
        if (isFinished()) {
            return;
        }
        long remainingBefore = getRemainingMillis();
        long delta = Math.min(Math.max(0L, deltaMillis), remainingBefore);
        if (delta <= 0L) {
            finish(GameRole.PLANTS, "Plants survived until the timer expired");
            return;
        }

        elapsedMillis += delta;
        boardTickRemainderMillis += delta;
        producePassiveIncome();
        produceSunflowerIncome();

        while (boardTickRemainderMillis >= CORE_TICK_MILLIS && !isFinished()) {
            boardTickRemainderMillis -= CORE_TICK_MILLIS;
            Set<String> before = livingSunflowerIds();
            board.updateTicks();
            rewardEatenSunflowers(before, livingSunflowerIds());
            if (getBrainsRemaining() == 0) {
                finish(GameRole.ZOMBIES, "Zombies ate every brain");
            }
        }

        if (!isFinished() && elapsedMillis >= matchDurationMillis) {
            finish(GameRole.PLANTS, "Plants survived for the full match time");
        }
        if (!isFinished()) {
            sequence++;
        }
    }

    public synchronized void forceFinish(GameRole forcedWinner, String reason) {
        if (isFinished()) {
            return;
        }
        if (forcedWinner == null) {
            throw new IllegalArgumentException("winner is required");
        }
        finish(forcedWinner, reason == null || reason.isBlank() ? "Match ended" : reason);
    }

    public synchronized GameSnapshot snapshot() {
        List<EntityState> entities = new ArrayList<>();
        for (Plant plant : board.getAllPlants()) {
            int row = clamp((int) Math.round(plant.getY()) - 1, 0, ROWS - 1);
            int column = clamp((int) Math.round(plant.getX()) - 1, 0, COLUMNS - 1);
            Map<String, String> attributes = new LinkedHashMap<>();
            attributes.put("column", String.valueOf(column));
            attributes.put("side", GameRole.PLANTS.name());
            entities.add(new EntityState(
                    plant.getId(),
                    "PLANT",
                    normalize(plant.getName()),
                    row,
                    column,
                    plant.getHp(),
                    plant.getMaxHp(),
                    attributes
            ));
        }
        for (Zombie zombie : board.getAllZombies()) {
            int row = clamp((int) Math.round(zombie.getY()) - 1, 0, ROWS - 1);
            Map<String, String> attributes = new LinkedHashMap<>();
            attributes.put("side", GameRole.ZOMBIES.name());
            entities.add(new EntityState(
                    zombie.getId(),
                    "ZOMBIE",
                    normalizeZombie(zombie.getName()),
                    row,
                    zombie.getX() - 1.0,
                    zombie.getHp(),
                    zombie.getMaxHp(),
                    attributes
            ));
        }

        return new GameSnapshot(
                sequence,
                isFinished() ? "FINISHED" : "RUNNING",
                1,
                elapsedMillis,
                getRemainingMillis(),
                plantSun,
                zombieSun,
                brainState(),
                entities,
                remainingCooldowns(plantReadyAt),
                remainingCooldowns(zombieReadyAt),
                winner,
                finishReason
        );
    }

    public synchronized boolean isFinished() {
        return winner != null;
    }

    public synchronized GameRole getWinner() {
        return winner;
    }

    public synchronized String getFinishReason() {
        return finishReason;
    }

    public synchronized long getRemainingMillis() {
        return Math.max(0L, matchDurationMillis - elapsedMillis);
    }

    public synchronized long getElapsedMillis() {
        return elapsedMillis;
    }

    public int getStage() {
        return stage;
    }

    public String getPlantsUsername() {
        return plantsUsername;
    }

    public String getZombiesUsername() {
        return zombiesUsername;
    }

    private void configureLevelOneBaseBoard() {
        board.setGraveSpawningAllowed(true);
        board.setTileType(new Position(5, 1), TileType.GRAVE);
        board.setTileType(new Position(6, 3), TileType.GRAVE);
        board.setTileType(new Position(4, 5), TileType.GRAVE);
        for (Lane lane : board.getLanes()) {
            lane.getLawnMower().disable();
            lane.setContinueAfterBrainEaten(true);
        }
        // Deliberately do not pre-place plants or zombies. The online match starts empty.
    }

    private void producePassiveIncome() {
        while (lastPassiveIncomeAt + PASSIVE_INCOME_INTERVAL_MILLIS <= elapsedMillis) {
            lastPassiveIncomeAt += PASSIVE_INCOME_INTERVAL_MILLIS;
            plantSun += PLANT_PASSIVE_INCOME;
            zombieSun += ZOMBIE_PASSIVE_INCOME;
        }
    }

    private void produceSunflowerIncome() {
        while (lastSunflowerIncomeAt + SUNFLOWER_INCOME_INTERVAL_MILLIS <= elapsedMillis) {
            lastSunflowerIncomeAt += SUNFLOWER_INCOME_INTERVAL_MILLIS;
            int living = 0;
            for (Plant plant : board.getAllPlants()) {
                if (isSunflower(plant)) {
                    living++;
                }
            }
            plantSun += living * SUNFLOWER_INCOME;
        }
    }

    private Set<String> livingSunflowerIds() {
        Set<String> result = new HashSet<>();
        for (Plant plant : board.getAllPlants()) {
            if (isSunflower(plant)) {
                result.add(plant.getId());
            }
        }
        return result;
    }

    private void rewardEatenSunflowers(Set<String> before, Set<String> after) {
        if (before == null || before.isEmpty()) {
            previousSunflowerIds.clear();
            if (after != null) {
                previousSunflowerIds.addAll(after);
            }
            return;
        }
        for (String id : before) {
            if (after == null || !after.contains(id)) {
                zombieSun += SUNFLOWER_EAT_REWARD;
            }
        }
        previousSunflowerIds.clear();
        if (after != null) {
            previousSunflowerIds.addAll(after);
        }
    }

    private boolean[] brainState() {
        boolean[] brains = new boolean[ROWS];
        for (int row = 0; row < ROWS; row++) {
            Lane lane = board.getLaneAt(row + 1);
            brains[row] = lane != null && !lane.hasBrainBeenEaten();
        }
        return brains;
    }

    private int getBrainsRemaining() {
        int remaining = 0;
        for (boolean brain : brainState()) {
            if (brain) {
                remaining++;
            }
        }
        return remaining;
    }

    private void finish(GameRole decidedWinner, String reason) {
        if (winner != null) {
            return;
        }
        winner = Objects.requireNonNull(decidedWinner, "winner");
        finishReason = reason == null ? "" : reason;
        sequence++;
    }

    private long remainingCooldown(Map<String, Long> readyAt, String type) {
        return Math.max(0L, readyAt.getOrDefault(type, 0L) - elapsedMillis);
    }

    private Map<String, Long> remainingCooldowns(Map<String, Long> readyAt) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : readyAt.entrySet()) {
            result.put(entry.getKey(), Math.max(0L, entry.getValue() - elapsedMillis));
        }
        return result;
    }

    private static long zombieRechargeMillis(int cost) {
        return Math.max(1_500L, Math.min(6_500L, 1_000L + cost * 15L));
    }

    private static Map<String, Integer> createPlantCosts() {
        PlantRegistry registry = DefaultPlantRegistry.getInstance();
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (String name : List.of(
                "Sunflower",
                "Peashooter",
                "Wall-nut",
                "Repeater",
                "Snow Pea",
                "Tall-nut",
                "Threepeater"
        )) {
            PlantType type = registry.getByName(name);
            if (type != null) {
                result.put(normalize(name), type.getSunCost());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Integer> createZombieCosts() {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (int localStage = 1; localStage <= 3; localStage++) {
            for (IZombieGame.ZombieOptionView option : IZombieGame.zombieOptionsForStage(localStage)) {
                result.putIfAbsent(normalizeZombie(option.zombieName()), option.sunCost());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static PlantType plantType(String normalizedType) {
        PlantRegistry registry = DefaultPlantRegistry.getInstance();
        for (PlantType type : registry.getAllPlantTypes()) {
            if (normalize(type.getName()).equals(normalizedType)) {
                return type;
            }
        }
        return null;
    }

    private static boolean isSunflower(Plant plant) {
        return plant != null && normalize(plant.getName()).equals("SUNFLOWER");
    }

    private static String zombieName(String normalized) {
        return switch (normalizeZombie(normalized)) {
            case "CONE_HEAD" -> "cone head";
            case "BUCKET_HEAD" -> "bucket head";
            case "RA" -> "Ra";
            case "EXPLORER" -> "Explorer";
            case "ALLSTAR" -> "Allstar";
            case "WIZARD" -> "Wizard";
            case "PROSPECTOR" -> "Prospector";
            case "GARGANTUAR" -> "Gargantuar";
            case "IMP" -> "Imp";
            default -> "Default";
        };
    }

    private static String normalizeZombie(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "CONEHEAD", "CONE_HEAD" -> "CONE_HEAD";
            case "BUCKETHEAD", "BUCKET_HEAD" -> "BUCKET_HEAD";
            default -> normalized;
        };
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static boolean validRow(int row) {
        return row >= 0 && row < ROWS;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String requireName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
