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
import models.account.PlantData;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantType;
import models.minigame.EgyptIZombieChooser;
import models.minigame.NetworkIZombieGame;
import network.game.AuthoritativeIZombieGame;
import ui.BackButton;
import ui.MenuButton;
import ui.SeedPacketCatalog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Ancient Egypt level-one plant chooser shown to the plant player before an online match starts. */
public final class NetworkPlantSelectionScreen extends BaseMenuScreen {
    private static final float PACKET_WIDTH = 132f;
    private static final float PACKET_HEIGHT = 88f;
    private static final Color TEXT_COLOR = Color.valueOf("4A3A1F");
    private static final Color TITLE_COLOR = Color.WHITE;

    private final EgyptIZombieChooser chooser;
    private final Table selectedSlots;
    private final Table detailPanel;
    private final Table plantGrid;
    private final Label selectionCount;
    private final Set<String> selectedPlants;
    private String focusedPlantName;
    private boolean starting;

    public NetworkPlantSelectionScreen(Main game, NetworkIZombieGame networkGame) {
        this(game, (EgyptIZombieChooser) networkGame);
    }

    public NetworkPlantSelectionScreen(Main game, EgyptIZombieChooser chooser) {
        super(game);
        this.chooser = chooser;
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

    @Override
    public void render(float delta) {
        if (chooser != null) {
            chooser.pumpChooser();
            if (chooser.isPlantsReady() && !starting) {
                starting = true;
                game.getScreenManager().showActiveMiniGame();
                return;
            }
            if (!chooser.isRunning()) {
                game.getScreenManager().showNetworkLobby();
                return;
            }
        }
        super.render(delta);
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
                "Defend the lawn for 10 minutes. Pick up to "
                        + AuthoritativeIZombieGame.MAX_SELECTED_PLANTS
                        + " plants, then press Let's Rock."
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
        plantGrid.padRight(8f);
        plantGrid.defaults().pad(3f);
        ScrollPane scrollPane = new ScrollPane(plantGrid, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setScrollbarsOnTop(false);
        browser.add(scrollPane).width(868f).height(280f);
        body.add(browser).width(885f).height(410f).top().center();

        panel.add(body).width(1015f).height(410f).center().row();

        Table actions = new Table();
        actions.add(new BackButton(skin, this::leaveMatch))
                .width(180f).height(46f).padRight(14f);
        actions.add(new MenuButton("LET'S ROCK", skin, "purple", this::startMatch))
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
        if (!focusedPlantName.isBlank() && catalogContains(focusedPlantName)) {
            return;
        }
        List<String> plants = catalog();
        focusedPlantName = plants.isEmpty() ? "" : plants.get(0);
    }

    private void rebuildSelectedSlots() {
        selectedSlots.clearChildren();
        selectedSlots.top();
        selectedSlots.defaults().padBottom(2f);
        List<String> selected = new ArrayList<>(selectedPlants);
        selectionCount.setText("Selected Plants: " + selected.size()
                + " / " + AuthoritativeIZombieGame.MAX_SELECTED_PLANTS);
        for (int index = 0; index < AuthoritativeIZombieGame.MAX_SELECTED_PLANTS; index++) {
            String plantName = index < selected.size() ? selected.get(index) : null;
            selectedSlots.add(createSelectedSlot(index + 1, plantName)).width(140f).height(48f).row();
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
                    togglePlant(plantName);
                }
            });
        }
        return slot;
    }

    private void rebuildDetailPanel() {
        detailPanel.clearChildren();
        PlantType type = DefaultPlantRegistry.getInstance().getByName(focusedPlantName);
        Table info = new Table();
        Label name = new Label(focusedPlantName.isBlank() ? "Select a plant" : focusedPlantName, skin, "medium_outline");
        name.setColor(TITLE_COLOR);
        info.add(name).left().row();
        Label detail = panelLabel(type == null ? "" : type.getBaseAbility());
        detail.setWrap(true);
        info.add(detail).width(620f).left();
        detailPanel.add(info).expandX().left().padRight(12f);

        Table actions = new Table();
        MenuButton select = new MenuButton(
                isSelected(focusedPlantName) ? "REMOVE" : "SELECT",
                skin,
                "green_small",
                () -> togglePlant(focusedPlantName)
        );
        actions.add(select).width(150f).height(38f);
        detailPanel.add(actions).width(170f).top();
    }

    private void rebuildPlantGrid() {
        plantGrid.clearChildren();
        int column = 0;
        for (String plantName : catalog()) {
            plantGrid.add(createSeedPacketChoice(plantName)).width(PACKET_WIDTH).height(PACKET_HEIGHT);
            column++;
            if (column % 6 == 0) {
                plantGrid.row();
            }
        }
    }

    private Table createSeedPacketChoice(String plantName) {
        Table card = new Table();
        Stack stack = createPacketStack(plantName, PACKET_WIDTH, PACKET_HEIGHT, 92f, 58f);

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.top().right();
        PlantType type = DefaultPlantRegistry.getInstance().getByName(plantName);
        Label cost = createSunCostLabel(type == null ? 0 : type.getSunCost());
        overlay.add(cost).padTop(6f).padRight(8f);
        stack.add(overlay);

        if (isSelected(plantName)) {
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
                focusedPlantName = plantName;
                togglePlant(plantName);
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
        label.setFontScale(0.42f);
        return label;
    }

    private void togglePlant(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return;
        }
        String existing = selectedName(plantName);
        if (existing != null) {
            selectedPlants.remove(existing);
            refreshAll();
            return;
        }
        if (selectedPlants.size() >= AuthoritativeIZombieGame.MAX_SELECTED_PLANTS) {
            return;
        }
        selectedPlants.add(plantName);
        refreshAll();
    }

    private void startMatch() {
        if (starting || chooser.isChooserBusy()) {
            return;
        }
        if (selectedPlants.isEmpty()) {
            notificationManager.showError("Pick at least one plant first.");
            return;
        }
        if (!chooser.lockSelectedPlants(new ArrayList<>(selectedPlants))
                && !chooser.getChooserMessage().isBlank()) {
            notificationManager.showError(chooser.getChooserMessage());
        }
    }

    private void leaveMatch() {
        chooser.abandonChooser();
        game.getTravelLogController().abandonMiniGame();
        game.getScreenManager().showNetworkLobby();
    }

    private List<String> catalog() {
        User user = game.getAuthController().getLoggedInUser();
        if (user != null && (user.isDebugAllContentUnlocked()
                || (user.getSettings() != null && user.getSettings().isDebugMode()))) {
            return unlockedPlantCatalog(user);
        }
        return chooser.getEgyptPlantCatalog();
    }

    private List<String> unlockedPlantCatalog(User user) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (PlantType type : DefaultPlantRegistry.getInstance().getAllPlantTypes()) {
            if (type == null || isHiddenSelectionPlant(type.getName())) {
                continue;
            }
            names.add(type.getName());
        }
        if (user != null && user.getCollection() != null) {
            for (PlantData data : user.getCollection().getOwnedPlants()) {
                if (data != null && data.isUnlocked() && !isHiddenSelectionPlant(data.getName())) {
                    names.add(data.getName());
                }
            }
        }
        List<String> result = new ArrayList<>(names);
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private boolean isHiddenSelectionPlant(String plantName) {
        String normalized = normalize(plantName);
        return normalized.equals("goo peashooter");
    }

    private boolean catalogContains(String plantName) {
        String normalized = normalize(plantName);
        for (String name : catalog()) {
            if (normalize(name).equals(normalized)) {
                return true;
            }
        }
        return false;
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
