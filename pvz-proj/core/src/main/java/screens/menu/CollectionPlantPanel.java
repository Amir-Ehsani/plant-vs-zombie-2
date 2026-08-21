package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import controllers.auth.AuthController;
import controllers.features.CollectionController;
import models.account.PlantData;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import pvz.skin.BorderedTable;
import ui.MenuButton;
import ui.PlantCard;
import ui.PvzAnimationService;

import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

public class CollectionPlantPanel extends Table {
    private static final Color PANEL_TEXT_COLOR = Color.valueOf("4A3A1F");
    private static final String ALL_FAMILIES = "All Families";
    private static final String ALL_STATES = "All";
    private static final String LOCKED = "Locked";
    private static final String UNLOCKED = "Unlocked";
    private static final String UPGRADEABLE = "Upgradeable";
    private final Skin skin;
    private final CollectionController controller;
    private final AuthController authController;
    private final PlantRegistry plantRegistry;
    private final PvzAnimationService animations;
    private final Consumer<String> messageHandler;
    private final Runnable resourceRefresh;
    private final SelectBox<String> familyFilter;
    private final SelectBox<String> stateFilter;
    private final Table cardsTable;
    private final Table detailsTable;
    private String selectedPlantName;

    public CollectionPlantPanel(
            Skin skin,
            CollectionController controller,
            AuthController authController,
            PvzAnimationService animations,
            Consumer<String> messageHandler,
            Runnable resourceRefresh
    ) {
        this.skin = skin;
        this.controller = controller;
        this.authController = authController;
        plantRegistry = DefaultPlantRegistry.getInstance();
        this.animations = animations;
        this.messageHandler = messageHandler;
        this.resourceRefresh = resourceRefresh;
        familyFilter = new SelectBox<>(skin, "default");
        stateFilter = new SelectBox<>(skin, "default");
        cardsTable = new Table();
        detailsTable = new Table();
        selectedPlantName = "";
        buildUi();
        bindFilters();
        refresh();
    }

    public void refresh() {
        List<PlantType> types = plantRegistry.getAllPlantTypes();
        refreshFamilyFilter(types);
        ensureSelection(types);
        refreshCards(types);
        refreshDetails();
    }

    private void buildUi() {
        defaults().pad(5f);
        Table filters = new Table();
        filters.add(panelLabel("Family")).padRight(6f);
        filters.add(familyFilter).width(185f).padRight(18f);
        filters.add(panelLabel("State")).padRight(6f);
        filters.add(stateFilter).width(165f);
        add(filters).colspan(2).left().row();
        ScrollPane cardsScroll = new ScrollPane(cardsTable, skin);
        cardsScroll.setFadeScrollBars(false);
        ScrollPane detailsScroll = new ScrollPane(detailsTable, skin);
        detailsScroll.setFadeScrollBars(false);
        add(cardsScroll).width(700f).height(430f).top();
        add(detailsScroll).width(405f).height(430f).top();
        stateFilter.setItems(ALL_STATES, UNLOCKED, LOCKED, UPGRADEABLE);
    }

