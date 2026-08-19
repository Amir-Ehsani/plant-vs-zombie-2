package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class ConfirmDialog extends Dialog {
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
        text(message == null ? "" : message);
        button("Cancel", Boolean.FALSE);
        button("Confirm", Boolean.TRUE);
    }

    @Override
    protected void result(Object object) {
        boolean confirmed = Boolean.TRUE.equals(object);
        if (confirmed && onConfirmed != null) {
            onConfirmed.run();
        }
        if (!confirmed && onCancelled != null) {
            onCancelled.run();
        }
    }
}
