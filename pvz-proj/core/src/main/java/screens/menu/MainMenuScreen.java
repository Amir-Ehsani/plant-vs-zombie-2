package screens.menu;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pvz.Main;
import controllers.features.MainMenuController;
import ui.ConfirmDialog;
import ui.MenuButton;

public class MainMenuScreen extends BaseMenuScreen {
    private final MainMenuController controller;

    public MainMenuScreen(Main game) {
        super(game);
        controller = game.getMainMenuController();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshResourceBar();
    }

    private void buildUi() {
        Table root = createRoot();
        addResourceBar(root);
        Table panel = createPanel();
        panel.defaults().width(260f).height(54f).pad(7f);
        panel.add(createTitle("Main Menu")).colspan(2).padBottom(16f).row();
        addMenuRows(panel);
        root.add(panel).expand().center();
    }

    private void addMenuRows(Table panel) {
        panel.add(new MenuButton("Adventure", skin, game.getScreenManager()::showAdventure));
        panel.add(new MenuButton("Collection", skin, game.getScreenManager()::showCollection)).row();
        panel.add(new MenuButton("Greenhouse", skin, game.getScreenManager()::showGreenhouse));
        panel.add(new MenuButton("Shop", skin, game.getScreenManager()::showShop)).row();
        panel.add(new MenuButton("Profile", skin, game.getScreenManager()::showProfile));
        panel.add(new MenuButton(newsText(), skin, "purple", game.getScreenManager()::showNews)).row();
        panel.add(new MenuButton("Leaderboard", skin, game.getScreenManager()::showLeaderboard));
        panel.add(new MenuButton("Settings", skin, game.getScreenManager()::showSettings)).row();
        panel.add(new MenuButton("Quests", skin, game.getScreenManager()::showQuests));
        panel.add(new MenuButton("Logout", skin, "brown", this::confirmLogout)).row();
    }

    private String newsText() {
        int count = controller.getUnreadNewsCount();
        return count > 0 ? "News (" + count + ")" : "News";
    }

    private void confirmLogout() {
        ConfirmDialog dialog = new ConfirmDialog(
                "Logout",
                "Do you want to logout?",
                skin,
                this::logout
        );
        dialog.show(stage);
    }

    private void logout() {
        controller.logout();
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            return;
        }
        game.getScreenManager().showLogin(controller.getLastMessage());
    }
}
