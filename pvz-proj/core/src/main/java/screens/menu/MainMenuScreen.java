package screens.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.pvz.Main;
import controllers.features.MainMenuController;
import ui.ConfirmDialog;
import ui.MenuButton;
import ui.PvzAnimationService;

import java.util.ArrayList;
import java.util.List;

public class MainMenuScreen extends BaseMenuScreen {
    private static final Color SHORTCUT_TEXT_COLOR = Color.valueOf("FFFBEA");
    private final MainMenuController controller;
    private final PvzAnimationService animations;
    private Texture backgroundTexture;

    public MainMenuScreen(Main game) {
        super(game);
        controller = game.getMainMenuController();
        animations = game.getAnimationService();
        backgroundTexture = loadTexture("menu-bg.png");
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
        addCenterCluster();
        addBottomNavigation();
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
        Table identity = new Table();
        Actor profileActor = createProfileActor();
        identity.add(profileActor).size(54f, 54f).padRight(10f);
        Label playerLabel = new Label(loggedInName(), skin, "medium_outline");
        playerLabel.setAlignment(Align.left);
        identity.add(playerLabel).left();
        table.add(identity).left().padBottom(8f).row();
        table.add(new MenuButton("Profile", skin, "green_small", game.getScreenManager()::showProfile))
                .width(150f).height(42f).left().padBottom(6f).row();
        table.add(new MenuButton("Logout", skin, "brown", this::confirmLogout))
                .width(150f).height(42f).left();
    }

    private Actor createProfileActor() {
        TextureRegion region = animations.region("IMAGE_UI_HUD_EVENTBUTTON_EVENT_ICON_LUNARZOOYEAR_UP");
        if (region == null) {
            return new Label("P", skin, "big_outline");
        }
        Image image = new Image(region);
        image.setScaling(Scaling.fit);
        return image;
    }

    private void addTopRightCluster() {
        Table table = createRoot();
        table.top().right();
        addResourceBar(table);
    }

    private void addCenterCluster() {
        Table table = createRoot();
        table.center();
        table.add(createLogoActor()).padBottom(8f).row();
        table.add(createBannerActor()).width(640f).height(250f).padBottom(16f).row();
        table.add(new MenuButton("Play", skin, "green", game.getScreenManager()::showAdventure))
                .width(220f).height(60f);
    }

    private Actor createLogoActor() {
        TextureRegion region = animations.region("IMAGE_UI_MAINMENU_PVZ2_LOGO_HORIZONTAL");
        if (region == null) {
            return createTitle("Plants vs. Zombies 2");
        }
        Image logo = new Image(region);
        logo.setScaling(Scaling.fit);
        return logo;
    }

