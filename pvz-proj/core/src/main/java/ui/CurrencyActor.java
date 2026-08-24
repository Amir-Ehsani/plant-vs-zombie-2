package ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

public class CurrencyActor extends Table {
    private final String name;
    private final Label valueLabel;

    public CurrencyActor(Skin skin, String name) {
        this(skin, name, null, null);
    }

    public CurrencyActor(Skin skin, String name, TextureRegion backgroundRegion, TextureRegion iconRegion) {
        this.name = name == null ? "" : name;
        if (backgroundRegion != null) {
            setBackground(new TextureRegionDrawable(backgroundRegion));
            pad(6f, 10f, 6f, 10f);
        }
        if (iconRegion != null) {
            Image icon = new Image(iconRegion);
            icon.setScaling(Scaling.fit);
            add(icon).size(18f, 18f).padRight(5f);
        } else if (!this.name.isBlank()) {
            Label nameLabel = new Label(this.name + ":", skin, "secondary");
            add(nameLabel).padRight(6f);
        }
        valueLabel = new Label("0", skin, backgroundRegion == null ? "medium_outline" : "secondary");
        valueLabel.setAlignment(Align.center);
        add(valueLabel);
        if (backgroundRegion != null && !this.name.isBlank()) {
            Label hiddenName = new Label(this.name, skin, "secondary");
            hiddenName.setVisible(false);
            add(hiddenName).width(0f).height(0f);
        }
    }

    public void setValue(int value) {
        valueLabel.setText(String.valueOf(Math.max(0, value)));
    }

    public String getCurrencyName() {
        return name;
    }
}
