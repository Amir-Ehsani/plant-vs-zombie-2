package controllers.core;

import models.account.PlantData;
import controllers.auth.AuthController;
import models.account.User;
import models.core.plant.PlantType;
import models.engine.board.Position;
import models.level.core.Level;
import models.level.rules.impl.LockedPlantsRule;

abstract class GameControllerSelectionSupport extends GameControllerLevelSupport {
    protected GameControllerSelectionSupport(AuthController authController) {
        super(authController);
    }

    protected int currentPlantSelectionLimit() {
        if (gameSession != null
                && gameSession.getCurrentLevel() != null
                && gameSession.getCurrentLevel().getLevelRule() instanceof LockedPlantsRule) {
            LockedPlantsRule rule = (LockedPlantsRule) gameSession.getCurrentLevel().getLevelRule();
            return rule.getAvailableSelectionSlotCount();
        }
        return PLANT_SELECTION_LIMIT;
    }

    protected boolean hasPreparedSession() {
        return gameSession != null && gameSession.getCurrentLevel() != null;
    }

    protected boolean isPlantAvailableForCurrentLevel(String plantName) {
        return isPlantAllowedInCurrentLevel(plantName) && isPlantUnlockedByUser(plantName);
    }

    protected boolean isPlantAllowedInCurrentLevel(String plantName) {
        if (plantName == null || gameSession == null) {
            return false;
        }
        User user = authController == null ? null : authController.getLoggedInUser();
        if (user != null && user.isDebugAllContentUnlocked() && isPlantUnlockedByUser(plantName)) {
            return true;
        }

        Level level = gameSession.getCurrentLevel();
        return level == null || level.isPlantAllowed(plantName);
    }

    protected boolean isPlantUnlockedByUser(String plantName) {
        if (plantName == null || authController == null || authController.getLoggedInUser() == null) {
            return false;
        }

        return authController.getLoggedInUser()
                .getCollection()
                .hasOwnedPlant(plantName);
    }

    protected boolean isPlantSelectedForThisLevel(String plantName) {
        if (plantName == null || gameSession == null) {
            return false;
        }

        for (String selectedPlantName : gameSession.getSelectedPlantNames()) {
            if (normalizeName(selectedPlantName).equals(normalizeName(plantName))) {
                return true;
            }
        }

        return false;
    }

    protected boolean isPlantBoostedForThisGame(String plantName) {
        return plantName != null && boostedPlantNames.contains(normalizeName(plantName));
    }

    protected void appendSelectablePlantLine(StringBuilder builder, PlantType type) {
        if (builder == null || type == null) {
            return;
        }

        boolean allowedInChapter = isPlantAllowedInCurrentLevel(type.getName());
        boolean unlocked = isPlantUnlockedByUser(type.getName());
        boolean available = allowedInChapter && unlocked;
        boolean selected = isPlantSelectedForThisLevel(type.getName());
        boolean boosted = isPlantBoostedForThisGame(type.getName());

        builder.append("\n- ")
                .append(type.getName())
                .append(" | status: ")
                .append(available ? "available" : "locked")
                .append(" | collection: ")
                .append(unlocked ? "unlocked" : "locked")
                .append(" | chapter: ")
                .append(allowedInChapter ? "allowed" : "not allowed")
                .append(" | cost: ")
                .append(type.getSunCost())
                .append(" | category: ")
                .append(type.getCategory())
                .append(" | selected: ")
                .append(selected ? "yes" : "no")
                .append(" | boosted: ")
                .append(boosted ? "yes" : "no");
    }

    protected void appendSelectedPlants(StringBuilder builder) {
        if (builder == null) {
            return;
        }

        builder.append(selectedPlantsText());
    }

    protected String selectedPlantsText() {
        StringBuilder builder = new StringBuilder();
        int selectedCount = gameSession == null ? 0 : gameSession.getSelectedPlantNames().size();

        builder.append("\nSelected Plants (")
                .append(selectedCount)
                .append("/")
                .append(currentPlantSelectionLimit())
                .append(")")
                .append("\n===============");

        if (gameSession == null || gameSession.getSelectedPlantNames().isEmpty()) {
            builder.append("\nNo plants selected.");
            return builder.toString();
        }

        for (String plantName : gameSession.getSelectedPlantNames()) {
            builder.append("\n- ").append(plantName);

            if (isPlantBoostedForThisGame(plantName)) {
                builder.append(" [boosted]");
            }
        }

        return builder.toString();
    }

    protected void applyEntranceBoost(
            Position position,
            StringBuilder builder,
            boolean paidBoost,
            boolean greenhouseBoost
    ) {
        if (position == null || builder == null) {
            return;
        }

        if (gameSession.activatePlantFood(position, false)) {
            builder.append(" Entrance boost was applied");
            if (paidBoost && greenhouseBoost) {
                builder.append(" from the game boost and the stored greenhouse boost was consumed");
            } else if (greenhouseBoost) {
                builder.append(" from the stored greenhouse boost");
            }
            builder.append(".");
            return;
        }

        builder.append(" Entrance boost could not be applied.");
    }

    protected boolean consumeGreenhouseBoost(String plantName) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return false;
        }
        PlantData plantData = user.getCollection().findOwnedPlant(plantName);
        if (plantData == null || !plantData.useBoost()) {
            return false;
        }
        saveUsers();
        return true;
    }

}
