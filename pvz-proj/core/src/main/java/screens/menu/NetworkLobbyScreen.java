package screens.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.pvz.Main;
import network.client.NetworkConfig;
import network.client.NetworkManager;
import network.client.NetworkOperationResult;
import ui.BackButton;
import ui.MenuButton;

import java.util.concurrent.CompletableFuture;

/** Matchmaking entry point for the phase-three network I, Zombie mode. */
public final class NetworkLobbyScreen extends BaseMenuScreen {
    private final NetworkManager network;
    private Label statusLabel;
    private TextField hostField;
    private TextField portField;
    private TextField opponentField;
    private MenuButton randomButton;
    private boolean waitingRandom;
    private boolean busy;

    public NetworkLobbyScreen(Main game) {
        super(game);
        this.network = game.getNetworkManager();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        updateStatus(initialStatus());
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        Table panel = createPanel();
        panel.pad(22f);

        Label title = createTitle("I, ZOMBIE - ONLINE");
        title.setColor(Color.WHITE);
        panel.add(title).colspan(2).padBottom(12f).row();
        panel.add(panelLabel("Server-authoritative two-player match"))
                .colspan(2).padBottom(14f).row();

        NetworkConfig config = network.getConfig();
        Table connection = new Table();
        connection.add(panelLabel("Server")).padRight(8f);
        hostField = new TextField(config.getHost(), skin);
        portField = new TextField(String.valueOf(config.getPort()), skin);
        connection.add(hostField).width(210f).height(40f).padRight(6f);
        connection.add(portField).width(90f).height(40f).padRight(8f);
        connection.add(new MenuButton("Check", skin, "green_small", this::connect))
                .width(120f).height(40f);
        panel.add(connection).colspan(2).padBottom(16f).row();

        Table direct = createPanel();
        direct.pad(16f);
        direct.add(sectionTitle("Challenge Player")).padBottom(10f).row();
        Label directHelp = panelLabel("Enter an online username. The opponent can accept or reject the invitation.");
        directHelp.setWrap(true);
        directHelp.setAlignment(Align.center);
        direct.add(directHelp).width(390f).height(56f).row();
        opponentField = new TextField("", skin);
        opponentField.setMessageText("Opponent username");
        direct.add(opponentField).width(300f).height(42f).padTop(8f).row();
        direct.add(new MenuButton("Send Challenge", skin, "purple", this::challenge))
                .width(235f).height(46f).padTop(10f);

        Table random = createPanel();
        random.pad(16f);
        random.add(sectionTitle("Matchmaking")).padBottom(10f).row();
        Label randomHelp = panelLabel("Join the random queue or play the same two-sided rules locally on one device.");
        randomHelp.setWrap(true);
        randomHelp.setAlignment(Align.center);
        random.add(randomHelp).width(390f).height(56f).row();
        randomButton = new MenuButton("Random Match", skin, "green", this::toggleRandom);
        random.add(randomButton).width(235f).height(46f).padTop(8f).row();
        random.add(new MenuButton("Couch Play", skin, "brown", () -> game.getScreenManager().showCouchIZombie(1)))
                .width(235f).height(46f).padTop(8f);

        panel.add(direct).width(440f).height(250f).padRight(12f);
        panel.add(random).width(440f).height(250f).row();

        statusLabel = panelLabel("");
        statusLabel.setWrap(true);
        statusLabel.setAlignment(Align.center);
        panel.add(statusLabel).colspan(2).width(850f).height(55f).padTop(12f).row();
        panel.add(new BackButton(skin, game.getScreenManager()::showMiniGames))
                .colspan(2).width(180f).height(44f).padTop(8f);
        root.add(panel).width(980f).height(610f);
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text, skin, "medium_outline");
        label.setColor(Color.WHITE);
        label.setAlignment(Align.center);
        return label;
    }

    private Label panelLabel(String text) {
        Label label = new Label(text == null ? "" : text, skin, "secondary");
        label.setColor(Color.valueOf("4A3A1F"));
        return label;
    }

    private String initialStatus() {
        if (network.isAuthenticated()) {
            return "Online as " + network.getAuthenticatedUsername() + " at " + network.getConfig().endpoint();
        }
        return "No active server session. Sign in against the server before matchmaking.";
    }

    private void connect() {
        if (busy) {
            return;
        }
        int port;
        try { port = Integer.parseInt(portField.getText().trim()); }
        catch (RuntimeException exception) { updateStatus("Port must be a number."); return; }
        if (port < 1 || port > 65535) {
            updateStatus("Port must be between 1 and 65535."); return;
        }
        String host = hostField.getText() == null ? "" : hostField.getText().trim();
        if (host.isBlank()) {
            updateStatus("Server host is required."); return;
        }
        network.configure(host, port);
        run("Checking server...", network.statusAsync());
    }

    private void challenge() {
        if (busy || !requireOnline()) {
            return;
        }
        String target = opponentField.getText() == null ? "" : opponentField.getText().trim();
        if (target.isBlank()) {
            updateStatus("Enter the opponent username."); return;
        }
        run("Sending challenge to " + target + "...", network.challengeAsync(target));
    }

    private void toggleRandom() {
        if (busy || !requireOnline()) {
            return;
        }
        boolean joining = !waitingRandom;
        CompletableFuture<NetworkOperationResult> future = joining
                ? network.joinRandomQueueAsync()
                : network.leaveRandomQueueAsync();
        busy = true;
        updateStatus(joining ? "Joining random queue..." : "Leaving random queue...");
        future.whenComplete((result, error) -> Gdx.app.postRunnable(() -> {
            busy = false;
            if (error != null) {
                updateStatus("Network error: " + error.getMessage()); return;
            }
            if (result != null && result.successful()) {
                waitingRandom = joining;
            }
            randomButton.setText(waitingRandom ? "Leave Queue" : "Random Match");
            updateStatus(result == null ? "Server returned no result." : result.message());
        }));
    }

    private boolean requireOnline() {
        if (network.isAuthenticated()) {
            return true;
        }
        updateStatus("Your server login is not active. Return to Login and sign in again.");
        return false;
    }

    private void run(String pending, CompletableFuture<NetworkOperationResult> future) {
        busy = true;
        updateStatus(pending);
        future.whenComplete((result, error) -> Gdx.app.postRunnable(() -> {
            busy = false;
            if (error != null) {
                updateStatus("Network error: " + error.getMessage());
            }
            else {
                updateStatus(result == null ? "Server returned no result." : result.message());
            }
        }));
    }

    private void updateStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text == null ? "" : text);
        }
    }

    @Override
    public void hide() {
        if (waitingRandom) {
            network.leaveRandomQueueAsync();
        }
        super.hide();
    }
}
