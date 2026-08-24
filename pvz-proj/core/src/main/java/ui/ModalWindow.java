package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import pvz.skin.BorderedTable;

public class ModalWindow extends Table {
    private final BorderedTable panel;
    private final Table content;

    public ModalWindow(String title, Skin skin) {
        setFillParent(true);
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
            }
        });
        panel = new BorderedTable();
        panel.pad(28f);
        content = new Table();
        Label titleLabel = new Label(title == null ? "" : title, skin, "big_outline");
        titleLabel.setColor(Color.valueOf("FFFBEA"));
        panel.add(titleLabel).growX().padBottom(16f).row();
        panel.add(content).grow();
        add(panel);
    }

    public Table getContentTable() {
        return content;
    }

    public void show(Stage stage) {
        stage.addActor(this);
        toFront();
    }

    public void close() {
        remove();
    }
}
