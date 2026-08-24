package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

public class CurrencyActor extends Table {
    private static final Color RESOURCE_TEXT_COLOR = Color.valueOf("FFFBEA");
    private final String name;
    private final Label valueLabel;

    public CurrencyActor(Skin skin, String name) {
        this(skin, name, null, null);
    }

    public CurrencyActor(Skin skin, String name, TextureRegion backgroundRegion, TextureRegion iconRegion) {
        this.name = name == null ? "" : name;
        if (backgroundRegion != null) {
            setBackground(new TextureRegionDrawable(backgroundRegion));
            pad(9f, 12f, 9f, 12f);
        }
        if (iconRegion != null) {
            Image icon = new Image(iconRegion);
            icon.setScaling(Scaling.fit);
            add(icon).size(22f, 22f).padRight(6f);
        } else if (!this.name.isBlank()) {
            Label nameLabel = new Label(this.name + ":", skin, "secondary");
            add(nameLabel).padRight(6f);
        }
        valueLabel = new Label("0", skin, backgroundRegion == null ? "medium_outline" : "secondary");
        if (backgroundRegion != null) {
            valueLabel.setColor(RESOURCE_TEXT_COLOR);
        }
        valueLabel.setAlignment(Align.center);
        add(valueLabel);
    }

    public void setValue(int value) {
        valueLabel.setText(String.valueOf(Math.max(0, value)));
    }

    public String getCurrencyName() {
        return name;
    }
}
