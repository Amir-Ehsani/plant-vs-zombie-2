package ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import pvz.skin.BorderedTable;

public class PlantCard extends BorderedTable {
    private final Image image;
    private final Label nameLabel;
    private final Label costLabel;
    private final Label levelLabel;
    private final Label stateLabel;
    private final Label cooldownLabel;
    private final ProgressBarActor seedProgress;
    private boolean locked;
    private boolean boosted;
    private boolean selected;

    public PlantCard(Skin skin) {
        image = new Image();
        nameLabel = new Label("Plant", skin, "medium_outline");
        costLabel = new Label("Cost: 0", skin, "secondary");
        levelLabel = new Label("Level: 1", skin, "secondary");
        stateLabel = new Label("", skin, "secondary");
        cooldownLabel = new Label("Ready", skin, "secondary");
        seedProgress = new ProgressBarActor(skin, 0f, 1f);
        pad(12f);
        add(image).size(88f).colspan(2).row();
        add(nameLabel).colspan(2).padTop(6f).row();
        add(costLabel).left();
        add(levelLabel).right().row();
        add(seedProgress).colspan(2).padTop(6f).row();
        add(stateLabel).colspan(2).padTop(4f).row();
        add(cooldownLabel).colspan(2).padTop(4f);
    }

    public void setPlantImage(Drawable drawable) {
        image.setDrawable(drawable);
    }

    public void setName(String name) {
        nameLabel.setText(name == null || name.isBlank() ? "Plant" : name);
    }

    public void setCost(int cost) {
        costLabel.setText("Cost: " + Math.max(0, cost));
    }

    public void setLevel(int level) {
        levelLabel.setText("Level: " + Math.max(1, level));
    }

    public void setSeedPacketProgress(int current, int required) {
        int safeRequired = Math.max(1, required);
        seedProgress.setRange(0f, safeRequired);
        seedProgress.setValue(Math.max(0, current));
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        refreshState();
    }

    public void setBoosted(boolean boosted) {
        this.boosted = boosted;
        refreshState();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        refreshState();
    }

    public void setCooldown(float cooldown) {
        float bounded = Math.max(0f, Math.min(1f, cooldown));
        int percent = Math.round(bounded * 100f);
        cooldownLabel.setText(percent >= 100 ? "Ready" : "Cooldown: " + percent + "%");
    }

    public void setOnClick(Runnable action) {
        clearListeners();
        if (action == null) {
            return;
        }
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
    }

    private void refreshState() {
        if (locked) {
            stateLabel.setText("LOCKED");
            return;
        }
        if (selected && boosted) {
            stateLabel.setText("SELECTED | BOOSTED");
            return;
        }
        if (selected) {
            stateLabel.setText("SELECTED");
            return;
        }
        stateLabel.setText(boosted ? "BOOSTED" : "");
    }
}
