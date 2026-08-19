package ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import pvz.skin.BorderedTable;

public class ZombieCard extends BorderedTable {
    private final Image image;
    private final Label nameLabel;
    private final Label stateLabel;
    private boolean discovered;

    public ZombieCard(Skin skin) {
        image = new Image();
        nameLabel = new Label("Unknown Zombie", skin, "medium_outline");
        stateLabel = new Label("?", skin, "secondary");
        discovered = false;
        pad(12f);
        add(image).size(88f).row();
        add(nameLabel).padTop(6f).row();
        add(stateLabel).padTop(4f);
    }

    public void setZombieImage(Drawable drawable) {
        image.setDrawable(drawable);
    }

    public void setName(String name) {
        nameLabel.setText(discovered && name != null && !name.isBlank() ? name : "Unknown Zombie");
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
        stateLabel.setText(discovered ? "DISCOVERED" : "?");
        if (!discovered) {
            nameLabel.setText("Unknown Zombie");
            image.setDrawable(null);
        }
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
}
