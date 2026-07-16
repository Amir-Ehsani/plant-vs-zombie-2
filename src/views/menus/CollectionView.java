package views.menus;

import controllers.core.MenuManager;
import controllers.features.CollectionController;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CollectionView extends BaseView {
    private static final Pattern SHOW_PLANT = Pattern.compile(
            "^menu\\s+collection\\s+show-plant\\s+-p\\s+(.+?)\\s*$"
    );
    private static final Pattern SHOW_ZOMBIE = Pattern.compile(
            "^menu\\s+collection\\s+show-zombie\\s+-z\\s+(.+?)\\s*$"
    );
    private static final Pattern UPGRADE_PLANT = Pattern.compile(
            "^menu\\s+collection\\s+upgrade-plant\\s+-p\\s+(.+?)\\s*$"
    );
    private static final Pattern PURCHASE_PLANT = Pattern.compile(
            "^menu\\s+collection\\s+purchase-plant\\s+-p\\s+(.+?)\\s*$"
    );

    private final MenuManager menuManager;
    private final CollectionController controller;

    public CollectionView(String viewName, MenuManager menuManager, CollectionController controller) {
        super(viewName);
        this.menuManager = menuManager;
        this.controller = controller;
    }

    public CollectionView() {
        this("Collection Menu", null, new CollectionController());
    }

    @Override
    public void display() {
        System.out.print(menuText());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);
        if (!isReady()) {
            printControllerMessage("ERROR: Collection menu is not connected.");
            return;
        }

        if (!controller.isLoggedIn()) {
            printControllerMessage("ERROR: No user is logged in.");
            menuManager.enterLoginMenu();
            return;
        }

        if (handleNavigation(command) || handleListCommands(command) || handleNamedCommands(command)) {
            return;
        }

        controller.invalidCommand("collection menu");
        printControllerMessage(controller.getLastMessage());
    }

    public String menuText() {
        return """
                Collection Menu
                menu collection show-plants
                menu collection show-all-plants
                menu collection show-zombies
                menu collection show-all-zombies
                menu collection show-plant -p <plant_name>
                menu collection show-zombie -z <zombie_name>
                menu collection upgrade-plant -p <plant_name>
                menu collection purchase-plant -p <plant_name>
                menu show current
                menu exit
                """;
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
        return false;
    }

    private boolean handleListCommands(String command) {
        String output = null;
        if ("menu collection show-plants".equals(command)) {
            output = controller.showPlants();
        } else if ("menu collection show-all-plants".equals(command)) {
            output = controller.showAllPlants();
        } else if ("menu collection show-zombies".equals(command)) {
            output = controller.showZombies();
        } else if ("menu collection show-all-zombies".equals(command)) {
            output = controller.showAllZombies();
        }

        if (output == null) {
            return false;
        }
        printResult(output);
        return true;
    }

    private boolean handleNamedCommands(String command) {
        Matcher matcher = SHOW_PLANT.matcher(command);
        if (matcher.matches()) {
            printResult(controller.showPlant(matcher.group(1)));
            return true;
        }

        matcher = SHOW_ZOMBIE.matcher(command);
        if (matcher.matches()) {
            printResult(controller.showZombie(matcher.group(1)));
            return true;
        }

        matcher = UPGRADE_PLANT.matcher(command);
        if (matcher.matches()) {
            controller.upgradePlant(matcher.group(1));
            printControllerMessage(controller.getLastMessage());
            return true;
        }

        matcher = PURCHASE_PLANT.matcher(command);
        if (matcher.matches()) {
            controller.purchasePlant(matcher.group(1));
            printControllerMessage(controller.getLastMessage());
            return true;
        }
        return false;
    }

    private void printResult(String output) {
        if (output != null && !output.isBlank()) {
            System.out.print(output);
        }
        if (!controller.wasSuccessful()) {
            printControllerMessage(controller.getLastMessage());
        }
    }

    private boolean isReady() {
        return menuManager != null && controller != null;
    }
}
