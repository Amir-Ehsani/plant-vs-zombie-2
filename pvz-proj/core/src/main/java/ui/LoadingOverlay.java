package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import pvz.skin.BorderedTable;

public class LoadingOverlay extends Table {
    private final Label messageLabel;

    public LoadingOverlay(Skin skin) {
        setFillParent(true);
        setVisible(false);
        center();
        BorderedTable panel = new BorderedTable();
        panel.pad(24f);
        messageLabel = new Label("Loading...", skin, "medium_outline");
        panel.add(messageLabel);
        add(panel);
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
