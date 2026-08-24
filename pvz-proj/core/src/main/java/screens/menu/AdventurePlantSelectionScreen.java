package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.pvz.Main;
import controllers.core.GameController;
import models.account.PlantData;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.engine.session.GameSession;
import models.level.core.Level;
import pvz.skin.BorderedTable;
import ui.AdventureMissionCatalog;
import ui.BackButton;
import ui.MenuButton;
import ui.PlantCard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AdventurePlantSelectionScreen extends BaseMenuScreen {
    private static final int MAX_SELECTED_PLANTS = 8;
    private static final Color TEXT_COLOR = Color.valueOf("4A3A1F");
    private final String chapterName;
    private final int levelNumber;
    private final GameController controller;
    private final PlantRegistry plantRegistry;
    private final Table plantGrid;
    private final Table selectedSlots;
    private final Label selectionCount;
    private final Set<String> paidBoostNames;

    public AdventurePlantSelectionScreen(Main game, String chapterName, int levelNumber) {
        super(game);
        this.chapterName = chapterName;
        this.levelNumber = levelNumber;
        controller = game.getGameController();
        plantRegistry = DefaultPlantRegistry.getInstance();
        plantGrid = new Table();
        selectedSlots = new Table();
        selectionCount = new Label("", skin, "secondary");
        selectionCount.setColor(TEXT_COLOR);
        paidBoostNames = new HashSet<>();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        if (!ensurePreparedLevel()) {
            return;
        }
        refreshAll();
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        addResourceBar(root);
        Table panel = createPanel();
        Label screenTitle = createTitle("Choose Your Plants");
        screenTitle.setColor(TEXT_COLOR);
        panel.add(screenTitle).padBottom(4f).row();
        Label mission = panelLabel(AdventureMissionCatalog.mission(chapterName, levelNumber));
        mission.setWrap(true);
        mission.setAlignment(Align.center);
        panel.add(mission).width(920f).padBottom(8f).row();
        panel.add(selectionCount).padBottom(6f).row();
        panel.add(selectedSlots).width(1000f).height(76f).padBottom(10f).row();
        plantGrid.top().left();
        plantGrid.defaults().pad(6f);
        ScrollPane scrollPane = new ScrollPane(plantGrid, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(false, false);
        panel.add(scrollPane).width(1060f).height(390f).row();
        Table actions = new Table();
        actions.add(new BackButton(skin, () -> game.getScreenManager().showAdventureMission(chapterName, levelNumber)))
                .width(180f).height(46f).padRight(10f);
        actions.add(new MenuButton("LET'S ROCK", skin, "green", this::startLevel))
                .width(220f).height(52f);
        panel.add(actions).padTop(10f);
        root.add(panel).width(1160f).height(680f);
    }

    private boolean ensurePreparedLevel() {
        GameSession session = controller.getGameSession();
        if (session != null && session.getCurrentLevel() != null && !session.isRunning()) {
            return true;
        }
        controller.prepareChapterLevel(chapterName, levelNumber);
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            game.getScreenManager().showAdventure();
            return false;
        }
        return true;
    }

    private void refreshAll() {
        refreshResourceBar();
        rebuildSelectedSlots();
        rebuildPlantGrid();
    }

    private void rebuildSelectedSlots() {
        selectedSlots.clearChildren();
        selectedSlots.defaults().padRight(5f);
        List<String> selected = selectedPlantNames();
        selectionCount.setText("Selected Plants: " + selected.size() + " / " + MAX_SELECTED_PLANTS);
        for (int index = 0; index < MAX_SELECTED_PLANTS; index++) {
            String plantName = index < selected.size() ? selected.get(index) : null;
            selectedSlots.add(createSelectedSlot(index + 1, plantName)).width(118f).height(68f);
        }
    }

    private Table createSelectedSlot(int slotNumber, String plantName) {
        BorderedTable slot = new BorderedTable();
        slot.pad(6f);
        Label slotLabel = panelLabel("Slot " + slotNumber);
        slotLabel.setAlignment(Align.center);
        slot.add(slotLabel).row();
        Label plantLabel = panelLabel(plantName == null ? "Empty" : plantName);
        plantLabel.setAlignment(Align.center);
        plantLabel.setWrap(true);
        slot.add(plantLabel).width(102f).height(30f).padTop(2f);
        if (plantName != null) {
            slot.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    removePlant(plantName);
                }
            });
        }
        return slot;
    }

    private void rebuildPlantGrid() {
        plantGrid.clearChildren();
        List<PlantData> plants = availablePlants();
        int column = 0;
        for (PlantData plant : plants) {
            plantGrid.add(createPlantCard(plant)).width(250f).height(330f);
            column++;
            if (column % 4 == 0) {
                plantGrid.row();
            }
        }
        if (plants.isEmpty()) {
            plantGrid.add(panelLabel("No unlocked plants are available for this level.")).pad(20f);
        }
    }

    private PlantCard createPlantCard(PlantData data) {
        PlantType type = plantRegistry.getByName(data.getName());
        PlantCard card = new PlantCard(skin);
        card.setSelectionMode(true);
        card.setName(data.getName());
        card.setLevel(data.getLevel());
        card.setCost(type == null ? 0 : type.getSunCost());
        card.setSeedPacketProgress(data.getSeedPackets(), data.getRequiredSeedPacketsForNextLevel());
        card.setFamily(type == null ? "-" : type.getCategory());
        card.setTags(type == null ? "-" : type.getTags());
        card.setHealth(type == null ? 0 : type.getBaseHp());
        card.setLocked(false);
        boolean selected = isSelected(data.getName());
        boolean boosted = data.getBoostCount() > 0 || paidBoostNames.contains(normalize(data.getName()));
        card.setSelected(selected);
        card.setBoosted(boosted);
        Actor actor = game.getAnimationService().createPlantActor(data.getName());
        card.setPlantActor(actor);
        card.setSelectionActions(
                selected && !paidBoostNames.contains(normalize(data.getName())),
                () -> boostPlant(data.getName()),
                data.canUpgrade(),
                () -> upgradePlant(data.getName())
        );
        card.setOnClick(() -> togglePlant(data.getName()));
        return card;
    }

    private void togglePlant(String plantName) {
        if (isSelected(plantName)) {
            removePlant(plantName);
            return;
        }
        if (selectedPlantNames().size() >= MAX_SELECTED_PLANTS) {
            showControllerMessage("ERROR: You can select at most 8 plants.");
            return;
        }
        controller.addPlantToSelection(plantName);
        showControllerMessage(controller.getLastMessage());
        refreshAll();
    }

    private void removePlant(String plantName) {
        controller.removePlantFromSelection(plantName);
        showControllerMessage(controller.getLastMessage());
        paidBoostNames.remove(normalize(plantName));
        refreshAll();
    }

    private void boostPlant(String plantName) {
        controller.boostPlant(plantName);
        showControllerMessage(controller.getLastMessage());
        if (controller.wasSuccessful()) {
            paidBoostNames.add(normalize(plantName));
        }
        refreshAll();
    }

    private void upgradePlant(String plantName) {
        game.getCollectionController().upgradePlant(plantName);
        showControllerMessage(game.getCollectionController().getLastMessage());
        refreshAll();
    }

    private void startLevel() {
        if (selectedPlantNames().isEmpty()) {
            showControllerMessage("ERROR: Select at least one plant before starting the level.");
            return;
        }
        controller.startGame();
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            return;
        }
        game.getScreenManager().showPreparedGame();
    }

    private List<PlantData> availablePlants() {
        User user = game.getAuthController().getLoggedInUser();
        GameSession session = controller.getGameSession();
        Level level = session == null ? null : session.getCurrentLevel();
        List<PlantData> result = new ArrayList<>();
        if (user == null || level == null) {
            return result;
        }
        for (PlantData data : user.getCollection().getOwnedPlants()) {
            if (data != null && data.isUnlocked() && level.isPlantAllowed(data.getName())) {
                result.add(data);
            }
        }
        result.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return result;
    }

    private List<String> selectedPlantNames() {
        GameSession session = controller.getGameSession();
        return session == null ? new ArrayList<>() : new ArrayList<>(session.getSelectedPlantNames());
    }

    private boolean isSelected(String plantName) {
        String key = normalize(plantName);
        for (String selected : selectedPlantNames()) {
            if (normalize(selected).equals(key)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ');
    }

    private Label panelLabel(String text) {
        Label label = new Label(text == null ? "" : text, skin, "secondary");
        label.setColor(TEXT_COLOR);
        return label;
    }
}
