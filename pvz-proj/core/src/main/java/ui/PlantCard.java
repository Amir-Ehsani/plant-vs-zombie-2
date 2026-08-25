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
    private final Table lockOverlay;
    private final Label nameLabel;
    private final Label costLabel;
    private final Label levelLabel;
    private final Label seedLabel;
    private final Label familyLabel;
    private final Label tagsLabel;
    private final Label healthLabel;
    private final Label cooldownLabel;
    private final Label stateLabel;
    private final ProgressBarActor seedProgress;
    private final Table actionTable;
    private boolean locked;
    private boolean boosted;
    private boolean selected;

    public PlantCard(Skin skin) {
        this.skin = skin;
        visualContainer = new Container<>();
        visualContainer.fill(false);
        visualContainer.center();
        image = new Image();
        fallbackLabel = new Label("", skin, "medium_outline");
        fallbackLabel.setVisible(false);
        lockLabel = new Label("LOCKED", skin, "medium_outline");
        lockOverlay = new Table();
        nameLabel = new Label("Plant", skin, "medium_outline");
        costLabel = panelLabel("Cost: 0");
        levelLabel = panelLabel("Level: 1");
        seedLabel = panelLabel("Seeds: 0 / 1");
        familyLabel = panelLabel("Family: -");
        tagsLabel = panelLabel("Tags: -");
        healthLabel = panelLabel("Health: 0");
        cooldownLabel = panelLabel("");
        cooldownLabel.setVisible(false);
        stateLabel = panelLabel("");
        seedProgress = new ProgressBarActor(skin, 0f, 1f);
        actionTable = new Table();
        buildLayout();
    }

    public void setPlantImage(Drawable drawable) {
        image.setDrawable(drawable);
        visualContainer.setActor(image);
    }

    public void setPlantActor(Actor actor) {
        if (actor != null) {
            actor.setSize(110f, 110f);
        }
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
        tagsLabel.setText("Tags: " + safeText(tags));
    }

    public void setHealth(int health) {
        healthLabel.setText("Health: " + Math.max(0, health));
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        lockOverlay.setVisible(locked);
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
        float remaining = Math.max(0f, cooldown);
        cooldownLabel.setVisible(true);
        if (remaining == 0f) {
            cooldownLabel.setText("Cooldown: Ready");
            return;
        }
        cooldownLabel.setText("Cooldown: " + formatCooldown(remaining));
    }

    public void clearCooldown() {
        cooldownLabel.setText("");
        cooldownLabel.setVisible(false);
    }

    public void setPurchaseAction(int price, Runnable action) {
        actionTable.clearChildren();
        actionTable.add(new MenuButton("Buy " + Math.max(0, price), skin, "green_small", action))
                .width(132f).height(32f);
    }

    public void setUpgradeAction(Runnable action) {
        setUpgradeAction(true, action);
    }

    public void setUpgradeAction(boolean available, Runnable action) {
        actionTable.clearChildren();
        String text = available ? "Upgrade" : "Need Resources";
        String style = available ? "purple" : "brown";
        actionTable.add(new MenuButton(text, skin, style, action)).width(144f).height(32f);
    }

    public void setSelectionMode(boolean enabled) {
        seedLabel.setVisible(!enabled);
        seedProgress.setVisible(!enabled);
        familyLabel.setVisible(!enabled);
        tagsLabel.setVisible(!enabled);
        healthLabel.setVisible(!enabled);
        cooldownLabel.setVisible(!enabled && cooldownLabel.isVisible());
    }

    public void setSelectionActions(
            boolean boostAvailable,
            Runnable boostAction,
            boolean upgradeAvailable,
            Runnable upgradeAction
    ) {
        actionTable.clearChildren();
        MenuButton boostButton = new MenuButton("Boost", skin, boostAvailable ? "purple" : "brown", boostAction);
        boostButton.setDisabled(!boostAvailable);
        MenuButton upgradeButton = new MenuButton(
                upgradeAvailable ? "Upgrade" : "Need Seeds",
                skin,
                upgradeAvailable ? "green_small" : "brown",
                upgradeAction
        );
        upgradeButton.setDisabled(!upgradeAvailable);
        actionTable.add(boostButton).width(104f).height(32f).padRight(4f);
        actionTable.add(upgradeButton).width(112f).height(32f);
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
                Actor target = event.getTarget();
                while (target != null && target != PlantCard.this) {
                    if (target == actionTable) {
                        return;
                    }
                    target = target.getParent();
                }
                action.run();
            }
        });
    }

    private void buildLayout() {
        pad(10f);
        defaults().center();
        seedProgress.setTextColor(PANEL_TEXT_COLOR);
        fallbackLabel.setAlignment(Align.center);
        fallbackLabel.setColor(PANEL_TEXT_COLOR);
        lockLabel.setAlignment(Align.center);
        lockLabel.setColor(PANEL_TEXT_COLOR);
        nameLabel.setAlignment(Align.center);
        nameLabel.setColor(PANEL_TEXT_COLOR);
        nameLabel.setWrap(true);
        costLabel.setAlignment(Align.center);
        levelLabel.setAlignment(Align.center);
        seedLabel.setAlignment(Align.center);
        familyLabel.setAlignment(Align.center);
        tagsLabel.setAlignment(Align.center);
        tagsLabel.setWrap(true);
        healthLabel.setAlignment(Align.center);
        cooldownLabel.setAlignment(Align.center);
        stateLabel.setAlignment(Align.center);
        lockOverlay.setTransform(true);
        lockOverlay.add(lockLabel).center();
        lockOverlay.setRotation(-18f);
        Stack visualStack = new Stack();
        visualStack.add(fallbackLabel);
        visualStack.add(visualContainer);
        visualStack.add(lockOverlay);
        add(visualStack).size(122f).padTop(2f).row();
        add(nameLabel).width(235f).padTop(3f).row();
        Table statRow = new Table();
        statRow.add(costLabel).width(102f).center();
        statRow.add(levelLabel).width(102f).center();
        add(statRow).row();
        add(seedLabel).width(235f).padTop(2f).row();
        add(seedProgress).width(190f).padTop(1f).center().row();
        add(familyLabel).width(235f).padTop(2f).row();
        add(tagsLabel).width(235f).padTop(1f).row();
        add(healthLabel).width(235f).padTop(1f).row();
        add(cooldownLabel).width(235f).padTop(1f).row();
        add(stateLabel).width(235f).padTop(2f).row();
        add(actionTable).padTop(10f).padBottom(8f).row();
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

    private String formatCooldown(float remaining) {
        if (remaining >= 10f) {
            return Math.round(remaining) + "s";
        }
        return String.format("%.1fs", remaining);
    }
}
