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
import com.badlogic.gdx.scenes.scene2d.ui.Button;
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
import controllers.features.TravelLogController;
import game.animation.core.PvzAnimationService;
import game.dialogue.LevelDialogueController;
import game.minigame.MiniGameVisualRenderer;
import game.notification.GameplayAnnouncementOverlay;
import game.render.BoardGeometry;
import models.engine.board.Position;
import models.minigame.IZombieGame;
import models.minigame.MiniGameSession;
import models.minigame.MiniGameType;
import models.minigame.VasebreakerGame;
import models.minigame.WallNutBowlingGame;
import screens.BaseScreen;
import ui.GameOverDialog;
import ui.MenuButton;
import ui.PauseDialog;
import ui.ResourceBar;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MiniGameScreen extends BaseScreen {
    private static final float BOARD_X = 315f;
    private static final float BOARD_Y = 74f;
    private static final float BOARD_WIDTH = 920f;
    private static final float BOARD_HEIGHT = 457f;
    private static final float TICK_SECONDS = 0.1f;

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
    private Position hoveredTile;
    private Integer selectedPacketId;
    private Integer draggedPacketId;
    private String draggedNutType;
    private String selectedNutType;
    private String selectedZombieName;
    private float tickAccumulator;
    private float visualStateTime;
    private boolean gameOverShown;
    private boolean paused;
    private boolean startupUiInitialized;
    private boolean introDialogueStarted;
    private PauseDialog pauseDialog;

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
        selectedZombieName = firstZombieOption();
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
        initializeStartupUi();
        update(delta);
        Gdx.gl.glClearColor(0.05f, 0.08f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);
        shapes.setProjectionMatrix(stage.getCamera().combined);
        enableBlending();
        visualRenderer.render(batch, shapes, visualStateTime, hoveredTile);
        if (draggedPacketId != null && session instanceof VasebreakerGame gameSession) {
            visualRenderer.renderDraggedPacket(
                    batch,
                    gameSession,
                    draggedPacketId,
                    cursorWorld.x,
                    cursorWorld.y,
                    visualStateTime
            );
        }
        if (draggedNutType != null && session instanceof WallNutBowlingGame) {
            visualRenderer.renderDraggedNut(
                    batch,
                    draggedNutType,
                    cursorWorld.x,
                    cursorWorld.y,
                    visualStateTime
            );
        }
        disableBlending();
        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
    }

    @Override
    public void dispose() {
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
        if (session instanceof IZombieGame) {
            resourceBar.showMiniGameResources();
            hud.add(resourceBar).right().top();
        }
        stage.addActor(hud);

        if (session instanceof IZombieGame gameSession) {
            buildIZombieBar(gameSession);
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
        for (IZombieGame.ZombieOptionView option : gameSession.getAvailableZombieOptions()) {
            cards.add(createZombieCard(option)).width(132f).height(88f).row();
        }
        host.add(cards).top().left();
        stage.addActor(host);
    }

    private Table createZombieCard(IZombieGame.ZombieOptionView option) {
        Table card = new Table();
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
                refreshZombieSelectionVisuals();
            }
        });
        return card;
    }

    private void update(float delta) {
        updatePointer();
        if (!paused && session.isRunning()) {
            advanceGame(delta);
        }
        float visualDelta = paused ? 0f : Math.min(delta, 1f / 15f);
        visualStateTime += visualDelta;
        visualRenderer.setSelectedNutType(selectedNutType);
        visualRenderer.update(visualDelta);
        syncPacketSelection();
        refreshHud();
        showGameOverIfNeeded();
    }

    private void advanceGame(float delta) {
        tickAccumulator += delta;
        while (tickAccumulator >= TICK_SECONDS && session.isRunning()) {
            controller.advanceMiniGameTime(1);
            tickAccumulator -= TICK_SECONDS;
        }
    }

    private void refreshHud() {
        if (session instanceof IZombieGame gameSession) {
            resourceBar.refreshMiniGame(game.getAuthController().getLoggedInUser(), gameSession.getSunAmount());
        }
    }

    private void refreshZombieSelectionVisuals() {
        for (Map.Entry<String, Image> entry : zombieSelectionFrames.entrySet()) {
            entry.getValue().setVisible(entry.getKey().equalsIgnoreCase(selectedZombieName));
        }
    }

    private InputAdapter createInput() {
        return new InputAdapter() {
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                updatePointer(screenX, screenY);
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                updatePointer(screenX, screenY);
                return draggedPacketId != null || draggedNutType != null;
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
                        draggedPacketId = packetId;
                        selectedPacketId = packetId;
                        return true;
                    }
                }

                if (session instanceof WallNutBowlingGame) {
                    String nutType = visualRenderer.findWallNutAt(cursorWorld.x, cursorWorld.y);
                    if (nutType != null) {
                        draggedNutType = nutType;
                        selectedNutType = nutType;
                        visualRenderer.setSelectedNutType(selectedNutType);
                        return true;
                    }
                }

                if (session instanceof IZombieGame) {
                    Integer sunDropId = visualRenderer.findSunDropAt(cursorWorld.x, cursorWorld.y);
                    if (sunDropId != null) {
                        controller.collectIZombieSun(sunDropId);
                        showActionMessage();
                        refreshHud();
                        return true;
                    }
                }

                return handleBoardClick();
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT) {
                    return false;
                }
                updatePointer(screenX, screenY);
                if (draggedPacketId != null) {
                    selectedPacketId = draggedPacketId;
                    draggedPacketId = null;
                    if (hoveredTile != null) {
                        boolean planted = controller.plantVasebreakerPacket(selectedPacketId, hoveredTile);
                        if (planted) {
                            selectedPacketId = null;
                        }
                        showActionMessage();
                    }
                    return true;
                }
                if (draggedNutType != null) {
                    String nutType = draggedNutType;
                    draggedNutType = null;
                    boolean launched = hoveredTile != null && controller.launchNut(nutType, hoveredTile);
                    showActionMessage();
                    selectedNutType = null;
                    visualRenderer.setSelectedNutType(null);
                    return launched || hoveredTile != null;
                }
                return false;
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
        return false;
    }

    private boolean handleIZombieClick() {
        if (selectedZombieName == null) {
            return false;
        }
        controller.spawnIZombie(selectedZombieName, hoveredTile);
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
        draggedPacketId = null;
    }

    private String firstZombieOption() {
        if (!(session instanceof IZombieGame gameSession) || gameSession.getAvailableZombieOptions().isEmpty()) {
            return null;
        }
        return gameSession.getAvailableZombieOptions().get(0).zombieName();
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
        announcementOverlay.push(miniGameAnnouncement());
    }

    private String miniGameAnnouncement() {
        return switch (session.getType()) {
            case VASEBREAKER -> "VASEBREAKER!";
            case WALLNUT_BOWLING -> "WALL-NUT BOWLING!";
            case I_ZOMBIE -> "I, ZOMBIE!";
        };
    }

    private void retryMiniGame() {
        if (pauseDialog != null) {
            pauseDialog.close();
            pauseDialog = null;
        }
        MiniGameType type = session.getType();
        int stageNumber = session.getStage();
        controller.enterMiniGame(type.getDisplayName(), stageNumber);
        if (controller.wasSuccessful()) {
            game.getScreenManager().showActiveMiniGame();
        }
    }

    private void exitMiniGame() {
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
