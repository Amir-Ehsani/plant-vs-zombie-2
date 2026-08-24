package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.pvz.Main;
import models.account.User;
import models.level.core.AdventureLevelCatalog;
import ui.BackButton;
import ui.MenuButton;
import ui.ResourceBar;

public class AdventureScreen extends BaseMenuScreen {
    private static final Color TITLE_COLOR = Color.WHITE;
    private static final Color TEXT_COLOR = Color.valueOf("F6F0CF");
    private static final Color LOCKED_COLOR = Color.valueOf("FFD35A");
    private static final float WORLD_WIDTH_CARD = 300f;
    private static final float WORLD_HEIGHT_CARD = 470f;

    private final Table worldRow;
    private final ScrollPane worldScroll;

    public AdventureScreen(Main game) {
        super(game);
        worldRow = new Table();
        worldScroll = new ScrollPane(worldRow, skin);
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
        buildWorldCarousel();
    }

    private void buildTopLeftNavigation() {
        Table root = createRoot();
        root.top().left();
        root.padTop(18f);
        Table nav = new Table();
        nav.defaults().width(170f).height(46f).padRight(10f);
        nav.add(new BackButton(skin, game.getScreenManager()::showMainMenu));
        nav.add(new MenuButton("Collection", skin, "green", game.getScreenManager()::showCollection));
        nav.add(new MenuButton("Greenhouse", skin, "green", game.getScreenManager()::showGreenhouse));
        root.add(nav).left().top();
    }

    private void buildTopRightCluster() {
        Table root = createRoot();
        root.top().right();
        root.padTop(8f).padRight(10f);
        Table cluster = new Table();
        resourceBar = new ResourceBar(skin, game.getAnimationService());
        refreshResourceBar();
        cluster.add(resourceBar).right().padRight(12f);
        cluster.add(createShopButton()).size(84f);
        root.add(cluster).top().right();
    }

    private void buildWorldCarousel() {
        Table root = createRoot();
        root.center();

        worldRow.defaults().padLeft(18f).padRight(18f);
        worldScroll.setFadeScrollBars(false);
        worldScroll.setScrollingDisabled(false, true);
        worldScroll.setForceScroll(false, false);
        worldScroll.setOverscroll(false, false);
        worldScroll.setSmoothScrolling(true);
        worldScroll.setScrollbarsOnTop(true);

        Table panel = new Table();
        Label title = createTitle("Choose Your Chapter");
        title.setColor(TITLE_COLOR);
        panel.add(title).padBottom(14f).row();
        panel.add(worldScroll).width(1120f).height(520f);

        root.add(panel).center();
    }

    private void refreshChapters() {
        worldRow.clearChildren();
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            return;
        }
        for (String chapterName : AdventureLevelCatalog.getChapterNames()) {
            worldRow.add(createChapterCard(user, chapterName)).width(WORLD_WIDTH_CARD).height(WORLD_HEIGHT_CARD);
        }
    }

    private Table createChapterCard(User user, String chapterName) {
        Table card = new Table();
        card.pad(10f);
        boolean unlocked = user.isChapterUnlocked(chapterName);

        Image worldImage = createWorldImage(chapterName);
        card.add(worldImage).width(250f).height(310f).padBottom(6f).row();

        Label name = new Label(worldDisplayName(chapterName), skin, "big_outline");
        name.setColor(TITLE_COLOR);
        name.setAlignment(Align.center);
        card.add(name).width(260f).padBottom(8f).row();

        Label progress = createSecondaryLabel(
                completedLevelCount(user, chapterName) + "/" + AdventureLevelCatalog.LAST_PLAYABLE_LEVEL
        );
        progress.setColor(TEXT_COLOR);
        progress.setAlignment(Align.center);
        card.add(progress).padBottom(4f).row();

        Label status = createSecondaryLabel(unlocked ? "UNLOCKED" : "LOCKED");
        status.setColor(unlocked ? TEXT_COLOR : LOCKED_COLOR);
        status.setAlignment(Align.center);
        card.add(status).padBottom(12f).row();

        MenuButton button = new MenuButton(
                unlocked ? "Review" : "Locked",
                skin,
                unlocked ? "green" : "brown",
                unlocked ? () -> game.getScreenManager().showAdventureLevels(chapterName) : null
        );
        button.setDisabled(!unlocked);
        card.add(button).width(170f).height(46f);
        return card;
    }

    private Image createWorldImage(String chapterName) {
        TextureRegion region = game.getAnimationService().region(worldRegionId(chapterName));
        if (region == null) {
            Label fallback = createSecondaryLabel(worldDisplayName(chapterName));
            fallback.setColor(TITLE_COLOR);
            Table fallbackHolder = new Table();
            fallbackHolder.add(fallback).center();
            Image image = new Image();
            image.setScaling(Scaling.fit);
            return image;
        }
        Image image = new Image(region);
        image.setScaling(Scaling.fit);
        return image;
    }

    private ImageButton createShopButton() {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        TextureRegion normal = game.getAnimationService().region("IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_NORMAL");
        TextureRegion pressed = game.getAnimationService().region("IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_SELECTED");
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
        ImageButton button = new ImageButton(style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!button.isDisabled()) {
                    game.getScreenManager().showShop();
                }
            }
        });
        return button;
    }

    private int completedLevelCount(User user, String chapterName) {
        int completed = 0;
        for (int level = AdventureLevelCatalog.FIRST_PLAYABLE_LEVEL;
             level <= AdventureLevelCatalog.LAST_PLAYABLE_LEVEL;
             level++) {
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
