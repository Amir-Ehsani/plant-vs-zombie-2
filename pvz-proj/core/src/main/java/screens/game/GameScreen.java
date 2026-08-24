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
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pvz.Main;
import controllers.features.SettingsController;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import game.render.BoardRenderer;
import game.render.entity.EntityOverlayRenderer;
import game.render.entity.EntityRenderSystem;
import models.account.PlantData;
import models.account.Settings;
import models.account.User;
import models.core.plant.Plant;
import models.core.plant.PlantType;
import models.core.zombie.Zombie;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.engine.session.GameSession;
import models.engine.session.PlantRechargeStatus;
import screens.BaseScreen;
import ui.NotificationManager;
import ui.PlantCard;
import ui.ResourceBar;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GameScreen extends BaseScreen {
    private static final float BOARD_X = 315f;
    private static final float BOARD_Y = 74f;
    private static final float BOARD_WIDTH = 920f;
    private static final float BOARD_HEIGHT = 457f;
    private static final String EGYPT_BACKGROUND = "IMAGE_BACKGROUNDS_EGYPT_TEXTURE";
    private static final float TICK_SECONDS = 0.1f;

    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BoardGeometry boardGeometry;
    private final BoardRenderer boardRenderer;
    private final PvzAnimationService animations;
    private final GameSession sandboxSession;
    private final GameplayClock gameplayClock;
    private final Label statusLabel;
    private final ResourceBar resourceBar;
    private final Table plantCardsTable;
    private final Map<String, PlantCard> gameplayPlantCards;
    private final EntityRenderSystem entityRenderSystem;
    private final EntityOverlayRenderer entityOverlayRenderer;

    private TextureRegion background;
    private Position hoveredTile;
    private float hudRefreshAccumulator;
    private String debugMessage;
    private boolean pausedByLifecycle;
    private boolean resourceBarConfigured;
    private boolean debugControlsVisible;

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
        resourceBar = new ResourceBar(game.getSkin());
        plantCardsTable = new Table();
        gameplayPlantCards = new LinkedHashMap<>();
        entityRenderSystem = animations.isAvailable()
                ? new EntityRenderSystem(boardGeometry, animations)
                : null;
        entityOverlayRenderer = new EntityOverlayRenderer(boardGeometry);
        applySettings();
        buildHud();
        buildPlantCardHud();
        loadStageAssets();
        debugMessage = "Stage 2 ready";
        refreshGameHud();
        refreshStatus(debugMessage);
    }

    @Override
    public void show() {
        super.show();
        applySettings();
        refreshGameHud();
        Gdx.input.setInputProcessor(new InputMultiplexer(createDebugInput(), stage));
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
        drawBoardGrid();
        drawEntities();
        drawEntityOverlays();
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
        session.setSelectedPlantNames(List.of(
                "Sunflower",
                "Wall-nut",
                "Pumpkin",
                "Lily Pad",
                "Tall-nut",
                "Peashooter"
        ));
        session.initSession();
        session.addSun(5000);
        Board board = session.getBoard();
        board.setTileType(new Position(3, 4), TileType.WATER);
        session.plant("Sunflower", new Position(3, 1));
        session.plant("Wall-nut", new Position(4, 3));
        session.plant("Pumpkin", new Position(4, 3));
        session.plant("Lily Pad", new Position(3, 4));
        session.plant("Tall-nut", new Position(3, 4));
        session.plant("Peashooter", new Position(3, 5));
        Tile frozenTile = board.getTileAt(new Position(3, 5));
        if (frozenTile != null && frozenTile.getCurrentPlant() != null) {
            frozenTile.getCurrentPlant().addIceHit();
            frozenTile.getCurrentPlant().addIceHit();
            frozenTile.getCurrentPlant().addIceHit();
        }
        session.spawnZombie("Default", new Position(9, 1));
        session.spawnZombie("cone head", new Position(9, 3));
        session.spawnZombie("bucket head", new Position(9, 4));
        Tile bucketTile = board.getTileAt(new Position(9, 4));
        if (bucketTile != null && !bucketTile.getZombies().isEmpty()) {
            Zombie bucket = bucketTile.getZombies().get(0);
            board.applyChill(bucket, 1000);
        }
        Tile octopusTile = board.getTileAt(new Position(3, 1));
        if (octopusTile != null) {
            Plant plant = octopusTile.getCurrentPlant();
            if (plant != null) {
                plant.addOctopus();
            }
        }
        return session;
    }

    private void buildHud() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(10f);
        statusLabel.setWrap(true);
        hud.add(statusLabel).width(460f).left().top().expandX().fillX();
        hud.add(resourceBar).right().top();
        stage.addActor(hud);
    }

    private void buildPlantCardHud() {
        plantCardsTable.top();
        plantCardsTable.defaults().padBottom(8f);
        for (PlantRechargeStatus status : sandboxSession.getPlantRechargeStatuses()) {
            if (!status.isSelected()) {
                continue;
            }
            PlantCard card = createGameplayPlantCard(status);
            gameplayPlantCards.put(status.getPlantName(), card);
            plantCardsTable.add(card).width(270f).row();
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

    private PlantCard createGameplayPlantCard(PlantRechargeStatus status) {
        PlantCard card = new PlantCard(game.getSkin());
        PlantType type = sandboxSession.getPlantType(status.getPlantName());
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
        card.setSeedPacketProgress(data.getSeedPackets(), data.getRequiredSeedPacketsForNextLevel());
    }

    private PlantData getPlantData(String plantName) {
        User user = game.getAuthController().getLoggedInUser();
        if (user == null || user.getCollection() == null) {
            return null;
        }
        return user.getCollection().findPlant(plantName);
    }

    private void loadStageAssets() {
        if (animations.isAvailable()) {
            background = animations.region(EGYPT_BACKGROUND);
        }
    }

    private void updateRuntime(float delta) {
        applySettings();
        animations.update();
        gameplayClock.update(delta);
        float visualDelta = gameplayClock.isPaused() ? 0f : delta * gameplayClock.getGameSpeed();
        if (entityRenderSystem != null) {
            entityRenderSystem.update(visualDelta, sandboxSession.getBoard());
        }
        hudRefreshAccumulator += delta;
        if (hudRefreshAccumulator >= 0.1f) {
            hudRefreshAccumulator = 0f;
            refreshGameHud();
            refreshStatus(debugMessage);
        }
    }

    private void applySettings() {
        Settings settings = getSettings();
        int speed = settings == null ? Settings.MIN_GAME_SPEED : settings.getGameSpeed();
        if (gameplayClock.getGameSpeed() != speed) {
            gameplayClock.setGameSpeed(speed);
        }
    }

    private Settings getSettings() {
        User user = game.getAuthController().getLoggedInUser();
        return user == null ? null : user.getSettings();
    }

    private void refreshGameHud() {
        User user = game.getAuthController().getLoggedInUser();
        Settings settings = user == null ? null : user.getSettings();
        boolean debug = settings != null && settings.isDebugMode();
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
        resourceBar.refreshGame(user, sandboxSession.getTotalSunAmount(), sandboxSession.getPlantFoodCount());
        refreshGameplayPlantCards();
    }

    private void refreshGameplayPlantCards() {
        for (PlantRechargeStatus status : sandboxSession.getPlantRechargeStatuses()) {
            PlantCard card = gameplayPlantCards.get(status.getPlantName());
            if (card == null) {
                continue;
            }
            card.setSelected(status.isSelected());
            card.setCooldown(status.getRemainingTicks() * TICK_SECONDS);
        }
    }

    private void addDebugCoins() {
        SettingsController controller = game.getSettingsController();
        controller.addDebugCoins(1000);
        showSettingsMessage(controller);
        refreshGameHud();
    }

    private void addDebugDiamonds() {
        SettingsController controller = game.getSettingsController();
        controller.addDebugDiamonds(10);
        showSettingsMessage(controller);
        refreshGameHud();
    }

    private void addDebugSun() {
        SettingsController controller = game.getSettingsController();
        controller.addDebugSun(sandboxSession, 250);
        showSettingsMessage(controller);
        refreshGameHud();
    }

    private void addDebugPlantFood() {
        SettingsController controller = game.getSettingsController();
        controller.addDebugPlantFood(sandboxSession);
        showSettingsMessage(controller);
        refreshGameHud();
    }

    private void showSettingsMessage(SettingsController controller) {
        String message = controller.getLastMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        if (controller.wasSuccessful()) {
            NotificationManager.showSuccess(stripMessagePrefix(message));
            return;
        }
        NotificationManager.showError(stripMessagePrefix(message));
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

    private void drawBoardGrid() {
        Settings settings = getSettings();
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
            entityRenderSystem.render(batch, sandboxSession.getBoard());
        }
    }

    private void drawEntityOverlays() {
        enableAlphaBlending();
        entityOverlayRenderer.render(shapes, sandboxSession.getBoard());
        disableAlphaBlending();
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
        if (keycode == Input.Keys.NUM_3) {
            setSpeed(3);
            return true;
        }
        if (keycode == Input.Keys.ESCAPE) {
            game.getScreenManager().showMainMenu();
            return true;
        }
        return false;
    }

    private void setSpeed(int speed) {
        SettingsController controller = game.getSettingsController();
        controller.changeGameSpeed(speed);
        applySettings();
        showSettingsMessage(controller);
        refreshStatus("Game speed x" + gameplayClock.getGameSpeed());
    }

    private void refreshStatus(String message) {
        if (message != null) {
            debugMessage = message;
        }
        String cursor = hoveredTile == null
                ? "outside board"
                : "(" + hoveredTile.getX() + ", " + hoveredTile.getY() + ")";
        String state = gameplayClock.isPaused() ? "PAUSED" : "RUNNING";
        Settings settings = getSettings();
        boolean gridVisible = settings != null && settings.isGridVisible();
        Board board = sandboxSession.getBoard();
        statusLabel.setText(
                debugMessage
                        + " | tick=" + gameplayClock.getCurrentTick()
                        + " | speed=x" + gameplayClock.getGameSpeed()
                        + " | " + state
                        + " | grid=" + (gridVisible ? "on" : "off")
                        + " | cursor=" + cursor
                        + " | plants=" + board.getPlantCount()
                        + " | zombies=" + board.getActiveZombieCount()
                        + "\nP/Space: pause | 1/2/3: speed | Left click: tile | Esc: main menu"
                        + "\n" + animations.getStatusMessage()
        );
    }
}
