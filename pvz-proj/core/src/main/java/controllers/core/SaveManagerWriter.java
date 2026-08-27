package controllers.core;

import models.account.Collection;
import models.account.Greenhouse;
import models.account.News;
import models.account.PlantData;
import models.account.Quest;
import models.account.Settings;
import models.account.User;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


abstract class SaveManagerWriter extends SaveManagerReader {
    protected String usersToJson(List<User> users) {
        StringBuilder builder = new StringBuilder();
        builder.append("[\n");

        if (users != null) {
            for (int i = 0; i < users.size(); i++) {
                if (i > 0) {
                    builder.append(",\n");
                }

                builder.append(userToJson(users.get(i)));
            }
        }

        builder.append("\n]");
        return builder.toString();
    }

    protected String userToJson(User user) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean first = appendIdentityFields(builder, user, true);
        first = appendProgressFields(builder, user, first);
        appendAccountContentFields(builder, user, first);
        builder.append("\n}");
        return builder.toString();
    }

    private boolean appendIdentityFields(StringBuilder builder, User user, boolean first) {
        first = appendField(builder, "username", jsonString(user.getUsername()), first);
        first = appendField(builder, "password", jsonString(user.getPassword()), first);
        first = appendField(builder, "passwordHash", jsonString(user.getPasswordHash()), first);
        first = appendField(builder, "nickname", jsonString(user.getNickname()), first);
        first = appendField(builder, "email", jsonString(user.getEmail()), first);
        first = appendField(builder, "gender", jsonString(user.getGender()), first);
        first = appendField(
                builder,
                "securityQuestionNumber",
                String.valueOf(user.getSecurityQuestionNumber()),
                first
        );
        first = appendField(builder, "securityAnswer", jsonString(user.getSecurityAnswer()), first);
        first = appendField(builder, "profileImage", jsonString(user.getProfileImage()), first);
        return appendField(builder, "stayLoggedIn", String.valueOf(user.isStayLoggedIn()), first);
    }

    private boolean appendProgressFields(StringBuilder builder, User user, boolean first) {
        first = appendField(builder, "coins", String.valueOf(user.getCoins()), first);
        first = appendField(builder, "gems", String.valueOf(user.getGems()), first);
        first = appendField(builder, "score", String.valueOf(user.getScore()), first);
        first = appendField(builder, "gamesPlayed", String.valueOf(user.getGamesPlayed()), first);
        first = appendField(builder, "passedLevels", String.valueOf(user.getPassedLevels()), first);
        first = appendField(builder, "bestMioPoint", String.valueOf(user.getBestMioPoint()), first);
        first = appendField(builder, "difficultyLevel", String.valueOf(user.getDifficultyLevel()), first);
        first = appendField(builder, "settings", settingsToJson(user.getSettings()), first);
        first = appendField(builder, "currentChapterName", jsonString(user.getPersistentCurrentChapterName()), first);
        first = appendField(builder, "unlockedChapters", stringListToJson(user.getUnlockedChapters()), first);
        first = appendField(
                builder,
                "completedMiniGameStages",
                stringListToJson(user.getCompletedMiniGameStages()),
                first
        );
        first = appendField(
                builder,
                "completedChapterLevels",
                stringListToJson(user.getCompletedChapterLevels()),
                first
        );
        first = appendField(
                builder,
                "currentChapterLevel",
                String.valueOf(user.getPersistentCurrentChapterLevel()),
                first
        );
        return appendField(
                builder,
                "allAdventureLevelsUnlocked",
                String.valueOf(user.isAllAdventureLevelsUnlocked()),
                first
        );
    }

    protected String settingsToJson(Settings settings) {
        Settings value = settings == null ? new Settings() : settings;
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean first = true;
        first = appendField(builder, "difficulty", String.valueOf(value.getDifficulty()), first);
        first = appendField(builder, "gameSpeed", String.valueOf(value.getGameSpeed()), first);
        first = appendField(builder, "showGrid", String.valueOf(value.isGridVisible()), first);
        first = appendField(builder, "debugMode", String.valueOf(value.isDebugMode()), first);
        first = appendField(builder, "musicVolume", String.valueOf(value.getMusicVolume()), first);
        first = appendField(builder, "soundVolume", String.valueOf(value.getSoundVolume()), first);
        appendField(builder, "musicEnabled", String.valueOf(value.isMusicEnabled()), first);
        builder.append("\n}");
        return builder.toString();
    }

    private void appendAccountContentFields(StringBuilder builder, User user, boolean first) {
        first = appendField(builder, "collection", collectionToJson(user.getCollection()), first);
        first = appendField(builder, "greenhouse", greenhouseToJson(user.getGreenhouse()), first);
        first = appendField(builder, "quests", questsToJson(user.getQuests()), first);
        appendField(builder, "newsList", newsListToJson(user.getNewsList()), first);
    }

    protected String collectionToJson(Collection collection) {
        if (collection == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;

        builder.append("{");
        first = appendField(builder, "ownedPlants", plantsToJson(collection.getOwnedPlants()), first);
        first = appendField(builder, "lockedPlants", plantsToJson(collection.getLockedPlants()), first);
        first = appendField(builder, "ownedZombies", stringListToJson(collection.getOwnedZombies()), first);
        first = appendField(builder, "lockedZombies", stringListToJson(collection.getLockedZombies()), first);
        first = appendField(builder, "storedPlantFood", String.valueOf(collection.getStoredPlantFood()), first);
        first = appendField(builder, "dailyOfferDate", jsonString(collection.getDailyOfferDate()), first);
        first = appendField(builder, "dailyOfferPlantName", jsonString(collection.getDailyOfferPlantName()), first);
        first = appendField(builder, "dailyOfferPurchased", String.valueOf(collection.isDailyOfferPurchased()), first);
        appendField(builder, "shopItemAmounts", intMapToJson(collection.getShopItemAmounts()), first);
        builder.append("\n}");

        return builder.toString();
    }


    protected String intMapToJson(Map<String, Integer> values) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean first = true;
        if (values != null) {
            for (Map.Entry<String, Integer> entry : values.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                if (!first) {
                    builder.append(",");
                }
                builder.append(jsonString(entry.getKey()));
                builder.append(":");
                builder.append(Math.max(0, entry.getValue()));
                first = false;
            }
        }
        builder.append("}");
        return builder.toString();
    }

    protected String plantsToJson(List<PlantData> plants) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        if (plants != null) {
            for (int i = 0; i < plants.size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }

                builder.append(plantToJson(plants.get(i)));
            }
        }

        builder.append("]");
        return builder.toString();
    }

    protected String plantToJson(PlantData plant) {
        if (plant == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;

        builder.append("{");
        first = appendField(builder, "name", jsonString(plant.getName()), first);
        first = appendField(builder, "price", String.valueOf(plant.getPrice()), first);
        first = appendField(builder, "level", String.valueOf(plant.getLevel()), first);
        first = appendField(builder, "unlocked", String.valueOf(plant.isUnlocked()), first);
        first = appendField(builder, "seedPackets", String.valueOf(plant.getSeedPackets()), first);
        appendField(builder, "boostCount", String.valueOf(plant.getBoostCount()), first);
        builder.append("\n}");

        return builder.toString();
    }

    protected String greenhouseToJson(Greenhouse greenhouse) {
        if (greenhouse == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;

        builder.append("{");
        first = appendField(builder, "productionAmount", String.valueOf(greenhouse.getProductionAmount()), first);
        first = appendField(builder, "lastHarvestTime", jsonString(greenhouse.getLastHarvestTime()), first);
        appendField(builder, "pots", potsToJson(greenhouse.getAllPots()), first);
        builder.append("\n}");

        return builder.toString();
    }

    protected String potsToJson(List<Greenhouse.Pot> pots) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        if (pots != null) {
            for (int i = 0; i < pots.size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }

                builder.append(potToJson(pots.get(i)));
            }
        }

        builder.append("]");
        return builder.toString();
    }

    protected String potToJson(Greenhouse.Pot pot) {
        if (pot == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;

        builder.append("{");
        first = appendField(builder, "x", String.valueOf(pot.getX()), first);
        first = appendField(builder, "y", String.valueOf(pot.getY()), first);
        first = appendField(builder, "unlocked", String.valueOf(pot.isUnlocked()), first);
        first = appendField(builder, "plantName", jsonString(pot.getPlantName()), first);
        first = appendField(builder, "plantedAt", dateTimeToJson(pot.getPlantedAt()), first);
        first = appendField(builder, "readyAt", dateTimeToJson(pot.getReadyAt()), first);
        first = appendField(builder, "status", jsonString(pot.getStatus()), first);
        appendField(builder, "harvestReward", String.valueOf(pot.getHarvestReward()), first);
        builder.append("\n}");

        return builder.toString();
    }

    protected String questsToJson(List<Quest> quests) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        if (quests != null) {
            for (int i = 0; i < quests.size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }

                builder.append(questToJson(quests.get(i)));
            }
        }

        builder.append("]");
        return builder.toString();
    }

    protected String questToJson(Quest quest) {
        if (quest == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;

        builder.append("{");
        first = appendField(builder, "questDescription", jsonString(quest.getQuestDescription()), first);
        first = appendField(builder, "type", jsonString(quest.getType()), first);
        first = appendField(builder, "conditionDescription", jsonString(quest.getConditionDescription()), first);
        first = appendField(builder, "rewardDescription", jsonString(quest.getRewardDescription()), first);
        first = appendField(builder, "priority", jsonString(quest.getPriority()), first);
        first = appendField(builder, "variables", jsonString(quest.getVariables()), first);
        first = appendField(builder, "progressKey", jsonString(quest.getProgressKey()), first);
        first = appendField(builder, "targetKey", jsonString(quest.getTargetKey()), first);
        first = appendField(builder, "progressAmount", String.valueOf(quest.getProgressAmount()), first);
        first = appendField(builder, "targetAmount", String.valueOf(quest.getTargetAmount()), first);
        first = appendField(builder, "coinReward", String.valueOf(quest.getCoinReward()), first);
        first = appendField(builder, "gemReward", String.valueOf(quest.getGemReward()), first);
        first = appendField(builder, "seedPacketReward", String.valueOf(quest.getSeedPacketReward()), first);
        first = appendField(builder, "randomPlantReward", String.valueOf(quest.hasRandomPlantReward()), first);
        appendField(builder, "rewardClaimed", String.valueOf(quest.isRewardClaimed()), first);
        builder.append("\n}");

        return builder.toString();
    }

    protected String newsListToJson(List<News> newsList) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        if (newsList != null) {
            for (int i = 0; i < newsList.size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }

                builder.append(newsToJson(newsList.get(i)));
            }
        }

        builder.append("]");
        return builder.toString();
    }

    protected String newsToJson(News news) {
        if (news == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;

        builder.append("{");
        first = appendField(builder, "id", jsonString(news.getId()), first);
        first = appendField(builder, "title", jsonString(news.getTitle()), first);
        first = appendField(builder, "content", jsonString(news.getContent()), first);
        first = appendField(builder, "type", jsonString(news.getType()), first);
        first = appendField(builder, "targetName", jsonString(news.getTargetName()), first);
        first = appendField(builder, "coinAmount", String.valueOf(news.getCoinAmount()), first);
        first = appendField(builder, "gemAmount", String.valueOf(news.getGemAmount()), first);
        first = appendField(builder, "read", String.valueOf(news.isRead()), first);
        first = appendField(builder, "createdAt", dateTimeToJson(news.getCreatedAt()), first);
        first = appendField(builder, "readAt", dateTimeToJson(news.getReadAt()), first);
        appendField(builder, "effectsApplied", String.valueOf(news.areEffectsApplied()), first);
        builder.append("\n}");

        return builder.toString();
    }

}
