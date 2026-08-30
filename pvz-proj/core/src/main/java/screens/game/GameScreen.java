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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.pvz.Main;
import controllers.core.GameController;
import controllers.features.SettingsController;
import game.animation.core.PvzAnimationService;
import game.chapter.ChapterVisualRenderer;
import game.effects.CombatFeedbackSystem;
import game.effects.ScreenShakeController;
import game.dialogue.LevelDialogueController;
import game.hud.CompactSeedBank;
import game.hud.BossHealthHud;
import game.hud.GameplayWaveBanner;
import game.hud.WaveProgressHud;
import game.input.GameplayInputMode;
import game.input.GameplayInteractionSystem;
import game.input.InteractionOverlayRenderer;
import game.modes.AdventureLevelModeAdapter;
import game.modes.LevelModeAdapter;
import game.notification.GameplayAnnouncementOverlay;
import game.render.BoardBackgroundCatalog;
import game.render.BoardGeometry;
import game.render.BoardRenderer;
import game.render.ChapterBackgroundLayout;
import game.render.boss.BossRenderSystem;
import game.render.drop.PlantFoodDropRenderSystem;
import game.render.entity.EntityRenderSystem;
import game.render.mower.LawnMowerRenderSystem;
import game.render.projectile.ProjectileRenderSystem;
import game.render.sun.SunRenderSystem;
import models.account.PlantData;
import models.account.Settings;
import models.account.User;
import models.core.plant.PlantType;
import models.core.plant.PlantActionTiming;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.session.GameSession;
import models.engine.session.GameState;
import models.engine.session.GroundRewardDrop;
import models.engine.session.PlantFoodDrop;
import models.engine.session.PlantRechargeStatus;
import models.engine.events.GameEvent;
import models.engine.events.GameEventType;
import models.engine.sun.Sun;
import models.level.core.AdventureLevelCatalog;
import models.level.wave.WaveManager;
import screens.BaseScreen;
import ui.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GameScreen extends BaseScreen {
    private static final float BOARD_X = 315f;
    private static final float BOARD_Y = 74f;
    private static final float BOARD_WIDTH = 920f;
    private static final float BOARD_HEIGHT = 457f;
    private static final float BOARD_LEFT_RATIO = BOARD_X / WORLD_WIDTH;
    private static final float BOARD_WIDTH_RATIO = BOARD_WIDTH / WORLD_WIDTH;
    private static final float TICK_SECONDS = 0.1f;
    private static final String SHOVEL_BUTTON_ID = "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON";
    private static final String SHOVEL_BUTTON_DOWN_ID = "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON_DOWN";

    private final GameController controller;
    private final GameSession session;
    private final Settings settings;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final Matrix4 worldTransform;
    private final ScreenShakeController screenShake;
    private final CombatFeedbackSystem combatFeedback;
    private final BoardGeometry boardGeometry;
    private final BoardRenderer boardRenderer;
    private final PvzAnimationService animations;
    private final GameplayClock gameplayClock;
    private final Label statusLabel;
    private final ResourceBar resourceBar;
    private Label sunValueLabel;
    private final Image[] plantFoodSlotIcons;
    private final Table plantCardsTable;
    private final Map<String, PlantCard> gameplayPlantCards;
    private final EntityRenderSystem entityRenderSystem;
    private final ProjectileRenderSystem projectileRenderSystem;
    private final SunRenderSystem sunRenderSystem;
    private final LawnMowerRenderSystem lawnMowerRenderSystem;
    private final PlantFoodDropRenderSystem plantFoodDropRenderSystem;
    private final CompactSeedBank compactSeedBank;
    private final WaveProgressHud waveProgressHud;
    private final BossRenderSystem bossRenderSystem;
    private final BossHealthHud bossHealthHud;
    private final GameplayWaveBanner gameplayWaveBanner;
    private final ChapterVisualRenderer chapterVisualRenderer;
    private final GameplayInteractionSystem interactions;
    private final InteractionOverlayRenderer interactionOverlay;
    private final LevelModeAdapter levelModeAdapter;
    private final GameplayAnnouncementOverlay announcementOverlay;
    private final LevelDialogueController dialogueController;
    private final BossPresentationOverlay bossPresentationOverlay;
    private final Vector2 cursorWorld;
    private Button shovelButton;

    private TextureRegion background;
    private TextureRegion backgroundLeft;
    private TextureRegion backgroundRight;
    private TextureRegion plantFoodDropRegion;
    private TextureRegion plantFoodSlotBackgroundRegion;
    private float backgroundCenterX;
    private Position hoveredTile;
    private float hudRefreshAccumulator;
    private String debugMessage;
    private boolean pausedByLifecycle;
    private boolean resourceBarConfigured;
    private boolean debugControlsVisible;
    private float visualStateTime;
    private PauseDialog pauseDialog;
    private ModalWindow gameOverDialog;
    private boolean gameOverShown;
    private boolean introDialogueStarted;
    private boolean pausedForIntro;
    private boolean bossOutroStarted;
    private int announcedWaveNumber;
    private int announcedUpcomingWave;
    private final int scoreAtLevelStart;
    private boolean mioPointAnnounced;

    public GameScreen(Main game) {
        this(game, prepareController(game));
    }

    public GameScreen(Main game, GameController controller) {
        super(game);
        requireRunningController(controller);
        this.controller = controller;
        session = controller.getGameSession();
        settings = game.getSettingsController().getSettings();
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        worldTransform = new Matrix4();
        screenShake = new ScreenShakeController();
        combatFeedback = new CombatFeedbackSystem(screenShake);
        boardGeometry = new BoardGeometry(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
        boardRenderer = new BoardRenderer(boardGeometry);
        animations = new PvzAnimationService();
        plantFoodDropRegion = animations.region("IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_PLANTFOOD_LARGE");
        plantFoodSlotIcons = new Image[3];
        plantFoodSlotBackgroundRegion = animations.region("IMAGE_UI_POWERUPS_POWERUP_FRAME");
        gameplayClock = new GameplayClock(controller);
        gameplayClock.setGameSpeed(resolveInitialGameSpeed());
        statusLabel = new Label("", game.getSkin());
        cursorWorld = new Vector2();
        resourceBar = new ResourceBar(game.getSkin(), game.getAnimationService());
        plantCardsTable = new Table();
        gameplayPlantCards = new LinkedHashMap<>();
        if (animations.isAvailable()) {
            entityRenderSystem = new EntityRenderSystem(
                boardGeometry,
                animations,
                session.getCurrentLevel() == null ? null : session.getCurrentLevel().getSeasonType()
            );
            projectileRenderSystem = new ProjectileRenderSystem(boardGeometry, animations);
            sunRenderSystem = new SunRenderSystem(boardGeometry, animations);
            plantFoodDropRenderSystem = new PlantFoodDropRenderSystem(boardGeometry, animations);
            lawnMowerRenderSystem = new LawnMowerRenderSystem(
                boardGeometry,
                animations,
                session.getCurrentLevel() == null ? null : session.getCurrentLevel().getSeasonType()
            );
            compactSeedBank = new CompactSeedBank(animations, game.getSkin());
        } else {
            entityRenderSystem = null;
            projectileRenderSystem = null;
            sunRenderSystem = null;
            lawnMowerRenderSystem = null;
            plantFoodDropRenderSystem = null;
            compactSeedBank = null;
        }
        waveProgressHud = new WaveProgressHud(animations);
        if (animations.isAvailable() && session.getCurrentLevel() != null
                && session.getCurrentLevel().getBossRuntime() != null) {
            bossRenderSystem = new BossRenderSystem(
                    boardGeometry, animations, session.getCurrentLevel().getBossRuntime()
            );
            bossHealthHud = new BossHealthHud(
                    animations, session.getCurrentLevel().getBossRuntime()
            );
        } else {
            bossRenderSystem = null;
            bossHealthHud = null;
        }
        gameplayWaveBanner = new GameplayWaveBanner(animations, game.getSkin());
        interactionOverlay = new InteractionOverlayRenderer(boardGeometry, animations);
        chapterVisualRenderer = new ChapterVisualRenderer(
            session.getCurrentLevel(),
            boardGeometry,
            animations,
            stage.getCamera()
        );
        interactions = new GameplayInteractionSystem(
            controller,
            game.getAuthController().getLoggedInUser()
        );
        levelModeAdapter = new AdventureLevelModeAdapter(
            controller,
            boardGeometry,
            shapes,
            stage,
            game.getSkin()
        );
        announcementOverlay = new GameplayAnnouncementOverlay(stage, game.getSkin());
        dialogueController = new LevelDialogueController(stage, game.getSkin(), animations);
        buildHud();
        buildInteractionControls();
        bossPresentationOverlay = session.getCurrentLevel() != null
                && session.getCurrentLevel().getBossRuntime() != null
                ? new BossPresentationOverlay(
                        stage,
                        game.getSkin(),
                        animations,
                        session.getCurrentLevel().getBossRuntime()
                )
                : null;
        levelModeAdapter.setup();
        loadStageAssets();
        debugMessage = "Adventure session connected";
        pauseDialog = null;
        gameOverDialog = null;
        gameOverShown = false;
        introDialogueStarted = false;
        pausedForIntro = false;
        bossOutroStarted = false;
        announcedWaveNumber = 0;
        announcedUpcomingWave = 0;
        User scoreUser = game.getAuthController().getLoggedInUser();
        scoreAtLevelStart = scoreUser == null ? 0 : scoreUser.getScore();
        mioPointAnnounced = false;
        refreshGameHud();
        refreshStatus(debugMessage);
    }

    @Override
    public void show() {
        super.show();
        applyStoredGameSpeed();
        refreshGameHud();
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, createInput()));
        showLevelIntroIfNeeded();
    }

    @Override
    public void render(float delta) {
        updateRuntime(delta);
        Gdx.gl.glClearColor(0.08f, 0.12f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);
        shapes.setProjectionMatrix(stage.getCamera().combined);
        applyWorldShake();
        drawBackground();
        drawChapterBehindEntities();
        levelModeAdapter.renderOverlay();
        drawInteractionTileHighlight();
        drawLawnMowers();
        drawEntities();
        drawBoss();
        drawProjectiles();
        drawSuns();
        drawGroundRewards();
        drawPlantFoodDrops();
        drawChapterAboveEntities();
        drawHover();
        resetWorldTransform();
        drawSeedBank();
        drawInteractionCursor();
        drawWaveNotification();
        syncInteractionControlState();
        UiHoverAnimator.attach(stage);
        float stageDelta = gameplayClock.isPaused() ? 0f : Math.min(delta, 1f / 15f);
        stage.act(stageDelta);
        stage.draw();
        drawWaveProgress();
    }

    @Override
    public void pause() {
        pausedByLifecycle = session.isRunning() && !gameplayClock.isPaused();
        if (pausedByLifecycle) {
            gameplayClock.togglePause();
            refreshStatus("Application paused");
        }
    }

    @Override
    public void resume() {
        if (pausedByLifecycle && session.isRunning() && gameplayClock.isPaused()) {
            gameplayClock.togglePause();
            refreshStatus("Application resumed");
        }
        pausedByLifecycle = false;
    }

    @Override
    public void dispose() {
        levelModeAdapter.dispose();
        super.dispose();
        animations.dispose();
        batch.dispose();
        shapes.dispose();
    }

    public Vector2 boardToScreen(int row, int column) {
        return boardGeometry.boardToScreen(row, column);
    }

    public Position screenToBoard(float x, float y) {
        return boardGeometry.screenToBoard(x, y);
    }

    public Rectangle getTileBounds(int row, int column) {
        return boardGeometry.getTileBounds(row, column);
    }

    private static GameController prepareController(Main game) {
        AdventureSessionBootstrap.prepareAndStart(game);
        return game.getGameController();
    }

    private void requireRunningController(GameController value) {
        if (value == null || value.getGameSession() == null || !value.getGameSession().isRunning()) {
            throw new IllegalArgumentException("GameScreen requires a running Adventure session.");
        }
        if (value.getGameSession().getBoard() == null) {
            throw new IllegalArgumentException("GameScreen requires an initialized board.");
        }
    }

    private int resolveInitialGameSpeed() {
        if (settings == null) {
            return Settings.MIN_GAME_SPEED;
        }
        return Math.max(
            Settings.MIN_GAME_SPEED,
            Math.min(Settings.MAX_GAME_SPEED, settings.getGameSpeed())
        );
    }

    private void applyStoredGameSpeed() {
        int speed = resolveInitialGameSpeed();
        if (gameplayClock.getGameSpeed() != speed) {
            gameplayClock.setGameSpeed(speed);
        }
    }

    private void buildHud() {
        statusLabel.setVisible(false);

        Table topLeft = new Table();
        topLeft.setFillParent(true);
        topLeft.top().left().padTop(10f).padLeft(12f);
        topLeft.add(createSunCounter()).width(150f).height(50f);
        stage.addActor(topLeft);

        Table topRight = new Table();
        topRight.setFillParent(true);
        topRight.top().right().padTop(10f).padRight(10f);
        topRight.add(resourceBar).right().top().padRight(8f);
        topRight.add(createPauseButton()).size(54f).top();
        stage.addActor(topRight);
    }

    private Table createSunCounter() {
        Table counter = new Table();
        TextureRegion backgroundRegion = game.getAnimationService().region(
                "IMAGE_UI_GENERIC_BUTTON_GENERIC_CURRENCY_NORMAL"
        );
        if (backgroundRegion != null) {
            counter.setBackground(new TextureRegionDrawable(backgroundRegion));
        }
        TextureRegion sunRegion = game.getAnimationService().region("IMAGE_UI_HUD_INGAME_SUN_DOWN");
        if (sunRegion != null) {
            Image sunIcon = new Image(sunRegion);
            sunIcon.setScaling(Scaling.fit);
            counter.add(sunIcon).size(38f).padLeft(6f).padRight(5f);
        }
        sunValueLabel = createGameplayResourceLabel("0");
        counter.add(sunValueLabel).expandX().center().padRight(10f);
        counter.setTouchable(Touchable.enabled);
        counter.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isDebugMode()) {
                    addDebugSun();
                }
            }
        });
        return counter;
    }

    private Label createGameplayResourceLabel(String text) {
        Label.LabelStyle style = new Label.LabelStyle(
                game.getSkin().getFont("FBUSV8C5EI_2_outline"), Color.WHITE
        );
        Label label = new Label(text, style);
        label.setColor(Color.WHITE);
        label.setFontScale(0.72f);
        label.setAlignment(Align.center);
        return label;
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

    private void buildInteractionControls() {
        Table shovelControls = new Table();
        shovelControls.setFillParent(true);
        shovelControls.bottom().right().padRight(225f).padBottom(14f);
        shovelButton = createShovelButton();
        shovelControls.add(shovelButton).width(76f).height(76f);
        stage.addActor(shovelControls);

        Table plantFoodControls = new Table();
        plantFoodControls.setFillParent(true);
        plantFoodControls.bottom().left().padLeft(225f).padBottom(14f);
        plantFoodControls.add(createPlantFoodCounter()).width(190f).height(57f);
        stage.addActor(plantFoodControls);
    }

    private Stack createPlantFoodCounter() {
        Stack counter = new Stack();

        if (plantFoodSlotBackgroundRegion != null) {
            Image backgroundImage = new Image(plantFoodSlotBackgroundRegion);
            backgroundImage.setScaling(Scaling.fill);
            counter.add(backgroundImage);
        }

        TextureRegion plantFoodRegion = game.getAnimationService().region(
                "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_PLANTFOOD_LARGE"
        );
        Table slots = new Table();
        slots.setFillParent(true);
        slots.top().left();
        slots.padLeft(14f).padRight(14f).padTop(1f).padBottom(15f);
        for (int index = 0; index < plantFoodSlotIcons.length; index++) {
            Image icon = plantFoodRegion == null ? new Image() : new Image(plantFoodRegion);
            icon.setScaling(Scaling.fit);
            icon.setVisible(false);
            plantFoodSlotIcons[index] = icon;
            slots.add(icon).expandX().size(42f).top();
        }
        counter.add(slots);

        counter.setTouchable(Touchable.enabled);
        counter.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectPlantFood();
            }
        });
        return counter;
    }

    private Button createShovelButton() {
        TextureRegion normal = animations.region(SHOVEL_BUTTON_ID);
        TextureRegion pressed = animations.region(SHOVEL_BUTTON_DOWN_ID);
        if (normal == null || pressed == null) {
            return new MenuButton("Shovel [S]", game.getSkin(), "green_small", this::selectShovel);
        }
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = new TextureRegionDrawable(normal);
        style.down = new TextureRegionDrawable(pressed);
        style.checked = new TextureRegionDrawable(pressed);
        ImageButton button = new ImageButton(style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectShovel();
            }
        });
        return button;
    }

    private void buildPlantCardHud() {
        plantCardsTable.top();
        plantCardsTable.defaults().padBottom(8f);
        for (PlantRechargeStatus status : session.getPlantRechargeStatuses()) {
            if (!status.isSelected()) {
                continue;
            }
            addGameplayPlantCard(status);
        }
        ScrollPane scrollPane = new ScrollPane(plantCardsTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setOverscroll(false, false);
        Table host = new Table();
        host.setFillParent(true);
        host.left().bottom().padLeft(10f).padBottom(12f);
        host.add(scrollPane).width(290f).height(530f);
        stage.addActor(host);
    }

    private void addGameplayPlantCard(PlantRechargeStatus status) {
        PlantCard card = createGameplayPlantCard(status);
        gameplayPlantCards.put(status.getPlantName(), card);
        plantCardsTable.add(card).width(270f).row();
    }

    private PlantCard createGameplayPlantCard(PlantRechargeStatus status) {
        PlantCard card = new PlantCard(game.getSkin());
        PlantType type = session.getPlantType(status.getPlantName());
        PlantData data = getPlantData(status.getPlantName());
        card.setName(status.getPlantName());
        card.setCost(status.getSunCost());
        card.setLevel(data == null ? 1 : data.getLevel());
        setSeedProgress(card, data);
        if (type != null) {
            card.setFamily(type.getCategory());
            card.setTags(type.getTags());
            card.setHealth(type.getBaseHp());
        }
        card.setLocked(data != null && !data.isUnlocked());
        card.setBoosted(data != null && data.getBoostCount() > 0);
        card.setSelected(status.isSelected());
        card.setCooldown(status.getRemainingTicks() * TICK_SECONDS);
        card.setPlantActor(game.getAnimationService().createPlantActor(status.getPlantName()));
        card.clearAction();
        return card;
    }

    private void setSeedProgress(PlantCard card, PlantData data) {
        if (data == null) {
            card.setSeedPacketProgress(0, 10);
            return;
        }
        card.setSeedPacketProgress(
            data.getSeedPackets(),
            data.getRequiredSeedPacketsForNextLevel()
        );
    }

    private PlantData getPlantData(String plantName) {
        User user = game.getAuthController().getLoggedInUser();
        if (user == null || user.getCollection() == null) {
            return null;
        }
        return user.getCollection().findPlant(plantName);
    }

    private void loadStageAssets() {
        if (!animations.isAvailable()) {
            return;
        }
        BoardBackgroundCatalog.BackgroundResources resources =
            BoardBackgroundCatalog.resources(session.getCurrentLevel());
        backgroundLeft = animations.region(resources.leftId());
        background = animations.region(resources.centerId());
        backgroundRight = animations.region(resources.rightId());
        updateBackgroundLayout();
    }

    private void updateBackgroundLayout() {
        if (background == null) {
            backgroundCenterX = 0f;
            boardGeometry.setBounds(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
            return;
        }

        float centerWidth = BoardRenderer.scaledWidth(background, WORLD_HEIGHT);
        backgroundCenterX = BOARD_X - centerWidth * BOARD_LEFT_RATIO;
        float correctedBoardWidth = centerWidth * BOARD_WIDTH_RATIO;
        boardGeometry.setBounds(BOARD_X, BOARD_Y, correctedBoardWidth, BOARD_HEIGHT);
    }

    private void updateRuntime(float delta) {
        applyStoredGameSpeed();
        animations.update();
        updateCursorWorld();
        gameplayClock.update(delta);
        levelModeAdapter.update();
        int currentTick = gameplayClock.getCurrentTick();
        float visualDelta = gameplayClock.isPaused() ? 0f : delta * gameplayClock.getGameSpeed();
        visualStateTime += visualDelta;
        waveProgressHud.update(session);
        gameplayWaveBanner.update(visualDelta, session);
        updateRenderSystems(visualDelta, currentTick);
        if (bossPresentationOverlay != null) {
            bossPresentationOverlay.update();
        }
        combatFeedback.update(visualDelta, session.getBoard());
        collectSunUnderPointer();
        collectGroundRewardUnderPointer();
        if (!session.isRunning() && interactions.isActive()) {
            interactions.cancel();
        }
        refreshHud(delta);
        drainGameplayAnnouncements();
        showGameOverIfNeeded();
    }

    private void updateRenderSystems(float visualDelta, int currentTick) {
        Board board = session.getBoard();
        if (entityRenderSystem != null) {
            entityRenderSystem.update(visualDelta, board);
        }
        if (bossRenderSystem != null) {
            bossRenderSystem.update(visualDelta);
        }
        if (projectileRenderSystem != null) {
            projectileRenderSystem.observe(board, currentTick);
            projectileRenderSystem.update(visualDelta);
        }
        if (lawnMowerRenderSystem != null) {
            lawnMowerRenderSystem.update(visualDelta, board);
        }
        if (sunRenderSystem != null) {
            sunRenderSystem.update(visualDelta, session.getSunManager());
        }
        if (plantFoodDropRenderSystem != null) {
            plantFoodDropRenderSystem.update(visualDelta);
        }
        chapterVisualRenderer.update(visualDelta);
    }

    private void refreshHud(float delta) {
        hudRefreshAccumulator += delta;
        if (hudRefreshAccumulator >= 0.1f) {
            hudRefreshAccumulator = 0f;
            refreshGameHud();
            refreshStatus(debugMessage);
        }
    }

    private void refreshGameHud() {
        User user = game.getAuthController().getLoggedInUser();
        boolean debug = isDebugMode();
        if (!resourceBarConfigured || debugControlsVisible != debug) {
            resourceBar.setDebugControls(
                debug,
                this::addDebugCoins,
                this::addDebugDiamonds
            );
            resourceBarConfigured = true;
            debugControlsVisible = debug;
        }
        resourceBar.refresh(user);
        if (sunValueLabel != null) {
            sunValueLabel.setText(String.valueOf(session.getTotalSunAmount()));
        }
        int plantFoodCount = Math.max(0, Math.min(plantFoodSlotIcons.length, session.getPlantFoodCount()));
        for (int index = 0; index < plantFoodSlotIcons.length; index++) {
            if (plantFoodSlotIcons[index] != null) {
                plantFoodSlotIcons[index].setVisible(index < plantFoodCount);
            }
        }
        refreshGameplayPlantCards();
    }

    private void refreshGameplayPlantCards() {
        for (PlantRechargeStatus status : session.getPlantRechargeStatuses()) {
            PlantCard card = gameplayPlantCards.get(status.getPlantName());
            if (status.isSelected() && card == null) {
                addGameplayPlantCard(status);
                card = gameplayPlantCards.get(status.getPlantName());
            }
            if (card == null) {
                continue;
            }
            card.setSelected(status.isSelected());
            card.setCooldown(status.getRemainingTicks() * TICK_SECONDS);
        }
    }

    private void addDebugCoins() {
        SettingsController settingsController = game.getSettingsController();
        settingsController.addDebugCoins(1000);
        showSettingsMessage(settingsController);
        refreshGameHud();
    }

    private void addDebugDiamonds() {
        SettingsController settingsController = game.getSettingsController();
        settingsController.addDebugDiamonds(10);
        showSettingsMessage(settingsController);
        refreshGameHud();
    }

    private void addDebugSun() {
        SettingsController settingsController = game.getSettingsController();
        settingsController.addDebugSun(controller, 250);
        showSettingsMessage(settingsController);
        refreshGameHud();
    }

    private void addDebugPlantFood() {
        SettingsController settingsController = game.getSettingsController();
        settingsController.addDebugPlantFood(controller);
        showSettingsMessage(settingsController);
        refreshGameHud();
    }

    private void showSettingsMessage(SettingsController settingsController) {
        String message = settingsController.getLastMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        refreshStatus(stripMessagePrefix(message));
    }

    private String stripMessagePrefix(String message) {
        int separator = message.indexOf(':');
        if (separator < 0 || separator + 1 >= message.length()) {
            return message;
        }
        return message.substring(separator + 1).trim();
    }

    private void drawBackground() {
        batch.begin();
        boardRenderer.drawBackground(
            batch,
            backgroundLeft,
            background,
            backgroundRight,
            WORLD_HEIGHT,
            backgroundCenterX,
            centerBackgroundYOffset()
        );
        batch.end();
        enableAlphaBlending();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        boardRenderer.drawBoardFill(shapes, background != null);
        shapes.end();
        disableAlphaBlending();
    }


    private float centerBackgroundYOffset() {
        return session.getCurrentLevel() == null
            ? 0f
            : ChapterBackgroundLayout.centerYOffset(session.getCurrentLevel().getSeasonType());
    }

    private void drawGrid() {
    }

    private void drawSeedBank() {
        if (compactSeedBank == null) {
            return;
        }
        enableAlphaBlending();
        compactSeedBank.render(
            shapes,
            batch,
            session,
            visualStateTime,
            interactions.getSelectedPlantName(),
            controller::isPlantBoostedForGameplay
        );
        disableAlphaBlending();
    }


    private void drawChapterBehindEntities() {
        enableAlphaBlending();
        chapterVisualRenderer.renderBehindEntities(batch, shapes);
        disableAlphaBlending();
    }

    private void drawChapterAboveEntities() {
        enableAlphaBlending();
        chapterVisualRenderer.renderAboveEntities(batch, shapes);
        disableAlphaBlending();
    }

    private void drawInteractionTileHighlight() {
        if (interactionOverlay == null || hoveredTile == null || !interactions.isActive()) {
            return;
        }
        enableAlphaBlending();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        interactionOverlay.drawTileHighlight(shapes, interactions, hoveredTile);
        shapes.end();
        disableAlphaBlending();
    }

    private void drawInteractionCursor() {
        if (interactionOverlay == null || !interactions.isActive()) {
            return;
        }
        batch.begin();
        interactionOverlay.drawPlantGhost(
            batch,
            session,
            interactions,
            cursorWorld.x,
            cursorWorld.y,
            visualStateTime
        );
        interactionOverlay.drawSpriteToolCursor(
            batch,
            interactions,
            cursorWorld.x,
            cursorWorld.y
        );
        batch.end();
        enableAlphaBlending();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        interactionOverlay.drawToolCursor(shapes, interactions, cursorWorld.x, cursorWorld.y);
        shapes.end();
        disableAlphaBlending();
    }

    private void drawEntities() {
        if (entityRenderSystem != null) {
            entityRenderSystem.render(batch, session.getBoard());
        }
    }

    private void drawBoss() {
        if (bossRenderSystem != null) {
            bossRenderSystem.render(batch);
        }
    }

    private void drawProjectiles() {
        if (projectileRenderSystem != null) {
            projectileRenderSystem.render(batch);
        }
    }

    private void drawSuns() {
        if (sunRenderSystem != null) {
            sunRenderSystem.render(batch, session.getSunManager(), visualStateTime);
        }
    }

    private void drawGroundRewards() {
        if (plantFoodDropRegion == null || session.getGroundRewardDrops().isEmpty()) {
            return;
        }
        batch.begin();
        for (GroundRewardDrop drop : session.getGroundRewardDrops()) {
            if (!drop.getType().equals("plant_food")) {
                continue;
            }
            Vector2 center = boardGeometry.entityToScreen(drop.getPosition().getX(), drop.getPosition().getY());
            float size = boardGeometry.getTileHeight() * 0.48f;
            float alpha = Math.min(1f, drop.getRemainingTicks() / 5f);
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(plantFoodDropRegion, center.x - size / 2f, center.y - size / 2f, size, size);
        }
        batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        batch.end();
    }

    private void drawPlantFoodDrops() {
        if (plantFoodDropRenderSystem != null) {
            plantFoodDropRenderSystem.render(batch, session);
        }
    }

    private void drawLawnMowers() {
        if (lawnMowerRenderSystem != null) {
            lawnMowerRenderSystem.render(batch, session.getBoard());
        }
    }

    private void drawWaveProgress() {
        batch.begin();
        if (bossHealthHud != null) {
            bossHealthHud.render(batch, WORLD_WIDTH, WORLD_HEIGHT);
        } else {
            waveProgressHud.render(batch, session, WORLD_WIDTH, WORLD_HEIGHT);
        }
        batch.end();
    }

    private void drawWaveNotification() {
        batch.begin();
        gameplayWaveBanner.render(batch, WORLD_WIDTH, WORLD_HEIGHT, visualStateTime);
        batch.end();
    }

    private void drawHover() {
        if (!isDebugMode() || hoveredTile == null || interactions.isActive()) {
            return;
        }
        enableAlphaBlending();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        boardRenderer.drawHover(shapes, hoveredTile);
        shapes.end();
        disableAlphaBlending();
    }

    private void collectSunUnderPointer() {
        if (sunRenderSystem == null || gameplayClock.isPaused() || !session.isRunning()
                || (pauseDialog != null && pauseDialog.getStage() != null) || gameOverShown) {
            return;
        }
        Vector2 world = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(world);
        Sun sun = sunRenderSystem.findHoveredSun(
            world.x,
            world.y,
            session.getSunManager()
        );
        if (sun == null) {
            return;
        }
        controller.collectSun(sun.getPosition());
        if (controller.wasSuccessful()) {
            refreshGameHud();
        }
    }

    private void collectGroundRewardUnderPointer() {
        if (gameplayClock.isPaused() || !session.isRunning()
                || (pauseDialog != null && pauseDialog.getStage() != null) || gameOverShown) {
            return;
        }
        Vector2 world = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        stage.getViewport().unproject(world);
        float radius = boardGeometry.getTileWidth() * 0.30f;
        for (GroundRewardDrop drop : new ArrayList<>(session.getGroundRewardDrops())) {
            Vector2 center = boardGeometry.entityToScreen(drop.getPosition().getX(), drop.getPosition().getY());
            if (Vector2.dst2(world.x, world.y, center.x, center.y) > radius * radius) {
                continue;
            }
            if (session.collectGroundReward(drop.getId())) {
                refreshGameHud();
            }
            return;
        }
    }

    private void applyWorldShake() {
        worldTransform.idt().translate(
            screenShake.getOffsetX(),
            screenShake.getOffsetY(),
            0f
        );
        batch.setTransformMatrix(worldTransform);
        shapes.setTransformMatrix(worldTransform);
    }

    private void resetWorldTransform() {
        worldTransform.idt();
        batch.setTransformMatrix(worldTransform);
        shapes.setTransformMatrix(worldTransform);
    }

    private void enableAlphaBlending() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void disableAlphaBlending() {
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private InputAdapter createInput() {
        return new InputAdapter() {
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                updateHoveredTile(screenX, screenY);
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                updatePointer(screenX, screenY);
                if (button == Input.Buttons.RIGHT && interactions.isActive()) {
                    cancelInteraction();
                    return true;
                }
                if (button == Input.Buttons.LEFT && handlePlantFoodDropClick()) {
                    return true;
                }
                if (button == Input.Buttons.LEFT && handleSeedBankClick()) {
                    return true;
                }
                return handleBoardClick(button);
            }

            @Override
            public boolean keyDown(int keycode) {
                return handleKey(keycode);
            }
        };
    }

    private boolean handlePlantFoodDropClick() {
        if (plantFoodDropRenderSystem == null || gameplayClock.isPaused() || !session.isRunning()
                || (pauseDialog != null && pauseDialog.getStage() != null) || gameOverShown) {
            return false;
        }
        PlantFoodDrop drop = plantFoodDropRenderSystem.findAt(
            session, cursorWorld.x, cursorWorld.y
        );
        if (drop == null) {
            return false;
        }
        if (!session.collectPlantFoodDrop(drop)) {
            refreshStatus("Plant Food storage is full.");
            return true;
        }
        plantFoodDropRenderSystem.playCollection(drop);
        refreshGameHud();
        refreshStatus("Plant Food collected.");
        return true;
    }

    private boolean handleBoardClick(int button) {
        if (pauseDialog != null && pauseDialog.getStage() != null) {
            return false;
        }
        if (gameOverShown || gameplayClock.isPaused() || !session.isRunning()) {
            return false;
        }
        if (button != Input.Buttons.LEFT || hoveredTile == null) {
            return false;
        }
        GameplayInputMode inputMode = interactions.getMode();
        String selectedPlantName = interactions.getSelectedPlantName();
        Position targetPosition = hoveredTile;
        if (interactions.handleTileClick(targetPosition)) {
            playImmediatePlantVisual(inputMode, selectedPlantName, targetPosition);
            showInteractionResult();
            refreshGameHud();
            return true;
        }
        controller.handleUserClick(hoveredTile);
        String tile = "Clicked tile row=" + hoveredTile.getY() + ", column=" + hoveredTile.getX();
        Gdx.app.log("GameScreen", tile);
        if (isDebugMode()) {
            refreshStatus(controller.getLastMessage());
        }
        refreshGameHud();
        return true;
    }

    private void playImmediatePlantVisual(
        GameplayInputMode inputMode,
        String plantName,
        Position position
    ) {
        if (entityRenderSystem == null
            || inputMode != GameplayInputMode.PLANTING
            || !interactions.wasSuccessful()
            || plantName == null
            || position == null) {
            return;
        }
        PlantType type = session.getPlantType(plantName);
        if (type == null || !isImmediatePlantVisual(plantName)) {
            return;
        }
        entityRenderSystem.playPlantAction(
            type,
            position,
            PlantActionTiming.immediateActionClip(plantName)
        );
        playImmediateFieldEffects(plantName, position);
        combatFeedback.onImmediatePlant(plantName);
    }

    private boolean isImmediatePlantVisual(String plantName) {
        String normalized = plantName == null ? "" : plantName.trim().toLowerCase()
            .replace('-', ' ').replace('_', ' ').replaceAll("\s+", " ");
        return normalized.equals("gold bloom")
            || normalized.equals("cherry bomb")
            || normalized.equals("grapeshot")
            || normalized.equals("jalapeno")
            || normalized.equals("doom shroom")
            || normalized.equals("ice shroom")
            || normalized.equals("hot potato")
            || normalized.equals("grave buster")
            || normalized.endsWith(" mint");
    }

    private void playImmediateFieldEffects(String plantName, Position position) {
        String normalized = plantName.trim().toLowerCase()
            .replace('-', ' ').replace('_', ' ').replaceAll("\s+", " ");
        float delay = PlantActionTiming.specialImpactTicks(plantName) / 10f;
        if (normalized.equals("cherry bomb")) {
            entityRenderSystem.playFieldEffect(
                "768/FULL/EFFECTS/CHERRYBOMB_EXPLOSION_REAR/CHERRYBOMB_EXPLOSION_REAR.PAM",
                "explosion", java.util.Collections.singletonList(position), 0.58f, delay, 0f, false
            );
            entityRenderSystem.playFieldEffect(
                "768/FULL/EFFECTS/CHERRYBOMB_EXPLOSION_TOP/CHERRYBOMB_EXPLOSION_TOP.PAM",
                "explosion", java.util.Collections.singletonList(position), 0.58f, delay, 0f, false
            );
        } else if (normalized.equals("grapeshot")) {
            entityRenderSystem.playFieldEffect(
                "768/INITIAL/EFFECTS/ESCAPEROOT_EXPLOSION_GRAPESHOT/ESCAPEROOT_EXPLOSION_GRAPESHOT.PAM",
                "animation", java.util.Collections.singletonList(position), 0.60f, delay, 0f, false
            );
        } else if (normalized.equals("grave buster")) {
            entityRenderSystem.playFieldEffect(
                "768/INITIAL/EFFECTS/GRAVEBUSTER_DIRT/GRAVEBUSTER_DIRT.PAM",
                "gravebuster_dirt_anim", java.util.Collections.singletonList(position), 0.48f, delay, 0f, false
            );
        } else if (normalized.equals("jalapeno")) {
            List<Position> lane = new ArrayList<>();
            for (int column = 1; column <= session.getBoard().getWidth(); column++) {
                lane.add(new Position(column, position.getY()));
            }
            entityRenderSystem.playFieldEffect(
                "768/INITIAL/EFFECTS/JALAPENO_FIRE/JALAPENO_FIRE.PAM",
                "idle2", lane, 0.50f, delay, 1.33f, true
            );
        } else if (normalized.equals("ice shroom")) {
            List<Position> tiles = new ArrayList<>();
            for (int row = 1; row <= session.getBoard().getHeight(); row++) {
                for (int column = 1; column <= session.getBoard().getWidth(); column++) {
                    tiles.add(new Position(column, row));
                }
            }
            entityRenderSystem.playFieldEffect(
                "768/FULL/EFFECTS/ICESHROOM_TILE_FX/ICESHROOM_TILE_FX.PAM",
                "spawn", tiles, 0.46f, delay, 0f, false
            );
        } else if (normalized.equals("hot potato")) {
            entityRenderSystem.playFieldEffect(
                "768/FULL/EFFECTS/HOTPOTATO_ICEBLOCK_STEAMFX/HOTPOTATO_ICEBLOCK_STEAMFX.PAM",
                "animation", java.util.Collections.singletonList(position), 0.48f, delay, 0f, false
            );
        }

    }

    private void updateHoveredTile(int screenX, int screenY) {
        updatePointer(screenX, screenY);
    }

    private void updatePointer(int screenX, int screenY) {
        cursorWorld.set(screenX, screenY);
        stage.getViewport().unproject(cursorWorld);
        hoveredTile = screenToBoard(cursorWorld.x, cursorWorld.y);
    }

    private void updateCursorWorld() {
        updatePointer(Gdx.input.getX(), Gdx.input.getY());
    }

    private boolean handleSeedBankClick() {
        if (compactSeedBank == null) {
            return false;
        }
        String plantName = compactSeedBank.findPlantAt(session, cursorWorld.x, cursorWorld.y);
        if (plantName == null) {
            return false;
        }
        if (isConveyorLevel()
                && plantName.equalsIgnoreCase(interactions.getSelectedPlantName())) {
            interactions.cancel();
        }
        boolean selected = interactions.selectPlant(plantName);
        showInteractionResult();
        return true;
    }

    private boolean isConveyorLevel() {
        return session.getCurrentLevel() != null && session.getCurrentLevel().usesConveyorBelt();
    }

    private boolean handleKey(int keycode) {
        if (keycode == Input.Keys.S) {
            selectShovel();
            return true;
        }
        if (keycode == Input.Keys.F) {
            if (isDebugMode()) {
                addDebugPlantFood();
            } else {
                selectPlantFood();
            }
            return true;
        }
        if (keycode == Input.Keys.P || keycode == Input.Keys.SPACE) {
            gameplayClock.togglePause();
            refreshStatus(gameplayClock.isPaused() ? "Paused" : "Resumed");
            return true;
        }
        if (keycode == Input.Keys.NUM_1 || keycode == Input.Keys.NUM_2 || keycode == Input.Keys.NUM_3) {
            int speed = keycode == Input.Keys.NUM_1 ? 1 : keycode == Input.Keys.NUM_2 ? 2 : 3;
            setSpeed(speed);
            return true;
        }
        if (keycode == Input.Keys.ESCAPE) {
            if (interactions.isActive()) {
                cancelInteraction();
                return true;
            }
            game.getScreenManager().showMainMenu();
            return true;
        }
        return false;
    }

    private void togglePauseMenu() {
        if (gameOverShown || !session.isRunning()) {
            return;
        }
        if (pauseDialog != null && pauseDialog.getStage() != null) {
            resumeFromPause();
            return;
        }
        showPauseDialog();
    }

    private void showPauseDialog() {
        if (gameOverShown || !session.isRunning()) {
            return;
        }
        if (pauseDialog != null && pauseDialog.getStage() != null) {
            return;
        }
        if (!gameplayClock.isPaused()) {
            gameplayClock.togglePause();
        }
        interactions.cancel();
        pauseDialog = new PauseDialog(
                game.getSkin(),
                this::resumeFromPause,
                this::restartCurrentLevel,
                this::saveAndExit
        );
        pauseDialog.show(stage);
        refreshStatus("Paused");
    }

    private void resumeFromPause() {
        if (pauseDialog != null) {
            pauseDialog.close();
            pauseDialog = null;
        }
        if (session.isRunning() && gameplayClock.isPaused()) {
            gameplayClock.togglePause();
        }
        refreshStatus("Resumed");
    }

    private void restartCurrentLevel() {
        if (pauseDialog != null) {
            pauseDialog.close();
            pauseDialog = null;
        }
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            game.getScreenManager().showMainMenu();
            return;
        }
        String chapter = user.getCurrentChapterName();
        int levelNumber = user.getCurrentChapterLevel();
        List<String> selectedPlants = new ArrayList<>(session.getSelectedPlantNames());
        controller.prepareChapterLevel(chapter, levelNumber);
        if (!controller.wasSuccessful()) {
            game.getScreenManager().showAdventure();
            return;
        }
        if (!controller.shouldAutoStartCurrentLevel()) {
            for (String plantName : selectedPlants) {
                controller.addPlantToSelection(plantName);
            }
        }
        controller.startGame();
        if (!controller.wasSuccessful()) {
            game.getScreenManager().showAdventureMission(chapter, levelNumber);
            return;
        }
        game.getScreenManager().showPreparedGame();
    }

    private void saveAndExit() {
        if (pauseDialog != null) {
            pauseDialog.close();
            pauseDialog = null;
        }
        game.getAuthController().saveUsers();
        game.getScreenManager().showAdventure();
    }

    private void showLevelIntroIfNeeded() {
        if (introDialogueStarted) {
            return;
        }
        introDialogueStarted = true;
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            announcementOverlay.push("READY... SET... PLANT!");
            return;
        }
        if (session.isRunning() && !gameplayClock.isPaused()) {
            gameplayClock.togglePause();
            pausedForIntro = true;
        }
        boolean shown = dialogueController.showIntro(
                user.getCurrentChapterName(),
                user.getCurrentChapterLevel(),
                this::finishLevelIntro
        );
        if (!shown) {
            finishLevelIntro();
        }
    }

    private void finishLevelIntro() {
        if (bossPresentationOverlay != null) {
            bossPresentationOverlay.showIntro(this::startBossBattleAfterIntro);
            return;
        }
        resumeAfterLevelIntro();
    }

    private void startBossBattleAfterIntro() {
        resumeAfterLevelIntro();
    }

    private void resumeAfterLevelIntro() {
        if (pausedForIntro && session.isRunning() && gameplayClock.isPaused()) {
            gameplayClock.togglePause();
        }
        pausedForIntro = false;
        announcementOverlay.push("READY... SET... PLANT!");
    }

    private void drainGameplayAnnouncements() {
        if (session.getCurrentLevel() == null) {
            return;
        }
        WaveManager waveManager = session.getCurrentLevel().getWaveManager();
        int currentWave = waveManager.getCurrentWaveNumber();
        int currentTick = session.getTickManager().getCurrentTick();
        int ticksUntilNext = waveManager.getTicksUntilNextWave(currentTick);
        if (ticksUntilNext >= 0 && ticksUntilNext <= 10 && waveManager.getNextWave() != null) {
            int upcoming = waveManager.getNextWave().getWaveNumber();
            if (upcoming > announcedUpcomingWave) {
                announcedUpcomingWave = upcoming;
                if (upcoming == waveManager.getTotalWaves()) {
                    announcementOverlay.push("A HUGE WAVE OF ZOMBIES IS APPROACHING!");
                } else if (upcoming == 1) {
                    announcementOverlay.push("ZOMBIES ARE COMING!");
                } else {
                    announcementOverlay.push("WAVE " + upcoming + " INCOMING!");
                }
            }
        }
        if (currentWave > announcedWaveNumber) {
            announcedWaveNumber = currentWave;
            if (currentWave > announcedUpcomingWave) {
                announcedUpcomingWave = currentWave;
                if (currentWave == waveManager.getTotalWaves()) {
                    announcementOverlay.push("A HUGE WAVE OF ZOMBIES IS APPROACHING!");
                } else if (currentWave == 1) {
                    announcementOverlay.push("ZOMBIES ARE COMING!");
                } else {
                    announcementOverlay.push("WAVE " + currentWave + "!");
                }
            }
        }
        for (GameEvent event : session.getCurrentLevel().drainChapterEvents()) {
            if (event.getType() != GameEventType.CHAPTER_EFFECT) {
                continue;
            }
            String message = event.getEntityName();
            if (message == null) {
                continue;
            }
            String normalized = message.toLowerCase(java.util.Locale.ROOT);
            if (normalized.contains("necromancy")) {
                announcementOverlay.push("NECROMANCY!");
            } else if (normalized.contains("low-tide zombies") || normalized.contains("surfacing")) {
                announcementOverlay.push("ZOMBIES ARE RISING FROM THE SHALLOWS!");
            } else if (normalized.contains("tide receded")) {
                announcementOverlay.push("LOW TIDE!");
            } else if (normalized.contains("tide rose")) {
                announcementOverlay.push("HIGH TIDE!");
            } else if (normalized.contains("sandstorm")) {
                announcementOverlay.push("SANDSTORM!");
            } else if (normalized.contains("icy wind")) {
                announcementOverlay.push("ICE WIND!");
            } else if (normalized.contains("new grave") || normalized.contains("graves rose")) {
                announcementOverlay.push("GRAVES ARE RISING!");
            }
        }
    }

    private void showGameOverIfNeeded() {
        if (gameOverShown || bossOutroStarted
                || session.getState() == null || !session.getState().isFinished()) {
            return;
        }
        if (pauseDialog != null) {
            pauseDialog.close();
            pauseDialog = null;
        }
        boolean victory = session.getState().getStatus() == GameState.Status.WON;
        announceMioPointIfNeeded();
        if (session.getCurrentLevel() != null && session.getCurrentLevel().getBossRuntime() != null) {
            bossOutroStarted = true;
            dialogueController.showBossOutro(victory, () -> showGameOverDialog(victory));
            return;
        }
        showGameOverDialog(victory);
    }

    private void showGameOverDialog(boolean victory) {
        gameOverShown = true;
        if (session.getCurrentLevel() != null && session.getCurrentLevel().getBossRuntime() != null) {
            boss.core.Boss boss = session.getCurrentLevel().getBossRuntime().getBoss();
            gameOverDialog = new BossGameOverDialog(
                    game.getSkin(),
                    victory,
                    boss.getDisplayName(),
                    animations.region("IMAGE_UI_PENNY_PURSUITS_ZPS_ZOMBOSS_METER_ICON"),
                    victory ? this::continueAfterVictory : this::retryAfterDefeat,
                    game.getScreenManager()::showAdventure
            );
        } else {
            String message = victory
                    ? "The lawn is safe. Continue your Adventure."
                    : "The zombies broke through. Try the level again.";
            gameOverDialog = new GameOverDialog(
                    game.getSkin(),
                    victory,
                    message,
                    victory ? this::continueAfterVictory : this::retryAfterDefeat,
                    game.getScreenManager()::showAdventure
            );
        }
        gameOverDialog.show(stage);
    }

    private void announceMioPointIfNeeded() {
        if (mioPointAnnounced) {
            return;
        }
        mioPointAnnounced = true;
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            return;
        }
        int gained = Math.max(0, user.getScore() - scoreAtLevelStart);
        if (gained > 0) {
            announcementOverlay.pushMioPoint(gained);
        }
    }

    private void retryAfterDefeat() {
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            game.getScreenManager().showAdventure();
            return;
        }
        game.getScreenManager().showAdventureMission(
                user.getCurrentChapterName(),
                user.getCurrentChapterLevel()
        );
    }

    private void continueAfterVictory() {
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            game.getScreenManager().showAdventure();
            return;
        }
        String chapter = AdventureLevelCatalog.normalizeChapterName(user.getCurrentChapterName());
        int levelNumber = user.getCurrentChapterLevel();
        int chapterLastLevel = AdventureLevelCatalog.lastRequiredLevel(chapter);
        if (levelNumber < chapterLastLevel
                && user.isChapterLevelUnlocked(chapter, levelNumber + 1)) {
            game.getScreenManager().showAdventureMission(chapter, levelNumber + 1);
            return;
        }
        String nextChapter = AdventureLevelCatalog.nextChapter(chapter);
        if (nextChapter != null && user.isChapterUnlocked(nextChapter)) {
            game.getScreenManager().showAdventureMission(nextChapter, 1);
            return;
        }
        game.getScreenManager().showAdventure();
    }

    private void setSpeed(int speed) {
        SettingsController settingsController = game.getSettingsController();
        settingsController.changeGameSpeed(speed);
        if (!settingsController.wasSuccessful()) {
            showSettingsMessage(settingsController);
            refreshStatus(settingsController.getLastMessage());
            return;
        }
        gameplayClock.setGameSpeed(speed);
        showSettingsMessage(settingsController);
        refreshStatus("Game speed x" + speed);
    }

    private void selectShovel() {
        interactions.selectShovel();
        showInteractionResult();
    }

    private void selectPlantFood() {
        interactions.selectPlantFood();
        showInteractionResult();
    }

    private void cancelInteraction() {
        interactions.cancel();
    }

    private void showInteractionResult() {
        String message = interactions.getLastMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        refreshStatus(message);
    }

    private String formatModeName(GameplayInputMode mode) {
        if (mode == null) {
            return "Normal";
        }
        return switch (mode) {
            case NORMAL -> "Normal";
            case PLANTING -> "Planting";
            case SHOVEL -> "Shovel";
            case PLANT_FOOD -> "Plant Food";
        };
    }


    private void syncInteractionControlState() {
        if (shovelButton != null) {
            shovelButton.setChecked(interactions.getMode() == GameplayInputMode.SHOVEL);
        }
    }

    private boolean isDebugMode() {
        return settings != null && settings.isDebugMode();
    }

    private void refreshStatus(String message) {
        if (message != null) {
            debugMessage = message;
        }
        statusLabel.setVisible(false);
    }

}
