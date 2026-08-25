package ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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
        grid.defaults().pad(8f);
        int column = 0;
        if (includeMarigold) {
            grid.add(createPlantChoice("Marigold", skin, animations, onSelected)).width(168f).height(132f);
            column++;
        }
        if (plants != null) {
            for (PlantData plant : plants) {
                if (plant == null || !plant.isUnlocked()) {
                    continue;
                }
                grid.add(createPlantChoice(plant.getName(), skin, animations, onSelected)).width(168f).height(132f);
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
        content.add(scrollPane).width(760f).height(360f).colspan(2).row();
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
        TextureRegion boost = animations == null ? null : animations.region("IMAGE_UI_PACKETS_BOOST");
        if (boost != null) {
            card.setBackground(new TextureRegionDrawable(boost));
        }
        card.pad(4f, 8f, 4f, 8f);
        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                select(plantName, onSelected);
            }
        });
        Actor packetActor = createPacketActor(plantName, skin, animations);
        Table packetHolder = new Table();
        packetHolder.add(packetActor).size(88f, 40f).left().bottom().padLeft(8f).padTop(22f).padBottom(2f);
        card.add(packetHolder).width(148f).height(60f).left().bottom().row();
        Label label = new Label(plantName, skin, "secondary");
        label.setAlignment(Align.center);
        label.setWrap(true);
        card.add(label).width(144f).height(40f).padTop(4f);
        return card;
    }

    private Actor createPacketActor(String plantName, Skin skin, PvzAnimationService animations) {
        TextureRegion packet = packetRegion(animations, plantName);
        if (packet != null) {
            Image image = new Image(packet);
            image.setScaling(Scaling.fit);
            Table holder = new Table();
            holder.add(image).size(84f, 36f).left().bottom().padLeft(2f).padBottom(2f);
            return holder;
        }
        if (animations != null) {
            Actor actor = animations.createPlantActor(plantName);
            if (actor != null) {
                actor.setSize(68f, 68f);
                return actor;
            }
        }
        Label fallback = new Label(plantName, skin, "medium_outline");
        fallback.setAlignment(Align.center);
        return fallback;
    }

    private TextureRegion packetRegion(PvzAnimationService animations, String plantName) {
        return SeedPacketCatalog.region(animations, plantName);
    }

    private void select(String plantName, Consumer<String> onSelected) {
        close();
        if (onSelected != null) {
            onSelected.accept(plantName);
        }
    }
}
