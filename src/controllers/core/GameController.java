package controllers.core;

import controllers.auth.AuthController;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.core.zombie.Armor;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.engine.events.GameEvent;
import models.engine.session.GameSession;
import models.engine.session.GameState;
import models.engine.session.PlantRechargeStatus;
import models.engine.sun.Sun;
import models.level.core.Level;
import models.level.core.LevelType;
import models.level.rules.LevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.NoSpecialRule;
import models.level.rules.SpecialLevelType;
import models.level.rules.impl.ConveyorBeltRule;
import models.level.rules.impl.DeadLineRule;
import models.level.rules.impl.LockedPlantsRule;
import models.level.rules.impl.LoveYourPlantsRule;
import models.level.rules.impl.NightOpsRule;
import models.level.rules.impl.SaveOurSeedsRule;
import models.level.rules.impl.TimedWarRule;
import models.level.wave.AttackPattern;
import models.level.wave.Wave;
import models.level.wave.WaveManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GameController {
    private static final double TICKS_PER_SECOND = 10.0;
    private static final int PLANT_SELECTION_LIMIT = 8;
    private static final int BOOST_GEM_COST = 2;

    private final AuthController authController;
    private final PlantRegistry plantRegistry;
    private final Set<String> boostedPlantNames;
    private GameSession gameSession;
    private String lastMessage;

    public GameController(AuthController authController) {
        this.authController = authController;
        this.plantRegistry = DefaultPlantRegistry.getInstance();
        this.boostedPlantNames = new LinkedHashSet<>();
        this.lastMessage = "";
    }

    public void setGameSession(GameSession gameSession) {
        if (gameSession == null) {
            fail("Game session cannot be null.");
            return;
        }

        this.gameSession = gameSession;
        success("Game session is ready.");
    }

    public void prepareChapter(String chapterName) {
        if (chapterName == null || chapterName.isBlank()) {
            fail("Chapter name is required.");
            return;
        }

        GameSession session = new GameSession();
        session.setCurrentLevel(createDefaultLevel(chapterName));

        boostedPlantNames.clear();
        this.gameSession = session;

        success("Chapter " + chapterName.trim()
                + " is ready. Select up to "
                + PLANT_SELECTION_LIMIT
                + " plants before starting the game.");
    }

    public void showAllPlants() {
        if (!hasPreparedSession()) {
            fail("Game session is not available.");
            return;
        }

        StringBuilder builder = new StringBuilder("All Plants\n==========");

        for (PlantType type : plantRegistry.getAllPlantTypes()) {
            appendSelectablePlantLine(builder, type);
        }

        appendSelectedPlants(builder);
        success(builder.toString());
    }

    public void showAvailablePlants() {
        if (!hasPreparedSession()) {
            fail("Game session is not available.");
            return;
        }

        StringBuilder builder = new StringBuilder("Available Plants\n================");
        int availableCount = 0;

        for (PlantType type : plantRegistry.getAllPlantTypes()) {
            if (isPlantAvailableForCurrentLevel(type.getName())) {
                appendSelectablePlantLine(builder, type);
                availableCount++;
            }
        }

        if (availableCount == 0) {
            builder.append("\nNo available plants.");
        }

        appendSelectedPlants(builder);
        success(builder.toString());
    }

    public void addPlantToSelection(String plantName) {
        if (!hasPreparedSession()) {
            fail("Game session is not available.");
            return;
        }

        if (gameSession.isRunning()) {
            fail("You cannot change selected plants after the game starts.");
            return;
        }

        PlantType type = plantRegistry.getByName(plantName);

        if (type == null) {
            fail("Plant does not exist.");
            return;
        }

        if (!isPlantAvailableForCurrentLevel(type.getName())) {
            fail("Plant is not available in this chapter.");
            return;
        }

        if (isPlantSelectedForThisLevel(type.getName())) {
            fail("Plant is already selected.");
            return;
        }

        if (gameSession.getSelectedPlantNames().size() >= PLANT_SELECTION_LIMIT) {
            fail("You can select at most " + PLANT_SELECTION_LIMIT + " plants.");
            return;
        }

        if (!gameSession.addSelectedPlant(type.getName())) {
            fail("Plant could not be selected.");
            return;
        }

        success(type.getName() + " added to selected plants." + selectedPlantsText());
    }

    public void removePlantFromSelection(String plantName) {
        if (!hasPreparedSession()) {
            fail("Game session is not available.");
            return;
        }

        if (gameSession.isRunning()) {
            fail("You cannot change selected plants after the game starts.");
            return;
        }

        PlantType type = plantRegistry.getByName(plantName);

        if (type == null) {
            fail("Plant does not exist.");
            return;
        }

        if (!isPlantSelectedForThisLevel(type.getName())) {
            fail("Plant is not selected.");
            return;
        }

        if (!gameSession.removeSelectedPlant(type.getName())) {
            fail("Plant could not be removed.");
            return;
        }

        boostedPlantNames.remove(normalizeName(type.getName()));
        success(type.getName() + " removed from selected plants." + selectedPlantsText());
    }

    public void boostPlant(String plantName) {
        if (!hasPreparedSession()) {
            fail("Game session is not available.");
            return;
        }

        if (gameSession.isRunning()) {
            fail("You cannot boost selected plants after the game starts.");
            return;
        }

        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }

        PlantType type = plantRegistry.getByName(plantName);

        if (type == null) {
            fail("Plant does not exist.");
            return;
        }

        if (!isPlantSelectedForThisLevel(type.getName())) {
            fail("Plant must be selected before boosting.");
            return;
        }

        String normalizedPlantName = normalizeName(type.getName());

        if (boostedPlantNames.contains(normalizedPlantName)) {
            fail("Plant is already boosted.");
            return;
        }

        if (!user.spendGems(BOOST_GEM_COST)) {
            fail("Not enough gems. Required: " + BOOST_GEM_COST + ".");
            return;
        }

        boostedPlantNames.add(normalizedPlantName);
        saveUsers();

        success(type.getName()
                + " boosted for this game. "
                + BOOST_GEM_COST
                + " gems were spent; remaining gems: "
                + user.getGems()
                + "."
                + selectedPlantsText());
    }

    public void startGame() {
        if (gameSession == null) {
            fail("Game session is not available.");
            return;
        }

        if (gameSession.isRunning()) {
            fail("Game is already running.");
            return;
        }

        if (gameSession.getSelectedPlantNames().isEmpty()) {
            fail("Select at least one plant before starting the game.");
            return;
        }

        try {
            gameSession.initSession();

            StringBuilder builder = new StringBuilder("Game started.");
            appendEvents(builder, gameSession.drainEvents());
            appendFinishedState(builder);

            success(builder.toString());
        } catch (IllegalStateException | IllegalArgumentException exception) {
            fail(exception.getMessage());
        }
    }

    public void advanceTime(int ticks) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (ticks <= 0) {
            fail("Tick count must be positive.");
            return;
        }

        gameSession.clearPendingEvents();

        if (!gameSession.advanceTicks(ticks)) {
            fail("Time could not be advanced.");
            return;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Advanced ")
                .append(gameSession.getLastAdvancedTickCount())
                .append(" ticks. Current tick = ")
                .append(gameSession.getTickManager().getCurrentTick())
                .append(".");

        appendEvents(builder, gameSession.drainEvents());
        appendFinishedState(builder);

        success(builder.toString());
    }

    public void update() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        gameSession.clearPendingEvents();
        gameSession.updateSession();

        StringBuilder builder = new StringBuilder("Game updated.");
        appendEvents(builder, gameSession.drainEvents());
        appendFinishedState(builder);

        success(builder.toString());
    }

    public void handlePause() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (gameSession.getTickManager().isPaused()) {
            gameSession.getTickManager().resume();
            success("Game resumed.");
        } else {
            gameSession.getTickManager().pause();
            success("Game paused.");
        }
    }

    public void handleUserClick(Position position) {
        showTile(position);
    }

    public void plant(String plantName, Position position) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (plantName == null || plantName.isBlank()) {
            fail("Plant name is required.");
            return;
        }

        if (position == null) {
            fail("Plant position is invalid.");
            return;
        }

        PlantType type = gameSession.getPlantType(plantName);

        if (type == null) {
            fail("Plant does not exist.");
            return;
        }

        if (!gameSession.isPlantSelected(type.getName())) {
            fail("Plant is not selected.");
            return;
        }

        Level level = gameSession.getCurrentLevel();

        if (level != null && !level.isPlantAllowed(type.getName())) {
            fail("Plant is locked or unavailable in this level.");
            return;
        }

        int remainingTicks = gameSession.getPlantRechargeRemainingTicks(type.getName());

        if (remainingTicks > 0) {
            fail("Plant is on cooldown for " + formatSeconds(remainingTicks) + " seconds.");
            return;
        }

        int cost = gameSession.getPlantCost(type.getName());

        if (cost > gameSession.getTotalSunAmount()) {
            fail("Not enough suns.");
            return;
        }

        if (!gameSession.plant(type.getName(), position)) {
            fail("Plant could not be planted at " + position + ".");
            return;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Plant ")
                .append(type.getName())
                .append(" planted at ")
                .append(position)
                .append("; sun amount: ")
                .append(gameSession.getTotalSunAmount())
                .append(".");

        if (isPlantBoostedForThisGame(type.getName())) {
            applyEntranceBoost(position, builder);
        }

        success(builder.toString());
    }

    public void pluck(Position position) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (position == null) {
            fail("Plant position is invalid.");
            return;
        }

        if (!gameSession.pluck(position)) {
            fail("There is no removable plant at " + position + ".");
            return;
        }

        success("Plant at " + position + " was plucked.");
    }

    public void feedPlant(Position position) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (position == null) {
            fail("Plant position is invalid.");
            return;
        }

        if (!gameSession.feedPlant(position)) {
            fail("Plant at " + position + " could not be fed.");
            return;
        }

        success("Plant at " + position
                + " was fed; plant foods: "
                + gameSession.getPlantFoodCount() + ".");
    }

    public void collectSun(Position position) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (position == null) {
            fail("Sun position is invalid.");
            return;
        }

        gameSession.clearPendingEvents();

        if (!gameSession.collectSun(position)) {
            fail("No collectible sun exists at " + position + ".");
            return;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Sun collected at ")
                .append(position)
                .append("; sun amount: ")
                .append(gameSession.getTotalSunAmount())
                .append(".");

        appendEvents(builder, gameSession.drainEvents());
        appendFinishedState(builder);

        success(builder.toString());
    }

    public void startZombieWaves() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (gameSession.getCurrentLevel() == null) {
            fail("Current level is not available.");
            return;
        }

        if (gameSession.getCurrentLevel().areZombieWavesStarted()) {
            success("Zombie waves are already started.");
            return;
        }

        if (!gameSession.startZombieWaves()) {
            fail("Zombie waves cannot be started now.");
            return;
        }

        success("Zombie waves started.");
    }

    public void addSunCheat(int amount) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (amount <= 0) {
            fail("Sun cheat amount must be positive.");
            return;
        }

        gameSession.addSun(amount);

        success(amount + " suns were added; sun amount: "
                + gameSession.getTotalSunAmount() + ".");
    }

    public void removeCooldownCheat() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (!gameSession.removePlantCooldowns()) {
            fail("Cooldowns could not be removed.");
            return;
        }

        success("Cooldowns have been removed.");
    }

    public void addPlantFoodCheat() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (!gameSession.addPlantFood()) {
            fail("Plant food storage is full; plant foods: "
                    + gameSession.getPlantFoodCount() + ".");
            return;
        }

        success("Plant food added; plant foods: "
                + gameSession.getPlantFoodCount() + ".");
    }

    public void spawnZombieCheat(String zombieName, Position position) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (zombieName == null || zombieName.isBlank()) {
            fail("Zombie name is required.");
            return;
        }

        if (position == null) {
            fail("Zombie position is invalid.");
            return;
        }

        if (!gameSession.spawnZombie(zombieName, position)) {
            fail("Zombie could not be spawned at " + position + ".");
            return;
        }

        success("Zombie " + zombieName.trim() + " spawned at " + position + ".");
    }

    public void releaseNuke() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        int destroyed = gameSession.releaseNuke();
        success("Nuke destroyed " + destroyed + " zombies.");
    }

    public void showSun() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Current sun amount: ")
                .append(gameSession.getTotalSunAmount())
                .append("\nSuns:");

        List<Sun> suns = gameSession.getSunManager().getSuns();

        if (suns.isEmpty()) {
            builder.append("\n  none");
        } else {
            for (Sun sun : suns) {
                builder.append("\n  ")
                        .append(sun.getType())
                        .append(" amount=")
                        .append(sun.getSunAmount())
                        .append(" at ")
                        .append(sun.getPosition())
                        .append(" state=");

                if (sun.isFalling()) {
                    builder.append("falling, time-left=")
                            .append(sun.getFallingTicksRemaining())
                            .append(" ticks (")
                            .append(formatSeconds(sun.getFallingTicksRemaining()))
                            .append("s)");
                } else if (sun.isProducedByPlant()) {
                    builder.append("plant-produced, permanent");
                } else {
                    builder.append("ground, permanent");
                }
            }
        }

        success(builder.toString());
    }

    public void showMap() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return;
        }

        Board board = gameSession.getBoard();
        Level level = gameSession.getCurrentLevel();
        int tick = gameSession.getTickManager().getCurrentTick();

        StringBuilder builder = new StringBuilder();

        appendGameHeader(builder, level, tick, board);
        appendSpecialLevelStatus(builder, level);
        appendLawnMowerStatus(builder, board);

        builder.append("\ncolumns:   1    2    3    4    5    6    7    8    9")
                .append("\nLegend: terrain[.=normal,G=grave,W=water,F=ice,L=low-tide,N=necromancy,^/v=slip], ")
                .append("middle=plant initial (+ means stacked), right=zombie count");

        for (Lane lane : board.getLanes()) {
            builder.append("\nrow ").append(lane.getLaneId()).append(" ");

            for (Tile tile : lane.getTiles()) {
                builder.append(formatTile(tile));
            }
        }

        success(builder.toString());
    }

    public void showPlants() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return;
        }

        List<PlantRechargeStatus> statuses = gameSession.getPlantRechargeStatuses();
        StringBuilder builder = new StringBuilder("Plants Status:");

        if (statuses.isEmpty()) {
            builder.append("\n  none");
        } else {
            for (PlantRechargeStatus status : statuses) {
                builder.append("\n")
                        .append(status.getPlantName())
                        .append(" | cost: ")
                        .append(status.getSunCost())
                        .append(" | plantable: ")
                        .append(status.isPlantable())
                        .append(" | boosted: ")
                        .append(isPlantBoostedForThisGame(status.getPlantName()))
                        .append(" | cooldown remaining: ")
                        .append(formatSeconds(status.getRemainingTicks()))
                        .append("s");
            }
        }

        success(builder.toString());
    }

    public void showZombies() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return;
        }

        List<Zombie> zombies = gameSession.getBoard().getAllZombies();
        StringBuilder builder = new StringBuilder("Zombies Info\n============");

        if (zombies.isEmpty()) {
            builder.append("\nNo zombies are on the map.");
        } else {
            for (Zombie zombie : zombies) {
                builder.append("\n")
                        .append(zombie.getName())
                        .append(":")
                        .append("\n  position: ")
                        .append(formatCoordinate(zombie.getX(), zombie.getY()))
                        .append("\n  health: ")
                        .append(zombie.getHp())
                        .append("/")
                        .append(zombie.getMaxHp())
                        .append("\n  speed: ")
                        .append(String.format(Locale.ROOT, "%.3f", zombie.getCurrentSpeed()))
                        .append("\n  damage/tick: ")
                        .append(zombie.getType().getDamagePerTick())
                        .append("\n  glowing: ")
                        .append(zombie.isGlowing())
                        .append("\n  armor: ");

                Armor armor = zombie.getArmor();

                if (armor == null || armor.isBroken()) {
                    builder.append("none");
                } else {
                    builder.append(armor.getName())
                            .append(" type=")
                            .append(armor.getArmorType())
                            .append(" hp=")
                            .append(armor.getHp());
                }
            }
        }

        success(builder.toString());
    }

    public void showTile(Position position) {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return;
        }

        if (position == null) {
            fail("Tile position is invalid.");
            return;
        }

        Tile tile = gameSession.getBoard().getTileAt(position);

        if (tile == null) {
            fail("Tile does not exist.");
            return;
        }

        Lane lane = gameSession.getBoard().getLaneAt(position.getY());
        StringBuilder builder = new StringBuilder();

        builder.append("Tile ")
                .append(position)
                .append("\ntype: ")
                .append(tile.getTileType())
                .append("\nplantable land: ")
                .append(tile.isPlantable())
                .append("\nlawn mower: ")
                .append(lane != null && lane.getLawnMower().isReady() ? "ready" : "used");

        if (tile.hasDamageableTerrain()) {
            builder.append("\nterrain health: ")
                    .append(tile.getTerrainHealth())
                    .append("/")
                    .append(tile.getMaximumTerrainHealth());
        }

        builder.append("\nplants:");

        if (tile.getPlants().isEmpty()) {
            builder.append(" none");
        } else {
            int layer = 1;

            for (Plant plant : tile.getPlants()) {
                builder.append("\n  layer ").append(layer).append(":");
                appendPlantDetails(builder, plant, "    ");
                layer++;
            }
        }

        builder.append("\nzombies:");

        int livingZombies = 0;

        for (Zombie zombie : tile.getZombies()) {
            if (zombie != null && zombie.isAlive()) {
                livingZombies++;
                appendZombieDetails(builder, zombie);
            }
        }

        if (livingZombies == 0) {
            builder.append(" none");
        }

        success(builder.toString());
    }

    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private boolean hasPreparedSession() {
        return gameSession != null && gameSession.getCurrentLevel() != null;
    }

    private boolean isPlantAvailableForCurrentLevel(String plantName) {
        if (plantName == null || gameSession == null) {
            return false;
        }

        Level level = gameSession.getCurrentLevel();
        return level == null || level.isPlantAllowed(plantName);
    }

    private boolean isPlantSelectedForThisLevel(String plantName) {
        if (plantName == null || gameSession == null) {
            return false;
        }

        for (String selectedPlantName : gameSession.getSelectedPlantNames()) {
            if (normalizeName(selectedPlantName).equals(normalizeName(plantName))) {
                return true;
            }
        }

        return false;
    }

    private boolean isPlantBoostedForThisGame(String plantName) {
        return plantName != null && boostedPlantNames.contains(normalizeName(plantName));
    }

    private void appendSelectablePlantLine(StringBuilder builder, PlantType type) {
        if (builder == null || type == null) {
            return;
        }

        boolean available = isPlantAvailableForCurrentLevel(type.getName());
        boolean selected = isPlantSelectedForThisLevel(type.getName());
        boolean boosted = isPlantBoostedForThisGame(type.getName());

        builder.append("\n- ")
                .append(type.getName())
                .append(" | status: ")
                .append(available ? "available" : "locked")
                .append(" | cost: ")
                .append(type.getSunCost())
                .append(" | category: ")
                .append(type.getCategory())
                .append(" | selected: ")
                .append(selected ? "yes" : "no")
                .append(" | boosted: ")
                .append(boosted ? "yes" : "no");
    }

    private void appendSelectedPlants(StringBuilder builder) {
        if (builder == null) {
            return;
        }

        builder.append(selectedPlantsText());
    }

    private String selectedPlantsText() {
        StringBuilder builder = new StringBuilder();
        int selectedCount = gameSession == null ? 0 : gameSession.getSelectedPlantNames().size();

        builder.append("\nSelected Plants (")
                .append(selectedCount)
                .append("/")
                .append(PLANT_SELECTION_LIMIT)
                .append(")")
                .append("\n===============");

        if (gameSession == null || gameSession.getSelectedPlantNames().isEmpty()) {
            builder.append("\nNo plants selected.");
            return builder.toString();
        }

        for (String plantName : gameSession.getSelectedPlantNames()) {
            builder.append("\n- ").append(plantName);

            if (isPlantBoostedForThisGame(plantName)) {
                builder.append(" [boosted]");
            }
        }

        return builder.toString();
    }

    private void applyEntranceBoost(Position position, StringBuilder builder) {
        if (position == null || builder == null) {
            return;
        }

        boolean plantFoodAdded = gameSession.addPlantFood();
        boolean plantFed = gameSession.feedPlant(position);

        if (plantFed) {
            builder.append(" Entrance boost was applied.");
            return;
        }

        if (plantFoodAdded) {
            builder.append(" Entrance boost could not be applied, but one plant food was added.");
            return;
        }

        builder.append(" Entrance boost could not be applied.");
    }

    private Level createDefaultLevel(String chapterName) {
        List<Wave> waves = new ArrayList<>();
        waves.add(new Wave(1, 20, zombies("Default", "Default")));
        waves.add(new Wave(2, 80, zombies("cone head", "Default")));
        waves.add(new Wave(3, 140, zombies("bucket head", "cone head")));

        WaveManager waveManager = new WaveManager(waves, null, AttackPattern.ROUND_ROBIN);
        List<String> allowedPlants = allowedPlantsForChapter(chapterName);
        List<String> allowedZombies = Arrays.asList("Default", "cone head", "bucket head");

        return new Level(
                resolveLevelId(chapterName),
                waveManager,
                LevelType.NORMAL,
                allowedPlants,
                allowedZombies,
                new NoSpecialRule(),
                150
        );
    }

    private List<String> allowedPlantsForChapter(String chapterName) {
        String normalizedChapterName = normalizeName(chapterName);

        if ("ice cave".equals(normalizedChapterName)) {
            return Arrays.asList(
                    "Sunflower",
                    "Peashooter",
                    "Repeater",
                    "Wall-nut",
                    "Potato Mine",
                    "Iceberg Lettuce",
                    "Hot Potato",
                    "Pepper-pult",
                    "Fire Peashooter"
            );
        }

        if ("wave beach".equals(normalizedChapterName)) {
            return Arrays.asList(
                    "Sunflower",
                    "Peashooter",
                    "Wall-nut",
                    "Potato Mine",
                    "Lily Pad",
                    "Tangle Kelp",
                    "Sea-shroom",
                    "Bowling Bulb",
                    "Rotobaga"
            );
        }

        if ("wild west".equals(normalizedChapterName)) {
            return Arrays.asList(
                    "Sunflower",
                    "Peashooter",
                    "Repeater",
                    "Wall-nut",
                    "Potato Mine",
                    "Split Pea",
                    "Pea Pod",
                    "Tall-nut",
                    "Melon-pult"
            );
        }

        return Arrays.asList(
                "Sunflower",
                "Peashooter",
                "Wall-nut",
                "Potato Mine",
                "Cabbage-pult",
                "Kernel-pult",
                "Iceberg Lettuce",
                "Bonk Choy",
                "Cherry Bomb"
        );
    }

    private List<Zombie> zombies(String... names) {
        ZombieFactory zombieFactory = new ZombieFactory();
        List<Zombie> zombies = new ArrayList<>();

        if (names == null) {
            return zombies;
        }

        for (String name : names) {
            zombies.add(zombieFactory.createZombie(name, 9, 1));
        }

        return zombies;
    }

    private int resolveLevelId(String chapterName) {
        String normalizedChapterName = normalizeName(chapterName);

        if ("ice cave".equals(normalizedChapterName)) {
            return 2;
        }

        if ("wave beach".equals(normalizedChapterName)) {
            return 3;
        }

        if ("wild west".equals(normalizedChapterName)) {
            return 4;
        }

        return 1;
    }

    private void appendPlantDetails(StringBuilder builder, Plant plant, String indent) {
        PlantType type = plant.getType();
        String tags = type.getTags();

        builder.append("\n")
                .append(indent)
                .append("name: ")
                .append(plant.getName())
                .append("\n")
                .append(indent)
                .append("category: ")
                .append(type.getCategory())
                .append("\n")
                .append(indent)
                .append("tags: ")
                .append(tags == null || tags.isBlank() ? "none" : tags)
                .append("\n")
                .append(indent)
                .append("health: ")
                .append(plant.getHp())
                .append("/")
                .append(plant.getMaxHp())
                .append("\n")
                .append(indent)
                .append("sun cost: ")
                .append(plant.getCurrentSunCost())
                .append("\n")
                .append(indent)
                .append("damage: ")
                .append(plant.getAttackDamage())
                .append("\n")
                .append(indent)
                .append("action interval: ")
                .append(type.getActionInterval())
                .append(" ticks")
                .append("\n")
                .append(indent)
                .append("seed recharge: ")
                .append(type.getRecharge())
                .append(" ticks")
                .append("\n")
                .append(indent)
                .append("attack cooldown remaining: ")
                .append(plant.getCooldownRemaining())
                .append(" ticks")
                .append("\n")
                .append(indent)
                .append("boosted: ")
                .append(plant.isBoosted());
    }

    private void appendZombieDetails(StringBuilder builder, Zombie zombie) {
        builder.append("\n  name: ")
                .append(zombie.getName())
                .append("\n    position: ")
                .append(formatCoordinate(zombie.getX(), zombie.getY()))
                .append("\n    health: ")
                .append(zombie.getHp())
                .append("/")
                .append(zombie.getMaxHp())
                .append("\n    speed: ")
                .append(String.format(Locale.ROOT, "%.3f", zombie.getCurrentSpeed()))
                .append("\n    damage/tick: ")
                .append(zombie.getType().getDamagePerTick())
                .append("\n    wave cost: ")
                .append(zombie.getType().getWaveCost())
                .append("\n    glowing: ")
                .append(zombie.isGlowing())
                .append("\n    armor: ");

        Armor armor = zombie.getArmor();

        if (armor == null || armor.isBroken()) {
            builder.append("none");
        } else {
            builder.append(armor.getName())
                    .append(" type=")
                    .append(armor.getArmorType())
                    .append(" hp=")
                    .append(armor.getHp());
        }
    }

    private void appendGameHeader(StringBuilder builder, Level level, int tick, Board board) {
        builder.append("tick: ")
                .append(tick)
                .append(" | second: ")
                .append(formatSeconds(tick))
                .append(" | state: ")
                .append(gameSession.getState().getStatus())
                .append(" | paused: ")
                .append(gameSession.getTickManager().isPaused());

        if (level != null) {
            builder.append("\nlevel: ")
                    .append(level.getLevelId())
                    .append(" | type: ")
                    .append(level.getLevelType())
                    .append(" | level-status: ")
                    .append(level.getStatus());

            if (level.getSpecialLevelType() != SpecialLevelType.NONE) {
                builder.append(" | special: ")
                        .append(level.getSpecialLevelType());
            }
        }

        builder.append("\nwave: ")
                .append(currentWaveNumber(level))
                .append("/")
                .append(totalWaves(level))
                .append(" | suns: ")
                .append(gameSession.getTotalSunAmount())
                .append(" | plant foods: ")
                .append(gameSession.getPlantFoodCount())
                .append(" | active zombies: ")
                .append(board.getActiveZombieCount());
    }

    private void appendSpecialLevelStatus(StringBuilder builder, Level level) {
        if (level == null) {
            return;
        }

        LevelRule rule = level.getLevelRule();
        LevelRuntimeContext context = createLevelContext();

        if (rule instanceof TimedWarRule) {
            TimedWarRule timedRule = (TimedWarRule) rule;
            int remainingTicks = timedRule.getRemainingTicks(context);

            builder.append("\ntimed-war: time-left=")
                    .append(remainingTicks)
                    .append(" ticks (")
                    .append(formatSeconds(remainingTicks))
                    .append("s) | objective=")
                    .append(timedRule.getObjective())
                    .append(" | progress=")
                    .append(timedRule.getProgress(context))
                    .append("/")
                    .append(timedRule.getTargetAmount());
            return;
        }

        if (rule instanceof ConveyorBeltRule) {
            builder.append("\nconveyor plants: ")
                    .append(level.getConveyorPlants().isEmpty()
                            ? "none"
                            : String.join(", ", level.getConveyorPlants()));
            return;
        }

        if (rule instanceof LockedPlantsRule) {
            LockedPlantsRule lockedRule = (LockedPlantsRule) rule;

            builder.append("\nplant selection: selected=")
                    .append(lockedRule.getSelectedPlants().size())
                    .append("/")
                    .append(lockedRule.getAvailableSelectionSlotCount())
                    .append(" | locked-slots=")
                    .append(lockedRule.getLockedSelectionSlotCount())
                    .append(" | remaining=")
                    .append(lockedRule.getRemainingSelectionSlotCount())
                    .append(" | selection-closed=")
                    .append(lockedRule.isSelectionLocked());

            if (!lockedRule.getUnavailablePlants().isEmpty()) {
                builder.append(" | unavailable=")
                        .append(String.join(", ", lockedRule.getUnavailablePlants()));
            }
            return;
        }

        if (rule instanceof SaveOurSeedsRule) {
            SaveOurSeedsRule saveRule = (SaveOurSeedsRule) rule;
            builder.append("\nprotected plants: ")
                    .append(saveRule.getProtectedPositions());
            return;
        }

        if (rule instanceof DeadLineRule) {
            DeadLineRule deadLineRule = (DeadLineRule) rule;
            builder.append("\ndead line: x=")
                    .append(deadLineRule.getDeadlineX());
            return;
        }

        if (rule instanceof LoveYourPlantsRule) {
            LoveYourPlantsRule loveRule = (LoveYourPlantsRule) rule;
            builder.append("\nplant losses: ")
                    .append(loveRule.getDestroyedPlantCount(context))
                    .append("/")
                    .append(loveRule.getMaximumPlantLosses());
            return;
        }

        if (rule instanceof NightOpsRule) {
            builder.append("\nnight ops: sky sun is disabled");
        }
    }

    private void appendLawnMowerStatus(StringBuilder builder, Board board) {
        builder.append("\nlawn mowers:");

        for (Lane lane : board.getLanes()) {
            builder.append(" row ")
                    .append(lane.getLaneId())
                    .append("=")
                    .append(lane.getLawnMower().isReady() ? "ready" : "used");
        }
    }

    private void appendEvents(StringBuilder builder, List<GameEvent> events) {
        if (events == null) {
            return;
        }

        for (GameEvent event : events) {
            String eventText = formatEvent(event);

            if (eventText != null && !eventText.isBlank()) {
                builder.append("\n").append(eventText);
            }
        }
    }

    private String formatEvent(GameEvent event) {
        if (event == null || event.getType() == null) {
            return "";
        }

        switch (event.getType()) {
            case WAVE_STARTED:
                return event.isFinalWave()
                        ? "The final wave has come."
                        : "Wave " + event.getWaveNumber() + " started.";

            case ZOMBIE_SPAWNED:
                return "Zombie " + event.getEntityName()
                        + " spawned at wave " + event.getWaveNumber()
                        + " in lane " + event.getLaneNumber()
                        + " which costed " + event.getWaveCost() + ".";

            case ZOMBIE_KILLED:
                if (event.isGroupedByLawnMower()) {
                    return "";
                }

                return "Zombie of type " + event.getEntityName()
                        + " is dead at " + formatCoordinate(event.getX(), event.getY());

            case PLANT_DESTROYED:
                return "Plant " + event.getEntityName()
                        + " at " + formatCoordinate(event.getX(), event.getY())
                        + " is destroyed.";

            case LAWN_MOWER_TRIGGERED:
                return "The lawn mower in the row " + event.getLaneNumber()
                        + " is triggered and killed these zombies: "
                        + (event.getEntityNames().isEmpty()
                        ? "none"
                        : String.join(", ", event.getEntityNames()));

            case PLANT_SUN_PRODUCED:
                return "plant " + event.getEntityName()
                        + " produced a sun at "
                        + formatCoordinate(event.getX(), event.getY());

            case SKY_SUN_DROPPING:
                return "New " + event.getSunType().name().toLowerCase(Locale.ROOT)
                        + " sun is dropping at position "
                        + formatCoordinate(event.getX(), event.getY());

            case SKY_SUN_LANDED:
                return "Sun reached the ground at position "
                        + formatCoordinate(event.getX(), event.getY());

            case RADIOACTIVE_SUN_EXPLODED:
                return "Radioactive sun exploded at "
                        + formatCoordinate(event.getX(), event.getY())
                        + "; zombies killed=" + event.getAmount()
                        + ", plants destroyed=" + event.getSecondaryAmount() + ".";

            case PLANT_FOOD_DROPPED:
                return "The glowing zombie dropped a plant food; you have "
                        + event.getCurrentCount() + " plant foods now.";

            case REWARD_DROPPED:
                return "A zombie dropped a " + event.getEntityName() + ".";

            default:
                return "";
        }
    }

    private void appendFinishedState(StringBuilder builder) {
        GameState state = gameSession.getState();

        if (state == null) {
            return;
        }

        if (state.getStatus() == GameState.Status.WON) {
            builder.append("\nDear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
        } else if (state.getStatus() == GameState.Status.LOST) {
            builder.append("\nThe zombie ate your brain; LOSER!!!");
        }
    }

    private int currentWaveNumber(Level level) {
        if (level == null || level.getWaveManager() == null) {
            return 0;
        }

        return level.getWaveManager().getCurrentWaveNumber();
    }

    private int totalWaves(Level level) {
        if (level == null || level.getWaveManager() == null) {
            return 0;
        }

        return level.getWaveManager().getTotalWaves();
    }

    private LevelRuntimeContext createLevelContext() {
        return new LevelRuntimeContext(
                gameSession.getBoard(),
                gameSession.getTickManager().getCurrentTick(),
                gameSession.getTotalSunAmount(),
                gameSession.getTotalSunProduced(),
                gameSession.getTotalZombiesKilled(),
                gameSession.getTotalPlantsDestroyed()
        );
    }

    private String formatSeconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / TICKS_PER_SECOND);
    }

    private String formatCoordinate(double x, double y) {
        if (Math.abs(x - Math.rint(x)) < 0.000001) {
            return "(" + (int) Math.rint(x) + ", " + (int) Math.rint(y) + ")";
        }

        return "(" + String.format(Locale.ROOT, "%.2f", x)
                + ", " + (int) Math.rint(y) + ")";
    }

    private String formatTile(Tile tile) {
        char terrain = terrainSymbol(tile.getTileType());
        char plant;

        if (tile.getPlantLayerCount() > 1) {
            plant = '+';
        } else if (tile.hasPlant()) {
            plant = Character.toUpperCase(tile.getCurrentPlant().getName().charAt(0));
        } else {
            plant = ' ';
        }

        int zombies = 0;

        for (Zombie zombie : tile.getZombies()) {
            if (zombie != null && zombie.isAlive()) {
                zombies++;
            }
        }

        String zombieText = zombies == 0 ? " " : Integer.toString(zombies);

        return "[" + terrain + plant + zombieText + "]";
    }

    private char terrainSymbol(TileType tileType) {
        switch (tileType) {
            case GRAVE:
                return 'G';
            case WATER:
                return 'W';
            case ICE:
                return 'F';
            case LOW_TIDE:
                return 'L';
            case NECROMANCY:
                return 'N';
            case SLIPPERY_UP:
                return '^';
            case SLIPPERY_DOWN:
                return 'v';
            default:
                return '.';
        }
    }

    private boolean hasRunningSession() {
        return gameSession != null && gameSession.isRunning();
    }

    private boolean hasInitializedSession() {
        return gameSession != null
                && gameSession.getBoard() != null
                && gameSession.getTickManager() != null
                && gameSession.getSunManager() != null;
    }

    private User getLoggedInUserOrFail() {
        if (authController == null || !authController.isLoggedIn()) {
            fail("You must login first.");
            return null;
        }

        User user = authController.getLoggedInUser();
        if (user == null) {
            fail("You must login first.");
            return null;
        }

        return user;
    }

    private void saveUsers() {
        if (authController != null) {
            authController.saveUsers();
        }
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + (message == null ? "Unknown error." : message);
    }
}