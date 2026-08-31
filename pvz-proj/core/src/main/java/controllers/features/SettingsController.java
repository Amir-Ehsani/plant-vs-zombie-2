package controllers.features;

import controllers.auth.AuthController;
import controllers.core.GameController;
import models.account.Collection;
import models.account.PlantData;
import models.account.Settings;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantType;
import models.core.zombie.DefaultZombieRegistry;
import models.core.zombie.ZombieType;
import models.engine.session.GameSession;

public class SettingsController {
    private final AuthController authController;
    private String lastMessage;

    public SettingsController(AuthController authController) {
        this.authController = authController;
        lastMessage = "";
    }

    public Settings getSettings() {
        User user = getLoggedInUserOrFail();
        return user == null ? null : user.getSettings();
    }

    public void changeDifficulty(int difficultyLevel) {
        updateSettings(settings -> settings.setDifficulty(difficultyLevel),
                difficultyLevel >= Settings.MIN_DIFFICULTY && difficultyLevel <= Settings.MAX_DIFFICULTY,
                "Difficulty must be between 1 and 5.",
                "Difficulty changed successfully.");
    }

    public void changeGameSpeed(int gameSpeed) {
        updateSettings(settings -> settings.setGameSpeed(gameSpeed),
                gameSpeed >= Settings.MIN_GAME_SPEED && gameSpeed <= Settings.MAX_GAME_SPEED,
                "Game speed must be between 1 and 3.",
                "Game speed changed successfully.");
    }

    public void setGridVisible(boolean visible) {
        updateSettings(settings -> settings.setGridVisible(visible), true, "", "Grid setting updated.");
    }

