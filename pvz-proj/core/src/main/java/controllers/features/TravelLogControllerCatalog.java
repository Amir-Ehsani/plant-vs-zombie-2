package controllers.features;

import controllers.auth.AuthController;
import models.account.PlantData;
import models.account.Quest;
import models.account.User;

import java.util.ArrayList;
import java.util.List;

abstract class TravelLogControllerCatalog extends TravelLogControllerData {
    protected TravelLogControllerCatalog(AuthController authController) {
        super(authController);
    }

    protected List<QuestTemplate> getQuestTemplates() {
        List<QuestTemplate> templates = new ArrayList<>();
        addQuestTemplatesPart1(templates);
        addQuestTemplatesPart2(templates);
        addQuestTemplatesPart3(templates);
        addQuestTemplatesPart4(templates);
        addQuestTemplatesPart5(templates);
        addQuestTemplatesPart6(templates);
        addQuestTemplatesPart7(templates);
        addQuestTemplatesPart8(templates);
        addQuestTemplatesPart9(templates);
        addQuestTemplatesPart10(templates);
        return templates;
    }

    private void addQuestTemplatesPart1(List<QuestTemplate> templates) {
        templates.add(new QuestTemplate(
                "Daily Sun Collector",
                "challenges",
                "Collect 3000 sun during one day.",
                "30 coins",
                "medium",
                "Original variable: sun_amount = 3000, 4000, or 5000. This implementation starts with 3000.",
                "daily_sun_collector",
                "sun_collected",
                3000,
                30,
                0,
                0,
                false
        ));

        templates.add(new QuestTemplate(
                "Chapter Hunter",
                "adventure",
                "Defeat 50 zombies in a chapter.",
                "10 seed packets",
                "high",
                "Any chapter.",
                "chapter_hunter",
                "zombies_killed_in_chapter",
                50,
                0,
                0,
                10,
                false
        ));
    }

    private void addQuestTemplatesPart2(List<QuestTemplate> templates) {
        templates.add(new QuestTemplate(
                "Professional Plant Player",
                "challenges",
                "Kill 10 zombies using only one killing plant.",
                "Random new plant",
                "high",
                "Any plant that can kill zombies.",
                "professional_plant_player",
                "single_plant_kills",
                10,
                0,
                0,
                0,
                true
        ));

        templates.add(new QuestTemplate(
                "Only Cactus",
                "challenges",
                "Kill 10 zombies using only Cactus.",
                "20 gems",
                "high",
                "",
                "only_cactus",
                "cactus_kills",
                10,
                0,
                20,
                0,
                false
        ));
    }

    private void addQuestTemplatesPart3(List<QuestTemplate> templates) {
        templates.add(new QuestTemplate(
                "Economic Gardener",
                "adventure",
                "Win a level without losing more than n plants.",
                "20 - n seed packets",
                "high",
                "n can be 0, 1, 2, 3, 4, or 5.",
                "economic_gardener",
                "max_destroyed_plants",
                1,
                0,
                0,
                20,
                false
        ));

        templates.add(new QuestTemplate(
                "Defense Master",
                "special",
                "Finish a level with exactly 0 sun.",
                "200 gems",
                "critical",
                "",
                "defense_master",
                "finish_with_zero_sun",
                1,
                0,
                200,
                0,
                false
        ));
    }

    private void addQuestTemplatesPart4(List<QuestTemplate> templates) {
        templates.add(new QuestTemplate(
                "Fast Reaction",
                "adventure",
                "Kill 10 zombies within 30 seconds after the first wave starts.",
                "500 coins",
                "medium",
                "",
                "fast_reaction",
                "quick_kills_after_first_wave",
                10,
                500,
                0,
                0,
                false
        ));

        templates.add(new QuestTemplate(
                "Professional Destroyer",
                "challenges",
                "Use 3 explosive plants in one level.",
                "100 coins",
                "low",
                "",
                "professional_destroyer",
                "explosive_plants_used",
                3,
                100,
                0,
                0,
                false
        ));
    }

    private void addQuestTemplatesPart5(List<QuestTemplate> templates) {
        templates.add(new QuestTemplate(
                "Symmetry",
                "challenges",
                "The final lawn must be symmetric.",
                "500 coins",
                "high",
                "",
                "symmetry",
                "symmetric_final_lawn",
                1,
                500,
                0,
                0,
                false
        ));

        templates.add(new QuestTemplate(
                "Family Massacre",
                "challenges",
                "Use only plants from one family type to kill zombies.",
                "1000 coins",
                "medium",
                "Plant family/category.",
                "family_massacre",
                "single_family_kills",
                1,
                1000,
                0,
                0,
                false
        ));
    }

    private void addQuestTemplatesPart6(List<QuestTemplate> templates) {
        templates.add(new QuestTemplate(
                "Bloom Under Limits",
                "challenges",
                "Win a level without using plants from one family type.",
                "100 gems",
                "high",
                "Plant family/category.",
                "bloom_under_limits",
                "missing_family_win",
                1,
                0,
                100,
                0,
                false
        ));

        templates.add(new QuestTemplate(
                "Night or Morning",
                "special",
                "Finish a day level using night plants, meaning mushroom plants.",
                "20 gems",
                "high",
                "",
                "night_or_morning",
                "day_level_with_mushrooms",
                1,
                0,
                20,
                0,
                false
        ));
    }

