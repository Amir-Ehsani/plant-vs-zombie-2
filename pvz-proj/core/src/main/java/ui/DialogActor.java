package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class DialogActor extends ModalWindow {
    private final Label messageLabel;

    public DialogActor(String title, String message, Skin skin) {
        super(title, skin);
        Table content = getContentTable();
        messageLabel = new Label(message == null ? "" : message, skin, "medium");
        messageLabel.setColor(Color.valueOf("FFFBEA"));
        messageLabel.setWrap(true);
        content.add(messageLabel).width(520f).padBottom(14f).row();
        content.add(new MenuButton("Close", skin, "brown", this::close)).width(180f).height(46f);
    }

    public void setMessage(String message) {
        messageLabel.setText(message == null ? "" : message);
    }
}
