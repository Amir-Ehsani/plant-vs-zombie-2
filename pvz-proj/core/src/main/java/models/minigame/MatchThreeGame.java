package models.minigame;

import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.combat.BoardTickResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class MatchThreeGame extends MiniGameSession {
    private static final int MATCH_SUN = 50;
    private static final int[] MATCH_TARGETS = {15, 25, 35};
    private static final int[] FIRST_WAVE_DELAYS = {55, 45, 35};
    private static final int[] ZOMBIES_PER_WAVE = {6, 7, 8};
    private static final int MIN_BURST_DELAY = 20;
    private static final int MAX_BURST_DELAY = 30;
    private static final int MIN_WAVE_DELAY = 65;
    private static final int MAX_WAVE_DELAY = 95;
    private static final int SHUFFLE_ATTEMPTS = 256;

    private final Board board;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Random random;
    private final List<PlantFamily> plantFamilies;
    private final Set<Position> craters;
    private final Set<Position> recentMatchedPositions;
    private final ArrayDeque<String> announcements;
    private int nextZombieTick;
    private int zombiesRemainingInWave;
    private int zombieWaveNumber;
    private int sunAmount;
    private int completedMatches;
    private int successfulSwaps;

    public MatchThreeGame(int stage) {
        super(MiniGameType.MATCH_THREE, stage);
        board = new Board();
        plantFactory = new PlantFactory();
        zombieFactory = new ZombieFactory();
        random = new Random(61_000L + stage);
        plantFamilies = createPlantFamilies(stage);
        craters = new LinkedHashSet<>();
        recentMatchedPositions = new LinkedHashSet<>();
        announcements = new ArrayDeque<>();
        sunAmount = 0;
        completedMatches = 0;
        successfulSwaps = 0;
        nextZombieTick = FIRST_WAVE_DELAYS[stage - 1];
        zombiesRemainingInWave = 0;
        zombieWaveNumber = 0;
        disableMowers();
        resetPlants();
        success("Beghouled stage " + stage + " started. Make " + getTargetMatches() + " matches.");
    }

    public boolean swapPlants(Position first, Position second) {
        if (!isRunning()) {
            fail("The mini-game is already finished.");
            return false;
        }
        if (!canSwap(first, second)) {
            fail("Select two adjacent plants that are not on craters.");
            return false;
        }

        recentMatchedPositions.clear();
        swapUnchecked(first, second);
        List<MatchRun> initialMatches = findMatchRuns();
        if (initialMatches.isEmpty()) {
            swapUnchecked(first, second);
            fail("That swap does not create a match.");
            return false;
        }

        successfulSwaps++;
        int before = completedMatches;
        resolveMatches(initialMatches);
        if (isRunning() && !hasLegalSwap()) {
            resetPlants();
        }
        if (completedMatches >= getTargetMatches()) {
            board.destroyAllZombies();
            markWon("Match target reached. Beghouled was won.");
            return true;
        }
        success("Matched " + (completedMatches - before) + " group(s). Sun: " + sunAmount + ".");
        return true;
    }

    public boolean upgradePlant(String sourcePlantName) {
        if (!isRunning()) {
            fail("The mini-game is already finished.");
            return false;
        }
        PlantFamily family = familyForCurrentPlant(sourcePlantName);
        UpgradeStep step = family == null ? null : family.nextUpgrade();
        if (step == null) {
            fail("That plant has no upgrade available.");
            return false;
        }
        if (sunAmount < step.cost()) {
            fail("Not enough sun. " + step.cost() + " sun is required.");
            return false;
        }

        replacePlantType(family.currentName(), step.targetName());
        sunAmount -= step.cost();
        family.advance();
        success(step.sourceName() + " upgraded to " + step.targetName() + ".");
        return true;
    }

    @Override
    protected void onTick() {
        spawnZombieIfReady();
        Map<Position, EatenPlantSnapshot> eatenPlants = snapshotPlantsBeingEaten();
        BoardTickResult result = board.updateTicks();
        markEatenPlantsAsCraters(eatenPlants);
    }

    @Override
    protected void evaluateStatus() {
        if (!isRunning()) {
            return;
        }
        if (completedMatches >= getTargetMatches()) {
            board.destroyAllZombies();
            markWon("Match target reached. Beghouled was won.");
            return;
        }
        if (board.hasBrainBeenEaten()) {
            markLost("A zombie reached the house. Beghouled was lost.");
        }
    }

    @Override
    public String renderMap() {
        Map<Position, String> overlays = new LinkedHashMap<>();
        for (Position crater : craters) {
            overlays.put(crater, "CRT");
        }
        return renderBoard(board, overlays);
    }

    @Override
    public String renderStatus() {
        return compactStatus()
                + "\nsun=" + sunAmount
                + "\nmatches=" + completedMatches + "/" + getTargetMatches()
                + "\nremaining matches=" + getRemainingMatches()
                + "\ncraters=" + craters.size()
                + "\nactive zombies=" + board.getActiveZombieCount()
                + "\nsuccessful swaps=" + successfulSwaps;
    }

    @Override
    public String renderHelp() {
        return """
                Beghouled commands
                swap plants -a <x, y> -b <x, y>
                upgrade plant -t <type>
                show map
                show status
                advance time -t <count> ticks
                """.trim();
    }

    public Board getBoard() {
        return board;
    }

    public int getSunAmount() {
        return sunAmount;
    }

    public int getTargetMatches() {
        return MATCH_TARGETS[getStage() - 1];
    }

    public int getCompletedMatches() {
        return completedMatches;
    }

    public int getRemainingMatches() {
        return Math.max(0, getTargetMatches() - completedMatches);
    }

    public Set<Position> getCraters() {
        return Collections.unmodifiableSet(craters);
    }

    public Set<Position> getRecentMatchedPositions() {
        return Collections.unmodifiableSet(recentMatchedPositions);
    }

    public List<String> consumeAnnouncements() {
        List<String> result = new ArrayList<>(announcements);
        announcements.clear();
        return List.copyOf(result);
    }

    public void addDebugSun(int amount) {
        if (amount <= 0) {
            return;
        }
        sunAmount += amount;
        success(amount + " debug sun added.");
    }

    public List<UpgradeOptionView> getUpgradeOptions() {
        List<UpgradeOptionView> result = new ArrayList<>();
        for (PlantFamily family : plantFamilies) {
            UpgradeStep step = family.nextUpgrade();
            if (step != null) {
                result.add(new UpgradeOptionView(step.sourceName(), step.targetName(), step.cost()));
            }
        }
        return List.copyOf(result);
    }

    private void resolveMatches(List<MatchRun> initialMatches) {
        List<MatchRun> matches = initialMatches;
        int cascade = 0;
        while (!matches.isEmpty()) {
            rewardMatches(matches, cascade);
            removeMatchedPlants(matches);
            collapseAndRefill();
            matches = findMatchRuns();
            cascade++;
        }
    }

    private void rewardMatches(List<MatchRun> matches, int cascade) {
        int cascadeBonus = cascade == 0 ? 0 : MATCH_SUN;
        for (MatchRun run : matches) {
            int reward = MATCH_SUN * Math.max(1, run.positions().size() - 2) + cascadeBonus;
            completedMatches++;
            sunAmount += reward;
        }
    }

    private void removeMatchedPlants(List<MatchRun> matches) {
        Set<Position> matchedPositions = new LinkedHashSet<>();
        for (MatchRun run : matches) {
            matchedPositions.addAll(run.positions());
        }
        recentMatchedPositions.addAll(matchedPositions);
        for (Position position : matchedPositions) {
            board.removePlant(position);
        }
    }

    private void collapseAndRefill() {
        for (int x = 1; x <= board.getWidth(); x++) {
            collapseColumn(x);
        }
        fillEmptyCells();
    }

    private void collapseColumn(int x) {
        List<Plant> plants = new ArrayList<>();
        for (int y = board.getHeight(); y >= 1; y--) {
            Position position = new Position(x, y);
            Plant plant = board.removePlant(position);
            if (plant != null) {
                plants.add(plant);
            }
        }

        int index = 0;
        for (int y = board.getHeight(); y >= 1 && index < plants.size(); y--) {
            Position target = new Position(x, y);
            if (craters.contains(target)) {
                continue;
            }
            Plant plant = plants.get(index++);
            plant.moveTo(x, y);
            board.placePlant(plant, target);
        }
    }

    private void fillEmptyCells() {
        for (int y = board.getHeight(); y >= 1; y--) {
            for (int x = 1; x <= board.getWidth(); x++) {
                Position position = new Position(x, y);
                if (!craters.contains(position) && !board.getTileAt(position).hasPlant()) {
                    placePlant(randomPlantName(), position);
                }
            }
        }
    }

    private void resetPlants() {
        for (int attempt = 0; attempt < SHUFFLE_ATTEMPTS; attempt++) {
            clearPlants();
            fillEmptyCells();
            if (findMatchRuns().isEmpty() && hasLegalSwap()) {
                return;
            }
        }
        clearPlants();
        fillEmptyCells();
    }

    private void clearPlants() {
        for (int y = 1; y <= board.getHeight(); y++) {
            for (int x = 1; x <= board.getWidth(); x++) {
                board.removePlant(new Position(x, y));
            }
        }
    }

    private List<MatchRun> findMatchRuns() {
        List<MatchRun> result = new ArrayList<>();
        findHorizontalRuns(result);
        findVerticalRuns(result);
        return result;
    }

    private void findHorizontalRuns(List<MatchRun> result) {
        for (int y = 1; y <= board.getHeight(); y++) {
            int start = 1;
            while (start <= board.getWidth()) {
                String name = plantNameAt(start, y);
                int end = start + 1;
                while (name != null && end <= board.getWidth() && name.equals(plantNameAt(end, y))) {
                    end++;
                }
                if (name != null && end - start >= 3) {
                    result.add(new MatchRun(linePositions(start, end - 1, y, true)));
                }
                start = name == null ? start + 1 : end;
            }
        }
    }

    private void findVerticalRuns(List<MatchRun> result) {
        for (int x = 1; x <= board.getWidth(); x++) {
            int start = 1;
            while (start <= board.getHeight()) {
                String name = plantNameAt(x, start);
                int end = start + 1;
                while (name != null && end <= board.getHeight() && name.equals(plantNameAt(x, end))) {
                    end++;
                }
                if (name != null && end - start >= 3) {
                    result.add(new MatchRun(linePositions(start, end - 1, x, false)));
                }
                start = name == null ? start + 1 : end;
            }
        }
    }

    private List<Position> linePositions(int start, int end, int fixed, boolean horizontal) {
        List<Position> positions = new ArrayList<>();
        for (int value = start; value <= end; value++) {
            positions.add(horizontal ? new Position(value, fixed) : new Position(fixed, value));
        }
        return positions;
    }

    private boolean hasLegalSwap() {
        for (int y = 1; y <= board.getHeight(); y++) {
            for (int x = 1; x <= board.getWidth(); x++) {
                Position position = new Position(x, y);
                if (x < board.getWidth() && createsMatch(position, new Position(x + 1, y))) {
                    return true;
                }
                if (y < board.getHeight() && createsMatch(position, new Position(x, y + 1))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean createsMatch(Position first, Position second) {
        if (!canSwap(first, second)) {
            return false;
        }
        swapUnchecked(first, second);
        boolean result = !findMatchRuns().isEmpty();
        swapUnchecked(first, second);
        return result;
    }

    private boolean canSwap(Position first, Position second) {
        if (first == null || second == null || !board.isValidPosition(first) || !board.isValidPosition(second)) {
            return false;
        }
        int distance = Math.abs(first.getX() - second.getX()) + Math.abs(first.getY() - second.getY());
        return distance == 1
                && !craters.contains(first)
                && !craters.contains(second)
                && board.getTileAt(first).hasPlant()
                && board.getTileAt(second).hasPlant();
    }

    private void swapUnchecked(Position first, Position second) {
        Plant firstPlant = board.removePlant(first);
        Plant secondPlant = board.removePlant(second);
        secondPlant.moveTo(first.getX(), first.getY());
        firstPlant.moveTo(second.getX(), second.getY());
        board.placePlant(secondPlant, first);
        board.placePlant(firstPlant, second);
    }

    private void spawnZombieIfReady() {
        if (getCurrentTick() < nextZombieTick) {
            return;
        }
        if (zombiesRemainingInWave <= 0) {
            startZombieWave();
        }
        spawnZombieBurst();
    }

    private void startZombieWave() {
        zombieWaveNumber++;
        int baseCount = ZOMBIES_PER_WAVE[getStage() - 1];
        zombiesRemainingInWave = Math.max(1, baseCount + random.nextInt(3) - 1);
        announce("WAVE " + zombieWaveNumber + " - ZOMBIES APPROACHING!");
    }

    private void spawnZombieBurst() {
        int burstSize = 1 + random.nextInt(Math.min(3, zombiesRemainingInWave));
        List<Integer> availableLanes = new ArrayList<>();
        for (int lane = 1; lane <= board.getHeight(); lane++) {
            availableLanes.add(lane);
        }

        String[] pool = zombiePool();
        for (int index = 0; index < burstSize; index++) {
            int laneIndex = random.nextInt(availableLanes.size());
            int lane = availableLanes.remove(laneIndex);
            String zombieName = pool[random.nextInt(pool.length)];
            double x = board.getWidth() + 2.8 + random.nextDouble() * 1.8;
            Zombie zombie = zombieFactory.createZombie(zombieName, x, lane);
            board.getTileAt(new Position(board.getWidth(), lane)).addZombie(zombie);
        }

        zombiesRemainingInWave -= burstSize;
        if (zombiesRemainingInWave > 0) {
            nextZombieTick = getCurrentTick() + randomDelay(MIN_BURST_DELAY, MAX_BURST_DELAY);
        } else {
            nextZombieTick = getCurrentTick() + randomDelay(MIN_WAVE_DELAY, MAX_WAVE_DELAY);
        }
    }

    private int randomDelay(int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    private Map<Position, EatenPlantSnapshot> snapshotPlantsBeingEaten() {
        Map<Position, EatenPlantSnapshot> result = new LinkedHashMap<>();
        for (Lane lane : board.getLanes()) {
            for (Zombie zombie : lane.getAllZombies()) {
                if (zombie == null || !zombie.isAlive() || zombie.getX() > board.getWidth()) {
                    continue;
                }
                int x = Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(zombie.getX())));
                Position position = new Position(x, lane.getLaneId());
                Tile tile = board.getTileAt(position);
                Plant plant = tile == null ? null : tile.getCurrentPlant();
                if (plant != null && plant.isAlive()) {
                    int health = plant.getHp() + plant.getArmorHp();
                    result.putIfAbsent(position, new EatenPlantSnapshot(plant, health));
                }
            }
        }
        return result;
    }

    private void markEatenPlantsAsCraters(Map<Position, EatenPlantSnapshot> snapshots) {
        for (Map.Entry<Position, EatenPlantSnapshot> entry : snapshots.entrySet()) {
            EatenPlantSnapshot snapshot = entry.getValue();
            Plant plant = snapshot.plant();
            int healthAfter = plant.getHp() + plant.getArmorHp();
            if (healthAfter < snapshot.healthBefore() && !plant.isAlive() && craters.add(entry.getKey())) {
                Position position = entry.getKey();
            }
        }
    }


    private void announce(String message) {
        if (message != null && !message.isBlank()) {
            announcements.add(message);
        }
    }

    private void replacePlantType(String source, String target) {
        List<Position> positions = new ArrayList<>();
        for (int y = 1; y <= board.getHeight(); y++) {
            for (int x = 1; x <= board.getWidth(); x++) {
                if (source.equalsIgnoreCase(plantNameAt(x, y))) {
                    positions.add(new Position(x, y));
                }
            }
        }
        for (Position position : positions) {
            board.removePlant(position);
            placePlant(target, position);
        }
    }

    private PlantFamily familyForCurrentPlant(String plantName) {
        String normalized = normalize(plantName);
        for (PlantFamily family : plantFamilies) {
            if (normalize(family.currentName()).equals(normalized)) {
                return family;
            }
        }
        return null;
    }

    private String randomPlantName() {
        return plantFamilies.get(random.nextInt(plantFamilies.size())).currentName();
    }

    private String plantNameAt(int x, int y) {
        Tile tile = board.getTileAt(new Position(x, y));
        Plant plant = tile == null ? null : tile.getCurrentPlant();
        return plant == null ? null : plant.getName();
    }

    private void placePlant(String plantName, Position position) {
        Plant plant = plantFactory.createPlant(plantName, position.getX(), position.getY());
        // Beghouled plants stay on the board until matched or eaten. This also prevents
        // temporary plants such as Puff-shroom from expiring on their normal adventure timer.
        plant.increaseLifespan(1_000_000);
        if (!board.placePlant(plant, position)) {
            throw new IllegalStateException("Cannot place " + plantName + " at " + position + ".");
        }
    }

    private void disableMowers() {
        for (Lane lane : board.getLanes()) {
            lane.getLawnMower().disable();
        }
    }

    private String[] zombiePool() {
        if (getStage() == 1) {
            return new String[]{"Default", "cone head", "Imp"};
        }
        if (getStage() == 2) {
            return new String[]{"Default", "cone head", "bucket head", "Imp"};
        }
        return new String[]{"Default", "cone head", "bucket head", "brick head", "Imp"};
    }

    private List<PlantFamily> createPlantFamilies(int stage) {
        List<PlantFamily> result = new ArrayList<>();
        List<UpgradeStep> peaUpgrades = new ArrayList<>();
        peaUpgrades.add(new UpgradeStep("Peashooter", "Repeater", 500));
        if (stage >= 2) {
            peaUpgrades.add(new UpgradeStep("Repeater", "Mega Gatling Pea", 1500));
        }
        result.add(new PlantFamily("Peashooter", peaUpgrades));
        result.add(new PlantFamily("Wall-nut", List.of(
                new UpgradeStep("Wall-nut", "Tall-nut", 500)
        )));
        List<UpgradeStep> puffUpgrades = stage == 3
                ? List.of()
                : List.of(new UpgradeStep("Puff-shroom", "Fume-shroom", 250));
        result.add(new PlantFamily("Puff-shroom", puffUpgrades));
        List<UpgradeStep> cabbageUpgrades = new ArrayList<>();
        cabbageUpgrades.add(new UpgradeStep("Cabbage-pult", "Melon-pult", 1000));
        if (stage != 2) {
            cabbageUpgrades.add(new UpgradeStep("Melon-pult", "Winter Melon", 750));
        }
        result.add(new PlantFamily("Cabbage-pult", cabbageUpgrades));
        String fifth = stage == 1 ? "Sunflower" : stage == 2 ? "Bonk Choy" : "Snow Pea";
        result.add(new PlantFamily(fifth, List.of()));
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }

    private record EatenPlantSnapshot(Plant plant, int healthBefore) {
    }

    public record UpgradeOptionView(String sourcePlantName, String targetPlantName, int cost) {
    }

    private record MatchRun(List<Position> positions) {
    }

    private record UpgradeStep(String sourceName, String targetName, int cost) {
    }

    private static final class PlantFamily {
        private final List<UpgradeStep> upgrades;
        private int upgradeIndex;
        private String currentName;

        private PlantFamily(String currentName, List<UpgradeStep> upgrades) {
            this.currentName = currentName;
            this.upgrades = upgrades;
            this.upgradeIndex = 0;
        }

        private String currentName() {
            return currentName;
        }

        private UpgradeStep nextUpgrade() {
            return upgradeIndex >= upgrades.size() ? null : upgrades.get(upgradeIndex);
        }

        private void advance() {
            UpgradeStep step = nextUpgrade();
            if (step != null) {
                currentName = step.targetName();
                upgradeIndex++;
            }
        }
    }
}
