package controllers.core;

import controllers.auth.AuthController;
import models.account.User;
import models.core.plant.PlantType;
import models.engine.board.Position;
import models.engine.events.GameEvent;
import models.engine.session.GameState;
import models.level.core.AdventureLevelCatalog;

import java.util.List;

abstract class GameControllerQuestSupport extends GameControllerQuestRules {
    protected GameControllerQuestSupport(AuthController authController) {
        super(authController);
    }

    protected void recordPlantUsedForQuests(PlantType type, Position position) {
        if (type == null || position == null) {
            return;
        }

        plantedPlantNamesThisLevel.add(normalizeName(type.getName()));
        plantedPlantFamiliesThisLevel.add(normalizeName(type.getCategory()));
        plantedPositionsThisLevel.add(position);

        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        if (isExplosivePlant(type)) {
            explosivePlantsUsedThisLevel++;

            if (explosivePlantsUsedThisLevel >= 3) {
                travelLogController.completeQuest(user, "professional_destroyer");
            }
        }

        if (isSunProducerPlant(type)) {
            sunProducerPlantsPlantedThisLevel++;
        }
    }

    protected void processQuestEvents(List<GameEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        ensureQuestList(user);
        boolean rewardChanged = false;

        for (GameEvent event : events) {
            if (event == null || event.getType() == null) {
                continue;
            }

            switch (event.getType()) {
                case WAVE_STARTED:
                    if (firstWaveStartTick < 0) {
                        firstWaveStartTick = gameSession.getTickManager().getCurrentTick();
                    }
                    break;

                case ZOMBIE_KILLED:
                    processZombieKillQuestEvent(user, event);
                    break;

                case LAWN_MOWER_TRIGGERED:
                    int killedByMower = event.getEntityNames().size();
                    lawnMowerKillsThisLevel += killedByMower;

                    if (killedByMower > 0) {
                        travelLogController.recordQuestProgress(user, "lawn_mower_time", killedByMower);
                    }
                    break;

                case REWARD_DROPPED:
                    rewardChanged |= applyZombieReward(user, event);
                    break;

                default:
                    break;
            }
        }
        if (rewardChanged) {
            saveUsers();
        }
    }

    private boolean applyZombieReward(User user, GameEvent event) {
        String type = normalizeName(event.getEntityName());
        int amount = Math.max(1, event.getAmount());
        if (type.equals("coin")) {
            user.addCoins(amount);
            return true;
        }
        if (type.equals("diamond")) {
            user.addGems(amount);
            return true;
        }
        if (!type.equals("pot")) {
            return false;
        }
        if (!user.getGreenhouse().unlockNextPot()) {
            user.addCoins(50);
        }
        return true;
    }

    protected void processZombieKillQuestEvent(User user, GameEvent event) {
        travelLogController.recordQuestProgress(user, "chapter_hunter", 1);

        String killerPlantName = normalizeName(event.getSourcePlantName());
        String killerFamily = normalizeName(event.getSourcePlantCategory());

        if (!killerPlantName.isBlank()) {
            killingPlantNamesThisLevel.add(killerPlantName);
            killsByPlantName.put(
                    killerPlantName,
                    killsByPlantName.getOrDefault(killerPlantName, 0) + 1
            );

            if ("cactus".equals(killerPlantName)) {
                cactusKillsThisLevel++;
            } else {
                anyNonCactusKillThisLevel = true;
            }
        }

        if (!killerFamily.isBlank()) {
            killingPlantFamiliesThisLevel.add(killerFamily);
            killsByPlantFamily.put(
                    killerFamily,
                    killsByPlantFamily.getOrDefault(killerFamily, 0) + 1
            );
        }

        if (firstWaveStartTick >= 0) {
            int currentTick = gameSession.getTickManager().getCurrentTick();
            int ticksSinceFirstWave = currentTick - firstWaveStartTick;

            if (ticksSinceFirstWave <= 300) {
                fastReactionKillsThisLevel++;
                travelLogController.recordQuestProgress(user, "fast_reaction", 1);
            }
        }

        if (event.getX() <= 1.000001 && isLawnMowerUsed(event.getLaneNumber())) {
            firstColumnNoMowerKillsThisLevel++;
            travelLogController.recordQuestProgress(user, "almost_won", 1);
        }
    }