    private void bindFilters() {
        ChangeListener listener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                refreshCards(plantRegistry.getAllPlantTypes());
            }
        };
        familyFilter.addListener(listener);
        stateFilter.addListener(listener);
    }

    private void refreshFamilyFilter(List<PlantType> types) {
        String previous = familyFilter.getSelected();
        TreeSet<String> families = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (PlantType type : types) {
            families.add(type.getCategory());
        }
        String[] items = new String[families.size() + 1];
        items[0] = ALL_FAMILIES;
        int index = 1;
        for (String family : families) {
            items[index++] = family;
        }
        familyFilter.setItems(items);
        if (previous != null && families.contains(previous)) {
            familyFilter.setSelected(previous);
        }
    }

    private void ensureSelection(List<PlantType> types) {
        if (findType(types, selectedPlantName) != null) {
            return;
        }
        selectedPlantName = types.isEmpty() ? "" : types.get(0).getName();
    }

    private void refreshCards(List<PlantType> types) {
        cardsTable.clearChildren();
        int column = 0;
        for (PlantType type : types) {
            PlantData data = plantData(type.getName());
            if (data == null || !matchesFilters(type, data)) {
                continue;
            }
            PlantCard card = createCard(type, data);
            cardsTable.add(card).width(325f).pad(6f).top();
            column++;
            if (column % 2 == 0) {
                cardsTable.row();
            }
        }
        if (column == 0) {
            cardsTable.add(panelLabel("No plants match these filters.")).pad(24f);
        }
    }

    private PlantCard createCard(PlantType type, PlantData data) {
        PlantCard card = new PlantCard(skin);
        card.setName(type.getName());
        card.setCost(type.getSunCost());
        card.setLevel(data.getLevel());
        card.setSeedPacketProgress(data.getSeedPackets(), data.getRequiredSeedPacketsForNextLevel());
        card.setFamily(type.getCategory());
        card.setTags(type.getTags());
        card.setHealth(type.getBaseHp());
        card.setLocked(!data.isUnlocked());
        card.setBoosted(data.getBoostCount() > 0);
        card.setSelected(type.getName().equalsIgnoreCase(selectedPlantName));
        card.setCooldown(1f);
        card.setPlantActor(animations.createPlantActor(type.getName()));
        card.setOnClick(() -> selectPlant(type.getName()));
        configureCardAction(card, type, data);
        return card;
    }

    private void configureCardAction(PlantCard card, PlantType type, PlantData data) {
        if (!data.isUnlocked()) {
            card.setPurchaseAction(CollectionController.PLANT_PURCHASE_PRICE, () -> purchase(type.getName()));
            return;
        }
        if (data.getLevel() < 4) {
            card.setUpgradeAction(() -> upgrade(type.getName()));
            return;
        }
        card.clearAction();
    }

    private boolean matchesFilters(PlantType type, PlantData data) {
        String family = familyFilter.getSelected();
        if (family != null && !ALL_FAMILIES.equals(family) && !family.equalsIgnoreCase(type.getCategory())) {
            return false;
        }
        String state = stateFilter.getSelected();
        if (LOCKED.equals(state)) {
            return !data.isUnlocked();
        }
        if (UNLOCKED.equals(state)) {
            return data.isUnlocked();
        }
        if (UPGRADEABLE.equals(state)) {
            return canUpgradePlant(type.getName());
        }
        return true;
    }

    private void selectPlant(String plantName) {
        selectedPlantName = plantName;
        refreshCards(plantRegistry.getAllPlantTypes());
        refreshDetails();
    }

    private void purchase(String plantName) {
        controller.purchasePlant(plantName);
        messageHandler.accept(controller.getLastMessage());
        resourceRefresh.run();
        selectedPlantName = plantName;
        refresh();
    }

    private void upgrade(String plantName) {
        controller.upgradePlant(plantName);
        messageHandler.accept(controller.getLastMessage());
        resourceRefresh.run();
        selectedPlantName = plantName;
        refresh();
    }

    private void refreshDetails() {
        detailsTable.clearChildren();
        PlantType type = plantRegistry.getByName(selectedPlantName);
        PlantData data = plantData(selectedPlantName);
        if (type == null || data == null) {
            detailsTable.add(panelLabel("Select a plant to view details."));
            return;
        }
        BorderedTable panel = new BorderedTable();
        panel.pad(18f);
        panel.add(new Label(type.getName(), skin, "medium_outline")).padBottom(8f).row();
        panel.add(animations.createPlantActor(type.getName())).size(190f).padBottom(8f).row();
        addPlantDetails(panel, type, data);
        addDetailAction(panel, type, data);
        detailsTable.add(panel).width(380f).top();
    }

    private void addPlantDetails(Table panel, PlantType type, PlantData data) {
        addDetail(panel, "Status", data.isUnlocked() ? "Unlocked" : "Locked");
        addDetail(panel, "Level", String.valueOf(data.getLevel()));
        addDetail(panel, "Seeds", seedText(data));
        addDetail(panel, "Family", type.getCategory());
        addDetail(panel, "Tags", safeText(type.getTags()));
        addDetail(panel, "Sun Cost", String.valueOf(type.getSunCost()));
        addDetail(panel, "Health", String.valueOf(type.getBaseHp()));
        addDetail(panel, "Damage", safeText(type.getDamage()));
        addDetail(panel, "Ability", safeText(type.getBaseAbility()));
        addDetail(panel, "Plant Food", safeText(type.getPlantFoodEffect()));
        addDetail(panel, "Stored Boosts", String.valueOf(data.getBoostCount()));
        addUpgradeDetails(panel, data);
    }

    private void addUpgradeDetails(Table panel, PlantData data) {
        if (data.getLevel() >= 4) {
            addDetail(panel, "Next Upgrade", "Maximum level");
            return;
        }
        addDetail(panel, "Upgrade Seeds", String.valueOf(data.getRequiredSeedPacketsForNextLevel()));
        addDetail(panel, "Upgrade Coins", String.valueOf(data.getUpgradePrice()));
        addDetail(panel, "Upgrade State", canUpgradePlant(data.getName()) ? "Ready" : "Not ready");
    }

    private void addDetailAction(Table panel, PlantType type, PlantData data) {
        if (!data.isUnlocked()) {
            String text = "Buy for " + CollectionController.PLANT_PURCHASE_PRICE + " Coins";
            panel.add(new MenuButton(text, skin, "green", () -> purchase(type.getName())))
                    .width(255f).height(46f).padTop(10f).row();
            return;
        }
        if (data.getLevel() < 4) {
            panel.add(new MenuButton("Upgrade", skin, "purple", () -> upgrade(type.getName())))
                    .width(220f).height(46f).padTop(10f).row();
        }
    }

    private void addDetail(Table panel, String title, String value) {
        Label label = panelLabel(title + ": " + safeText(value));
        label.setWrap(true);
        label.setAlignment(Align.left);
        panel.add(label).width(330f).left().padTop(3f).row();
    }

    private Label panelLabel(String text) {
        Label label = new Label(text, skin, "secondary");
        label.setColor(PANEL_TEXT_COLOR);
        return label;
    }

    private PlantData plantData(String plantName) {
        User user = authController.getLoggedInUser();
        if (user == null) {
            return null;
        }
        PlantData data = user.getCollection().findPlant(plantName);
        if (data != null) {
            return data;
        }
        boolean starter = CollectionController.getStarterPlantNames().contains(plantName);
        return new PlantData(plantName, CollectionController.PLANT_PURCHASE_PRICE, starter);
    }

    private boolean canUpgradePlant(String plantName) {
        User user = authController.getLoggedInUser();
        PlantData data = user == null ? null : user.getCollection().findOwnedPlant(plantName);
        return data != null && data.canUpgrade() && user.getCoins() >= data.getUpgradePrice();
    }

    private String seedText(PlantData data) {
        int required = data.getRequiredSeedPacketsForNextLevel();
        return required == Integer.MAX_VALUE
                ? data.getSeedPackets() + " / MAX"
                : data.getSeedPackets() + " / " + required;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private PlantType findType(List<PlantType> types, String name) {
        for (PlantType type : types) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
