package controllers.features;

import controllers.auth.AuthController;
import models.account.Collection;
import models.account.News;
import models.account.PlantData;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.core.zombie.DefaultZombieRegistry;
import models.core.zombie.ZombieRegistry;
import models.core.zombie.ZombieType;

import java.util.Arrays;
import java.util.List;

public class CollectionController {
    public static final int PLANT_PURCHASE_PRICE = 2000;

    private static final List<String> STARTER_PLANTS = Arrays.asList(
            "Sunflower",
            "Peashooter",
            "Wall-nut",
            "Potato Mine",
            "Cabbage-pult",
            "Kernel-pult",
            "Iceberg Lettuce",
            "Bonk Choy",
            "Cherry Bomb"
    );

    private final AuthController authController;
    private final PlantRegistry plantRegistry;
    private final ZombieRegistry zombieRegistry;
    private String lastMessage;

    public CollectionController() {
        this(null);
    }

    public CollectionController(AuthController authController) {
        this.authController = authController;
        plantRegistry = DefaultPlantRegistry.getInstance();
        zombieRegistry = DefaultZombieRegistry.getInstance();
        lastMessage = "";
    }

    public static List<String> getStarterPlantNames() {
        return STARTER_PLANTS;
    }

    public String showPlants() {
        User user = getLoggedInUserOrFail();
        return user == null ? "" : showPlants(user.getCollection());
    }

    public String showAllPlants() {
        User user = getLoggedInUserOrFail();
        return user == null ? "" : showAllPlants(user.getCollection());
    }

    public String showZombies() {
        User user = getLoggedInUserOrFail();
        return user == null ? "" : showZombies(user.getCollection());
    }

    public String showAllZombies() {
        User user = getLoggedInUserOrFail();
        return user == null ? "" : showAllZombies(user.getCollection());
    }

    public String showPlant(String plantName) {
        User user = getLoggedInUserOrFail();
        return user == null ? "" : showPlant(user.getCollection(), plantName);
    }

    public String showZombie(String zombieName) {
        User user = getLoggedInUserOrFail();
        return user == null ? "" : showZombie(user.getCollection(), zombieName);
    }

