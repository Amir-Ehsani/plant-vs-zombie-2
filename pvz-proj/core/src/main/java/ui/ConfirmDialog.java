package ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class ConfirmDialog extends ModalWindow {
    private final Runnable onConfirmed;
    private final Runnable onCancelled;

    public ConfirmDialog(String title, String message, Skin skin, Runnable onConfirmed) {
        this(title, message, skin, onConfirmed, null);
    }

    public ConfirmDialog(
            String title,
            String message,
            Skin skin,
            Runnable onConfirmed,
            Runnable onCancelled
    ) {
        super(title == null ? "Confirm" : title, skin);
        this.onConfirmed = onConfirmed;
        this.onCancelled = onCancelled;
        buildContent(message, skin);
    }

    private void buildContent(String message, Skin skin) {
        Table content = getContentTable();
        Label label = new Label(message == null ? "" : message, skin, "medium");
        label.setWrap(true);
        content.add(label).width(500f).padBottom(18f).colspan(2).row();
        content.add(new MenuButton("Cancel", skin, "brown", this::cancel)).width(180f).height(48f).padRight(8f);
        content.add(new MenuButton("Confirm", skin, "green", this::confirm)).width(180f).height(48f);
    }

    private void confirm() {
        close();
        if (onConfirmed != null) {
            onConfirmed.run();
        }
    }

    private void cancel() {
        close();
        if (onCancelled != null) {
            onCancelled.run();
        }
    }

    @Override
    public void show(Stage stage) {
        super.show(stage);
    }
}
