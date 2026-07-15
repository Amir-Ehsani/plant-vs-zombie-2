package controllers.core;

import models.core.plant.Plant;
import models.core.zombie.Zombie;
import models.engine.board.Board;
import models.engine.session.GameSession;
import models.engine.session.GameState;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.sun.Sun;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.level.rules.impl.ConveyorBeltRule;
import models.level.rules.impl.DeadLineRule;
import models.level.core.Level;
import models.level.rules.LevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.impl.LockedPlantsRule;
import models.level.rules.impl.LoveYourPlantsRule;
import models.level.rules.impl.NightOpsRule;
import models.level.rules.impl.SaveOurSeedsRule;
import models.level.rules.impl.TimedWarRule;
import models.level.wave.WaveManager;

import java.util.List;
import java.util.Locale;

public class GameController {
    private static final double TICKS_PER_SECOND = 10.0;

    private GameSession gameSession;
    private String lastMessage;

    public GameController() {
        this.lastMessage = "";
    }

    public void setGameSession(GameSession gameSession) {
        if (gameSession == null) {
            throw new IllegalArgumentException("Game session cannot be null.");
        }
        this.gameSession = gameSession;
    }

    public boolean startGame() {
        if (gameSession.isRunning()) {
            return fail("Game is already running.");
        }

        try {
            gameSession.initSession();
            return success("Game started.");
        } catch (IllegalStateException | IllegalArgumentException exception) {
            return fail(exception.getMessage());
        }
    }

    public boolean advanceTime(int ticks) {
        if (ticks <= 0) {
            return fail("Tick count must be positive.");
        }

        Level level = gameSession.getCurrentLevel();
        int previousWave = currentWaveNumber(level);

        if (!gameSession.advanceTicks(ticks)) {
            return fail("Time could not be advanced.");
        }

        StringBuilder message = new StringBuilder();
        message.append("Advanced ").append(ticks).append(" ticks.");

        int currentWave = currentWaveNumber(level);
        if (currentWave > previousWave) {
            if (currentWave == totalWaves(level)) {
                message.append(" The final wave has come.");
            } else {
                message.append(" Wave ").append(currentWave).append(" started.");
            }
        }

        appendFinishedState(message);
        return success(message.toString());
    }

    public boolean plant(String plantName, Position position) {
        if (!hasRunningSession()) {
            return fail("No running game is available.");
        }
        if (plantName == null || plantName.isBlank() || position == null) {
            return fail("Plant name and position are required.");
        }
        if (!gameSession.plant(plantName, position)) {
            return fail("Plant could not be planted at " + position + ".");
        }

        return success(
                "Plant " + plantName.trim() + " planted at " + position
                        + "; sun amount: " + gameSession.getTotalSunAmount() + "."
        );
    }

    public boolean pluck(Position position) {
        if (!hasRunningSession()) {
            return fail("No running game is available.");
        }
        if (position == null || !gameSession.pluck(position)) {
            return fail("There is no removable plant at " + position + ".");
        }
        return success("Plant at " + position + " was plucked.");
    }

    public boolean feedPlant(Position position) {
        if (!hasRunningSession()) {
            return fail("No running game is available.");
        }
        if (position == null || !gameSession.feedPlant(position)) {
            return fail("Plant at " + position + " could not be fed.");
        }

        return success(
                "Plant at " + position + " was fed; plant foods: "
                        + gameSession.getPlantFoodCount() + "."
        );
    }

    public boolean collectSun(Position position) {
        if (!hasRunningSession()) {
            return fail("No running game is available.");
        }
        if (position == null || !gameSession.collectSun(position)) {
            return fail("No collectible sun exists at " + position + ".");
        }

        return success(
                "Sun collected; sun amount: "
                        + gameSession.getTotalSunAmount() + "."
        );
    }

    public boolean startZombieWaves() {
        if (!hasRunningSession() || !gameSession.startZombieWaves()) {
            return fail("Zombie waves cannot be started now.");
        }
        return success("Zombie waves started.");
    }

    public boolean addSunCheat(int amount) {
        if (!hasRunningSession() || amount <= 0) {
            return fail("Sun cheat amount must be positive.");
        }

        gameSession.addSun(amount);
        return success(
                amount + " suns were added; sun amount: "
                        + gameSession.getTotalSunAmount() + "."
        );
    }

    public boolean addPlantFoodCheat() {
        if (!hasRunningSession() || !gameSession.addPlantFood()) {
            return fail("Plant food could not be added.");
        }

        return success(
                "Plant food added; plant foods: "
                        + gameSession.getPlantFoodCount() + "."
        );
    }

    public boolean spawnZombieCheat(String zombieName, Position position) {
        if (!hasRunningSession()) {
            return fail("No running game is available.");
        }
        if (zombieName == null || zombieName.isBlank() || position == null) {
            return fail("Zombie name and position are required.");
        }
        if (!gameSession.spawnZombie(zombieName, position)) {
            return fail("Zombie could not be spawned at " + position + ".");
        }

        return success(
                "Zombie " + zombieName.trim() + " spawned at " + position + "."
        );
    }

    public boolean releaseNuke() {
        if (!hasRunningSession()) {
            return fail("No running game is available.");
        }

        int destroyed = gameSession.releaseNuke();
        return success("Nuke destroyed " + destroyed + " zombies.");
    }

    public void handleUserClick(Position position) {
        showTile(position);
    }

