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
        if (controllerMessage == null || controllerMessage.isBlank()) {
            return;
        }
        for (String line : controllerMessage.split("\\R")) {
            String clean = cleanSingleLine(line);
            if (clean.startsWith("Sun collected at")
                || clean.startsWith("Radioactive sun exploded at")) {
                notifications.push(clean, NotificationType.INFO);
            }
        }
    }

    private List<String> feedbackLines(String message) {
        List<String> result = new ArrayList<>();
        if (message == null || message.isBlank()) {
            return result;
        }
        for (String line : message.split("\\R")) {
            String clean = cleanSingleLine(line);
            if (isDropFeedback(clean) || isMowerFeedback(clean)) {
                result.add(clean);
            }
        }
        return result;
    }

    private boolean isDropFeedback(String line) {
        return line.startsWith("A zombie dropped a plant food")
            || line.startsWith("A zombie dropped 50 coins")
            || line.startsWith("A zombie dropped 1 diamond")
            || line.startsWith("A zombie dropped a greenhouse pot");
    }

    private boolean isMowerFeedback(String line) {
        return line.startsWith("The lawn mower in the row");
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
