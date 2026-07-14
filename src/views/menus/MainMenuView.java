package views.menus;

import controllers.auth.AuthController;
import controllers.core.MenuManager;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainMenuView extends BaseView {
    private static final Pattern MENU_ENTER_PATTERN = Pattern.compile("^menu\\s+enter\\s+(\\S+)\\s*$");
    private static final Pattern MENU_ENTER_CHAPTER_PATTERN = Pattern.compile(
            "^menu\\s+enter\\s+chapter\\s+-c\\s+(.+)\\s*$"
    );
    private static final Pattern CHAPTER_ENTER_PATTERN = Pattern.compile("^chapter\\s+enter\\s+-c\\s+(.+)\\s*$");
    private static final Pattern CHANGE_DIFFICULTY_PATTERN = Pattern.compile(
            "^menu\\s+settings\\s+change-difficulty\\s+-l\\s+(\\d+)\\s*$"
    );
    private static final Pattern CHEAT_CURRENCY_PATTERN = Pattern.compile(
            "^menu\\s+cheat\\s+add\\s+(\\d+)\\s+(coin|coins|diamond|diamonds|gem|gems)\\s*$"
    );

    private final MenuManager menuManager;
    private final AuthController authController;

    public MainMenuView(String viewName) {
        super(viewName);
        this.menuManager = null;
        this.authController = null;
    }

    public MainMenuView(String viewName, MenuManager menuManager, AuthController authController) {
        super(viewName);
        this.menuManager = menuManager;
        this.authController = authController;
    }

    @Override
    public void display() {
        if (menuManager == null) {
            return;
        }

        menuManager.showMainMenuText();
        printControllerMessage(menuManager.getLastMessage());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);

        if (!authController.isLoggedIn()) {
            authController.invalidCommand("main menu");
            printControllerMessage(authController.getLastMessage());
            menuManager.enterLoginMenu();
            return;
        }

        if (handleMenuBasics(command)) {
            return;
        }

        if (handleSettingsCommand(command)) {
            return;
        }

        if (handleGameMenuCommand(command)) {
            return;
        }

        if (handleChapterCommand(command)) {
            return;
        }

        authController.invalidCommand("main menu");
        printControllerMessage(authController.getLastMessage());
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

        if ("menu logout".equals(command)) {
            authController.logout();
            printControllerMessage(authController.getLastMessage());
            menuManager.enterLoginMenu();
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

    //remove from here and add settings menu classes
    private boolean handleSettingsCommand(String command) {
        Matcher matcher = CHANGE_DIFFICULTY_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        Integer difficulty = parseInteger(matcher.group(1));

        if (difficulty == null) {
            authController.invalidCommand("main menu");
            printControllerMessage(authController.getLastMessage());
            return true;
        }

        authController.changeDifficulty(difficulty);
        printControllerMessage(authController.getLastMessage());
        return true;
    }

    //remove from here and their classes
    private boolean handleGameMenuCommand(String command) {
        if ("menu greenhouse".equals(command)) {
            menuManager.enterNamedMenu("greenhouse");
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        if ("menu travel-log".equals(command)) {
            menuManager.enterNamedMenu("travel-log");
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        if ("menu coin-wallet".equals(command)) {
            authController.showCoinWallet();
            printControllerMessage(authController.getLastMessage());
            return true;
        }

        if ("menu gem-wallet".equals(command)) {
            authController.showGemWallet();
            printControllerMessage(authController.getLastMessage());
            return true;
        }

        Matcher matcher = CHEAT_CURRENCY_PATTERN.matcher(command);

        if (matcher.matches()) {
            Integer amount = parseInteger(matcher.group(1));

            if (amount == null) {
                authController.invalidCommand("main menu");
                printControllerMessage(authController.getLastMessage());
                return true;
            }

            authController.addCheatCurrency(amount, matcher.group(2));
            printControllerMessage(authController.getLastMessage());
            return true;
        }

        return false;
    }

    //remove from here and add its classes
    private boolean handleChapterCommand(String command) {
        Matcher menuMatcher = MENU_ENTER_CHAPTER_PATTERN.matcher(command);

        if (menuMatcher.matches()) {
            menuManager.enterChapter(menuMatcher.group(1));
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        Matcher chapterMatcher = CHAPTER_ENTER_PATTERN.matcher(command);

        if (chapterMatcher.matches()) {
            menuManager.enterChapter(chapterMatcher.group(1));
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        return false;
    }
}