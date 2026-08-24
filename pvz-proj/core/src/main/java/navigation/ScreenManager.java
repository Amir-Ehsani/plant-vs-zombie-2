package navigation;

import com.badlogic.gdx.Screen;
import com.pvz.Main;
import screens.menu.CollectionScreen;
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
import screens.menu.ShopScreen;
import screens.game.GameScreen;

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
        show(new GameScreen(game));
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
        showPlaceholder("Quests");
    }

    public void showPlaceholder(String title) {
        show(new PlaceholderMenuScreen(game, title));
    }

    private void show(Screen nextScreen) {
        Screen currentScreen = game.getScreen();
        game.setScreen(nextScreen);
        if (currentScreen != null) {
            currentScreen.dispose();
        }
    }
}
