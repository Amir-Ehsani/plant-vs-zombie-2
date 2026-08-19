package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class DialogActor extends ModalWindow {
    private final Label messageLabel;

    public DialogActor(String title, String message, Skin skin) {
        super(title, skin);
        messageLabel = new Label(message == null ? "" : message, skin);
        messageLabel.setWrap(true);
        add(messageLabel).width(520f).padBottom(14f).row();
        add(new MenuButton("Close", skin, this::close)).width(180f).height(46f);
    }

    public void setMessage(String message) {
        messageLabel.setText(message == null ? "" : message);
    }
}
