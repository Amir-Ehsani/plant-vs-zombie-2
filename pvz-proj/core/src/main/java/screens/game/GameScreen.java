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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pvz.Main;
import controllers.core.GameController;
import game.animation.core.PvzAnimationService;
import game.hud.GameplayEventFeedback;
import game.render.BoardBackgroundCatalog;
import game.render.BoardGeometry;
import game.render.BoardRenderer;
import game.render.entity.EntityRenderSystem;
import game.render.mower.LawnMowerRenderSystem;
import game.render.projectile.ProjectileRenderSystem;
import game.render.sun.SunRenderSystem;
import models.account.Settings;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.session.GameSession;
import models.engine.sun.Sun;
import screens.BaseScreen;

public final class GameScreen extends BaseScreen {
    private static final float BOARD_X = 315f;
    private static final float BOARD_Y = 74f;
    private static final float BOARD_WIDTH = 920f;
    private static final float BOARD_HEIGHT = 457f;

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
    private float visualStateTime;

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
        buildDebugHud();
        loadStageAssets();
        debugMessage = "Adventure session connected";
        refreshStatus(debugMessage);
    }

    @Override
    public void show() {
        super.show();
        Gdx.input.setInputProcessor(new InputMultiplexer(createInput(), stage));
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
        return Math.max(Settings.MIN_GAME_SPEED, Math.min(Settings.MAX_GAME_SPEED, settings.getGameSpeed()));
    }

    private void buildDebugHud() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().left().pad(14f);
        statusLabel.setWrap(true);
        statusLabel.setVisible(isDebugMode());
        hud.add(statusLabel).width(900f).left();
        stage.addActor(hud);
    }

    private void loadStageAssets() {
        if (!animations.isAvailable()) {
            return;
        }
        String resourceId = BoardBackgroundCatalog.resourceId(session.getCurrentLevel());
        background = animations.region(resourceId);
    }

    private void updateRuntime(float delta) {
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
        refreshDebugHud(delta);
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

    private void refreshDebugHud(float delta) {
        hudRefreshAccumulator += delta;
        if (hudRefreshAccumulator >= 0.1f) {
            hudRefreshAccumulator = 0f;
            refreshStatus(debugMessage);
        }
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
        Sun sun = sunRenderSystem.findHoveredSun(world.x, world.y, session.getSunManager());
        if (sun == null) {
            return;
        }
        controller.collectSun(sun.getPosition());
        if (controller.wasSuccessful()) {
            eventFeedback.showSunCollection(controller.getLastMessage());
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
        if (button != Input.Buttons.LEFT || hoveredTile == null) {
            return false;
        }
        controller.handleUserClick(hoveredTile);
        String tile = "Clicked tile row=" + hoveredTile.getY() + ", column=" + hoveredTile.getX();
        Gdx.app.log("GameScreen", tile);
        if (isDebugMode()) {
            refreshStatus(controller.getLastMessage());
        }
        return true;
    }

    private void updateHoveredTile(int screenX, int screenY) {
        Vector2 world = new Vector2(screenX, screenY);
        stage.getViewport().unproject(world);
        hoveredTile = screenToBoard(world.x, world.y);
    }

    private boolean handleKey(int keycode) {
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
            game.getScreenManager().showMainMenu();
            return true;
        }
        return false;
    }

    private void setSpeed(int speed) {
        game.getSettingsController().changeGameSpeed(speed);
        if (!game.getSettingsController().wasSuccessful()) {
            refreshStatus(game.getSettingsController().getLastMessage());
            return;
        }
        gameplayClock.setGameSpeed(speed);
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
        String state = gameplayClock.isPaused() ? "PAUSED" : session.isRunning() ? "RUNNING" : "FINISHED";
        Board board = session.getBoard();
        statusLabel.setText(
            debugMessage
                + " | tick=" + gameplayClock.getCurrentTick()
                + " | speed=x" + gameplayClock.getGameSpeed()
                + " | " + state
                + " | cursor=" + cursor
                + " | plants=" + board.getPlantCount()
                + " | zombies=" + board.getActiveZombieCount()
                + "\nP/Space: pause | 1/2/3: speed | Left click: inspect tile | Esc: main menu"
                + "\n" + animations.getStatusMessage()
        );
    }
}
