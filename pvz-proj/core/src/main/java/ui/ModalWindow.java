package ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import pvz.skin.BorderedTable;

public class ModalWindow extends Table {
    private static final float OPEN_SECONDS = 0.16f;
    private final BorderedTable panel;
    private final Table content;

    public ModalWindow(String title, Skin skin) {
        setFillParent(true);
        setTransform(true);
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
            }
        });
        panel = new BorderedTable();
        panel.setTransform(true);
        panel.pad(28f);
        content = new Table();
        Label titleLabel = new Label(title == null ? "" : title, skin, "big_outline");
        panel.add(titleLabel).growX().padBottom(16f).row();
        panel.add(content).grow();
        add(panel);
    }

    public Table getContentTable() {
        return content;
    }

    public void show(Stage stage) {
        getColor().a = 0f;
        panel.setOrigin(Align.center);
        panel.setScale(0.90f);
        stage.addActor(this);
        toFront();
        clearActions();
        panel.clearActions();
        addAction(Actions.fadeIn(OPEN_SECONDS, Interpolation.fade));
        panel.addAction(Actions.scaleTo(1f, 1f, OPEN_SECONDS, Interpolation.swingOut));
    }

    public void close() {
        // Keep close immediate: some confirm callbacks switch screens, so no delayed action may
        // retain a reference to a disposed Stage.
        remove();
    }
}
