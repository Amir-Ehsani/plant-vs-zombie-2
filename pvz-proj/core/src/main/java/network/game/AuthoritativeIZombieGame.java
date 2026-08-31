package network.game;

import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantActionTiming;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
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
    public static final long DEFAULT_MATCH_DURATION_MILLIS = 600_000L;
    public static final int MAX_SELECTED_PLANTS = 8;
    public static final int INITIAL_SUN = 50;

    private static final long CORE_TICK_MILLIS = 100L;
    private static final int TICKS_PER_SECOND = 10;
    private static final int FALLING_SUN_TICKS = 50;
    private static final int GROUND_SUN_LIFETIME_TICKS = 100;
    private static final int NORMAL_SKY_SUN_AMOUNT = 25;
    private static final int BASE_PLANT_SUN_AMOUNT = 50;
    private static final int TWIN_SUNFLOWER_SUN_AMOUNT = 100;

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
    private final Map<String, Integer> nextPlantSunTick;
    private final List<SunDrop> sunDrops;
    private final List<DelayedPlantEffect> delayedPlantEffects;
    private final LinkedHashSet<String> plantLoadout;
    private final Random random;

    private long elapsedMillis;
    private long nextPlantSkySunAt;
    private long nextZombieSkySunAt;
    private long boardTickRemainderMillis;
    private int boardTick;
    private int nextSunDropId;
    private long sequence;
    private int plantSun;
    private int zombieSun;
    private boolean plantsReady;
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
        this.nextPlantSunTick = new HashMap<>();
        this.sunDrops = new ArrayList<>();
        this.delayedPlantEffects = new ArrayList<>();
        this.plantLoadout = new LinkedHashSet<>();
        this.random = new Random(plantsUsername.hashCode() * 31L + zombiesUsername.hashCode());
        this.elapsedMillis = 0L;
        this.nextPlantSkySunAt = -1L;
        this.nextZombieSkySunAt = -1L;
        this.boardTickRemainderMillis = 0L;
        this.boardTick = 0;
        this.nextSunDropId = 1;
        this.sequence = 1L;
        this.plantSun = INITIAL_SUN;
        this.zombieSun = INITIAL_SUN;
        this.plantsReady = false;
        this.winner = null;
        this.finishReason = "";
        configureLevelOneBaseBoard();
    }

    public static Map<String, Integer> plantCosts() {
        return PLANT_COSTS;
    }

    public static int plantSunCost(String type) {
        String key = normalize(type);
        Integer mapped = PLANT_COSTS.get(key);
        if (mapped != null) {
            return mapped;
        }
        PlantType plantType = plantType(key);
        return plantType == null ? Integer.MAX_VALUE : Math.max(0, plantType.getSunCost());
    }

    public static boolean isPlantAvailable(String type) {
        return plantType(normalize(type)) != null;
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

    /** Couch Play skips the selection screen and uses the full Ancient Egypt level-one catalog. */
    public synchronized void useFullCatalogLoadout() {
        if (plantsReady || isFinished()) {
            return;
        }
        plantLoadout.clear();
        plantLoadout.addAll(PLANT_COSTS.keySet());
        plantsReady = true;
        scheduleNextSkySun(GameRole.PLANTS);
        scheduleNextSkySun(GameRole.ZOMBIES);
        sequence++;
    }

    public synchronized ActionResult lockPlants(String actorUsername, List<String> types) {
        if (isFinished()) {
            return ActionResult.failure("match is already finished", snapshot());
        }
        if (!plantsUsername.equalsIgnoreCase(requireName(actorUsername, "actor username"))) {
            return ActionResult.failure("only the plant player can choose plants", snapshot());
        }
        if (plantsReady) {
            return ActionResult.failure("plants are already locked in", snapshot());
        }
        if (types == null || types.isEmpty()) {
            return ActionResult.failure("choose at least one plant", snapshot());
        }
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        for (String type : types) {
            String key = normalize(type);
            if (!isPlantAvailable(key)) {
                return ActionResult.failure("plant type is not available", snapshot());
            }
            selected.add(key);
            if (selected.size() > MAX_SELECTED_PLANTS) {
                return ActionResult.failure("select at most " + MAX_SELECTED_PLANTS + " plants", snapshot());
            }
        }
        plantLoadout.clear();
        plantLoadout.addAll(selected);
        plantsReady = true;
        scheduleNextSkySun(GameRole.PLANTS);
        scheduleNextSkySun(GameRole.ZOMBIES);
        sequence++;
        return ActionResult.success("Plants locked in. Let's rock!", snapshot());
    }

    public synchronized ActionResult collectSun(String actorUsername, int dropId) {
        if (isFinished()) {
            return ActionResult.failure("match is already finished", snapshot());
        }
        if (!plantsReady) {
            return ActionResult.failure("waiting for the plant player to choose plants", snapshot());
        }
        String actor = requireName(actorUsername, "actor username");
        GameRole role;
        if (plantsUsername.equalsIgnoreCase(actor)) {
            role = GameRole.PLANTS;
        } else if (zombiesUsername.equalsIgnoreCase(actor)) {
            role = GameRole.ZOMBIES;
        } else {
            return ActionResult.failure("you are not in this match", snapshot());
        }
        SunDrop found = null;
        for (SunDrop drop : sunDrops) {
            if (drop.id == dropId) {
                found = drop;
                break;
            }
        }
        if (found == null) {
            return ActionResult.failure("that sun has already disappeared", snapshot());
        }
        if (found.owner != role) {
            return ActionResult.failure("that sun belongs to the other player", snapshot());
        }
        if (role == GameRole.PLANTS) {
            plantSun += found.amount;
        } else {
            zombieSun += found.amount;
        }
        sunDrops.remove(found);
        sequence++;
        return ActionResult.success("Collected " + found.amount + " sun", snapshot());
    }

    public synchronized ActionResult placePlant(String actorUsername, String type, int row, int column) {
        if (isFinished()) {
            return ActionResult.failure("match is already finished", snapshot());
        }
        if (!plantsUsername.equalsIgnoreCase(requireName(actorUsername, "actor username"))) {
            return ActionResult.failure("only the plant player can place plants", snapshot());
        }
        if (!plantsReady) {
            return ActionResult.failure("choose your plants first", snapshot());
        }
        if (!validRow(row) || column < 0 || column > LAST_PLANT_COLUMN) {
            return ActionResult.failure("plant position is outside the plant side", snapshot());
        }

        String key = normalize(type);
        if (!plantLoadout.contains(key)) {
            return ActionResult.failure("that plant was not selected for this match", snapshot());
        }
        int cost = plantSunCost(key);
        PlantType plantType = plantType(key);
        if (cost == Integer.MAX_VALUE || plantType == null) {
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
        schedulePlantSunIfProducer(plant);
        scheduleImmediatePlant(plant, position);
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
        if (!plantsReady) {
            return ActionResult.failure("waiting for the plant player to choose plants", snapshot());
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
        if (!plantsReady) {
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
        produceSkySun();

        while (boardTickRemainderMillis >= CORE_TICK_MILLIS && !isFinished()) {
            boardTickRemainderMillis -= CORE_TICK_MILLIS;
            boardTick++;
            producePlantSun();
            tickSunDrops();
            board.updateTicks();
            tickDelayedPlantEffects();
            pruneDeadProducerSchedules();
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
            attributes.put("attackSerial", String.valueOf(plant.getVisualAttackSerial()));
            attributes.put("attackClip", plant.getVisualAttackClip() == null ? "" : plant.getVisualAttackClip());
            attributes.put("specialSerial", String.valueOf(plant.getVisualSpecialSerial()));
            attributes.put("specialClip", plant.getVisualSpecialClip() == null ? "" : plant.getVisualSpecialClip());
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
            attributes.put("eating", String.valueOf(isZombieEatingPlant(zombie)));
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
        for (SunDrop drop : sunDrops) {
            Map<String, String> attributes = new LinkedHashMap<>();
            attributes.put("owner", drop.owner.name());
            attributes.put("dropId", String.valueOf(drop.id));
            attributes.put("falling", String.valueOf(drop.falling));
            attributes.put("fromPlant", String.valueOf(drop.fromPlant));
            entities.add(new EntityState(
                    "sun-" + drop.id,
                    "SUN",
                    drop.fromPlant ? "PLANT" : "SKY",
                    clamp((int) Math.round(drop.y) - 1, 0, ROWS - 1),
                    drop.x - 1.0,
                    drop.amount,
                    drop.amount,
                    attributes
            ));
        }

        return new GameSnapshot(
                sequence,
                isFinished() ? "FINISHED" : (plantsReady ? "RUNNING" : "WAITING"),
                1,
                elapsedMillis,
                plantsReady ? getRemainingMillis() : matchDurationMillis,
                plantSun,
                zombieSun,
                brainState(),
                entities,
                remainingCooldowns(plantReadyAt),
                remainingCooldowns(zombieReadyAt),
                winner,
                finishReason,
                plantsReady,
                plantLoadoutDisplayNames()
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

    public synchronized Board getBoard() {
        return board;
    }

    public synchronized int getPlantSun() {
        return plantSun;
    }

    public synchronized int getZombieSun() {
        return zombieSun;
    }

    public synchronized boolean isPlantsReady() {
        return plantsReady;
    }

    public synchronized long plantCooldownMillis(String type) {
        return Math.max(0L, plantReadyAt.getOrDefault(normalize(type), 0L) - elapsedMillis);
    }

    public synchronized long zombieCooldownMillis(String type) {
        return Math.max(0L, zombieReadyAt.getOrDefault(normalize(type), 0L) - elapsedMillis);
    }

    private void configureLevelOneBaseBoard() {
        board.setGraveSpawningAllowed(false);
        for (Lane lane : board.getLanes()) {
            lane.getLawnMower().disable();
            lane.setContinueAfterBrainEaten(true);
        }
        // Deliberately do not pre-place plants or zombies. The online match starts empty.
    }

    private void produceSkySun() {
        spawnSkySunIfDue(GameRole.PLANTS);
        spawnSkySunIfDue(GameRole.ZOMBIES);
    }

    private void spawnSkySunIfDue(GameRole owner) {
        long nextAt = owner == GameRole.PLANTS ? nextPlantSkySunAt : nextZombieSkySunAt;
        if (nextAt < 0L || elapsedMillis < nextAt) {
            return;
        }
        Position position = randomSkySunPosition(owner);
        if (position != null && !hasSunAt(position.getX(), position.getY())) {
            sunDrops.add(new SunDrop(
                    nextSunDropId++,
                    owner,
                    false,
                    position.getX(),
                    position.getY(),
                    NORMAL_SKY_SUN_AMOUNT,
                    FALLING_SUN_TICKS + GROUND_SUN_LIFETIME_TICKS,
                    true,
                    FALLING_SUN_TICKS
            ));
        }
        scheduleNextSkySun(owner);
    }

    private void scheduleNextSkySun(GameRole owner) {
        double elapsedSeconds = elapsedMillis / 1000.0;
        // Same cadence as Ancient Egypt sky sun: one drop, roughly every 12s.
        double intervalSeconds = Math.max(6.0 + 0.05 * elapsedSeconds, 12.0);
        long interval = Math.max(1_000L, (long) Math.ceil(intervalSeconds * 1000.0));
        long next = elapsedMillis + interval;
        if (owner == GameRole.PLANTS) {
            nextPlantSkySunAt = next;
        } else {
            nextZombieSkySunAt = next;
        }
    }

    private Position randomSkySunPosition(GameRole owner) {
        int minColumn = owner == GameRole.PLANTS ? 1 : LAST_PLANT_COLUMN + 2;
        int maxColumn = owner == GameRole.PLANTS ? LAST_PLANT_COLUMN + 1 : COLUMNS;
        Position fallback = new Position(
                minColumn + random.nextInt(Math.max(1, maxColumn - minColumn + 1)),
                1 + random.nextInt(ROWS)
        );
        for (int attempt = 0; attempt < ROWS * COLUMNS; attempt++) {
            int column = minColumn + random.nextInt(Math.max(1, maxColumn - minColumn + 1));
            int row = 1 + random.nextInt(ROWS);
            if (!hasSunAt(column, row)) {
                return new Position(column, row);
            }
        }
        return fallback;
    }

    private void producePlantSun() {
        for (Plant plant : board.getAllPlants()) {
            if (!isSunProducer(plant)) {
                continue;
            }
            nextPlantSunTick.putIfAbsent(plant.getId(), boardTick + Math.max(1, plant.getProductionTimeTicks()));
            int nextTick = nextPlantSunTick.getOrDefault(plant.getId(), Integer.MAX_VALUE);
            if (boardTick < nextTick) {
                continue;
            }
            int column = clamp((int) Math.round(plant.getX()), 1, COLUMNS);
            int row = clamp((int) Math.round(plant.getY()), 1, ROWS);
            if (hasSunAt(column, row)) {
                nextPlantSunTick.put(plant.getId(), boardTick + TICKS_PER_SECOND);
                continue;
            }
            plant.triggerSpecialAnimation("special");
            sunDrops.add(new SunDrop(
                    nextSunDropId++,
                    GameRole.PLANTS,
                    true,
                    column,
                    row,
                    plantSunAmount(plant),
                    GROUND_SUN_LIFETIME_TICKS,
                    false,
                    0
            ));
            nextPlantSunTick.put(plant.getId(), boardTick + Math.max(1, plant.getProductionTimeTicks()));
        }
    }

    private void schedulePlantSunIfProducer(Plant plant) {
        if (isSunProducer(plant)) {
            nextPlantSunTick.put(plant.getId(), boardTick + Math.max(1, plant.getProductionTimeTicks()));
        }
    }

    private void pruneDeadProducerSchedules() {
        Set<String> living = new LinkedHashSet<>();
        for (Plant plant : board.getAllPlants()) {
            living.add(plant.getId());
        }
        nextPlantSunTick.keySet().removeIf(id -> !living.contains(id));
    }

    private void tickSunDrops() {
        Iterator<SunDrop> iterator = sunDrops.iterator();
        while (iterator.hasNext()) {
            SunDrop drop = iterator.next();
            drop.remainingTicks--;
            if (drop.falling) {
                drop.fallingTicksRemaining--;
                if (drop.fallingTicksRemaining <= 0) {
                    drop.falling = false;
                }
            }
            if (drop.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    private boolean hasSunAt(double x, double y) {
        int column = (int) Math.round(x);
        int row = (int) Math.round(y);
        for (SunDrop drop : sunDrops) {
            if ((int) Math.round(drop.x) == column && (int) Math.round(drop.y) == row) {
                return true;
            }
        }
        return false;
    }

    private List<String> plantLoadoutDisplayNames() {
        List<String> names = new ArrayList<>();
        for (String key : plantLoadout) {
            PlantType type = plantType(key);
            names.add(type == null ? key : type.getName());
        }
        return names;
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
                "Potato Mine",
                "Cabbage-pult",
                "Kernel-pult",
                "Iceberg Lettuce",
                "Bonk Choy",
                "Cherry Bomb",
                "Grave Buster"
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

    private static boolean isSunProducer(Plant plant) {
        if (plant == null || plant.getType() == null || plant.getType().getCategory() == null) {
            return false;
        }
        return plant.getType().getCategory().trim().equalsIgnoreCase("sun producer");
    }

    private static int plantSunAmount(Plant plant) {
        if (plant != null && normalize(plant.getName()).equals("TWIN_SUNFLOWER")) {
            return TWIN_SUNFLOWER_SUN_AMOUNT;
        }
        return BASE_PLANT_SUN_AMOUNT;
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

    private boolean isZombieEatingPlant(Zombie zombie) {
        if (zombie == null || !zombie.isAlive()) {
            return false;
        }
        int column = clamp((int) Math.round(zombie.getX()), 1, COLUMNS);
        int row = clamp((int) Math.round(zombie.getY()), 1, ROWS);
        Tile tile = board.getTileAt(new Position(column, row));
        Plant plant = tile == null ? null : tile.getCurrentPlant();
        return plant != null && plant.isAlive();
    }

    private static String requireName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private void scheduleImmediatePlant(Plant plant, Position position) {
        String name = plantNameKey(plant);
        if ("cherry bomb".equals(name)) {
            plant.prepareAttackAnimation("attack");
            plant.triggerSpecialAnimation("attack");
            delayedPlantEffects.add(new DelayedPlantEffect(
                    Math.max(1, PlantActionTiming.specialImpactTicks("cherry bomb")),
                    plant,
                    position,
                    "cherry bomb"
            ));
            return;
        }
        if ("grave buster".equals(name)) {
            plant.prepareAttackAnimation("attack");
            plant.triggerSpecialAnimation("attack");
            delayedPlantEffects.add(new DelayedPlantEffect(
                    Math.max(1, PlantActionTiming.specialImpactTicks("grave buster")),
                    plant,
                    position,
                    "grave buster"
            ));
        }
    }

    private void tickDelayedPlantEffects() {
        Iterator<DelayedPlantEffect> iterator = delayedPlantEffects.iterator();
        while (iterator.hasNext()) {
            DelayedPlantEffect effect = iterator.next();
            effect.remainingTicks--;
            if (effect.remainingTicks > 0) {
                continue;
            }
            iterator.remove();
            resolveImmediatePlant(effect);
        }
    }

    private void resolveImmediatePlant(DelayedPlantEffect effect) {
        if (effect.plant == null || !effect.plant.isAlive()) {
            return;
        }
        if ("cherry bomb".equals(effect.kind)) {
            String category = effect.plant.getType() == null || effect.plant.getType().getCategory() == null
                    ? ""
                    : effect.plant.getType().getCategory();
            board.damageZombiesInArea(
                    effect.position,
                    1,
                    1,
                    Math.max(1800, effect.plant.getAttackDamage()),
                    "cherry bomb",
                    effect.plant.getName(),
                    category
            );
            removePlantFromBoard(effect.plant, effect.position);
            return;
        }
        if ("grave buster".equals(effect.kind)) {
            board.removeTerrain(effect.position, TileType.GRAVE);
            removePlantFromBoard(effect.plant, effect.position);
        }
    }

    private void removePlantFromBoard(Plant plant, Position position) {
        Tile tile = board.getTileAt(position);
        if (tile != null) {
            tile.removePlant(plant);
        }
    }

    private static String plantNameKey(Plant plant) {
        return plant == null || plant.getName() == null
                ? ""
                : plant.getName().trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }

    private static final class DelayedPlantEffect {
        private int remainingTicks;
        private final Plant plant;
        private final Position position;
        private final String kind;

        private DelayedPlantEffect(int remainingTicks, Plant plant, Position position, String kind) {
            this.remainingTicks = remainingTicks;
            this.plant = plant;
            this.position = position;
            this.kind = kind;
        }
    }

    private static final class SunDrop {
        private final int id;
        private final GameRole owner;
        private final boolean fromPlant;
        private final double x;
        private final double y;
        private final int amount;
        private int remainingTicks;
        private boolean falling;
        private int fallingTicksRemaining;

        private SunDrop(
                int id,
                GameRole owner,
                boolean fromPlant,
                double x,
                double y,
                int amount,
                int remainingTicks,
                boolean falling,
                int fallingTicksRemaining
        ) {
            this.id = id;
            this.owner = owner;
            this.fromPlant = fromPlant;
            this.x = x;
            this.y = y;
            this.amount = amount;
            this.remainingTicks = remainingTicks;
            this.falling = falling;
            this.fallingTicksRemaining = fallingTicksRemaining;
        }
    }
}