    public void setDebugMode(boolean enabled) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }
        if (!enabled) {
            user.disableDebugAdventureUnlockOverride();
            user.disableDebugAllContentUnlocked();
        }
        user.getSettings().setDebugMode(enabled);
        saveAndSucceed("Debug mode updated.");
    }

    public void setMusicVolume(float volume) {
        updateSettings(settings -> settings.setMusicVolume(volume), isValidVolume(volume),
                "Music volume must be between 0 and 1.", "Music volume updated.");
    }

    public void setSoundVolume(float volume) {
        updateSettings(settings -> settings.setSoundVolume(volume), isValidVolume(volume),
                "Sound volume must be between 0 and 1.", "Sound volume updated.");
    }

    public void setMusicEnabled(boolean enabled) {
        updateSettings(settings -> settings.setMusicEnabled(enabled), true, "", "Music setting updated.");
    }

    public void unlockAllAdventureContentForDebug() {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }
        if (!user.getSettings().isDebugMode()) {
            fail("Debug mode is disabled.");
            return;
        }
        user.enableDebugAdventureUnlockOverride();
        success("All adventure chapters and levels are temporarily unlocked for debug mode.");
    }

    public boolean isDebugAdventureUnlockActive() {
        User user = authController.getLoggedInUser();
        return user != null && user.isDebugAdventureUnlockOverride();
    }

    public void unlockAllCollectionContent() {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }
        if (!user.getSettings().isDebugMode()) {
            fail("Debug mode is disabled.");
            return;
        }

        Collection collection = user.getCollection();
        int plantsUnlocked = 0;
        int zombiesUnlocked = 0;

        for (PlantType type : DefaultPlantRegistry.getInstance().getAllPlantTypes()) {
            if (type == null || type.getName() == null || type.getName().isBlank()) {
                continue;
            }
            if (!collection.hasPlant(type.getName())) {
                collection.addPlant(new PlantData(
                        type.getName(),
                        CollectionController.PLANT_PURCHASE_PRICE,
                        true
                ));
            }
            if (collection.unlockPlant(type.getName())) {
                plantsUnlocked++;
            }
        }

        for (ZombieType type : DefaultZombieRegistry.getInstance().getAllZombieTypes()) {
            if (type == null || type.getName() == null || type.getName().isBlank()) {
                continue;
            }
            if (!collection.hasZombie(type.getName())) {
                collection.addZombie(type.getName(), true);
                zombiesUnlocked++;
            } else if (collection.unlockZombie(type.getName())) {
                zombiesUnlocked++;
            }
        }

        user.enableDebugAllContentUnlocked();
        saveAndSucceed("Unlocked all plants and zombies. Plants: "
                + plantsUnlocked + ", zombies: " + zombiesUnlocked + ".");
    }

    public void addDebugCoins(int amount) {
        User user = validateDebugAmount(amount);
        if (user == null) {
            return;
        }
        user.addCoins(amount);
        saveAndSucceed(amount + " coins added.");
    }

    public void addDebugDiamonds(int amount) {
        User user = validateDebugAmount(amount);
        if (user == null) {
            return;
        }
        user.addGems(amount);
        saveAndSucceed(amount + " diamonds added.");
    }

    public void addDebugSun(GameController gameController, int amount) {
        if (!validateDebugController(gameController)) {
            return;
        }
        gameController.addSunCheat(amount);
        copyGameControllerResult(gameController);
    }

    public void addDebugPlantFood(GameController gameController) {
        if (!validateDebugController(gameController)) {
            return;
        }
        gameController.addPlantFoodCheat();
        copyGameControllerResult(gameController);
    }

    public void addDebugSun(GameSession gameSession, int amount) {
        if (!validateDebugSession(gameSession)) {
            return;
        }
        if (amount <= 0) {
            fail("Debug amount must be positive.");
            return;
        }
        gameSession.addSun(amount);
        success(amount + " sun added.");
    }

    public void addDebugPlantFood(GameSession gameSession) {
        if (!validateDebugSession(gameSession)) {
            return;
        }
        if (!gameSession.addPlantFood()) {
            fail("Plant food storage is full.");
            return;
        }
        success("Plant food added.");
    }

    public boolean applyGameSpeed(GameSession gameSession) {
        Settings settings = getSettings();
        if (settings == null || gameSession == null || gameSession.getTickManager() == null) {
            return false;
        }
        gameSession.getTickManager().setSpeedMultiplier(settings.getGameSpeed());
        return true;
    }

    public boolean isLoggedIn() {
        return authController.isLoggedIn();
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
    }

    private void updateSettings(SettingsAction action, boolean valid, String error, String successMessage) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }
        if (!valid) {
            fail(error);
            return;
        }
        action.apply(user.getSettings());
        saveAndSucceed(successMessage);
    }

    private User validateDebugAmount(int amount) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return null;
        }
        if (!user.getSettings().isDebugMode()) {
            fail("Debug mode is disabled.");
            return null;
        }
        if (amount <= 0) {
            fail("Debug amount must be positive.");
            return null;
        }
        return user;
    }

    private boolean validateDebugController(GameController gameController) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return false;
        }
        if (!user.getSettings().isDebugMode()) {
            fail("Debug mode is disabled.");
            return false;
        }
        if (gameController == null) {
            fail("Game controller is not available.");
            return false;
        }
        return true;
    }

    private boolean validateDebugSession(GameSession gameSession) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return false;
        }
        if (!user.getSettings().isDebugMode()) {
            fail("Debug mode is disabled.");
            return false;
        }
        if (gameSession == null || !gameSession.isRunning()) {
            fail("Game session is not available.");
            return false;
        }
        return true;
    }

    private User getLoggedInUserOrFail() {
        User user = authController.getLoggedInUser();
        if (user == null) {
            fail("No user is logged in.");
        }
        return user;
    }

    private boolean isValidVolume(float volume) {
        return volume >= 0f && volume <= 1f;
    }

    private void copyGameControllerResult(GameController gameController) {
        lastMessage = gameController.getLastMessage();
    }

    private void saveAndSucceed(String message) {
        authController.saveUsers();
        success(message);
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }

    private interface SettingsAction {
        void apply(Settings settings);
    }
}
