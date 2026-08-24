package ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import models.account.PlantData;
import pvz.skin.BorderedTable;

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
            grid.add(createPlantChoice("Marigold", skin, animations, onSelected)).width(180f).height(170f);
            column++;
        }
        if (plants != null) {
            for (PlantData plant : plants) {
                if (plant == null || !plant.isUnlocked()) {
                    continue;
                }
                grid.add(createPlantChoice(plant.getName(), skin, animations, onSelected))
                        .width(180f).height(170f);
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
        content.add(scrollPane).width(770f).height(390f).colspan(2).row();
        content.add(new MenuButton("Close", skin, "brown", this::close))
                .width(180f).height(44f).colspan(2).padTop(12f);
    }

    private Table createPlantChoice(
            String plantName,
            Skin skin,
            PvzAnimationService animations,
            Consumer<String> onSelected
    ) {
        BorderedTable card = new BorderedTable();
        card.pad(8f);
        card.setClip(true);
        Actor actor = animations == null ? null : animations.createPlantActor(plantName);
        if (actor != null) {
            actor.setSize(100f, 100f);
            card.add(actor).size(104f).padBottom(4f).row();
        }
        Label label = new Label(plantName, skin, "medium_outline");
        label.setAlignment(Align.center);
        label.setWrap(true);
        card.add(label).width(150f).height(44f).row();
        card.add(new MenuButton("Select", skin, "green_small", () -> select(plantName, onSelected)))
                .width(126f).height(34f).padTop(4f);
        return card;
    }

    private void select(String plantName, Consumer<String> onSelected) {
        close();
        if (onSelected != null) {
            onSelected.accept(plantName);
        }
    }
}
