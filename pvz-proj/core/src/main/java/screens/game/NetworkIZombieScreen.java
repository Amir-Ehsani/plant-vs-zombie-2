package screens.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.pvz.Main;
import models.account.User;
import network.client.NetworkManager;
import network.client.NetworkMatchContext;
import network.client.NetworkOperationResult;
import network.game.AuthoritativeIZombieGame;
import network.protocol.EntityState;
import network.protocol.GameRole;
import network.protocol.GameSnapshot;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.ReactionCatalog;
import network.protocol.ReactionCategory;
import network.ui.IZombieBoardActor;
import network.ui.ReactionGraphicActor;
import screens.BaseScreen;
import ui.ConfirmDialog;
import ui.MenuButton;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Graphical client for the server-authoritative two-player I, Zombie match. */
public final class NetworkIZombieScreen extends BaseScreen {
    private final NetworkManager network;
    private final NetworkMatchContext context;
    private final Map<String, TextButton> choiceButtons = new LinkedHashMap<>();
    private IZombieBoardActor board;
    private ReactionGraphicActor reactionGraphic;
    private Label timerLabel;
    private Label resourceLabel;
    private Label statusLabel;
    private Label selectedLabel;
    private Label reactionLabel;
    private GameSnapshot snapshot;
    private long lastSequence = -1L;
    private String selectedType;
    private boolean actionInFlight;
    private boolean reactionInFlight;
    private long reactionCooldownUntil;
    private boolean endShown;

    public NetworkIZombieScreen(Main game, NetworkMatchContext context) {
        super(game);
        this.network = game.getNetworkManager();
        this.context = context;
        Map<String, Integer> choices = choices();
        this.selectedType = choices.isEmpty() ? "" : choices.keySet().iterator().next();
        network.clearMatchEvents();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        statusLabel.setText("Connected to " + context.opponent() + ". Waiting for synchronized server state...");
    }

    private void buildUi() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(12f);

        Table header = new Table();
        Label title = new Label("I, ZOMBIE ONLINE - " + context.role(), game.getSkin(), "big_outline");
        title.setColor(Color.WHITE);
        timerLabel = new Label("02:00", game.getSkin(), "medium_outline");
        timerLabel.setColor(Color.WHITE);
        resourceLabel = new Label("SUN -   BRAINS -", game.getSkin(), "secondary");
        resourceLabel.setColor(Color.WHITE);
        header.add(title).left().expandX();
        header.add(timerLabel).width(120f).center();
        header.add(resourceLabel).width(290f).right();
        root.add(header).growX().height(56f).row();

        Table body = new Table();
        board = new IZombieBoardActor(this::boardSelected);
        body.add(board).width(890f).height(500f).padRight(10f);
        body.add(buildControls()).width(340f).height(500f);
        root.add(body).grow().row();

        statusLabel = new Label("", game.getSkin(), "secondary");
        statusLabel.setColor(Color.WHITE);
        statusLabel.setWrap(true);
        statusLabel.setAlignment(Align.center);
        root.add(statusLabel).growX().height(48f).padTop(4f);
        stage.addActor(root);

