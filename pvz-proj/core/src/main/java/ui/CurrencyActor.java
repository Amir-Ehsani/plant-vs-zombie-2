package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

public class CurrencyActor extends Table {
    private static final Color RESOURCE_TEXT_COLOR = Color.WHITE;
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
        } else if (backgroundRegion == null && !this.name.isBlank()) {
            Label nameLabel = new Label(this.name + ":", skin, "secondary");
            add(nameLabel).padRight(6f);
        }
        if (backgroundRegion != null) {
            Label.LabelStyle style = new Label.LabelStyle(skin.getFont("FBUSV8C5EI_2_outline"), Color.WHITE);
            valueLabel = new Label("0", style);
            valueLabel.setColor(RESOURCE_TEXT_COLOR);
            valueLabel.setFontScale(0.72f);
        } else {
            valueLabel = new Label("0", skin, "medium_outline");
        }
        valueLabel.setAlignment(Align.center);
        add(valueLabel).expandX().center();
        setTouchable(Touchable.disabled);
    }

    public void setValue(int value) {
        valueLabel.setText(String.valueOf(Math.max(0, value)));
    }

    public void setAction(Runnable action) {
        clearListeners();
        if (action == null) {
            setTouchable(Touchable.disabled);
            return;
        }
        setTouchable(Touchable.enabled);
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
    }

    public String getCurrencyName() {
        return name;
    }
}
