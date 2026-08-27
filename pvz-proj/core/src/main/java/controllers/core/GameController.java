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


public class GameController extends GameControllerPlaySupport {
    public GameController(AuthController authController) {
        super(authController);
    }

    public void setGameSession(GameSession gameSession) {
        if (gameSession == null) {
            fail("Game session cannot be null.");
            return;
        }

        this.gameSession = gameSession;
        success("Game session is ready.");
    }

    public GameSession getGameSession() {
        return gameSession;
    }

    public void prepareChapter(String chapterName) {
        prepareChapterLevel(chapterName, 1);
    }

    public void prepareChapterLevel(String chapterName, int levelNumber) {
        User user = getLoggedInUserOrFail();
        if (user == null) return;
        String chapter = validateChapterRequest(chapterName, levelNumber, user);
        if (chapter == null) return;
        AdventureUnlockSummary unlockSummary = synchronizeAdventureContent(user, chapter, levelNumber);
        GameSession session = new GameSession();
        session.setCurrentLevel(createAdventureLevel(chapter, levelNumber));
        boostedPlantNames.clear();
        gameSession = session;
        currentChapterName = chapter;
        currentLevelNumber = levelNumber;
        resetRuntimeQuestTracking();
        ensureQuestList(user);
        reportPreparedLevel(session, chapter, levelNumber, unlockSummary);
    }

    private String validateChapterRequest(String chapterName, int levelNumber, User user) {
        if (chapterName == null || chapterName.isBlank()) {
            fail("Chapter name is required.");
            return null;
        }
        String chapter = AdventureLevelCatalog.normalizeChapterName(chapterName);
        if (!AdventureLevelCatalog.chapterExists(chapter)) {
            fail("Chapter " + chapterName.trim() + " does not exist.");
            return null;
        }
        if (!AdventureLevelCatalog.isPlayableLevel(levelNumber)) {
            fail("Adventure level number must be between 1 and 4.");
            return null;
        }
        if (levelNumber == AdventureLevelCatalog.BOSS_LEVEL
                && !AdventureLevelCatalog.isBossLevelAvailable(chapter)) {
            fail("This chapter boss is handled outside P2-09 and is not available yet.");
            return null;
        }
        if (!user.isChapterLevelUnlocked(chapter, levelNumber)) {
            fail("Level " + levelNumber + " is locked.");
            return null;
        }
        return chapter;
    }

    private void reportPreparedLevel(
            GameSession session, String chapter, int levelNumber, AdventureUnlockSummary unlockSummary
    ) {
        String title = AdventureLevelCatalog.levelTitle(chapter, levelNumber);
        String prefix = "Chapter " + AdventureLevelCatalog.displayChapterName(chapter)
                + " level " + levelNumber + " (" + title + ") is ready";
        if (session.getCurrentLevel().usesConveyorBelt()) {
            success(prefix + " and will start automatically." + unlockSummary.asMessage());
            return;
        }
        success(prefix + ". Select up to " + currentPlantSelectionLimit()
                + " unlocked plants before starting the game." + unlockSummary.asMessage());
    }


    public boolean shouldAutoStartCurrentLevel() {
        return hasPreparedSession() && gameSession.getCurrentLevel().usesConveyorBelt();
    }

    public boolean isGameFinished() {
        if (gameSession == null || gameSession.getState() == null) {
            return false;
        }
        GameState.Status status = gameSession.getState().getStatus();
        return status == GameState.Status.WON || status == GameState.Status.LOST;
    }

    public void returnToLevelMenuRejected() {
        fail("The game is still running. Finish or leave the level before returning to level selection.");
    }

    public void showAllPlants() {
        if (!hasPreparedSession()) {
            fail("Game session is not available.");
            return;
        }

        StringBuilder builder = new StringBuilder("All Plants\n==========");

        for (PlantType type : plantRegistry.getAllPlantTypes()) {
            appendSelectablePlantLine(builder, type);
        }

        appendSelectedPlants(builder);
        success(builder.toString());
    }

    public void showAvailablePlants() {
        if (!hasPreparedSession()) {
            fail("Game session is not available.");
            return;
        }

        StringBuilder builder = new StringBuilder("Available Plants\n================");
        int availableCount = 0;

        for (PlantType type : plantRegistry.getAllPlantTypes()) {
            if (isPlantAvailableForCurrentLevel(type.getName())) {
                appendSelectablePlantLine(builder, type);
                availableCount++;
            }
        }

        if (availableCount == 0) {
            builder.append("\nNo available plants.");
        }

        appendSelectedPlants(builder);
        success(builder.toString());
    }

    public void addPlantToSelection(String plantName) {
        if (!hasPreparedSession()) {
            fail("Game session is not available.");
            return;
        }

        if (gameSession.isRunning()) {
            fail("You cannot change selected plants after the game starts.");
            return;
        }

        PlantType type = plantRegistry.getByName(plantName);

        if (type == null) {
            fail("Plant does not exist.");
            return;
        }

        if (!isPlantAllowedInCurrentLevel(type.getName())) {
            fail("Plant is not available in this chapter.");
            return;
        }

        if (!isPlantUnlockedByUser(type.getName())) {
            fail("Plant is locked in your collection.");
            return;
        }

        if (isPlantSelectedForThisLevel(type.getName())) {
            fail("Plant is already selected.");
            return;
        }

        int selectionLimit = currentPlantSelectionLimit();
        if (gameSession.getSelectedPlantNames().size() >= selectionLimit) {
            fail("You can select at most " + selectionLimit + " plants.");
            return;
        }

        if (!gameSession.addSelectedPlant(type.getName())) {
            fail("Plant could not be selected.");
            return;
        }

        success(type.getName() + " added to selected plants." + selectedPlantsText());
    }

