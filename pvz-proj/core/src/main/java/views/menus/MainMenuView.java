package views.menus;

import controllers.core.MenuManager;
import controllers.features.MainMenuController;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainMenuView extends BaseView {
    private static final Pattern MENU_ENTER_PATTERN = Pattern.compile("^menu\\s+enter\\s+(\\S+)\\s*$");

    private final MenuManager menuManager;
    private final MainMenuController mainMenuController;

    public MainMenuView(String viewName, MenuManager menuManager, MainMenuController mainMenuController) {
        super(viewName);
        this.menuManager = menuManager;
        this.mainMenuController = mainMenuController;
    }

    @Override
    public void display() {
        menuManager.showMainMenuText();
        printControllerMessage(menuManager.getLastMessage());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);

        if (!mainMenuController.isLoggedIn()) {
            mainMenuController.invalidCommand("main menu");
            printControllerMessage(mainMenuController.getLastMessage());
            menuManager.enterLoginMenu();
            return;
        }

        if (handleMenuBasics(command)) {
            return;
        }

        mainMenuController.invalidCommand("main menu");
        printControllerMessage(mainMenuController.getLastMessage());
    }

    private boolean handleMenuBasics(String command) {
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

        if ("menu exit program".equals(command)) {
            menuManager.closeProgram();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        if ("menu logout".equals(command)) {
            mainMenuController.logout();
            printControllerMessage(mainMenuController.getLastMessage());
            menuManager.enterRegisterMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        Matcher matcher = MENU_ENTER_PATTERN.matcher(command);

        if (matcher.matches()) {
            menuManager.enterNamedMenu(matcher.group(1));
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        return false;
    }
}