package views.menus;

import controllers.core.GameController;
import controllers.core.MenuManager;
import models.engine.board.Position;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameView extends BaseView {
    private static final Pattern ADD_PLANT_PATTERN = Pattern.compile("^add\\s+plant\\s+-t\\s+(.+?)\\s*$");
    private static final Pattern REMOVE_PLANT_PATTERN = Pattern.compile("^remove\\s+plant\\s+-t\\s+(.+?)\\s*$");
    private static final Pattern BOOST_PLANT_PATTERN = Pattern.compile("^boost\\s+plant\\s+-t\\s+(.+?)\\s*$");
    private static final Pattern ADVANCE_TIME_PATTERN = Pattern.compile("^advance\\s+time\\s+-t\\s+(\\d+)\\s*$");
    private static final Pattern CHEAT_ADD_SUN_PATTERN = Pattern.compile("^cheat\\s+add\\s+sun\\s+-a\\s+(\\d+)\\s*$");
    private static final Pattern SHOW_TILE_PATTERN = Pattern.compile("^show\\s+tile\\s+-l\\s+<?\\s*(\\d+)\\s*,\\s*(\\d+)\\s*>?\\s*$");
    private static final Pattern PLANT_PATTERN = Pattern.compile("^plant\\s+-t\\s+(.+?)\\s+-l\\s+<?\\s*(\\d+)\\s*,\\s*(\\d+)\\s*>?\\s*$");
    private static final Pattern PLUCK_PATTERN = Pattern.compile("^pluck\\s+-l\\s+<?\\s*(\\d+)\\s*,\\s*(\\d+)\\s*>?\\s*$");
    private static final Pattern FEED_PLANT_PATTERN = Pattern.compile("^feed\\s+plant\\s+-l\\s+<?\\s*(\\d+)\\s*,\\s*(\\d+)\\s*>?\\s*$");
    private static final Pattern COLLECT_SUN_PATTERN = Pattern.compile("^collect\\s+sun\\s+-l\\s+<?\\s*(\\d+)\\s*,\\s*(\\d+)\\s*>?\\s*$");
    private static final Pattern SPAWN_ZOMBIE_PATTERN = Pattern.compile("^cheat\\s+spawn-zombie\\s+-t\\s+(.+?)\\s+-l\\s+<?\\s*(\\d+)\\s*,\\s*(\\d+)\\s*>?\\s*$");

    private final MenuManager menuManager;
    private final GameController gameController;

    public GameView(String viewName, MenuManager menuManager, GameController gameController) {
        super(viewName);
        this.menuManager = menuManager;
        this.gameController = gameController;
    }

    @Override
    public void display() {
        menuManager.showGamePlayMenuText();
        printControllerMessage(menuManager.getLastMessage());
        printControllerMessage(gameController.getLastMessage());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);

        if (handleNavigation(command)) return;
        if (handlePlantSelectionCommands(command)) return;
        if (handleSimpleCommands(command)) return;
        if (handleAdvanceTime(command)) return;
        if (handleLocationCommands(command)) return;
        if (handleCheatCommands(command)) return;

        gameController.invalidCommand("game play menu");
        printControllerMessage(gameController.getLastMessage());
    }

    private boolean handleNavigation(String command) {
        if ("menu show current".equals(command)) {
            menuManager.showCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        if ("menu exit".equals(command)) {
            menuManager.exitCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        if ("return to level menu".equals(command)) {
            if (!gameController.isGameFinished()) {
                gameController.returnToLevelMenuRejected();
                printControllerMessage(gameController.getLastMessage());
                return true;
            }

            menuManager.enterChapterLevelMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        return false;
    }

    private boolean handlePlantSelectionCommands(String command) {
        if ("show all plants".equals(command)) {
            gameController.showAllPlants();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        if ("show available plants".equals(command)) {
            gameController.showAvailablePlants();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        Matcher matcher = ADD_PLANT_PATTERN.matcher(command);
        if (matcher.matches()) {
            gameController.addPlantToSelection(matcher.group(1));
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        matcher = REMOVE_PLANT_PATTERN.matcher(command);
        if (matcher.matches()) {
            gameController.removePlantFromSelection(matcher.group(1));
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        matcher = BOOST_PLANT_PATTERN.matcher(command);
        if (matcher.matches()) {
            gameController.boostPlant(matcher.group(1));
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        return false;
    }

    private boolean handleSimpleCommands(String command) {
        if ("start game".equals(command)) {
            gameController.startGame();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        if ("pause game".equals(command)) {
            gameController.handlePause();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        if ("update game".equals(command)) {
            gameController.update();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        if ("show map".equals(command)) {
            gameController.showMap();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        if ("show sun".equals(command)) {
            gameController.showSun();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        if ("show plants".equals(command)) {
            gameController.showPlants();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        if ("zombies info".equals(command)) {
            gameController.showZombies();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        if ("start zombie waves".equals(command)) {
            gameController.startZombieWaves();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        return false;
    }

    private boolean handleAdvanceTime(String command) {
        Matcher matcher = ADVANCE_TIME_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        Integer ticks = parseInteger(matcher.group(1));

        if (ticks == null) {
            gameController.invalidCommand("game play menu");
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        gameController.advanceTime(ticks);
        printControllerMessage(gameController.getLastMessage());
        return true;
    }

    private boolean handleLocationCommands(String command) {
        Matcher matcher = SHOW_TILE_PATTERN.matcher(command);

        if (matcher.matches()) {
            gameController.showTile(positionFrom(matcher.group(1), matcher.group(2)));
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        matcher = PLANT_PATTERN.matcher(command);
        if (matcher.matches()) {
            gameController.plant(matcher.group(1), positionFrom(matcher.group(2), matcher.group(3)));
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        matcher = PLUCK_PATTERN.matcher(command);
        if (matcher.matches()) {
            gameController.pluck(positionFrom(matcher.group(1), matcher.group(2)));
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        matcher = FEED_PLANT_PATTERN.matcher(command);
        if (matcher.matches()) {
            gameController.feedPlant(positionFrom(matcher.group(1), matcher.group(2)));
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        matcher = COLLECT_SUN_PATTERN.matcher(command);
        if (matcher.matches()) {
            gameController.collectSun(positionFrom(matcher.group(1), matcher.group(2)));
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        return false;
    }

    private boolean handleCheatCommands(String command) {
        Matcher matcher = CHEAT_ADD_SUN_PATTERN.matcher(command);

        if (matcher.matches()) {
            Integer amount = parseInteger(matcher.group(1));

            if (amount == null) {
                gameController.invalidCommand("game play menu");
                printControllerMessage(gameController.getLastMessage());
                return true;
            }

            gameController.addSunCheat(amount);
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        if ("cheat remove cooldown".equals(command)) {
            gameController.removeCooldownCheat();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        if ("cheat add plant-food".equals(command)) {
            gameController.addPlantFoodCheat();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        matcher = SPAWN_ZOMBIE_PATTERN.matcher(command);
        if (matcher.matches()) {
            gameController.spawnZombieCheat(matcher.group(1), positionFrom(matcher.group(2), matcher.group(3)));
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        if ("cheat nuke".equals(command)) {
            gameController.releaseNuke();
            printControllerMessage(gameController.getLastMessage());
            return true;
        }

        return false;
    }

    private Position positionFrom(String xText, String yText) {
        Integer x = parseInteger(xText);
        Integer y = parseInteger(yText);

        if (x == null || y == null) {
            return null;
        }

        try {
            return new Position(x, y);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
