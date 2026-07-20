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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        private int productionCount;

        private ProducerState(Zombie zombie, int nextProductionTick) {
            this.zombie = zombie;
            this.nextProductionTick = nextProductionTick;
            this.productionCount = 0;
        }
    }

    private static final int INITIAL_SUN = 150;
    private static final int PRODUCED_SUN = 25;
    private static final int RED_LINE_COLUMN = 6;
    private static final int PRODUCER_HP = 1290;

    private final Board board;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Map<String, ZombieOption> availableZombies;
    private final List<ProducerState> producers;
    private int sunAmount;
    private int spentSun;
    private int spawnedZombies;

    public IZombieGame(int stage) {
        super(MiniGameType.I_ZOMBIE, stage);
        this.board = new Board();
        this.plantFactory = new PlantFactory();
        this.zombieFactory = new ZombieFactory();
        this.availableZombies = new LinkedHashMap<>();
        this.producers = new ArrayList<>();
        this.sunAmount = INITIAL_SUN;
        this.spentSun = 0;
        this.spawnedZombies = 0;
        disableMowers();
        initializeZombieOptions();
        initializePlantDefense();
        initializeSunProducers();
        success("I, Zombie stage " + stage + " started with 150 sun.");
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
        if (sunAmount < option.sunCost) {
            fail("Not enough sun. " + option.sunCost + " sun is required.");
            return false;
        }

        Zombie zombie;
        try {
            zombie = zombieFactory.createZombie(
                    option.zombieName,
                    position.getX(),
                    position.getY()
            );
        } catch (IllegalArgumentException exception) {
            fail("The selected zombie type is unavailable in the registry.");
            return false;
        }

        Tile tile = board.getTileAt(position);
        tile.addZombie(zombie);
        sunAmount -= option.sunCost;
        spentSun += option.sunCost;
        spawnedZombies++;
        success(zombie.getName() + " placed at " + position
                + ". Remaining sun: " + sunAmount + ".");
        return true;
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

    @Override
    protected void onTick() {
        board.updateTicks();
        updateSunProducers();
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
        if (!hasLivingZombieInOpenLane() && sunAmount < minimumZombieCost()) {
            markLost("No zombies remain and there is not enough sun to place another zombie.");
        }
    }

    @Override
    public String renderMap() {
        StringBuilder builder = new StringBuilder();
        builder.append("red line: column ").append(RED_LINE_COLUMN).append('\n');
        builder.append("brains: ");
        for (Lane lane : board.getLanes()) {
            builder.append("row ").append(lane.getLaneId()).append('=')
                    .append(lane.hasBrainBeenEaten() ? "eaten" : "safe").append(' ');
        }
        builder.append('\n');
        builder.append(renderBoard(board, null));
        return builder.toString();
    }

    @Override
    public String renderStatus() {
        return compactStatus()
                + "\nsun=" + sunAmount
                + "\nbrains eaten=" + eatenBrainCount() + "/" + board.getHeight()
                + "\nactive zombies=" + activeZombieCountInOpenLanes()
                + "\nliving sun producers=" + livingProducerCount()
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

    private void initializeZombieOptions() {
        if (getStage() == 1) {
            addOption("Default", 50);
            addOption("cone head", 75);
            addOption("Imp", 50);
            addOption("Ra", 75);
            addOption("Explorer", 100);
            return;
        }
        if (getStage() == 2) {
            addOption("bucket head", 125);
            addOption("brick head", 175);
            addOption("Dodo", 125);
            addOption("Hunter", 150);
            addOption("News Paper", 125);
            return;
        }
        addOption("knight", 175);
        addOption("Gargantuar", 350);
        addOption("Allstar", 225);
        addOption("Wizard", 175);
        addOption("Prospector", 100);
    }

    private void initializePlantDefense() {
        for (int lane = 1; lane <= board.getHeight(); lane++) {
            if (getStage() == 1) {
                placePlant(lane % 2 == 0 ? "Sunflower" : "Peashooter", 2, lane);
                placePlant("Peashooter", 4, lane);
                placePlant("Wall-nut", 5, lane);
            } else if (getStage() == 2) {
                placePlant("Sunflower", 2, lane);
                placePlant("Repeater", 3, lane);
                placePlant("Snow Pea", 4, lane);
                placePlant("Wall-nut", 5, lane);
            } else {
                placePlant("Sunflower", 1, lane);
                placePlant("Repeater", 2, lane);
                placePlant("Threepeater", 3, lane);
                placePlant("Snow Pea", 4, lane);
                placePlant("Tall-nut", 5, lane);
            }
        }
    }

    private void initializeSunProducers() {
        int firstProductionTick = 180 + (getStage() - 1) * 20;
        for (int lane = 1; lane <= board.getHeight(); lane++) {
            ZombieType producerType = new ZombieType(
                    "Sun Producer Zombie",
                    PRODUCER_HP,
                    0,
                    0,
                    0,
                    "ZombieSunProducer",
                    null
            );
            MovementStrategy stationary = zombie -> {
            };
            Zombie producer = new Zombie(
                    producerType,
                    board.getWidth(),
                    lane,
                    null,
                    stationary,
                    null
            );
            board.getTileAt(new Position(board.getWidth(), lane)).addZombie(producer);
            producers.add(new ProducerState(producer, firstProductionTick));
        }
    }

    private void updateSunProducers() {
        for (ProducerState state : producers) {
            if (!state.zombie.isAlive()) {
                continue;
            }
            Lane lane = board.getLaneAt((int) Math.round(state.zombie.getY()));
            if (lane == null || lane.hasBrainBeenEaten()) {
                continue;
            }
            if (getCurrentTick() < state.nextProductionTick) {
                continue;
            }

            sunAmount += PRODUCED_SUN;
            state.productionCount++;
            int initialInterval = 180 + (getStage() - 1) * 20;
            int nextInterval = Math.max(50, initialInterval - state.productionCount * 10);
            state.nextProductionTick = getCurrentTick() + nextInterval;
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
