package game.modes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import controllers.core.GameController;
import game.render.BoardGeometry;
import models.engine.board.Position;
import models.engine.session.GameSession;
import models.level.core.Level;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.SpecialLevelType;
import models.level.rules.impl.DeadLineRule;
import models.level.rules.impl.LoveYourPlantsRule;
import models.level.rules.impl.SaveOurSeedsRule;
import models.level.rules.impl.TimedWarRule;
import ui.MenuButton;

import java.util.Locale;

public final class AdventureLevelModeAdapter implements LevelModeAdapter {
    private static final float TICKS_PER_SECOND = 10f;
    private static final float BORDER_SIZE = 5f;
    private static final float DEADLINE_WIDTH = 7f;

    private final GameController controller;
    private final GameSession session;
    private final Level level;
    private final SpecialLevelType type;
    private final BoardGeometry boardGeometry;
    private final ShapeRenderer shapes;
    private final Stage stage;
    private final Skin skin;

    private Table hud;
    private Label modeLabel;
    private Label timeLabel;
    private Label zombiesLabel;
    private Label sunLabel;
    private MenuButton startButton;

    public AdventureLevelModeAdapter(
            GameController controller,
            BoardGeometry boardGeometry,
            ShapeRenderer shapes,
            Stage stage,
            Skin skin
    ) {
        this.controller = controller;
        this.session = controller.getGameSession();
        this.level = session.getCurrentLevel();
        this.type = level.getLevelRule().getType();
        this.boardGeometry = boardGeometry;
        this.shapes = shapes;
        this.stage = stage;
        this.skin = skin;
    }

    @Override
    public void setup() {
        if (type == SpecialLevelType.NONE) {
            return;
        }
        hud = new Table();
        if (type == SpecialLevelType.TIMED_WAR) {
            setupTimedWarHud();
        } else {
            hud.setFillParent(true);
            hud.top().padTop(50f);
            modeLabel = createHudLabel();
            hud.add(modeLabel);
            if (type == SpecialLevelType.PLANT_WHAT_YOU_GET) {
                startButton = new MenuButton("START", skin, "green", this::startZombieWaves);
                hud.add(startButton).width(150f).height(44f).padLeft(14f);
            }
        }
        stage.addActor(hud);
        update();
    }

    @Override
    public void update() {
        if (type == SpecialLevelType.TIMED_WAR) {
            updateTimedWarHud((TimedWarRule) level.getLevelRule());
        } else if (modeLabel != null) {
            modeLabel.setText(modeText());
        }
        if (startButton != null) {
            startButton.setVisible(!level.areZombieWavesStarted());
        }
    }

    @Override
    public void renderOverlay() {
        if (type == SpecialLevelType.SAVE_OUR_SEEDS) {
            drawProtectedTiles((SaveOurSeedsRule) level.getLevelRule());
        } else if (type == SpecialLevelType.DEAD_LINE) {
            drawDeadline((DeadLineRule) level.getLevelRule());
        }
    }

    @Override
    public void dispose() {
        if (hud != null) {
            hud.remove();
        }
    }

    private String modeText() {
        return switch (type) {
            case LOVE_YOUR_PLANTS -> loveYourPlantsText((LoveYourPlantsRule) level.getLevelRule());
            default -> "";
        };
    }

    private void setupTimedWarHud() {
        hud.setBounds(220f, 610f, 840f, 38f);
        timeLabel = createHudLabel();
        zombiesLabel = createHudLabel();
        sunLabel = createHudLabel();
        hud.add(timeLabel).width(230f).center();
        hud.add(separator()).width(18f).center();
        hud.add(zombiesLabel).width(220f).center();
        hud.add(separator()).width(18f).center();
        hud.add(sunLabel).width(210f).center();
    }

    private Label createHudLabel() {
        Label label = new Label("", skin, "medium_outline");
        label.setColor(Color.WHITE);
        label.setAlignment(com.badlogic.gdx.utils.Align.center);
        return label;
    }

    private Label separator() {
        Label label = createHudLabel();
        label.setText("|");
        return label;
    }

    private void updateTimedWarHud(TimedWarRule rule) {
        LevelRuntimeContext context = createContext();
        float seconds = rule.getRemainingTicks(context) / TICKS_PER_SECOND;
        int kills = rule.getKillProgress(context);
        int suns = rule.getSunProgress(context);
        int killTarget = rule.getKillTarget();
        int sunTarget = rule.getSunTarget();
        timeLabel.setText(String.format(Locale.ROOT, "Time Left: %.1fs", seconds));
        zombiesLabel.setText("Zombies: " + kills + "/" + killTarget);
        sunLabel.setText("Sun: " + suns + "/" + sunTarget);
    }

    private String loveYourPlantsText(LoveYourPlantsRule rule) {
        int destroyed = rule.getDestroyedPlantCount(createContext());
        int remaining = Math.max(0, rule.getMaximumPlantLosses() - destroyed);
        return "Plants Remaining: " + remaining;
    }

    private LevelRuntimeContext createContext() {
        return new LevelRuntimeContext(
                session.getBoard(),
                session.getTickManager().getCurrentTick(),
                session.getTotalSunAmount(),
                session.getTotalSunProduced(),
                session.getTotalSunCollected(),
                session.getTotalZombiesKilled(),
                session.getTotalPlantsDestroyed()
        );
    }

    private void startZombieWaves() {
        controller.startZombieWaves();
        update();
    }

    private void drawProtectedTiles(SaveOurSeedsRule rule) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.YELLOW);
        for (Position position : rule.getProtectedPositions()) {
            Rectangle tile = boardGeometry.getTileBounds(position.getY(), position.getX());
            drawBorder(tile);
        }
        shapes.end();
    }

    private void drawBorder(Rectangle tile) {
        shapes.rect(tile.x, tile.y, tile.width, BORDER_SIZE);
        shapes.rect(tile.x, tile.y + tile.height - BORDER_SIZE, tile.width, BORDER_SIZE);
        shapes.rect(tile.x, tile.y, BORDER_SIZE, tile.height);
        shapes.rect(tile.x + tile.width - BORDER_SIZE, tile.y, BORDER_SIZE, tile.height);
    }

    private void drawDeadline(DeadLineRule rule) {
        Rectangle board = boardGeometry.getBoardBounds();
        float x = boardGeometry.entityToScreen(rule.getDeadlineX() + 0.5f, 1).x;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.RED);
        shapes.rect(x - DEADLINE_WIDTH / 2f, board.y, DEADLINE_WIDTH, board.height);
        shapes.end();
    }
}
