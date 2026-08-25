package screens.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.pvz.Main;
import controllers.core.GameController;
import controllers.features.SettingsController;
import game.animation.core.PvzAnimationService;
import game.hud.GameplayEventFeedback;
import game.render.BoardBackgroundCatalog;
import game.render.BoardGeometry;
import game.render.BoardRenderer;
import game.render.entity.EntityRenderSystem;
import game.render.mower.LawnMowerRenderSystem;
import game.render.projectile.ProjectileRenderSystem;
import game.render.sun.SunRenderSystem;
import models.account.PlantData;
import models.account.Settings;
import models.account.User;
import models.core.plant.PlantType;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.session.GameSession;
import models.engine.session.GameState;
import models.engine.session.PlantRechargeStatus;
import models.engine.sun.Sun;
import models.level.core.AdventureLevelCatalog;
import screens.BaseScreen;
import ui.GameOverDialog;
import ui.MenuButton;
import ui.PauseDialog;
import ui.PlantCard;
import ui.ResourceBar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GameScreen extends BaseScreen {
    private static final float BOARD_X = 315f;
    private static final float BOARD_Y = 74f;
    private static final float BOARD_WIDTH = 920f;
    private static final float BOARD_HEIGHT = 457f;
    private static final float TICK_SECONDS = 0.1f;

    private final GameController controller;
    private final GameSession session;
    private final Settings settings;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BoardGeometry boardGeometry;
    private final BoardRenderer boardRenderer;
    private final PvzAnimationService animations;
    private final GameplayClock gameplayClock;
    private final Label statusLabel;
    private final ResourceBar resourceBar;
    private final Table plantCardsTable;
    private final Map<String, PlantCard> gameplayPlantCards;
    private final EntityRenderSystem entityRenderSystem;
    private final ProjectileRenderSystem projectileRenderSystem;
    private final SunRenderSystem sunRenderSystem;
    private final LawnMowerRenderSystem lawnMowerRenderSystem;
    private final GameplayEventFeedback eventFeedback;

    private TextureRegion background;
    private Position hoveredTile;
    private float hudRefreshAccumulator;
    private String debugMessage;
    private boolean pausedByLifecycle;
    private boolean resourceBarConfigured;
    private boolean debugControlsVisible;
    private float visualStateTime;
    private PauseDialog pauseDialog;
    private GameOverDialog gameOverDialog;
    private boolean gameOverShown;

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
        boardGeometry = new BoardGeometry(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
        boardRenderer = new BoardRenderer(boardGeometry);
        animations = new PvzAnimationService();
        gameplayClock = new GameplayClock(controller);
        gameplayClock.setGameSpeed(resolveInitialGameSpeed());
        statusLabel = new Label("", game.getSkin());
        resourceBar = new ResourceBar(game.getSkin());
        plantCardsTable = new Table();
        gameplayPlantCards = new LinkedHashMap<>();
        if (animations.isAvailable()) {
            entityRenderSystem = new EntityRenderSystem(boardGeometry, animations);
            projectileRenderSystem = new ProjectileRenderSystem(boardGeometry, animations);
            sunRenderSystem = new SunRenderSystem(boardGeometry, animations);
            lawnMowerRenderSystem = new LawnMowerRenderSystem(
                    boardGeometry,
                    animations,
                    session.getCurrentLevel() == null ? null : session.getCurrentLevel().getSeasonType()
            );
        } else {
            entityRenderSystem = null;
            projectileRenderSystem = null;
            sunRenderSystem = null;
            lawnMowerRenderSystem = null;
        }
        eventFeedback = new GameplayEventFeedback(notificationManager);
        buildHud();
        buildPlantCardHud();
        loadStageAssets();
        debugMessage = "Adventure session connected";
        pauseDialog = null;
        gameOverDialog = null;
        gameOverShown = false;
        refreshGameHud();
        refreshStatus(debugMessage);
    }

    @Override
    public void show() {
        super.show();
        applyStoredGameSpeed();
        refreshGameHud();
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, createInput()));
    }

    @Override
    public void render(float delta) {
        updateRuntime(delta);
        Gdx.gl.glClearColor(0.08f, 0.12f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);
        shapes.setProjectionMatrix(stage.getCamera().combined);
        drawBackground();
        drawGrid();
        drawLawnMowers();
        drawEntities();
        drawProjectiles();
        drawSuns();
        drawHover();
        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
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
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(10f);
        statusLabel.setWrap(true);
        statusLabel.setVisible(isDebugMode());
        hud.add(statusLabel).width(460f).left().top().expandX().fillX();
        hud.add(createPauseButton()).size(54f).padRight(8f).top();
        hud.add(resourceBar).right().top();
        stage.addActor(hud);
    }

    private ImageButton createPauseButton() {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        TextureRegion region = game.getAnimationService().region("IMAGE_UI_HUD_INGAME_PAUSE_BUTTON");
        if (region != null) {
            TextureRegionDrawable drawable = new TextureRegionDrawable(region);
            style.up = drawable;
            style.over = drawable;
            style.down = drawable;
            style.checked = drawable;
        }
        ImageButton button = new ImageButton(style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showPauseDialog();
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
        String resourceId = BoardBackgroundCatalog.resourceId(session.getCurrentLevel());
        background = animations.region(resourceId);
    }

    private void updateRuntime(float delta) {
        applyStoredGameSpeed();
        animations.update();
        int previousTick = gameplayClock.getCurrentTick();
        gameplayClock.update(delta);
        int currentTick = gameplayClock.getCurrentTick();
        float visualDelta = gameplayClock.isPaused() ? 0f : delta * gameplayClock.getGameSpeed();
        visualStateTime += visualDelta;
        updateRenderSystems(visualDelta, currentTick);
        if (currentTick != previousTick) {
            eventFeedback.acceptTickMessage(currentTick, controller.getLastMessage());
        }
        collectSunUnderPointer();
        refreshHud(delta);
        showGameOverIfNeeded();
    }

    private void updateRenderSystems(float visualDelta, int currentTick) {
        Board board = session.getBoard();
        if (entityRenderSystem != null) {
            entityRenderSystem.update(visualDelta, board);
        }
        if (projectileRenderSystem != null) {
            projectileRenderSystem.observe(board, currentTick);
            projectileRenderSystem.update(visualDelta);
        }
        if (lawnMowerRenderSystem != null) {
            lawnMowerRenderSystem.update(visualDelta, board);
        }
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
            resourceBar.setGameDebugControls(
                    debug,
                    this::addDebugCoins,
                    this::addDebugDiamonds,
                    this::addDebugSun,
                    this::addDebugPlantFood
            );
            resourceBarConfigured = true;
            debugControlsVisible = debug;
        }
        resourceBar.refreshGame(
                user,
                session.getTotalSunAmount(),
                session.getPlantFoodCount()
        );
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
        String displayMessage = stripMessagePrefix(message);
        if (settingsController.wasSuccessful()) {
            notificationManager.push(displayMessage, ui.NotificationType.SUCCESS);
            return;
        }
        notificationManager.push(displayMessage, ui.NotificationType.ERROR);
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
        boardRenderer.drawBackground(batch, background, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();
        enableAlphaBlending();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        boardRenderer.drawBoardFill(shapes, background != null);
        shapes.end();
        disableAlphaBlending();
    }

    private void drawGrid() {
        if (settings == null || !settings.isGridVisible()) {
            return;
        }
        enableAlphaBlending();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        boardRenderer.drawGrid(shapes);
        shapes.end();
        disableAlphaBlending();
    }

    private void drawEntities() {
        if (entityRenderSystem != null) {
            entityRenderSystem.render(batch, session.getBoard());
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

    private void drawLawnMowers() {
        if (lawnMowerRenderSystem != null) {
            lawnMowerRenderSystem.render(batch, session.getBoard());
        }
    }

    private void drawHover() {
        if (!isDebugMode() || hoveredTile == null) {
            return;
        }
        enableAlphaBlending();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        boardRenderer.drawHover(shapes, hoveredTile);
        shapes.end();
        disableAlphaBlending();
    }

    private void collectSunUnderPointer() {
        if (sunRenderSystem == null || gameplayClock.isPaused() || !session.isRunning()) {
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
            eventFeedback.showSunCollection(controller.getLastMessage());
            refreshGameHud();
        }
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
                updateHoveredTile(screenX, screenY);
                return handleBoardClick(button);
            }

            @Override
            public boolean keyDown(int keycode) {
                return handleKey(keycode);
            }
        };
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
        controller.handleUserClick(hoveredTile);
        String tile = "Clicked tile row=" + hoveredTile.getY() + ", column=" + hoveredTile.getX();
        Gdx.app.log("GameScreen", tile);
        if (isDebugMode()) {
            refreshStatus(controller.getLastMessage());
        }
        refreshGameHud();
        return true;
    }

    private void updateHoveredTile(int screenX, int screenY) {
        Vector2 world = new Vector2(screenX, screenY);
        stage.getViewport().unproject(world);
        hoveredTile = screenToBoard(world.x, world.y);
    }

    private boolean handleKey(int keycode) {
        if (keycode == Input.Keys.P || keycode == Input.Keys.SPACE || keycode == Input.Keys.ESCAPE) {
            togglePauseMenu();
            return true;
        }
        if (keycode == Input.Keys.NUM_1 || keycode == Input.Keys.NUM_2 || keycode == Input.Keys.NUM_3) {
            int speed = keycode == Input.Keys.NUM_1 ? 1 : keycode == Input.Keys.NUM_2 ? 2 : 3;
            setSpeed(speed);
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
        if (!gameplayClock.isPaused()) {
            gameplayClock.togglePause();
        }
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

    private void showGameOverIfNeeded() {
        if (gameOverShown || session.getState() == null || !session.getState().isFinished()) {
            return;
        }
        gameOverShown = true;
        boolean victory = session.getState().getStatus() == GameState.Status.WON;
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
        gameOverDialog.show(stage);
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
        if (levelNumber < AdventureLevelCatalog.LAST_PLAYABLE_LEVEL
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

    private boolean isDebugMode() {
        return settings != null && settings.isDebugMode();
    }

    private void refreshStatus(String message) {
        if (message != null) {
            debugMessage = message;
        }
        statusLabel.setVisible(isDebugMode());
        if (!isDebugMode()) {
            return;
        }
        String cursor = hoveredTile == null
                ? "outside board"
                : "(" + hoveredTile.getX() + ", " + hoveredTile.getY() + ")";
        String state = gameplayClock.isPaused()
                ? "PAUSED"
                : session.isRunning() ? "RUNNING" : "FINISHED";
        String grid = settings != null && settings.isGridVisible() ? "on" : "off";
        Board board = session.getBoard();
        statusLabel.setText(
                debugMessage
                        + " | tick=" + gameplayClock.getCurrentTick()
                        + " | speed=x" + gameplayClock.getGameSpeed()
                        + " | " + state
                        + " | grid=" + grid
                        + " | cursor=" + cursor
                        + " | plants=" + board.getPlantCount()
                        + " | zombies=" + board.getActiveZombieCount()
                        + "\nP/Space: pause | 1/2/3: speed | Left click: inspect tile | Esc: main menu"
                        + "\n" + animations.getStatusMessage()
        );
    }
}
