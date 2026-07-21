package controllers.features;

import controllers.auth.AuthController;
import models.account.Collection;
import models.account.Greenhouse;
import models.account.PlantData;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GreenhouseController {
    private final AuthController authController;
    private final Random random;
    private String lastMessage;
    private int lastHarvestAmount;

    public GreenhouseController() {
        this(null, new Random());
    }

    public GreenhouseController(AuthController authController) {
        this(authController, new Random());
    }

    GreenhouseController(AuthController authController, Random random) {
        this.authController = authController;
        this.random = random;
        lastMessage = "";
        lastHarvestAmount = 0;
    }

    public Greenhouse getCurrentGreenhouse() {
        User user = getLoggedInUserOrFail();
        return user == null ? null : user.getGreenhouse();
    }

    public void plantRandomAt(int x, int y) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }

        Greenhouse greenhouse = user.getGreenhouse();
        Greenhouse.Pot pot = greenhouse.getPot(x, y);
        if (!validateEmptyPot(pot, x, y)) {
            return;
        }

        PlantData selectedPlant = chooseRandomPlant(user.getCollection());
        boolean plantMarigold = selectedPlant == null || random.nextBoolean();
        boolean planted = plantMarigold
                ? greenhouse.plantMarigold(x, y)
                : greenhouse.plantSeed(selectedPlant, x, y);

        if (!planted) {
            fail("Seed could not be planted at (" + x + ", " + y + ").");
            return;
        }

        String name = plantMarigold ? "Marigold" : selectedPlant.getName();
        saveUsers();
        success(name + " planted at (" + x + ", " + y + ").");
    }

    public void collect(int x, int y) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }
        performHarvest(user.getGreenhouse(), x, y);
    }

    public void growNow(int x, int y) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }

        Greenhouse.Pot pot = user.getGreenhouse().getPot(x, y);
        if (pot == null || pot.isLocked() || pot.isEmpty()) {
            fail("There is no growing plant at (" + x + ", " + y + ").");
            return;
        }
        if (pot.isReady()) {
            fail("Plant is already ready to collect.");
            return;
        }

        int gemCost = Math.max(1, pot.getRemainingHoursRoundedUp());
        if (!user.spendGems(gemCost)) {
            fail("Not enough gems. Required: " + gemCost + ".");
            return;
        }

        pot.forceReady();
        saveUsers();
        success("Plant is ready. " + gemCost + " gems spent.");
    }

    public void plantSeed(Greenhouse greenhouse, PlantData plant, int x, int y) {
        if (greenhouse == null || plant == null) {
            fail("Plant or greenhouse is not available.");
            return;
        }
        if (!greenhouse.plantSeed(plant, x, y)) {
            fail("Seed could not be planted at (" + x + ", " + y + ").");
            return;
        }
        success("Seed planted at (" + x + ", " + y + ").");
    }

    public void plantMarigold(Greenhouse greenhouse, int x, int y) {
        if (greenhouse == null || !greenhouse.plantMarigold(x, y)) {
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
        Greenhouse.Pot pot = greenhouse.getPot(x, y);
        if (pot == null || !pot.isReady()) {
            fail("No plant is ready to harvest at (" + x + ", " + y + ").");
            return 0;
        }

        User user = currentUserFor(greenhouse);
        if (!pot.isMarigold() && !canStoreBoost(user, pot.getPlantName())) {
            fail("The plant boost cannot be stored; the pot was not cleared.");
            return 0;
        }

        Greenhouse.HarvestResult result = greenhouse.collect(x, y);
        if (!result.isSuccessful()) {
            fail("Harvest failed.");
            return 0;
        }
        int reward = applyHarvestResult(user, result);
        lastHarvestAmount = reward;
        saveUsers();
        success(result.isMarigold()
                ? "Harvest completed. +" + reward + " coins."
                : result.getPlantName() + " collected. One stored boost added.");
        return reward;
    }

    public int performHarvest(Greenhouse greenhouse) {
        if (greenhouse == null) {
            fail("Greenhouse is not available.");
            return 0;
        }
        User user = currentUserFor(greenhouse);
        int totalReward = 0;
        int collected = 0;
        for (Greenhouse.Pot pot : greenhouse.getAllPots()) {
            if (!pot.isReady()) {
                continue;
            }
            if (!pot.isMarigold() && !canStoreBoost(user, pot.getPlantName())) {
                continue;
            }
            Greenhouse.HarvestResult result = greenhouse.collect(pot.getX(), pot.getY());
            if (result.isSuccessful()) {
                totalReward += applyHarvestResult(user, result);
                collected++;
            }
        }
        lastHarvestAmount = totalReward;
        if (collected == 0) {
            fail("No ready harvest could be collected.");
            return 0;
        }
        saveUsers();
        success("Harvested " + collected + " pot(s). +" + totalReward + " coins.");
        return totalReward;
    }

    public void grow(Greenhouse greenhouse, int x, int y) {
        if (greenhouse == null || !greenhouse.grow(x, y)) {
            fail("Pot at (" + x + ", " + y + ") could not be grown.");
            return;
        }
        success("Pot at (" + x + ", " + y + ") is ready.");
    }

    public boolean isLoggedIn() {
        return authController != null && authController.isLoggedIn();
    }

    public AuthController getAuthController() {
        return authController;
    }

    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
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

    private PlantData chooseRandomPlant(Collection collection) {
        List<PlantData> eligible = new ArrayList<>();
        for (PlantData plant : collection.getOwnedPlants()) {
            PlantType type = DefaultPlantRegistry.getInstance().getByName(plant.getName());
            if (type != null && type.hasPlantFoodEffect()) {
                eligible.add(plant);
            }
        }
        if (eligible.isEmpty()) {
            return null;
        }
        return eligible.get(random.nextInt(eligible.size()));
    }

    private boolean validateEmptyPot(Greenhouse.Pot pot, int x, int y) {
        if (pot == null) {
            fail("Invalid greenhouse position.");
            return false;
        }
        if (pot.isLocked()) {
            fail("Pot at (" + x + ", " + y + ") is locked.");
            return false;
        }
        if (!pot.isEmpty()) {
            fail("Pot at (" + x + ", " + y + ") is occupied.");
            return false;
        }
        return true;
    }

    private User currentUserFor(Greenhouse greenhouse) {
        if (authController == null) {
            return null;
        }
        User user = authController.getLoggedInUser();
        return user != null && user.getGreenhouse() == greenhouse ? user : null;
    }

    private boolean canStoreBoost(User user, String plantName) {
        if (user == null) {
            return false;
        }
        PlantData plant = user.getCollection().findOwnedPlant(plantName);
        return plant != null && plant.getBoostCount() == 0;
    }

    private int applyHarvestResult(User user, Greenhouse.HarvestResult result) {
        if (result.isMarigold()) {
            if (user != null) {
                user.addCoins(result.getCoinReward());
            }
            return result.getCoinReward();
        }
        if (user != null) {
            PlantData plant = user.getCollection().findOwnedPlant(result.getPlantName());
            if (plant != null) {
                plant.addStoredGreenhouseBoost();
            }
        }
        return 0;
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
        lastHarvestAmount = 0;
    }
}
