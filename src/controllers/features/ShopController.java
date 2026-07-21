package controllers.features;

import controllers.auth.AuthController;
import models.account.Collection;
import models.account.Greenhouse;
import models.account.IPurchasable;
import models.account.PlantData;
import models.account.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ShopController {
    private static final int POT_PRICE = 2000;
    private static final int PLANT_FOOD_PRICE = 3;
    private static final int RANDOM_SEED_PRICE = 1000;
    private static final int SELECTED_SEED_PRICE = 5;
    private static final int EXCHANGE_PRICE = 5;
    private static final int DAILY_SEED_PRICE = 1600;

    private final AuthController authController;
    private final Random random;
    private final List<ShopItem> permanentItems;
    private boolean insideShop;
    private String lastMessage;

    public ShopController() {
        this(null);
    }

    public ShopController(AuthController authController) {
        this.authController = authController;
        random = new Random();
        permanentItems = createPermanentItems();
        insideShop = false;
        lastMessage = "";
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
        List<ShopItem> items = getPermanentItems();
        items.addAll(getDailyItems());
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
                return item;
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
            if (plantData.isUnlocked() || user.getCollection().hasOwnedPlant(plantData.getName())) {
                fail("Item is already unlocked.");
                return false;
            }
            int totalPrice = Math.max(0, plantData.getPrice()) * count;
            if (count != 1) {
                fail("A plant can only be unlocked once.");
                return false;
            }
            if (!user.spendCoins(totalPrice)) {
                fail("Not enough coins.");
                return false;
            }
            plantData.unlock();
            user.getCollection().unlockPlant(plantData);
            user.addNews(models.account.News.plantUnlocked(plantData.getName()));
            saveUsers();
            success("Purchase completed.");
            return true;
        }
        fail("Unsupported purchasable item.");
        return false;
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

        int totalPrice = item.getPrice() * count;
        if (!spendCurrency(user, item.getCurrency(), totalPrice)) {
            fail("Not enough " + item.getCurrency() + "s.");
            return false;
        }
        if (!applyPurchasedItem(user, item, count, plantType)) {
            refundCurrency(user, item.getCurrency(), totalPrice);
            fail("Purchase could not be applied.");
            return false;
        }

        if (item.isDaily()) {
            user.getCollection().markDailyOfferPurchased();
        }
        saveUsers();
        success("Purchased " + count + "x " + item.getName() + ".");
        return true;
    }

    public User getCurrentUser() {
        return authController == null ? null : authController.getLoggedInUser();
    }

    public AuthController getAuthController() {
        return authController;
    }

    public boolean isLoggedIn() {
        return authController != null && authController.isLoggedIn();
    }

    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private boolean validatePurchase(User user, ShopItem item, int count, String plantType) {
        Collection collection = user.getCollection();
        if ("pot".equals(item.getType()) && user.getGreenhouse().getLockedPotCount() < count) {
            fail("Greenhouse pot capacity is full.");
            return false;
        }
        if ("plant_food".equals(item.getType())
                && collection.getRemainingPlantFoodCapacity() < count) {
            fail("Plant Food storage is full.");
            return false;
        }
        if ("selected_seed_packet".equals(item.getType())
                && collection.findOwnedPlant(plantType) == null) {
            fail("The selected plant is not unlocked.");
            return false;
        }
        if (("random_seed_packet".equals(item.getType()) || item.isDaily())
                && collection.getOwnedPlants().isEmpty()) {
            fail("No unlocked plant is available for seed packets.");
            return false;
        }
        if (item.isDaily() && collection.isDailyOfferPurchased()) {
            fail("Today's offer has already been purchased.");
            return false;
        }
        return true;
    }

    private boolean applyPurchasedItem(User user, ShopItem item, int count, String plantType) {
        String type = item.getType();
        if ("pot".equals(type)) {
            return unlockPots(user.getGreenhouse(), count);
        }
        if ("plant_food".equals(type)) {
            return user.getCollection().addStoredPlantFood(count);
        }
        if ("random_seed_packet".equals(type)) {
            return addRandomSeeds(user.getCollection(), item.getAmount() * count);
        }
        if ("selected_seed_packet".equals(type)) {
            return addSelectedSeeds(user.getCollection(), plantType, item.getAmount() * count);
        }
        if ("currency_exchange".equals(type)) {
            user.addCoins(item.getAmount() * count);
            return true;
        }
        if ("daily_seed_packet".equals(type)) {
            return addSelectedSeeds(user.getCollection(), item.getTargetName(), item.getAmount());
        }
        return false;
    }

    private boolean unlockPots(Greenhouse greenhouse, int count) {
        for (int index = 0; index < count; index++) {
            if (!greenhouse.unlockNextPot()) {
                return false;
            }
        }
        return true;
    }

    private boolean addRandomSeeds(Collection collection, int amount) {
        List<PlantData> plants = collection.getOwnedPlants();
        if (plants.isEmpty()) {
            return false;
        }
        PlantData plant = plants.get(random.nextInt(plants.size()));
        plant.addSeedPackets(amount);
        return true;
    }

    private boolean addSelectedSeeds(Collection collection, String plantName, int amount) {
        PlantData plant = collection.findOwnedPlant(plantName);
        if (plant == null) {
            return false;
        }
        plant.addSeedPackets(amount);
        return true;
    }

    private ShopItem createDailyItem(User user) {
        if (user == null) {
            return null;
        }

        Collection collection = user.getCollection();
        LocalDate today = LocalDate.now();
        if (!today.toString().equals(collection.getDailyOfferDate())) {
            collection.refreshDailyOffer(selectDailyPlant(collection), today);
            saveUsers();
        }
        if (collection.getDailyOfferPlantName().isBlank()) {
            return null;
        }
        return new ShopItem(
                "daily_seed_packet",
                "Daily Seed Packet",
                "daily_seed_packet",
                collection.getDailyOfferPlantName(),
                DAILY_SEED_PRICE,
                "coin",
                10,
                true
        );
    }

    private String selectDailyPlant(Collection collection) {
        List<PlantData> plants = collection.getOwnedPlants();
        if (plants.isEmpty()) {
            return "";
        }
        int index = Math.floorMod(LocalDate.now().toString().hashCode(), plants.size());
        return plants.get(index).getName();
    }

    private List<ShopItem> createPermanentItems() {
        List<ShopItem> items = new ArrayList<>();
        items.add(new ShopItem("pot", "Greenhouse Pot", "pot", "", POT_PRICE, "coin", 1, false));
        items.add(new ShopItem(
                "plant_food", "Plant Food", "plant_food", "", PLANT_FOOD_PRICE, "gem", 1, false
        ));
        items.add(new ShopItem(
                "random_seed_packet", "Random Seed Packet", "random_seed_packet", "",
                RANDOM_SEED_PRICE, "coin", 5, false
        ));
        items.add(new ShopItem(
                "selected_seed_packet", "Selected Seed Packet", "selected_seed_packet", "",
                SELECTED_SEED_PRICE, "gem", 10, false
        ));
        items.add(new ShopItem(
                "currency_exchange", "Currency Exchange", "currency_exchange", "",
                EXCHANGE_PRICE, "gem", 500, false
        ));
        return items;
    }

    private boolean spendCurrency(User user, String currency, int amount) {
        return "gem".equals(currency) ? user.spendGems(amount) : user.spendCoins(amount);
    }

    private void refundCurrency(User user, String currency, int amount) {
        if ("gem".equals(currency)) {
            user.addGems(amount);
        } else {
            user.addCoins(amount);
        }
    }

    private User getLoggedInUserOrFail() {
        User user = getCurrentUser();
        if (user == null) {
            fail("No user is logged in.");
        }
        return user;
    }

    private void saveUsers() {
        if (authController != null) {
            authController.saveUsers();
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
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
            this.id = safeText(id);
            this.name = safeText(name);
            this.type = safeText(type);
            this.targetName = safeText(targetName);
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

        private static String safeText(String value) {
            return value == null ? "" : value.trim();
        }

        private static String normalizeCurrency(String value) {
            String normalized = safeText(value).toLowerCase(Locale.ROOT);
            return "diamond".equals(normalized) ? "gem" : normalized;
        }
    }
}