    public boolean upgradePlant(String plantName) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return false;
        }

        Collection collection = prepareCollection(user.getCollection());
        PlantData plant = collection.findOwnedPlant(plantName);

        if (!validateUpgrade(user, plant)) {
            return false;
        }

        int price = plant.getUpgradePrice();
        user.spendCoins(price);

        if (!plant.upgrade()) {
            user.addCoins(price);
            fail("Plant could not be upgraded.");
            return false;
        }

        saveUsers();
        success(plant.getName() + " upgraded to level " + plant.getLevel() + ".");
        return true;
    }

    public boolean purchasePlant(String plantName) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return false;
        }

        PlantType type = plantRegistry.getByName(plantName);

        if (type == null) {
            fail("Plant was not found.");
            return false;
        }

        Collection collection = prepareCollection(user.getCollection());

        if (collection.hasOwnedPlant(type.getName())) {
            fail("Plant is already unlocked.");
            return false;
        }

        if (!user.spendCoins(PLANT_PURCHASE_PRICE)) {
            fail("Not enough coins. Required: " + PLANT_PURCHASE_PRICE + ".");
            return false;
        }

        collection.unlockPlant(type.getName());
        user.addNews(News.plantUnlocked(type.getName()));
        saveUsers();
        success(type.getName() + " purchased for " + PLANT_PURCHASE_PRICE + " coins.");
        return true;
    }

    public String showPlants(Collection collection) {
        Collection prepared = prepareCollection(collection);

        if (prepared == null) {
            return "";
        }

        StringBuilder builder = header("Unlocked Plants");
        List<PlantData> plants = prepared.getOwnedPlants();

        if (plants.isEmpty()) {
            builder.append("No unlocked plants.\n");
        } else {
            appendPlants(builder, plants);
        }

        success("Unlocked plants shown.");
        return builder.toString();
    }

    public String showAllPlants(Collection collection) {
        Collection prepared = prepareCollection(collection);

        if (prepared == null) {
            return "";
        }

        StringBuilder builder = header("All Plants");
        appendPlants(builder, prepared.getAllPlants());
        success("All plants shown.");
        return builder.toString();
    }

    public String showZombies(Collection collection) {
        Collection prepared = prepareCollection(collection);

        if (prepared == null) {
            return "";
        }

        StringBuilder builder = header("Discovered Zombies");
        List<String> zombies = prepared.getOwnedZombies();

        if (zombies.isEmpty()) {
            builder.append("No discovered zombies.\n");
        } else {
            for (String zombie : zombies) {
                builder.append("- ").append(zombie).append("\n");
            }
        }

        success("Discovered zombies shown.");
        return builder.toString();
    }

    public String showAllZombies(Collection collection) {
        Collection prepared = prepareCollection(collection);

        if (prepared == null) {
            return "";
        }

        StringBuilder builder = header("All Zombies");

        for (ZombieType type : zombieRegistry.getAllZombieTypes()) {
            String status = prepared.hasOwnedZombie(type.getName()) ? "discovered" : "unknown";
            builder.append("- ").append(type.getName()).append(" [").append(status).append("]\n");
        }

        success("All zombies shown.");
        return builder.toString();
    }

    public String showPlant(Collection collection, String plantName) {
        Collection prepared = prepareCollection(collection);

        if (prepared == null) {
            return "";
        }

        PlantType type = plantRegistry.getByName(plantName);
        PlantData data = prepared.findPlant(plantName);

        if (type == null || data == null) {
            fail("Plant was not found.");
            return "Plant was not found.\n";
        }

        success("Plant shown.");
        return renderPlantDetails(type, data);
    }

    public String showZombie(Collection collection, String zombieName) {
        Collection prepared = prepareCollection(collection);

        if (prepared == null) {
            return "";
        }

        ZombieType type = zombieRegistry.getZombieTypeByName(zombieName);

        if (type == null) {
            fail("Zombie was not found.");
            return "Zombie was not found.\n";
        }

        if (!prepared.hasOwnedZombie(type.getName())) {
            fail("Zombie has not been discovered yet.");
            return "Zombie has not been discovered yet.\n";
        }

        success("Zombie shown.");
        return renderZombieDetails(type, true);
    }

    public boolean upgradePlant(Collection collection, String plantName) {
        Collection prepared = prepareCollection(collection);

        if (prepared == null) {
            return false;
        }

        PlantData plant = prepared.findOwnedPlant(plantName);

        if (plant == null || !plant.upgrade()) {
            fail("Plant could not be upgraded.");
            return false;
        }

        success("Plant upgraded.");
        return true;
    }

    public boolean purchasePlant(Collection collection, PlantData plant) {
        Collection prepared = prepareCollection(collection);

        if (prepared == null || plant == null) {
            fail("Plant is not available.");
            return false;
        }

        prepared.unlockPlant(plant);
        success("Plant purchased.");
        return true;
    }

    public boolean purchasePlant(Collection collection, String plantName, int price) {
        Collection prepared = prepareCollection(collection);

        if (prepared == null || plantName == null || plantName.isBlank()) {
            fail("Plant name is empty.");
            return false;
        }

        PlantData plant = prepared.findPlant(plantName);

        if (plant == null) {
            plant = new PlantData(plantName, price, true);
        }

        prepared.unlockPlant(plant);
        success("Plant purchased.");
        return true;
    }

    public void ensureStarterPlantsForUser(User user) {
        if (user == null) {
            return;
        }

        Collection collection = prepareCollection(user.getCollection());

        for (String plantName : STARTER_PLANTS) {
            if (!collection.hasPlant(plantName)) {
                collection.addPlant(new PlantData(plantName, PLANT_PURCHASE_PRICE, true));
            }

            collection.unlockPlant(plantName);
        }
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

    private boolean validateUpgrade(User user, PlantData plant) {
        if (plant == null) {
            fail("Plant is not unlocked.");
            return false;
        }

        if (plant.getLevel() >= 4) {
            fail("Plant is already at maximum level.");
            return false;
        }

        if (plant.getSeedPackets() < plant.getRequiredSeedPacketsForNextLevel()) {
            fail("Not enough seed packets. Required: " + plant.getRequiredSeedPacketsForNextLevel() + ".");
            return false;
        }

        if (user.getCoins() < plant.getUpgradePrice()) {
            fail("Not enough coins. Required: " + plant.getUpgradePrice() + ".");
            return false;
        }

        return true;
    }

    private Collection prepareCollection(Collection collection) {
        if (collection == null) {
            fail("Collection is not available.");
            return null;
        }

        for (PlantType type : plantRegistry.getAllPlantTypes()) {
            if (!collection.hasPlant(type.getName())) {
                boolean starter = STARTER_PLANTS.contains(type.getName());
                collection.addPlant(new PlantData(type.getName(), PLANT_PURCHASE_PRICE, starter));
            }
        }

        if (collection.getOwnedPlants().isEmpty()) {
            for (String plantName : STARTER_PLANTS) {
                if (!collection.hasPlant(plantName)) {
                    collection.addPlant(new PlantData(plantName, PLANT_PURCHASE_PRICE, true));
                }

                collection.unlockPlant(plantName);
            }
        }

        for (ZombieType type : zombieRegistry.getAllZombieTypes()) {
            if (!collection.hasZombie(type.getName())) {
                collection.addZombie(type.getName(), false);
            }
        }

        return collection;
    }

    private void appendPlants(StringBuilder builder, List<PlantData> plants) {
        for (PlantData plant : plants) {
            builder.append(formatPlantLine(plant)).append("\n");
        }
    }

    private String formatPlantLine(PlantData plant) {
        String status = plant.isUnlocked() ? "unlocked" : "locked";
        return "- " + plant.getName() + " [" + status + "] level=" + plant.getLevel()
                + " seeds=" + plant.getSeedPackets() + " boosts=" + plant.getBoostCount();
    }

    private String renderPlantDetails(PlantType type, PlantData data) {
        StringBuilder builder = header("Plant Details");
        builder.append("Name: ").append(type.getName()).append("\n");
        builder.append("Status: ").append(data.isUnlocked() ? "unlocked" : "locked").append("\n");
        builder.append("Level: ").append(data.getLevel()).append("\n");
        builder.append("Purchase price: ").append(PLANT_PURCHASE_PRICE).append(" coins\n");
        builder.append("Seed packets: ").append(data.getSeedPackets()).append("\n");
        appendUpgradeState(builder, data);
        builder.append("Stored boosts: ").append(data.getBoostCount()).append("\n");
        builder.append("Category: ").append(type.getCategory()).append("\n");
        builder.append("Tags: ").append(blankAsDash(type.getTags())).append("\n");
        builder.append("Sun cost: ").append(type.getSunCost()).append("\n");
        builder.append("Base HP: ").append(type.getBaseHp()).append("\n");
        builder.append("Damage: ").append(type.getDamage()).append("\n");
        builder.append("Ability: ").append(blankAsDash(type.getBaseAbility())).append("\n");
        builder.append("Plant food: ").append(blankAsDash(type.getPlantFoodEffect())).append("\n");
        builder.append("Level 2: ").append(blankAsDash(type.getLevel2Upgrade())).append("\n");
        builder.append("Level 3: ").append(blankAsDash(type.getLevel3Upgrade())).append("\n");
        builder.append("Level 4: ").append(blankAsDash(type.getLevel4Upgrade())).append("\n");
        return builder.toString();
    }

    private void appendUpgradeState(StringBuilder builder, PlantData data) {
        if (data.getLevel() >= 4) {
            builder.append("Next upgrade: maximum level\n");
            return;
        }

        builder.append("Next upgrade seeds: ").append(data.getRequiredSeedPacketsForNextLevel()).append("\n");
        builder.append("Next upgrade coins: ").append(data.getUpgradePrice()).append("\n");
    }

    private String renderZombieDetails(ZombieType type, boolean discovered) {
        StringBuilder builder = header("Zombie Details");
        builder.append("Name: ").append(type.getName()).append("\n");
        builder.append("Status: ").append(discovered ? "discovered" : "unknown").append("\n");
        builder.append("Base HP: ").append(type.getBaseHp()).append("\n");
        builder.append("Speed: ").append(type.getSpeed()).append("\n");
        builder.append("Damage per tick: ").append(type.getDamagePerTick()).append("\n");
        builder.append("Wave cost: ").append(type.getWaveCost()).append("\n");
        builder.append("Armor: ").append(blankAsDash(type.getDefaultArmorName())).append("\n");

        String tags = type.getTags().isEmpty() ? "-" : String.join(", ", type.getTags());
        builder.append("Tags: ").append(tags).append("\n");
        builder.append("Ability: ").append(blankAsDash(type.getAbility())).append("\n");
        return builder.toString();
    }

    private StringBuilder header(String title) {
        return new StringBuilder(title).append("\n").append("=".repeat(title.length())).append("\n");
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private User getLoggedInUserOrFail() {
        if (authController == null || authController.getLoggedInUser() == null) {
            fail("No user is logged in.");
            return null;
        }

        return authController.getLoggedInUser();
    }

    private void saveUsers() {
        if (authController != null) {
            authController.saveUsers();
        }
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }
}
