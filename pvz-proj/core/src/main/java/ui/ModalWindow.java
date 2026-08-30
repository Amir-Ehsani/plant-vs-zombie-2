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
    private static final float CLOSE_SECONDS = 0.12f;

    private final BorderedTable panel;
    private final Table content;
    private boolean closing;

    public ModalWindow(String title, Skin skin) {
        setFillParent(true);
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
            }
        });
        panel = new BorderedTable();
        panel.setTransform(true);
        panel.setOrigin(Align.center);
        panel.pad(28f);
        content = new Table();
        Label titleLabel = new Label(title == null ? "" : title, skin, "big_outline");
        panel.add(titleLabel).growX().padBottom(16f).row();
        panel.add(content).grow();
        add(panel);
        closing = false;
    }

    public Table getContentTable() {
        return content;
    }

    public void show(Stage stage) {
        if (stage == null) {
            return;
        }
        getColor().a = 0f;
        panel.pack();
        panel.setOrigin(Align.center);
        panel.setScale(0.88f);
        stage.addActor(this);
        toFront();
        clearActions();
        panel.clearActions();
        addAction(Actions.fadeIn(OPEN_SECONDS, Interpolation.fade));
        panel.addAction(Actions.scaleTo(1f, 1f, OPEN_SECONDS, Interpolation.swingOut));
    }

    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        clearActions();
        panel.clearActions();
        addAction(Actions.sequence(
                Actions.fadeOut(CLOSE_SECONDS, Interpolation.fade),
                Actions.removeActor()
        ));
        panel.addAction(Actions.scaleTo(0.92f, 0.92f, CLOSE_SECONDS, Interpolation.sineIn));
    }
}