    private void addQuestTemplatesPart7(List<QuestTemplate> templates) {
        templates.add(new QuestTemplate(
                "Win Streak",
                "challenges",
                "Win 5 levels in a row on the highest difficulty.",
                "5000 coins",
                "medium",
                "",
                "win_streak",
                "max_difficulty_win_streak",
                5,
                5000,
                0,
                0,
                false
        ));

        templates.add(new QuestTemplate(
                "Almost Won",
                "challenges",
                "Kill 10 zombies in the first column of a row whose lawn mower is gone.",
                "300 coins",
                "medium",
                "",
                "almost_won",
                "first_column_no_mower_kills",
                10,
                300,
                0,
                0,
                false
        ));
    }

    private void addQuestTemplatesPart8(List<QuestTemplate> templates) {
        templates.add(new QuestTemplate(
                "Anti OCD",
                "challenges",
                "Win with no symmetry in the lawn, except the middle row.",
                "800 coins",
                "medium",
                "",
                "anti_ocd",
                "non_symmetric_final_lawn",
                1,
                800,
                0,
                0,
                false
        ));

        templates.add(new QuestTemplate(
                "Cloudy Day",
                "challenges",
                "Win a level using only 3 sun-producing plants.",
                "10 gems",
                "high",
                "",
                "cloudy_day",
                "three_sun_producers_win",
                1,
                0,
                10,
                0,
                false
        ));
    }

    private void addQuestTemplatesPart9(List<QuestTemplate> templates) {
        templates.add(new QuestTemplate(
                "One Less Column",
                "challenges",
                "Win a level without planting anything in column n.",
                "10 gems",
                "high",
                "n is one of the lawn columns.",
                "one_less_column",
                "empty_column_win",
                1,
                0,
                10,
                0,
                false
        ));

        templates.add(new QuestTemplate(
                "Defenseless Row",
                "challenges",
                "Win a level without planting anything in row n.",
                "20 gems",
                "high",
                "n is one of the lawn rows.",
                "defenseless_row",
                "empty_row_win",
                1,
                0,
                20,
                0,
                false
        ));
    }

    private void addQuestTemplatesPart10(List<QuestTemplate> templates) {
        templates.add(new QuestTemplate(
                "Defenseless Cross",
                "challenges",
                "Win a level with both row n and column n empty.",
                "25 gems",
                "high",
                "n is based on the minimum of row count and column count.",
                "defenseless_cross",
                "empty_cross_win",
                1,
                0,
                25,
                0,
                false
        ));

        templates.add(new QuestTemplate(
                "Lawn Mower Time",
                "special",
                "Kill at least 10 zombies with lawn mowers.",
                "10 gems",
                "medium",
                "Original variable: n = 10, 20, 30, 40, or 50. This implementation starts with 10.",
                "lawn_mower_time",
                "lawn_mower_kills",
                10,
                0,
                10,
                0,
                false
        ));
    }

    protected String applyQuestReward(User user, Quest quest) {
        user.addCoins(quest.getCoinReward());
        user.addGems(quest.getGemReward());

        StringBuilder reward = new StringBuilder();

        if (quest.getCoinReward() > 0) {
            reward.append(quest.getCoinReward()).append(" coins");
        }

        if (quest.getGemReward() > 0) {
            if (reward.length() > 0) {
                reward.append(", ");
            }
            reward.append(quest.getGemReward()).append(" gems");
        }

        if (quest.getSeedPacketReward() > 0) {
            String plantName = addSeedPacketsToRewardPlant(user, quest.getSeedPacketReward());

            if (reward.length() > 0) {
                reward.append(", ");
            }
            reward.append(quest.getSeedPacketReward()).append(" seed packets");

            if (!plantName.isBlank()) {
                reward.append(" for ").append(plantName);
            }
        }

        if (quest.hasRandomPlantReward()) {
            String unlockedPlantName = unlockRandomPlantReward(user);

            if (reward.length() > 0) {
                reward.append(", ");
            }

            if (unlockedPlantName.isBlank()) {
                reward.append("random plant reward was already owned");
            } else {
                reward.append("random plant unlocked: ").append(unlockedPlantName);
            }
        }

        if (reward.length() == 0) {
            reward.append(quest.rewardText());
        }

        return reward.toString();
    }

    protected String addSeedPacketsToRewardPlant(User user, int amount) {
        prepareCollectionPlants(user);

        List<PlantData> ownedPlants = user.getCollection().getOwnedPlants();

        if (ownedPlants.isEmpty()) {
            user.getCollection().unlockPlant("Peashooter");
            ownedPlants = user.getCollection().getOwnedPlants();
        }

        if (ownedPlants.isEmpty()) {
            return "";
        }

        String plantName = ownedPlants.get(0).getName();
        user.getCollection().addSeedPackets(plantName, amount);
        return plantName;
    }

    protected String unlockRandomPlantReward(User user) {
        prepareCollectionPlants(user);

        List<PlantData> lockedPlants = user.getCollection().getLockedPlants();

        if (lockedPlants.isEmpty()) {
            return "";
        }

        String plantName = lockedPlants.get(0).getName();
        user.getCollection().unlockPlant(plantName);
        return plantName;
    }

}
