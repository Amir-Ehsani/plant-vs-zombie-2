package screens.menu;

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pvz.Main;
import controllers.features.ShopController;
import models.account.PlantData;
import models.account.User;
import ui.BackButton;
import ui.ConfirmDialog;
import ui.MenuButton;
import ui.PlantSelectionDialog;
import ui.ShopItemCard;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends BaseMenuScreen {
    private static final int COLUMN_COUNT = 4;
    private final ShopController controller;
    private final Runnable backAction;
    private final Table itemsGrid;
    private final List<ShopItemCard> itemCards;

    public ShopScreen(Main game, Runnable backAction) {
        super(game);
        controller = game.getShopController();
        this.backAction = backAction == null ? game.getScreenManager()::showMainMenu : backAction;
        itemsGrid = new Table();
        itemsGrid.top();
        itemCards = new ArrayList<>();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        controller.enterShop();
        refreshAll();
    }

    @Override
    public void dispose() {
        disposeItemCards();
        controller.exitShop();
        super.dispose();
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        root.pad(8f, 10f, 8f, 10f);
        addResourceBar(root);
        Table panel = createPanel();
        panel.pad(16f, 20f, 16f, 20f);
        panel.add(createTitle("Shop")).colspan(3).padBottom(8f).row();
        Table navigation = new Table();
        navigation.add(new MenuButton("Greenhouse", skin, "green", game.getScreenManager()::showGreenhouse))
                .width(170f).height(44f);
        navigation.add().expandX().fillX();
        navigation.add(new BackButton(skin, backAction)).width(160f).height(44f);
        panel.add(navigation).colspan(3).width(1070f).fillX().padBottom(8f).row();
        ScrollPane scrollPane = new ScrollPane(itemsGrid, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);
        panel.add(scrollPane).colspan(3).width(1070f).height(446f).top();
        root.add(panel).width(1185f).height(610f);
    }

    private void refreshAll() {
        disposeItemCards();
        itemsGrid.clearChildren();
        itemsGrid.defaults().pad(7f).top();
        List<ShopController.ShopItem> items = controller.getAllItems();
        int column = 0;
        for (ShopController.ShopItem item : items) {
            ShopItemCard card = new ShopItemCard(
                    skin,
                    item,
                    remainingText(item),
                    () -> handleBuy(item)
            );
            card.setBuyEnabled(item.getAmount() > 0);
            itemCards.add(card);
            itemsGrid.add(card).width(245f).height(302f).top();
            column++;
            if (column % COLUMN_COUNT == 0) {
                itemsGrid.row();
            }
        }
        refreshResourceBar();
    }

    private void handleBuy(ShopController.ShopItem item) {
        if (item == null) {
            return;
        }
        if ("selected_seed_packet".equals(item.getType())) {
            showPlantSelection(item);
            return;
        }
        beginPurchase(item, "");
    }

    private void showPlantSelection(ShopController.ShopItem item) {
        PlantSelectionDialog dialog = new PlantSelectionDialog(
                "Select Seed Packet Plant",
                skin,
                game.getAnimationService(),
                ownedPlants(),
                false,
                plantName -> beginPurchase(item, plantName)
        );
        dialog.show(stage);
    }

    private List<PlantData> ownedPlants() {
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            return new ArrayList<>();
        }
        List<PlantData> plants = new ArrayList<>(user.getCollection().getOwnedPlants());
        plants.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return plants;
    }

    private void beginPurchase(ShopController.ShopItem item, String plantName) {
        if (!controller.canBuy(item.getId(), 1, plantName)) {
            showControllerMessage(controller.getLastMessage());
            refreshAll();
            return;
        }
        StringBuilder message = new StringBuilder();
        message.append("Buy ").append(item.getName())
                .append(" for ").append(item.getPrice()).append(' ')
                .append(currencyName(item.getCurrency())).append('?');
        if (plantName != null && !plantName.isBlank()) {
            message.append(" Plant: ").append(plantName).append('.');
        }
        ConfirmDialog dialog = new ConfirmDialog(
                "Confirm Purchase",
                message.toString(),
                skin,
                () -> purchase(item, plantName)
        );
        dialog.show(stage);
    }

    private void purchase(ShopController.ShopItem item, String plantName) {
        controller.buy(item.getId(), 1, plantName);
        showControllerMessage(controller.getLastMessage());
        refreshAll();
    }

    private String remainingText(ShopController.ShopItem item) {
        if (item == null) {
            return "";
        }
        if (item.isDaily()) {
            if (item.getAmount() <= 0) {
                return "Daily limit reached";
            }
            return "Remaining: 1 | Refresh: " + timeUntilMidnight();
        }
        return "Remaining: " + item.getAmount();
    }

    private String timeUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT);
        long minutes = Math.max(0L, Duration.between(now, midnight).toMinutes());
        return minutes / 60L + "h " + minutes % 60L + "m";
    }

    private String currencyName(String currency) {
        return "gem".equals(currency) ? "Diamonds" : "Coins";
    }

    private void disposeItemCards() {
        for (ShopItemCard card : itemCards) {
            card.dispose();
        }
        itemCards.clear();
    }
}
