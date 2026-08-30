package screens.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.pvz.Main;
import network.game.ActionResult;
import network.game.AuthoritativeIZombieGame;
import network.game.CouchIZombieController;
import network.protocol.GameRole;
import network.protocol.GameSnapshot;
import network.ui.IZombieBoardActor;
import screens.BaseScreen;
import ui.ConfirmDialog;
import ui.MenuButton;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Bonus one-device I, Zombie: plants use mouse and zombies use keyboard. */
public final class CouchIZombieScreen extends BaseScreen {
    private final int requestedStage;
    private final CouchIZombieController controller;
    private final Map<String, TextButton> plantButtons = new LinkedHashMap<>();
    private final Map<String, Label> zombieLabels = new LinkedHashMap<>();
    private IZombieBoardActor board;
    private Label timerLabel;
    private Label resourcesLabel;
    private Label statusLabel;
    private Label plantSelectionLabel;
    private Label zombieSelectionLabel;
    private double pendingMillis;
    private boolean endShown;

    public CouchIZombieScreen(Main game, int stage) {
        super(game);
        requestedStage = Math.max(1, Math.min(3, stage));
        controller = new CouchIZombieController(requestedStage);
        buildUi();
    }

    private void buildUi() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(12f);
        Table header = new Table();
        Label title = new Label("I, ZOMBIE - COUCH PLAY", game.getSkin(), "big_outline");
        title.setColor(Color.WHITE);
        timerLabel = new Label("02:00", game.getSkin(), "medium_outline");
        timerLabel.setColor(Color.WHITE);
        resourcesLabel = new Label("", game.getSkin(), "secondary");
        resourcesLabel.setColor(Color.WHITE);
        header.add(title).expandX().left();
        header.add(timerLabel).width(120f);
        header.add(resourcesLabel).width(390f).right();
        root.add(header).growX().height(56f).row();

