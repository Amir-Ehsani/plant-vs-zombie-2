package controllers.core;

import models.core.plant.Plant;
import models.core.plant.PlantType;
import models.core.zombie.Armor;
import models.core.zombie.Zombie;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.engine.events.GameEvent;
import models.engine.events.GameEventType;
import models.engine.session.GameSession;
import models.engine.session.GameState;
import models.engine.session.PlantRechargeStatus;
import models.engine.sun.Sun;
import models.level.core.Level;
import models.level.rules.LevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.SpecialLevelType;
import models.level.rules.impl.ConveyorBeltRule;
import models.level.rules.impl.DeadLineRule;
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
        if (gameSession == null) {
            return fail("Game session is not available.");
        }
        if (gameSession.isRunning()) {
            return fail("Game is already running.");
        }

        try {
            gameSession.initSession();
            StringBuilder message = new StringBuilder("Game started.");
            appendEvents(message, gameSession.drainEvents());
            appendFinishedState(message);
            return success(message.toString());
        } catch (IllegalStateException | IllegalArgumentException exception) {
            return fail(exception.getMessage());
        }
    }

    public boolean advanceTime(int ticks) {
        if (!hasRunningSession()) {
            return fail("No running game is available.");
        }
        if (ticks <= 0) {
            return fail("Tick count must be positive.");
        }

        gameSession.clearPendingEvents();
        if (!gameSession.advanceTicks(ticks)) {
            return fail("Time could not be advanced.");
        }

        StringBuilder message = new StringBuilder();
        message.append("Advanced ")
                .append(gameSession.getLastAdvancedTickCount())
                .append(" ticks. Current tick=")
                .append(gameSession.getTickManager().getCurrentTick());

        appendEvents(message, gameSession.drainEvents());
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

        PlantType type = gameSession.getPlantType(plantName);
        if (type == null) {
            return fail("Plant does not exist.");
        }
        if (!gameSession.isPlantSelected(plantName)) {
            return fail("Plant is not selected.");
        }

        Level level = gameSession.getCurrentLevel();
        if (level != null && !level.isPlantAllowed(plantName)) {
            return fail("Plant is locked or unavailable in this level.");
        }

        int remainingTicks = gameSession.getPlantRechargeRemainingTicks(plantName);
        if (remainingTicks > 0) {
            return fail(
                    "Plant is on cooldown for "
                            + formatSeconds(remainingTicks)
                            + " seconds."
            );
        }

        int cost = gameSession.getPlantCost(plantName);
        if (cost > gameSession.getTotalSunAmount()) {
            return fail("Not enough suns.");
        }

        if (!gameSession.plant(plantName, position)) {
            return fail("Plant could not be planted at " + position + ".");
        }

        return success(
                "Plant " + type.getName() + " planted at " + position
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
        if (position == null) {
            return fail("Sun position is required.");
        }

        gameSession.clearPendingEvents();
        if (!gameSession.collectSun(position)) {
            return fail("No collectible sun exists at " + position + ".");
        }

        StringBuilder message = new StringBuilder();
        message.append("Sun collected at ")
                .append(position)
                .append("; sun amount: ")
                .append(gameSession.getTotalSunAmount())
                .append(".");
        appendEvents(message, gameSession.drainEvents());
        appendFinishedState(message);
        return success(message.toString());
    }

    public boolean startZombieWaves() {
        if (!hasRunningSession() || !gameSession.startZombieWaves()) {
            return fail("Zombie waves cannot be started now.");
        }
        return success("Zombie waves started.");
    }

    public boolean addSunCheat(int amount) {
        if (!hasRunningSession()) {
            return fail("No running game is available.");
        }
        if (amount <= 0) {
            return fail("Sun cheat amount must be positive.");
        }

        gameSession.addSun(amount);
        return success(
                amount + " suns were added; sun amount: "
                        + gameSession.getTotalSunAmount() + "."
        );
    }

    public boolean removeCooldownCheat() {
        if (!hasRunningSession() || !gameSession.removePlantCooldowns()) {
            return fail("Cooldowns could not be removed.");
        }
        return success("Cooldowns have been removed.");
    }

    public boolean addPlantFoodCheat() {
        if (!hasRunningSession()) {
            return fail("No running game is available.");
        }
        if (!gameSession.addPlantFood()) {
            return fail(
                    "Plant food storage is full; plant foods: "
                            + gameSession.getPlantFoodCount() + "."
            );
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

    public void update() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        gameSession.clearPendingEvents();
        gameSession.updateSession();
        StringBuilder message = new StringBuilder("Game updated.");
        appendEvents(message, gameSession.drainEvents());
        appendFinishedState(message);
        success(message.toString());
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
        return lastMessage;
    }

    public String showPlants() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return lastMessage;
        }

        List<PlantRechargeStatus> statuses = gameSession.getPlantRechargeStatuses();
        StringBuilder builder = new StringBuilder("plants status:");

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
                        .append(" | cooldown remaining: ")
                        .append(formatSeconds(status.getRemainingTicks()))
                        .append("s");
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
                        .append(formatCoordinate(zombie.getX(), zombie.getY()))
                        .append(" hp=")
                        .append(zombie.getHp())
                        .append("/")
                        .append(zombie.getMaxHp())
                        .append(" speed=")
                        .append(String.format(Locale.ROOT, "%.3f", zombie.getCurrentSpeed()))
                        .append(" damage/tick=")
                        .append(zombie.getType().getDamagePerTick())
                        .append(" glowing=")
                        .append(zombie.isGlowing());
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

        Lane lane = gameSession.getBoard().getLaneAt(position.getY());
        StringBuilder builder = new StringBuilder();
        builder.append("tile ").append(position)
                .append("\ntype: ").append(tile.getTileType())
                .append("\nplantable land: ").append(tile.isPlantable())
                .append("\nlawn mower: ")
                .append(lane != null && lane.getLawnMower().isReady() ? "ready" : "used");

        if (tile.hasDamageableTerrain()) {
            builder.append("\nterrain health: ")
                    .append(tile.getTerrainHealth())
                    .append("/")
                    .append(tile.getMaximumTerrainHealth());
        }

        builder.append("\nplants (bottom to top):");
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
        return lastMessage;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private void appendPlantDetails(StringBuilder builder, Plant plant, String indent) {
        PlantType type = plant.getType();
        builder.append("\n").append(indent).append("name: ").append(plant.getName())
                .append("\n").append(indent).append("category: ").append(type.getCategory())
                .append("\n").append(indent).append("tags: ")
                .append(type.getTags().isBlank() ? "none" : type.getTags())
                .append("\n").append(indent).append("health: ")
                .append(plant.getHp()).append("/").append(plant.getMaxHp())
                .append("\n").append(indent).append("sun cost: ").append(plant.getCurrentSunCost())
                .append("\n").append(indent).append("damage: ").append(plant.getAttackDamage())
                .append("\n").append(indent).append("action interval: ")
                .append(type.getActionInterval()).append(" ticks")
                .append("\n").append(indent).append("seed recharge: ")
                .append(type.getRecharge()).append(" ticks")
                .append("\n").append(indent).append("attack cooldown remaining: ")
                .append(plant.getCooldownRemaining()).append(" ticks")
                .append("\n").append(indent).append("boosted: ").append(plant.isBoosted());
    }

    private void appendZombieDetails(StringBuilder builder, Zombie zombie) {
        builder.append("\n  name: ").append(zombie.getName())
                .append("\n    position: ").append(formatCoordinate(zombie.getX(), zombie.getY()))
                .append("\n    health: ").append(zombie.getHp()).append("/").append(zombie.getMaxHp())
                .append("\n    speed: ").append(String.format(Locale.ROOT, "%.3f", zombie.getCurrentSpeed()))
                .append("\n    damage/tick: ").append(zombie.getType().getDamagePerTick())
                .append("\n    wave cost: ").append(zombie.getType().getWaveCost())
                .append("\n    glowing: ").append(zombie.isGlowing())
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
        for (GameEvent event : events) {
            String eventText = formatEvent(event);
            if (eventText != null && !eventText.isBlank()) {
                builder.append("\n").append(eventText);
            }
        }
    }

    private String formatEvent(GameEvent event) {
        if (event == null) {
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

    private void appendFinishedState(StringBuilder message) {
        GameState state = gameSession.getState();
        if (state == null) {
            return;
        }

        if (state.getStatus() == GameState.Status.WON) {
            message.append("\nDear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
        } else if (state.getStatus() == GameState.Status.LOST) {
            message.append("\nThe zombie ate your brain; LOSER!!!");
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
