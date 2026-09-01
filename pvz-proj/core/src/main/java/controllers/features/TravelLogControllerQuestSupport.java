package controllers.features;

import controllers.auth.AuthController;
import models.account.Quest;
import models.account.User;
import models.minigame.MiniGameSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

abstract class TravelLogControllerQuestSupport extends TravelLogControllerCatalog {
    protected TravelLogControllerQuestSupport(AuthController authController) {
        super(authController);
    }

    protected MiniGameSession requireActiveMiniGame() {
        if (activeMiniGame == null) {
            fail("No mini-game is active.");
            return null;
        }

        return activeMiniGame;
    }

    protected List<Quest> getQuestsByPage(User user, String pageName) {
        List<Quest> quests = new ArrayList<>();

        for (Quest quest : user.getQuests()) {
            if (quest == null) {
                continue;
            }

            if ("all".equals(pageName) || quest.matchesType(pageName)) {
                quests.add(quest);
            }
        }

        quests.sort((firstQuest, secondQuest) -> {
            if (firstQuest.canClaimReward() && !secondQuest.canClaimReward()) {
                return -1;
            }

            if (!firstQuest.canClaimReward() && secondQuest.canClaimReward()) {
                return 1;
            }

            int firstPriority = questPriority(firstQuest);
            int secondPriority = questPriority(secondQuest);

            if (firstPriority != secondPriority) {
                return Integer.compare(firstPriority, secondPriority);
            }

            int firstPagePriority = pagePriority(firstQuest);
            int secondPagePriority = pagePriority(secondQuest);

            if (firstPagePriority != secondPagePriority) {
                return Integer.compare(firstPagePriority, secondPagePriority);
            }

            return firstQuest.getQuestDescription()
                    .compareToIgnoreCase(secondQuest.getQuestDescription());
        });

        return quests;
    }

    protected int pagePriority(Quest quest) {
        if (quest.matchesType("adventure")) {
            return 1;
        }
        if (quest.matchesType("special")) {
            return 2;
        }
        if (quest.matchesType("challenges")) {
            return 3;
        }
        if (quest.matchesType("minigames")) {
            return 4;
        }
        if (quest.matchesType("community")) {
            return 5;
        }
        if (quest.matchesType("mystery")) {
            return 6;
        }

        return 100;
    }

    protected int questPriority(Quest quest) {
        String priority = quest.getPriority();

        if (priority == null) {
            return 50;
        }

        String normalizedPriority = priority.trim().toLowerCase(Locale.ROOT);

        if ("critical".equals(normalizedPriority) || "بحرانی".equals(normalizedPriority)) {
            return 1;
        }
        if ("high".equals(normalizedPriority) || "بالا".equals(normalizedPriority)) {
            return 2;
        }
        if ("medium".equals(normalizedPriority) || "متوسط".equals(normalizedPriority)) {
            return 3;
        }
        if ("low".equals(normalizedPriority) || "کم".equals(normalizedPriority)) {
            return 4;
        }

        return 50;
    }

    protected void ensureDefaultQuests(User user) {
        if (user.getQuests() == null) {
            user.setQuests(new ArrayList<>());
        }

        boolean changed = removeOldPlaceholderQuests(user.getQuests());

        for (QuestTemplate template : getQuestTemplates()) {
            Quest existingQuest = findQuestByDescription(user.getQuests(), template.questDescription);

            if (existingQuest == null) {
                user.addQuest(template.createQuest());
                changed = true;
                continue;
            }

            changed = updateQuestFromTemplate(existingQuest, template) || changed;
        }

        if (changed) {
            authController.saveUsers();
        }
    }

    protected boolean removeOldPlaceholderQuests(List<Quest> quests) {
        List<String> oldQuestDescriptions = Arrays.asList(
                "Finish Ancient Egypt Part 1",
                "Adventure Extra: Daytime Dark Ages",
                "Win Vasebreaker",
                "Collect from the greenhouse",
                "Find a mystery reward"
        );

        boolean removed = false;

        for (int i = quests.size() - 1; i >= 0; i--) {
            Quest quest = quests.get(i);

            if (quest == null || oldQuestDescriptions.contains(quest.getQuestDescription())) {
                quests.remove(i);
                removed = true;
            }
        }

        return removed;
    }

    protected Quest findQuestByDescription(List<Quest> quests, String questDescription) {
        String normalizedQuestDescription = normalizeQuestDescription(questDescription);

        for (Quest quest : quests) {
            if (quest == null) {
                continue;
            }

            if (normalizeQuestDescription(quest.getQuestDescription()).equals(normalizedQuestDescription)) {
                return quest;
            }
        }

        return null;
    }

    protected boolean updateQuestFromTemplate(Quest quest, QuestTemplate template) {
        boolean changed = updateQuestTextFields(quest, template);
        changed |= updateQuestTargetFields(quest, template);
        changed |= updateQuestRewardFields(quest, template);
        return changed;
    }

    private boolean updateQuestTextFields(Quest quest, QuestTemplate template) {
        boolean changed = false;
        if (!quest.getType().equals(template.type)) {
            quest.setType(template.type);
            changed = true;
        }
        if (!quest.getConditionDescription().equals(template.conditionDescription)) {
            quest.setConditionDescription(template.conditionDescription);
            changed = true;
        }
        if (!quest.getRewardDescription().equals(template.rewardDescription)) {
            quest.setRewardDescription(template.rewardDescription);
            changed = true;
        }
        if (!quest.getPriority().equals(template.priority)) {
            quest.setPriority(template.priority);
            changed = true;
        }
        if (!quest.getVariables().equals(template.variables)) {
            quest.setVariables(template.variables);
            changed = true;
        }
        return changed;
    }

    private boolean updateQuestTargetFields(Quest quest, QuestTemplate template) {
        boolean changed = false;
        if (!quest.getProgressKey().equals(template.progressKey)) {
            quest.setProgressKey(template.progressKey);
            changed = true;
        }
        if (!quest.getTargetKey().equals(template.targetKey)) {
            quest.setTargetKey(template.targetKey);
            changed = true;
        }
        if (quest.getTargetAmount() != template.targetAmount) {
            quest.setTargetAmount(template.targetAmount);
            changed = true;
        }
        return changed;
    }

    private boolean updateQuestRewardFields(Quest quest, QuestTemplate template) {
        boolean changed = false;
        if (quest.getCoinReward() != template.coinReward) {
            quest.setCoinReward(template.coinReward);
            changed = true;
        }
        if (quest.getGemReward() != template.gemReward) {
            quest.setGemReward(template.gemReward);
            changed = true;
        }
        if (quest.getSeedPacketReward() != template.seedPacketReward) {
            quest.setSeedPacketReward(template.seedPacketReward);
            changed = true;
        }
        if (quest.hasRandomPlantReward() != template.randomPlantReward) {
            quest.setRandomPlantReward(template.randomPlantReward);
            changed = true;
        }
        return changed;
    }

}
