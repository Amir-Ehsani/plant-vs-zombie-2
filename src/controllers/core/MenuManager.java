package controllers.core;

import controllers.auth.AuthController;
import controllers.features.GameMenuController;
import controllers.features.MainMenuController;
import controllers.features.ProfileController;
import controllers.features.SettingsController;
import views.core.BaseView;
import views.menus.*;

public class MenuManager {
    private final AuthController authController;
    private final MainMenuController mainMenuController;
    private final ProfileController profileController;
    private final SettingsController settingsController;
    private final GameMenuController gameMenuController;
    private BaseView currentView;
    private String lastMessage;

    public MenuManager() {
        this.authController = new AuthController();
        this.mainMenuController = new MainMenuController(authController);
        this.profileController = new ProfileController(authController);
        this.settingsController = new SettingsController(authController);
        this.gameMenuController = new GameMenuController(authController);
        this.lastMessage = "";

        if (authController.isLoggedIn()) {
            currentView = new MainMenuView("Main Menu", this, mainMenuController);
        } else {
            currentView = new RegisterView("Register Menu", this, authController);
        }
    }

    public void changeView(BaseView newView) {
        currentView = newView;
    }

    public BaseView getCurrentView() {
        return currentView;
    }

    public AuthController getAuthController() {
        return authController;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    public void showCurrentMenu() {
        if (currentView == null) {
            success("Current menu: none");
            return;
        }

        success("Current menu: " + currentView.getViewName());
    }


    public void enterRegisterMenu() {
        if (authController.isLoggedIn()) {
            fail("You are already logged in. Use menu logout first.");
            return;
        }

        changeView(new RegisterView("Register Menu", this, authController));
        success("Entered register menu.");
    }

    public void enterLoginMenu() {
        if (authController.isLoggedIn()) {
            fail("You are already logged in. Use menu logout first.");
            return;
        }

        changeView(new LoginView("Login Menu", this, authController));
        success("Entered login menu.");
    }

    public void enterMainMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new MainMenuView("Main Menu", this, mainMenuController));
        success("Entered main menu.");
    }

    public void enterProfileMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new ProfileView("Profile Menu", this, profileController));
        success("Entered profile menu.");
    }

    public void enterSettingsMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new SettingsView("Settings Menu", this, settingsController));
        success("Entered settings menu.");
    }

    public void enterGameMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new GameMenuView("Game Menu", this, gameMenuController));
        success("Entered game menu.");
    }

    public void enterNamedMenu(String menuName) {
        if (menuName == null || menuName.isBlank()) {
            fail("Menu name is required.");
            return;
        }

        switch (menuName) {
            case "register":
                enterRegisterMenu();
                break;
            case "login":
                enterLoginMenu();
                break;
            case "main":
                enterMainMenu();
                break;
            case "profile":
                enterProfileMenu();
                break;
            case "settings":
                enterSettingsMenu();
                break;
            case "game":
                enterGameMenu();
                break;
            default:
                fail("can't find " + menuName + " menu.");
                break;
        }
    }

    public void exitCurrentMenu() {
        if (currentView == null) {
            success("Program closed.");
            return;
        }

        String viewName = currentView.getViewName();

        if ("Register Menu".equals(viewName)) {
            closeProgram();
            return;
        }

        if ("Login Menu".equals(viewName)) {
            enterRegisterMenu();
            return;
        }

        if ("Main Menu".equals(viewName)) {
            fail("Use menu logout to leave the main menu.");
            return;
        }

        enterMainMenu();
    }

    public void closeProgram() {
        authController.saveUsers();
        changeView(null);
        success("Program closed.");
    }

    public void showRegisterMenuText() {
        success("""
                Register Menu
                register -u <username> -p <password> <password_confirm> -n <nickname> -e <email> -g <gender>
                pick question -q <question_number> -a <answer> -c <answer_confirm>
                menu enter login
                menu show current
                menu exit""");
    }

    public void showLoginMenuText() {
        success("""
                Login Menu
                login -u <username> -p <password> -stay-logged-in
                forget password -u <username> -e <email>
                answer -a <answer>
                menu enter register
                menu show current
                menu exit""");
    }

    public void showMainMenuText() {
        success("""
                Main Menu
                menu enter game
                menu enter settings
                menu enter profile
                menu enter news
                menu enter network
                menu logout
                menu show current
                menu exit""");
    }

    public void showProfileMenuText() {
        success("""
                Profile Menu
                menu profile show-info
                menu profile change-username -u <username>
                menu profile change-nickname -u <nickname>
                menu profile change-email -e <email>
                menu profile change-password -p <new_password> -o <old_password>
                menu show current
                menu exit""");
    }

    public void showSettingsMenuText() {
        success("""
                Settings Menu
                menu settings change-difficulty -l <difficulty_level>
                menu show current
                menu exit""");
    }

    public void showGameMenuText() {
        success("""
                Game Menu
                menu enter collection
                menu enter chapter -c <chaptername>
                menu coin-wallet
                menu gem-wallet
                menu greenhouse
                menu travel-log
                menu leaderboard
                menu cheat add <n> <coin/diamond>
                menu show current
                menu exit""");
    }


    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }
}