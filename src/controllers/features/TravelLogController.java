package controllers.features;

import controllers.auth.AuthController;
import models.account.PlantData;
import models.account.Quest;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.engine.board.Position;
import models.minigame.IZombieGame;
import models.minigame.MiniGameSession;
import models.minigame.MiniGameType;
import models.minigame.VasebreakerGame;
import models.minigame.WallNutBowlingGame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class TravelLogController extends TravelLogControllerMiniGameSupport {
    public TravelLogController(AuthController authController) {
        super(authController);
    }

    public List<Quest> showPage(String pageName) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return new ArrayList<>();
        }

        String normalizedPageName = normalizePageName(pageName);

        if (!isValidPage(normalizedPageName)) {
            fail("Travel log page " + pageName + " does not exist.");
            return new ArrayList<>();
        }

        ensureDefaultQuests(user);

        List<Quest> quests = getQuestsByPage(user, normalizedPageName);
        success("Travel log page " + displayPageName(normalizedPageName) + " shown.");
        return quests;
    }

    public void collectQuestReward(String pageName, int questNumber) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        String normalizedPageName = normalizePageName(pageName);

        if (!isValidPage(normalizedPageName)) {
            fail("Travel log page " + pageName + " does not exist.");
            return;
        }

        ensureDefaultQuests(user);

        List<Quest> quests = getQuestsByPage(user, normalizedPageName);

        if (questNumber < 1 || questNumber > quests.size()) {
            fail("Quest number is invalid.");
            return;
        }

        Quest quest = quests.get(questNumber - 1);

        if (!quest.isCompleted()) {
            fail("Quest is not done yet.");
            return;
        }

        if (quest.isRewardClaimed()) {
            fail("Quest reward is already collected.");
            return;
        }

        quest.claimReward();
        String rewardText = applyQuestReward(user, quest);
        authController.saveUsers();

        success("Quest reward collected: " + rewardText + ".");
    }

    public void collectAllDoneRewards(String pageName) {
        User user = getLoggedInUserOrFail();
        if (user == null) return;
        String normalizedPageName = normalizePageName(pageName);
        if (!isValidPage(normalizedPageName)) {
            fail("Travel log page " + pageName + " does not exist.");
            return;
        }
        ensureDefaultQuests(user);
        RewardTotals totals = collectAvailableRewards(user, getQuestsByPage(user, normalizedPageName));
        if (totals.count == 0) {
            fail("There are no done quests with uncollected rewards.");
            return;
        }
        authController.saveUsers();
        success(totals.message());
    }

    private RewardTotals collectAvailableRewards(User user, List<Quest> quests) {
        RewardTotals totals = new RewardTotals();
        for (Quest quest : quests) {
            if (!quest.canClaimReward()) continue;
            quest.claimReward();
            applyQuestReward(user, quest);
            totals.add(quest);
        }
        return totals;
    }

    private static final class RewardTotals {
        private int count;
        private int coins;
        private int gems;
        private int seedPackets;
        private int randomPlants;

        private void add(Quest quest) {
            count++;
            coins += quest.getCoinReward();
            gems += quest.getGemReward();
            seedPackets += quest.getSeedPacketReward();
            if (quest.hasRandomPlantReward()) randomPlants++;
        }

        private String message() {
            return "Collected rewards from " + count + " quests: " + coins + " coins, "
                    + gems + " gems, " + seedPackets + " seed packets, "
                    + randomPlants + " random plants.";
        }
    }


    public void recordQuestProgress(User user, String progressKey, int amount) {
        if (user == null || progressKey == null || progressKey.isBlank() || amount <= 0) {
            return;
        }

        ensureDefaultQuests(user);

        for (Quest quest : user.getQuests()) {
            if (quest == null || quest.isCompleted() || quest.isRewardClaimed()) {
                continue;
            }

            if (quest.matchesProgressKey(progressKey)) {
                quest.addProgress(amount);
            }
        }

        saveUsers();
    }

    public void completeQuest(User user, String progressKey) {
        if (user == null || progressKey == null || progressKey.isBlank()) {
            return;
        }

        ensureDefaultQuests(user);

        for (Quest quest : user.getQuests()) {
            if (quest == null || quest.isCompleted() || quest.isRewardClaimed()) {
                continue;
            }

            if (quest.matchesProgressKey(progressKey)) {
                quest.complete();
            }
        }

        saveUsers();
    }

    public void completeQuestWithSeedReward(User user, String progressKey, int seedPacketReward) {
        if (user == null || progressKey == null || progressKey.isBlank()) {
            return;
        }

        ensureDefaultQuests(user);

        for (Quest quest : user.getQuests()) {
            if (quest == null || quest.isCompleted() || quest.isRewardClaimed()) {
                continue;
            }

            if (quest.matchesProgressKey(progressKey)) {
                quest.setSeedPacketReward(seedPacketReward);
                quest.complete();
            }
        }

        saveUsers();
    }

    public void completeQuestWithGemReward(User user, String progressKey, int gemReward) {
        if (user == null || progressKey == null || progressKey.isBlank()) {
            return;
        }

        ensureDefaultQuests(user);

        for (Quest quest : user.getQuests()) {
            if (quest == null || quest.isCompleted() || quest.isRewardClaimed()) {
                continue;
            }

            if (quest.matchesProgressKey(progressKey)) {
                quest.setGemReward(gemReward);
                quest.complete();
            }
        }

        saveUsers();
    }

}
