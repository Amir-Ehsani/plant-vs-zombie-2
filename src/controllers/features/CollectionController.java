package controllers.features;

import models.account.Collection;
import models.account.PlantData;

public class CollectionController {
    private String lastMessage;

    public CollectionController() {
        this.lastMessage = "";
    }

    public String showPlants(Collection collection) {
        if (collection == null) {
            fail("Collection is not available.");
            return "";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Unlocked Plants\n");
        builder.append("===============\n");

        if (collection.getOwnedPlants().isEmpty()) {
            builder.append("No unlocked plants.\n");
            success("Unlocked plants shown.");
            return builder.toString();
        }

        for (PlantData plant : collection.getOwnedPlants()) {
            builder.append(formatPlantLine(plant)).append("\n");
        }

        success("Unlocked plants shown.");
        return builder.toString();
    }

    public String showAllPlants(Collection collection) {
        if (collection == null) {
            fail("Collection is not available.");
            return "";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("All Plants\n");
        builder.append("==========\n");

        if (collection.getAllPlants().isEmpty()) {
            builder.append("No plants available.\n");
            success("All plants shown.");
            return builder.toString();
        }

        for (PlantData plant : collection.getAllPlants()) {
            builder.append(formatPlantLine(plant)).append("\n");
        }

        success("All plants shown.");
        return builder.toString();
    }

    public String showZombies(Collection collection) {
        if (collection == null) {
            fail("Collection is not available.");
            return "";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Unlocked Zombies\n");
        builder.append("================\n");

        if (collection.getOwnedZombies().isEmpty()) {
            builder.append("No unlocked zombies.\n");
            success("Unlocked zombies shown.");
            return builder.toString();
        }

        for (String zombie : collection.getOwnedZombies()) {
            builder.append("- ").append(zombie).append("\n");
        }

        success("Unlocked zombies shown.");
        return builder.toString();
    }

    public String showAllZombies(Collection collection) {
        if (collection == null) {
            fail("Collection is not available.");
            return "";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("All Zombies\n");
        builder.append("===========\n");

        if (collection.getAllZombies().isEmpty()) {
            builder.append("No zombies available.\n");
            success("All zombies shown.");
            return builder.toString();
        }

        for (String zombie : collection.getAllZombies()) {
            String status = collection.hasOwnedZombie(zombie) ? "unlocked" : "locked";
            builder.append("- ").append(zombie).append(" [").append(status).append("]\n");
        }

        success("All zombies shown.");
        return builder.toString();
    }

    public String showPlant(Collection collection, String plantName) {
        if (collection == null) {
            fail("Collection is not available.");
            return "";
        }

        PlantData plant = collection.findPlant(plantName);

        if (plant == null) {
            fail("Plant was not found.");
            return "Plant was not found.\n";
        }

        success("Plant shown.");
        return renderPlantDetails(plant);
    }

    public String showZombie(Collection collection, String zombieName) {
        if (collection == null) {
            fail("Collection is not available.");
            return "";
        }

        if (!collection.hasZombie(zombieName)) {
            fail("Zombie was not found.");
            return "Zombie was not found.\n";
        }

        String status = collection.hasOwnedZombie(zombieName) ? "unlocked" : "locked";

        StringBuilder builder = new StringBuilder();

        builder.append("Zombie Details\n");
        builder.append("==============\n");
        builder.append("Name: ").append(zombieName).append("\n");
        builder.append("Status: ").append(status).append("\n");

        success("Zombie shown.");
        return builder.toString();
    }

    public boolean upgradePlant(Collection collection, String plantName) {
        if (collection == null) {
            fail("Collection is not available.");
            return false;
        }

        PlantData plant = collection.findOwnedPlant(plantName);

        if (plant == null) {
            fail("Plant is not unlocked.");
            return false;
        }

        boolean upgraded = collection.upgradePlant(plantName);

        if (!upgraded) {
            fail("Plant could not be upgraded.");
            return false;
        }

        success("Plant upgraded.");
        return true;
    }

    public boolean purchasePlant(Collection collection, PlantData plant) {
        if (collection == null) {
            fail("Collection is not available.");
            return false;
        }

        if (plant == null) {
            fail("Plant is not available.");
            return false;
        }

        collection.unlockPlant(plant);
        success("Plant purchased.");
        return true;
    }

    public boolean purchasePlant(Collection collection, String plantName, int price) {
        if (collection == null) {
            fail("Collection is not available.");
            return false;
        }

        if (plantName == null || plantName.isBlank()) {
            fail("Plant name is empty.");
            return false;
        }

        PlantData plant = collection.findPlant(plantName);

        if (plant == null) {
            plant = new PlantData(plantName, price, true);
        }

        collection.unlockPlant(plant);
        success("Plant purchased.");
        return true;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private String formatPlantLine(PlantData plant) {
        if (plant == null) {
            return "- Unknown";
        }

        String status = plant.isUnlocked() ? "unlocked" : "locked";

        return "- "
                + plant.getName()
                + " ["
                + status
                + "] level="
                + plant.getLevel()
                + " seeds="
                + plant.getSeedPackets()
                + " boosts="
                + plant.getBoostCount()
                + " price="
                + plant.getPrice();
    }

    private String renderPlantDetails(PlantData plant) {
        StringBuilder builder = new StringBuilder();

        builder.append("Plant Details\n");
        builder.append("=============\n");
        builder.append("Name: ").append(plant.getName()).append("\n");
        builder.append("Status: ").append(plant.isUnlocked() ? "unlocked" : "locked").append("\n");
        builder.append("Level: ").append(plant.getLevel()).append("\n");
        builder.append("Price: ").append(plant.getPrice()).append("\n");
        builder.append("Seed packets: ").append(plant.getSeedPackets()).append("\n");
        builder.append("Required seeds for next level: ").append(plant.getRequiredSeedPacketsForNextLevel()).append("\n");
        builder.append("Boosts: ").append(plant.getBoostCount()).append("\n");
        builder.append("Can upgrade: ").append(plant.canUpgrade() ? "yes" : "no").append("\n");

        return builder.toString();
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }
}