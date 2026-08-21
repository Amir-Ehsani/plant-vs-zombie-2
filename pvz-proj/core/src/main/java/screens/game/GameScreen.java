package screens.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pvz.Main;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import game.render.BoardRenderer;
import models.engine.board.Position;
import models.engine.session.GameSession;
import screens.BaseScreen;

public final class GameScreen extends BaseScreen {
    private static final float BOARD_X = 205f;
    private static final float BOARD_Y = 92f;
    private static final float BOARD_WIDTH = 930f;
    private static final float BOARD_HEIGHT = 505f;

    private static final String EGYPT_BACKGROUND = "IMAGE_BACKGROUNDS_EGYPT_TEXTURE";
    private static final String SUNFLOWER_PAM = "768/INITIAL/PLANT/SUNFLOWER/SUNFLOWER.PAM";
    private static final String ZOMBIE_PAM = "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM";

    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BoardGeometry boardGeometry;
    private final BoardRenderer boardRenderer;
    private final PvzAnimationService animations;
    private final GameSession sandboxSession;
    private final GameplayClock gameplayClock;
    private final Label statusLabel;

    private TextureRegion background;
    private Position hoveredTile;
    private float animationTime;
    private float hudRefreshAccumulator;
    private String debugMessage;
    private boolean pausedByLifecycle;

    public GameScreen(Main game) {
        super(game);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        boardGeometry = new BoardGeometry(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
        boardRenderer = new BoardRenderer(boardGeometry);
        animations = new PvzAnimationService();
        sandboxSession = createSandboxSession();
        gameplayClock = new GameplayClock(sandboxSession);
        statusLabel = new Label("", game.getSkin());
        buildDebugHud();
        loadStageOneAssets();
        debugMessage = "Stage 1 ready";
        refreshStatus(debugMessage);
    }

    @Override
    public void show() {
        super.show();
        InputMultiplexer multiplexer = new InputMultiplexer(createDebugInput(), stage);
        Gdx.input.setInputProcessor(multiplexer);
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
        drawBoardDebug();
        drawSampleEntities();
        drawHover();

        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void pause() {
        pausedByLifecycle = !gameplayClock.isPaused();
        if (pausedByLifecycle) {
            gameplayClock.togglePause();
            refreshStatus("Application paused");
        }
    }

    @Override
    public void resume() {
        if (pausedByLifecycle && gameplayClock.isPaused()) {
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

    private GameSession createSandboxSession() {
        GameSession session = new GameSession();
        session.initSession();
        return session;
    }

    private void buildDebugHud() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().left().pad(14f);
        statusLabel.setWrap(true);
        hud.add(statusLabel).width(760f).left();
        stage.addActor(hud);
    }

    private void loadStageOneAssets() {
        if (!animations.isAvailable()) {
            return;
        }
        background = animations.region(EGYPT_BACKGROUND);
        animations.preload(SUNFLOWER_PAM);
        animations.preload(ZOMBIE_PAM);
    }

    private void updateRuntime(float delta) {
        animations.update();
        gameplayClock.update(delta);
        if (!gameplayClock.isPaused()) {
            animationTime += delta * gameplayClock.getGameSpeed();
        }
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

    private void drawBoardDebug() {
        enableAlphaBlending();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        boardRenderer.drawGrid(shapes);
        shapes.end();
        disableAlphaBlending();
    }

    private void drawSampleEntities() {
        if (!animations.isAvailable()) {
            return;
        }

        Vector2 plant = boardGeometry.boardToScreen(3, 3);
        Vector2 zombie = boardGeometry.boardToScreen(3, 7);
        batch.begin();
        animations.draw(batch, SUNFLOWER_PAM, "idle", animationTime, plant.x, plant.y, 0.48f, true);
        animations.draw(batch, ZOMBIE_PAM, "walk", animationTime, zombie.x, zombie.y, 0.52f, true);
        batch.end();
    }

    private void drawHover() {
        if (hoveredTile == null) {
            return;
        }
        enableAlphaBlending();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        boardRenderer.drawHover(shapes, hoveredTile);
        shapes.end();
        disableAlphaBlending();
    }

    private void enableAlphaBlending() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void disableAlphaBlending() {
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private InputAdapter createDebugInput() {
        return new InputAdapter() {
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                updateHoveredTile(screenX, screenY);
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                updateHoveredTile(screenX, screenY);
                if (button == Input.Buttons.LEFT && hoveredTile != null) {
                    String tile = "Clicked tile row=" + hoveredTile.getY() + ", column=" + hoveredTile.getX();
                    Gdx.app.log("GameScreen", tile);
                    refreshStatus(tile);
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                return handleDebugKey(keycode);
            }
        };
    }

    private void updateHoveredTile(int screenX, int screenY) {
        Vector2 world = new Vector2(screenX, screenY);
        stage.getViewport().unproject(world);
        hoveredTile = boardGeometry.screenToBoard(world.x, world.y);
    }

    private boolean handleDebugKey(int keycode) {
        if (keycode == Input.Keys.P || keycode == Input.Keys.SPACE) {
            gameplayClock.togglePause();
            refreshStatus(gameplayClock.isPaused() ? "Paused" : "Resumed");
            return true;
        }
        if (keycode == Input.Keys.NUM_1) {
            setSpeed(1);
            return true;
        }
        if (keycode == Input.Keys.NUM_2) {
            setSpeed(2);
            return true;
        }
        if (keycode == Input.Keys.NUM_4) {
            setSpeed(4);
            return true;
        }
        if (keycode == Input.Keys.ESCAPE) {
            game.getScreenManager().showMainMenu();
            return true;
        }
        return false;
    }

    private void setSpeed(int speed) {
        gameplayClock.setGameSpeed(speed);
        refreshStatus("Game speed changed");
    }

    private void refreshStatus(String message) {
        debugMessage = message;
        String tileText = hoveredTile == null ? "outside board" : hoveredTile.toString();
        statusLabel.setText(
            message + " | tick=" + gameplayClock.getCurrentTick()
                + " | speed=x" + gameplayClock.getGameSpeed()
                + " | " + (gameplayClock.isPaused() ? "PAUSED" : "RUNNING")
                + " | cursor=" + tileText
                + "\nP/Space: pause | 1/2/4: speed | Left click: tile | Esc: main menu"
                + "\n" + animations.getStatusMessage()
        );
    }
}
