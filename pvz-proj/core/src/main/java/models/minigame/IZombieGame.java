package models.minigame;

import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.zombie.MovementStrategy;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.core.zombie.ZombieType;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class IZombieGame extends MiniGameSession {
    private static final class ZombieOption {
        private final String zombieName;
        private final int sunCost;

        private ZombieOption(String zombieName, int sunCost) {
            this.zombieName = zombieName;
            this.sunCost = sunCost;
        }
    }

    private static final class ProducerState {
        private final Zombie zombie;
        private int nextProductionTick;

        private ProducerState(Zombie zombie, int nextProductionTick) {
            this.zombie = zombie;
            this.nextProductionTick = nextProductionTick;
        }
    }

    private static final class SunDrop {
        private final int id;
        private final int amount;
        private final double x;
        private final double y;
        private int remainingTicks;

        private SunDrop(int id, int amount, double x, double y, int remainingTicks) {
            this.id = id;
            this.amount = amount;
            this.x = x;
            this.y = y;
            this.remainingTicks = remainingTicks;
        }
    }

    public static final int INITIAL_SUN = 350;
    public static final int RED_LINE_COLUMN = 6;
    private static final int PRODUCER_HP = 1290;
    private static final int PRODUCED_SUN = 50;
    private static final int SUN_DROP_LIFE_TICKS = 55;
    private static final int FIRST_MINUTE_INTERVAL = 120;
    private static final int SECOND_MINUTE_INTERVAL = 80;
    private static final int LATE_GAME_INTERVAL = 50;

    private final Board board;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Map<String, ZombieOption> availableZombies;
    private final List<ProducerState> producers;
    private final List<SunDrop> sunDrops;
    private final Set<Integer> resolvedBrainLanes;
    private int nextSunDropId;
    private int sunAmount;
    private int spentSun;
    private int spawnedZombies;
    private boolean sunProductionStarted;

    public IZombieGame(int stage) {
        super(MiniGameType.I_ZOMBIE, stage);
        board = new Board();
        plantFactory = new PlantFactory();
        zombieFactory = new ZombieFactory();
        availableZombies = new LinkedHashMap<>();
        producers = new ArrayList<>();
        sunDrops = new ArrayList<>();
        resolvedBrainLanes = new LinkedHashSet<>();
        nextSunDropId = 1;
        sunAmount = INITIAL_SUN;
        spentSun = 0;
        spawnedZombies = 0;
        sunProductionStarted = false;
        disableMowers();
        enableContinuedLaneCombat();
        initializeZombieOptions();
        initializePlantDefense();
        initializeSunProducers();
        success("I, Zombie stage " + stage + " started with " + INITIAL_SUN + " sun.");
    }

    public boolean spawnZombie(String zombieName, Position position) {
        if (!isRunning()) {
            fail("The mini-game is already finished.");
            return false;
        }
        ZombieOption option = availableZombies.get(normalize(zombieName));
        if (option == null) {
            fail("That zombie is not available in this stage.");
            return false;
        }
        if (position == null || !board.isValidPosition(position)) {
            fail("Zombie position is invalid.");
            return false;
        }
        if (position.getX() <= RED_LINE_COLUMN) {
            fail("Zombies must be placed to the right of column " + RED_LINE_COLUMN + ".");
            return false;
        }
        if (position.getX() == board.getWidth() && hasLivingProducerAt(position.getY())) {
            fail("The wizard must die before that tile can be used.");
            return false;
        }
        if (sunAmount < option.sunCost) {
            fail("Not enough sun. " + option.sunCost + " sun is required.");
            return false;
        }

        Zombie zombie;
        try {
            zombie = createPlayableZombie(option.zombieName, position);
        } catch (IllegalArgumentException exception) {
            fail("The selected zombie type is unavailable in the registry.");
            return false;
        }

        Tile tile = board.getTileAt(position);
        tile.addZombie(zombie);
        sunAmount -= option.sunCost;
        spentSun += option.sunCost;
        spawnedZombies++;
        startSunProductionIfNeeded();
        success(zombie.getName() + " placed at " + position + ". Remaining sun: " + sunAmount + ".");
        return true;
    }

    public boolean collectSunDrop(int dropId) {
        Iterator<SunDrop> iterator = sunDrops.iterator();
        while (iterator.hasNext()) {
            SunDrop drop = iterator.next();
            if (drop.id != dropId) {
                continue;
            }
            sunAmount += drop.amount;
            iterator.remove();
            success("Collected " + drop.amount + " sun.");
            return true;
        }
        fail("That sun has already disappeared.");
        return false;
    }

    public String renderAvailableZombies() {
        StringBuilder builder = new StringBuilder("Available zombies:\n");
        for (ZombieOption option : availableZombies.values()) {
            builder.append(option.zombieName)
                    .append(" | cost=")
                    .append(option.sunCost)
                    .append(" sun\n");
        }
        return builder.toString().trim();
    }

    public int getSunAmount() {
        return sunAmount;
    }

    public void addDebugSun(int amount) {
        if (amount <= 0) {
            return;
        }
        sunAmount += amount;
        success(amount + " debug sun added.");
    }

    @Override
    protected void onTick() {
        updateSunDrops();
        updateSunProducers();
        if (spawnedZombies > 0) {
            board.updateTicks();
            resolveEatenBrainLanes();
            killZombiesAtEatenBrainPosition();
        }
    }

    @Override
    protected void evaluateStatus() {
        if (!isRunning()) {
            return;
        }
        if (eatenBrainCount() == board.getHeight()) {
            board.destroyAllZombies();
            markWon("All five brains were eaten. I, Zombie was won.");
            return;
        }
        if (board.getActiveZombieCount() == 0
                && sunDrops.isEmpty()
                && sunAmount < minimumZombieCost()) {
            markLost("All zombies are dead and there is not enough sun to place another zombie.");
        }
    }

    @Override
    public String renderMap() {
        return "red line: column " + RED_LINE_COLUMN + '\n' + renderBoard(board, null);
    }

    @Override
    public String renderStatus() {
        return compactStatus()
                + "\nsun=" + sunAmount
                + "\nbrains eaten=" + eatenBrainCount() + "/" + board.getHeight()
                + "\nactive zombies=" + activeZombieCountInOpenLanes()
                + "\nliving sun producers=" + livingProducerCount()
                + "\nsun drops=" + sunDrops.size()
                + "\nspawned zombies=" + spawnedZombies
                + "\nspent sun=" + spentSun;
    }

    @Override
    public String renderHelp() {
        return """
                I, Zombie commands
                show zombies
                show sun
                spawn zombie -t <type> -l <x, y>
                show map
                show status
                advance time -t <count> ticks
                """.trim();
    }

    public Board getBoard() {
        return board;
    }

    public List<ZombieOptionView> getAvailableZombieOptions() {
        List<ZombieOptionView> result = new ArrayList<>();
        for (ZombieOption option : availableZombies.values()) {
            result.add(new ZombieOptionView(option.zombieName, option.sunCost, 0, 0));
        }
        return List.copyOf(result);
    }

    public List<SunDropView> getSunDrops() {
        List<SunDropView> result = new ArrayList<>();
        for (SunDrop drop : sunDrops) {
            result.add(new SunDropView(drop.id, drop.amount, drop.x, drop.y, drop.remainingTicks));
        }
        return List.copyOf(result);
    }

    public int getRedLineColumn() {
        return RED_LINE_COLUMN;
    }

    public record ZombieOptionView(String zombieName, int sunCost, int remainingRechargeTicks, int rechargeTicks) {
    }

    public record SunDropView(int id, int amount, double x, double y, int remainingTicks) {
    }

    private Zombie createPlayableZombie(String zombieName, Position position) {
        if (!"Wizard".equalsIgnoreCase(zombieName)) {
            return zombieFactory.createZombie(zombieName, position.getX(), position.getY());
        }
        ZombieType base = zombieFactory.getZombieRegistry().getZombieTypeByName(zombieName);
        if (base == null) {
            throw new IllegalArgumentException("Unknown zombie type: " + zombieName);
        }
        ZombieType type = new ZombieType(
                "I Zombie Wizard",
                base.getBaseHp(),
                base.getSpeed(),
                base.getDamagePerTick(),
                base.getWaveCost(),
                base.getId(),
                base.getDefaultArmorName(),
                base.getTags(),
                base.getAbility()
        );
        return zombieFactory.createZombie(type, position.getX(), position.getY());
    }

    private void initializeZombieOptions() {
        for (ZombieOptionView option : zombieOptionsForStage(getStage())) {
            addOption(option.zombieName(), option.sunCost());
        }
    }

    private void initializePlantDefense() {
        String[][] layouts = plantLayoutForStage(getStage());
        for (int lane = 1; lane <= board.getHeight(); lane++) {
            for (int column = 1; column <= RED_LINE_COLUMN; column++) {
                placePlant(layouts[lane - 1][column - 1], column, lane);
            }
        }
    }

    /**
     * Shared stage catalog used by both the local I, Zombie mode and the
     * phase-three authoritative online mode.
     */
    public static List<ZombieOptionView> zombieOptionsForStage(int requestedStage) {
        int stage = Math.max(1, Math.min(3, requestedStage));
        if (stage == 1) {
            return List.of(
                    new ZombieOptionView("Default", 50, 0, 0),
                    new ZombieOptionView("cone head", 75, 0, 0),
                    new ZombieOptionView("Imp", 50, 0, 0)
            );
        }
        if (stage == 2) {
            return List.of(
                    new ZombieOptionView("bucket head", 125, 0, 0),
                    new ZombieOptionView("Ra", 100, 0, 0),
                    new ZombieOptionView("Explorer", 125, 0, 0)
            );
        }
        return List.of(
                new ZombieOptionView("Allstar", 225, 0, 0),
                new ZombieOptionView("Wizard", 175, 0, 0),
                new ZombieOptionView("Prospector", 100, 0, 0),
                new ZombieOptionView("Gargantuar", 350, 0, 0)
        );
    }

    public static String[][] plantLayoutForStage(int requestedStage) {
        int stage = Math.max(1, Math.min(3, requestedStage));
        if (stage == 1) {
            return new String[][]{
                    {"Sunflower", "Peashooter", "Wall-nut", "Peashooter", "Wall-nut", "Peashooter"},
                    {"Sunflower", "Peashooter", "Peashooter", "Wall-nut", "Peashooter", "Wall-nut"},
                    {"Sunflower", "Peashooter", "Wall-nut", "Peashooter", "Wall-nut", "Peashooter"},
                    {"Sunflower", "Peashooter", "Peashooter", "Wall-nut", "Peashooter", "Wall-nut"},
                    {"Sunflower", "Peashooter", "Wall-nut", "Peashooter", "Wall-nut", "Peashooter"}
            };
        }
        if (stage == 2) {
            return new String[][]{
                    {"Sunflower", "Peashooter", "Repeater", "Snow Pea", "Wall-nut", "Tall-nut"},
                    {"Sunflower", "Repeater", "Wall-nut", "Snow Pea", "Repeater", "Tall-nut"},
                    {"Sunflower", "Peashooter", "Repeater", "Snow Pea", "Wall-nut", "Tall-nut"},
                    {"Sunflower", "Repeater", "Wall-nut", "Snow Pea", "Repeater", "Tall-nut"},
                    {"Sunflower", "Peashooter", "Repeater", "Snow Pea", "Wall-nut", "Tall-nut"}
            };
        }
        return new String[][]{
                {"Sunflower", "Repeater", "Threepeater", "Snow Pea", "Wall-nut", "Tall-nut"},
                {"Sunflower", "Snow Pea", "Repeater", "Threepeater", "Wall-nut", "Tall-nut"},
                {"Sunflower", "Repeater", "Threepeater", "Snow Pea", "Wall-nut", "Tall-nut"},
                {"Sunflower", "Snow Pea", "Repeater", "Threepeater", "Wall-nut", "Tall-nut"},
                {"Sunflower", "Repeater", "Threepeater", "Snow Pea", "Wall-nut", "Tall-nut"}
        };
    }

    private void initializeSunProducers() {
        for (int lane = 1; lane <= board.getHeight(); lane++) {
            ZombieType producerType = new ZombieType(
                    "Sun Producer Zombie",
                    PRODUCER_HP,
                    0,
                    0,
                    0,
                    "ZombieWizard",
                    null,
                    List.of("stationary"),
                    ""
            );
            MovementStrategy stationary = zombie -> {
            };
            Zombie producer = new Zombie(producerType, board.getWidth(), lane, null, stationary, null);
            board.getTileAt(new Position(board.getWidth(), lane)).addZombie(producer);
            producers.add(new ProducerState(producer, Integer.MAX_VALUE));
        }
    }

    private void startSunProductionIfNeeded() {
        if (sunProductionStarted) {
            return;
        }
        sunProductionStarted = true;
        int laneOffset = 0;
        for (ProducerState state : producers) {
            state.nextProductionTick = getCurrentTick() + 35 + laneOffset;
            laneOffset += 17;
        }
    }

    private void updateSunDrops() {
        Iterator<SunDrop> iterator = sunDrops.iterator();
        while (iterator.hasNext()) {
            SunDrop drop = iterator.next();
            drop.remainingTicks--;
            if (drop.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    private void updateSunProducers() {
        if (!sunProductionStarted) {
            return;
        }
        for (ProducerState state : producers) {
            if (!state.zombie.isAlive()) {
                continue;
            }
            Lane lane = board.getLaneAt((int) Math.round(state.zombie.getY()));
            if (lane == null || lane.hasBrainBeenEaten() || getCurrentTick() < state.nextProductionTick) {
                continue;
            }
            double dropX = Math.max(RED_LINE_COLUMN + 0.5, state.zombie.getX() - 0.70);
            sunDrops.add(new SunDrop(
                    nextSunDropId++,
                    PRODUCED_SUN,
                    dropX,
                    state.zombie.getY(),
                    SUN_DROP_LIFE_TICKS
            ));
            int nextTick = getCurrentTick() + productionIntervalForTick(getCurrentTick());
            if (getCurrentTick() < 600 && nextTick > 600) {
                nextTick = 600;
            } else if (getCurrentTick() < 1200 && nextTick > 1200) {
                nextTick = 1200;
            }
            state.nextProductionTick = nextTick;
        }
    }

    private int productionIntervalForTick(int tick) {
        if (tick < 600) {
            return FIRST_MINUTE_INTERVAL;
        }
        if (tick < 1200) {
            return SECOND_MINUTE_INTERVAL;
        }
        return LATE_GAME_INTERVAL;
    }

    private void resolveEatenBrainLanes() {
        for (Lane lane : board.getLanes()) {
            if (!lane.hasBrainBeenEaten() || !resolvedBrainLanes.add(lane.getLaneId())) {
                continue;
            }
            killLaneZombies(lane);
        }
        board.removeDeadEntities();
    }

    private void killZombiesAtEatenBrainPosition() {
        boolean killedAny = false;
        for (Lane lane : board.getLanes()) {
            if (!lane.hasBrainBeenEaten()) {
                continue;
            }
            for (Zombie zombie : new ArrayList<>(lane.getAllZombies())) {
                if (zombie.isAlive() && zombie.getX() <= 0.0) {
                    zombie.kill();
                    killedAny = true;
                }
            }
        }
        if (killedAny) {
            board.removeDeadEntities();
        }
    }

    private void killLaneZombies(Lane lane) {
        for (Zombie zombie : new ArrayList<>(lane.getAllZombies())) {
            if (zombie.isAlive()) {
                zombie.kill();
            }
        }
    }

    private void placePlant(String name, int x, int y) {
        Plant plant = plantFactory.createPlant(name, x, y);
        Position position = new Position(x, y);
        if (!board.placePlant(plant, position)) {
            throw new IllegalStateException("Cannot initialize plant " + name + " at " + position + ".");
        }
    }

    private void addOption(String name, int cost) {
        availableZombies.put(normalize(name), new ZombieOption(name, cost));
    }

    private int eatenBrainCount() {
        int count = 0;
        for (Lane lane : board.getLanes()) {
            if (lane.hasBrainBeenEaten()) {
                count++;
            }
        }
        return count;
    }

    private boolean hasLivingZombieInOpenLane() {
        return activeZombieCountInOpenLanes() > 0;
    }

    private int activeZombieCountInOpenLanes() {
        int count = 0;
        for (Lane lane : board.getLanes()) {
            if (!lane.hasBrainBeenEaten()) {
                count += lane.getActiveZombieCount();
            }
        }
        return count;
    }

    private int livingProducerCount() {
        int count = 0;
        for (ProducerState producer : producers) {
            if (producer.zombie.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private int minimumZombieCost() {
        int minimum = Integer.MAX_VALUE;
        for (ZombieOption option : availableZombies.values()) {
            minimum = Math.min(minimum, option.sunCost);
        }
        return minimum == Integer.MAX_VALUE ? 0 : minimum;
    }

    private boolean hasLivingProducerAt(int laneNumber) {
        for (ProducerState producer : producers) {
            if (producer.zombie.isAlive() && (int) Math.round(producer.zombie.getY()) == laneNumber) {
                return true;
            }
        }
        return false;
    }

    private void enableContinuedLaneCombat() {
        for (Lane lane : board.getLanes()) {
            lane.setContinueAfterBrainEaten(true);
        }
    }

    private void disableMowers() {
        for (Lane lane : board.getLanes()) {
            lane.getLawnMower().disable();
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}
