package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
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

public class AdventureLevelSelectionScreen extends BaseMenuScreen {
    private static final Color TITLE_COLOR = Color.WHITE;
    private static final Color TEXT_COLOR = Color.valueOf("F6F0CF");
    private static final Color LOCKED_COLOR = Color.valueOf("C8C8C8");
    private static final String LEVEL_NODE_REGION = "IMAGE_WORLDMAP_DANGER_NODE_DARK_DANGER_NODE_DARK_471X471";

    private final String chapterName;

    public AdventureLevelSelectionScreen(Main game, String chapterName) {
        super(game);
        this.chapterName = chapterName;
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
        addMenuBackground();
        buildTopLeftNavigation();
        buildTopRightCluster();
        buildChapterMap();
    }

    private void buildTopLeftNavigation() {
        Table root = createRoot();
        root.top().left();
        root.padTop(18f).padLeft(18f);
        Table nav = new Table();
        nav.defaults().size(72f).padRight(10f);
        nav.add(createNavIconButton("IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL",
                "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED", game.getScreenManager()::showAdventure));
        nav.add(createNavSkinButton("almanac", game.getScreenManager()::showCollection));
        nav.add(createNavIconButton("IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_NORMAL",
                "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_SELECTED", game.getScreenManager()::showGreenhouse));
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

    private void buildChapterMap() {
        Group group = new Group();
        group.setSize(WORLD_WIDTH, WORLD_HEIGHT);
        group.setTouchable(Touchable.childrenOnly);
        stage.addActor(group);

        Label title = createTitle(AdventureLevelCatalog.displayChapterName(chapterName));
        title.setColor(TITLE_COLOR);
        title.setFontScale(1f);
        title.setSize(600f, 60f);
        title.setPosition((WORLD_WIDTH - title.getWidth()) / 2f, 552f);
        title.setAlignment(Align.center);
        title.setTouchable(Touchable.disabled);
        group.addActor(title);

        Label subtitle = createSecondaryLabel("Choose a level");
        subtitle.setColor(TEXT_COLOR);
        subtitle.setSize(320f, 32f);
        subtitle.setPosition((WORLD_WIDTH - subtitle.getWidth()) / 2f, 520f);
        subtitle.setAlignment(Align.center);
        subtitle.setTouchable(Touchable.disabled);
        group.addActor(subtitle);

        ChapterVisualConfig config = ChapterVisualConfig.forChapter(chapterName);
        addAsset(group, config.backdropRegionId, 450f, 110f, config.backdropWidth, config.backdropHeight);
        addAsset(group, config.spaceRegionIdA, config.spaceAX, config.spaceAY, config.spaceAWidth, config.spaceAHeight);
        addAsset(group, config.spaceRegionIdB, config.spaceBX, config.spaceBY, config.spaceBWidth, config.spaceBHeight);
        addAsset(group, config.spaceRegionIdC, config.spaceCX, config.spaceCY, config.spaceCWidth, config.spaceCHeight);
        addAsset(group, config.spaceRegionIdD, config.spaceDX, config.spaceDY, config.spaceDWidth, config.spaceDHeight);

        addIsland(group, config.homeRegionId, 70f, 190f, config.homeWidth, config.homeHeight);
        if ("wave-beach".equals(AdventureLevelCatalog.normalizeChapterName(chapterName))) {
            addAsset(group, "IMAGE_WORLDMAP_BEACH_ANIM27_ANIM27_875X481", 112f, 248f, 170f, 94f);
            addAsset(group, "IMAGE_WORLDMAP_BEACH_ANIM27_ANIM27_877X488", 110f, 300f, 176f, 98f);
        }
        addIsland(group, config.platformOneRegionId, 345f, 290f, config.platformOneWidth, config.platformOneHeight);
        addIsland(group, config.platformTwoRegionId, 620f, 305f, config.platformTwoWidth, config.platformTwoHeight);
        addIsland(group, config.castleRegionId, 890f, 160f, config.castleWidth, config.castleHeight);

        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            return;
        }

        addLevelNode(group, user, 1, 145f, 305f);
        addLevelNode(group, user, 2, 402f, 346f);
        addLevelNode(group, user, 3, 676f, 370f);
        addLevelNode(group, user, 4, 1015f, 355f);
    }

