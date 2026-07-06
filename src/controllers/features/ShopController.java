package controllers.features;

import models.account.Collection;
import models.account.Greenhouse;
import models.account.IPurchasable;
import models.account.PlantData;
import models.account.User;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShopController {
    private final List<ShopItem> permanentItems;
    private final List<ShopItem> dailyItems;
    private boolean insideShop;
    private String lastMessage;

    public ShopController() {
        this.permanentItems = new ArrayList<>();
        this.dailyItems = new ArrayList<>();
        this.insideShop = false;
        this.lastMessage = "";
        initializePermanentItems();
        initializeDailyItems();
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
        return new ArrayList<>(permanentItems);
    }

    public List<ShopItem> getDailyItems() {
        return new ArrayList<>(dailyItems);
    }

    public List<ShopItem> getAllItems() {
        List<ShopItem> items = new ArrayList<>();
        items.addAll(permanentItems);
        items.addAll(dailyItems);
        return items;
    }

    public ShopItem findItemById(String itemId) {
        String normalizedId = normalize(itemId);

        for (ShopItem item : getAllItems()) {
            if (normalize(item.getId()).equals(normalizedId)) {
                return item;
            }
        }

        return null;
    }

    public void processPurchase(User user, IPurchasable item) {
        if (user == null) {
            fail("User is not available.");
            return;
        }

        if (item == null) {
            fail("Item is not available.");
            return;
        }

        if (item.isUnlocked()) {
            fail("Item is already unlocked.");
            return;
        }

        int price = item.getPrice();

        if (!spendCoins(user, price)) {
            fail("Not enough coins.");
            return;
        }

        if (item instanceof PlantData plantData) {
            plantData.unlock();
            Collection collection = getOrCreateCollection(user);

            if (collection != null) {
                collection.unlockPlant(plantData);
            }
        }

        success("Purchase completed.");
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

        ShopItem item = findItemById(itemId);

        if (item == null) {
            fail("Item was not found.");
            return false;
        }

        int totalPrice = item.getPrice() * count;

        if (!spendCurrency(user, item.getCurrency(), totalPrice)) {
            fail("Not enough " + item.getCurrency() + ".");
            return false;
        }

        boolean applied = applyPurchasedItem(user, item, count, plantType);

        if (!applied) {
            addCurrency(user, item.getCurrency(), totalPrice);
            fail("Purchase could not be applied.");
            return false;
        }

        success("Purchased " + count + "x " + item.getName() + ".");
        return true;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private boolean applyPurchasedItem(User user, ShopItem item, int count, String plantType) {
        Collection collection = getOrCreateCollection(user);

        if (item.getType().equals("plant")) {
            if (collection == null) {
                return false;
            }

            PlantData plant = collection.findPlant(item.getTargetName());

            if (plant == null) {
                plant = new PlantData(item.getTargetName(), item.getPrice(), true);
            }

            collection.unlockPlant(plant);
            return true;
        }

        if (item.getType().equals("seed_packet")) {
            if (collection == null || plantType == null || plantType.isBlank()) {
                return false;
            }

            PlantData plant = collection.findPlant(plantType);

            if (plant == null) {
                plant = new PlantData(plantType, 0, false);
                collection.addPlant(plant);
            }

            plant.addSeedPackets(item.getAmount() * count);
            return true;
        }

        if (item.getType().equals("plant_food")) {
            if (collection == null || plantType == null || plantType.isBlank()) {
                return false;
            }

            PlantData plant = collection.findOwnedPlant(plantType);

            if (plant == null) {
                return false;
            }

            plant.addBoost(item.getAmount() * count);
            return true;
        }

        if (item.getType().equals("pot")) {
            Greenhouse greenhouse = getOrCreateGreenhouse(user);

            if (greenhouse == null) {
                return false;
            }

            int unlockedCount = 0;

            for (Greenhouse.Pot pot : greenhouse.getAllPots()) {
                if (unlockedCount >= count) {
                    break;
                }

                if (pot.isLocked()) {
                    pot.unlock();
                    unlockedCount++;
                }
            }

            return unlockedCount == count;
        }

        if (item.getType().equals("coin_pack")) {
            addCoins(user, item.getAmount() * count);
            return true;
        }

        if (item.getType().equals("gem_pack")) {
            addGems(user, item.getAmount() * count);
            return true;
        }

        return false;
    }

    private void initializePermanentItems() {
        permanentItems.add(new ShopItem("plant_peashooter", "Peashooter", "plant", "Peashooter", 100, "coin", 1, false));
        permanentItems.add(new ShopItem("plant_sunflower", "Sunflower", "plant", "Sunflower", 50, "coin", 1, false));
        permanentItems.add(new ShopItem("plant_wallnut", "Wall-nut", "plant", "Wall-nut", 50, "coin", 1, false));
        permanentItems.add(new ShopItem("seed_packet", "Seed Packet", "seed_packet", "", 25, "coin", 10, false));
        permanentItems.add(new ShopItem("plant_food", "Plant Food", "plant_food", "", 75, "coin", 1, false));
        permanentItems.add(new ShopItem("greenhouse_pot", "Greenhouse Pot", "pot", "", 100, "coin", 1, false));
    }

    private void initializeDailyItems() {
        dailyItems.add(new ShopItem("daily_seed_packet", "Daily Seed Packet", "seed_packet", "", 10, "coin", 5, true));
        dailyItems.add(new ShopItem("daily_plant_food", "Daily Plant Food", "plant_food", "", 40, "coin", 1, true));
        dailyItems.add(new ShopItem("daily_greenhouse_pot", "Daily Greenhouse Pot", "pot", "", 75, "coin", 1, true));
    }

    private boolean spendCoins(User user, int amount) {
        return spendCurrency(user, "coin", amount);
    }

    private boolean spendCurrency(User user, String currency, int amount) {
        if (amount < 0) {
            return false;
        }

        String fieldName = resolveCurrencyFieldName(currency);
        Integer currentAmount = getIntField(user, fieldName);

        if (currentAmount == null || currentAmount < amount) {
            return false;
        }

        return setIntField(user, fieldName, currentAmount - amount);
    }

    private void addCurrency(User user, String currency, int amount) {
        if (currency.equals("gem") || currency.equals("diamond")) {
            addGems(user, amount);
            return;
        }

        addCoins(user, amount);
    }

    private void addCoins(User user, int amount) {
        if (amount <= 0) {
            return;
        }

        Integer currentCoins = getIntField(user, "coins");

        if (currentCoins == null) {
            return;
        }

        setIntField(user, "coins", currentCoins + amount);
    }

    private void addGems(User user, int amount) {
        if (amount <= 0) {
            return;
        }

        Integer currentGems = getIntField(user, "gems");

        if (currentGems == null) {
            return;
        }

        setIntField(user, "gems", currentGems + amount);
    }

    private String resolveCurrencyFieldName(String currency) {
        String normalizedCurrency = normalize(currency);

        if (normalizedCurrency.equals("gem") || normalizedCurrency.equals("diamond")) {
            return "gems";
        }

        return "coins";
    }

    private Collection getOrCreateCollection(User user) {
        Object value = getObjectField(user, "collection");

        if (value instanceof Collection collection) {
            return collection;
        }

        Collection collection = new Collection();

        if (setObjectField(user, "collection", collection)) {
            return collection;
        }

        return null;
    }

    private Greenhouse getOrCreateGreenhouse(User user) {
        Object value = getObjectField(user, "greenhouse");

        if (value instanceof Greenhouse greenhouse) {
            return greenhouse;
        }

        Greenhouse greenhouse = new Greenhouse();

        if (setObjectField(user, "greenhouse", greenhouse)) {
            return greenhouse;
        }

        return null;
    }

    private Integer getIntField(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);

            if (field == null) {
                return null;
            }

            field.setAccessible(true);
            return field.getInt(target);
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    private boolean setIntField(Object target, String fieldName, int value) {
        try {
            Field field = findField(target.getClass(), fieldName);

            if (field == null) {
                return false;
            }

            field.setAccessible(true);
            field.setInt(target, Math.max(0, value));
            return true;
        } catch (IllegalAccessException exception) {
            return false;
        }
    }

    private Object getObjectField(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);

            if (field == null) {
                return null;
            }

            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    private boolean setObjectField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);

            if (field == null) {
                return false;
            }

            field.setAccessible(true);
            field.set(target, value);
            return true;
        } catch (IllegalAccessException exception) {
            return false;
        }
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;

        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException exception) {
                current = current.getSuperclass();
            }
        }

        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }

    public static class ShopItem implements IPurchasable {
        private final String id;
        private final String name;
        private final String type;
        private final String targetName;
        private final int price;
        private final String currency;
        private final int amount;
        private final boolean daily;

        public ShopItem(String id, String name, String type, String targetName, int price, String currency, int amount, boolean daily) {
            this.id = normalizeStatic(id);
            this.name = normalizeStatic(name);
            this.type = normalizeStatic(type);
            this.targetName = normalizeStatic(targetName);
            this.price = Math.max(0, price);
            this.currency = normalizeCurrency(currency);
            this.amount = Math.max(1, amount);
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

        private static String normalizeStatic(String value) {
            if (value == null) {
                return "";
            }

            return value.trim();
        }

        private static String normalizeCurrency(String value) {
            String normalized = normalizeStatic(value).toLowerCase(Locale.ROOT);

            if (normalized.equals("diamond")) {
                return "gem";
            }

            if (normalized.isEmpty()) {
                return "coin";
            }

            return normalized;
        }
    }
}