package ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class MenuButton extends TextButton {
    public MenuButton(String text, Skin skin, Runnable action) {
        this(text, skin, "green", action);
    }

    public MenuButton(String text, Skin skin, String styleName, Runnable action) {
        super(text == null ? "" : text, skin, styleName == null ? "green" : styleName);
        if (action != null) {
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!isDisabled()) {
                        action.run();
                    }
                }
            });
        }
    }
}
