package views.menus;

import controllers.core.MenuManager;
import controllers.features.GreenhouseController;
import controllers.features.ShopController;
import models.account.IPurchasable;
import views.core.BaseView;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShopView extends BaseView {
    private static final Pattern BUY_PATTERN = Pattern.compile(
            "^shop\\s+buy\\s+-i\\s+(\\S+)\\s+-n\\s+(\\d+)(?:\\s+-t\\s+(.+?))?\\s*$"
    );

    private final MenuManager menuManager;
    private final ShopController controller;
    private final GreenhouseController greenhouseController;

    public ShopView(
            String viewName,
            MenuManager menuManager,
            ShopController controller,
            GreenhouseController greenhouseController
    ) {
        super(viewName);
        this.menuManager = menuManager;
        this.controller = controller;
        this.greenhouseController = greenhouseController;
    }

    public ShopView(String viewName) {
        this(viewName, null, new ShopController(), null);
    }

    public ShopView() {
        this("Shop Menu");
    }

    @Override
    public void display() {
        System.out.print(menuText());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);
        if (!isConnected()) {
            printControllerMessage("ERROR: Shop menu is not connected.");
            return;
        }
        if (!controller.isLoggedIn()) {
            printControllerMessage("ERROR: No user is logged in.");
            menuManager.enterLoginMenu();
            return;
        }

        if (handleNavigation(command) || handleList(command) || handleBuy(command)) {
            return;
        }
        controller.invalidCommand("shop menu");
        printControllerMessage(controller.getLastMessage());
    }

    public String menuText() {
        return """
                Shop Menu
                shop list
                shop daily
                shop buy -i <item_id> -n <count> [-t <plant_type>]
                menu show current
                menu exit
                """;
    }

    public void showCatalog(List<IPurchasable> permanent, List<IPurchasable> daily) {
        System.out.print(renderCatalog(permanent, daily));
    }

    public void showPermanentItems(List<ShopController.ShopItem> items) {
        System.out.print(renderShopItems("Permanent Shop", items));
    }

    public void showDailyItems(List<ShopController.ShopItem> items) {
        System.out.print(renderShopItems("Daily Offer", items));
    }

    public void showAllItems(
            List<ShopController.ShopItem> permanentItems,
            List<ShopController.ShopItem> dailyItems
    ) {
        System.out.print(renderShopItems("Permanent Shop", permanentItems));
        System.out.println();
        System.out.print(renderShopItems("Daily Offer", dailyItems));
    }

    public String renderCatalog(List<IPurchasable> permanent, List<IPurchasable> daily) {
        StringBuilder builder = new StringBuilder("Shop Catalog\n============\n\n");
        builder.append("Permanent Items\n---------------\n");
        builder.append(renderPurchasableList(permanent));
        builder.append("\nDaily Items\n-----------\n");
        builder.append(renderPurchasableList(daily));
        return builder.toString();
    }

    public String renderShopItems(String title, List<ShopController.ShopItem> items) {
        StringBuilder builder = new StringBuilder(title).append("\n");
        builder.append("=".repeat(title.length())).append("\n");
        if (items == null || items.isEmpty()) {
            builder.append("No items available.\n");
            return builder.toString();
        }

        builder.append(String.format(
                "%-22s %-22s %-22s %-9s %-9s %-7s%n",
                "ID", "Name", "Type", "Price", "Currency", "Amount"
        ));
        for (ShopController.ShopItem item : items) {
            if (item != null) {
                appendItemRow(builder, item);
            }
        }
        return builder.toString();
    }

    public String renderItemDetails(ShopController.ShopItem item) {
        if (item == null) {
            return "Item is not available.\n";
        }
        StringBuilder builder = new StringBuilder("Item Details\n============\n");
        builder.append("ID: ").append(item.getId()).append("\n");
        builder.append("Name: ").append(item.getName()).append("\n");
        builder.append("Type: ").append(item.getType()).append("\n");
        if (!item.getTargetName().isBlank()) {
            builder.append("Target: ").append(item.getTargetName()).append("\n");
        }
        builder.append("Price: ").append(item.getPrice()).append(" ").append(item.getCurrency()).append("\n");
        builder.append("Amount: ").append(item.getAmount()).append("\n");
        builder.append("Daily: ").append(item.isDaily() ? "yes" : "no").append("\n");
        return builder.toString();
    }

    private boolean handleNavigation(String command) {
        if ("menu show current".equals(command)) {
            menuManager.showCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }
        if ("menu exit".equals(command)) {
            if (greenhouseController == null) {
                menuManager.enterGameMenu();
            } else {
                menuManager.changeView(new GreenhouseView(
                        "Greenhouse Menu", menuManager, greenhouseController, controller
                ));
            }
            return true;
        }
        return false;
    }

    private boolean handleList(String command) {
        if ("shop list".equals(command)) {
            showPermanentItems(controller.getPermanentItems());
            return true;
        }
        if ("shop daily".equals(command)) {
            showDailyItems(controller.getDailyItems());
            return true;
        }
        return false;
    }

    private boolean handleBuy(String command) {
        Matcher matcher = BUY_PATTERN.matcher(command);
        if (!matcher.matches()) {
            return false;
        }

        Integer count = parseInteger(matcher.group(2));
        String plantType = matcher.group(3) == null ? "" : matcher.group(3);
        if (count == null) {
            controller.invalidCommand("shop menu");
        } else {
            controller.buy(matcher.group(1), count, plantType);
        }
        printControllerMessage(controller.getLastMessage());
        return true;
    }

    private void appendItemRow(StringBuilder builder, ShopController.ShopItem item) {
        builder.append(String.format(
                "%-22s %-22s %-22s %-9d %-9s %-7d%n",
                item.getId(), item.getName(), item.getType(), item.getPrice(), item.getCurrency(), item.getAmount()
        ));
        if (!item.getTargetName().isBlank()) {
            builder.append("  target: ").append(item.getTargetName()).append("\n");
        }
    }

    private String renderPurchasableList(List<IPurchasable> items) {
        if (items == null || items.isEmpty()) {
            return "No items available.\n";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (IPurchasable item : items) {
            if (item == null) {
                continue;
            }
            builder.append(index++).append(". price=").append(item.getPrice());
            if (item instanceof ShopController.ShopItem shopItem) {
                builder.append(" id=").append(shopItem.getId()).append(" name=").append(shopItem.getName());
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private boolean isConnected() {
        return menuManager != null && controller != null;
    }
}
