package network.game;

import models.minigame.IZombieGame;
import network.protocol.EntityState;
import network.protocol.GameRole;
import network.protocol.GameSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Small, deterministic, server-authoritative simulation for the network I, Zombie mode.
 *
 * The network clients never mutate this state directly. They submit PLACE_PLANT or
 * SPAWN_ZOMBIE commands and receive immutable GameSnapshot projections. This class is
 * intentionally independent from LibGDX so the same rules can run on the headless server
 * and in the optional Couch Play screen.
 */
public final class AuthoritativeIZombieGame {
    public static final int ROWS = 5;
    public static final int COLUMNS = 9;
    public static final int LAST_PLANT_COLUMN = IZombieGame.RED_LINE_COLUMN - 1;
    public static final long DEFAULT_MATCH_DURATION_MILLIS = 120_000L;

    private static final double ZOMBIE_SPAWN_X = COLUMNS - 0.35;
    private static final double BRAIN_X = 0.08;
    private static final long PASSIVE_INCOME_INTERVAL_MILLIS = 5_000L;
    private static final int PLANT_PASSIVE_INCOME = 25;
    private static final int ZOMBIE_PASSIVE_INCOME = 0;
    private static final int SUNFLOWER_EAT_REWARD = 200;

    private static final Map<String, Integer> PLANT_COSTS;
    private static final Map<String, Integer> ZOMBIE_COSTS;
    private static final Map<String, PlantDefinition> PLANT_DEFINITIONS;
    private static final Map<String, ZombieDefinition> ZOMBIE_DEFINITIONS;

    static {
        LinkedHashMap<String, Integer> plantCosts = new LinkedHashMap<>();
        plantCosts.put("SUNFLOWER", 50);
        plantCosts.put("PEASHOOTER", 100);
        plantCosts.put("WALL_NUT", 50);
        plantCosts.put("REPEATER", 200);
        plantCosts.put("SNOW_PEA", 175);
        plantCosts.put("TALL_NUT", 125);
        plantCosts.put("THREEPEATER", 325);
        PLANT_COSTS = Collections.unmodifiableMap(plantCosts);

        LinkedHashMap<String, Integer> zombieCosts = new LinkedHashMap<>();
        for (int stage = 1; stage <= 3; stage++) {
            for (IZombieGame.ZombieOptionView option : IZombieGame.zombieOptionsForStage(stage)) {
                zombieCosts.putIfAbsent(normalize(option.zombieName()), option.sunCost());
            }
        }
        ZOMBIE_COSTS = Collections.unmodifiableMap(zombieCosts);

        LinkedHashMap<String, PlantDefinition> plants = new LinkedHashMap<>();
        plants.put("PEASHOOTER", new PlantDefinition(300, 100, 1_200L, 1, false, 0, 0L, 2_000L));
        plants.put("SUNFLOWER", new PlantDefinition(260, 0, 0L, 0, false, 50, 7_000L, 2_500L));
        plants.put("WALL_NUT", new PlantDefinition(1_800, 0, 0L, 0, false, 0, 0L, 6_000L));
        plants.put("SNOW_PEA", new PlantDefinition(300, 90, 1_350L, 1, true, 0, 0L, 4_000L));
        plants.put("REPEATER", new PlantDefinition(350, 85, 950L, 2, false, 0, 0L, 3_500L));
        plants.put("TALL_NUT", new PlantDefinition(3_200, 0, 0L, 0, false, 0, 0L, 7_000L));
        plants.put("THREEPEATER", new PlantDefinition(300, 80, 1_200L, 3, false, 0, 0L, 4_500L));
        PLANT_DEFINITIONS = Collections.unmodifiableMap(plants);

        LinkedHashMap<String, ZombieDefinition> zombies = new LinkedHashMap<>();
        zombies.put("DEFAULT", new ZombieDefinition(300, 0.48, 80, 900L, 2_000L));
        zombies.put("CONE_HEAD", new ZombieDefinition(620, 0.45, 82, 900L, 2_400L));
        zombies.put("IMP", new ZombieDefinition(190, 0.64, 55, 650L, 1_500L));
        zombies.put("BUCKET_HEAD", new ZombieDefinition(1_050, 0.38, 90, 850L, 3_200L));
        zombies.put("RA", new ZombieDefinition(430, 0.44, 84, 900L, 2_700L));
        zombies.put("EXPLORER", new ZombieDefinition(520, 0.56, 100, 820L, 3_000L));
        zombies.put("ALLSTAR", new ZombieDefinition(850, 0.78, 120, 700L, 4_000L));
        zombies.put("WIZARD", new ZombieDefinition(620, 0.40, 96, 860L, 3_800L));
        zombies.put("PROSPECTOR", new ZombieDefinition(560, 0.66, 92, 780L, 2_800L));
        zombies.put("GARGANTUAR", new ZombieDefinition(3_000, 0.24, 240, 950L, 6_500L));
        ZOMBIE_DEFINITIONS = Collections.unmodifiableMap(zombies);
    }

