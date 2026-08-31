package models.minigame;

import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.board.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class WallNutBowlingGame extends MiniGameSession {
    private enum NutType {
        BOWLING("bowling", 550),
        EXPLOSIVE("explosive", 1800),
        GIANT("giant", Integer.MAX_VALUE);

        private final String commandName;
        private final int damage;

        NutType(String commandName, int damage) {
            this.commandName = commandName;
            this.damage = damage;
        }

        private static NutType fromText(String value) {
            String normalized = normalize(value);
            if (normalized.equals("bowling") || normalized.equals("wallnut") || normalized.equals("bowling wallnut")) {
                return BOWLING;
            }
            if (normalized.equals("explosive")
                    || normalized.equals("explode o nut")
                    || normalized.equals("exploding")) {
                return EXPLOSIVE;
            }
            if (normalized.equals("giant") || normalized.equals("big")) {
                return GIANT;
            }
            return null;
        }
    }

    private static final class BowlingNut {
        private final int id;
        private final NutType type;
        private final Set<Zombie> hitZombies;
        private double x;
        private double y;
        private double dx;
        private double dy;
        private int collisionCount;
        private boolean active;

        private BowlingNut(int id, NutType type, double x, double y) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.dx = 0.34;
            this.dy = 0;
            this.collisionCount = 0;
            this.active = true;
            this.hitZombies = Collections.newSetFromMap(new IdentityHashMap<>());
        }
    }

    private static final class BowlingExplosion {
        private static final int LIFETIME_TICKS = 12;
        private final int id;
        private final double x;
        private final double y;
        private int ageTicks;

        private BowlingExplosion(int id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.ageTicks = 0;
        }
    }

    private static final class SpawnEntry {
        private final int tick;
        private final int lane;
        private final String zombieName;

        private SpawnEntry(int tick, int lane, String zombieName) {
            this.tick = tick;
            this.lane = lane;
            this.zombieName = zombieName;
        }
    }

    private final Board board;
    private final ZombieFactory zombieFactory;
    private final Random random;
    private final List<BowlingNut> activeNuts;
    private final List<SpawnEntry> spawnSchedule;
    private final List<BowlingExplosion> explosions;
    private final Map<NutType, Integer> inventory;
    private final int redLineColumn;
    private int nextSpawnIndex;
    private int nextNutId;
    private int nextExplosionId;
    private int launchedNuts;
    private int crushedZombies;

    public WallNutBowlingGame(int stage) {
        super(MiniGameType.WALLNUT_BOWLING, stage);
        board = new Board();
        zombieFactory = new ZombieFactory();
        random = new Random(9_200L + stage);
        activeNuts = new ArrayList<>();
        spawnSchedule = new ArrayList<>();
        explosions = new ArrayList<>();
        inventory = new LinkedHashMap<>();
        redLineColumn = 3;
        nextSpawnIndex = 0;
        nextNutId = 1;
        nextExplosionId = 1;
        launchedNuts = 0;
        crushedZombies = 0;
        initializeInventory();
        initializeSchedule();
        success("Wall-nut Bowling stage " + stage + " started.");
    }

    public boolean launchNut(String typeName, Position position) {
        if (!isRunning()) {
            fail("The mini-game is already finished.");
            return false;
        }
        NutType type = NutType.fromText(typeName);
        if (type == null) {
            fail("Unknown nut type. Use bowling, explosive, or giant.");
            return false;
        }
        if (position == null || !board.isValidPosition(position)) {
            fail("Launch position is invalid.");
            return false;
        }
        if (position.getX() > redLineColumn) {
            fail("Nuts can only be launched at or left of column " + redLineColumn + ".");
            return false;
        }
        int remaining = inventory.getOrDefault(type, 0);
        if (remaining <= 0) {
            fail("No " + type.commandName + " nuts remain.");
            return false;
        }

        inventory.put(type, remaining - 1);
        activeNuts.add(new BowlingNut(nextNutId++, type, position.getX(), position.getY()));
        launchedNuts++;
        success(type.commandName + " nut launched from " + position + ".");
        return true;
    }

    public String renderInventory() {
        StringBuilder builder = new StringBuilder("Nut inventory:\n");
        for (NutType type : NutType.values()) {
            builder.append(type.commandName)
                    .append(" = ")
                    .append(inventory.getOrDefault(type, 0))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    @Override
    protected void onTick() {
        spawnReadyZombies();
        moveNuts();
        handleNutCollisions();
        updateExplosions();
        board.removeDeadEntities();
        board.updateTicks();
    }

    @Override
    protected void evaluateStatus() {
        if (!isRunning()) {
            return;
        }
        if (board.hasBrainBeenEaten()) {
            markLost("A zombie reached the house. Wall-nut Bowling was lost.");
            return;
        }
        boolean allSpawned = nextSpawnIndex >= spawnSchedule.size();
        if (allSpawned && board.getActiveZombieCount() == 0) {
            markWon("All bowling waves were defeated.");
            return;
        }
        if (activeNuts.isEmpty() && totalInventory() == 0 && (board.getActiveZombieCount() > 0 || !allSpawned)) {
            markLost("No bowling nuts remain while zombies are still coming.");
        }
    }

    @Override
    public String renderMap() {
        Map<Position, String> overlays = new LinkedHashMap<>();
        for (BowlingNut nut : activeNuts) {
            int x = clamp((int) Math.round(nut.x), 1, board.getWidth());
            int y = clamp((int) Math.round(nut.y), 1, board.getHeight());
            String symbol = switch (nut.type) {
                case BOWLING -> "NB";
                case EXPLOSIVE -> "NE";
                case GIANT -> "NG";
            };
            overlays.put(new Position(x, y), symbol);
        }
        return "red line: column " + redLineColumn + '\n' + renderBoard(board, overlays);
    }

    @Override
    public String renderStatus() {
        return compactStatus()
                + "\nactive zombies=" + board.getActiveZombieCount()
                + "\nremaining scheduled zombies=" + (spawnSchedule.size() - nextSpawnIndex)
                + "\nactive nuts=" + activeNuts.size()
                + "\nremaining nuts=" + totalInventory()
                + "\nlaunched nuts=" + launchedNuts
                + "\nzombies hit or crushed=" + crushedZombies;
    }

    @Override
    public String renderHelp() {
        return """
                Wall-nut Bowling commands
                show nuts
                launch nut -t <bowling|explosive|giant> -l <x, y>
                show map
                show status
                advance time -t <count> ticks
                """.trim();
    }

    public Board getBoard() {
        return board;
    }

    public Map<String, Integer> getInventory() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (NutType type : NutType.values()) {
            result.put(type.commandName, inventory.getOrDefault(type, 0));
        }
        return Collections.unmodifiableMap(result);
    }

    public List<BowlingNutView> getActiveNuts() {
        List<BowlingNutView> result = new ArrayList<>();
        for (BowlingNut nut : activeNuts) {
            result.add(new BowlingNutView(nut.id, nut.type.commandName, nut.x, nut.y));
        }
        return Collections.unmodifiableList(result);
    }

    public int getRedLineColumn() {
        return redLineColumn;
    }

    public record BowlingNutView(int id, String type, double x, double y) {
    }

    public List<BowlingExplosionView> getExplosions() {
        List<BowlingExplosionView> result = new ArrayList<>();
        for (BowlingExplosion explosion : explosions) {
            result.add(new BowlingExplosionView(
                    explosion.id,
                    explosion.x,
                    explosion.y,
                    explosion.ageTicks,
                    BowlingExplosion.LIFETIME_TICKS
            ));
        }
        return Collections.unmodifiableList(result);
    }

    public record BowlingExplosionView(int id, double x, double y, int ageTicks, int lifetimeTicks) {
    }

    private void moveNuts() {
        Iterator<BowlingNut> iterator = activeNuts.iterator();
        while (iterator.hasNext()) {
            BowlingNut nut = iterator.next();
            nut.x += nut.dx;
            nut.y += nut.dy;

            if (nut.y < 1) {
                nut.y = 1 + (1 - nut.y);
                nut.dy = Math.abs(nut.dy);
            } else if (nut.y > board.getHeight()) {
                nut.y = board.getHeight() - (nut.y - board.getHeight());
                nut.dy = -Math.abs(nut.dy);
            }

            if (!nut.active || nut.x > board.getWidth() + 0.80) {
                iterator.remove();
            }
        }
    }

    private void handleNutCollisions() {
        for (BowlingNut nut : new ArrayList<>(activeNuts)) {
            if (!nut.active) {
                continue;
            }
            for (Zombie zombie : new ArrayList<>(board.getAllZombies())) {
                if (!zombie.isAlive() || nut.hitZombies.contains(zombie)) {
                    continue;
                }
                if (Math.abs(nut.x - zombie.getX()) > 0.48 || Math.abs(nut.y - zombie.getY()) > 0.48) {
                    continue;
                }
                nut.hitZombies.add(zombie);
                collide(nut, zombie);
                if (!nut.active) {
                    break;
                }
            }
        }
        activeNuts.removeIf(nut -> !nut.active);
    }

    private void collide(BowlingNut nut, Zombie zombie) {
        if (nut.type == NutType.GIANT) {
            if (zombie.isAlive()) {
                zombie.kill();
                crushedZombies++;
            }
            return;
        }
        if (nut.type == NutType.EXPLOSIVE) {
            explosions.add(new BowlingExplosion(nextExplosionId++, nut.x, nut.y));
            explodeAt(nut.x, nut.y);
            nut.active = false;
            return;
        }

        zombie.takeDamage(new Damage(nut.type.damage, "bowling wall-nut"));
        crushedZombies++;
        nut.collisionCount++;
        if (nut.collisionCount == 1) {
            nut.dy = random.nextBoolean() ? nut.dx : -nut.dx;
        } else {
            nut.dy = nut.dy == 0 ? nut.dx : -nut.dy;
        }
    }

    private void updateExplosions() {
        for (BowlingExplosion explosion : explosions) {
            explosion.ageTicks++;
        }
        explosions.removeIf(explosion -> explosion.ageTicks >= BowlingExplosion.LIFETIME_TICKS);
    }

    private void explodeAt(double centerX, double centerY) {
        int hits = 0;
        for (Zombie zombie : new ArrayList<>(board.getAllZombies())) {
            if (Math.abs(zombie.getX() - centerX) <= 1.0 && Math.abs(zombie.getY() - centerY) <= 1.0) {
                zombie.takeDamage(new Damage(NutType.EXPLOSIVE.damage, "exploding wall-nut"));
                hits++;
            }
        }
        crushedZombies += hits;
    }

    private void spawnReadyZombies() {
        while (nextSpawnIndex < spawnSchedule.size() && spawnSchedule.get(nextSpawnIndex).tick <= getCurrentTick()) {
            SpawnEntry entry = spawnSchedule.get(nextSpawnIndex++);
            Zombie zombie = zombieFactory.createZombie(entry.zombieName, board.getWidth() + 4.0, entry.lane);
            Tile tile = board.getTileAt(new Position(board.getWidth(), entry.lane));
            tile.addZombie(zombie);
        }
    }

    private void initializeInventory() {
        // Keep the total stock close to the original stages while matching the
        // conveyor odds: normal 1.0x, Explode-o-nut 0.8x, giant/tall 0.6x.
        if (getStage() == 1) {
            inventory.put(NutType.BOWLING, 10);
            inventory.put(NutType.EXPLOSIVE, 8);
            inventory.put(NutType.GIANT, 6);
            return;
        }
        if (getStage() == 2) {
            inventory.put(NutType.BOWLING, 13);
            inventory.put(NutType.EXPLOSIVE, 10);
            inventory.put(NutType.GIANT, 8);
            return;
        }
        inventory.put(NutType.BOWLING, 16);
        inventory.put(NutType.EXPLOSIVE, 13);
        inventory.put(NutType.GIANT, 10);
    }

    private void initializeSchedule() {
        int zombieCount = getStage() == 1 ? 14 : getStage() == 2 ? 20 : 26;
        int interval = getStage() == 1 ? 30 : getStage() == 2 ? 24 : 20;
        String[][] pools = {
                {"Default", "cone head", "Imp"},
                {"Default", "cone head", "bucket head", "Explorer", "Imp"},
                {"cone head", "bucket head", "brick head", "Explorer", "Hunter", "Imp"}
        };
        String[] pool = pools[getStage() - 1];
        for (int index = 0; index < zombieCount; index++) {
            int tick = 10 + index * interval;
            int lane = random.nextInt(board.getHeight()) + 1;
            String zombieName = pool[random.nextInt(pool.length)];
            spawnSchedule.add(new SpawnEntry(tick, lane, zombieName));
        }
    }

    private int totalInventory() {
        int total = 0;
        for (Integer value : inventory.values()) {
            total += value;
        }
        return total;
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replace("'", "")
                .replaceAll("\\s+", " ");
    }
}
