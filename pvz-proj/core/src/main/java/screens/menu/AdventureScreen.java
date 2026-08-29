package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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
import models.account.User;
import models.level.core.AdventureLevelCatalog;
import ui.ResourceBar;

public class AdventureScreen extends BaseMenuScreen {
    private static final Color TITLE_COLOR = Color.WHITE;
    private static final float CARD_WIDTH = 285f;
    private static final float CARD_HEIGHT = 490f;
    private static final float NAV_BUTTON_SIZE = 72f;

    private final Table chapterTable;

    public AdventureScreen(Main game) {
        super(game);
        chapterTable = new Table();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshChapters();
        refreshResourceBar();
    }

    private void buildUi() {
        addMenuBackground();
        buildTopLeftNavigation();
        buildTopRightCluster();
        buildWorldSelection();
    }

    private void buildTopLeftNavigation() {
        Table root = createRoot();
        root.top().left();
        root.padTop(18f).padLeft(18f);
        Table nav = new Table();
        nav.defaults().size(NAV_BUTTON_SIZE).padRight(10f);
        nav.add(createNavIconButton(
                "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL",
                "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED",
                game.getScreenManager()::showMainMenu
        ));
        nav.add(createNavSkinButton("almanac", game.getScreenManager()::showCollection));
        nav.add(createNavIconButton(
                "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_NORMAL",
                "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_SELECTED",
                game.getScreenManager()::showGreenhouse
        ));
        nav.add(createShopButton());
        root.add(nav).left().top();
    }

    private void buildTopRightCluster() {
        Table root = createRoot();
        root.top().right();
        root.padTop(8f).padRight(10f);
        resourceBar = new ResourceBar(skin, game.getAnimationService());
        refreshResourceBar();
        root.add(resourceBar).top().right();
    }

    private void buildWorldSelection() {
        Table root = createRoot();
        root.center();

        Table panel = new Table();
        Label title = createTitle("Choose Your Chapter");
        title.setColor(TITLE_COLOR);
        panel.add(title).padBottom(-28f).row();

        chapterTable.defaults().width(CARD_WIDTH).height(CARD_HEIGHT).padLeft(8f).padRight(8f);
        panel.add(chapterTable).width(1240f).height(540f);
        root.add(panel).center();
    }

    private void refreshChapters() {
        chapterTable.clearChildren();
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            return;
        }
        for (String chapterName : AdventureLevelCatalog.getChapterNames()) {
            chapterTable.add(createChapterCard(user, chapterName));
        }
    }

    private Table createChapterCard(User user, String chapterName) {
        Table card = new Table();
        boolean unlocked = user.isChapterUnlocked(chapterName);

        card.add(createWorldImage(chapterName, unlocked))
                .width(255f).height(350f).padTop(62f).padBottom(-6f).row();

        Label name = new Label(worldDisplayName(chapterName), skin, "big_outline");
        name.setColor(TITLE_COLOR);
        name.setFontScale(0.9f);
        name.setAlignment(Align.center);
        name.setWrap(true);
        card.add(name).width(240f).height(62f).padBottom(2f).row();

        Label progress = chapterStatusLabel(
                completedLevelCount(user, chapterName) + "/" + AdventureLevelCatalog.BOSS_LEVEL
        );
        card.add(progress).padBottom(6f).row();

        Label status = chapterStatusLabel(unlocked ? "UNLOCKED" : "LOCKED");
        card.add(status).padBottom(8f).row();
        return card;
    }

    private Image createWorldImage(String chapterName, boolean unlocked) {
        TextureRegion region = game.getAnimationService().region(worldRegionId(chapterName));
        Image image = region == null ? new Image() : new Image(region);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.enabled);
        image.setOrigin(Align.center);
        image.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                image.clearActions();
                image.addAction(Actions.scaleTo(1.07f, 1.07f, 0.16f, Interpolation.sineOut));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                image.clearActions();
                image.addAction(Actions.scaleTo(1f, 1f, 0.16f, Interpolation.sineOut));
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (unlocked) {
                    game.getScreenManager().showAdventureLevels(chapterName);
                }
            }
        });
        return image;
    }

    private Label chapterStatusLabel(String text) {
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("FBUSV8C6EI_3"), Color.WHITE);
        Label label = new Label(text, style);
        label.setColor(Color.WHITE);
        label.setFontScale(0.95f);
        label.setAlignment(Align.center);
        return label;
    }

    private ImageButton createShopButton() {
        return createNavIconButton(
                "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_NORMAL",
                "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_SELECTED",
                game.getScreenManager()::showShop
        );
    }

    private ImageButton createNavSkinButton(String styleName, Runnable action) {
        ImageButton button = new ImageButton(skin, styleName);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!button.isDisabled()) {
                    action.run();
                }
            }
        });
        return button;
    }

    private ImageButton createNavIconButton(String normalRegionId, String pressedRegionId, Runnable action) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        TextureRegion normal = game.getAnimationService().region(normalRegionId);
        TextureRegion pressed = pressedRegionId == null ? null : game.getAnimationService().region(pressedRegionId);
        if (normal != null) {
            TextureRegionDrawable drawable = new TextureRegionDrawable(normal);
            style.up = drawable;
            style.over = drawable;
            style.checked = drawable;
        } else if (pressed != null) {
            TextureRegionDrawable selectedDrawable = new TextureRegionDrawable(pressed);
            style.up = selectedDrawable.tint(new Color(0.72f, 0.72f, 0.72f, 1f));
            style.over = selectedDrawable.tint(new Color(0.88f, 0.88f, 0.88f, 1f));
            style.checked = style.up;
        }
        if (pressed != null) {
            style.down = new TextureRegionDrawable(pressed);
        } else if (normal != null) {
            style.down = new TextureRegionDrawable(normal);
        }
        ImageButton button = new ImageButton(style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!button.isDisabled()) {
                    action.run();
                }
            }
        });
        return button;
    }

    private int completedLevelCount(User user, String chapterName) {
        int completed = 0;
        for (int level = AdventureLevelCatalog.FIRST_PLAYABLE_LEVEL; level <= AdventureLevelCatalog.BOSS_LEVEL; level++) {
            if (user.isChapterLevelCompleted(chapterName, level)) {
                completed++;
            }
        }
        return completed;
    }

    private String worldDisplayName(String chapterName) {
        return switch (AdventureLevelCatalog.normalizeChapterName(chapterName)) {
            case "ancient-egypt" -> "Ancient Egypt";
            case "ice-cave" -> "Ice Age";
            case "wave-beach" -> "Wave Beach";
            case "wild-west" -> "Dark Ages";
            default -> AdventureLevelCatalog.displayChapterName(chapterName);
        };
    }

    private String worldRegionId(String chapterName) {
        return switch (AdventureLevelCatalog.normalizeChapterName(chapterName)) {
            case "ancient-egypt" -> "IMAGE_UI_UNIVERSE_WORLDS_EGYPT";
            case "ice-cave" -> "IMAGE_UI_UNIVERSE_WORLDS_ICEAGE";
            case "wave-beach" -> "IMAGE_UI_UNIVERSE_WORLDS_BEACH";
            case "wild-west" -> "IMAGE_UI_UNIVERSE_WORLDS_DARK";
            default -> "";
        };
    }
}
