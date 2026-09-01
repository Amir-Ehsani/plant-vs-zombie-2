package com.pvz;

import audio.AudioManager;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import controllers.auth.AuthController;
import controllers.core.GameController;
import controllers.features.CollectionController;
import controllers.features.GreenhouseController;
import controllers.features.LeaderboardController;
import controllers.features.MainMenuController;
import controllers.features.NewsController;
import controllers.features.ProfileController;
import controllers.features.ShopController;
import controllers.features.SettingsController;
import controllers.features.TravelLogController;
import navigation.ScreenManager;
import network.client.NetworkManager;
import network.ui.NetworkCoordinator;
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
    private GreenhouseController greenhouseController;
    private ShopController shopController;
    private SettingsController settingsController;
    private TravelLogController travelLogController;
    private PvzAnimationService animationService;
    private AudioManager audioManager;
    private ScreenManager screenManager;
    private NetworkManager networkManager;
    private NetworkCoordinator networkCoordinator;
    private int windowedWidth;
    private int windowedHeight;

    @Override
    public void create() {
        skin = PvzSkin.get();
        networkManager = new NetworkManager();
        authController = new AuthController(networkManager);
        audioManager = new AudioManager(authController);
        mainMenuController = new MainMenuController(authController);
        gameController = new GameController(authController);
        profileController = new ProfileController(authController);
        newsController = new NewsController(authController);
        leaderboardController = new LeaderboardController(authController, networkManager);
        collectionController = new CollectionController(authController);
        greenhouseController = new GreenhouseController(authController);
        shopController = new ShopController(authController);
        settingsController = new SettingsController(authController);
        travelLogController = new TravelLogController(authController);
        screenManager = new ScreenManager(this);
        networkCoordinator = new NetworkCoordinator(this, networkManager);
        windowedWidth = 1280;
        windowedHeight = 720;
        screenManager.showInitialScreen();
        if (audioManager != null) {
            audioManager.playMenuMusic();
        }
    }

    @Override
    public void render() {
        handleFullscreenToggle();
        if (animationService != null) {
            animationService.update();
        }
        super.render();
    }

    private void handleFullscreenToggle() {
        if (Gdx.graphics == null || Gdx.input == null
                || !Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            return;
        }
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(windowedWidth, windowedHeight);
            return;
        }
        windowedWidth = Math.max(640, Gdx.graphics.getWidth());
        windowedHeight = Math.max(360, Gdx.graphics.getHeight());
        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
        if (displayMode != null) {
            Gdx.graphics.setFullscreenMode(displayMode);
        }
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
        if (audioManager != null) {
            audioManager.dispose();
        }
        if (networkManager != null) {
            networkManager.close();
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

    public GreenhouseController getGreenhouseController() {
        return greenhouseController;
    }

    public ShopController getShopController() {
        return shopController;
    }

    public TravelLogController getTravelLogController() {
        return travelLogController;
    }

    public PvzAnimationService getAnimationService() {
        if (animationService == null) {
            animationService = new PvzAnimationService();
        }
        return animationService;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public ScreenManager getScreenManager() {
        return screenManager;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public NetworkCoordinator getNetworkCoordinator() {
        return networkCoordinator;
    }
}
