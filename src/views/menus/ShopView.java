package views.menus;

import controllers.features.ShopController;
import models.account.IPurchasable;
import views.core.BaseView;

import java.util.List;

public class ShopView extends BaseView {
    public ShopView(String viewName) {
        super(viewName);
    }

    public ShopView() {
        super("Shop");
    }

    @Override
    public void display() {
        System.out.println("Shop");
        System.out.println("====");
        System.out.println("Use shop list to see permanent items.");
        System.out.println("Use shop daily to see daily items.");
        System.out.println("Use shop buy -i <item_id> -n <count> [-t <plant_type>] to buy an item.");
    }

    @Override
    public void showErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        System.out.println("ERROR: " + message);
    }

    @Override
    public void showSuccessMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        System.out.println("OK: " + message);
    }

    public void showCatalog(List<IPurchasable> permanent, List<IPurchasable> daily) {
        System.out.print(renderCatalog(permanent, daily));
    }

    public void showPermanentItems(List<ShopController.ShopItem> items) {
        System.out.print(renderShopItems("Permanent Shop", items));
    }

    public void showDailyItems(List<ShopController.ShopItem> items) {
        System.out.print(renderShopItems("Daily Shop", items));
    }

    public void showAllItems(List<ShopController.ShopItem> permanentItems, List<ShopController.ShopItem> dailyItems) {
        StringBuilder builder = new StringBuilder();
        builder.append(renderShopItems("Permanent Shop", permanentItems));
        builder.append("\n");
        builder.append(renderShopItems("Daily Shop", dailyItems));
        System.out.print(builder);
    }

    public void showPurchaseMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        System.out.println(message);
    }

    public String renderCatalog(List<IPurchasable> permanent, List<IPurchasable> daily) {
        StringBuilder builder = new StringBuilder();

        builder.append("Shop Catalog\n");
        builder.append("============\n\n");

        builder.append("Permanent Items\n");
        builder.append("---------------\n");
        builder.append(renderPurchasableList(permanent));

        builder.append("\n");

        builder.append("Daily Items\n");
        builder.append("-----------\n");
        builder.append(renderPurchasableList(daily));

        return builder.toString();
    }

    public String renderShopItems(String title, List<ShopController.ShopItem> items) {
        StringBuilder builder = new StringBuilder();

        builder.append(title)
                .append("\n");
        builder.append(repeat("=", title.length()))
                .append("\n");

        if (items == null || items.isEmpty()) {
            builder.append("No items available.\n");
            return builder.toString();
        }

        builder.append(String.format("%-24s %-22s %-14s %-10s %-10s %-8s%n", "ID", "Name", "Type", "Price", "Currency", "Amount"));
        builder.append(String.format("%-24s %-22s %-14s %-10s %-10s %-8s%n", "--", "----", "----", "-----", "--------", "------"));

        for (ShopController.ShopItem item : items) {
            if (item == null) {
                continue;
            }

            builder.append(String.format(
                    "%-24s %-22s %-14s %-10d %-10s %-8d%n",
                    item.getId(),
                    item.getName(),
                    item.getType(),
                    item.getPrice(),
                    item.getCurrency(),
                    item.getAmount()
            ));
        }

        return builder.toString();
    }

    public String renderItemDetails(ShopController.ShopItem item) {
        if (item == null) {
            return "Item is not available.\n";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Item Details\n");
        builder.append("============\n");
        builder.append("ID: ").append(item.getId()).append("\n");
        builder.append("Name: ").append(item.getName()).append("\n");
        builder.append("Type: ").append(item.getType()).append("\n");

        if (item.getTargetName() != null && !item.getTargetName().isBlank()) {
            builder.append("Target: ").append(item.getTargetName()).append("\n");
        }

        builder.append("Price: ").append(item.getPrice()).append(" ").append(item.getCurrency()).append("\n");
        builder.append("Amount: ").append(item.getAmount()).append("\n");
        builder.append("Daily: ").append(item.isDaily() ? "yes" : "no").append("\n");

        return builder.toString();
    }

    private String renderPurchasableList(List<IPurchasable> items) {
        StringBuilder builder = new StringBuilder();

        if (items == null || items.isEmpty()) {
            builder.append("No items available.\n");
            return builder.toString();
        }

        int index = 1;

        for (IPurchasable item : items) {
            if (item == null) {
                continue;
            }

            builder.append(index)
                    .append(". ");

            if (item instanceof ShopController.ShopItem shopItem) {
                builder.append(shopItem.getId())
                        .append(" | ")
                        .append(shopItem.getName())
                        .append(" | ")
                        .append(shopItem.getPrice())
                        .append(" ")
                        .append(shopItem.getCurrency())
                        .append(" | ")
                        .append(shopItem.getType());

                if (shopItem.getTargetName() != null && !shopItem.getTargetName().isBlank()) {
                    builder.append(" | target: ")
                            .append(shopItem.getTargetName());
                }
            } else {
                builder.append("Item")
                        .append(" | price: ")
                        .append(item.getPrice())
                        .append(" | unlocked: ")
                        .append(item.isUnlocked() ? "yes" : "no");
            }

            builder.append("\n");
            index++;
        }

        if (index == 1) {
            builder.append("No items available.\n");
        }

        return builder.toString();
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < count; i++) {
            builder.append(value);
        }

        return builder.toString();
    }
}