    private void addIsland(Group group, String regionId, float x, float y, float width, float height) {
        addAsset(group, regionId, x, y, width, height);
    }

    private void addLevelNode(Group group, User user, int levelNumber, float x, float y) {
        boolean unlocked = levelNumber == 1
                ? user.isChapterUnlocked(chapterName)
                : user.isChapterLevelUnlocked(chapterName, levelNumber);
        boolean completed = user.isChapterLevelCompleted(chapterName, levelNumber);

        Stack stack = new Stack();
        stack.setTouchable(Touchable.childrenOnly);
        stack.setSize(66f, 66f);
        stack.setPosition(x + 13f, y + 3f);

        ImageButton nodeButton = createNodeButton(unlocked, levelNumber);
        stack.add(nodeButton);

        Label number = new Label(String.valueOf(levelNumber), skin, "medium_outline");
        number.setColor(TITLE_COLOR);
        number.setAlignment(Align.center);
        number.setTouchable(Touchable.disabled);
        stack.add(number);

        if (completed) {
            Label completedLabel = createSecondaryLabel("DONE");
            completedLabel.setColor(TEXT_COLOR);
            completedLabel.setFontScale(0.8f);
            completedLabel.setAlignment(Align.bottom);
            completedLabel.setTouchable(Touchable.disabled);
            stack.add(completedLabel);
        }

        group.addActor(stack);

        if (!unlocked) {
            Label locked = createSecondaryLabel("LOCKED");
            locked.setColor(LOCKED_COLOR);
            locked.setAlignment(Align.center);
            locked.setSize(120f, 24f);
            locked.setPosition(x - 14f, y - 24f);
            locked.setTouchable(Touchable.disabled);
            group.addActor(locked);
        }
    }

