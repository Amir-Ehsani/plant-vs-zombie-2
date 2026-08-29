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
import controllers.core.GameController;
import models.account.PlantData;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.engine.session.GameSession;
import models.level.core.Level;
import models.level.rules.impl.LockedPlantsRule;
import ui.AdventureMissionCatalog;
import ui.BackButton;
import ui.MenuButton;
import ui.SeedPacketCatalog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AdventurePlantSelectionScreen extends BaseMenuScreen {
    private static final Color TEXT_COLOR = Color.valueOf("4A3A1F");
    private static final Color TITLE_COLOR = Color.WHITE;
    private static final float PACKET_WIDTH = 132f;
    private static final float PACKET_HEIGHT = 88f;
    private static final Color LOCKED_DIM_COLOR = new Color(0f, 0f, 0f, 0.50f);

    private final String chapterName;
    private final int levelNumber;
    private final GameController controller;
    private final PlantRegistry plantRegistry;
    private final Table plantGrid;
    private final Table selectedSlots;
    private final Table detailPanel;
    private final Label selectionCount;
    private final Set<String> paidBoostNames;
    private String focusedPlantName;

    public AdventurePlantSelectionScreen(Main game, String chapterName, int levelNumber) {
        super(game);
        this.chapterName = chapterName;
        this.levelNumber = levelNumber;
        controller = game.getGameController();
        plantRegistry = DefaultPlantRegistry.getInstance();
        plantGrid = new Table();
        selectedSlots = new Table();
        detailPanel = new Table();
        selectionCount = new Label("", skin, "secondary");
        selectionCount.setColor(TEXT_COLOR);
        paidBoostNames = new HashSet<>();
        focusedPlantName = "";
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
        root.top();
        addResourceBar(root);

        Table panel = createPanel();
        panel.pad(14f, 18f, 14f, 18f);

        Label screenTitle = createTitle("Choose Your Plants");
        screenTitle.setColor(TITLE_COLOR);
        panel.add(screenTitle).padBottom(2f).row();

        Label mission = panelLabel(AdventureMissionCatalog.mission(chapterName, levelNumber));
        mission.setWrap(true);
        mission.setAlignment(Align.center);
        panel.add(mission).width(1010f).padBottom(4f).row();
        panel.add(selectionCount).padBottom(4f).row();

        Table body = new Table();
        body.top();

        Table selectedColumn = new Table();
        selectedColumn.top();
        Label selectedTitle = new Label("Selected", skin, "medium_outline");
        selectedTitle.setColor(TITLE_COLOR);
        selectedTitle.setAlignment(Align.center);
        selectedColumn.add(selectedTitle).width(142f).padBottom(4f).row();
        selectedColumn.add(selectedSlots).width(146f).top();
        body.add(selectedColumn).width(150f).height(500f).top().padRight(10f);

        Table browser = new Table();
        browser.top();
        browser.add(detailPanel).width(820f).height(150f).padBottom(6f).row();

        plantGrid.top().left();
        plantGrid.defaults().pad(4f);
        ScrollPane scrollPane = new ScrollPane(plantGrid, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);
        browser.add(scrollPane).width(840f).height(330f);
        body.add(browser).width(850f).height(500f).top();

        panel.add(body).width(1015f).height(500f).row();

        Table actions = new Table();
        actions.add(new BackButton(skin, () -> game.getScreenManager().showAdventureMission(chapterName, levelNumber)))
                .width(180f).height(46f).padRight(14f);
        actions.add(new MenuButton("LET'S ROCK", skin, "purple", this::startLevel))
                .width(220f).height(52f);
        panel.add(actions).padTop(6f);

        root.add(panel).width(1100f).height(650f);
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
        ensureFocusedPlant();
        rebuildSelectedSlots();
        rebuildDetailPanel();
        rebuildPlantGrid();
    }

    private void ensureFocusedPlant() {
        if (!focusedPlantName.isBlank() && findPlantData(focusedPlantName) != null) {
            return;
        }
        List<PlantData> available = availablePlants();
        focusedPlantName = available.isEmpty() ? "" : available.get(0).getName();
    }

    private void rebuildSelectedSlots() {
        selectedSlots.clearChildren();
        selectedSlots.top();
        selectedSlots.defaults().padBottom(6f);
        List<String> selected = selectedPlantNames();
        int selectionLimit = controller.getCurrentPlantSelectionLimit();
        int totalSlotCount = totalSelectionSlotCount();
        selectionCount.setText("Selected Plants: " + selected.size() + " / " + selectionLimit);
        for (int index = 0; index < totalSlotCount; index++) {
            boolean lockedSlot = isLockedSelectionSlot(index);
            String plantName = !lockedSlot && index < selected.size() ? selected.get(index) : null;
            selectedSlots.add(createSelectedSlot(index + 1, plantName, lockedSlot)).width(140f).height(56f).row();
        }
    }

    private Table createSelectedSlot(int slotNumber, String plantName, boolean lockedSlot) {
        Table slot = new Table();
        Stack stack = createPacketStack(plantName, 134f, 54f, 84f, 42f);
        slot.add(stack).width(134f).height(54f);
        if (lockedSlot) {
            addLockedOverlay(stack, 134f, 54f);
        } else if (plantName == null) {
            Label empty = new Label(String.valueOf(slotNumber), skin, "secondary");
            empty.setColor(TEXT_COLOR);
            empty.setAlignment(Align.center);
            stack.add(empty);
        } else {
            slot.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    focusedPlantName = plantName;
                    removePlant(plantName);
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

        PlantType type = plantRegistry.getByName(data.getName());
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
        info.add(panelLabel("Boost: " + (isBoosted(data.getName()) ? "Ready" : "None"))).left().row();
        detailPanel.add(info).width(320f).top().left().padRight(12f);

        Table actions = new Table();
        actions.top();
        boolean selected = isSelected(data.getName());
        boolean locked = isPlantLockedForCurrentLevel(data.getName());
        MenuButton select = new MenuButton(
                selected ? "Remove" : locked ? "Locked" : "Select",
                skin,
                selected ? "brown" : "green_small",
                () -> {
                    if (locked) {
                        showControllerMessage("This seed packet is locked for this level.");
                        refreshAll();
                        return;
                    }
                    togglePlant(data.getName());
                }
        );
        select.setDisabled(locked && !selected);
        actions.add(select).width(150f).height(38f).padBottom(5f).row();
        MenuButton boost = new MenuButton("Boost", skin, "purple", () -> boostPlant(data.getName()));
        boost.setDisabled(!selected || paidBoostNames.contains(normalize(data.getName())));
        actions.add(boost).width(150f).height(38f).padBottom(5f).row();
        MenuButton upgrade = new MenuButton("Upgrade", skin, "green_small", () -> upgradePlant(data.getName()));
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
            plantGrid.add(panelLabel("No unlocked plants are available for this level.")).pad(20f);
        }
    }

    private Table createSeedPacketChoice(PlantData data) {
        Table card = new Table();
        Stack stack = createPacketStack(data.getName(), PACKET_WIDTH, PACKET_HEIGHT, 115f, 72f);
        boolean locked = isPlantLockedForCurrentLevel(data.getName());

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.top().right();
        Label cost = createSunCostLabel(resolveSunCost(data.getName()));
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
        } else if (locked) {
            addLockedOverlay(stack, PACKET_WIDTH, PACKET_HEIGHT);
        }

        card.add(stack).width(PACKET_WIDTH).height(PACKET_HEIGHT);
        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                focusedPlantName = data.getName();
                if (locked) {
                    showControllerMessage("This seed packet is locked for this level.");
                    refreshAll();
                    return;
                }
                togglePlant(data.getName());
            }
        });
        return card;
    }

    private Stack createPacketStack(String plantName, float width, float height, float packetWidth, float packetHeight) {
        Stack stack = new Stack();
        TextureRegion backgroundRegion = game.getAnimationService().region(packetBackgroundId(plantName));
        if (backgroundRegion != null) {
            Image background = new Image(backgroundRegion);
            background.setScaling(Scaling.fill);
            stack.add(background);
        }

        if (plantName != null && !plantName.isBlank()) {
            TextureRegion packetRegion = SeedPacketCatalog.region(game.getAnimationService(), plantName);
            if (packetRegion != null) {
                Table packetHolder = new Table();
                packetHolder.setFillParent(true);
                packetHolder.bottom().left();
                Image packet = new Image(new TextureRegionDrawable(packetRegion));
                packet.setScaling(Scaling.fit);
                packetHolder.add(packet).width(packetWidth).height(packetHeight).left().bottom().padBottom(8);
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
        }
        return stack;
    }


    private void addLockedOverlay(Stack stack, float width, float height) {
        TextureRegion backgroundRegion = game.getAnimationService().region("IMAGE_UI_PACKETS_HOMELESS");
        if (backgroundRegion != null) {
            Image dim = new Image(new TextureRegionDrawable(backgroundRegion));
            dim.setColor(LOCKED_DIM_COLOR);
            dim.setScaling(Scaling.fill);
            stack.add(dim);
        }

        TextureRegion lockRegion = game.getAnimationService().region("IMAGE_UI_CARDS_LOCK_MEDIUM_GOLD");
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.center();
        if (lockRegion != null) {
            Image lock = new Image(new TextureRegionDrawable(lockRegion));
            lock.setScaling(Scaling.fit);
            overlay.add(lock).width(Math.min(width * 0.34f, 44f)).height(Math.min(height * 0.68f, 50f)).padBottom(2f).row();
        }
        Label locked = new Label("LOCKED", skin, "secondary");
        locked.setColor(TITLE_COLOR);
        locked.setAlignment(Align.center);
        overlay.add(locked).padBottom(2f);
        stack.add(overlay);
    }

    private String packetBackgroundId(String plantName) {
        return plantName != null && !plantName.isBlank() && isSelected(plantName)
                ? "IMAGE_UI_PACKETS_READY_PREMIUM"
                : "IMAGE_UI_PACKETS_HOMELESS";
    }

    private Label createSunCostLabel(int sunCost) {
        Label.LabelStyle style = new Label.LabelStyle(skin.get("secondary", Label.LabelStyle.class));
        style.font = skin.getFont("FBUSV8C6EI_3");
        Label label = new Label(String.valueOf(sunCost), style);
        label.setColor(Color.YELLOW);
        return label;
    }

    private void togglePlant(String plantName) {
        if (isSelected(plantName)) {
            removePlant(plantName);
            return;
        }
        int selectionLimit = controller.getCurrentPlantSelectionLimit();
        if (selectedPlantNames().size() >= selectionLimit) {
            showControllerMessage("ERROR: You can select at most " + selectionLimit + " plants.");
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
        boolean debugMode = user.getSettings() != null && user.getSettings().isDebugMode();
        for (PlantData data : user.getCollection().getOwnedPlants()) {
            if (data == null || !data.isUnlocked() || !isPlantVisibleInGrid(level, data.getName())) {
                continue;
            }
            result.add(data);
        }
        result.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return result;
    }


    private boolean isPlantVisibleInGrid(Level level, String plantName) {
        if (level == null || plantName == null || plantName.isBlank()) {
            return false;
        }
        if (level.getLevelRule() instanceof LockedPlantsRule) {
            List<String> allowedPlants = level.getAllowedPlants();
            return allowedPlants.isEmpty() || containsIgnoreCase(allowedPlants, plantName);
        }
        return level.isPlantAllowed(plantName);
    }

    private boolean isPlantLockedForCurrentLevel(String plantName) {
        if (plantName == null || isSelected(plantName)) {
            return false;
        }
        GameSession session = controller.getGameSession();
        Level level = session == null ? null : session.getCurrentLevel();
        return level != null
                && level.getLevelRule() instanceof LockedPlantsRule
                && !level.isPlantAllowed(plantName);
    }

    private int totalSelectionSlotCount() {
        LockedPlantsRule rule = lockedPlantsRule();
        return rule == null ? controller.getCurrentPlantSelectionLimit() : rule.getTotalSelectionSlotCount();
    }

    private boolean isLockedSelectionSlot(int slotIndex) {
        LockedPlantsRule rule = lockedPlantsRule();
        return rule != null && slotIndex >= rule.getAvailableSelectionSlotCount();
    }

    private LockedPlantsRule lockedPlantsRule() {
        GameSession session = controller.getGameSession();
        Level level = session == null ? null : session.getCurrentLevel();
        return level != null && level.getLevelRule() instanceof LockedPlantsRule
                ? (LockedPlantsRule) level.getLevelRule()
                : null;
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        String normalizedTarget = normalize(target);
        for (String value : values) {
            if (normalize(value).equals(normalizedTarget)) {
                return true;
            }
        }
        return false;
    }

    private PlantData findPlantData(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return null;
        }
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            return null;
        }
        return user.getCollection().findPlant(plantName);
    }

    private int resolveSunCost(String plantName) {
        PlantType type = plantRegistry.getByName(plantName);
        return type == null ? 0 : type.getSunCost();
    }

    private boolean isBoosted(String plantName) {
        PlantData data = findPlantData(plantName);
        return data != null && (data.getBoostCount() > 0 || paidBoostNames.contains(normalize(plantName)));
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
