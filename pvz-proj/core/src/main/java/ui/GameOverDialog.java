package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

public class GameOverDialog extends ModalWindow {
    public GameOverDialog(
            Skin skin,
            boolean victory,
            String message,
            Runnable primaryAction,
            Runnable exitAction
    ) {
        super(victory ? "Victory" : "Defeat", skin);
        Table content = getContentTable();
        Label result = new Label(message == null ? "" : message, skin, "medium");
        result.setColor(Color.valueOf("4A3A1F"));
        result.setAlignment(Align.center);
        result.setWrap(true);
        content.add(result).width(520f).padBottom(16f).colspan(2).row();
        content.add(new MenuButton("Exit", skin, "brown", exitAction)).width(190f).height(48f).padRight(8f);
        content.add(new MenuButton(victory ? "Continue" : "Retry", skin, "green", primaryAction))
                .width(190f).height(48f);
    }
}
