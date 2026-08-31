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


abstract class GameControllerPlaySupport extends GameControllerDisplaySupport {
    protected GameControllerPlaySupport(AuthController authController) {
        super(authController);
    }

    public void plant(String plantName, Position position) {
        PlantType type = validatePlantRequest(plantName, position);
        if (type == null || !validatePlantResources(type)) return;
        gameSession.clearPendingEvents();
        if (!gameSession.plant(type.getName(), position)) {
            fail("Plant could not be planted at " + position + ".");
            return;
        }
        recordPlantUsedForQuests(type, position);
        StringBuilder builder = plantedMessage(type, position);
        applyAvailableEntranceBoost(type, position, builder);
        List<GameEvent> events = gameSession.drainEvents();
        processQuestEvents(events);
        appendEvents(builder, events);
        updateFinishedStatsAndQuestsIfNeeded(builder);
        success(builder.toString());
    }

    private PlantType validatePlantRequest(String plantName, Position position) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return null;
        }
        if (plantName == null || plantName.isBlank()) {
            fail("Plant name is required.");
            return null;
        }
        if (position == null) {
            fail("Plant position is invalid.");
            return null;
        }
        PlantType type = gameSession.getPlantType(plantName);
        if (type == null) {
            fail("Plant does not exist.");
            return null;
        }
        if (!gameSession.isPlantSelected(type.getName())) {
            fail("Plant is not selected.");
            return null;
        }
        Level level = gameSession.getCurrentLevel();
        if ((level == null || level.getLevelType() != LevelType.BOSS)
                && (level == null || !level.usesConveyorBelt())
                && !isPlantUnlockedByUser(type.getName())) {
            fail("Plant is locked in your collection.");
            return null;
        }
        if (level != null && !level.isPlantAllowed(type.getName()) && !gameSession.isPlantSelected(type.getName())) {
            fail("Plant is locked or unavailable in this level.");
            return null;
        }
        if (level != null && level.getBossRuntime() != null
                && level.getBossRuntime().isPlantingBlocked(position)) {
            fail("This tile is burning and cannot be planted on yet.");
            return null;
        }
        return type;
    }

    private boolean validatePlantResources(PlantType type) {
        int remainingTicks = gameSession.getPlantRechargeRemainingTicks(type.getName());
        if (remainingTicks > 0) {
            fail("Plant is on cooldown for " + formatSeconds(remainingTicks) + " seconds.");
            return false;
        }
        if (gameSession.getPlantCost(type.getName()) > gameSession.getTotalSunAmount()) {
            fail("Not enough suns.");
            return false;
        }
        return true;
    }

    private StringBuilder plantedMessage(PlantType type, Position position) {
        return new StringBuilder("Plant ").append(type.getName()).append(" planted at ")
                .append(position).append("; sun amount: ")
                .append(gameSession.getTotalSunAmount()).append(".");
    }

    private void applyAvailableEntranceBoost(
            PlantType type, Position position, StringBuilder builder
    ) {
        boolean paidBoost = isPlantBoostedForThisGame(type.getName());
        boolean greenhouseBoost = consumeGreenhouseBoost(type.getName());
        if (paidBoost || greenhouseBoost) {
            applyEntranceBoost(position, builder, paidBoost, greenhouseBoost);
        }
    }


    public void pluck(Position position) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (position == null) {
            fail("Plant position is invalid.");
            return;
        }

        if (!gameSession.pluck(position)) {
            fail("There is no removable plant at " + position + ".");
            return;
        }

        success("Plant at " + position + " was plucked.");
    }

    public void feedPlant(Position position) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (position == null) {
            fail("Plant position is invalid.");
            return;
        }

        if (!gameSession.feedPlant(position)) {
            fail("Plant at " + position + " could not be fed.");
            return;
        }

        success("Plant at " + position
                + " was fed; plant foods: "
                + gameSession.getPlantFoodCount() + ".");
    }

    public void collectSun(Position position) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (position == null) {
            fail("Sun position is invalid.");
            return;
        }

        int sunBefore = gameSession.getTotalSunAmount();
        gameSession.clearPendingEvents();

        if (!gameSession.collectSun(position)) {
            fail("No collectible sun exists at " + position + ".");
            return;
        }

        int collectedAmount = Math.max(0, gameSession.getTotalSunAmount() - sunBefore);
        User user = getLoggedInUserOrFail();

        if (user != null && collectedAmount > 0) {
            travelLogController.recordQuestProgress(user, "daily_sun_collector", collectedAmount);
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Sun collected at ")
                .append(position)
                .append("; sun amount: ")
                .append(gameSession.getTotalSunAmount())
                .append(".");

        List<GameEvent> events = gameSession.drainEvents();
        processQuestEvents(events);
        appendEvents(builder, events);
        updateFinishedStatsAndQuestsIfNeeded(builder);

        success(builder.toString());
    }

    public void startZombieWaves() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (gameSession.getCurrentLevel() == null) {
            fail("Current level is not available.");
            return;
        }

        if (gameSession.getCurrentLevel().areZombieWavesStarted()) {
            success("Zombie waves are already started.");
            return;
        }

        if (!gameSession.startZombieWaves()) {
            fail("Zombie waves cannot be started now.");
            return;
        }

        success("Zombie waves started.");
    }

    public void addSunCheat(int amount) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (amount <= 0) {
            fail("Sun cheat amount must be positive.");
            return;
        }

        gameSession.addSun(amount);

        success(amount + " suns were added; sun amount: "
                + gameSession.getTotalSunAmount() + ".");
    }

    public void removeCooldownCheat() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (!gameSession.removePlantCooldowns()) {
            fail("Cooldowns could not be removed.");
            return;
        }

        success("Cooldowns have been removed.");
    }

    public void addPlantFoodCheat() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (!gameSession.addPlantFood()) {
            fail("Plant food storage is full; plant foods: "
                    + gameSession.getPlantFoodCount() + ".");
            return;
        }

        success("Plant food added; plant foods: "
                + gameSession.getPlantFoodCount() + ".");
    }

    public void spawnZombieCheat(String zombieName, Position position) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (zombieName == null || zombieName.isBlank()) {
            fail("Zombie name is required.");
            return;
        }

        if (position == null) {
            fail("Zombie position is invalid.");
            return;
        }

        if (!gameSession.spawnZombie(zombieName, position)) {
            fail("Zombie could not be spawned at " + position + ".");
            return;
        }

        success("Zombie " + zombieName.trim() + " spawned at " + position + ".");
    }

    public void releaseNuke() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        int destroyed = gameSession.releaseNuke();
        success("Nuke destroyed " + destroyed + " zombies.");
    }

}
