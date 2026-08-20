package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class BackButton extends MenuButton {
    public BackButton(Skin skin, Runnable action) {
        super("Back", skin, "brown", action);
    }
}
