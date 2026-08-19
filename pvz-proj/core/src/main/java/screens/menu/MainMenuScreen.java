package screens.menu;

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
import ui.TextureAssetLoader;

public class MainMenuScreen extends BaseMenuScreen {
    private final MainMenuController controller;
    private Texture backgroundTexture;
    private Texture bannerTexture;

    public MainMenuScreen(Main game) {
        super(game);
        controller = game.getMainMenuController();
        backgroundTexture = TextureAssetLoader.load("menu-bg.webp");
        bannerTexture = TextureAssetLoader.load("pvz2-enter-adventure.webp");
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
        addBackground();
        addTopLeftCluster();
        addTopRightCluster();
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
        table.add(playerLabel).left().padBottom(10f).row();
        table.add(new MenuButton("Profile", skin, "green_small", game.getScreenManager()::showProfile)).width(170f).height(46f).left().padBottom(8f).row();
        table.add(new MenuButton("Logout", skin, "brown", this::confirmLogout)).width(170f).height(46f).left();
    }

    private void addTopRightCluster() {
        Table table = createRoot();
        table.top().right();
        addResourceBar(table);
        Table quickLinks = new Table();
        quickLinks.add(new MenuButton("Shop", skin, "green_small", game.getScreenManager()::showShop)).width(160f).height(44f).padLeft(8f);
        quickLinks.add(new MenuButton("Greenhouse", skin, "green_small", game.getScreenManager()::showGreenhouse)).width(190f).height(44f).padLeft(8f);
        quickLinks.add(new MenuButton("Leaderboard", skin, "purple", game.getScreenManager()::showLeaderboard)).width(170f).height(44f).padLeft(8f);
        table.add(quickLinks).right().padTop(8f).row();
    }

    private void addCenterCluster() {
        Table table = createRoot();
        table.center();
        table.add(createTitle("Plants vs. Zombies 2")).padBottom(18f).row();
        table.add(createBannerActor()).width(780f).height(300f).padBottom(24f).row();
        table.add(new MenuButton("Play", skin, "green", game.getScreenManager()::showAdventure)).width(260f).height(74f);
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
        fallback.add(createTitle("Adventure")).padBottom(16f).row();
        fallback.add(createSecondaryLabel("Start your journey")).padBottom(18f).row();
        fallback.add(new MenuButton("Enter Adventure", skin, game.getScreenManager()::showAdventure)).width(260f).height(60f);
        return fallback;
    }

    private void addBottomLeftCluster() {
        Table table = createRoot();
        table.bottom().left();
        table.add(createShortcut("Collection", "almanac", game.getScreenManager()::showCollection)).padRight(20f);
        table.add(createShortcut(newsText(), "hud_zg", game.getScreenManager()::showNews));
    }

    private void addBottomRightCluster() {
        Table table = createRoot();
        table.bottom().right();
        table.add(createShortcut("Quests", "hud_quests", game.getScreenManager()::showQuests)).padLeft(20f);
        table.add(createShortcut("Settings", "settings", game.getScreenManager()::showSettings)).padLeft(20f);
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
        shortcut.add(button).size(88f).center().row();
        shortcut.add(label).width(170f).padTop(8f).center();
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