    public void handlePause() {
        if (gameSession == null || gameSession.getTickManager() == null) {
            fail("Game is not available.");
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

    public void update() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        gameSession.updateSession();
        success("Game updated.");
    }

    public String showSun() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return lastMessage;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("current sun amount: ")
                .append(gameSession.getTotalSunAmount())
                .append("\nsuns:");

        List<Sun> suns = gameSession.getSunManager().getSuns();
        if (suns.isEmpty()) {
            builder.append("\n  none");
        } else {
            for (Sun sun : suns) {
                builder.append("\n  ")
                        .append(sun.getSunAmount())
                        .append(" at ")
                        .append(sun.getPosition())
                        .append(" time-left=");

                if (sun.getTimeLeft() == Integer.MAX_VALUE) {
                    builder.append("permanent");
                } else {
                    builder.append(sun.getTimeLeft());
                }
            }
        }

        success(builder.toString());
        return lastMessage;
    }

    public String showMap() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return lastMessage;
        }

        Board board = gameSession.getBoard();
        Level level = gameSession.getCurrentLevel();
        int tick = gameSession.getTickManager().getCurrentTick();
        StringBuilder builder = new StringBuilder();

        appendGameHeader(builder, level, tick, board);
        appendSpecialLevelStatus(builder, level);
        appendLawnMowerStatus(builder, board);

        builder.append("\nLegend: terrain[.=normal,G=grave,W=water,F=ice,L=low-tide,N=necromancy,^/v=slip]");
        for (Lane lane : board.getLanes()) {
            builder.append("\nrow ").append(lane.getLaneId()).append(" ");
            for (Tile tile : lane.getTiles()) {
                builder.append(formatTile(tile));
            }
        }

        success(builder.toString());
        return lastMessage;
    }

    public String showPlants() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return lastMessage;
        }

        List<Plant> plants = gameSession.getBoard().getAllPlants();
        StringBuilder builder = new StringBuilder("plants:");

        if (plants.isEmpty()) {
            builder.append("\n  none");
        } else {
            for (Plant plant : plants) {
                builder.append("\n  ")
                        .append(plant.getName())
                        .append(" at (")
                        .append((int) plant.getX())
                        .append(", ")
                        .append((int) plant.getY())
                        .append(") hp=")
                        .append(plant.getHp())
                        .append("/")
                        .append(plant.getMaxHp())
                        .append(" cooldown=")
                        .append(plant.getCooldownRemaining());
            }
        }

        success(builder.toString());
        return lastMessage;
    }

    public String showZombies() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return lastMessage;
        }

        List<Zombie> zombies = gameSession.getBoard().getAllZombies();
        StringBuilder builder = new StringBuilder("zombies:");

        if (zombies.isEmpty()) {
            builder.append("\n  none");
        } else {
            for (Zombie zombie : zombies) {
                builder.append("\n  ")
                        .append(zombie.getName())
                        .append(" position=")
                        .append(String.format(Locale.ROOT, "%.2f", zombie.getX()))
                        .append(",")
                        .append((int) zombie.getY())
                        .append(" hp=")
                        .append(zombie.getHp())
                        .append("/")
                        .append(zombie.getMaxHp());
            }
        }

        success(builder.toString());
        return lastMessage;
    }

    public String showTile(Position position) {
        if (!hasInitializedSession() || position == null) {
            fail("Tile position is invalid.");
            return lastMessage;
        }

        Tile tile = gameSession.getBoard().getTileAt(position);
        if (tile == null) {
            fail("Tile does not exist.");
            return lastMessage;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("tile ").append(position)
                .append("\ntype: ").append(tile.getTileType())
                .append("\nplant: ");

        Plant plant = tile.getCurrentPlant();
        if (plant == null) {
            builder.append("none");
        } else {
            builder.append(plant.getName())
                    .append(" hp=")
                    .append(plant.getHp())
                    .append("/")
                    .append(plant.getMaxHp());
        }

        builder.append("\nzombies:");
        int livingZombies = 0;
        for (Zombie zombie : tile.getZombies()) {
            if (zombie != null && zombie.isAlive()) {
                livingZombies++;
                builder.append("\n  ")
                        .append(zombie.getName())
                        .append(" hp=")
                        .append(zombie.getHp())
                        .append("/")
                        .append(zombie.getMaxHp());
            }
        }

        if (livingZombies == 0) {
            builder.append(" none");
        }

        success(builder.toString());
        return lastMessage;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private void appendGameHeader(
            StringBuilder builder,
            Level level,
            int tick,
            Board board
    ) {
        builder.append("tick: ").append(tick)
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

            if (level.getSpecialLevelType() != null) {
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
                    .append(lockedRule.getRemainingSelectionSlotCount());

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

    private String formatTile(Tile tile) {
        char terrain = terrainSymbol(tile.getTileType());
        char plant = tile.hasPlant()
                ? Character.toUpperCase(tile.getCurrentPlant().getName().charAt(0))
                : ' ';

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

    private void appendFinishedState(StringBuilder message) {
        GameState state = gameSession.getState();
        if (state == null) {
            return;
        }

        if (state.getStatus() == GameState.Status.WON) {
            message.append(" Level won.");
        } else if (state.getStatus() == GameState.Status.LOST) {
            message.append(" The zombie ate your brain; LOSER!");
        }
    }

    private int currentWaveNumber(Level level) {
        if (level == null) {
            return 0;
        }

        WaveManager manager = level.getWaveManager();
        return manager == null ? 0 : manager.getCurrentWaveNumber();
    }

    private int totalWaves(Level level) {
        if (level == null || level.getWaveManager() == null) {
            return 0;
        }
        return level.getWaveManager().getTotalWaves();
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

    private boolean success(String message) {
        lastMessage = "OK: " + message;
        return true;
    }

    private boolean fail(String message) {
        lastMessage = "ERROR: " + (message == null ? "Unknown error." : message);
        return false;
    }
}
