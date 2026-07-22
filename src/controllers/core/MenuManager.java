package controllers.core;

import controllers.auth.AuthController;
import controllers.features.*;
import views.core.BaseView;
import views.menus.*;

public class MenuManager {
    private final AuthController authController;
    private final MainMenuController mainMenuController;
    private final ProfileController profileController;
    private final SettingsController settingsController;
    private final GameMenuController gameMenuController;
    private final ChapterLevelController chapterLevelController;
    private final GameController gameController;
    private final CollectionController collectionController;
    private final GreenhouseController greenhouseController;
    private final ShopController shopController;
    private final NewsController newsController;
    private final TravelLogController travelLogController;
    private final LeaderboardController leaderboardController;

    private BaseView currentView;
    private String lastMessage;

    public MenuManager() {
        this.authController = new AuthController();
        this.mainMenuController = new MainMenuController(authController);
        this.profileController = new ProfileController(authController);
        this.settingsController = new SettingsController(authController);
        this.gameMenuController = new GameMenuController(authController);
        this.chapterLevelController = new ChapterLevelController(authController);
        this.gameController = new GameController(authController);
        this.collectionController = new CollectionController(authController);
        this.greenhouseController = new GreenhouseController(authController);
        this.shopController = new ShopController(authController);
        this.newsController = new NewsController(authController);
        this.travelLogController = new TravelLogController(authController);
        this.leaderboardController = new LeaderboardController(authController);
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

    public void enterChapterLevelMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        String chapterName = authController.getLoggedInUser().getCurrentChapterName();
        if (chapterName == null || chapterName.isBlank()) {
            fail("Select a chapter first.");
            return;
        }

        changeView(new ChapterLevelView(
                "Chapter Level Menu",
                this,
                chapterLevelController
        ));
        success("Entered chapter level menu.");
    }

    public void enterGamePlayMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        String chapterName = authController.getLoggedInUser().getCurrentChapterName();
        int levelNumber = authController.getLoggedInUser().getCurrentChapterLevel();
        gameController.prepareChapterLevel(chapterName, levelNumber);

        if (!gameController.wasSuccessful()) {
            String message = gameController.getLastMessage();

            if (message != null && message.startsWith("ERROR: ")) {
                message = message.substring("ERROR: ".length());
            }

            fail(message);
            return;
        }

        changeView(new GameView("Game Play Menu", this, gameController));

        if (gameController.shouldAutoStartCurrentLevel()) {
            gameController.startGame();
            if (!gameController.wasSuccessful()) {
                String message = gameController.getLastMessage();
                fail(message != null && message.startsWith("ERROR: ")
                        ? message.substring("ERROR: ".length())
                        : message);
                return;
            }
        }

