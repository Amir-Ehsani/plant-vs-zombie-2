package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.pvz.Main;
import controllers.features.TravelLogController;
import models.minigame.MiniGameType;
import ui.BackButton;
import ui.CroppedImage;
import ui.MenuButton;
import ui.PvzAnimationService;

import java.util.HashMap;
import java.util.Map;

public class MiniGameHubScreen extends BaseMenuScreen {
    private static final Color TEXT_COLOR = Color.valueOf("4A3A1F");
    private static final float CARD_WIDTH = 198f;
    private static final float CARD_HEIGHT = 338f;

    private final TravelLogController controller;
    private final PvzAnimationService animations;
    private final Table gameList;
    private final Map<String, Integer> selectedStageByGame;

    public MiniGameHubScreen(Main game) {
        super(game);
        controller = game.getTravelLogController();
        animations = game.getAnimationService();
        gameList = new Table();
        selectedStageByGame = new HashMap<>();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshGames();
        refreshResourceBar();
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        root.pad(8f, 10f, 8f, 10f);
        addResourceBar(root);

        Table panel = createPanel();
        panel.setClip(true);
        panel.pad(16f, 20f, 14f, 20f);

        Label screenTitle = createTitle("Minigames");
        screenTitle.setColor(Color.WHITE);
        Table header = new Table();
        header.add(screenTitle).expandX().center();
        header.add(new BackButton(skin, game.getScreenManager()::showQuests))
                .width(170f).height(44f).right();
        panel.add(header).width(1070f).padBottom(6f).row();

        Label note = createSecondaryLabel("Arcade modes, bonus stages, and online I, Zombie");
        note.setAlignment(Align.center);
        note.setColor(Color.WHITE);
        panel.add(note).padBottom(10f).row();

        panel.add(createFeaturedOnlineBanner()).width(1070f).height(128f).padBottom(10f).row();

        gameList.top().left();
        ScrollPane scrollPane = new ScrollPane(gameList, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(false, true);
        panel.add(scrollPane).width(1070f).height(412f).top().row();

        root.add(panel).width(1185f).height(648f);
    }

    private Actor createFeaturedOnlineBanner() {
        Stack stack = new Stack();
        Table clip = new Table();
        clip.setClip(true);
        TextureRegion banner = firstRegion(
                "IMAGE_UI_THYMED_EVENTS_LAWNOFDOOM_EVENT_BG",
                "IMAGE_UI_THYMED_EVENTS_VALENBRAINZ2025_EVENT_BG",
                "IMAGE_UI_THYMED_EVENTS_GEM_SPREE_EVENT_BG"
        );
        if (banner != null) {
            clip.add(new CroppedImage(banner, 28f)).grow();
        } else {
            clip.add(createPanel()).grow();
        }
        stack.add(clip);

        Table overlay = new Table();
        overlay.pad(14f, 22f, 12f, 22f);
        overlay.left();

        Label kicker = whiteLabel("FEATURED  •  2 PLAYERS", "secondary");
        overlay.add(kicker).left().padBottom(2f).row();

        Label title = new Label("I, Zombie Online", skin, "big_outline");
        title.setColor(Color.WHITE);
        overlay.add(title).left().padBottom(4f).row();

        Label detail = whiteLabel("Plant side vs zombie side. Defend the lawn for 10 minutes.", "secondary");
        overlay.add(detail).left().padBottom(8f).row();

        overlay.add(new MenuButton("Play Online", skin, "green", game.getScreenManager()::showNetworkLobby))
                .width(200f).height(42f).left();
        stack.add(overlay);
        return stack;
    }

    private void refreshGames() {
        gameList.clearChildren();
        gameList.defaults().padRight(12f).top();
        for (TravelLogController.MiniGameInfo gameInfo : controller.getMiniGames()) {
            if (!selectedStageByGame.containsKey(gameInfo.getName())) {
                selectedStageByGame.put(gameInfo.getName(), defaultStage(gameInfo));
            }
            gameList.add(createMiniGameCard(gameInfo)).width(CARD_WIDTH).height(CARD_HEIGHT).top();
        }
    }

    private Table createMiniGameCard(TravelLogController.MiniGameInfo gameInfo) {
        Table card = new Table();
        card.setClip(true);
        card.pad(10f, 10f, 10f, 10f);
        TextureRegion cardArt = animations.region("IMAGE_UI_STORE_GACHA_PINATA_GENERAL_CARD");
        if (cardArt != null) {
            card.setBackground(new TextureRegionDrawable(cardArt));
        } else {
            Table fallback = createPanel();
            fallback.setClip(true);
            fallback.pad(0f);
            card = fallback;
            card.pad(10f, 10f, 10f, 10f);
        }

        MiniGameType type = MiniGameType.fromText(gameInfo.getName());
        Table bannerClip = new Table();
        bannerClip.setClip(true);
        bannerClip.add(createBanner(type)).grow();
        card.add(bannerClip).width(168f).height(86f).padTop(16f).padBottom(6f).row();

        Label name = new Label(gameInfo.getDisplayName(), skin, "medium_outline");
        name.setColor(Color.WHITE);
        name.setAlignment(Align.center);
        name.setWrap(true);
        card.add(name).width(176f).height(42f).row();

        Label blurb = new Label(flavorText(type), skin, "secondary");
        blurb.setColor(TEXT_COLOR);
        blurb.setAlignment(Align.center);
        blurb.setWrap(true);
        card.add(blurb).width(176f).height(40f).padBottom(4f).row();

        Table stages = new Table();
        int selected = selectedStage(gameInfo);
        for (TravelLogController.MiniGameStageInfo stageInfo : gameInfo.getStages()) {
            boolean chosen = stageInfo.getStage() == selected;
            String style = !stageInfo.isUnlocked() ? "brown" : chosen ? "green_small" : "brown";
            String label = stageInfo.isCompleted() ? stageInfo.getStage() + "*" : String.valueOf(stageInfo.getStage());
            MenuButton chip = new MenuButton(
                    label,
                    skin,
                    style,
                    stageInfo.isUnlocked() ? () -> selectStage(gameInfo, stageInfo.getStage()) : null
            );
            chip.setDisabled(!stageInfo.isUnlocked());
            stages.add(chip).width(48f).height(30f).padRight(4f);
        }
        card.add(stages).padBottom(6f).row();

        TravelLogController.MiniGameStageInfo playStage = stage(gameInfo, selected);
        boolean canPlay = playStage != null && playStage.isUnlocked();
        Label status = new Label(stageStatus(playStage), skin, "secondary");
        status.setColor(TEXT_COLOR);
        status.setAlignment(Align.center);
        card.add(status).width(176f).padBottom(4f).row();

        MenuButton play = new MenuButton(
                canPlay ? "Play Stage " + selected : "Locked",
                skin,
                canPlay ? "green" : "brown",
                canPlay ? () -> openMiniGame(gameInfo.getName(), selected) : null
        );
        play.setDisabled(!canPlay);
        card.add(play).width(156f).height(38f).padBottom(6f);
        return card;
    }

    private Actor createBanner(MiniGameType type) {
        TextureRegion region = animations.region(bannerId(type));
        if (region == null) {
            region = firstRegion(
                    "IMAGE_UI_THYMED_EVENTS_LAWNBOWL_EVENT_BG",
                    "IMAGE_UI_THYMED_EVENTS_FOODFIGHT_EVENT_BG"
            );
        }
        if (region == null) {
            return new Image();
        }
        return new CroppedImage(region, 22f);
    }

    private Label whiteLabel(String text, String styleName) {
        Label.LabelStyle style = new Label.LabelStyle(skin.get(styleName, Label.LabelStyle.class));
        style.fontColor = Color.WHITE;
        Label label = new Label(text == null ? "" : text, style);
        label.setColor(Color.WHITE);
        return label;
    }

    private void selectStage(TravelLogController.MiniGameInfo gameInfo, int stage) {
        selectedStageByGame.put(gameInfo.getName(), stage);
        refreshGames();
    }

    private int selectedStage(TravelLogController.MiniGameInfo gameInfo) {
        Integer stored = selectedStageByGame.get(gameInfo.getName());
        if (stored != null) {
            TravelLogController.MiniGameStageInfo info = stage(gameInfo, stored);
            if (info != null && info.isUnlocked()) {
                return stored;
            }
        }
        return defaultStage(gameInfo);
    }

    private int defaultStage(TravelLogController.MiniGameInfo gameInfo) {
        int firstUnlocked = 1;
        boolean foundUnlocked = false;
        for (TravelLogController.MiniGameStageInfo stageInfo : gameInfo.getStages()) {
            if (!stageInfo.isUnlocked()) {
                continue;
            }
            if (!foundUnlocked) {
                firstUnlocked = stageInfo.getStage();
                foundUnlocked = true;
            }
            if (!stageInfo.isCompleted()) {
                return stageInfo.getStage();
            }
        }
        return foundUnlocked ? firstUnlocked : 1;
    }

    private TravelLogController.MiniGameStageInfo stage(TravelLogController.MiniGameInfo gameInfo, int stage) {
        for (TravelLogController.MiniGameStageInfo stageInfo : gameInfo.getStages()) {
            if (stageInfo.getStage() == stage) {
                return stageInfo;
            }
        }
        return null;
    }

    private String stageStatus(TravelLogController.MiniGameStageInfo stageInfo) {
        if (stageInfo == null) {
            return "Unavailable";
        }
        if (stageInfo.isCompleted()) {
            return "Completed";
        }
        if (stageInfo.isUnlocked()) {
            return "Ready";
        }
        return "Locked";
    }

    private void openMiniGame(String gameName, int stage) {
        if (MiniGameType.fromText(gameName) == MiniGameType.PLANT_ZOMBIES) {
            game.getScreenManager().showZombotanyPlantSelection(stage);
            return;
        }
        controller.enterMiniGame(gameName, stage);
        showControllerMessage(controller.getLastMessage());
        if (!controller.wasSuccessful()) {
            return;
        }
        game.getScreenManager().showActiveMiniGame();
    }

    private String flavorText(MiniGameType type) {
        if (type == null) {
            return "A bonus lawn challenge.";
        }
        return switch (type) {
            case VASEBREAKER -> "Smash the vases. Survive the surprise.";
            case WALLNUT_BOWLING -> "Roll nuts. Flatten the horde.";
            case I_ZOMBIE -> "Command the undead. Eat the brains.";
            case MATCH_THREE -> "Swap plants. Match three. Survive.";
            case PLANT_ZOMBIES -> "Plants that bite back.";
        };
    }

    private String bannerId(MiniGameType type) {
        if (type == null) {
            return "IMAGE_UI_THYMED_EVENTS_FOODFIGHT_EVENT_BG";
        }
        return switch (type) {
            case VASEBREAKER -> "IMAGE_UI_THYMED_EVENTS_FOODFIGHT_EVENT_BG";
            case WALLNUT_BOWLING -> "IMAGE_UI_THYMED_EVENTS_LAWNBOWL_EVENT_BG";
            case I_ZOMBIE -> "IMAGE_UI_THYMED_EVENTS_VALENBRAINZ2025_EVENT_BG";
            case MATCH_THREE -> "IMAGE_UI_THYMED_EVENTS_GEM_SPREE_EVENT_BG";
            case PLANT_ZOMBIES -> "IMAGE_UI_THYMED_EVENTS_LAWNOFDOOM_EVENT_BG";
        };
    }

    private TextureRegion firstRegion(String... ids) {
        if (animations == null || ids == null) {
            return null;
        }
        for (String id : ids) {
            TextureRegion region = animations.region(id);
            if (region != null) {
                return region;
            }
        }
        return null;
    }
}
