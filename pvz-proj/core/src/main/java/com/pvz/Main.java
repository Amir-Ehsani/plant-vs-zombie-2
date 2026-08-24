package com.pvz;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import controllers.auth.AuthController;
import controllers.core.GameController;
import controllers.features.CollectionController;
import controllers.features.LeaderboardController;
import controllers.features.MainMenuController;
import controllers.features.NewsController;
import controllers.features.ProfileController;
import controllers.features.SettingsController;
import navigation.ScreenManager;
import pvz.skin.PvzSkin;
import ui.PvzAnimationService;

public class Main extends Game {
    private Skin skin;
    private AuthController authController;
    private MainMenuController mainMenuController;
    private GameController gameController;
    private ProfileController profileController;
    private NewsController newsController;
    private LeaderboardController leaderboardController;
    private CollectionController collectionController;
    private SettingsController settingsController;
    private PvzAnimationService animationService;
    private ScreenManager screenManager;

    @Override
    public void create() {
        skin = PvzSkin.get();
        authController = new AuthController();
        mainMenuController = new MainMenuController(authController);
        gameController = new GameController(authController);
        profileController = new ProfileController(authController);
        newsController = new NewsController(authController);
        leaderboardController = new LeaderboardController(authController);
        collectionController = new CollectionController(authController);
        settingsController = new SettingsController(authController);
        screenManager = new ScreenManager(this);
        screenManager.showInitialScreen();
    }

    @Override
    public void render() {
        if (animationService != null) {
            animationService.update();
        }
        super.render();
    }

    @Override
    public void dispose() {
        Screen currentScreen = getScreen();
        if (currentScreen != null) {
            currentScreen.dispose();
        }
        if (authController != null) {
            authController.saveUsers();
        }
        if (animationService != null) {
            animationService.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
    }

    public Skin getSkin() {
        return skin;
    }

    public AuthController getAuthController() {
        return authController;
    }

    public GameController getGameController() {
        return gameController;
    }

    public MainMenuController getMainMenuController() {
        return mainMenuController;
    }

    public ProfileController getProfileController() {
        return profileController;
    }

    public NewsController getNewsController() {
        return newsController;
    }

    public LeaderboardController getLeaderboardController() {
        return leaderboardController;
    }

    public CollectionController getCollectionController() {
        return collectionController;
    }

    public SettingsController getSettingsController() {
        return settingsController;
    }

    public PvzAnimationService getAnimationService() {
        if (animationService == null) {
            animationService = new PvzAnimationService();
        }
        return animationService;
    }

    public ScreenManager getScreenManager() {
        return screenManager;
    }
}