        success("Entered game play menu.");
    }

    public void enterCollectionMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new CollectionView("Collection Menu", this, collectionController));
        success("Entered collection menu.");
    }

    public void enterGreenhouseMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new GreenhouseView("Greenhouse Menu", this, greenhouseController, shopController));
        success("Entered greenhouse menu.");
    }

    public void enterShopMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new ShopView("Shop Menu", this, shopController, greenhouseController));
        success("Entered shop menu.");
    }

    public void enterNewsMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new NewsView("News Menu", this, newsController));
        success("Entered news menu.");
    }

    public void enterLeaderboardMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new LeaderboardView("Leaderboard Menu", this, leaderboardController));
        success("Entered leaderboard menu.");
    }

    public void enterTravelLogMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new TravelLogView("Travel Log Menu", this, travelLogController));
        success("Entered travel log menu.");
    }

    public void enterMiniGameMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        if (!travelLogController.hasActiveMiniGame()) {
            fail("No mini-game is active.");
            return;
        }

        changeView(new MiniGameView("Mini-game Menu", this, travelLogController));
        success("Entered mini-game menu.");
    }

    public void enterNamedMenu(String menuName) {
        if (menuName == null || menuName.isBlank()) {
            fail("Menu name is required.");
            return;
        }

        String targetMenu = menuName.trim().toLowerCase();
        String currentMenu = currentView == null ? "" : currentView.getViewName();

        switch (currentMenu) {
            case "Register Menu":
                if ("login".equals(targetMenu)) {
                    enterLoginMenu();
                    return;
                }

                fail("You can't enter " + targetMenu + " menu from register menu.");
                return;

            case "Login Menu":
                if ("register".equals(targetMenu)) {
                    enterRegisterMenu();
                    return;
                }

                if ("main".equals(targetMenu) && authController.isLoggedIn()) {
                    enterMainMenu();
                    return;
                }

                fail("You can't enter " + targetMenu + " menu from login menu.");
                return;

            case "Main Menu":
                switch (targetMenu) {
                    case "game":
                        enterGameMenu();
                        return;
                    case "settings":
                        enterSettingsMenu();
                        return;
                    case "profile":
                        enterProfileMenu();
                        return;
                    case "news":
                        enterNewsMenu();
                        return;
                    case "network":
                        fail("Network menu is not implemented yet.");
                        return;
                    default:
                        fail("You can't enter " + targetMenu + " menu from main menu.");
                        return;
                }

            case "Game Menu":
                if ("collection".equals(targetMenu)) {
                    enterCollectionMenu();
                    return;
                }

                fail("You can't enter " + targetMenu + " menu from game menu.");
                return;

            default:
                fail("You can't enter " + targetMenu + " menu from " + currentMenu + ".");
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

        if ("Game Play Menu".equals(viewName)) {
            enterChapterLevelMenu();
            return;
        }

        if ("Chapter Level Menu".equals(viewName)) {
            enterGameMenu();
            return;
        }

        if ("Collection Menu".equals(viewName)) {
            enterGameMenu();
            return;
        }

        if ("Greenhouse Menu".equals(viewName)) {
            enterGameMenu();
            return;
        }

        if ("Shop Menu".equals(viewName)) {
            enterGreenhouseMenu();
            return;
        }

        if ("Leaderboard Menu".equals(viewName)) {
            enterGameMenu();
            return;
        }

        if ("Travel Log Menu".equals(viewName)) {
            enterGameMenu();
            return;
        }

        if ("Mini-game Menu".equals(viewName)) {
            travelLogController.abandonMiniGame();
            enterTravelLogMenu();
            return;
        }

        if ("News Menu".equals(viewName)) {
            enterMainMenu();
            return;
        }

        if ("Profile Menu".equals(viewName)) {
            enterMainMenu();
            return;
        }

        if ("Settings Menu".equals(viewName)) {
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
            menu exit program
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
                menu cheat unlock-all-levels
                menu show current
                menu exit""");
    }

    public void showGamePlayMenuText() {
        success("""
                Game View
                
                Plant Selection Phase
                show all plants
                show available plants
                add plant -t <type>
                remove plant -t <type>
                boost plant -t <type>
                start game
                
                Game Play Phase
                show map
                show sun
                show plants
                zombies info
                show tile -l <x, y>
                plant -t <plant_type> -l <x, y>
                pluck -l <x, y>
                feed plant -l <x, y>
                collect sun -l <x, y>
                advance time -t <ticks>
                update game
                pause game
                start zombie waves
                cheat add sun -a <amount>
                cheat remove cooldown
                cheat add plant-food
                cheat spawn-zombie -t <zombie_type> -l <x, y>
                cheat nuke
                return to level menu
                
                menu show current
                menu exit""");
    }

    public void showTravelLogMenuText() {
        success("""
                Travel Log Menu
                travel log page adventure
                travel log page special
                travel log page minigames
                travel log page community
                travel log page challenges
                travel log page mystery
                travel log collect -q <quest_number>
                travel log collect all
                show minigames
                enter minigame -n <mini_game_name> [-s <stage>]
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
