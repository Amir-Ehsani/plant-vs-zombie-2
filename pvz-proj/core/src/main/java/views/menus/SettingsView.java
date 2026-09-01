package views.menus;

import controllers.core.MenuManager;
import controllers.features.SettingsController;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsView extends BaseView {
    private static final Pattern CHANGE_DIFFICULTY_PATTERN = Pattern.compile(
            "^menu\\s+settings\\s+change-difficulty\\s+-l\\s+(\\d+)\\s*$");

    private final MenuManager menuManager;
    private final SettingsController settingsController;

    public SettingsView(String viewName, MenuManager menuManager, SettingsController settingsController) {
        super(viewName);
        this.menuManager = menuManager;
        this.settingsController = settingsController;
    }

    @Override
    public void display() {
        menuManager.showSettingsMenuText();
        printControllerMessage(menuManager.getLastMessage());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);

        if (!settingsController.isLoggedIn()) {
            settingsController.invalidCommand("settings menu");
            printControllerMessage(settingsController.getLastMessage());
            menuManager.enterLoginMenu();
            return;
        }

        if (handleMenuCommand(command)) {
            return;
        }

        if (handleChangeDifficulty(command)) {
            return;
        }

        settingsController.invalidCommand("settings menu");
        printControllerMessage(settingsController.getLastMessage());
    }

    private boolean handleMenuCommand(String command) {
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

    private boolean handleChangeDifficulty(String command) {
        Matcher matcher = CHANGE_DIFFICULTY_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        Integer difficulty = parseInteger(matcher.group(1));

        if (difficulty == null) {
            settingsController.invalidCommand("settings menu");
            printControllerMessage(settingsController.getLastMessage());
            return true;
        }

        settingsController.changeDifficulty(difficulty);
        printControllerMessage(settingsController.getLastMessage());
        return true;
    }
}