    public void removePlantFromSelection(String plantName) {
        if (!hasPreparedSession()) {
            fail("Game session is not available.");
            return;
        }

        if (gameSession.isRunning()) {
            fail("You cannot change selected plants after the game starts.");
            return;
        }

        PlantType type = plantRegistry.getByName(plantName);

        if (type == null) {
            fail("Plant does not exist.");
            return;
        }

        if (!isPlantSelectedForThisLevel(type.getName())) {
            fail("Plant is not selected.");
            return;
        }

        if (!gameSession.removeSelectedPlant(type.getName())) {
            fail("Plant could not be removed.");
            return;
        }

        boostedPlantNames.remove(normalizeName(type.getName()));
        success(type.getName() + " removed from selected plants." + selectedPlantsText());
    }

    public void boostPlant(String plantName) {
        if (!canChangePlantSelection("boost selected plants")) return;
        User user = getLoggedInUserOrFail();
        if (user == null) return;
        PlantType type = plantRegistry.getByName(plantName);
        if (!validateBoostPlant(type)) return;
        String normalizedName = normalizeName(type.getName());
        if (boostedPlantNames.contains(normalizedName)) {
            fail("Plant is already boosted.");
            return;
        }
        if (!user.spendGems(BOOST_GEM_COST)) {
            fail("Not enough gems. Required: " + BOOST_GEM_COST + ".");
            return;
        }
        boostedPlantNames.add(normalizedName);
        saveUsers();
        success(type.getName() + " boosted for this game. " + BOOST_GEM_COST
                + " gems were spent; remaining gems: " + user.getGems() + "." + selectedPlantsText());
    }

    private boolean canChangePlantSelection(String action) {
        if (!hasPreparedSession()) {
            fail("Game session is not available.");
            return false;
        }
        if (gameSession.isRunning()) {
            fail("You cannot " + action + " after the game starts.");
            return false;
        }
        return true;
    }

    private boolean validateBoostPlant(PlantType type) {
        if (type == null) {
            fail("Plant does not exist.");
            return false;
        }
        if (!isPlantSelectedForThisLevel(type.getName())) {
            fail("Plant must be selected before boosting.");
            return false;
        }
        return true;
    }


    public void startGame() {
        if (gameSession == null) {
            fail("Game session is not available.");
            return;
        }

        if (gameSession.isRunning()) {
            fail("Game is already running.");
            return;
        }

        if (gameSession.getSelectedPlantNames().isEmpty()
                && !gameSession.getCurrentLevel().usesConveyorBelt()) {
            fail("Select at least one plant before starting the game.");
            return;
        }

        try {
            resetRuntimeQuestTracking();
            User user = getLoggedInUserOrFail();
            ensureQuestList(user);
            int storedPlantFood = user.getCollection().getStoredPlantFood();
            gameSession.setInitialPlantFoodCount(storedPlantFood);
            gameSession.initSession();
            user.getCollection().takeStoredPlantFood();
            saveUsers();

            StringBuilder builder = new StringBuilder("Game started.");
            List<GameEvent> events = gameSession.drainEvents();
            processQuestEvents(events);
            appendEvents(builder, events);
            updateFinishedStatsAndQuestsIfNeeded(builder);

            success(builder.toString());
        } catch (IllegalStateException | IllegalArgumentException exception) {
            fail(exception.getMessage());
        }
    }

    public void advanceTime(int ticks) {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (ticks <= 0) {
            fail("Tick count must be positive.");
            return;
        }

        gameSession.clearPendingEvents();

        if (!gameSession.advanceTicks(ticks)) {
            fail("Time could not be advanced.");
            return;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Advanced ")
                .append(gameSession.getLastAdvancedTickCount())
                .append(" ticks. Current tick = ")
                .append(gameSession.getTickManager().getCurrentTick())
                .append(".");

        List<GameEvent> events = gameSession.drainEvents();
        processQuestEvents(events);
        appendEvents(builder, events);
        updateFinishedStatsAndQuestsIfNeeded(builder);

        success(builder.toString());
    }

    public void update() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        gameSession.clearPendingEvents();
        gameSession.updateSession();

        StringBuilder builder = new StringBuilder("Game updated.");
        List<GameEvent> events = gameSession.drainEvents();
        processQuestEvents(events);
        appendEvents(builder, events);
        updateFinishedStatsAndQuestsIfNeeded(builder);

        success(builder.toString());
    }

    public void handlePause() {
        if (!hasRunningSession()) {
            fail("No running game is available.");
            return;
        }

        if (gameSession.getTickManager().isPaused()) {
            gameSession.getTickManager().resume();
            success("Game resumed.");
        } else {
            gameSession.getTickManager().pause();
            success("Game paused.");
        }
    }

    public void handleUserClick(Position position) {
        showTile(position);
    }

}
