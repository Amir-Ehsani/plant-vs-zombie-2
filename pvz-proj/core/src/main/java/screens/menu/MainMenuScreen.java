package screens.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.pvz.Main;
import controllers.features.MainMenuController;
import ui.ConfirmDialog;
import ui.MenuButton;

public class MainMenuScreen extends BaseMenuScreen {
    private final MainMenuController controller;
    private Texture backgroundTexture;
    private Texture bannerTexture;

    public MainMenuScreen(Main game) {
        super(game);
        controller = game.getMainMenuController();
        backgroundTexture = loadTexture("menu-bg.png");
        bannerTexture = loadTexture("pvz2-enter-adventure.png");
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

    private Texture loadTexture(String path) {
        try {
            if (!Gdx.files.internal(path).exists()) {
                return null;
            }
            return new Texture(Gdx.files.internal(path));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void buildUi() {
        addBackground();
        addTopLeftCluster();
        addTopRightCluster();
        addRightSideCluster();
        addCenterCluster();
        addBottomLeftCluster();
        addBottomRightCluster();
    }

    private void addBackground() {
        if (backgroundTexture == null) {
            return;
        }
        Image background = new Image(backgroundTexture);
        background.setBounds(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        background.setScaling(Scaling.fill);
        stage.addActor(background);
    }

    private void addTopLeftCluster() {
        Table table = createRoot();
        table.top().left();
        Label playerLabel = new Label(loggedInName(), skin, "medium_outline");
        playerLabel.setAlignment(Align.left);
        table.add(playerLabel).left().padBottom(8f).row();
        table.add(new MenuButton("Profile", skin, "green_small", game.getScreenManager()::showProfile))
                .width(150f).height(42f).left().padBottom(6f).row();
        table.add(new MenuButton("Logout", skin, "brown", this::confirmLogout))
                .width(150f).height(42f).left();
    }

    private void addTopRightCluster() {
        Table table = createRoot();
        table.top().right();
        addResourceBar(table);
    }

    private void addRightSideCluster() {
        Table table = createRoot();
        table.right();
        table.padTop(40f);
        table.add(new MenuButton("Shop", skin, "green_small", game.getScreenManager()::showShop))
                .width(150f).height(42f).padBottom(8f).row();
        table.add(new MenuButton("Greenhouse", skin, "green_small", game.getScreenManager()::showGreenhouse))
                .width(150f).height(42f).padBottom(8f).row();
        table.add(new MenuButton("Leaderboard", skin, "purple", game.getScreenManager()::showLeaderboard))
                .width(150f).height(42f);
    }

    private void addCenterCluster() {
        Table table = createRoot();
        table.center();
        table.add(createTitle("Plants vs. Zombies 2")).padBottom(14f).row();
        table.add(createBannerActor()).width(600f).height(230f).padBottom(18f).row();
        table.add(new MenuButton("Play", skin, "green", game.getScreenManager()::showAdventure))
                .width(220f).height(60f);
    }

    private Actor createBannerActor() {
        if (bannerTexture != null) {
            Image banner = new Image(bannerTexture);
            banner.setScaling(Scaling.fit);
            banner.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.getScreenManager().showAdventure();
                }
            });
            return banner;
        }
        Table fallback = createPanel();
        fallback.pad(22f);
        fallback.add(createTitle("Adventure")).padBottom(10f).row();
        fallback.add(createSecondaryLabel("Start your journey")).padBottom(12f).row();
        fallback.add(new MenuButton("Enter Adventure", skin, game.getScreenManager()::showAdventure))
                .width(220f).height(52f);
        return fallback;
    }

    private void addBottomLeftCluster() {
        Table table = createRoot();
        table.bottom().left();
        table.add(createShortcut("Collection", "almanac", game.getScreenManager()::showCollection)).padRight(14f);
        table.add(createShortcut(newsText(), "hud_zg", game.getScreenManager()::showNews));
    }

    private void addBottomRightCluster() {
        Table table = createRoot();
        table.bottom().right();
        table.add(createShortcut("Quests", "hud_quests", game.getScreenManager()::showQuests)).padLeft(14f);
        table.add(createShortcut("Settings", "settings", game.getScreenManager()::showSettings)).padLeft(14f);
    }

    private Table createShortcut(String title, String styleName, Runnable action) {
        Table shortcut = new Table();
        ImageButton button = new ImageButton(skin, styleName);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        Label label = new Label(title, skin, "secondary");
        label.setAlignment(Align.center);
        shortcut.add(button).size(72f).center().row();
        shortcut.add(label).width(130f).padTop(5f).center();
        return shortcut;
    }

    private String loggedInName() {
        if (!game.getAuthController().isLoggedIn()) {
            return "Guest";
        }
        String nickname = game.getAuthController().getLoggedInUser().getNickname();
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        return game.getAuthController().getLoggedInUser().getUsername();
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

    @Override
    public void dispose() {
        super.dispose();
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        if (bannerTexture != null) {
            bannerTexture.dispose();
        }
    }
}
