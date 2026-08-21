package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import pvz.skin.BorderedTable;

public class PlantCard extends BorderedTable {
    private static final Color PANEL_TEXT_COLOR = Color.valueOf("4A3A1F");
    private final Skin skin;
    private final Container<Actor> visualContainer;
    private final Image image;
    private final Label fallbackLabel;
    private final Label lockLabel;
    private final Label nameLabel;
    private final Label costLabel;
    private final Label levelLabel;
    private final Label seedLabel;
    private final Label familyLabel;
    private final Label tagsLabel;
    private final Label healthLabel;
    private final Label stateLabel;
    private final Label cooldownLabel;
    private final ProgressBarActor seedProgress;
    private final Table actionTable;
    private boolean locked;
    private boolean boosted;
    private boolean selected;

    public PlantCard(Skin skin) {
        this.skin = skin;
        visualContainer = new Container<>();
        visualContainer.fill();
        image = new Image();
        fallbackLabel = new Label("PLANT", skin, "medium_outline");
        lockLabel = new Label("LOCKED", skin, "medium_outline");
        nameLabel = new Label("Plant", skin, "medium_outline");
        costLabel = panelLabel("Cost: 0");
        levelLabel = panelLabel("Level: 1");
        seedLabel = panelLabel("Seeds: 0 / 1");
        familyLabel = panelLabel("Family: -");
        tagsLabel = panelLabel("Tags: -");
        healthLabel = panelLabel("Health: 0");
        stateLabel = panelLabel("");
        cooldownLabel = panelLabel("Ready");
        seedProgress = new ProgressBarActor(skin, 0f, 1f);
        actionTable = new Table();
        buildLayout();
    }

    public void setPlantImage(Drawable drawable) {
        image.setDrawable(drawable);
        visualContainer.setActor(image);
    }

    public void setPlantActor(Actor actor) {
        visualContainer.setActor(actor);
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
        int safeCurrent = Math.max(0, current);
        if (required == Integer.MAX_VALUE) {
            seedProgress.setRange(0f, 1f);
            seedProgress.setValue(1f);
            seedProgress.setValueText("MAX");
            seedLabel.setText("Seeds: " + safeCurrent + " / MAX");
            return;
        }
        int safeRequired = Math.max(1, required);
        seedProgress.setRange(0f, safeRequired);
        seedProgress.setValue(safeCurrent);
        seedLabel.setText("Seeds: " + safeCurrent + " / " + safeRequired);
    }

    public void setFamily(String family) {
        familyLabel.setText("Family: " + safeText(family));
    }

    public void setTags(String tags) {
        tagsLabel.setText("Tags: " + shorten(safeText(tags), 26));
    }

    public void setHealth(int health) {
        healthLabel.setText("Health: " + Math.max(0, health));
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        lockLabel.setVisible(locked);
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

    public void setPurchaseAction(int price, Runnable action) {
        actionTable.clearChildren();
        actionTable.add(new MenuButton("Buy " + Math.max(0, price), skin, "green_small", action))
                .width(150f).height(38f);
    }

    public void setUpgradeAction(Runnable action) {
        actionTable.clearChildren();
        actionTable.add(new MenuButton("Upgrade", skin, "purple", action)).width(150f).height(38f);
    }

    public void clearAction() {
        actionTable.clearChildren();
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

    private void buildLayout() {
        pad(12f);
        seedProgress.setTextColor(PANEL_TEXT_COLOR);
        fallbackLabel.setAlignment(Align.center);
        lockLabel.setAlignment(Align.center);
        Stack visualStack = new Stack();
        visualStack.add(fallbackLabel);
        visualStack.add(visualContainer);
        visualStack.add(lockLabel);
        add(visualStack).size(128f).colspan(2).row();
        add(nameLabel).colspan(2).padTop(6f).row();
        add(costLabel).left();
        add(levelLabel).right().row();
        add(seedLabel).colspan(2).padTop(4f).row();
        add(seedProgress).colspan(2).padTop(4f).row();
        add(familyLabel).colspan(2).left().padTop(4f).row();
        add(tagsLabel).colspan(2).left().padTop(2f).row();
        add(healthLabel).colspan(2).left().padTop(2f).row();
        add(stateLabel).colspan(2).padTop(4f).row();
        add(cooldownLabel).colspan(2).padTop(2f).row();
        add(actionTable).colspan(2).padTop(6f);
    }

    private Label panelLabel(String text) {
        Label label = new Label(text, skin, "secondary");
        label.setColor(PANEL_TEXT_COLOR);
        return label;
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
        stateLabel.setText(boosted ? "BOOSTED" : "UNLOCKED");
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String shorten(String value, int maximumLength) {
        if (value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, Math.max(1, maximumLength - 3)) + "...";
    }
}
