package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class PauseDialog extends ModalWindow {
    public PauseDialog(Skin skin, Runnable resumeAction, Runnable restartAction, Runnable saveExitAction) {
        super("Paused", skin);
        Table content = getContentTable();
        content.defaults().width(220f).height(48f).padTop(8f);
        content.add(new MenuButton("Resume", skin, "green", resumeAction)).row();
        content.add(new MenuButton("Restart", skin, "brown", restartAction)).row();
        content.add(new MenuButton("Save & Exit", skin, "brown", saveExitAction));
    }
}
