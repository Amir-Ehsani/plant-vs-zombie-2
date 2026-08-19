package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class GameNotification extends Table {
    private static final float DEFAULT_DURATION = 3.5f;
    private final Label messageLabel;
    private float remainingTime;

    public GameNotification(Skin skin, String message, NotificationType type) {
        this(skin, message, type, DEFAULT_DURATION);
    }

    public GameNotification(Skin skin, String message, NotificationType type, float duration) {
        messageLabel = new Label(message == null ? "" : message, skin);
        messageLabel.setWrap(true);
        messageLabel.setColor(colorFor(type));
        remainingTime = Math.max(0.5f, duration);
        pad(12f);
        add(messageLabel).width(560f);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        remainingTime -= delta;
        if (remainingTime <= 0f) {
            remove();
        }
    }

    public void setMessage(String message) {
        messageLabel.setText(message == null ? "" : message);
    }

    private Color colorFor(NotificationType type) {
        if (type == NotificationType.ERROR) {
            return Color.SCARLET;
        }
        if (type == NotificationType.SUCCESS) {
            return Color.GREEN;
        }
        if (type == NotificationType.WARNING) {
            return Color.GOLD;
        }
        return Color.WHITE;
    }
}