    private final String plantsUsername;
    private final String zombiesUsername;
    private final int stage;
    private final long matchDurationMillis;
    private final boolean[] brains = {true, true, true, true, true};
    private final Map<Integer, PlantUnit> plantsByCell = new LinkedHashMap<>();
    private final List<ZombieUnit> zombies = new ArrayList<>();
    private final List<ProjectileUnit> projectiles = new ArrayList<>();
    private final Map<String, Long> plantReadyAt = new LinkedHashMap<>();
    private final Map<String, Long> zombieReadyAt = new LinkedHashMap<>();

    private long elapsedMillis;
    private long lastPassiveIncomeAt;
    private long sequence = 1L;
    private int plantSun;
    private int zombieSun;
    private GameRole winner;
    private String finishReason = "";

    public AuthoritativeIZombieGame(String plantsUsername, String zombiesUsername) {
        this(plantsUsername, zombiesUsername, 1, DEFAULT_MATCH_DURATION_MILLIS);
    }

    public AuthoritativeIZombieGame(String plantsUsername, String zombiesUsername, int stage) {
        this(plantsUsername, zombiesUsername, stage, DEFAULT_MATCH_DURATION_MILLIS);
    }

    /** Public primarily so deterministic tests and Couch Play can use shorter custom rounds. */
    public AuthoritativeIZombieGame(String plantsUsername, String zombiesUsername, int stage, long durationMillis) {
        this.plantsUsername = requireName(plantsUsername, "plants username");
        this.zombiesUsername = requireName(zombiesUsername, "zombies username");
        if (this.plantsUsername.equalsIgnoreCase(this.zombiesUsername)) {
            throw new IllegalArgumentException("players must be different users");
        }
        this.stage = Math.max(1, Math.min(3, stage));
        this.matchDurationMillis = Math.max(1_000L, durationMillis);
        this.plantSun = IZombieGame.INITIAL_SUN;
        this.zombieSun = IZombieGame.INITIAL_SUN;
        seedOriginalStageDefense();
    }

    public static Map<String, Integer> plantCosts() { return PLANT_COSTS; }
    public static Map<String, Integer> zombieCosts() { return ZOMBIE_COSTS; }

