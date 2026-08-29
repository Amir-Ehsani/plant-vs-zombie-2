package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.pvz.Main;
import controllers.features.TravelLogController;
import models.account.PlantData;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantType;
import ui.BackButton;
import ui.MenuButton;
import ui.SeedPacketCatalog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ZombotanyPlantSelectionScreen extends BaseMenuScreen {
    private static final int MAX_SELECTED_PLANTS = 8;
    private static final float PACKET_WIDTH = 132f;
    private static final float PACKET_HEIGHT = 88f;
    private static final Color TEXT_COLOR = Color.valueOf("4A3A1F");
    private static final Color TITLE_COLOR = Color.WHITE;

    private final int miniGameStage;
    private final TravelLogController controller;
    private final Table selectedSlots;
    private final Table detailPanel;
    private final Table plantGrid;
    private final Label selectionCount;
    private final Set<String> selectedPlants;
    private String focusedPlantName;

    public ZombotanyPlantSelectionScreen(Main game, int miniGameStage) {
        super(game);
        this.miniGameStage = miniGameStage;
        controller = game.getTravelLogController();
        selectedSlots = new Table();
        detailPanel = new Table();
        plantGrid = new Table();
        selectionCount = new Label("", skin, "secondary");
        selectionCount.setColor(TEXT_COLOR);
        selectedPlants = new LinkedHashSet<>();
        focusedPlantName = "";
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshAll();
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        root.top();
        addResourceBar(root);

        Table panel = createPanel();
        panel.pad(24f, 24f, 22f, 24f);

        Label title = createTitle("Choose Your Plants");
        title.setColor(TITLE_COLOR);
        panel.add(title).padBottom(2f).row();

        Label mission = panelLabel(
                "Zombotany - defend the lawn against zombies with plant abilities."
        );
        mission.setWrap(true);
        mission.setAlignment(Align.center);
        panel.add(mission).width(1010f).padBottom(4f).row();
        panel.add(selectionCount).padBottom(4f).row();

        Table body = new Table();
        body.top().center();

        Table browser = new Table();
        browser.top();
        browser.add(detailPanel).width(820f).height(120f).padBottom(4f).row();

        plantGrid.top().left();
        plantGrid.defaults().pad(3f);
        ScrollPane scrollPane = new ScrollPane(plantGrid, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setScrollbarsOnTop(false);
        browser.add(scrollPane).width(832f).height(280f).padRight(8f);
        body.add(browser).width(850f).height(410f).top().center();

        panel.add(body).width(1015f).height(410f).center().row();

        Table actions = new Table();
        actions.add(new BackButton(skin, game.getScreenManager()::showMiniGames))
                .width(180f).height(46f).padRight(14f);
        actions.add(new MenuButton("LET'S ROCK", skin, "purple", this::startGame))
                .width(220f).height(52f);
        panel.add(actions).padTop(4f).padBottom(4f);

        root.add(panel).width(1100f).height(600f);
    }

    private void refreshAll() {
        refreshResourceBar();
        ensureFocusedPlant();
        rebuildSelectedSlots();
        rebuildDetailPanel();
        rebuildPlantGrid();
    }

    private void ensureFocusedPlant() {
        if (!focusedPlantName.isBlank() && findPlantData(focusedPlantName) != null) {
            return;
        }
        List<PlantData> plants = availablePlants();
        focusedPlantName = plants.isEmpty() ? "" : plants.get(0).getName();
    }

    private void rebuildSelectedSlots() {
        selectedSlots.clearChildren();
        selectedSlots.top();
        selectedSlots.defaults().padBottom(6f);
        List<String> selected = new ArrayList<>(selectedPlants);
        selectionCount.setText("Selected Plants: " + selected.size() + " / " + MAX_SELECTED_PLANTS);
        for (int index = 0; index < MAX_SELECTED_PLANTS; index++) {
            String plantName = index < selected.size() ? selected.get(index) : null;
            selectedSlots.add(createSelectedSlot(index + 1, plantName)).width(140f).height(56f).row();
        }
    }

    private Table createSelectedSlot(int slotNumber, String plantName) {
        Table slot = new Table();
        Stack stack = createPacketStack(plantName, 134f, 54f, 84f, 42f);
        slot.add(stack).width(134f).height(54f);
        if (plantName == null) {
            Label empty = new Label(String.valueOf(slotNumber), skin, "secondary");
            empty.setColor(TEXT_COLOR);
            empty.setAlignment(Align.center);
            stack.add(empty);
        } else {
            slot.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    focusedPlantName = plantName;
                    selectedPlants.remove(selectedName(plantName));
                    refreshAll();
                }
            });
        }
        return slot;
    }

    private void rebuildDetailPanel() {
        detailPanel.clearChildren();
        detailPanel.pad(8f);
        PlantData data = findPlantData(focusedPlantName);
        if (data == null) {
            Label empty = panelLabel("Select a seed packet to view plant details.");
            empty.setAlignment(Align.center);
            detailPanel.add(empty).expand().center();
            return;
        }

        PlantType type = DefaultPlantRegistry.getInstance().getByName(data.getName());
        Stack preview = createPacketStack(data.getName(), 170f, 120f, 118f, 72f);
        detailPanel.add(preview).width(170f).height(120f).padRight(12f);

        Table info = new Table();
        info.top().left();
        Label name = new Label(data.getName(), skin, "medium_outline");
        name.setColor(TITLE_COLOR);
        name.setAlignment(Align.left);
        info.add(name).left().padBottom(6f).row();
        info.add(panelLabel("Sun Cost: " + (type == null ? 0 : type.getSunCost()))).left().row();
        info.add(panelLabel("Level: " + data.getLevel())).left().row();
        info.add(panelLabel("Family: " + (type == null ? "-" : type.getCategory()))).left().row();
        detailPanel.add(info).width(330f).top().left().padRight(12f);

        Table actions = new Table();
        actions.top();
        boolean selected = isSelected(data.getName());
        MenuButton select = new MenuButton(
                selected ? "Remove" : "Select",
                skin,
                selected ? "brown" : "green_small",
                () -> togglePlant(data.getName())
        );
        actions.add(select).width(150f).height(38f).padBottom(5f).row();
        MenuButton upgrade = new MenuButton(
                "Upgrade", skin, "green_small", () -> upgradePlant(data.getName())
        );
        upgrade.setDisabled(!data.canUpgrade());
        actions.add(upgrade).width(150f).height(38f);
        detailPanel.add(actions).width(170f).top();
    }

    private void rebuildPlantGrid() {
        plantGrid.clearChildren();
        List<PlantData> plants = availablePlants();
        int column = 0;
        for (PlantData plant : plants) {
            plantGrid.add(createSeedPacketChoice(plant)).width(PACKET_WIDTH).height(PACKET_HEIGHT);
            column++;
            if (column % 6 == 0) {
                plantGrid.row();
            }
        }
        if (plants.isEmpty()) {
            plantGrid.add(panelLabel("No unlocked plants are available.")).pad(20f);
        }
    }

    private Table createSeedPacketChoice(PlantData data) {
        Table card = new Table();
        Stack stack = createPacketStack(data.getName(), PACKET_WIDTH, PACKET_HEIGHT, 92f, 58f);

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.top().right();
        PlantType type = DefaultPlantRegistry.getInstance().getByName(data.getName());
        Label cost = createSunCostLabel(type == null ? 0 : type.getSunCost());
        overlay.add(cost).padTop(6f).padRight(8f);
        stack.add(overlay);

        if (isSelected(data.getName())) {
            Table selectedOverlay = new Table();
            selectedOverlay.setFillParent(true);
            selectedOverlay.bottom().right();
            Label selected = new Label("SELECTED", skin, "secondary");
            selected.setColor(TITLE_COLOR);
            selectedOverlay.add(selected).padRight(6f).padBottom(4f);
            stack.add(selectedOverlay);
        }

        card.add(stack).width(PACKET_WIDTH).height(PACKET_HEIGHT);
        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                focusedPlantName = data.getName();
                togglePlant(data.getName());
            }
        });
        return card;
    }

    private Stack createPacketStack(
            String plantName, float width, float height, float packetWidth, float packetHeight
    ) {
        Stack stack = new Stack();
        TextureRegion backgroundRegion = game.getAnimationService().region(packetBackgroundId(plantName));
        if (backgroundRegion != null) {
            Image background = new Image(backgroundRegion);
            background.setScaling(Scaling.fill);
            stack.add(background);
        }
        if (plantName == null || plantName.isBlank()) {
            return stack;
        }

        TextureRegion packetRegion = SeedPacketCatalog.region(game.getAnimationService(), plantName);
        if (packetRegion != null) {
            Table packetHolder = new Table();
            packetHolder.setFillParent(true);
            packetHolder.bottom().left();
            Image packet = new Image(new TextureRegionDrawable(packetRegion));
            packet.setScaling(Scaling.fit);
            packetHolder.add(packet).width(packetWidth).height(packetHeight).left().bottom();
            stack.add(packetHolder);
        } else {
            Table fallback = new Table();
            fallback.setFillParent(true);
            fallback.bottom().left();
            Label label = new Label(plantName, skin, "secondary");
            label.setColor(TEXT_COLOR);
            label.setWrap(true);
            fallback.add(label).width(width - 12f).left().bottom().pad(6f);
            stack.add(fallback);
        }
        return stack;
    }

    private String packetBackgroundId(String plantName) {
        return plantName != null && !plantName.isBlank() && isSelected(plantName)
                ? "IMAGE_UI_PACKETS_READY_PREMIUM"
                : "IMAGE_UI_PACKETS_HOMELESS";
    }

    private Label createSunCostLabel(int sunCost) {
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("FBUSV8C5EI_1_outline"), Color.WHITE);
        Label label = new Label(String.valueOf(sunCost), style);
        label.setColor(Color.YELLOW);
        label.setFontScale(0.55f);
        return label;
    }

    private void togglePlant(String plantName) {
        String existing = selectedName(plantName);
        if (existing != null) {
            selectedPlants.remove(existing);
            refreshAll();
            return;
        }
        if (selectedPlants.size() >= MAX_SELECTED_PLANTS) {
            return;
        }
        selectedPlants.add(plantName);
        refreshAll();
    }

    private void upgradePlant(String plantName) {
        game.getCollectionController().upgradePlant(plantName);
        refreshAll();
    }

    private void startGame() {
        if (selectedPlants.isEmpty()) {
            return;
        }
        controller.enterZombotanyMiniGame(miniGameStage, new ArrayList<>(selectedPlants));
        if (controller.wasSuccessful()) {
            game.getScreenManager().showActiveMiniGame();
        }
    }

    private List<PlantData> availablePlants() {
        User user = game.getAuthController().getLoggedInUser();
        List<PlantData> result = new ArrayList<>();
        if (user == null || user.getCollection() == null) {
            return result;
        }
        for (PlantData data : user.getCollection().getOwnedPlants()) {
            if (data != null && data.isUnlocked()) {
                result.add(data);
            }
        }
        result.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return result;
    }

    private PlantData findPlantData(String plantName) {
        User user = game.getAuthController().getLoggedInUser();
        if (user == null || user.getCollection() == null || plantName == null) {
            return null;
        }
        return user.getCollection().findPlant(plantName);
    }

    private boolean isSelected(String plantName) {
        return selectedName(plantName) != null;
    }

    private String selectedName(String plantName) {
        String normalized = normalize(plantName);
        for (String selected : selectedPlants) {
            if (normalize(selected).equals(normalized)) {
                return selected;
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }

    private Label panelLabel(String text) {
        Label label = new Label(text == null ? "" : text, skin, "secondary");
        label.setColor(TEXT_COLOR);
        return label;
    }
}