    private Actor createBannerActor() {
        List<TextureRegion> regions = new ArrayList<>();
        addRegion(regions, "IMAGE_UI_THYMED_EVENTS_LAWNBOWL_EVENT_BG");
        addRegion(regions, "IMAGE_UI_THYMED_EVENTS_LAWNOFDOOM_EVENT_BG");
        addRegion(regions, "IMAGE_UI_THYMED_EVENTS_GEM_SPREE_EVENT_BG");
        addRegion(regions, "IMAGE_UI_THYMED_EVENTS_FOODFIGHT_EVENT_BG");
        addRegion(regions, "IMAGE_UI_THYMED_EVENTS_VALENBRAINZ2025_EVENT_BG");
        if (regions.isEmpty()) {
            Table fallback = createPanel();
            fallback.pad(22f);
            fallback.add(createTitle("Adventure")).padBottom(10f).row();
            fallback.add(createSecondaryLabel("Start your journey")).padBottom(12f).row();
            fallback.add(new MenuButton("Enter Adventure", skin, game.getScreenManager()::showAdventure))
                    .width(220f).height(52f);
            return fallback;
        }
        Image banner = new Image(regions.get(0));
        banner.setScaling(Scaling.fill);
        banner.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.getScreenManager().showAdventure();
            }
        });
        final int[] index = {0};
        banner.addAction(Actions.forever(Actions.sequence(
                Actions.delay(2.8f),
                Actions.run(() -> {
                    index[0] = (index[0] + 1) % regions.size();
                    banner.setDrawable(new TextureRegionDrawable(regions.get(index[0])));
                })
        )));
        return banner;
    }

    private void addRegion(List<TextureRegion> regions, String regionId) {
        TextureRegion region = animations.region(regionId);
        if (region != null) {
            regions.add(region);
        }
    }

    private void addBottomNavigation() {
        Table table = createRoot();
        table.bottom().center();
        table.padBottom(2f);
        table.add(createSkinShortcut("Collection", "almanac", game.getScreenManager()::showCollection))
                .padRight(10f);
        table.add(createAssetShortcut(
                newsText(),
                "IMAGE_UI_HUD_NEWSBUTTON_BUTTONS_HUD_NEWS_NORMAL",
                null,
                game.getScreenManager()::showNews
        )).padRight(10f);
        table.add(new MenuButton("Leaderboard", skin, "brown", game.getScreenManager()::showLeaderboard))
                .width(160f).height(44f).padRight(10f).padBottom(16f);
        table.add(createSkinShortcut("Quests", "hud_quests", game.getScreenManager()::showQuests))
                .padRight(10f);
        table.add(createSkinShortcut("Settings", "settings", game.getScreenManager()::showSettings));
    }

    private void addBottomRightCluster() {
        Table table = createRoot();
        table.bottom().right();
        table.padBottom(102f);
        Table shortcuts = new Table();
        shortcuts.add(createAssetShortcut(
                "Shop",
                "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_NORMAL",
                "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_SELECTED",
                80f,
                game.getScreenManager()::showShop
        )).row();
        shortcuts.add(createAssetShortcut(
                "Greenhouse",
                "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_NORMAL",
                "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_SELECTED",
                72f,
                game.getScreenManager()::showGreenhouse
        )).padTop(4f);
        table.add(shortcuts);
    }

    private Table createSkinShortcut(String title, String styleName, Runnable action) {
        ImageButton button = new ImageButton(skin, styleName);
        return createShortcut(title, button, action);
    }

    private Table createAssetShortcut(
            String title,
            String normalRegionId,
            String pressedRegionId,
            Runnable action
    ) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        TextureRegion normal = animations.region(normalRegionId);
        TextureRegion pressed = pressedRegionId == null ? null : animations.region(pressedRegionId);
        if (normal != null) {
            style.up = new TextureRegionDrawable(normal);
            style.over = new TextureRegionDrawable(normal);
            style.checked = new TextureRegionDrawable(normal);
        }
        if (pressed != null) {
            style.down = new TextureRegionDrawable(pressed);
        } else if (normal != null) {
            style.down = new TextureRegionDrawable(normal);
        }
        return createShortcut(title, new ImageButton(style), 72f, action);
    }

    private Table createAssetShortcut(
            String title,
            String normalRegionId,
            String pressedRegionId,
            float iconSize,
            Runnable action
    ) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        TextureRegion normal = animations.region(normalRegionId);
        TextureRegion pressed = pressedRegionId == null ? null : animations.region(pressedRegionId);
        if (normal != null) {
            style.up = new TextureRegionDrawable(normal);
            style.over = new TextureRegionDrawable(normal);
            style.checked = new TextureRegionDrawable(normal);
        }
        if (pressed != null) {
            style.down = new TextureRegionDrawable(pressed);
        } else if (normal != null) {
            style.down = new TextureRegionDrawable(normal);
        }
        return createShortcut(title, new ImageButton(style), iconSize, action);
    }

    private Table createShortcut(String title, ImageButton button, Runnable action) {
        return createShortcut(title, button, 72f, action);
    }

    private Table createShortcut(String title, ImageButton button, float iconSize, Runnable action) {
        Table shortcut = new Table();
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        Label label = new Label(title, skin, "secondary");
        label.setColor(SHORTCUT_TEXT_COLOR);
        label.setAlignment(Align.center);
        shortcut.add(button).size(iconSize).center().row();
        shortcut.add(label).width(106f).padTop(3f).center();
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
    }
}
