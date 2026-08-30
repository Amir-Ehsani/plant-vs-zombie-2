package screens.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
import ui.ResourceBar;
import ui.SeedPacketCatalog;
import ui.UiHoverAnimator;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Online I, Zombie using the same lawn proportions and packet UI as the local
 * MiniGameScreen. Only synchronization/matchmaking are network-specific.
 */
public final class NetworkIZombieScreen extends BaseScreen {
    private static final float BOARD_X = 315f;
    private static final float BOARD_Y = 74f;
    private static final float BOARD_WIDTH = 920f;
    private static final float BOARD_HEIGHT = 457f;

    private final NetworkManager network;
    private final NetworkMatchContext context;
    private final ResourceBar resourceBar;
    private final Map<String, ChoiceVisual> choiceVisuals = new LinkedHashMap<>();
    private IZombieBoardActor board;
    private ReactionGraphicActor reactionGraphic;
    private Table reactionTray;
    private Label timerLabel;
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
        this.resourceBar = new ResourceBar(game.getSkin(), game.getAnimationService());
        Map<String, Integer> available = choices();
        selectedType = available.isEmpty() ? "" : available.keySet().iterator().next();
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

        buildTopHud();
        buildChoiceBar();
        buildNetworkStatus();
        buildReactionUi();
        refreshChoices();
    }

    private void buildTopHud() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(10f);
        hud.add().expandX();
        hud.add(createPauseButton()).size(54f).padRight(8f).top();
        resourceBar.showMiniGameResources();
        hud.add(resourceBar).right().top();
        stage.addActor(hud);

        timerLabel = new Label("02:00", game.getSkin(), "medium_outline");
        timerLabel.setColor(Color.WHITE);
        timerLabel.setAlignment(Align.center);
        timerLabel.setBounds(570f, 668f, 140f, 38f);
        stage.addActor(timerLabel);
    }

    private Button createPauseButton() {
        TextureRegion region = game.getAnimationService().region("IMAGE_UI_HUD_INGAME_PAUSE_BUTTON");
        if (region == null) {
            return new MenuButton("Pause", game.getSkin(), "brown", this::confirmLeave);
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
                confirmLeave();
            }
        });
        return button;
    }

    private void buildChoiceBar() {
        Table host = new Table();
        host.setFillParent(true);
        host.left().top().padLeft(14f).padTop(68f);
        Table cards = new Table();
        cards.top().left();
        cards.defaults().padBottom(5f);
        for (Map.Entry<String, Integer> entry : choices().entrySet()) {
            ChoiceVisual visual = createChoiceCard(entry.getKey(), entry.getValue());
            choiceVisuals.put(entry.getKey(), visual);
            cards.add(visual.root).width(132f).height(88f).row();
        }
        host.add(cards).top().left();
        stage.addActor(host);
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
        content.setFillParent(true);
        Actor actor = choiceActor(type);
        actor.setTouchable(Touchable.disabled);
        content.add(actor).width(72f).height(72f).padLeft(2f).padRight(2f);

        Table info = new Table();
        Label name = new Label(choiceDisplayName(type), game.getSkin(), "secondary");
        name.setColor(Color.valueOf("4A3A1F"));
        name.setWrap(true);
        name.setAlignment(Align.center);
        info.add(name).width(52f).center().row();
        Label costLabel = new Label(String.valueOf(cost), game.getSkin(), "secondary");
        costLabel.setColor(Color.valueOf("4A3A1F"));
        costLabel.setAlignment(Align.center);
        info.add(costLabel).center().padTop(4f);
        content.add(info).width(54f).expandY().center();
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

    private Actor choiceActor(String type) {
        if (context.role() == GameRole.ZOMBIES) {
            return game.getAnimationService().createZombieActor(IZombieBoardActor.zombieDisplayName(type));
        }
        TextureRegion packet = SeedPacketCatalog.region(
                game.getAnimationService(), IZombieBoardActor.plantDisplayName(type)
        );
        if (packet != null) {
            Image packetImage = new Image(new TextureRegionDrawable(packet));
            packetImage.setScaling(Scaling.fit);
            return packetImage;
        }
        PamAnimationActor plant = game.getAnimationService().createPlantActor(IZombieBoardActor.plantDisplayName(type));
        return plant;
    }

    private String choiceDisplayName(String type) {
        return context.role() == GameRole.ZOMBIES
                ? IZombieBoardActor.zombieDisplayName(type)
                : IZombieBoardActor.plantDisplayName(type);
    }

    private void buildNetworkStatus() {
        statusLabel = new Label("", game.getSkin(), "secondary");
        statusLabel.setColor(Color.WHITE);
        statusLabel.setAlignment(Align.center);
        statusLabel.setBounds(330f, 20f, 610f, 30f);
        stage.addActor(statusLabel);

        Label match = new Label(roleText(), game.getSkin(), "secondary");
        match.setColor(Color.WHITE);
        match.setAlignment(Align.right);
        match.setBounds(920f, 26f, 320f, 28f);
        stage.addActor(match);

        MenuButton react = new MenuButton("React", game.getSkin(), "purple", this::toggleReactions);
        react.setBounds(1140f, 535f, 92f, 30f);
        stage.addActor(react);
    }

    private void buildReactionUi() {
        reactionTray = new Table();
        reactionTray.setBounds(765f, 60f, 480f, 142f);
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
                ? AuthoritativeIZombieGame.plantCostsForStage(context.stage())
                : AuthoritativeIZombieGame.zombieCostsForStage(context.stage());
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
        if (cooldown > 0L) { setStatus(choiceDisplayName(selectedType) + " is cooling down."); return false; }
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
        resourceBar.refreshMiniGame(game.getAuthController().getLoggedInUser(), sun);
        refreshChoices();
        if (next.isFinished()) showEnd(null);
    }

    private void refreshChoices() {
        int sun = snapshot == null ? 0 : context.role() == GameRole.PLANTS
                ? snapshot.getPlantSun() : snapshot.getZombieSun();
        for (Map.Entry<String, ChoiceVisual> entry : choiceVisuals.entrySet()) {
            String type = entry.getKey();
            ChoiceVisual visual = entry.getValue();
            int cost = choices().getOrDefault(type, 0);
            long cooldown = snapshot == null ? 0L : context.role() == GameRole.PLANTS
                    ? snapshot.getPlantCooldownMillis(type) : snapshot.getZombieCooldownMillis(type);
            boolean enabled = snapshot != null && !snapshot.isFinished() && !actionInFlight
                    && sun >= cost && cooldown <= 0L;
            if (visual.selection != null) visual.selection.setVisible(type.equals(selectedType));
            visual.cooldown.setText(cooldown > 0L ? String.format(Locale.US, "%.1f", cooldown / 1000.0) : "");
            visual.root.setColor(1f, 1f, 1f, enabled ? 1f : 0.58f);
            visual.root.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
        }
        if (board != null) {
            board.setInteraction(context.role(), snapshot != null && !snapshot.isFinished() && !actionInFlight);
        }
    }

    private void showReaction(NetworkMessage event) {
        String category = event.getOrDefault("category", "TEXT");
        String value = event.getOrDefault("value", "");
        reactionLabel.clearActions();
        reactionLabel.setText(event.getOrDefault("sender", context.opponent())
                + ("TEXT".equals(category) ? ": " + value : " reacted"));
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
                Actions.repeat(3, Actions.sequence(
                        Actions.scaleTo(1.25f, 1.25f, 0.16f),
                        Actions.scaleTo(1f, 1f, 0.16f)
                )),
                Actions.delay(1.0f), Actions.fadeOut(0.3f), Actions.visible(false)));
    }

    private void showEnd(NetworkMessage event) {
        if (endShown || snapshot == null || !snapshot.isFinished()) return;
        endShown = true;
        boolean won = snapshot.getWinner() == context.role();
        if (won) markStageCompleted();
        String reason = snapshot.getFinishReason();
        if ((reason == null || reason.isBlank()) && event != null) {
            reason = event.getOrDefault("message", "Match finished.");
        }
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
        if (statusLabel != null) statusLabel.setText(text == null ? "" : text);
    }

    private static String shortText(String value) {
        return value == null ? "" : value.replace("!", "");
    }

    private static String stickerShort(String value) {
        return value == null ? "" : value
                .replace("DANCING_", "")
                .replace("DIZZY_", "")
                .replace("BOUNCING_", "");
    }

    @Override
    public void render(float delta) {
        processEvents();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) confirmLeave();
        UiHoverAnimator.attach(stage);
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
