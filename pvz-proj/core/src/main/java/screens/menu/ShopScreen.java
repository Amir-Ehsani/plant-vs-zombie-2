package screens.menu;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.pvz.Main;
import controllers.features.ShopController;
import models.account.PlantData;
import models.account.User;
import ui.BackButton;
import ui.ConfirmDialog;
import ui.MenuButton;
import ui.PlantSelectionDialog;
import ui.PvzAnimationService;
import ui.ShopItemCard;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends BaseMenuScreen {
    private static final int PERMANENT_COLUMNS = 4;
    private static final int DAILY_COLUMNS = 3;
    private final ShopController controller;
    private final Runnable backAction;
    private final PvzAnimationService animations;
    private final Table permanentGrid;
    private final Table dailyGrid;
    private final List<ShopItemCard> itemCards;

    public ShopScreen(Main game, Runnable backAction) {
        super(game);
        controller = game.getShopController();
        this.backAction = backAction == null ? game.getScreenManager()::showMainMenu : backAction;
        animations = game.getAnimationService();
        permanentGrid = new Table();
        dailyGrid = new Table();
        permanentGrid.top();
        dailyGrid.top();
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
        addShopBackground();
        Table root = createRoot();
        root.pad(8f, 10f, 8f, 10f);
        addResourceBar(root);
        Table panel = createPanel();
        panel.pad(16f, 20f, 16f, 20f);
        panel.add(createTitle("Shop")).colspan(2).padBottom(4f).row();
        panel.add(createHeaderNote()).colspan(2).padBottom(8f).row();
        Table navigation = new Table();
        navigation.add(new MenuButton("Greenhouse", skin, "green", game.getScreenManager()::showGreenhouse))
                .width(170f).height(44f);
        navigation.add().expandX().fillX();
        navigation.add(new BackButton(skin, backAction)).width(160f).height(44f);
        panel.add(navigation).colspan(2).width(1070f).fillX().padBottom(8f).row();
        panel.add(createStoreTab("Permanent Store")).left().padBottom(4f).row();
        ScrollPane permanentScroll = new ScrollPane(permanentGrid, skin);
        permanentScroll.setFadeScrollBars(false);
        permanentScroll.setOverscroll(false, false);
        permanentScroll.setScrollingDisabled(true, false);
        panel.add(permanentScroll).width(1070f).height(276f).top().padBottom(8f).row();
        panel.add(createStoreTab("Daily Offer")).left().padBottom(4f).row();
        ScrollPane dailyScroll = new ScrollPane(dailyGrid, skin);
        dailyScroll.setFadeScrollBars(false);
        dailyScroll.setOverscroll(false, false);
        dailyScroll.setScrollingDisabled(true, false);
        panel.add(dailyScroll).width(1070f).height(166f).top();
        root.add(panel).width(1185f).height(648f);
    }

    private void addShopBackground() {
        TextureRegion region = animations.region("IMAGE_TITLEBACKGROUNDS_BACKDROP_B");
        if (region == null) {
            addMenuBackground();
            return;
        }
        Image background = new Image(region);
        background.setBounds(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        background.setScaling(Scaling.fill);
        stage.addActor(background);
    }

    private Label createHeaderNote() {
        Label label = new Label("Plants you buy in the store will be bought for all profiles", skin, "secondary");
        label.setAlignment(Align.center);
        return label;
    }

    private Stack createStoreTab(String title) {
        TextureRegion region = animations.region("IMAGE_UI_STORE_PLANTSTAB_ACTIVE");
        Stack stack = new Stack();
        if (region != null) {
            Image background = new Image(region);
            background.setScaling(Scaling.stretch);
            stack.add(background);
        }
        Label label = new Label(title, skin, "medium_outline");
        label.setAlignment(Align.center);
        Table overlay = new Table();
        overlay.add(label).expand().fill();
        stack.add(overlay);
        stack.setSize(252f, 54f);
        return stack;
    }

    private void refreshAll() {
        disposeItemCards();
        rebuildGrid(permanentGrid, controller.getPermanentItems(), PERMANENT_COLUMNS);
        rebuildGrid(dailyGrid, controller.getDailyItems(), DAILY_COLUMNS);
        refreshResourceBar();
    }

    private void rebuildGrid(Table grid, List<ShopController.ShopItem> items, int columnCount) {
        grid.clearChildren();
        grid.defaults().pad(8f).top();
        int column = 0;
        for (ShopController.ShopItem item : items) {
            ShopItemCard card = new ShopItemCard(
                    skin,
                    animations,
                    item,
                    remainingText(item),
                    () -> handleBuy(item)
            );
            card.setBuyEnabled(item.getAmount() > 0);
            itemCards.add(card);
            grid.add(card).width(236f).height(248f).top();
            column++;
            if (column % columnCount == 0) {
                grid.row();
            }
        }
        if (column == 0) {
            grid.add(new Label("No items available.", skin, "medium_outline")).pad(12f);
        }
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
                .append(" for ").append(item.getPrice()).append(' ').append(currencyName(item.getCurrency())).append('?');
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
                return "Purchased today | Refresh: " + timeUntilMidnight();
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
