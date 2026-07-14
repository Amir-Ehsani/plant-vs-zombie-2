package controllers.core;

import controllers.auth.AuthController;
import views.core.BaseView;
import views.menus.LoginView;
import views.menus.MainMenuView;
import views.menus.ProfileView;

public class MenuManager {
    private final AuthController authController;
    private BaseView currentView;
    private String lastMessage;

    public MenuManager() {
        this.authController = new AuthController();
        this.lastMessage = "";

        if (authController.isLoggedIn()) {
            currentView = new MainMenuView("Main Menu", this, authController);
        } else {
            currentView = new LoginView("Login Menu", this, authController);
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

    public void enterLoginMenu() {
        changeView(new LoginView("Login Menu", this, authController));
        success("Entered login menu.");
    }

    public void enterMainMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new MainMenuView("Main Menu", this, authController));
        success("Entered main menu.");
    }

    public void enterProfileMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new ProfileView("Profile Menu", this, authController));
        success("Entered profile menu.");
    }

    public void enterNamedMenu(String menuName) {
        if (menuName == null || menuName.isBlank()) {
            fail("Menu name is required.");
            return;
        }

        if ("login".equals(menuName)) {
            enterLoginMenu();
            return;
        }

        if ("main".equals(menuName)) {
            enterMainMenu();
            return;
        }

        if ("profile".equals(menuName)) {
            enterProfileMenu();
            return;
        }


        fail("can't find " + menuName + " menu.");
    }

    public void exitCurrentMenu() {
        if (currentView == null) {
            success("Program closed.");
            return;
        }

        String viewName = currentView.getViewName();

        if ("Login Menu".equals(viewName)) {
            closeProgram();
            return;
        }

        if ("Main Menu".equals(viewName)) {
            fail("Use menu logout to leave the main menu.");
            return;
        }

        if ("Profile Menu".equals(viewName)) {
            enterMainMenu();
            return;
        }

        enterMainMenu();
    }

    public void closeProgram() {
        authController.saveUsers();
        changeView(null);
        success("Program closed.");
    }

    public void enterChapter(String chapterName) {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        if (chapterName == null || chapterName.isBlank()) {
            fail("Chapter name is required.");
            return;
        }

        success("Entered chapter " + chapterName + ".");
    }

    public void showLoginMenuText() {
        success("""
                Login/Register Menu
                register -u <username> -p <password> <password_confirm> -n <nickname> -e <email> -g <gender>
                pick question -q <question_number> -a <answer> -c <answer_confirm>
                login -u <username> -p <password> -stay-logged-in
                forget password -u <username> -e <email>
                answer -a <answer>
                menu show current
                menu exit""");
    }

    public void showMainMenuText() {
        success("""
                Main Menu
                menu enter <menu_name>
                menu show current
                menu logout
                menu profile show-info
                menu profile change-username -u <username>
                menu profile change-nickname -u <nickname>
                menu profile change-email -e <email>
                menu profile change-password -p <new_password> -o <old_password>
                menu settings change-difficulty -l <difficulty_level>""");
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