    public static Map<String, Integer> plantCostsForStage(int requestedStage) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (String[] row : IZombieGame.plantLayoutForStage(requestedStage)) {
            for (String plantName : row) {
                String key = normalize(plantName);
                Integer cost = PLANT_COSTS.get(key);
                if (cost != null) result.putIfAbsent(key, cost);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Integer> zombieCostsForStage(int requestedStage) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (IZombieGame.ZombieOptionView option : IZombieGame.zombieOptionsForStage(requestedStage)) {
            result.put(normalize(option.zombieName()), option.sunCost());
        }
        return Collections.unmodifiableMap(result);
    }

    public synchronized ActionResult placePlant(String actorUsername, String type, int row, int column) {
        if (isFinished()) return ActionResult.failure("match is already finished", snapshot());
        if (!isPlantsPlayer(actorUsername)) return ActionResult.failure("only the plant player can place plants", snapshot());
        String normalized = normalize(type);
        Integer cost = PLANT_COSTS.get(normalized);
        PlantDefinition definition = PLANT_DEFINITIONS.get(normalized);
        if (cost == null || definition == null) return ActionResult.failure("unknown plant type: " + type, snapshot());
        if (!validRow(row)) return ActionResult.failure("row must be between 0 and " + (ROWS - 1), snapshot());
        if (column < 0 || column > LAST_PLANT_COLUMN) {
            return ActionResult.failure("plants may only be placed in columns 0-" + LAST_PLANT_COLUMN, snapshot());
        }
        int key = cellKey(row, column);
        if (plantsByCell.containsKey(key)) return ActionResult.failure("that tile already contains a plant", snapshot());
        if (plantSun < cost) return ActionResult.failure("not enough plant sun", snapshot());
        long readyAt = plantReadyAt.getOrDefault(normalized, 0L);
        if (elapsedMillis < readyAt) {
            return ActionResult.failure("plant is cooling down for " + (readyAt - elapsedMillis) + " ms", snapshot());
        }

        PlantUnit unit = new PlantUnit(id("p"), normalized, row, column, definition);
        plantsByCell.put(key, unit);
        plantSun -= cost;
        plantReadyAt.put(normalized, elapsedMillis + definition.placementCooldownMillis);
        sequence++;
        return ActionResult.success(normalized + " planted at row " + row + ", column " + column, snapshot());
    }

    public synchronized ActionResult spawnZombie(String actorUsername, String type, int row) {
        if (isFinished()) return ActionResult.failure("match is already finished", snapshot());
        if (!isZombiesPlayer(actorUsername)) return ActionResult.failure("only the zombie player can release zombies", snapshot());
        String normalized = normalize(type);
        Integer cost = ZOMBIE_COSTS.get(normalized);
        ZombieDefinition definition = ZOMBIE_DEFINITIONS.get(normalized);
        if (cost == null || definition == null) return ActionResult.failure("unknown zombie type: " + type, snapshot());
        if (!validRow(row)) return ActionResult.failure("row must be between 0 and " + (ROWS - 1), snapshot());
        if (zombieSun < cost) return ActionResult.failure("not enough zombie sun", snapshot());
        long readyAt = zombieReadyAt.getOrDefault(normalized, 0L);
        if (elapsedMillis < readyAt) {
            return ActionResult.failure("zombie is cooling down for " + (readyAt - elapsedMillis) + " ms", snapshot());
        }

        ZombieUnit unit = new ZombieUnit(id("z"), normalized, row, ZOMBIE_SPAWN_X, definition);
        zombies.add(unit);
        zombieSun -= cost;
        zombieReadyAt.put(normalized, elapsedMillis + definition.spawnCooldownMillis);
        sequence++;
        return ActionResult.success(normalized + " released in row " + row, snapshot());
    }

    /** Advances authoritative time and all combat. Safe to call from one server ticker. */
    public synchronized void advance(long deltaMillis) {
        if (isFinished() || deltaMillis <= 0L) return;
        long remainingBefore = getRemainingMillis();
        long delta = Math.min(deltaMillis, remainingBefore);
        if (delta <= 0L) {
            finish(GameRole.PLANTS, "Plants survived until the timer expired");
            return;
        }

        elapsedMillis += delta;
        producePassiveIncome();
        produceSunflowers();
        plantsAttack();
        advanceProjectiles(delta);
        advanceZombies(delta);
        removeDeadEntities();

        if (getBrainsRemaining() == 0) {
            finish(GameRole.ZOMBIES, "Zombies ate every brain");
        } else if (elapsedMillis >= matchDurationMillis) {
            finish(GameRole.PLANTS, "Plants survived for the full match time");
        } else {
            sequence++;
        }
    }

    public synchronized void forceFinish(GameRole forcedWinner, String reason) {
        if (isFinished()) return;
        if (forcedWinner == null) throw new IllegalArgumentException("winner is required");
        finish(forcedWinner, reason == null || reason.isBlank() ? "Match ended" : reason);
    }

    public synchronized GameSnapshot snapshot() {
        List<EntityState> entities = new ArrayList<>(plantsByCell.size() + zombies.size() + projectiles.size());
        for (PlantUnit plant : plantsByCell.values()) {
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("column", String.valueOf(plant.column));
            attrs.put("side", GameRole.PLANTS.name());
            if (plant.definition.sunAmount > 0) attrs.put("producer", "true");
            entities.add(new EntityState(plant.id, "PLANT", plant.type, plant.row, plant.column + 0.5,
                    plant.health, plant.definition.maxHealth, attrs));
        }
        for (ZombieUnit zombie : zombies) {
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("side", GameRole.ZOMBIES.name());
            if (zombie.slowUntilMillis > elapsedMillis) attrs.put("slowed", "true");
            entities.add(new EntityState(zombie.id, "ZOMBIE", zombie.type, zombie.row, zombie.x,
                    zombie.health, zombie.definition.maxHealth, attrs));
        }
        for (ProjectileUnit projectile : projectiles) {
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("damage", String.valueOf(projectile.damage));
            entities.add(new EntityState(projectile.id, "PROJECTILE", projectile.type, projectile.row,
                    projectile.x, 1, 1, attrs));
        }
        return new GameSnapshot(sequence, isFinished() ? "FINISHED" : "RUNNING", stage, elapsedMillis,
                getRemainingMillis(), plantSun, zombieSun, brains, entities, remainingCooldowns(plantReadyAt),
                remainingCooldowns(zombieReadyAt), winner, finishReason);
    }

    private Map<String, Long> remainingCooldowns(Map<String, Long> readyAtByType) {
        LinkedHashMap<String, Long> remaining = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : readyAtByType.entrySet()) {
            remaining.put(entry.getKey(), Math.max(0L, entry.getValue() - elapsedMillis));
        }
        return remaining;
    }

