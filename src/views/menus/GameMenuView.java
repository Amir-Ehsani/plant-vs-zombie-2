package views.menus;

import controllers.core.MenuManager;
import controllers.features.GameMenuController;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameMenuView extends BaseView {
    private static final Pattern MENU_ENTER_CHAPTER_PATTERN = Pattern.compile("^menu\\s+enter\\s+chapter\\s+-c\\s+(.+)\\s*$");
    private static final Pattern CHEAT_CURRENCY_PATTERN = Pattern.compile("^menu\\s+cheat\\s+add\\s+(\\d+)\\s+(coins|gems)\\s*$");

    private final MenuManager menuManager;
    private final GameMenuController gameMenuController;

    public GameMenuView(String viewName, MenuManager menuManager, GameMenuController gameMenuController) {
        super(viewName);
        this.menuManager = menuManager;
        this.gameMenuController = gameMenuController;
    }

    @Override
    public void display() {
        menuManager.showGameMenuText();
        printControllerMessage(menuManager.getLastMessage());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);

        if (!gameMenuController.isLoggedIn()) {
            gameMenuController.invalidCommand("game menu");
            printControllerMessage(gameMenuController.getLastMessage());
            menuManager.enterLoginMenu();
            return;
        }

        if (handleMenuCommand(command)) {
            return;
        }

        if (handleChapterCommand(command)) {
            return;
        }

        if (handleWalletCommand(command)) {
            return;
        }

        if (handleNotImplementedMenus(command)) {
            return;
        }

        if (handleCheatCommand(command)) {
            return;
        }

        gameMenuController.invalidCommand("game menu");
        printControllerMessage(gameMenuController.getLastMessage());
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

    private boolean handleChapterCommand(String command) {
        Matcher menuMatcher = MENU_ENTER_CHAPTER_PATTERN.matcher(command);

        if (menuMatcher.matches()) {
            gameMenuController.enterChapter(menuMatcher.group(1));
            printControllerMessage(gameMenuController.getLastMessage());
            return true;
        }

        return false;
    }

    private boolean handleWalletCommand(String command) {
        if ("menu coin-wallet".equals(command)) {
            gameMenuController.showCoinWallet();
            printControllerMessage(gameMenuController.getLastMessage());
            return true;
        }

        if ("menu gem-wallet".equals(command)) {
            gameMenuController.showGemWallet();
            printControllerMessage(gameMenuController.getLastMessage());
            return true;
        }

        return false;
    }

    private boolean handleNotImplementedMenus(String command) {
        if ("menu greenhouse".equals(command)) {
            gameMenuController.enterGreenhouse();
            printControllerMessage(gameMenuController.getLastMessage());
            return true;
        }

        if ("menu travel-log".equals(command)) {
            gameMenuController.enterTravelLog();
            printControllerMessage(gameMenuController.getLastMessage());
            return true;
        }

        return false;
    }

    private boolean handleCheatCommand(String command) {
        Matcher matcher = CHEAT_CURRENCY_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        Integer amount = parseInteger(matcher.group(1));

        if (amount == null) {
            gameMenuController.invalidCommand("game menu");
            printControllerMessage(gameMenuController.getLastMessage());
            return true;
        }

        gameMenuController.addCheatCurrency(amount, matcher.group(2));
        printControllerMessage(gameMenuController.getLastMessage());
        return true;
    }
}