        reactionLabel = new Label("", game.getSkin(), "medium_outline");
        reactionLabel.setColor(Color.YELLOW);
        reactionLabel.setWrap(true);
        reactionLabel.setAlignment(Align.center);
        reactionLabel.setBounds(930f, 580f, 320f, 70f);
        reactionLabel.setVisible(false);
        stage.addActor(reactionLabel);
        reactionGraphic = new ReactionGraphicActor();
        reactionGraphic.setPosition(1060f, 480f);
        stage.addActor(reactionGraphic);
    }

    private Table buildControls() {
        Table controls = new Table();
        controls.top();
        Label opponent = new Label("vs " + context.opponent() + "  |  Stage " + context.stage(), game.getSkin(), "secondary");
        opponent.setColor(Color.WHITE);
        controls.add(opponent).padBottom(8f).row();

        selectedLabel = new Label("Selected: " + selectedType, game.getSkin(), "secondary");
        selectedLabel.setColor(Color.WHITE);
        controls.add(selectedLabel).padBottom(5f).row();

        for (Map.Entry<String, Integer> entry : choices().entrySet()) {
            String type = entry.getKey();
            TextButton button = new TextButton(type + "  " + entry.getValue(), game.getSkin(), "brown");
            button.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    selectType(type);
                }
            });
            choiceButtons.put(type, button);
            controls.add(button).width(285f).height(38f).padBottom(3f).row();
        }

        controls.add(new Label("REACTIONS", game.getSkin(), "medium_outline")).padTop(8f).padBottom(4f).row();
        Table reactions = new Table();
        for (String text : ReactionCatalog.texts()) {
            reactions.add(new MenuButton(shortText(text), game.getSkin(), "green_small",
                    () -> sendReaction(ReactionCategory.TEXT, text))).width(93f).height(32f).pad(2f);
        }
        reactions.row();
        for (String emoji : ReactionCatalog.emojis()) {
            reactions.add(new MenuButton(emoji, game.getSkin(), "purple",
                    () -> sendReaction(ReactionCategory.EMOJI, emoji))).width(93f).height(32f).pad(2f);
        }
        reactions.row();
        for (String sticker : ReactionCatalog.stickers()) {
            reactions.add(new MenuButton(stickerShort(sticker), game.getSkin(), "brown",
                    () -> sendReaction(ReactionCategory.STICKER, sticker))).width(93f).height(32f).pad(2f);
        }
        controls.add(reactions).row();
        controls.add(new MenuButton("Leave Match", game.getSkin(), "brown", this::confirmLeave))
                .width(210f).height(38f).padTop(10f);
        return controls;
    }

    private Map<String, Integer> choices() {
        return context.role() == GameRole.PLANTS
                ? AuthoritativeIZombieGame.plantCosts()
                : AuthoritativeIZombieGame.zombieCosts();
    }

    private void selectType(String type) {
        selectedType = type;
        selectedLabel.setText("Selected: " + type);
        refreshChoices();
    }

    private void boardSelected(int row, int column) {
        if (snapshot == null || snapshot.isFinished() || actionInFlight) return;
        if (!canUseSelected()) return;
        if (context.role() == GameRole.PLANTS && column > AuthoritativeIZombieGame.LAST_PLANT_COLUMN) {
            setStatus("Plants can only be placed on the defensive side.");
            return;
        }
        if (context.role() == GameRole.PLANTS && hasPlantAt(row, column)) {
            setStatus("That tile already has a plant.");
            return;
        }
        actionInFlight = true;
        refreshChoices();
        CompletableFuture<NetworkOperationResult> future = context.role() == GameRole.PLANTS
                ? network.placePlantAsync(context.matchId(), selectedType, row, column)
                : network.spawnZombieAsync(context.matchId(), selectedType, row);
        future.whenComplete((result, error) -> Gdx.app.postRunnable(() -> {
            actionInFlight = false;
            refreshChoices();
            if (error != null) setStatus("Network action failed: " + error.getMessage());
            else setStatus(result == null ? "No server response." : result.message());
        }));
    }

    private boolean canUseSelected() {
        int cost = choices().getOrDefault(selectedType, Integer.MAX_VALUE);
        int sun = context.role() == GameRole.PLANTS ? snapshot.getPlantSun() : snapshot.getZombieSun();
        long cooldown = context.role() == GameRole.PLANTS
                ? snapshot.getPlantCooldownMillis(selectedType)
                : snapshot.getZombieCooldownMillis(selectedType);
        if (sun < cost) { setStatus("Not enough sun for " + selectedType + "."); return false; }
        if (cooldown > 0L) { setStatus(selectedType + " is cooling down."); return false; }
        return true;
    }

    private boolean hasPlantAt(int row, int column) {
        for (EntityState entity : snapshot.getEntities()) {
            if (!"PLANT".equals(entity.getCategory()) || entity.getRow() != row) continue;
            int entityColumn;
            try { entityColumn = Integer.parseInt(entity.getAttribute("column")); }
            catch (RuntimeException ignored) { entityColumn = (int) Math.floor(entity.getX()); }
            if (entityColumn == column) return true;
        }
        return false;
    }

    private void sendReaction(ReactionCategory category, String value) {
        if (snapshot == null || snapshot.isFinished() || reactionInFlight) return;
        long now = System.currentTimeMillis();
        if (now < reactionCooldownUntil) { setStatus("Reaction is cooling down."); return; }
        reactionInFlight = true;
        reactionCooldownUntil = now + ReactionCatalog.COOLDOWN_MILLIS;
        network.sendReactionAsync(context.matchId(), category, value).whenComplete((result, error) ->
                Gdx.app.postRunnable(() -> {
                    reactionInFlight = false;
                    if (error != null) setStatus("Reaction failed: " + error.getMessage());
                    else if (result != null) setStatus(result.message());
                }));
    }

    private void processEvents() {
        NetworkMessage event;
        while ((event = network.pollMatchEvent()) != null) {
            if (event.getMatchId() != null && !context.matchId().equals(event.getMatchId())) continue;
            if (event.getType() == MessageType.MATCH_SNAPSHOT) applySnapshot(event.getSnapshot());
            else if (event.getType() == MessageType.REACTION_RECEIVED) showReaction(event);
            else if (event.getType() == MessageType.MATCH_FINISHED) {
                applySnapshot(event.getSnapshot());
                showEnd(event);
            }
        }
    }

    private void applySnapshot(GameSnapshot next) {
        if (next == null || next.getSequence() <= lastSequence) return;
        snapshot = next;
        lastSequence = next.getSequence();
        board.setSnapshot(next);
        board.setInteraction(context.role(), !next.isFinished() && !actionInFlight);
        long seconds = (next.getRemainingMillis() + 999L) / 1000L;
        timerLabel.setText(String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L));
        int sun = context.role() == GameRole.PLANTS ? next.getPlantSun() : next.getZombieSun();
        resourceLabel.setText("SUN " + sun + "   BRAINS " + next.getBrainsRemaining());
        refreshChoices();
        if (next.isFinished()) showEnd(null);
    }

    private void refreshChoices() {
        int sun = snapshot == null ? 0 : context.role() == GameRole.PLANTS ? snapshot.getPlantSun() : snapshot.getZombieSun();
        for (Map.Entry<String, TextButton> entry : choiceButtons.entrySet()) {
            String type = entry.getKey();
            TextButton button = entry.getValue();
            int cost = choices().getOrDefault(type, 0);
            long cooldown = snapshot == null ? 0L : context.role() == GameRole.PLANTS
                    ? snapshot.getPlantCooldownMillis(type) : snapshot.getZombieCooldownMillis(type);
            boolean enabled = snapshot != null && !snapshot.isFinished() && !actionInFlight && sun >= cost && cooldown <= 0L;
            button.setDisabled(!enabled);
            button.setText((type.equals(selectedType) ? "> " : "") + type + " " + cost
                    + (cooldown > 0L ? " [" + String.format(Locale.US, "%.1fs", cooldown / 1000.0) + "]" : ""));
        }
        if (board != null) board.setInteraction(context.role(), snapshot != null && !snapshot.isFinished() && !actionInFlight);
    }

    private void showReaction(NetworkMessage event) {
        String category = event.getOrDefault("category", "TEXT");
        String value = event.getOrDefault("value", "");
        reactionLabel.clearActions();
        reactionLabel.setText(event.getOrDefault("sender", context.opponent()) + ("TEXT".equals(category) ? ": " + value : " reacted"));
        reactionLabel.getColor().a = 1f;
        reactionLabel.setVisible(true);
        reactionLabel.addAction(Actions.sequence(Actions.delay(2.3f), Actions.fadeOut(0.35f), Actions.visible(false)));
        reactionGraphic.clearActions();
        reactionGraphic.setGraphic(category, value);
        reactionGraphic.getColor().a = 1f;
        reactionGraphic.setScale(1f);
        reactionGraphic.setRotation(0f);
        reactionGraphic.setVisible(true);
        reactionGraphic.addAction(Actions.sequence(
                Actions.repeat(3, Actions.sequence(Actions.scaleTo(1.25f, 1.25f, 0.16f), Actions.scaleTo(1f, 1f, 0.16f))),
                Actions.delay(1.0f), Actions.fadeOut(0.3f), Actions.visible(false)));
    }

    private void showEnd(NetworkMessage event) {
        if (endShown || snapshot == null || !snapshot.isFinished()) return;
        endShown = true;
        boolean won = snapshot.getWinner() == context.role();
        if (won) markStageCompleted();
        String reason = snapshot.getFinishReason();
        if ((reason == null || reason.isBlank()) && event != null) reason = event.getOrDefault("message", "Match finished.");
        ConfirmDialog dialog = new ConfirmDialog(
                won ? "Victory" : "Defeat",
                (reason == null ? "Match finished." : reason) + "\nWinner: " + snapshot.getWinner(),
                game.getSkin(),
                this::returnToLobby,
                this::returnToLobby
        );
        dialog.show(stage);
    }

    private void markStageCompleted() {
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) return;
        user.completeMiniGameStage("i-zombie", context.stage());
        game.getAuthController().saveUsers();
    }

    private void confirmLeave() {
        if (snapshot != null && snapshot.isFinished()) { returnToLobby(); return; }
        new ConfirmDialog("Leave Match", "Leave this online match?", game.getSkin(), () -> {
            network.leaveMatchAsync(context.matchId());
            returnToLobby();
        }).show(stage);
    }

    private void returnToLobby() {
        network.clearActiveMatch();
        game.getScreenManager().showNetworkLobby(context.stage());
    }

    private void setStatus(String text) { if (statusLabel != null) statusLabel.setText(text == null ? "" : text); }
    private static String shortText(String value) { return value == null ? "" : value.replace("!", ""); }
    private static String stickerShort(String value) { return value == null ? "" : value.replace("DANCING_", "").replace("DIZZY_", "").replace("BOUNCING_", ""); }

    @Override
    public void render(float delta) {
        processEvents();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) confirmLeave();
        super.render(delta);
    }

    @Override
    public void dispose() {
        if (board != null) board.dispose();
        if (reactionGraphic != null) reactionGraphic.dispose();
        super.dispose();
    }
}