    public synchronized boolean isFinished() { return winner != null; }
    public synchronized GameRole getWinner() { return winner; }
    public synchronized String getFinishReason() { return finishReason; }
    public synchronized long getRemainingMillis() { return Math.max(0L, matchDurationMillis - elapsedMillis); }
    public synchronized long getElapsedMillis() { return elapsedMillis; }
    public int getStage() { return stage; }
    public String getPlantsUsername() { return plantsUsername; }
    public String getZombiesUsername() { return zombiesUsername; }

    private void producePassiveIncome() {
        while (lastPassiveIncomeAt + PASSIVE_INCOME_INTERVAL_MILLIS <= elapsedMillis) {
            lastPassiveIncomeAt += PASSIVE_INCOME_INTERVAL_MILLIS;
            plantSun += PLANT_PASSIVE_INCOME;
            zombieSun += ZOMBIE_PASSIVE_INCOME;
        }
    }

    private void produceSunflowers() {
        for (PlantUnit plant : plantsByCell.values()) {
            if (plant.health <= 0 || plant.definition.sunAmount <= 0 || plant.definition.sunIntervalMillis <= 0L) continue;
            while (plant.lastSunAt + plant.definition.sunIntervalMillis <= elapsedMillis) {
                plant.lastSunAt += plant.definition.sunIntervalMillis;
                plantSun += plant.definition.sunAmount;
            }
        }
    }

