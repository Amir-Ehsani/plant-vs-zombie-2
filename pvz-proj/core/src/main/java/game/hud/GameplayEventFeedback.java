package game.hud;

import ui.NotificationManager;
import ui.NotificationType;

import java.util.ArrayList;
import java.util.List;

public final class GameplayEventFeedback {
    private final NotificationManager notifications;
    private int lastTick = -1;

    public GameplayEventFeedback(NotificationManager notifications) {
        if (notifications == null) {
            throw new IllegalArgumentException("Notification manager cannot be null.");
        }
        this.notifications = notifications;
    }

    public void acceptTickMessage(int tick, String controllerMessage) {
        if (tick == lastTick) {
            return;
        }
        lastTick = tick;
        for (String line : feedbackLines(controllerMessage)) {
            notifications.push(line, NotificationType.INFO);
        }
    }

    public void showSunCollection(String controllerMessage) {
        // Sun collection happens constantly during gameplay and does not need a toast.
    }

    private List<String> feedbackLines(String message) {
        List<String> result = new ArrayList<>();
        if (message == null || message.isBlank()) {
            return result;
        }
        for (String line : message.split("\\R")) {
            String clean = cleanSingleLine(line);
            if (isDropFeedback(clean)) {
                result.add(clean);
            }
        }
        return result;
    }

    private boolean isDropFeedback(String line) {
        return line.startsWith("A zombie dropped a plant food")
            || line.startsWith("A reward grave released")
            || line.startsWith("A zombie dropped 1 diamond")
            || line.startsWith("A zombie dropped a greenhouse pot");
    }


    private String cleanSingleLine(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.trim();
        if (clean.startsWith("OK: ")) {
            return clean.substring(4).trim();
        }
        if (clean.startsWith("ERROR: ")) {
            return clean.substring(7).trim();
        }
        return clean;
    }
}
