package controllers.core;

import controllers.features.TravelLogController;
import models.account.Collection;
import models.account.News;
import models.account.PlantData;
import models.account.Quest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import controllers.auth.AuthController;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.core.zombie.Armor;
import models.core.zombie.DefaultZombieRegistry;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.core.zombie.ZombieRegistry;
import models.core.zombie.ZombieType;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.engine.events.GameEvent;
import models.engine.session.GameSession;
import models.engine.session.GameState;
import models.engine.session.PlantRechargeStatus;
import models.engine.sun.Sun;
import models.level.core.AdventureContentCatalog;
import models.level.core.AdventureLevelCatalog;
import models.level.core.Level;
import models.level.core.LevelType;
import models.level.rules.LevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.NoSpecialRule;
import models.level.rules.SpecialLevelType;
import models.level.rules.TimedWarObjective;
import models.level.rules.impl.ConveyorBeltRule;
import models.level.rules.impl.DeadLineRule;
import models.level.rules.impl.LockedPlantsRule;
import models.level.rules.impl.LoveYourPlantsRule;
import models.level.rules.impl.NightOpsRule;
import models.level.rules.impl.PlantWhatYouGetRule;
import models.level.rules.impl.SaveOurSeedsRule;
import models.level.rules.impl.TimedWarRule;
import models.level.wave.AttackPattern;
import models.level.wave.Wave;
import models.level.wave.WaveManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


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
