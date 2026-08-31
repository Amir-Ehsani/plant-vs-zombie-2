package network.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.pvz.Main;
import network.client.NetworkManager;
import network.client.NetworkMatchContext;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import ui.ConfirmDialog;
import models.minigame.NetworkIZombieGame;
import ui.NotificationManager;

/** Routes unsolicited server events to the active LibGDX screen. */
public final class NetworkCoordinator {
    private final Main game;
    private final NetworkManager network;
    private String visibleChallengeId;
    private String openedMatchId;

    public NetworkCoordinator(Main game, NetworkManager network) {
        this.game = game;
        this.network = network;
    }

    public void pump(Stage stage) {
        if (network == null) return;
        network.pumpEvents();
        routeSystemEvents();
        routeChallenges(stage);
        routeMatches();
    }

    private void routeSystemEvents() {
        NetworkMessage event;
        while ((event = network.pollSystemEvent()) != null) {
            if (event.getType() == MessageType.SESSION_REPLACED) {
                String message = eventMessage(event, "Your server session was replaced. Please sign in again.");
                Gdx.app.postRunnable(() -> game.getScreenManager().showLogin(message));
            } else if (event.getType() == MessageType.CHALLENGE_RESPONSE) {
                String message = eventMessage(event, "Challenge response received.");
                if (event.getBoolean("accepted", false)) NotificationManager.showSuccess(message);
                else NotificationManager.showWarning(message);
            } else {
                String message = event.get("message");
                if (message != null && !message.isBlank()) NotificationManager.showInfo(message);
            }
        }
    }

    private void routeChallenges(Stage stage) {
        if (stage == null || game.getTravelLogController().getActiveMiniGameSession() instanceof NetworkIZombieGame) return;
        NetworkMessage event = network.pollChallengeEvent();
        if (event == null) return;
        String challengeId = event.getOrDefault("challengeId", "");
        if (challengeId.isBlank() || challengeId.equals(visibleChallengeId)) return;
        visibleChallengeId = challengeId;
        String challenger = event.getOrDefault("challenger", "Another player");
        ConfirmDialog dialog = new ConfirmDialog(
                "I, Zombie Online",
                challenger + " invited you to an online I, Zombie match.",
                game.getSkin(),
                () -> answerChallenge(challengeId, true),
                () -> answerChallenge(challengeId, false)
        );
        dialog.show(stage);
    }

    private void answerChallenge(String challengeId, boolean accepted) {
        visibleChallengeId = null;
        network.respondToChallengeAsync(challengeId, accepted);
    }

    private void routeMatches() {
        NetworkMessage event;
        while ((event = network.pollMatchStartedEvent()) != null) {
            NetworkMatchContext context = NetworkMatchContext.from(event);
            if (context == null || context.matchId().equals(openedMatchId)) continue;
            openedMatchId = context.matchId();
            Gdx.app.postRunnable(() -> game.getScreenManager().showNetworkIZombie(context));
        }
        if (network.getActiveMatch() == null) openedMatchId = null;
    }

    private static String eventMessage(NetworkMessage event, String fallback) {
        String message = event == null ? null : event.get("message");
        return message == null || message.isBlank() ? fallback : message;
    }
}
