package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class LoadingOverlay extends Table {
    private final Label messageLabel;

    public LoadingOverlay(Skin skin) {
        messageLabel = new Label("Loading...", skin);
        setFillParent(true);
        setVisible(false);
        center();
        add(messageLabel);
    }

    public void show(String message) {
        messageLabel.setText(message == null || message.isBlank() ? "Loading..." : message);
        setVisible(true);
        toFront();
    }

    public void hideOverlay() {
        setVisible(false);
    }
}
