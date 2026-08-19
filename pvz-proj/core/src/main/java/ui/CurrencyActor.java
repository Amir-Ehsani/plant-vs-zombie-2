package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class CurrencyActor extends Table {
    private final String name;
    private final Label valueLabel;

    public CurrencyActor(Skin skin, String name) {
        this.name = name == null ? "" : name;
        Label nameLabel = new Label(this.name + ":", skin);
        valueLabel = new Label("0", skin);
        add(nameLabel).padRight(6f);
        add(valueLabel);
    }

    public void setValue(int value) {
        valueLabel.setText(String.valueOf(Math.max(0, value)));
    }

    public String getCurrencyName() {
        return name;
    }
}
