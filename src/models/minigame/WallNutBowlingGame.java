package models.minigame;

import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Lane;
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
            if (normalized.equals("bowling") || normalized.equals("wallnut")
                    || normalized.equals("bowling wallnut")) {
                return BOWLING;
            }
            if (normalized.equals("explosive") || normalized.equals("explode o nut")
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
        private final NutType type;
        private final Set<Zombie> hitZombies;
        private double x;
        private double y;
        private double dx;
        private double dy;
        private int collisionCount;
        private boolean active;

        private BowlingNut(NutType type, double x, double y) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.dx = 0.32;
            this.dy = 0;
            this.collisionCount = 0;
            this.active = true;
            this.hitZombies = Collections.newSetFromMap(new IdentityHashMap<>());
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
    private final Map<NutType, Integer> inventory;
    private final int redLineColumn;
    private int nextSpawnIndex;
    private int launchedNuts;
    private int crushedZombies;

    public WallNutBowlingGame(int stage) {
        super(MiniGameType.WALLNUT_BOWLING, stage);
        this.board = new Board();
        this.zombieFactory = new ZombieFactory();
        this.random = new Random(9_200L + stage);
        this.activeNuts = new ArrayList<>();
        this.spawnSchedule = new ArrayList<>();
        this.inventory = new LinkedHashMap<>();
        this.redLineColumn = 3;
        this.nextSpawnIndex = 0;
        this.launchedNuts = 0;
        this.crushedZombies = 0;
        disableMowers();
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
        activeNuts.add(new BowlingNut(type, position.getX(), position.getY()));
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
        if (activeNuts.isEmpty() && totalInventory() == 0
                && (board.getActiveZombieCount() > 0 || !allSpawned)) {
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
        StringBuilder builder = new StringBuilder();
        builder.append("red line: column ").append(redLineColumn).append('\n');
        builder.append(renderBoard(board, overlays));
        return builder.toString();
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

            if (!nut.active || nut.x > board.getWidth() + 0.75) {
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
                if (Math.abs(nut.x - zombie.getX()) > 0.48
                        || Math.abs(nut.y - zombie.getY()) > 0.48) {
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

    private void explodeAt(double centerX, double centerY) {
        int hits = 0;
        for (Zombie zombie : new ArrayList<>(board.getAllZombies())) {
            if (Math.abs(zombie.getX() - centerX) <= 1.0
                    && Math.abs(zombie.getY() - centerY) <= 1.0) {
                zombie.takeDamage(new Damage(NutType.EXPLOSIVE.damage, "exploding wall-nut"));
                hits++;
            }
        }
        crushedZombies += hits;
    }

    private void spawnReadyZombies() {
        while (nextSpawnIndex < spawnSchedule.size()
                && spawnSchedule.get(nextSpawnIndex).tick <= getCurrentTick()) {
            SpawnEntry entry = spawnSchedule.get(nextSpawnIndex++);
            Zombie zombie = zombieFactory.createZombie(
                    entry.zombieName,
                    board.getWidth(),
                    entry.lane
            );
            Tile tile = board.getTileAt(new Position(board.getWidth(), entry.lane));
            tile.addZombie(zombie);
        }
    }

    private void initializeInventory() {
        int stage = getStage();
        inventory.put(NutType.BOWLING, 12 + stage * 5);
        inventory.put(NutType.EXPLOSIVE, 2 + stage);
        inventory.put(NutType.GIANT, stage == 1 ? 1 : stage);
    }

    private void initializeSchedule() {
        int stage = getStage();
        int zombieCount = 12 + stage * 8;
        int interval = 28 - stage * 4;
        String[][] pools = {
                {"Default", "cone head", "Imp"},
                {"Default", "cone head", "bucket head", "Explorer", "Imp"},
                {"cone head", "bucket head", "brick head", "Explorer", "Hunter", "Imp"}
        };
        String[] pool = pools[stage - 1];
        for (int index = 0; index < zombieCount; index++) {
            int tick = 5 + index * interval;
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

    private void disableMowers() {
        for (Lane lane : board.getLanes()) {
            lane.getLawnMower().disable();
        }
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
