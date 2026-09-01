package controllers.features;

import controllers.auth.AuthController;
import models.account.IPurchasable;
import models.account.PlantData;
import models.account.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShopController extends ShopControllerBase {
    public ShopController() {
        super();
    }

    public ShopController(AuthController authController) {
        super(authController);
    }

    public void enterShop() {
        insideShop = true;
        success("Entered shop.");
    }

    public void exitShop() {
        insideShop = false;
        success("Exited shop.");
    }

    public boolean isInsideShop() {
        return insideShop;
    }

    public List<ShopItem> getPermanentItems() {
        User user = authController == null ? null : authController.getLoggedInUser();
        return getPermanentItems(user);
    }

    public List<ShopItem> getPermanentItems(User user) {
        List<ShopItem> items = new ArrayList<>();
        for (ShopItem item : permanentItems) {
            items.add(resolveItemAmount(user, item));
        }
        return items;
    }

    public List<ShopItem> getDailyItems() {
        User user = authController == null ? null : authController.getLoggedInUser();
        return getDailyItems(user);
    }

    public List<ShopItem> getDailyItems(User user) {
        ShopItem daily = createDailyItem(user);
        List<ShopItem> items = new ArrayList<>();
        if (daily != null) {
            items.add(daily);
        }
        return items;
    }

    public List<ShopItem> getAllItems() {
        User user = authController == null ? null : authController.getLoggedInUser();
        List<ShopItem> items = getPermanentItems(user);
        items.addAll(getDailyItems(user));
        return items;
    }

    public ShopItem findItemById(String itemId) {
        User user = authController == null ? null : authController.getLoggedInUser();
        return findItemById(user, itemId);
    }

    public ShopItem findItemById(User user, String itemId) {
        String normalizedId = normalize(itemId);
        for (ShopItem item : permanentItems) {
            if (normalize(item.getId()).equals(normalizedId)) {
                return resolveItemAmount(user, item);
            }
        }
        ShopItem daily = createDailyItem(user);
        if (daily != null && normalize(daily.getId()).equals(normalizedId)) {
            return daily;
        }
        return null;
    }

    public void processPurchase(User user, IPurchasable item) {
        processPurchase(user, item, 1, "");
    }

    public boolean processPurchase(User user, IPurchasable item, int count, String plantType) {
        if (user == null || item == null) {
            fail("User or item is not available.");
            return false;
        }
        if (count <= 0) {
            fail("Count must be positive.");
            return false;
        }
        if (item instanceof ShopItem shopItem) {
            return buy(user, shopItem.getId(), count, plantType);
        }
        if (item instanceof PlantData plantData) {
            return purchasePlant(user, plantData, count);
        }
        fail("Unsupported purchasable item.");
        return false;
    }

    public boolean canBuy(String itemId, int count, String plantType) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return false;
        }
        if (count <= 0) {
            fail("Count must be positive.");
            return false;
        }
        ShopItem item = findItemById(user, itemId);
        if (item == null) {
            fail("Item was not found.");
            return false;
        }
        if (item.isDaily() && count != 1) {
            fail("The daily offer can only be bought once.");
            return false;
        }
        if (!validatePurchase(user, item, count, plantType)) {
            return false;
        }
        int totalPrice = calculateTotalPrice(item, count);
        if (totalPrice < 0) {
            fail("The purchase amount is too large.");
            return false;
        }
        int balance = "gem".equals(item.getCurrency()) ? user.getGems() : user.getCoins();
        if (balance < totalPrice) {
            fail("Not enough " + item.getCurrency() + "s.");
            return false;
        }
        success("Purchase is available.");
        return true;
    }

    public boolean buy(String itemId, int count, String plantType) {
        User user = getLoggedInUserOrFail();
        return user != null && buy(user, itemId, count, plantType);
    }

    public boolean buy(User user, String itemId, int count) {
        return buy(user, itemId, count, "");
    }

    public boolean buy(User user, String itemId, int count, String plantType) {
        if (user == null) {
            fail("User is not available.");
            return false;
        }
        if (count <= 0) {
            fail("Count must be positive.");
            return false;
        }
        ShopItem item = findItemById(user, itemId);
        if (item == null) {
            fail("Item was not found.");
            return false;
        }
        if (item.isDaily() && count != 1) {
            fail("The daily offer can only be bought once.");
            return false;
        }
        if (!validatePurchase(user, item, count, plantType)) {
            return false;
        }
        int totalPrice = calculateTotalPrice(item, count);
        if (totalPrice < 0) {
            fail("The purchase amount is too large.");
            return false;
        }
        if (!spendCurrency(user, item.getCurrency(), totalPrice)) {
            fail("Not enough " + item.getCurrency() + "s.");
            return false;
        }
        if (!consumeStock(user, item, count)) {
            refundCurrency(user, item.getCurrency(), totalPrice);
            fail("The requested amount is no longer available.");
            return false;
        }
        if (!applyPurchasedItem(user, item, count, plantType)) {
            restoreStock(user, item, count);
            refundCurrency(user, item.getCurrency(), totalPrice);
            fail("Purchase could not be applied.");
            return false;
        }
        if (item.isDaily()) {
            user.getCollection().markDailyOfferPurchased();
        }
        saveUsers();
        int remainingAmount = getRemainingAmount(user, item);
        if ("random_seed_packet".equals(item.getType()) && lastGrantedSeedPlantName != null
                && !lastGrantedSeedPlantName.isBlank()) {
            success("Purchased Random Seed Packet. You received "
                    + (item.getUnitAmount() * count) + " seeds for "
                    + lastGrantedSeedPlantName + ".");
        } else {
            success("Purchased " + count + "x " + item.getName()
                    + ". Remaining amount: " + remainingAmount + ".");
        }
        return true;
    }

    public static class ShopItem implements IPurchasable {
        protected final String id;
        protected final String name;
        protected final String type;
        protected final String targetName;
        protected final int price;
        protected final String currency;
        protected final int unitAmount;
        protected final int defaultAmount;
        protected final int amount;
        protected final boolean daily;

        public ShopItem(
                String id,
                String name,
                String type,
                String targetName,
                int price,
                String currency,
                int amount,
                boolean daily
        ) {
            this(id, name, type, targetName, price, currency, amount, amount, amount, daily);
        }

        public ShopItem(
                String id,
                String name,
                String type,
                String targetName,
                int price,
                String currency,
                int unitAmount,
                int defaultAmount,
                int amount,
                boolean daily
        ) {
            this.id = safeText(id);
            this.name = safeText(name);
            this.type = safeText(type);
            this.targetName = safeText(targetName);
            this.price = Math.max(0, price);
            this.currency = normalizeCurrency(currency);
            this.unitAmount = Math.max(1, unitAmount);
            this.defaultAmount = Math.max(0, defaultAmount);
            this.amount = Math.max(0, amount);
            this.daily = daily;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getTargetName() {
            return targetName;
        }

        @Override
        public int getPrice() {
            return price;
        }

        public String getCurrency() {
            return currency;
        }

        public int getUnitAmount() {
            return unitAmount;
        }

        public int getDefaultAmount() {
            return defaultAmount;
        }

        public int getAmount() {
            return amount;
        }

        public boolean isDaily() {
            return daily;
        }

        @Override
        public boolean isUnlocked() {
            return false;
        }

        protected ShopItem copyWithAmount(int newAmount) {
            return new ShopItem(
                    id, name, type, targetName, price, currency,
                    unitAmount, defaultAmount, newAmount, daily
            );
        }

        protected static String safeText(String value) {
            return value == null ? "" : value.trim();
        }

        protected static String normalizeCurrency(String value) {
            String normalized = safeText(value).toLowerCase(Locale.ROOT);
            return "diamond".equals(normalized) ? "gem" : normalized;
        }
    }
}
