package ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

public class ModalWindow extends Window {
    public ModalWindow(String title, Skin skin) {
        super(title == null ? "" : title, skin);
        setModal(true);
        setMovable(false);
        pad(20f);
    }

    public void show(Stage stage) {
        pack();
        float x = (stage.getViewport().getWorldWidth() - getWidth()) / 2f;
        float y = (stage.getViewport().getWorldHeight() - getHeight()) / 2f;
        setPosition(x, y);
        stage.addActor(this);
    }

    public void close() {
        remove();
    }
}