    private ImageButton createNodeButton(boolean unlocked, int levelNumber) {
        TextureRegion region = game.getAnimationService().region(LEVEL_NODE_REGION);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        if (region != null) {
            TextureRegionDrawable drawable = new TextureRegionDrawable(region);
            style.up = drawable;
            style.over = drawable;
            style.down = drawable;
            style.checked = drawable;
        }
        ImageButton button = new ImageButton(style);
        button.getColor().a = unlocked ? 1f : 0.4f;
        button.setDisabled(!unlocked);
        button.setTouchable(unlocked ? Touchable.enabled : Touchable.disabled);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!button.isDisabled()) {
                    game.getScreenManager().showAdventureMission(chapterName, levelNumber);
                }
            }
        });
        return button;
    }

    private void addAsset(Group group, String regionId, float x, float y, float width, float height) {
        if (regionId == null || regionId.isBlank()) {
            return;
        }
        Actor actor = createRegionActor(regionId, width, height);
        actor.setPosition(x, y);
        actor.setTouchable(Touchable.disabled);
        group.addActor(actor);
    }

    private Actor createRegionActor(String regionId, float width, float height) {
        TextureRegion region = game.getAnimationService().region(regionId);
        Image image = region == null ? new Image() : new Image(region);
        image.setSize(width, height);
        image.setScaling(Scaling.fit);
        return image;
    }

    private ImageButton createNavSkinButton(String styleName, Runnable action) {
        ImageButton button = new ImageButton(skin, styleName);
        button.addListener(new ClickListener() {
            
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
            
            public void clicked(InputEvent event, float x, float y) {
                if (!button.isDisabled()) {
                    action.run();
                }
            }
        });
        return button;
    }

    private ImageButton createShopButton() {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        TextureRegion normal = game.getAnimationService().region("IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_NORMAL");
        TextureRegion pressed = game.getAnimationService().region("IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_SELECTED");
        if (normal != null) {
            TextureRegionDrawable drawable = new TextureRegionDrawable(normal);
            style.up = drawable;
            style.over = drawable;
            style.checked = drawable;
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

    private static final class ChapterVisualConfig {
        private final String backdropRegionId;
        private final float backdropWidth;
        private final float backdropHeight;
        private final String homeRegionId;
        private final float homeWidth;
        private final float homeHeight;
        private final String platformOneRegionId;
        private final float platformOneWidth;
        private final float platformOneHeight;
        private final String platformTwoRegionId;
        private final float platformTwoWidth;
        private final float platformTwoHeight;
        private final String castleRegionId;
        private final float castleWidth;
        private final float castleHeight;
        private final String spaceRegionIdA;
        private final float spaceAX;
        private final float spaceAY;
        private final float spaceAWidth;
        private final float spaceAHeight;
        private final String spaceRegionIdB;
        private final float spaceBX;
        private final float spaceBY;
        private final float spaceBWidth;
        private final float spaceBHeight;
        private final String spaceRegionIdC;
        private final float spaceCX;
        private final float spaceCY;
        private final float spaceCWidth;
        private final float spaceCHeight;
        private final String spaceRegionIdD;
        private final float spaceDX;
        private final float spaceDY;
        private final float spaceDWidth;
        private final float spaceDHeight;

        private ChapterVisualConfig(
                String backdropRegionId,
                float backdropWidth,
                float backdropHeight,
                String homeRegionId,
                float homeWidth,
                float homeHeight,
                String platformOneRegionId,
                float platformOneWidth,
                float platformOneHeight,
                String platformTwoRegionId,
                float platformTwoWidth,
                float platformTwoHeight,
                String castleRegionId,
                float castleWidth,
                float castleHeight,
                String spaceRegionIdA,
                float spaceAX,
                float spaceAY,
                float spaceAWidth,
                float spaceAHeight,
                String spaceRegionIdB,
                float spaceBX,
                float spaceBY,
                float spaceBWidth,
                float spaceBHeight,
                String spaceRegionIdC,
                float spaceCX,
                float spaceCY,
                float spaceCWidth,
                float spaceCHeight,
                String spaceRegionIdD,
                float spaceDX,
                float spaceDY,
                float spaceDWidth,
                float spaceDHeight
        ) {
            this.backdropRegionId = backdropRegionId;
            this.backdropWidth = backdropWidth;
            this.backdropHeight = backdropHeight;
            this.homeRegionId = homeRegionId;
            this.homeWidth = homeWidth;
            this.homeHeight = homeHeight;
            this.platformOneRegionId = platformOneRegionId;
            this.platformOneWidth = platformOneWidth;
            this.platformOneHeight = platformOneHeight;
            this.platformTwoRegionId = platformTwoRegionId;
            this.platformTwoWidth = platformTwoWidth;
            this.platformTwoHeight = platformTwoHeight;
            this.castleRegionId = castleRegionId;
            this.castleWidth = castleWidth;
            this.castleHeight = castleHeight;
            this.spaceRegionIdA = spaceRegionIdA;
            this.spaceAX = spaceAX;
            this.spaceAY = spaceAY;
            this.spaceAWidth = spaceAWidth;
            this.spaceAHeight = spaceAHeight;
            this.spaceRegionIdB = spaceRegionIdB;
            this.spaceBX = spaceBX;
            this.spaceBY = spaceBY;
            this.spaceBWidth = spaceBWidth;
            this.spaceBHeight = spaceBHeight;
            this.spaceRegionIdC = spaceRegionIdC;
            this.spaceCX = spaceCX;
            this.spaceCY = spaceCY;
            this.spaceCWidth = spaceCWidth;
            this.spaceCHeight = spaceCHeight;
            this.spaceRegionIdD = spaceRegionIdD;
            this.spaceDX = spaceDX;
            this.spaceDY = spaceDY;
            this.spaceDWidth = spaceDWidth;
            this.spaceDHeight = spaceDHeight;
        }

        private static ChapterVisualConfig forChapter(String chapterName) {
            return switch (AdventureLevelCatalog.normalizeChapterName(chapterName)) {
                case "ancient-egypt" -> new ChapterVisualConfig(
                        "IMAGE_WORLDMAP_EGYPT_ISLAND14", 360f, 430f,
                        "IMAGE_WORLDMAP_EGYPT_ISLAND1", 210f, 255f,
                        "IMAGE_WORLDMAP_EGYPT_ISLAND4", 175f, 140f,
                        "IMAGE_WORLDMAP_EGYPT_ISLAND9", 185f, 145f,
                        "IMAGE_WORLDMAP_ZOMBOSS_NODE_EGYPT_ZOMBOSS_NODE_EGYPT_914X994", 280f, 330f,
                        "IMAGE_WORLDMAP_EGYPT_ISLAND23", 150f, 490f, 120f, 95f,
                        "IMAGE_WORLDMAP_EGYPT_ISLAND22", 1005f, 520f, 125f, 100f,
                        null, 0f, 0f, 0f, 0f,
                        null, 0f, 0f, 0f, 0f
                );
                case "ice-cave" -> new ChapterVisualConfig(
                        "IMAGE_WORLDMAP_ICEAGE_ISLAND1", 430f, 440f,
                        "IMAGE_WORLDMAP_ICEAGE_ANIM3_ANIM3_1307X1318", 250f, 275f,
                        "IMAGE_WORLDMAP_ICEAGE_ANIM26_ANIM26_375X281", 165f, 120f,
                        "IMAGE_WORLDMAP_ICEAGE_ANIM12_ANIM12_400X500", 170f, 170f,
                        "IMAGE_WORLDMAP_ZOMBOSS_NODE_ICEAGE_ZOMBOSS_NODE_ICEAGE_1055X1280", 280f, 330f,
                        "IMAGE_WORLDMAP_ICEAGE_ISLAND43", 120f, 500f, 110f, 90f,
                        "IMAGE_WORLDMAP_ICEAGE_ISLAND44", 960f, 505f, 115f, 95f,
                        "IMAGE_WORLDMAP_ICEAGE_ISLAND42", 845f, 175f, 110f, 90f,
                        null, 0f, 0f, 0f, 0f
                );
                case "wave-beach" -> new ChapterVisualConfig(
                        "IMAGE_WORLDMAP_BEACH_ISLAND1", 430f, 430f,
                        "IMAGE_WORLDMAP_BEACH_ANIM27_ANIM27_1362X953", 255f, 210f,
                        "IMAGE_WORLDMAP_BEACH_ANIM10_ANIM10_295X271", 160f, 115f,
                        "IMAGE_WORLDMAP_BEACH_ANIM17_ANIM17_321X255", 160f, 115f,
                        "IMAGE_WORLDMAP_ZOMBOSS_NODE_BEACH_ZOMBOSS_NODE_BEACH_905X1096", 270f, 320f,
                        "IMAGE_WORLDMAP_BEACH_ISLAND42", 170f, 505f, 110f, 90f,
                        "IMAGE_WORLDMAP_BEACH_ISLAND41", 980f, 510f, 110f, 90f,
                        null, 0f, 0f, 0f, 0f,
                        null, 0f, 0f, 0f, 0f
                );
                default -> new ChapterVisualConfig(
                        "IMAGE_WORLDMAP_DARK_ANIM22_ANIM22_534X1169", 300f, 440f,
                        "IMAGE_WORLDMAP_DARK_ANIM1_ANIM1_1201X1413", 235f, 270f,
                        "IMAGE_WORLDMAP_DARK_ANIM9_ANIM9_373X659", 150f, 220f,
                        "IMAGE_WORLDMAP_DARK_ANIM10_ANIM10_352X358", 160f, 150f,
                        "IMAGE_WORLDMAP_ZOMBOSS_NODE_DARK_ZOMBOSS_NODE_DARK_905X1096", 270f, 320f,
                        "IMAGE_WORLDMAP_DARK_ANIM6_ANIM6_644X528", 145f, 505f, 120f, 95f,
                        "IMAGE_WORLDMAP_DARK_ANIM7_ANIM7_498X405", 965f, 505f, 120f, 95f,
                        "IMAGE_WORLDMAP_DARK_ISLAND58", 845f, 165f, 120f, 95f,
                        "IMAGE_WORLDMAP_DARK_ANIM6_ANIM6_649X585", 555f, 120f, 120f, 95f
                );
            };
        }
    }
}
