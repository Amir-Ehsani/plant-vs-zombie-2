package controllers.features;

import controllers.auth.AuthController;
import models.account.Collection;
import models.account.Greenhouse;
import models.account.PlantData;
import models.account.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

abstract class ShopControllerBase {
    protected static final int POT_PRICE = 2000;
    protected static final int PLANT_FOOD_PRICE = 3;
    protected static final int RANDOM_SEED_PRICE = 1000;
    protected static final int SELECTED_SEED_PRICE = 5;
    protected static final int EXCHANGE_PRICE = 5;
    protected static final int DAILY_SEED_PRICE = 1600;
    protected static final int POT_UNIT_AMOUNT = 1;
    protected static final int PLANT_FOOD_UNIT_AMOUNT = 1;
    protected static final int RANDOM_SEED_UNIT_AMOUNT = 5;
    protected static final int SELECTED_SEED_UNIT_AMOUNT = 10;
    protected static final int EXCHANGE_UNIT_AMOUNT = 500;
    protected static final int DAILY_SEED_UNIT_AMOUNT = 10;
    protected static final int POT_STOCK = 15;
    protected static final int PLANT_FOOD_STOCK = 3;
    protected static final int RANDOM_SEED_STOCK = 5;
    protected static final int SELECTED_SEED_STOCK = 10;
    protected static final int EXCHANGE_STOCK = 500;
    protected static final int DAILY_SEED_STOCK = 1;

    protected final AuthController authController;
    protected final Random random;
    protected final List<ShopController.ShopItem> permanentItems;
    protected boolean insideShop;
    protected String lastGrantedSeedPlantName;
    protected String lastMessage;

    protected ShopControllerBase() {
        this(null);
    }

