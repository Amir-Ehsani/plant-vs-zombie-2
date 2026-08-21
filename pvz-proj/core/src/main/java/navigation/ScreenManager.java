package navigation;

import com.badlogic.gdx.Screen;
import com.pvz.Main;
import screens.menu.ForgotPasswordScreen;
import screens.menu.LeaderboardScreen;
import screens.menu.LoginScreen;
import screens.menu.MainMenuScreen;
import screens.menu.NewsScreen;
import screens.menu.PlaceholderMenuScreen;
import screens.menu.ProfileScreen;
import screens.menu.RegisterScreen;
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
        showPlaceholder("Collection");
    }

    public void showGreenhouse() {
        showPlaceholder("Greenhouse");
    }

    public void showShop() {
        showPlaceholder("Shop");
    }

    public void showSettings() {
        showPlaceholder("Settings");
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
