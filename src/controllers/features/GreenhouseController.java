package controllers.features;

import models.account.Greenhouse;
import models.account.PlantData;

public class GreenhouseController {
    private String lastMessage;
    private int lastHarvestAmount;

    public GreenhouseController() {
        this.lastMessage = "";
        this.lastHarvestAmount = 0;
    }

    public void plantSeed(Greenhouse greenhouse, PlantData plant, int x, int y) {
        if (greenhouse == null) {
            fail("Greenhouse is not available.");
            return;
        }

        if (plant == null) {
            fail("Plant is not available.");
            return;
        }

        boolean planted = greenhouse.plantSeed(plant, x, y);

        if (!planted) {
            fail("Seed could not be planted at (" + x + ", " + y + ").");
            return;
        }

        success("Seed planted at (" + x + ", " + y + ").");
    }

    public void plantMarigold(Greenhouse greenhouse, int x, int y) {
        if (greenhouse == null) {
            fail("Greenhouse is not available.");
            return;
        }

        boolean planted = greenhouse.plantMarigold(x, y);

        if (!planted) {
            fail("Marigold could not be planted at (" + x + ", " + y + ").");
            return;
        }

        success("Marigold planted at (" + x + ", " + y + ").");
    }

    public int performHarvest(Greenhouse greenhouse, int x, int y) {
        if (greenhouse == null) {
            fail("Greenhouse is not available.");
            return 0;
        }

        int reward = greenhouse.harvest(x, y);
        lastHarvestAmount = reward;

        if (reward <= 0) {
            fail("No plant is ready to harvest at (" + x + ", " + y + ").");
            return 0;
        }

        success("Harvest completed at (" + x + ", " + y + "). +" + reward + " coins.");
        return reward;
    }

    public int performHarvest(Greenhouse greenhouse) {
        if (greenhouse == null) {
            fail("Greenhouse is not available.");
            return 0;
        }

        int reward = greenhouse.harvestAllReadyPots();
        lastHarvestAmount = reward;

        if (reward <= 0) {
            fail("No plants are ready to harvest.");
            return 0;
        }

        success("Harvest completed. +" + reward + " coins.");
        return reward;
    }

    public void grow(Greenhouse greenhouse, int x, int y) {
        if (greenhouse == null) {
            fail("Greenhouse is not available.");
            return;
        }

        boolean grown = greenhouse.grow(x, y);

        if (!grown) {
            fail("Pot at (" + x + ", " + y + ") could not be grown.");
            return;
        }

        success("Pot at (" + x + ", " + y + ") is ready.");
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public int getLastHarvestAmount() {
        return lastHarvestAmount;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
        lastHarvestAmount = 0;
    }
}