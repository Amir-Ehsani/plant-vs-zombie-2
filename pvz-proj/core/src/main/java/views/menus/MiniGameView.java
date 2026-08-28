package views.menus;

import controllers.core.MenuManager;
import controllers.features.TravelLogController;
import models.engine.board.Position;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniGameView extends BaseView {
    private static final String LOCATION =
            "[<(]\\s*(\\d+)\\s*,\\s*(\\d+)\\s*[>)]";

    private static final Pattern ADVANCE_PATTERN = Pattern.compile(
            "^advance\\s+time\\s+-t\\s+(\\d+)\\s+ticks?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BREAK_VASE_PATTERN = Pattern.compile(
            "^break\\s+vase\\s+-l\\s*" + LOCATION + "\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PLANT_PACKET_PATTERN = Pattern.compile(
            "^plant\\s+packet\\s+-i\\s+(\\d+)\\s+-l\\s*" + LOCATION + "\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LAUNCH_NUT_PATTERN = Pattern.compile(
            "^launch\\s+nut\\s+-t\\s+(.+?)\\s+-l\\s*" + LOCATION + "\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SPAWN_ZOMBIE_PATTERN = Pattern.compile(
            "^spawn\\s+zombie\\s+-t\\s+(.+?)\\s+-l\\s*" + LOCATION + "\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SWAP_MATCH_PATTERN = Pattern.compile(
            "^swap\\s+plants\\s+-a\\s*" + LOCATION + "\\s+-b\\s*" + LOCATION + "\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UPGRADE_MATCH_PATTERN = Pattern.compile(
            "^upgrade\\s+plant\\s+-t\\s+(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PLANT_ZOMBOTANY_PATTERN = Pattern.compile(
            "^plant\\s+-t\\s+(.+?)\\s+-l\\s*" + LOCATION + "\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private final MenuManager menuManager;
    private final TravelLogController controller;

    public MiniGameView(
            String viewName,
            MenuManager menuManager,
            TravelLogController controller
    ) {
        super(viewName);
        this.menuManager = menuManager;
        this.controller = controller;
    }

    @Override
    public void display() {
        if (!controller.hasActiveMiniGame()) {
            printControllerMessage("ERROR: No mini-game is active.");
            menuManager.enterTravelLogMenu();
            return;
        }

        System.out.println(controller.getActiveMiniGameTitle());
        printControllerMessage(controller.showActiveMiniGameStatus());
        printControllerMessage(controller.showActiveMiniGameHelp());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);

        if (!controller.isLoggedIn()) {
            printControllerMessage("ERROR: No user is logged in.");
            menuManager.enterLoginMenu();
            return;
        }

        if (!controller.hasActiveMiniGame()) {
            printControllerMessage("ERROR: No mini-game is active.");
            menuManager.enterTravelLogMenu();
            return;
        }

        if (handleNavigation(command)) {
            return;
        }

        if (handleUniversalCommand(command)) {
            return;
        }

        if (handleVasebreakerCommand(command)) {
            return;
        }

        if (handleBowlingCommand(command)) {
            return;
        }

        if (handleIZombieCommand(command)) {
            return;
        }

        if (handleMatchThreeCommand(command)) {
            return;
        }

        if (handleZombotanyCommand(command)) {
            return;
        }

        controller.invalidCommand("mini-game menu");
        printControllerMessage(controller.getLastMessage());
    }

    private boolean handleNavigation(String command) {
        if ("menu show current".equalsIgnoreCase(command)) {
            menuManager.showCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        if ("menu exit".equalsIgnoreCase(command)
                || "abandon minigame".equalsIgnoreCase(command)) {
            controller.abandonMiniGame();
            printControllerMessage(controller.getLastMessage());
            menuManager.enterTravelLogMenu();
            return true;
        }

        return false;
    }

    private boolean handleUniversalCommand(String command) {
        if ("show map".equalsIgnoreCase(command)) {
            printControllerMessage(controller.showActiveMiniGameMap());
            return true;
        }

        if ("show status".equalsIgnoreCase(command)) {
            printControllerMessage(controller.showActiveMiniGameStatus());
            return true;
        }

        if ("show help".equalsIgnoreCase(command)) {
            printControllerMessage(controller.showActiveMiniGameHelp());
            return true;
        }

        Matcher matcher = ADVANCE_PATTERN.matcher(command);
        if (!matcher.matches()) {
            return false;
        }

        Integer ticks = parseInteger(matcher.group(1));
        if (ticks == null) {
            controller.invalidCommand("mini-game menu");
        } else {
            controller.advanceMiniGameTime(ticks);
        }

        printControllerMessage(controller.getLastMessage());
        return true;
    }

    private boolean handleVasebreakerCommand(String command) {
        if ("show packets".equalsIgnoreCase(command)) {
            printControllerMessage(controller.showVasebreakerPackets());
            return true;
        }

        Matcher breakMatcher = BREAK_VASE_PATTERN.matcher(command);
        if (breakMatcher.matches()) {
            Position position = positionFromGroups(breakMatcher, 1, 2);
            controller.breakVase(position);
            printControllerMessage(controller.getLastMessage());
            return true;
        }

        Matcher plantMatcher = PLANT_PACKET_PATTERN.matcher(command);
        if (!plantMatcher.matches()) {
            return false;
        }

        Integer packetId = parseInteger(plantMatcher.group(1));
        Position position = positionFromGroups(plantMatcher, 2, 3);

        if (packetId == null || position == null) {
            controller.invalidCommand("mini-game menu");
        } else {
            controller.plantVasebreakerPacket(packetId, position);
        }

        printControllerMessage(controller.getLastMessage());
        return true;
    }

    private boolean handleBowlingCommand(String command) {
        if ("show nuts".equalsIgnoreCase(command)) {
            printControllerMessage(controller.showBowlingNuts());
            return true;
        }

        Matcher matcher = LAUNCH_NUT_PATTERN.matcher(command);
        if (!matcher.matches()) {
            return false;
        }

        String nutType = matcher.group(1).trim();
        Position position = positionFromGroups(matcher, 2, 3);

        if (position == null) {
            controller.invalidCommand("mini-game menu");
        } else {
            controller.launchNut(nutType, position);
        }

        printControllerMessage(controller.getLastMessage());
        return true;
    }

    private boolean handleIZombieCommand(String command) {
        if ("show zombies".equalsIgnoreCase(command)) {
            printControllerMessage(controller.showIZombieOptions());
            return true;
        }

        if ("show sun".equalsIgnoreCase(command)) {
            printControllerMessage(controller.showIZombieSun());
            return true;
        }

        Matcher matcher = SPAWN_ZOMBIE_PATTERN.matcher(command);
        if (!matcher.matches()) {
            return false;
        }

        String zombieType = matcher.group(1).trim();
        Position position = positionFromGroups(matcher, 2, 3);

        if (position == null) {
            controller.invalidCommand("mini-game menu");
        } else {
            controller.spawnIZombie(zombieType, position);
        }

        printControllerMessage(controller.getLastMessage());
        return true;
    }

    private boolean handleMatchThreeCommand(String command) {
        Matcher swapMatcher = SWAP_MATCH_PATTERN.matcher(command);
        if (swapMatcher.matches()) {
            Position first = positionFromGroups(swapMatcher, 1, 2);
            Position second = positionFromGroups(swapMatcher, 3, 4);
            if (first == null || second == null) {
                controller.invalidCommand("mini-game menu");
            } else {
                controller.swapMatchThreePlants(first, second);
            }
            printControllerMessage(controller.getLastMessage());
            return true;
        }

        Matcher upgradeMatcher = UPGRADE_MATCH_PATTERN.matcher(command);
        if (!upgradeMatcher.matches()) {
            return false;
        }
        controller.upgradeMatchThreePlant(upgradeMatcher.group(1).trim());
        printControllerMessage(controller.getLastMessage());
        return true;
    }

    private boolean handleZombotanyCommand(String command) {
        if ("show plants".equalsIgnoreCase(command)) {
            printControllerMessage(controller.showZombotanyPlants());
            return true;
        }

        Matcher matcher = PLANT_ZOMBOTANY_PATTERN.matcher(command);
        if (!matcher.matches()) {
            return false;
        }
        String plantType = matcher.group(1).trim();
        Position position = positionFromGroups(matcher, 2, 3);
        if (position == null) {
            controller.invalidCommand("mini-game menu");
        } else {
            controller.plantZombotany(plantType, position);
        }
        printControllerMessage(controller.getLastMessage());
        return true;
    }

    private Position positionFromGroups(Matcher matcher, int xGroup, int yGroup) {
        Integer x = parseInteger(matcher.group(xGroup));
        Integer y = parseInteger(matcher.group(yGroup));

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
