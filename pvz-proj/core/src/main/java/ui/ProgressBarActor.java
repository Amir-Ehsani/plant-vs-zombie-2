package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class ProgressBarActor extends Table {
    private final ProgressBar progressBar;
    private final Label valueLabel;
    private float minimum;
    private float maximum;

    public ProgressBarActor(Skin skin, float minimum, float maximum) {
        this.minimum = minimum;
        this.maximum = Math.max(minimum + 1f, maximum);
        progressBar = new ProgressBar(this.minimum, this.maximum, 1f, false, skin, "xp_green");
        valueLabel = new Label("", skin, "secondary");
        add(progressBar).width(170f).padRight(8f);
        add(valueLabel).width(90f);
        setValue(this.minimum);
    }

    public void setRange(float minimum, float maximum) {
        this.minimum = minimum;
        this.maximum = Math.max(minimum + 1f, maximum);
        progressBar.setRange(this.minimum, this.maximum);
        setValue(progressBar.getValue());
    }

    public void setTextColor(Color color) {
        if (color != null) {
            valueLabel.setColor(color);
        }
    }

    public void setValueText(String text) {
        valueLabel.setText(text == null ? "" : text);
    }

    public void setValue(float value) {
        float bounded = Math.max(minimum, Math.min(maximum, value));
        progressBar.setValue(bounded);
        valueLabel.setText(formatValue(bounded) + " / " + formatValue(maximum));
    }

    private String formatValue(float value) {
        if (value == Math.round(value)) {
            return String.valueOf(Math.round(value));
        }
        return String.format("%.1f", value);
    }
}
