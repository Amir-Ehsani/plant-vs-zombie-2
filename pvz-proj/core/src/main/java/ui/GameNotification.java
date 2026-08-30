package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class GameNotification extends Table {
    private static final float DEFAULT_DURATION = 3.5f;
    private static final float ENTER_SECONDS = 0.15f;
    private static final float EXIT_SECONDS = 0.18f;
    private final Label messageLabel;

    public GameNotification(Skin skin, String message, NotificationType type) {
        this(skin, message, type, DEFAULT_DURATION);
    }

    public GameNotification(Skin skin, String message, NotificationType type, float duration) {
        setTransform(true);
        setBackground(safeDrawable(skin));
        pad(10f, 16f, 10f, 16f);
        messageLabel = new Label(message == null ? "" : message, skin, "medium_outline");
        messageLabel.setWrap(true);
        messageLabel.setColor(colorFor(type));
        add(messageLabel).width(430f).center();

        getColor().a = 0f;
        setScale(0.94f);
        float holdSeconds = Math.max(0.5f, duration);
        addAction(Actions.sequence(
                Actions.parallel(
                        Actions.fadeIn(ENTER_SECONDS, Interpolation.fade),
                        Actions.scaleTo(1f, 1f, ENTER_SECONDS, Interpolation.sineOut)
                ),
                Actions.delay(holdSeconds),
                Actions.parallel(
                        Actions.fadeOut(EXIT_SECONDS, Interpolation.fade),
                        Actions.scaleTo(0.96f, 0.96f, EXIT_SECONDS, Interpolation.sineIn)
                ),
                Actions.removeActor()
        ));
    }

    public void setMessage(String message) {
        messageLabel.setText(message == null ? "" : message);
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable safeDrawable(Skin skin) {
        try {
            return skin.getDrawable("image_ui_quests_panel_edge_to_edge_ten");
        } catch (Exception ignored) {
            return null;
        }
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
