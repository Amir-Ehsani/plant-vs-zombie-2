package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class ErrorToast extends GameNotification {
    public ErrorToast(Skin skin, String message) {
        super(skin, message, NotificationType.ERROR);
    }
}
