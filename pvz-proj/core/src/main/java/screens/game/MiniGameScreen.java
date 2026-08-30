package screens.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.pvz.Main;
import controllers.features.SettingsController;
import controllers.features.TravelLogController;
import game.animation.core.PvzAnimationService;
import game.dialogue.LevelDialogueController;
import game.hud.CompactSeedBank;
import game.hud.GameplayWaveBanner;
import game.hud.WaveProgressHud;
import game.minigame.MiniGameVisualRenderer;
import game.notification.GameplayAnnouncementOverlay;
import game.render.BoardGeometry;
import models.account.Settings;
import models.engine.board.Position;
import models.minigame.IZombieGame;
import models.minigame.MatchThreeGame;
import models.minigame.NetworkIZombieGame;
import models.minigame.MiniGameSession;
import models.minigame.MiniGameType;
import models.minigame.VasebreakerGame;
import models.minigame.WallNutBowlingGame;
import models.minigame.ZombotanyGame;
import network.protocol.GameRole;
import network.protocol.NetworkMessage;
import network.protocol.ReactionCatalog;
import network.protocol.ReactionCategory;
import network.ui.ReactionGraphicActor;
import screens.BaseScreen;
import ui.ConfirmDialog;
import ui.GameOverDialog;
import ui.MenuButton;
import ui.PauseDialog;
import ui.ProgressBarActor;
import ui.ResourceBar;
import ui.UiHoverAnimator;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MiniGameScreen extends BaseScreen {
    private static final float BOARD_X = 315f;
    private static final float BOARD_Y = 74f;
    private static final float BOARD_WIDTH = 920f;
    private static final float BOARD_HEIGHT = 457f;
    private static final float TICK_SECONDS = 0.1f;
    private static final float MATCH_TRANSITION_SECONDS = 0.75f;
    private static final String SHOVEL_BUTTON_ID = "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON";
    private static final String SHOVEL_BUTTON_DOWN_ID = "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON_DOWN";

    private final TravelLogController controller;
    private final MiniGameSession session;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final MiniGameVisualRenderer visualRenderer;
    private final GameplayAnnouncementOverlay announcementOverlay;
    private final LevelDialogueController dialogueController;
    private final Vector2 cursorWorld;
    private final ResourceBar resourceBar;
    private final Map<String, Image> zombieSelectionFrames;
    private final Map<String, Table> iZombieChoiceCards;
    private final Map<String, MenuButton> matchUpgradeButtons;
    private final CompactSeedBank compactSeedBank;
    private final WaveProgressHud zombotanyWaveHud;
    private final GameplayWaveBanner zombotanyWaveBanner;
    private Position hoveredTile;
    private Position selectedMatchTile;
    private Integer selectedPacketId;
    private String selectedNutType;
    private String selectedZombieName;
    private String selectedPlantName;
    private Label matchProgressLabel;
    private ProgressBarActor matchProgressBar;
    private Table matchUpgradeTable;
    private String matchUpgradeSignature;
    private float tickAccumulator;
    private float visualStateTime;
    private float matchTransitionTime;
    private boolean gameOverShown;
    private boolean paused;
    private boolean startupUiInitialized;
    private boolean introDialogueStarted;
    private boolean resourceBarConfigured;
    private boolean debugControlsVisible;
    private boolean zombotanyShovelSelected;
    private boolean zombotanyPlantFoodSelected;
    private Button zombotanyShovelButton;
    private MenuButton zombotanyPlantFoodButton;
    private PauseDialog pauseDialog;
    private Label networkTimerLabel;
    private Label networkStatusLabel;
    private Label networkReactionLabel;
    private Table networkReactionTray;
    private ReactionGraphicActor networkReactionGraphic;
    private boolean networkReactionInFlight;
    private long networkReactionCooldownUntil;

    public MiniGameScreen(Main game) {
        super(game);
        controller = game.getTravelLogController();
        session = requireActiveSession(controller);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        geometry = new BoardGeometry(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
        animations = new PvzAnimationService();
        visualRenderer = new MiniGameVisualRenderer(
                session,
                geometry,
                animations,
                game.getAnimationService(),
                WORLD_HEIGHT
        );
        cursorWorld = new Vector2();
        resourceBar = new ResourceBar(game.getSkin(), game.getAnimationService());
        zombieSelectionFrames = new LinkedHashMap<>();
        iZombieChoiceCards = new LinkedHashMap<>();
        matchUpgradeButtons = new LinkedHashMap<>();
        compactSeedBank = session instanceof ZombotanyGame
                ? new CompactSeedBank(animations, game.getSkin()) : null;
        zombotanyWaveHud = session instanceof ZombotanyGame
                ? new WaveProgressHud(animations) : null;
        zombotanyWaveBanner = session instanceof ZombotanyGame
                ? new GameplayWaveBanner(animations, game.getSkin()) : null;
        selectedZombieName = firstZombieOption();
        selectedPlantName = firstPlantOption();
        matchUpgradeSignature = "";
        matchTransitionTime = 0f;
        resourceBarConfigured = false;
        debugControlsVisible = false;
        zombotanyShovelSelected = false;
        zombotanyPlantFoodSelected = false;
        buildHud();
        announcementOverlay = new GameplayAnnouncementOverlay(stage, game.getSkin());
        dialogueController = new LevelDialogueController(stage, game.getSkin(), animations);
        startupUiInitialized = false;
        introDialogueStarted = false;
        refreshHud();
    }

    @Override
    public void show() {
        super.show();
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, createInput()));
    }

    @Override
    public void render(float delta) {
        if (game.getNetworkCoordinator() != null) {
            game.getNetworkCoordinator().pump(stage);
        }
        initializeStartupUi();
        update(delta);
        Gdx.gl.glClearColor(0.05f, 0.08f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);
        shapes.setProjectionMatrix(stage.getCamera().combined);
        enableBlending();
        visualRenderer.render(batch, shapes, visualStateTime, hoveredTile);
        if (session instanceof ZombotanyGame gameSession && compactSeedBank != null) {
            compactSeedBank.renderZombotany(
                    shapes, batch, gameSession, visualStateTime, selectedPlantName
            );
        }
        disableBlending();
        UiHoverAnimator.attach(stage);
        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
        if (session instanceof ZombotanyGame gameSession && zombotanyWaveHud != null) {
            batch.begin();
            zombotanyWaveHud.renderZombotany(batch, gameSession, WORLD_WIDTH, WORLD_HEIGHT);
            batch.end();
        }
        if (zombotanyWaveBanner != null) {
            batch.begin();
            zombotanyWaveBanner.render(batch, WORLD_WIDTH, WORLD_HEIGHT, visualStateTime);
            batch.end();
        }
    }

    @Override
    public void dispose() {
        if (networkReactionGraphic != null) {
            networkReactionGraphic.dispose();
        }
        visualRenderer.dispose();
        batch.dispose();
        shapes.dispose();
        super.dispose();
    }

    private MiniGameSession requireActiveSession(TravelLogController value) {
        MiniGameSession active = value == null ? null : value.getActiveMiniGameSession();
        if (active == null) {
            throw new IllegalStateException("MiniGameScreen requires an active mini-game session.");
        }
        return active;
    }

    private void buildHud() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(10f);
        hud.add().expandX();
        hud.add(createPauseButton()).size(54f).padRight(8f).top();
        if (usesSunResourceBar()) {
            resourceBar.showMiniGameResources();
        } else {
            resourceBar.showMiniGameCurrencies();
        }
        hud.add(resourceBar).right().top();
        stage.addActor(hud);

        if (session instanceof IZombieGame gameSession) {
            buildIZombieBar(gameSession);
            if (gameSession instanceof NetworkIZombieGame networkGame) {
                buildNetworkIZombieHud(networkGame);
            }
        } else if (session instanceof MatchThreeGame gameSession) {
            buildMatchThreeHud(gameSession);
        } else if (session instanceof ZombotanyGame) {
            buildZombotanyInteractionControls();
        }
    }

    private boolean usesSunResourceBar() {
        return session instanceof IZombieGame
                || session instanceof MatchThreeGame
                || session instanceof ZombotanyGame;
    }

    private void buildZombotanyInteractionControls() {
        Table controls = new Table();
        controls.setFillParent(true);
        controls.bottom().right().padRight(14f).padBottom(12f);
        zombotanyShovelButton = createZombotanyShovelButton();
        controls.add(zombotanyShovelButton).width(76f).height(76f).padRight(8f);
        zombotanyPlantFoodButton = new MenuButton(
                "Plant Food [F]",
                game.getSkin(),
                "purple",
                this::selectZombotanyPlantFood
        );
        controls.add(zombotanyPlantFoodButton).width(142f).height(36f);
        stage.addActor(controls);
    }

    private Button createZombotanyShovelButton() {
        TextureRegion normal = animations.region(SHOVEL_BUTTON_ID);
        TextureRegion pressed = animations.region(SHOVEL_BUTTON_DOWN_ID);
        if (normal == null || pressed == null) {
            return new MenuButton("Shovel [S]", game.getSkin(), "green_small", this::selectZombotanyShovel);
        }
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = new TextureRegionDrawable(normal);
        style.down = new TextureRegionDrawable(pressed);
        style.checked = new TextureRegionDrawable(pressed);
        ImageButton button = new ImageButton(style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectZombotanyShovel();
            }
        });
        return button;
    }

    private void selectZombotanyShovel() {
        zombotanyShovelSelected = !zombotanyShovelSelected;
        zombotanyPlantFoodSelected = false;
        if (zombotanyShovelButton != null) {
            zombotanyShovelButton.setChecked(zombotanyShovelSelected);
        }
        if (zombotanyPlantFoodButton != null) {
            zombotanyPlantFoodButton.setChecked(false);
        }
    }

    private void selectZombotanyPlantFood() {
        if (!(session instanceof ZombotanyGame gameSession) || gameSession.getPlantFoodAmount() <= 0) {
            notificationManager.showError("No plant food available.");
            return;
        }
        zombotanyPlantFoodSelected = !zombotanyPlantFoodSelected;
        zombotanyShovelSelected = false;
        if (zombotanyPlantFoodButton != null) {
            zombotanyPlantFoodButton.setChecked(zombotanyPlantFoodSelected);
        }
        if (zombotanyShovelButton != null) {
            zombotanyShovelButton.setChecked(false);
        }
    }

    private void cancelZombotanyInteraction() {
        zombotanyShovelSelected = false;
        zombotanyPlantFoodSelected = false;
        if (zombotanyShovelButton != null) {
            zombotanyShovelButton.setChecked(false);
        }
        if (zombotanyPlantFoodButton != null) {
            zombotanyPlantFoodButton.setChecked(false);
        }
    }

    private Button createPauseButton() {
        TextureRegion region = game.getAnimationService().region("IMAGE_UI_HUD_INGAME_PAUSE_BUTTON");
        if (region == null) {
            return new MenuButton("Pause", game.getSkin(), "brown", this::showPauseDialog);
        }
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        TextureRegionDrawable drawable = new TextureRegionDrawable(region);
        style.up = drawable;
        style.over = drawable;
        style.down = drawable;
        style.checked = drawable;
        ImageButton button = new ImageButton(style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showPauseDialog();
            }
        });
        return button;
    }

    private void buildIZombieBar(IZombieGame gameSession) {
        Table host = new Table();
        host.setFillParent(true);
        host.left().top().padLeft(14f).padTop(68f);
        Table cards = new Table();
        cards.top().left();
        cards.defaults().padBottom(5f);

        if (gameSession instanceof NetworkIZombieGame networkGame
                && networkGame.getRole() == GameRole.PLANTS) {
            for (NetworkIZombieGame.PlantOptionView option : networkGame.getAvailablePlantOptions()) {
                cards.add(createNetworkPlantCard(option)).width(132f).height(88f).row();
            }
        } else {
            for (IZombieGame.ZombieOptionView option : gameSession.getAvailableZombieOptions()) {
                cards.add(createZombieCard(option)).width(132f).height(88f).row();
            }
        }
        host.add(cards).top().left();
        stage.addActor(host);
    }

    private Table createZombieCard(IZombieGame.ZombieOptionView option) {
        Table card = new Table();
        card.setTransform(true);
        Stack stack = new Stack();
        TextureRegion baseRegion = game.getAnimationService().region("IMAGE_UI_PACKETS_SELECTED");
        TextureRegion selectionRegion = game.getAnimationService().region("IMAGE_UI_PACKETS_SELECT");
        if (baseRegion != null) {
            Image base = new Image(new TextureRegionDrawable(baseRegion));
            base.setScaling(Scaling.fill);
            stack.add(base);
        }

        Table content = new Table();
        content.setFillParent(true);
        Actor zombie = game.getAnimationService().createZombieActor(option.zombieName());
        content.add(zombie).width(72f).height(72f).padLeft(2f).padRight(2f);
        Table info = new Table();
        Label name = new Label(option.zombieName(), game.getSkin(), "secondary");
        name.setColor(Color.valueOf("4A3A1F"));
        name.setWrap(true);
        name.setAlignment(Align.center);
        info.add(name).width(52f).center().row();
        Label cost = new Label(String.valueOf(option.sunCost()), game.getSkin(), "secondary");
        cost.setColor(Color.valueOf("4A3A1F"));
        info.add(cost).center().padTop(4f);
        content.add(info).width(54f).expandY().center();
        stack.add(content);

        if (selectionRegion != null) {
            Image selection = new Image(new TextureRegionDrawable(selectionRegion));
            selection.setScaling(Scaling.fill);
            selection.setVisible(option.zombieName().equalsIgnoreCase(selectedZombieName));
            stack.add(selection);
            zombieSelectionFrames.put(option.zombieName(), selection);
        }

        card.add(stack).expand().fill();
        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedZombieName = option.zombieName();
                refreshIZombieSelectionVisuals();
            }
        });
        iZombieChoiceCards.put(option.zombieName(), card);
        return card;
    }

    private Table createNetworkPlantCard(NetworkIZombieGame.PlantOptionView option) {
        Table card = new Table();
        card.setTransform(true);
        Stack stack = new Stack();
        TextureRegion baseRegion = game.getAnimationService().region("IMAGE_UI_PACKETS_SELECTED");
        TextureRegion selectionRegion = game.getAnimationService().region("IMAGE_UI_PACKETS_SELECT");
        if (baseRegion != null) {
            Image base = new Image(new TextureRegionDrawable(baseRegion));
            base.setScaling(Scaling.fill);
            stack.add(base);
        }

        Table content = new Table();
        content.setFillParent(true);
        Actor plant = game.getAnimationService().createPlantActor(option.plantName());
        content.add(plant).width(72f).height(72f).padLeft(2f).padRight(2f);
        Table info = new Table();
        Label name = new Label(option.plantName(), game.getSkin(), "secondary");
        name.setColor(Color.valueOf("4A3A1F"));
        name.setWrap(true);
        name.setAlignment(Align.center);
        info.add(name).width(52f).center().row();
        Label cost = new Label(String.valueOf(option.sunCost()), game.getSkin(), "secondary");
        cost.setColor(Color.valueOf("4A3A1F"));
        info.add(cost).center().padTop(4f);
        content.add(info).width(54f).expandY().center();
        stack.add(content);

        if (selectionRegion != null) {
            Image selection = new Image(new TextureRegionDrawable(selectionRegion));
            selection.setScaling(Scaling.fill);
            selection.setVisible(option.plantName().equalsIgnoreCase(selectedPlantName));
            stack.add(selection);
            zombieSelectionFrames.put(option.plantName(), selection);
        }

        card.add(stack).expand().fill();
        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedPlantName = option.plantName();
                refreshIZombieSelectionVisuals();
            }
        });
        iZombieChoiceCards.put(option.plantName(), card);
        return card;
    }

    private void buildNetworkIZombieHud(NetworkIZombieGame networkGame) {
        networkTimerLabel = new Label("02:00", game.getSkin(), "medium_outline");
        networkTimerLabel.setColor(Color.WHITE);
        networkTimerLabel.setAlignment(Align.center);
        networkTimerLabel.setBounds(570f, 670f, 140f, 38f);
        stage.addActor(networkTimerLabel);

        networkStatusLabel = new Label("", game.getSkin(), "secondary");
        networkStatusLabel.setColor(Color.WHITE);
        networkStatusLabel.setAlignment(Align.center);
        networkStatusLabel.setBounds(330f, 18f, 610f, 28f);
        stage.addActor(networkStatusLabel);

        MenuButton react = new MenuButton("React", game.getSkin(), "purple", this::toggleNetworkReactions);
        react.setBounds(1140f, 540f, 92f, 30f);
        stage.addActor(react);

        networkReactionTray = new Table();
        networkReactionTray.setBounds(760f, 58f, 490f, 142f);
        Table reactionPanel = new Table();
        for (String text : ReactionCatalog.texts()) {
            reactionPanel.add(new MenuButton(text.replace("!", ""), game.getSkin(), "green_small",
                    () -> sendNetworkReaction(ReactionCategory.TEXT, text)))
                    .width(145f).height(32f).pad(2f);
        }
        reactionPanel.row();
        for (String emoji : ReactionCatalog.emojis()) {
            reactionPanel.add(new MenuButton(emoji, game.getSkin(), "purple",
                    () -> sendNetworkReaction(ReactionCategory.EMOJI, emoji)))
                    .width(145f).height(32f).pad(2f);
        }
        reactionPanel.row();
        for (String sticker : ReactionCatalog.stickers()) {
            String shortName = sticker.replace("DANCING_", "").replace("DIZZY_", "").replace("BOUNCING_", "");
            reactionPanel.add(new MenuButton(shortName, game.getSkin(), "brown",
                    () -> sendNetworkReaction(ReactionCategory.STICKER, sticker)))
                    .width(145f).height(32f).pad(2f);
        }
        networkReactionTray.add(reactionPanel);
        networkReactionTray.setVisible(false);
        stage.addActor(networkReactionTray);

        networkReactionLabel = new Label("", game.getSkin(), "medium_outline");
        networkReactionLabel.setColor(Color.YELLOW);
        networkReactionLabel.setWrap(true);
        networkReactionLabel.setAlignment(Align.center);
        networkReactionLabel.setBounds(900f, 475f, 330f, 70f);
        networkReactionLabel.setVisible(false);
        stage.addActor(networkReactionLabel);

        networkReactionGraphic = new ReactionGraphicActor();
        networkReactionGraphic.setPosition(1060f, 405f);
        stage.addActor(networkReactionGraphic);
    }

    private void buildMatchThreeHud(MatchThreeGame gameSession) {
        Table host = new Table();
        host.setFillParent(true);
        host.left().top().padLeft(18f).padTop(86f);

        Table panel = new Table();
        panel.setBackground(matchPanelDrawable());
        panel.pad(14f, 12f, 16f, 12f);

        Label title = new Label("BEGHOULED", game.getSkin(), "medium_outline");
        title.setColor(Color.valueOf("FFF2A6"));
        title.setAlignment(Align.center);
        panel.add(title).width(250f).padBottom(1f).row();

        Label subtitle = createWhiteSecondaryLabel("SWAP  •  MATCH  •  SURVIVE");
        subtitle.setAlignment(Align.center);
        panel.add(subtitle).width(250f).padBottom(8f).row();

        matchProgressBar = new ProgressBarActor(game.getSkin(), 0f, gameSession.getTargetMatches());
        matchProgressBar.setTextColor(Color.WHITE);
        panel.add(matchProgressBar).width(220f).padBottom(2f).row();

        matchProgressLabel = createWhiteSecondaryLabel("");
        matchProgressLabel.setAlignment(Align.center);
        panel.add(matchProgressLabel).width(240f).padBottom(9f).row();

        Label hint = createWhiteSecondaryLabel("Swap adjacent plants to make 3+");
        hint.setAlignment(Align.center);
        panel.add(hint).width(240f).padBottom(10f).row();

        Label upgrades = createWhiteSecondaryLabel("UPGRADES");
        upgrades.setAlignment(Align.center);
        panel.add(upgrades).width(240f).padBottom(5f).row();

        matchUpgradeTable = new Table();
        panel.add(matchUpgradeTable).width(245f).top().row();
        host.add(panel).width(275f).top().left();
        stage.addActor(host);
        rebuildMatchUpgradeButtons(gameSession);
    }

    private Label createWhiteSecondaryLabel(String text) {
        Label.LabelStyle style = new Label.LabelStyle(
                game.getSkin().get("secondary", Label.LabelStyle.class)
        );
        style.fontColor = Color.WHITE;
        return new Label(text == null ? "" : text, style);
    }

    private Drawable matchPanelDrawable() {
        try {
            return game.getSkin().getDrawable("image_ui_quests_panel_edge_to_edge_ten");
        } catch (Exception ignored) {
            return null;
        }
    }

    private void rebuildMatchUpgradeButtons(MatchThreeGame gameSession) {
        if (matchUpgradeTable == null) {
            return;
        }
        matchUpgradeTable.clearChildren();
        matchUpgradeButtons.clear();
        matchUpgradeTable.defaults().padBottom(6f);
        for (MatchThreeGame.UpgradeOptionView option : gameSession.getUpgradeOptions()) {
            Table row = new Table();
            row.pad(2f);
            Label route = createWhiteSecondaryLabel(
                    option.sourcePlantName() + "  →  " + option.targetPlantName()
            );
            route.setWrap(true);
            route.setAlignment(Align.center);
            row.add(route).width(230f).padBottom(3f).row();

            MenuButton button = new MenuButton(
                    "UPGRADE   " + option.cost() + " SUN",
                    game.getSkin(),
                    "green_small",
                    () -> {
                        boolean upgraded = controller.upgradeMatchThreePlant(option.sourcePlantName());
                        if (upgraded) {
                            visualRenderer.startMatchUpgradeEffect(option.targetPlantName());
                        }
                        showActionMessage();
                        drainMatchAnnouncements();
                        refreshHud();
                    }
            );
            button.setDisabled(gameSession.getSunAmount() < option.cost());
            matchUpgradeButtons.put(option.sourcePlantName(), button);
            row.add(button).width(220f).height(36f);
            matchUpgradeTable.add(row).width(240f).row();
        }
        matchUpgradeSignature = matchUpgradeSignature(gameSession);
    }

    private void update(float delta) {
        updatePointer();
        if (session instanceof NetworkIZombieGame networkGame) {
            networkGame.pumpNetworkEvents();
            drainNetworkReactions(networkGame);
        }
        int gameSpeed = session instanceof NetworkIZombieGame ? 1 : resolveGameSpeed();
        float gameplayDelta = paused ? 0f : Math.max(0f, delta) * gameSpeed;
        if (matchTransitionTime > 0f) {
            matchTransitionTime = Math.max(0f, matchTransitionTime - gameplayDelta);
        }
        if (!paused && session.isRunning() && matchTransitionTime <= 0f
                && !(session instanceof NetworkIZombieGame)) {
            advanceGame(gameplayDelta);
        }
        float visualDelta = paused ? 0f : Math.min(delta, 1f / 15f) * gameSpeed;
        visualStateTime += visualDelta;
        visualRenderer.setSelectedNutType(selectedNutType);
        visualRenderer.setSelectedMatchTile(selectedMatchTile);
        visualRenderer.update(visualDelta);
        collectIZombieSunUnderPointer();
        collectZombotanySunUnderPointer();
        if (session instanceof ZombotanyGame gameSession && zombotanyWaveHud != null) {
            zombotanyWaveHud.updateZombotany(gameSession);
        }
        if (zombotanyWaveBanner != null) {
            zombotanyWaveBanner.update(visualDelta, null);
        }
        syncPacketSelection();
        refreshHud();
        if (!paused) {
            drainMatchAnnouncements();
            drainZombotanyAnnouncements();
        }
        showGameOverIfNeeded();
    }

    private void advanceGame(float gameplayDelta) {
        tickAccumulator += gameplayDelta;
        while (tickAccumulator >= TICK_SECONDS && session.isRunning()) {
            controller.advanceMiniGameTime(1);
            tickAccumulator -= TICK_SECONDS;
        }
    }

    private int resolveGameSpeed() {
        Settings settings = game.getSettingsController().getSettings();
        if (settings == null) {
            return Settings.MIN_GAME_SPEED;
        }
        return Math.max(
                Settings.MIN_GAME_SPEED,
                Math.min(Settings.MAX_GAME_SPEED, settings.getGameSpeed())
        );
    }

    private void refreshHud() {
        configureMiniGameResourceBar();
        resourceBar.refresh(game.getAuthController().getLoggedInUser());
        if (session instanceof IZombieGame gameSession) {
            resourceBar.refreshMiniGame(game.getAuthController().getLoggedInUser(), gameSession.getSunAmount());
            if (gameSession instanceof NetworkIZombieGame networkGame) {
                refreshNetworkIZombieHud(networkGame);
            }
        } else if (session instanceof MatchThreeGame gameSession) {
            resourceBar.refreshMiniGame(game.getAuthController().getLoggedInUser(), gameSession.getSunAmount());
            if (matchProgressBar != null) {
                matchProgressBar.setValue(gameSession.getCompletedMatches());
            }
            if (matchProgressLabel != null) {
                matchProgressLabel.setText(
                        gameSession.getCompletedMatches() + " / " + gameSession.getTargetMatches()
                                + " matches   •   " + gameSession.getRemainingMatches() + " left"
                );
            }
            String signature = matchUpgradeSignature(gameSession);
            if (!signature.equals(matchUpgradeSignature)) {
                rebuildMatchUpgradeButtons(gameSession);
            } else {
                refreshMatchUpgradeButtonState(gameSession);
            }
        } else if (session instanceof ZombotanyGame gameSession) {
            resourceBar.refreshGame(
                    game.getAuthController().getLoggedInUser(),
                    gameSession.getSunAmount(),
                    gameSession.getPlantFoodAmount()
            );
            if (zombotanyPlantFoodButton != null) {
                zombotanyPlantFoodButton.setDisabled(gameSession.getPlantFoodAmount() <= 0);
            }
        }
    }

    private String matchUpgradeSignature(MatchThreeGame gameSession) {
        StringBuilder result = new StringBuilder();
        for (MatchThreeGame.UpgradeOptionView option : gameSession.getUpgradeOptions()) {
            result.append(option.sourcePlantName()).append('>')
                    .append(option.targetPlantName()).append(':')
                    .append(option.cost()).append(';');
        }
        return result.toString();
    }

    private void refreshMatchUpgradeButtonState(MatchThreeGame gameSession) {
        for (MatchThreeGame.UpgradeOptionView option : gameSession.getUpgradeOptions()) {
            MenuButton button = matchUpgradeButtons.get(option.sourcePlantName());
            if (button != null) {
                button.setDisabled(gameSession.getSunAmount() < option.cost());
            }
        }
    }

    private void configureMiniGameResourceBar() {
        boolean debug = isDebugMode();
        if (resourceBarConfigured && debugControlsVisible == debug) {
            return;
        }
        if (session instanceof ZombotanyGame) {
            resourceBar.setGameDebugControls(
                    debug,
                    this::addDebugCoins,
                    this::addDebugDiamonds,
                    this::addDebugSun,
                    this::addDebugPlantFood
            );
        } else {
            resourceBar.setMiniGameDebugControls(
                    usesSunResourceBar(),
                    debug,
                    this::addDebugCoins,
                    this::addDebugDiamonds,
                    usesSunResourceBar() ? this::addDebugSun : null
            );
        }
        resourceBarConfigured = true;
        debugControlsVisible = debug;
    }

    private boolean isDebugMode() {
        Settings settings = game.getSettingsController().getSettings();
        return settings != null && settings.isDebugMode();
    }

    private void addDebugCoins() {
        SettingsController settingsController = game.getSettingsController();
        settingsController.addDebugCoins(1000);
        showSettingsMessage(settingsController);
        refreshHud();
    }

    private void addDebugDiamonds() {
        SettingsController settingsController = game.getSettingsController();
        settingsController.addDebugDiamonds(10);
        showSettingsMessage(settingsController);
        refreshHud();
    }

    private void addDebugSun() {
        if (session instanceof MatchThreeGame gameSession) {
            gameSession.addDebugSun(250);
        } else if (session instanceof IZombieGame gameSession) {
            gameSession.addDebugSun(250);
            announcementOverlay.push("DEBUG +250 SUN");
        } else if (session instanceof ZombotanyGame gameSession) {
            gameSession.addDebugSun(250);
            announcementOverlay.push("DEBUG +250 SUN");
        }
        refreshHud();
    }

    private void addDebugPlantFood() {
        if (session instanceof ZombotanyGame) {
            controller.addZombotanyDebugPlantFood();
            showActionMessage();
            refreshHud();
        }
    }

    private void showSettingsMessage(SettingsController settingsController) {
        String message = settingsController.getLastMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        if (message.startsWith("ERROR:")) {
            notificationManager.showError(stripPrefix(message));
        } else {
            announcementOverlay.push(stripPrefix(message));
        }
    }

    private void drainMatchAnnouncements() {
        if (!(session instanceof MatchThreeGame gameSession)) {
            return;
        }
        for (String message : gameSession.consumeAnnouncements()) {
            announcementOverlay.push(message);
        }
    }

    private void drainZombotanyAnnouncements() {
        if (!(session instanceof ZombotanyGame gameSession)) {
            return;
        }
        for (String message : gameSession.consumeAnnouncements()) {
            announcementOverlay.push(message);
        }
    }

    private void refreshIZombieSelectionVisuals() {
        String selected = selectedZombieName;
        if (session instanceof NetworkIZombieGame networkGame && networkGame.getRole() == GameRole.PLANTS) {
            selected = selectedPlantName;
        }
        for (Map.Entry<String, Image> entry : zombieSelectionFrames.entrySet()) {
            entry.getValue().setVisible(selected != null && entry.getKey().equalsIgnoreCase(selected));
        }
    }


    private InputAdapter createInput() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.S && session instanceof ZombotanyGame) {
                    selectZombotanyShovel();
                    return true;
                }
                if (keycode == Input.Keys.F && session instanceof ZombotanyGame) {
                    selectZombotanyPlantFood();
                    return true;
                }
                if (keycode == Input.Keys.P || keycode == Input.Keys.SPACE) {
                    if (pauseDialog == null) {
                        showPauseDialog();
                    } else {
                        resumeFromPause();
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                updatePointer(screenX, screenY);
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                updatePointer(screenX, screenY);
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT || paused || !session.isRunning()) {
                    return false;
                }
                updatePointer(screenX, screenY);

                if (session instanceof VasebreakerGame) {
                    Integer packetId = visualRenderer.findPacketAt(cursorWorld.x, cursorWorld.y);
                    if (packetId != null) {
                        selectedPacketId = selectedPacketId != null && selectedPacketId.equals(packetId)
                                ? null : packetId;
                        return true;
                    }
                }

                if (session instanceof WallNutBowlingGame) {
                    String nutType = visualRenderer.findWallNutAt(cursorWorld.x, cursorWorld.y);
                    if (nutType != null) {
                        selectedNutType = nutType.equals(selectedNutType) ? null : nutType;
                        visualRenderer.setSelectedNutType(selectedNutType);
                        return true;
                    }
                }

                if (session instanceof IZombieGame && !(session instanceof NetworkIZombieGame)) {
                    Integer sunDropId = visualRenderer.findSunDropAt(cursorWorld.x, cursorWorld.y);
                    if (sunDropId != null) {
                        controller.collectIZombieSun(sunDropId);
                        showActionMessage();
                        refreshHud();
                        return true;
                    }
                }

                if (session instanceof ZombotanyGame) {
                    Integer sunDropId = visualRenderer.findSunDropAt(cursorWorld.x, cursorWorld.y);
                    if (sunDropId != null) {
                        controller.collectZombotanySun(sunDropId);
                        refreshHud();
                        return true;
                    }
                }

                if (session instanceof ZombotanyGame gameSession && compactSeedBank != null) {
                    String plantName = compactSeedBank.findZombotanyPlantAt(
                            gameSession, cursorWorld.x, cursorWorld.y
                    );
                    if (plantName != null) {
                        selectedPlantName = plantName;
                        cancelZombotanyInteraction();
                        return true;
                    }
                }

                return handleBoardClick();
            }

        };
    }

    private boolean handleBoardClick() {
        if (hoveredTile == null) {
            return false;
        }
        return switch (session.getType()) {
            case VASEBREAKER -> handleVasebreakerClick();
            case WALLNUT_BOWLING -> handleBowlingClick();
            case I_ZOMBIE -> handleIZombieClick();
            case MATCH_THREE -> handleMatchThreeClick();
            case PLANT_ZOMBIES -> handleZombotanyClick();
        };
    }

    private boolean handleVasebreakerClick() {
        if (selectedPacketId != null) {
            boolean result = controller.plantVasebreakerPacket(selectedPacketId, hoveredTile);
            if (result) {
                selectedPacketId = null;
            }
            showActionMessage();
            return true;
        }
        String kind = currentVaseKindAt(hoveredTile);
        if (kind == null) {
            return false;
        }
        boolean result = controller.breakVase(hoveredTile);
        if (result) {
            visualRenderer.onVaseBroken(hoveredTile, kind);
        }
        showActionMessage();
        return true;
    }

    private boolean handleBowlingClick() {
        if (selectedNutType == null) {
            return false;
        }
        boolean launched = controller.launchNut(selectedNutType, hoveredTile);
        showActionMessage();
        if (launched) {
            selectedNutType = null;
            visualRenderer.setSelectedNutType(null);
        }
        return true;
    }

    private boolean handleIZombieClick() {
        if (session instanceof NetworkIZombieGame networkGame) {
            boolean accepted;
            if (networkGame.getRole() == GameRole.PLANTS) {
                if (selectedPlantName == null) {
                    return false;
                }
                accepted = networkGame.placePlant(selectedPlantName, hoveredTile);
            } else {
                if (selectedZombieName == null) {
                    return false;
                }
                accepted = networkGame.spawnZombie(selectedZombieName, hoveredTile);
            }
            if (!accepted && !networkGame.getNetworkMessage().isBlank()) {
                notificationManager.showError(networkGame.getNetworkMessage());
            }
            refreshHud();
            return true;
        }
        if (selectedZombieName == null) {
            return false;
        }
        controller.spawnIZombie(selectedZombieName, hoveredTile);
        showActionMessage();
        refreshHud();
        return true;
    }

    private boolean handleMatchThreeClick() {
        if (!(session instanceof MatchThreeGame gameSession)) {
            return false;
        }
        if (gameSession.getCraters().contains(hoveredTile)
                || !gameSession.getBoard().getTileAt(hoveredTile).hasPlant()) {
            selectedMatchTile = null;
            visualRenderer.setSelectedMatchTile(null);
            return false;
        }
        if (selectedMatchTile == null) {
            selectedMatchTile = hoveredTile;
            visualRenderer.setSelectedMatchTile(selectedMatchTile);
            return true;
        }
        if (selectedMatchTile.equals(hoveredTile)) {
            selectedMatchTile = null;
            visualRenderer.setSelectedMatchTile(null);
            return true;
        }
        int distance = Math.abs(selectedMatchTile.getX() - hoveredTile.getX())
                + Math.abs(selectedMatchTile.getY() - hoveredTile.getY());
        if (distance != 1) {
            selectedMatchTile = hoveredTile;
            visualRenderer.setSelectedMatchTile(selectedMatchTile);
            return true;
        }

        if (matchTransitionTime > 0f) {
            return true;
        }
        boolean matched = controller.swapMatchThreePlants(selectedMatchTile, hoveredTile);
        if (matched) {
            matchTransitionTime = MATCH_TRANSITION_SECONDS;
        }
        selectedMatchTile = null;
        visualRenderer.setSelectedMatchTile(null);
        showActionMessage();
        drainMatchAnnouncements();
        refreshHud();
        return true;
    }

    private boolean handleZombotanyClick() {
        if (zombotanyShovelSelected) {
            controller.pluckZombotany(hoveredTile);
            showActionMessage();
            refreshHud();
            return true;
        }
        if (zombotanyPlantFoodSelected) {
            boolean fed = controller.feedZombotanyPlant(hoveredTile);
            showActionMessage();
            if (fed) {
                zombotanyPlantFoodSelected = false;
                if (zombotanyPlantFoodButton != null) {
                    zombotanyPlantFoodButton.setChecked(false);
                }
            }
            refreshHud();
            return true;
        }
        if (selectedPlantName == null) {
            return false;
        }
        controller.plantZombotany(selectedPlantName, hoveredTile);
        showActionMessage();
        refreshHud();
        return true;
    }

    private String currentVaseKindAt(Position position) {
        if (!(session instanceof VasebreakerGame gameSession) || position == null) {
            return null;
        }
        for (VasebreakerGame.VaseView vase : gameSession.getVases()) {
            if (vase.position().equals(position)) {
                return vase.kind();
            }
        }
        return null;
    }

    private void syncPacketSelection() {
        if (!(session instanceof VasebreakerGame gameSession) || selectedPacketId == null) {
            return;
        }
        for (VasebreakerGame.SeedPacketView packet : gameSession.getSeedPackets()) {
            if (packet.id() == selectedPacketId) {
                return;
            }
        }
        selectedPacketId = null;
    }

    private String firstZombieOption() {
        if (!(session instanceof IZombieGame gameSession) || gameSession.getAvailableZombieOptions().isEmpty()) {
            return null;
        }
        return gameSession.getAvailableZombieOptions().get(0).zombieName();
    }

    private String firstPlantOption() {
        if (session instanceof NetworkIZombieGame networkGame
                && networkGame.getRole() == GameRole.PLANTS
                && !networkGame.getAvailablePlantOptions().isEmpty()) {
            return networkGame.getAvailablePlantOptions().get(0).plantName();
        }
        if (!(session instanceof ZombotanyGame gameSession) || gameSession.getSeedOptions().isEmpty()) {
            return null;
        }
        return gameSession.getSeedOptions().get(0).plantName();
    }

    private void collectIZombieSunUnderPointer() {
        if (paused || !(session instanceof IZombieGame) || session instanceof NetworkIZombieGame || !session.isRunning()) {
            return;
        }
        Integer sunDropId = visualRenderer.findSunDropAt(cursorWorld.x, cursorWorld.y);
        if (sunDropId == null) {
            return;
        }
        controller.collectIZombieSun(sunDropId);
        refreshHud();
    }

    private void collectZombotanySunUnderPointer() {
        if (paused || !(session instanceof ZombotanyGame) || !session.isRunning()) {
            return;
        }
        Integer sunDropId = visualRenderer.findSunDropAt(cursorWorld.x, cursorWorld.y);
        if (sunDropId == null) {
            return;
        }
        controller.collectZombotanySun(sunDropId);
        refreshHud();
    }

    private void showActionMessage() {
        String message = controller.getLastMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        if (message.startsWith("ERROR:")) {
            notificationManager.showError(stripPrefix(message));
        }
    }

    private void showGameOverIfNeeded() {
        if (gameOverShown || session.isRunning()) {
            return;
        }
        gameOverShown = true;
        boolean victory = session.isWon();
        if (session instanceof NetworkIZombieGame) {
            GameOverDialog onlineDialog = new GameOverDialog(
                    game.getSkin(),
                    victory,
                    stripPrefix(session.getLastMessage()),
                    this::returnToNetworkLobby,
                    this::returnToNetworkLobby
            );
            onlineDialog.show(stage);
            announcementOverlay.push(victory ? "VICTORY!" : "DEFEAT!");
            return;
        }
        GameOverDialog dialog = new GameOverDialog(
                game.getSkin(),
                victory,
                stripPrefix(session.getLastMessage()),
                victory ? this::exitMiniGame : this::retryMiniGame,
                this::exitMiniGame
        );
        dialog.show(stage);
        announcementOverlay.push(victory ? "LEVEL COMPLETE!" : "TRY AGAIN!");
    }

    private void initializeStartupUi() {
        if (startupUiInitialized) {
            return;
        }
        startupUiInitialized = true;
        showIntroDialogueIfNeeded();
    }

    private void showIntroDialogueIfNeeded() {
        if (introDialogueStarted) {
            return;
        }
        introDialogueStarted = true;
        if (session instanceof NetworkIZombieGame) {
            paused = false;
            announcementOverlay.push("I, ZOMBIE ONLINE!");
            return;
        }
        paused = true;
        boolean shown = dialogueController.showMiniGameIntro(
                session.getType(),
                session.getStage(),
                this::finishIntroDialogue
        );
        if (!shown) {
            finishIntroDialogue();
        }
    }

    private void finishIntroDialogue() {
        if (pauseDialog == null && session.isRunning()) {
            paused = false;
        }
        if (session.getType() != MiniGameType.PLANT_ZOMBIES) {
            announcementOverlay.push(miniGameAnnouncement());
        }
    }

    private String miniGameAnnouncement() {
        return switch (session.getType()) {
            case VASEBREAKER -> "VASEBREAKER!";
            case WALLNUT_BOWLING -> "WALL-NUT BOWLING!";
            case I_ZOMBIE -> "I, ZOMBIE!";
            case MATCH_THREE -> "BEGHOULED!";
            case PLANT_ZOMBIES -> "ZOMBOTANY!";
        };
    }

    private void retryMiniGame() {
        if (session instanceof NetworkIZombieGame) {
            returnToNetworkLobby();
            return;
        }
        if (pauseDialog != null) {
            pauseDialog.close();
            pauseDialog = null;
        }
        MiniGameType type = session.getType();
        int stageNumber = session.getStage();
        java.util.List<String> zombotanyPlants = session instanceof ZombotanyGame gameSession
                ? new java.util.ArrayList<>(gameSession.getSelectedPlantNames())
                : java.util.List.of();
        if (controller.hasActiveMiniGame()) {
            controller.abandonMiniGame();
        }
        boolean started = type == MiniGameType.PLANT_ZOMBIES
                ? controller.enterZombotanyMiniGame(stageNumber, zombotanyPlants)
                : controller.enterMiniGame(type.getDisplayName(), stageNumber);
        if (started && controller.wasSuccessful()) {
            game.getScreenManager().showActiveMiniGame();
        }
    }

    private void exitMiniGame() {
        if (session instanceof NetworkIZombieGame) {
            returnToNetworkLobby();
            return;
        }
        if (pauseDialog != null) {
            pauseDialog.close();
            pauseDialog = null;
        }
        if (controller.hasActiveMiniGame()) {
            controller.abandonMiniGame();
        }
        game.getScreenManager().showMiniGames();
    }

    private void updatePointer() {
        updatePointer(Gdx.input.getX(), Gdx.input.getY());
    }

    private void updatePointer(int screenX, int screenY) {
        cursorWorld.set(screenX, screenY);
        stage.getViewport().unproject(cursorWorld);
        hoveredTile = geometry.screenToBoard(cursorWorld.x, cursorWorld.y);
    }

    private void showPauseDialog() {
        if (session instanceof NetworkIZombieGame) {
            confirmLeaveNetworkMatch();
            return;
        }
        if (pauseDialog != null || !session.isRunning()) {
            return;
        }
        paused = true;
        pauseDialog = new PauseDialog(
                game.getSkin(),
                this::resumeFromPause,
                this::retryMiniGame,
                this::exitMiniGame
        );
        pauseDialog.show(stage);
    }

    private void resumeFromPause() {
        if (pauseDialog != null) {
            pauseDialog.close();
            pauseDialog = null;
        }
        paused = false;
    }

    private void refreshNetworkIZombieHud(NetworkIZombieGame networkGame) {
        if (networkTimerLabel != null) {
            long seconds = (networkGame.getRemainingMillis() + 999L) / 1000L;
            networkTimerLabel.setText(String.format(java.util.Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L));
        }
        if (networkStatusLabel != null) {
            String side = networkGame.getRole() == GameRole.PLANTS ? "PLANTS" : "ZOMBIES";
            networkStatusLabel.setText(side + " vs " + networkGame.getOpponent() + "   " + networkGame.getNetworkMessage());
        }
        for (Map.Entry<String, Table> entry : iZombieChoiceCards.entrySet()) {
            Table card = entry.getValue();
            int cost = networkGame.getChoiceCost(entry.getKey());
            long cooldown = networkGame.getCooldownMillis(entry.getKey());
            boolean enabled = session.isRunning() && !networkGame.isActionInFlight()
                    && networkGame.getSunAmount() >= cost && cooldown <= 0L;
            card.setColor(1f, 1f, 1f, enabled ? 1f : 0.58f);
        }
    }

    private void toggleNetworkReactions() {
        if (networkReactionTray != null) {
            networkReactionTray.setVisible(!networkReactionTray.isVisible());
        }
    }

    private void sendNetworkReaction(ReactionCategory category, String value) {
        if (!(session instanceof NetworkIZombieGame networkGame) || networkReactionInFlight) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < networkReactionCooldownUntil) {
            notificationManager.showWarning("Reaction is cooling down.");
            return;
        }
        networkReactionInFlight = true;
        networkReactionCooldownUntil = now + ReactionCatalog.COOLDOWN_MILLIS;
        networkGame.sendReactionAsync(category, value).whenComplete((result, error) -> {
            networkReactionInFlight = false;
            if (networkReactionTray != null) {
                Gdx.app.postRunnable(() -> networkReactionTray.setVisible(false));
            }
        });
    }

    private void drainNetworkReactions(NetworkIZombieGame networkGame) {
        NetworkMessage event;
        while ((event = networkGame.pollReactionEvent()) != null) {
            showNetworkReaction(event);
        }
    }

    private void showNetworkReaction(NetworkMessage event) {
        if (event == null || networkReactionLabel == null || networkReactionGraphic == null) {
            return;
        }
        String category = event.getOrDefault("category", "TEXT");
        String value = event.getOrDefault("value", "");
        networkReactionLabel.clearActions();
        networkReactionLabel.setText(event.getOrDefault("sender", "Opponent")
                + ("TEXT".equals(category) ? ": " + value : " reacted"));
        networkReactionLabel.getColor().a = 1f;
        networkReactionLabel.setVisible(true);
        networkReactionLabel.addAction(Actions.sequence(
                Actions.delay(2.3f), Actions.fadeOut(0.35f), Actions.visible(false)
        ));

        networkReactionGraphic.clearActions();
        networkReactionGraphic.setGraphic(category, value);
        networkReactionGraphic.getColor().a = 1f;
        networkReactionGraphic.setScale(1f);
        networkReactionGraphic.setVisible(true);
        networkReactionGraphic.addAction(Actions.sequence(
                Actions.repeat(3, Actions.sequence(
                        Actions.scaleTo(1.25f, 1.25f, 0.16f),
                        Actions.scaleTo(1f, 1f, 0.16f)
                )),
                Actions.delay(1f), Actions.fadeOut(0.3f), Actions.visible(false)
        ));
    }

    private void confirmLeaveNetworkMatch() {
        if (!(session instanceof NetworkIZombieGame networkGame)) {
            return;
        }
        new ConfirmDialog(
                "Leave Match",
                "Leave this online I, Zombie match?",
                game.getSkin(),
                () -> {
                    networkGame.leaveMatchAsync();
                    returnToNetworkLobby();
                }
        ).show(stage);
    }

    private void returnToNetworkLobby() {
        if (session instanceof NetworkIZombieGame networkGame) {
            networkGame.clearNetworkMatch();
        }
        if (controller.hasActiveMiniGame()) {
            controller.abandonMiniGame();
        }
        game.getScreenManager().showNetworkLobby();
    }

    private String stripPrefix(String message) {
        if (message == null) {
            return "";
        }
        if (message.startsWith("OK: ")) {
            return message.substring(4);
        }
        if (message.startsWith("ERROR: ")) {
            return message.substring(7);
        }
        return message;
    }

    private void enableBlending() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void disableBlending() {
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