    private void plantsAttack() {
        for (PlantUnit plant : plantsByCell.values()) {
            if (plant.health <= 0 || plant.definition.projectileDamage <= 0 || plant.definition.attackIntervalMillis <= 0L) continue;
            ZombieUnit target = nearestZombieAhead(plant.row, plant.column + 0.5);
            if (target == null || elapsedMillis < plant.nextAttackAt) continue;
            plant.nextAttackAt = elapsedMillis + plant.definition.attackIntervalMillis;
            for (int shot = 0; shot < Math.max(1, plant.definition.projectilesPerAttack); shot++) {
                double offset = shot * 0.08;
                projectiles.add(new ProjectileUnit(id("pr"), plant.definition.snowProjectile ? "SNOW" : "PEA",
                        plant.row, plant.column + 0.72 + offset, plant.definition.projectileDamage,
                        3.6, plant.definition.snowProjectile ? 3_000L : 0L));
            }
        }
    }

    private void advanceProjectiles(long deltaMillis) {
        double seconds = deltaMillis / 1_000.0;
        Iterator<ProjectileUnit> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            ProjectileUnit projectile = iterator.next();
            double oldX = projectile.x;
            projectile.x += projectile.speed * seconds;
            ZombieUnit target = firstZombieCrossed(projectile.row, oldX, projectile.x);
            if (target != null) {
                target.health -= projectile.damage;
                if (projectile.slowMillis > 0L && target.health > 0) {
                    target.slowUntilMillis = Math.max(target.slowUntilMillis, elapsedMillis + projectile.slowMillis);
                }
                iterator.remove();
            } else if (projectile.x > COLUMNS + 0.5) {
                iterator.remove();
            }
        }
    }

    private void advanceZombies(long deltaMillis) {
        double seconds = deltaMillis / 1_000.0;
        for (ZombieUnit zombie : zombies) {
            if (zombie.health <= 0) continue;
            PlantUnit blocker = blockingPlant(zombie);
            if (blocker != null) {
                if (elapsedMillis >= zombie.nextBiteAt) {
                    blocker.health -= zombie.definition.biteDamage;
                    zombie.nextBiteAt = elapsedMillis + zombie.definition.biteIntervalMillis;
                }
                continue;
            }
            double slowFactor = zombie.slowUntilMillis > elapsedMillis ? 0.52 : 1.0;
            zombie.x -= zombie.definition.speedCellsPerSecond * slowFactor * seconds;
            if (zombie.x <= BRAIN_X) {
                if (brains[zombie.row]) brains[zombie.row] = false;
                zombie.health = 0;
            }
        }
    }

    private void removeDeadEntities() {
        Iterator<Map.Entry<Integer, PlantUnit>> plantIterator = plantsByCell.entrySet().iterator();
        while (plantIterator.hasNext()) {
            PlantUnit plant = plantIterator.next().getValue();
            if (plant.health > 0) continue;
            if (plant.definition.sunAmount > 0) {
                zombieSun += SUNFLOWER_EAT_REWARD;
            }
            plantIterator.remove();
        }
        zombies.removeIf(zombie -> zombie.health <= 0);
    }

    private void seedOriginalStageDefense() {
        String[][] layout = IZombieGame.plantLayoutForStage(stage);
        for (int row = 0; row < Math.min(ROWS, layout.length); row++) {
            String[] lane = layout[row];
            for (int column = 0; column < Math.min(IZombieGame.RED_LINE_COLUMN, lane.length); column++) {
                addSeedPlant(normalize(lane[column]), row, column);
            }
        }
    }

    private void addSeedPlant(String type, int row, int column) {
        PlantDefinition definition = PLANT_DEFINITIONS.get(type);
        if (definition == null || !validRow(row) || column < 0 || column > LAST_PLANT_COLUMN) return;
        int key = cellKey(row, column);
        if (plantsByCell.containsKey(key)) return;
        plantsByCell.put(key, new PlantUnit(id("p"), type, row, column, definition));
    }

    private ZombieUnit nearestZombieAhead(int row, double x) {
        ZombieUnit best = null;
        for (ZombieUnit zombie : zombies) {
            if (zombie.health <= 0 || zombie.row != row || zombie.x <= x) continue;
            if (best == null || zombie.x < best.x) best = zombie;
        }
        return best;
    }

    private ZombieUnit firstZombieCrossed(int row, double oldX, double newX) {
        ZombieUnit best = null;
        for (ZombieUnit zombie : zombies) {
            if (zombie.health <= 0 || zombie.row != row) continue;
            if (zombie.x + 0.18 < oldX || zombie.x - 0.18 > newX) continue;
            if (best == null || zombie.x < best.x) best = zombie;
        }
        return best;
    }

    private PlantUnit blockingPlant(ZombieUnit zombie) {
        PlantUnit best = null;
        double bestX = -Double.MAX_VALUE;
        for (PlantUnit plant : plantsByCell.values()) {
            if (plant.health <= 0 || plant.row != zombie.row) continue;
            double px = plant.column + 0.5;
            if (px > zombie.x + 0.28 || zombie.x - px > 0.52) continue;
            if (px > bestX) {
                bestX = px;
                best = plant;
            }
        }
        return best;
    }

    private void finish(GameRole decidedWinner, String reason) {
        if (winner != null) return;
        winner = Objects.requireNonNull(decidedWinner, "winner");
        finishReason = reason == null ? "" : reason;
        sequence++;
    }

    private boolean isPlantsPlayer(String username) {
        return username != null && plantsUsername.equalsIgnoreCase(username.trim());
    }

    private boolean isZombiesPlayer(String username) {
        return username != null && zombiesUsername.equalsIgnoreCase(username.trim());
    }

    private int getBrainsRemaining() {
        int count = 0;
        for (boolean brain : brains) if (brain) count++;
        return count;
    }

    private static boolean validRow(int row) { return row >= 0 && row < ROWS; }
    private static int cellKey(int row, int column) { return row * COLUMNS + column; }
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
    private static String id(String prefix) { return prefix + '-' + UUID.randomUUID(); }
    private static String requireName(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value.trim();
    }

    private record PlantDefinition(int maxHealth,
                                   int projectileDamage,
                                   long attackIntervalMillis,
                                   int projectilesPerAttack,
                                   boolean snowProjectile,
                                   int sunAmount,
                                   long sunIntervalMillis,
                                   long placementCooldownMillis) { }

    private record ZombieDefinition(int maxHealth,
                                    double speedCellsPerSecond,
                                    int biteDamage,
                                    long biteIntervalMillis,
                                    long spawnCooldownMillis) { }

    private static final class PlantUnit {
        final String id;
        final String type;
        final int row;
        final int column;
        final PlantDefinition definition;
        int health;
        long nextAttackAt;
        long lastSunAt;

        PlantUnit(String id, String type, int row, int column, PlantDefinition definition) {
            this.id = id;
            this.type = type;
            this.row = row;
            this.column = column;
            this.definition = definition;
            this.health = definition.maxHealth;
        }
    }

    private static final class ZombieUnit {
        final String id;
        final String type;
        final int row;
        final ZombieDefinition definition;
        double x;
        int health;
        long nextBiteAt;
        long slowUntilMillis;

        ZombieUnit(String id, String type, int row, double x, ZombieDefinition definition) {
            this.id = id;
            this.type = type;
            this.row = row;
            this.x = x;
            this.definition = definition;
            this.health = definition.maxHealth;
        }
    }

    private static final class ProjectileUnit {
        final String id;
        final String type;
        final int row;
        double x;
        final int damage;
        final double speed;
        final long slowMillis;

        ProjectileUnit(String id, String type, int row, double x, int damage, double speed, long slowMillis) {
            this.id = id;
            this.type = type;
            this.row = row;
            this.x = x;
            this.damage = damage;
            this.speed = speed;
            this.slowMillis = slowMillis;
        }
    }
}