    protected ShopControllerBase(AuthController authController) {
        this.authController = authController;
        random = new Random();
        permanentItems = createPermanentItems();
        insideShop = false;
        lastGrantedSeedPlantName = "";
        lastMessage = "";
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

    protected boolean purchasePlant(User user, PlantData plantData, int count) {
        if (plantData.isUnlocked() || user.getCollection().hasOwnedPlant(plantData.getName())) {
            fail("Item is already unlocked.");
            return false;
        }
        if (count != 1) {
            fail("A plant can only be unlocked once.");
            return false;
        }
        int totalPrice = Math.max(0, plantData.getPrice());
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

    protected boolean validatePurchase(User user, ShopController.ShopItem item, int count, String plantType) {
        Collection collection = user.getCollection();
        if (item.isDaily() && collection.isDailyOfferPurchased()) {
            fail("Today's offer has already been purchased.");
            return false;
        }
        if (item.getAmount() <= 0) {
            fail("Item is out of stock.");
            return false;
        }
        if (count > item.getAmount()) {
            fail("Only " + item.getAmount() + " item(s) are available.");
            return false;
        }
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
        return true;
    }

    protected boolean applyPurchasedItem(User user, ShopController.ShopItem item, int count, String plantType) {
        int receivedAmount = item.getUnitAmount() * count;
        String type = item.getType();
        if ("pot".equals(type)) {
            return unlockPots(user.getGreenhouse(), count);
        }
        if ("plant_food".equals(type)) {
            return user.getCollection().addStoredPlantFood(receivedAmount);
        }
        if ("random_seed_packet".equals(type)) {
            lastGrantedSeedPlantName = "";
            return addRandomSeeds(user.getCollection(), receivedAmount);
        }
        if ("selected_seed_packet".equals(type)) {
            return addSelectedSeeds(user.getCollection(), plantType, receivedAmount);
        }
        if ("currency_exchange".equals(type)) {
            user.addCoins(receivedAmount);
            return true;
        }
        if ("daily_seed_packet".equals(type)) {
            return addSelectedSeeds(user.getCollection(), item.getTargetName(), receivedAmount);
        }
        return false;
    }

    protected boolean consumeStock(User user, ShopController.ShopItem item, int count) {
        if (item.isDaily()) {
            return !user.getCollection().isDailyOfferPurchased() && item.getAmount() >= count;
        }
        return user.getCollection().reduceShopItemAmount(
                item.getId(), count, item.getDefaultAmount()
        );
    }

    protected void restoreStock(User user, ShopController.ShopItem item, int count) {
        if (!item.isDaily()) {
            user.getCollection().restoreShopItemAmount(
                    item.getId(), count, item.getDefaultAmount()
            );
        }
    }

    protected int getRemainingAmount(User user, ShopController.ShopItem item) {
        if (item.isDaily()) {
            return user.getCollection().isDailyOfferPurchased() ? 0 : DAILY_SEED_STOCK;
        }
        return resolveItemAmount(user, item).getAmount();
    }

    protected int calculateTotalPrice(ShopController.ShopItem item, int count) {
        long totalPrice = (long) item.getPrice() * count;
        if (totalPrice > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) totalPrice;
    }

    protected boolean unlockPots(Greenhouse greenhouse, int count) {
        for (int index = 0; index < count; index++) {
            if (!greenhouse.unlockNextPot()) {
                return false;
            }
        }
        return true;
    }

    protected boolean addRandomSeeds(Collection collection, int amount) {
        List<PlantData> plants = collection.getOwnedPlants();
        if (plants.isEmpty()) {
            return false;
        }
        PlantData plant = plants.get(random.nextInt(plants.size()));
        plant.addSeedPackets(amount);
        lastGrantedSeedPlantName = plant.getName();
        return true;
    }

    protected boolean addSelectedSeeds(Collection collection, String plantName, int amount) {
        PlantData plant = collection.findOwnedPlant(plantName);
        if (plant == null) {
            return false;
        }
        plant.addSeedPackets(amount);
        return true;
    }

    protected ShopController.ShopItem createDailyItem(User user) {
        if (user == null) {
            return null;
        }
        Collection collection = user.getCollection();
        LocalDate today = LocalDate.now();
        if (!today.toString().equals(collection.getDailyOfferDate())
                || collection.getDailyOfferPlantName().isBlank()
                || collection.findOwnedPlant(collection.getDailyOfferPlantName()) == null) {
            collection.refreshDailyOffer(selectDailyPlant(user, collection, today), today);
            saveUsers();
        }
        if (collection.getDailyOfferPlantName().isBlank()) {
            return null;
        }
        int amount = collection.isDailyOfferPurchased() ? 0 : DAILY_SEED_STOCK;
        return new ShopController.ShopItem(
                "daily_seed_packet",
                "Daily Seed Packet",
                "daily_seed_packet",
                collection.getDailyOfferPlantName(),
                DAILY_SEED_PRICE,
                "coin",
                DAILY_SEED_UNIT_AMOUNT,
                DAILY_SEED_STOCK,
                amount,
                true
        );
    }

    protected String selectDailyPlant(User user, Collection collection, LocalDate date) {
        List<PlantData> plants = new ArrayList<>(collection.getOwnedPlants());
        if (plants.isEmpty()) {
            return "";
        }
        plants.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        String username = user == null || user.getUsername() == null ? "" : user.getUsername();
        int index = Math.floorMod((username + "|" + date).hashCode(), plants.size());
        return plants.get(index).getName();
    }

    protected List<ShopController.ShopItem> createPermanentItems() {
        List<ShopController.ShopItem> items = new ArrayList<>();
        items.add(new ShopController.ShopItem(
                "pot", "Greenhouse Pot", "pot", "", POT_PRICE, "coin",
                POT_UNIT_AMOUNT, POT_STOCK, POT_STOCK, false
        ));
        items.add(new ShopController.ShopItem(
                "plant_food", "Plant Food", "plant_food", "", PLANT_FOOD_PRICE, "gem",
                PLANT_FOOD_UNIT_AMOUNT, PLANT_FOOD_STOCK, PLANT_FOOD_STOCK, false
        ));
        items.add(new ShopController.ShopItem(
                "random_seed_packet", "Random Seed Packet", "random_seed_packet", "",
                RANDOM_SEED_PRICE, "coin", RANDOM_SEED_UNIT_AMOUNT,
                RANDOM_SEED_STOCK, RANDOM_SEED_STOCK, false
        ));
        items.add(new ShopController.ShopItem(
                "selected_seed_packet", "Selected Seed Packet", "selected_seed_packet", "",
                SELECTED_SEED_PRICE, "gem", SELECTED_SEED_UNIT_AMOUNT,
                SELECTED_SEED_STOCK, SELECTED_SEED_STOCK, false
        ));
        items.add(new ShopController.ShopItem(
                "currency_exchange", "Currency Exchange", "currency_exchange", "",
                EXCHANGE_PRICE, "gem", EXCHANGE_UNIT_AMOUNT,
                EXCHANGE_STOCK, EXCHANGE_STOCK, false
        ));
        return items;
    }

    protected ShopController.ShopItem resolveItemAmount(User user, ShopController.ShopItem item) {
        if (item.isDaily() || user == null) {
            return item.copyWithAmount(item.getAmount());
        }
        int amount = user.getCollection().getShopItemAmount(
                item.getId(), item.getDefaultAmount()
        );
        if ("pot".equals(item.getType())) {
            amount = Math.min(amount, user.getGreenhouse().getLockedPotCount());
        } else if ("plant_food".equals(item.getType())) {
            amount = Math.min(amount, user.getCollection().getRemainingPlantFoodCapacity());
        }
        return item.copyWithAmount(amount);
    }

    protected boolean spendCurrency(User user, String currency, int amount) {
        return "gem".equals(currency) ? user.spendGems(amount) : user.spendCoins(amount);
    }

    protected void refundCurrency(User user, String currency, int amount) {
        if ("gem".equals(currency)) {
            user.addGems(amount);
        } else {
            user.addCoins(amount);
        }
    }

    protected User getLoggedInUserOrFail() {
        User user = getCurrentUser();
        if (user == null) {
            fail("No user is logged in.");
        }
        return user;
    }

    protected void saveUsers() {
        if (authController != null) {
            authController.saveUsers();
        }
    }

    protected String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    protected void success(String message) {
        lastMessage = "OK: " + message;
    }

    protected void fail(String message) {
        lastMessage = "ERROR: " + message;
    }

}
