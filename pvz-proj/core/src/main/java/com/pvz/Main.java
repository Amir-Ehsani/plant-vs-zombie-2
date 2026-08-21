package com.pvz;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import controllers.auth.AuthController;
import controllers.features.LeaderboardController;
import controllers.features.MainMenuController;
import controllers.features.NewsController;
import controllers.features.ProfileController;
import controllers.features.SettingsController;
import navigation.ScreenManager;
import pvz.skin.PvzSkin;

public class Main extends Game {
    private Skin skin;
    private AuthController authController;
    private MainMenuController mainMenuController;
    private ProfileController profileController;
    private NewsController newsController;
    private LeaderboardController leaderboardController;
    private SettingsController settingsController;
    private ScreenManager screenManager;

    @Override
    public void create() {
        skin = PvzSkin.get();
        authController = new AuthController();
        mainMenuController = new MainMenuController(authController);
        profileController = new ProfileController(authController);
        newsController = new NewsController(authController);
        leaderboardController = new LeaderboardController(authController);
        settingsController = new SettingsController(authController);
        screenManager = new ScreenManager(this);
        screenManager.showInitialScreen();
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

    public SettingsController getSettingsController() {
        return settingsController;
    }

    public ScreenManager getScreenManager() {
        return screenManager;
    }
}