    protected void updateFinishedStatsAndQuestsIfNeeded(StringBuilder builder) {
        appendFinishedState(builder);
        if (!shouldRecordFinalStats()) {
            return;
        }
        finalStatsRecorded = true;
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }
        boolean won = gameSession.getState().getStatus() == GameState.Status.WON;
        CompletionUpdate update = updateAdventureCompletion(user, won);
        recordLeaderboardStats(user, won, update.newlyCompleted);
        if (won) {
            recordWinQuests(user);
        }
        else {
            resetQuestProgress(user, "win_streak");
        }
        saveUsers();
        appendSavedProgress(builder, won, update);
    }

    private boolean shouldRecordFinalStats() {
        if (gameSession == null || gameSession.getState() == null || finalStatsRecorded) {
            return false;
        }
        GameState.Status status = gameSession.getState().getStatus();
        return status == GameState.Status.WON || status == GameState.Status.LOST;
    }

    private CompletionUpdate updateAdventureCompletion(User user, boolean won) {
        CompletionUpdate update = new CompletionUpdate();
        if (!won) {
            return update;
        }
        update.newlyCompleted = user.completeChapterLevel(currentChapterName, currentLevelNumber);
        int chapterLastLevel = AdventureLevelCatalog.lastRequiredLevel(currentChapterName);
        if (currentLevelNumber == chapterLastLevel) {
            update.unlockedChapter = AdventureLevelCatalog.nextChapter(currentChapterName);
            if (update.unlockedChapter != null) {
                user.unlockChapter(update.unlockedChapter);
            }
        }
        return update;
    }

    private void appendSavedProgress(StringBuilder builder, boolean won, CompletionUpdate update) {
        builder.append("\nProgress and quest stats were saved.");
        int chapterLastLevel = AdventureLevelCatalog.lastRequiredLevel(currentChapterName);
        if (won && update.newlyCompleted && currentLevelNumber < chapterLastLevel) {
            builder.append("\nLevel ").append(currentLevelNumber).append(" completed. Level ")
                    .append(currentLevelNumber + 1).append(" is now unlocked.");
        } else if (won && currentLevelNumber == chapterLastLevel) {
            appendChapterCompletion(builder, update.unlockedChapter);
        }
        builder.append("\nType 'return to level menu' to return to the chapter level selection page.");
    }

    private void appendChapterCompletion(StringBuilder builder, String unlockedChapter) {
        if (unlockedChapter == null) {
            builder.append("\nAll currently implemented adventure chapters are completed.");
            return;
        }
        builder.append("\nChapter completed. ")
                .append(AdventureLevelCatalog.displayChapterName(unlockedChapter))
                .append(" is now unlocked.");
    }

    private static final class CompletionUpdate {
        private boolean newlyCompleted;
        private String unlockedChapter;
    }

    protected void recordLeaderboardStats(User user, boolean won, boolean newlyCompleted) {
        user.increaseGamesPlayed();

        int difficulty = currentDifficultyLevel();
        int kills = gameSession.getTotalZombiesKilled();
        int remainingSun = gameSession.getTotalSunAmount();

        int scoreGain = 100 + difficulty * 50 + kills * 10 + Math.max(0, remainingSun);

        if (won) {
            scoreGain += 500;
            if (newlyCompleted) {
                user.increasePassedLevels();
            }
        }

        user.addScore(scoreGain);
        user.updateBestMioPoint(scoreGain);
    }

    protected void recordWinQuests(User user) {
        recordResourceAndCombatWinQuests(user);
        recordLayoutWinQuests(user);
        recordRestrictionWinQuests(user);
    }

    private void recordResourceAndCombatWinQuests(User user) {
        int destroyedPlants = gameSession.getTotalPlantsDestroyed();
        if (destroyedPlants <= 5) {
            travelLogController.completeQuestWithSeedReward(
                    user, "economic_gardener", Math.max(0, 20 - destroyedPlants)
            );
        }
        if (gameSession.getTotalSunAmount() == 0) {
            travelLogController.completeQuest(user, "defense_master");
        }
        if (hasSingleKillingPlantWithAtLeastTenKills()) {
            travelLogController.completeQuest(user, "professional_plant_player");
        }
        if (cactusKillsThisLevel >= 10 && !anyNonCactusKillThisLevel) {
            travelLogController.completeQuest(user, "only_cactus");
        }
        if (hasSingleKillingFamily()) {
            travelLogController.completeQuest(user, "family_massacre");
        }
    }

    private void recordLayoutWinQuests(User user) {
        if (isFinalLawnSymmetric()) {
            travelLogController.completeQuest(user, "symmetry");
        }
        if (isFinalLawnNonSymmetricExceptMiddleRow()) {
            travelLogController.completeQuest(user, "anti_ocd");
        }
        if (hasEmptyColumnByPlantHistory()) {
            travelLogController.completeQuest(user, "one_less_column");
        }
        if (hasEmptyRowByPlantHistory()) {
            travelLogController.completeQuest(user, "defenseless_row");
        }
        if (hasEmptyCrossByPlantHistory()) {
            travelLogController.completeQuest(user, "defenseless_cross");
        }
    }

    private void recordRestrictionWinQuests(User user) {
        if (hasUnusedPlantFamily()) {
            travelLogController.completeQuest(user, "bloom_under_limits");
        }
        if (usedOnlyMushroomPlants()) {
            travelLogController.completeQuest(user, "night_or_morning");
        }
        if (sunProducerPlantsPlantedThisLevel == 3) {
            travelLogController.completeQuest(user, "cloudy_day");
        }
        if (currentDifficultyLevel() == 5) {
            travelLogController.recordQuestProgress(user, "win_streak", 1);
        } else {
            resetQuestProgress(user, "win_streak");
        }
    }

    protected abstract void appendFinishedState(StringBuilder builder);
}
