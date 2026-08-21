package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import pvz.skin.BorderedTable;

public class ZombieCard extends BorderedTable {
    private static final Color PANEL_TEXT_COLOR = Color.valueOf("4A3A1F");
    private final Container<Actor> visualContainer;
    private final Image image;
    private final Label fallbackLabel;
    private final Label unknownLabel;
    private final Label nameLabel;
    private final Label stateLabel;
    private boolean discovered;
    private boolean selected;

    public ZombieCard(Skin skin) {
        visualContainer = new Container<>();
        visualContainer.fill(false);
        visualContainer.center();
        image = new Image();
        fallbackLabel = new Label("ZOMBIE", skin, "medium_outline");
        unknownLabel = new Label("?", skin, "big_outline");
        nameLabel = new Label("Unknown Zombie", skin, "medium_outline");
        stateLabel = new Label("?", skin, "secondary");
        stateLabel.setColor(PANEL_TEXT_COLOR);
        discovered = false;
        selected = false;
        buildLayout();
    }

    public void setZombieImage(Drawable drawable) {
        image.setDrawable(drawable);
        visualContainer.setActor(image);
    }

    public void setZombieActor(Actor actor) {
        if (actor != null) {
            actor.setSize(102f, 102f);
        }
        visualContainer.setActor(actor);
    }

    public void setName(String name) {
        nameLabel.setText(discovered && name != null && !name.isBlank() ? name : "Unknown Zombie");
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
        unknownLabel.setVisible(!discovered);
        refreshState();
        if (!discovered) {
            nameLabel.setText("Unknown Zombie");
            visualContainer.setActor(null);
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        refreshState();
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
        pad(14f);
        fallbackLabel.setAlignment(Align.center);
        fallbackLabel.setWrap(true);
        unknownLabel.setAlignment(Align.center);
        nameLabel.setAlignment(Align.center);
        nameLabel.setWrap(true);
        stateLabel.setAlignment(Align.center);
        Stack visualStack = new Stack();
        visualStack.add(fallbackLabel);
        visualStack.add(visualContainer);
        visualStack.add(unknownLabel);
        add(visualStack).size(110f).row();
        add(nameLabel).width(180f).padTop(8f).row();
        add(stateLabel).width(180f).padTop(6f).row();
    }

    private void refreshState() {
        if (!discovered) {
            stateLabel.setText("UNKNOWN");
            return;
        }
        stateLabel.setText(selected ? "DISCOVERED | SELECTED" : "DISCOVERED");
    }
}
