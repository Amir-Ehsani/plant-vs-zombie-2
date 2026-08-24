package ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import models.account.PlantData;

import java.util.List;
import java.util.function.Consumer;

public class PlantSelectionDialog extends ModalWindow {
    private static final int COLUMN_COUNT = 4;

    public PlantSelectionDialog(
            String title,
            Skin skin,
            PvzAnimationService animations,
            List<PlantData> plants,
            boolean includeMarigold,
            Consumer<String> onSelected
    ) {
        super(title == null ? "Select Plant" : title, skin);
        buildContent(skin, animations, plants, includeMarigold, onSelected);
    }

    private void buildContent(
            Skin skin,
            PvzAnimationService animations,
            List<PlantData> plants,
            boolean includeMarigold,
            Consumer<String> onSelected
    ) {
        Table grid = new Table();
        grid.top();
        grid.defaults().pad(6f);
        int column = 0;
        if (includeMarigold) {
            grid.add(createPlantChoice("Marigold", skin, animations, onSelected)).width(180f).height(214f);
            column++;
        }
        if (plants != null) {
            for (PlantData plant : plants) {
                if (plant == null || !plant.isUnlocked()) {
                    continue;
                }
                grid.add(createPlantChoice(plant.getName(), skin, animations, onSelected))
                        .width(180f).height(214f);
                column++;
                if (column % COLUMN_COUNT == 0) {
                    grid.row();
                }
            }
        }
        if (column == 0) {
            grid.add(new Label("No plant is available.", skin, "medium")).pad(20f);
        }
        ScrollPane scrollPane = new ScrollPane(grid, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);
        Table content = getContentTable();
        content.add(scrollPane).width(770f).height(430f).colspan(2).row();
        content.add(new MenuButton("Close", skin, "brown", this::close))
                .width(180f).height(44f).colspan(2).padTop(12f);
    }

    private Table createPlantChoice(
            String plantName,
            Skin skin,
            PvzAnimationService animations,
            Consumer<String> onSelected
    ) {
        Table card = new Table();
        card.pad(8f);
        TextureRegion boost = animations == null ? null : animations.region("IMAGE_UI_PACKETS_BOOST");
        if (boost != null) {
            card.setBackground(new TextureRegionDrawable(boost));
        }
        Actor packetActor = createPacketActor(plantName, skin, animations);
        card.add(packetActor).size(122f, 110f).padTop(6f).padBottom(2f).row();
        Label label = new Label(plantName, skin, "medium_outline");
        label.setAlignment(Align.center);
        label.setWrap(true);
        card.add(label).width(150f).height(44f).row();
        card.add(new MenuButton("Select", skin, "green_small", () -> select(plantName, onSelected)))
                .width(126f).height(34f).padTop(6f).padBottom(4f);
        return card;
    }

    private Actor createPacketActor(String plantName, Skin skin, PvzAnimationService animations) {
        TextureRegion packet = packetRegion(animations, plantName);
        if (packet != null) {
            Image image = new Image(packet);
            image.setScaling(Scaling.fit);
            Stack stack = new Stack();
            Table center = new Table();
            center.add(image).size(80f, 96f).center();
            stack.add(center);
            return stack;
        }
        if (animations != null) {
            Actor actor = animations.createPlantActor(plantName);
            if (actor != null) {
                actor.setSize(96f, 96f);
                return actor;
            }
        }
        Label fallback = new Label(plantName, skin, "medium_outline");
        fallback.setAlignment(Align.center);
        return fallback;
    }

    private TextureRegion packetRegion(PvzAnimationService animations, String plantName) {
        if (animations == null || plantName == null || plantName.isBlank()) {
            return null;
        }
        String token = plantName.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        TextureRegion region = animations.region("IMAGE_UI_PACKETS_" + token);
        if (region != null) {
            return region;
        }
        return animations.region("IMAGE_UI_PACKETS_READY");
    }

    private void select(String plantName, Consumer<String> onSelected) {
        close();
        if (onSelected != null) {
            onSelected.accept(plantName);
        }
    }
}
