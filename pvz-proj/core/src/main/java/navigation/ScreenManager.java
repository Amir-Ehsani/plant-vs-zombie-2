package navigation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.pvz.Main;
import screens.menu.BaseMenuScreen;
import screens.menu.CollectionScreen;
import screens.menu.QuestScreen;
import screens.menu.MiniGameHubScreen;
import screens.menu.AdventureScreen;
import screens.menu.AdventureLevelSelectionScreen;
import screens.menu.AdventurePlantSelectionScreen;
import screens.menu.AdventureMissionScreen;
import screens.menu.ForgotPasswordScreen;
import screens.menu.GreenhouseScreen;
import screens.menu.LeaderboardScreen;
import screens.menu.LoginScreen;
import screens.menu.MainMenuScreen;
import screens.menu.NewsScreen;
import screens.menu.PlaceholderMenuScreen;
import screens.menu.ProfileScreen;
import screens.menu.RegisterScreen;
import screens.menu.SettingsScreen;
import screens.menu.ZombotanyPlantSelectionScreen;
import screens.menu.ShopScreen;
import screens.game.GameScreen;
import screens.game.MiniGameScreen;
import screens.game.CouchIZombieScreen;
import screens.menu.NetworkLobbyScreen;
import screens.menu.NetworkPlantSelectionScreen;
import network.client.NetworkMatchContext;
import network.protocol.GameRole;
import models.minigame.NetworkIZombieGame;

public class ScreenManager {
    private final Main game;

    public ScreenManager(Main game) {
        this.game = game;
    }

    public void showInitialScreen() {
        if (game.getAuthController().isLoggedIn()) {
            showMainMenu();
            return;
        }
        showLogin();
    }

    public void showLogin() {
        showLogin("");
    }

    public void showLogin(String message) {
        show(new LoginScreen(game, message));
    }

    public void showRegister() {
        show(new RegisterScreen(game));
    }

    public void showForgotPassword() {
        show(new ForgotPasswordScreen(game));
    }

    public void showMainMenu() {
        show(new MainMenuScreen(game));
    }

    public void showProfile() {
        show(new ProfileScreen(game));
    }

    public void showNews() {
        show(new NewsScreen(game));
    }

    public void showLeaderboard() {
        show(new LeaderboardScreen(game));
    }

    public void showAdventure() {
        show(new AdventureScreen(game));
    }

    public void showAdventureLevels(String chapterName) {
        show(new AdventureLevelSelectionScreen(game, chapterName));
    }

    public void showCollection() {
        show(new CollectionScreen(game));
    }

    public void showGreenhouse() {
        show(new GreenhouseScreen(game));
    }

    public void showShop() {
        show(new ShopScreen(game, this::showMainMenu));
    }

    public void showShopFromGreenhouse() {
        show(new ShopScreen(game, this::showGreenhouse));
    }

    public void showSettings() {
        show(new SettingsScreen(game));
    }

    public void showQuests() {
        show(new QuestScreen(game));
    }

    public void showMiniGames() {
        show(new MiniGameHubScreen(game));
    }

    public void showActiveMiniGame() {
        show(new MiniGameScreen(game));
    }

    public void showNetworkLobby() {
        show(new NetworkLobbyScreen(game));
    }

    /** Backward-compatible route; online I, Zombie no longer exposes stages. */
    public void showNetworkLobby(int ignoredStage) {
        showNetworkLobby();
    }

    public void showNetworkIZombie(NetworkMatchContext context) {
        if (context == null) {
            return;
        }
        NetworkIZombieGame networkGame = new NetworkIZombieGame(game.getNetworkManager(), context);
        if (!game.getTravelLogController().enterNetworkIZombie(networkGame)) {
            showNetworkLobby();
            return;
        }
        if (context.role() == GameRole.PLANTS) {
            show(new NetworkPlantSelectionScreen(game, networkGame));
            return;
        }
        show(new MiniGameScreen(game));
    }

    public void showCouchIZombie(int stage) {
        show(new CouchIZombieScreen(game, stage));
    }

    public void showZombotanyPlantSelection(int stage) {
        show(new ZombotanyPlantSelectionScreen(game, stage));
    }

    public void showAdventureMission(String chapterName, int levelNumber) {
        show(new AdventureMissionScreen(game, chapterName, levelNumber));
    }

    public void showPlantSelection(String chapterName, int levelNumber) {
        show(new AdventurePlantSelectionScreen(game, chapterName, levelNumber));
    }

    public void showPreparedGame() {
        show(new GameScreen(game, game.getGameController()));
    }

    public void showPlaceholder(String title) {
        show(new PlaceholderMenuScreen(game, title));
    }

    private void show(Screen nextScreen) {
        Screen currentScreen = game.getScreen();
        if (currentScreen instanceof BaseMenuScreen menuScreen) {
            boolean started = menuScreen.transitionOut(() ->
                    Gdx.app.postRunnable(() -> swapScreens(currentScreen, nextScreen))
            );
            if (!started) {
                nextScreen.dispose();
            }
            return;
        }
        swapScreens(currentScreen, nextScreen);
    }

    private void swapScreens(Screen expectedCurrent, Screen nextScreen) {
        if (game.getScreen() != expectedCurrent && expectedCurrent != null) {
            nextScreen.dispose();
            return;
        }
        game.setScreen(nextScreen);
        switchMusic(nextScreen);
        if (expectedCurrent != null) {
            expectedCurrent.dispose();
        }
    }

    private void switchMusic(Screen nextScreen) {
        if (game.getAudioManager() == null) {
            return;
        }
        if (nextScreen instanceof GameScreen && game.getGameController().getGameSession() != null) {
            game.getAudioManager().playGameplayMusic(
                    game.getGameController().getGameSession().getCurrentLevel()
            );
            return;
        }
        if (nextScreen instanceof MiniGameScreen
                || nextScreen instanceof CouchIZombieScreen) {
            game.getAudioManager().playMiniGameMusic();
        }
    }
}