        Table body = new Table();
        board = new IZombieBoardActor(game.getAnimationService(), this::plantMouseClick);
        body.add(board).width(890f).height(500f).padRight(10f);
        body.add(buildControls()).width(340f).height(500f);
        root.add(body).grow().row();
        statusLabel = new Label("Plants: mouse. Zombies: W/S or arrows, 1-5, Space.", game.getSkin(), "secondary");
        statusLabel.setColor(Color.WHITE);
        statusLabel.setWrap(true);
        statusLabel.setAlignment(Align.center);
        root.add(statusLabel).growX().height(46f);
        stage.addActor(root);
        refresh(controller.snapshot());
    }

    private Table buildControls() {
        Table controls = new Table();
        controls.top();
        Label plants = new Label("PLANTS - MOUSE", game.getSkin(), "medium_outline");
        plants.setColor(Color.WHITE);
        controls.add(plants).padBottom(4f).row();
        for (String name : controller.getPlantTypes()) {
            TextButton button = new TextButton(name, game.getSkin(), "green");
            button.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    controller.selectPlant(name);
                    refresh(controller.snapshot());
                }
            });
            plantButtons.put(name, button);
            controls.add(button).width(285f).height(34f).pad(1f).row();
        }
        plantSelectionLabel = new Label("", game.getSkin(), "secondary");
        plantSelectionLabel.setColor(Color.WHITE);
        controls.add(plantSelectionLabel).height(28f).row();

        Label zombies = new Label("ZOMBIES - KEYBOARD", game.getSkin(), "medium_outline");
        zombies.setColor(Color.WHITE);
        controls.add(zombies).padTop(6f).padBottom(3f).row();
        int index = 1;
        for (String name : controller.getZombieTypes()) {
            Label label = new Label(index + " " + name, game.getSkin(), "secondary");
            label.setColor(Color.WHITE);
            zombieLabels.put(name, label);
            controls.add(label).height(24f).row();
            index++;
        }
        zombieSelectionLabel = new Label("", game.getSkin(), "secondary");
        zombieSelectionLabel.setColor(Color.WHITE);
        zombieSelectionLabel.setWrap(true);
        zombieSelectionLabel.setAlignment(Align.center);
        controls.add(zombieSelectionLabel).width(300f).height(46f).row();
        controls.add(new MenuButton("Exit", game.getSkin(), "brown", this::confirmExit))
                .width(180f).height(36f).padTop(5f);
        return controls;
    }

    private void plantMouseClick(int row, int column) {
        if (controller.isFinished()) return;
        ActionResult result = controller.placeSelectedPlant(row, column);
        setStatus(result.getMessage());
        refresh(result.getSnapshot() == null ? controller.snapshot() : result.getSnapshot());
    }

    private void handleZombieKeyboard() {
        if (controller.isFinished()) return;
        boolean changed = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            controller.moveZombieRow(-1); changed = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            controller.moveZombieRow(1); changed = true;
        }
        int index = pressedZombieIndex();
        if (index >= 0) changed |= controller.selectZombieByIndex(index);
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            ActionResult result = controller.releaseSelectedZombie();
            setStatus(result.getMessage());
            refresh(result.getSnapshot() == null ? controller.snapshot() : result.getSnapshot());
        } else if (changed) refresh(controller.snapshot());
    }

    private int pressedZombieIndex() {
        int[] keys = {Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5};
        for (int i = 0; i < Math.min(keys.length, controller.getZombieTypes().size()); i++) {
            if (Gdx.input.isKeyJustPressed(keys[i])) return i;
        }
        return -1;
    }

    private void refresh(GameSnapshot snapshot) {
        if (snapshot == null) return;
        board.setSnapshot(snapshot);
        board.setKeyboardLaneRow(controller.getZombieRow());
        board.setInteraction(GameRole.PLANTS, !snapshot.isFinished());
        long seconds = (snapshot.getRemainingMillis() + 999L) / 1000L;
        timerLabel.setText(String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L));
        resourcesLabel.setText("PLANT SUN " + snapshot.getPlantSun() + "   ZOMBIE SUN " + snapshot.getZombieSun()
                + "   BRAINS " + snapshot.getBrainsRemaining());
        updatePlantControls(snapshot);
        updateZombieControls(snapshot);
        if (snapshot.isFinished()) showEnd(snapshot);
    }

    private void updatePlantControls(GameSnapshot snapshot) {
        String selected = controller.getSelectedPlant();
        for (Map.Entry<String, TextButton> entry : plantButtons.entrySet()) {
            String type = entry.getKey();
            int cost = controller.plantCost(type);
            long cooldown = snapshot.getPlantCooldownMillis(type);
            entry.getValue().setText((type.equals(selected) ? "> " : "") + type + " " + cost
                    + (cooldown > 0L ? " [" + seconds(cooldown) + "]" : ""));
            entry.getValue().setDisabled(snapshot.isFinished());
        }
        plantSelectionLabel.setText("Mouse: " + selected);
    }

    private void updateZombieControls(GameSnapshot snapshot) {
        String selected = controller.getSelectedZombie();
        int i = 1;
        for (String type : controller.getZombieTypes()) {
            Label label = zombieLabels.get(type);
            if (label != null) label.setText((type.equals(selected) ? "> " : "") + i + " = " + type + " "
                    + controller.zombieCost(type) + (snapshot.getZombieCooldownMillis(type) > 0L
                    ? " [" + seconds(snapshot.getZombieCooldownMillis(type)) + "]" : ""));
            i++;
        }
        zombieSelectionLabel.setText("Zombie: " + selected + "\nLane " + (controller.getZombieRow() + 1));
    }

    private static String seconds(long millis) { return String.format(Locale.US, "%.1fs", Math.max(0L, millis) / 1000.0); }

    private void showEnd(GameSnapshot snapshot) {
        if (endShown) return;
        endShown = true;
        new ConfirmDialog(
                snapshot.getWinner() == GameRole.PLANTS ? "Plants Win" : "Zombies Win",
                snapshot.getFinishReason(), game.getSkin(),
                () -> game.getScreenManager().showCouchIZombie(requestedStage),
                () -> game.getScreenManager().showNetworkLobby(requestedStage)
        ).show(stage);
    }

    private void confirmExit() {
        if (controller.isFinished()) { game.getScreenManager().showNetworkLobby(requestedStage); return; }
        new ConfirmDialog("Exit Couch Match", "End this local two-player match?", game.getSkin(),
                () -> game.getScreenManager().showNetworkLobby(requestedStage)).show(stage);
    }

    private void setStatus(String value) { if (statusLabel != null) statusLabel.setText(value == null ? "" : value); }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) confirmExit();
        handleZombieKeyboard();
        if (!controller.isFinished()) {
            pendingMillis += Math.min(delta, 0.1f) * 1000.0;
            long advance = (long) pendingMillis;
            if (advance > 0L) { pendingMillis -= advance; controller.advance(advance); }
        }
        refresh(controller.snapshot());
        super.render(delta);
    }

    @Override
    public void dispose() {
        if (board != null) board.dispose();
        super.dispose();
    }
}
