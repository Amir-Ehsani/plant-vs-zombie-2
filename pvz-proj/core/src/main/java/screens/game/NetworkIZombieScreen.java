package screens.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
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
import ui.PamAnimationActor;
import ui.SeedPacketCatalog;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** PvZ-style graphical client for the server-authoritative two-player I, Zombie match. */
public final class NetworkIZombieScreen extends BaseScreen {
    private static final float BOARD_X = 18f;
    private static final float BOARD_Y = 42f;
    private static final float BOARD_WIDTH = 1244f;
    private static final float BOARD_HEIGHT = 565f;
    private static final float BANK_Y = 607f;
    private static final float BANK_HEIGHT = 108f;

    private final NetworkManager network;
    private final NetworkMatchContext context;
    private final Map<String, ChoiceVisual> choiceVisuals = new LinkedHashMap<>();
    private IZombieBoardActor board;
    private ReactionGraphicActor reactionGraphic;
    private Table reactionTray;
    private Label timerLabel;
    private Label sunLabel;
    private Label brainsLabel;
    private Label statusLabel;
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
        selectedType = choices.isEmpty() ? "" : choices.keySet().iterator().next();
        network.clearMatchEvents();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        setStatus("Waiting for synchronized server state...");
    }

    private void buildUi() {
        board = new IZombieBoardActor(game.getAnimationService(), this::boardSelected);
        board.setBounds(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
        stage.addActor(board);

        Table seedBank = new Table();
        seedBank.setBounds(12f, BANK_Y, 1256f, BANK_HEIGHT);
        seedBank.left().center();
        seedBank.add(createSunCounter()).width(116f).height(94f).padLeft(4f).padRight(5f);
        for (Map.Entry<String, Integer> entry : choices().entrySet()) {
            ChoiceVisual visual = createChoiceCard(entry.getKey(), entry.getValue());
            choiceVisuals.put(entry.getKey(), visual);
            seedBank.add(visual.root).width(139f).height(96f).padRight(4f);
        }
        seedBank.add(buildMatchInfo()).expandX().right().padRight(5f);
        stage.addActor(seedBank);

        statusLabel = new Label("", game.getSkin(), "secondary");
        statusLabel.setColor(Color.WHITE);
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(320f, 7f, 640f, 30f);
        stage.addActor(statusLabel);

        brainsLabel = new Label("5 BRAINS", game.getSkin(), "medium_outline");
        brainsLabel.setColor(Color.WHITE);
        brainsLabel.setAlignment(Align.center);
        brainsLabel.setBounds(500f, 8f, 280f, 32f);
        stage.addActor(brainsLabel);

        buildReactionUi();
        refreshChoices();
    }

    private Actor createSunCounter() {
        Stack stack = new Stack();
        TextureRegion base = game.getAnimationService().region("IMAGE_UI_GENERIC_BUTTON_GENERIC_CURRENCY_NORMAL");
        if (base != null) {
            Image background = new Image(new TextureRegionDrawable(base));
            background.setScaling(Scaling.fill);
            stack.add(background);
        }
        Table content = new Table();
        TextureRegion sun = game.getAnimationService().region("IMAGE_UI_HUD_INGAME_SUN_DOWN");
        if (sun == null) sun = game.getAnimationService().region("IMAGE_EFFECTS_SUN_SUN_110X110");
        if (sun != null) {
            Image icon = new Image(new TextureRegionDrawable(sun));
            icon.setScaling(Scaling.fit);
            content.add(icon).size(50f).padRight(2f);
        }
        sunLabel = new Label("0", game.getSkin(), "medium_outline");
        sunLabel.setColor(Color.WHITE);
        sunLabel.setAlignment(Align.center);
        content.add(sunLabel).width(48f);
        stack.add(content);
        return stack;
    }

    private ChoiceVisual createChoiceCard(String type, int cost) {
        Table card = new Table();
        card.setTransform(true);
        Stack stack = new Stack();

        TextureRegion base = game.getAnimationService().region("IMAGE_UI_PACKETS_SELECTED");
        if (base == null) base = game.getAnimationService().region("IMAGE_UI_PACKETS_HOMELESS");
        if (base != null) {
            Image image = new Image(new TextureRegionDrawable(base));
            image.setScaling(Scaling.fill);
            stack.add(image);
        }

        Table content = new Table();
        content.pad(4f);
        Actor visual;
        if (context.role() == GameRole.PLANTS) {
            TextureRegion packet = SeedPacketCatalog.region(game.getAnimationService(), IZombieBoardActor.plantDisplayName(type));
            if (packet != null) {
                Image packetImage = new Image(new TextureRegionDrawable(packet));
                packetImage.setScaling(Scaling.fit);
                visual = packetImage;
            } else {
                PamAnimationActor plant = game.getAnimationService().createPlantActor(IZombieBoardActor.plantDisplayName(type));
                visual = plant;
            }
        } else {
            visual = game.getAnimationService().createZombieActor(IZombieBoardActor.zombieDisplayName(type));
        }
        visual.setTouchable(Touchable.disabled);
        content.add(visual).size(78f, 67f).expandY().center().row();
        Label costLabel = new Label(String.valueOf(cost), game.getSkin(), "secondary");
        costLabel.setColor(Color.valueOf("4A3A1F"));
        costLabel.setAlignment(Align.center);
        content.add(costLabel).width(100f).height(20f).center();
        stack.add(content);

        Image selection = null;
        TextureRegion selected = game.getAnimationService().region("IMAGE_UI_PACKETS_SELECT");
        if (selected != null) {
            selection = new Image(new TextureRegionDrawable(selected));
            selection.setScaling(Scaling.fill);
            selection.setTouchable(Touchable.disabled);
            stack.add(selection);
        }

        Label cooldown = new Label("", game.getSkin(), "medium_outline");
        cooldown.setColor(Color.WHITE);
        cooldown.setAlignment(Align.center);
        cooldown.setTouchable(Touchable.disabled);
        stack.add(cooldown);

        card.add(stack).grow();
        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectType(type);
            }
        });
        return new ChoiceVisual(card, selection, cooldown);
    }

    private Table buildMatchInfo() {
        Table info = new Table();
        info.right();
        timerLabel = new Label("02:00", game.getSkin(), "medium_outline");
        timerLabel.setColor(Color.WHITE);
        timerLabel.setAlignment(Align.center);
        info.add(timerLabel).width(92f).height(34f).right().row();

        Label role = new Label(roleText(), game.getSkin(), "secondary");
        role.setColor(Color.WHITE);
        role.setAlignment(Align.right);
        role.setWrap(true);
        info.add(role).width(230f).height(36f).right().row();

        Table buttons = new Table();
        buttons.add(new MenuButton("React", game.getSkin(), "purple", this::toggleReactions))
                .width(92f).height(30f).padRight(4f);
        buttons.add(new MenuButton("Leave", game.getSkin(), "brown", this::confirmLeave))
                .width(92f).height(30f);
        info.add(buttons).right().padTop(2f);
        return info;
    }

    private void buildReactionUi() {
        reactionTray = new Table();
        reactionTray.setBounds(768f, 60f, 480f, 142f);
        reactionTray.bottom().right();
        Table panel = new Table();
        for (String text : ReactionCatalog.texts()) {
            panel.add(new MenuButton(shortText(text), game.getSkin(), "green_small",
                    () -> sendReaction(ReactionCategory.TEXT, text))).width(145f).height(32f).pad(2f);
        }
        panel.row();
        for (String emoji : ReactionCatalog.emojis()) {
            panel.add(new MenuButton(emoji, game.getSkin(), "purple",
                    () -> sendReaction(ReactionCategory.EMOJI, emoji))).width(145f).height(32f).pad(2f);
        }
        panel.row();
        for (String sticker : ReactionCatalog.stickers()) {
            panel.add(new MenuButton(stickerShort(sticker), game.getSkin(), "brown",
                    () -> sendReaction(ReactionCategory.STICKER, sticker))).width(145f).height(32f).pad(2f);
        }
        reactionTray.add(panel).right().bottom();
        reactionTray.setVisible(false);
        stage.addActor(reactionTray);

        reactionLabel = new Label("", game.getSkin(), "medium_outline");
        reactionLabel.setColor(Color.YELLOW);
        reactionLabel.setWrap(true);
        reactionLabel.setAlignment(Align.center);
        reactionLabel.setBounds(900f, 470f, 330f, 70f);
        reactionLabel.setVisible(false);
        stage.addActor(reactionLabel);

        reactionGraphic = new ReactionGraphicActor();
        reactionGraphic.setPosition(1060f, 400f);
        stage.addActor(reactionGraphic);
    }

    private String roleText() {
        String side = context.role() == GameRole.PLANTS ? "PLANTS" : "ZOMBIES";
        return side + "  vs  " + context.opponent() + "  •  Stage " + context.stage();
    }

    private void toggleReactions() {
        reactionTray.setVisible(!reactionTray.isVisible());
    }

    private Map<String, Integer> choices() {
        return context.role() == GameRole.PLANTS
                ? AuthoritativeIZombieGame.plantCosts()
                : AuthoritativeIZombieGame.zombieCosts();
    }

    private void selectType(String type) {
        selectedType = type;
        refreshChoices();
    }

    private void boardSelected(int row, int column) {
        if (snapshot == null || snapshot.isFinished() || actionInFlight) return;
        if (!canUseSelected()) return;
        if (context.role() == GameRole.PLANTS && column > AuthoritativeIZombieGame.LAST_PLANT_COLUMN) {
            setStatus("Plants can only be placed left of the red line.");
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
            else setStatus(result == null ? "" : result.message());
        }));
    }

    private boolean canUseSelected() {
        int cost = choices().getOrDefault(selectedType, Integer.MAX_VALUE);
        int sun = context.role() == GameRole.PLANTS ? snapshot.getPlantSun() : snapshot.getZombieSun();
        long cooldown = context.role() == GameRole.PLANTS
                ? snapshot.getPlantCooldownMillis(selectedType)
                : snapshot.getZombieCooldownMillis(selectedType);
        if (sun < cost) { setStatus("Not enough sun."); return false; }
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
                    reactionTray.setVisible(false);
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
        sunLabel.setText(String.valueOf(sun));
        brainsLabel.setText(next.getBrainsRemaining() + " / 5 BRAINS");
        refreshChoices();
        if (next.isFinished()) showEnd(null);
    }

    private void refreshChoices() {
        int sun = snapshot == null ? 0 : context.role() == GameRole.PLANTS ? snapshot.getPlantSun() : snapshot.getZombieSun();
        for (Map.Entry<String, ChoiceVisual> entry : choiceVisuals.entrySet()) {
            String type = entry.getKey();
            ChoiceVisual visual = entry.getValue();
            int cost = choices().getOrDefault(type, 0);
            long cooldown = snapshot == null ? 0L : context.role() == GameRole.PLANTS
                    ? snapshot.getPlantCooldownMillis(type) : snapshot.getZombieCooldownMillis(type);
            boolean enabled = snapshot != null && !snapshot.isFinished() && !actionInFlight && sun >= cost && cooldown <= 0L;
            visual.root.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
            visual.root.getColor().a = enabled || type.equals(selectedType) ? 1f : 0.48f;
            if (visual.selection != null) visual.selection.setVisible(type.equals(selectedType));
            visual.cooldown.setText(cooldown > 0L ? String.format(Locale.US, "%.1f", cooldown / 1000.0) : "");
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

    private void setStatus(String text) {
        if (statusLabel == null) return;
        statusLabel.clearActions();
        statusLabel.getColor().a = 1f;
        statusLabel.setText(text == null ? "" : text);
        if (text != null && !text.isBlank()) {
            statusLabel.addAction(Actions.sequence(Actions.delay(2.2f), Actions.fadeOut(0.35f)));
        }
    }

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

    private static final class ChoiceVisual {
        private final Table root;
        private final Image selection;
        private final Label cooldown;

        private ChoiceVisual(Table root, Image selection, Label cooldown) {
            this.root = root;
            this.selection = selection;
            this.cooldown = cooldown;
        }
    }
}
