package controllers.core;

import controllers.auth.AuthController;
import controllers.features.ChapterLevelController;
import controllers.features.CollectionController;
import controllers.features.GameMenuController;
import controllers.features.GreenhouseController;
import controllers.features.LeaderboardController;
import controllers.features.MainMenuController;
import controllers.features.NewsController;
import controllers.features.ProfileController;
import controllers.features.SettingsController;
import controllers.features.ShopController;
import controllers.features.TravelLogController;
import views.core.BaseView;
import views.menus.ChapterLevelView;
import views.menus.CollectionView;
import views.menus.GameMenuView;
import views.menus.GameView;
import views.menus.GreenhouseView;
import views.menus.LeaderboardView;
import views.menus.LoginView;
import views.menus.MainMenuView;
import views.menus.MiniGameView;
import views.menus.NewsView;
import views.menus.ProfileView;
import views.menus.RegisterView;
import views.menus.SettingsView;
import views.menus.ShopView;
import views.menus.TravelLogView;


abstract class MenuManagerBase {
    protected final AuthController authController;
    protected final MainMenuController mainMenuController;
    protected final ProfileController profileController;
    protected final SettingsController settingsController;
    protected final GameMenuController gameMenuController;
    protected final ChapterLevelController chapterLevelController;
    protected final GameController gameController;
    protected final CollectionController collectionController;
    protected final GreenhouseController greenhouseController;
    protected final ShopController shopController;
    protected final NewsController newsController;
    protected final TravelLogController travelLogController;
    protected final LeaderboardController leaderboardController;

    protected BaseView currentView;
    protected String lastMessage;

    protected MenuManagerBase() {
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
            currentView = new MainMenuView("Main Menu", manager(), mainMenuController);
        } else {
            currentView = new RegisterView("Register Menu", manager(), authController);
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

        changeView(new RegisterView("Register Menu", manager(), authController));
        success("Entered register menu.");
    }

    public void enterLoginMenu() {
        if (authController.isLoggedIn()) {
            fail("You are already logged in. Use menu logout first.");
            return;
        }

        changeView(new LoginView("Login Menu", manager(), authController));
        success("Entered login menu.");
    }

    public void enterMainMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new MainMenuView("Main Menu", manager(), mainMenuController));
        success("Entered main menu.");
    }

    public void enterProfileMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new ProfileView("Profile Menu", manager(), profileController));
        success("Entered profile menu.");
    }

    public void enterSettingsMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new SettingsView("Settings Menu", manager(), settingsController));
        success("Entered settings menu.");
    }

    public void enterGameMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new GameMenuView("Game Menu", manager(), gameMenuController));
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
                manager(),
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

        changeView(new GameView("Game Play Menu", manager(), gameController));

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

        changeView(new CollectionView("Collection Menu", manager(), collectionController));
        success("Entered collection menu.");
    }

    public void enterGreenhouseMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new GreenhouseView("Greenhouse Menu", manager(), greenhouseController, shopController));
        success("Entered greenhouse menu.");
    }

    public void enterShopMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new ShopView("Shop Menu", manager(), shopController, greenhouseController));
        success("Entered shop menu.");
    }

    public void enterNewsMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new NewsView("News Menu", manager(), newsController));
        success("Entered news menu.");
    }

    public void enterLeaderboardMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new LeaderboardView("Leaderboard Menu", manager(), leaderboardController));
        success("Entered leaderboard menu.");
    }

    public void enterTravelLogMenu() {
        if (!authController.isLoggedIn()) {
            fail("You must login first.");
            return;
        }

        changeView(new TravelLogView("Travel Log Menu", manager(), travelLogController));
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

        changeView(new MiniGameView("Mini-game Menu", manager(), travelLogController));
        success("Entered mini-game menu.");
    }


    protected MenuManager manager() {
        return (MenuManager) this;
    }

    protected void success(String message) {
        lastMessage = message;
    }

    protected void fail(String message) {
        lastMessage = "ERROR: " + message;
    }

}
