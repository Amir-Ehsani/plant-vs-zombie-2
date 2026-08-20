package ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class NotificationManager {
    private static NotificationManager activeManager;
    private final Skin skin;
    private final Table host;

    public NotificationManager(Stage stage, Skin skin) {
        this.skin = skin;
        host = new Table();
        host.setFillParent(true);
        host.top();
        host.padTop(24f);
        host.setTouchable(Touchable.disabled);
        stage.addActor(host);
    }

    public void activate() {
        activeManager = this;
    }

    public void deactivate() {
        if (activeManager == this) {
            activeManager = null;
        }
    }

    public void push(String message, NotificationType type) {
        GameNotification notification = type == NotificationType.ERROR
                ? new ErrorToast(skin, message)
                : new GameNotification(skin, message, type);
        host.add(notification).padBottom(10f).row();
        host.toFront();
    }

    public static void showError(String message) {
        show(message, NotificationType.ERROR);
    }

    public static void showSuccess(String message) {
        show(message, NotificationType.SUCCESS);
    }

    public static void showWarning(String message) {
        show(message, NotificationType.WARNING);
    }

    public static void showInfo(String message) {
        show(message, NotificationType.INFO);
    }

    private static void show(String message, NotificationType type) {
        if (activeManager != null) {
            activeManager.push(message, type);
        }
    }
}
