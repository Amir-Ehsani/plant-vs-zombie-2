package controllers.core;

import models.account.Collection;
import models.account.Greenhouse;
import models.account.News;
import models.account.PlantData;
import models.account.Quest;
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


abstract class SaveManagerReader extends SaveManagerJsonSupport {
    protected User mapToUser(Object object) {
        Map<String, Object> map = asMap(object);

        if (map == null) {
            return null;
        }

        User user = new User(
                string(map, "username"),
                string(map, "password"),
                string(map, "nickname"),
                string(map, "email"),
                string(map, "gender")
        );

        user.setPasswordHash(string(map, "passwordHash"));
        user.setSecurityQuestionNumber(integer(map, "securityQuestionNumber", 0));
        user.setSecurityAnswer(string(map, "securityAnswer"));
        user.setCoins(integer(map, "coins", 0));
        user.setGems(integer(map, "gems", 0));
        user.setScore(integer(map, "score", 0));
        user.setGamesPlayed(integer(map, "gamesPlayed", 0));
        user.setPassedLevels(integer(map, "passedLevels", 0));
        user.setBestMioPoint(integer(map, "bestMioPoint", 0));
        user.setDifficultyLevel(integer(map, "difficultyLevel", 3));
        user.setProfileImage(string(map, "profileImage"));
        user.setCurrentChapterName(string(map, "currentChapterName"));
        user.setStayLoggedIn(bool(map, "stayLoggedIn", false));
        user.setUnlockedChapters(stringsFromList(list(map, "unlockedChapters")));
        user.setCompletedMiniGameStages(stringsFromList(list(map, "completedMiniGameStages")));
        user.setCompletedChapterLevels(stringsFromList(list(map, "completedChapterLevels")));
        user.setCurrentChapterLevel(integer(map, "currentChapterLevel", 1));
        user.setAllAdventureLevelsUnlocked(bool(map, "allAdventureLevelsUnlocked", false));
        user.setCollection(mapToCollection(map.get("collection")));
        user.setGreenhouse(mapToGreenhouse(map.get("greenhouse")));
        user.setQuests(mapToQuests(map.get("quests")));
        user.setNewsList(mapToNewsList(map.get("newsList")));

        return user;
    }

    protected Collection mapToCollection(Object object) {
        Collection collection = new Collection();
        Map<String, Object> map = asMap(object);

        if (map == null) {
            return collection;
        }

        for (Object plantObject : list(map, "ownedPlants")) {
            PlantData plant = mapToPlant(plantObject, true);

            if (plant != null) {
                collection.addPlant(plant);
            }
        }

        for (Object plantObject : list(map, "lockedPlants")) {
            PlantData plant = mapToPlant(plantObject, false);

            if (plant != null) {
                collection.addPlant(plant);
            }
        }

        for (String zombieName : stringsFromList(list(map, "ownedZombies"))) {
            collection.addZombie(zombieName, true);
        }

        for (String zombieName : stringsFromList(list(map, "lockedZombies"))) {
            collection.addZombie(zombieName, false);
        }

        setPrivateField(collection, "storedPlantFood", integer(map, "storedPlantFood", 0));
        setPrivateField(collection, "dailyOfferDate", string(map, "dailyOfferDate"));
        setPrivateField(collection, "dailyOfferPlantName", string(map, "dailyOfferPlantName"));
        setPrivateField(collection, "dailyOfferPurchased", bool(map, "dailyOfferPurchased", false));
        collection.setShopItemAmounts(integerMap(map.get("shopItemAmounts")));

        return collection;
    }

    protected PlantData mapToPlant(Object object, boolean defaultUnlocked) {
        Map<String, Object> map = asMap(object);

        if (map == null) {
            return null;
        }

        PlantData plant = new PlantData(
                string(map, "name"),
                integer(map, "price", 0),
                bool(map, "unlocked", defaultUnlocked)
        );

        plant.setLevel(integer(map, "level", 1));
        plant.addSeedPackets(integer(map, "seedPackets", 0));
        plant.addBoost(integer(map, "boostCount", 0));

        return plant;
    }

    protected Greenhouse mapToGreenhouse(Object object) {
        Greenhouse greenhouse = new Greenhouse();
        Map<String, Object> map = asMap(object);

        if (map == null) {
            return greenhouse;
        }

        setPrivateField(greenhouse, "productionAmount", integer(map, "productionAmount", 0));
        setPrivateField(greenhouse, "lastHarvestTime", string(map, "lastHarvestTime"));

        for (Object potObject : list(map, "pots")) {
            Map<String, Object> potMap = asMap(potObject);

            if (potMap == null) {
                continue;
            }

            int x = integer(potMap, "x", 0);
            int y = integer(potMap, "y", 0);
            Greenhouse.Pot pot = greenhouse.getPot(x, y);

            if (pot == null) {
                continue;
            }

            setPrivateField(pot, "unlocked", bool(potMap, "unlocked", false));
            setPrivateField(pot, "plantName", string(potMap, "plantName"));
            setPrivateField(pot, "plantedAt", dateTime(potMap, "plantedAt"));
            setPrivateField(pot, "readyAt", dateTime(potMap, "readyAt"));
            setPrivateField(pot, "status", string(potMap, "status"));
            setPrivateField(pot, "harvestReward", integer(potMap, "harvestReward", 0));
        }

        return greenhouse;
    }

    protected List<Quest> mapToQuests(Object object) {
        List<Quest> quests = new ArrayList<>();

        for (Object questObject : asList(object)) {
            Map<String, Object> map = asMap(questObject);

            if (map == null) {
                continue;
            }

            Quest quest = new Quest(
                    string(map, "questDescription"),
                    string(map, "type"),
                    string(map, "conditionDescription"),
                    string(map, "rewardDescription"),
                    string(map, "priority"),
                    string(map, "variables"),
                    string(map, "progressKey"),
                    string(map, "targetKey"),
                    integer(map, "targetAmount", 1),
                    integer(map, "coinReward", 0),
                    integer(map, "gemReward", 0),
                    integer(map, "seedPacketReward", 0),
                    bool(map, "randomPlantReward", false)
            );

            quest.setProgressAmount(integer(map, "progressAmount", 0));
            setPrivateField(quest, "rewardClaimed", bool(map, "rewardClaimed", false));

            quests.add(quest);
        }

        return quests;
    }

    protected List<News> mapToNewsList(Object object) {
        List<News> newsList = new ArrayList<>();

        for (Object newsObject : asList(object)) {
            Map<String, Object> map = asMap(newsObject);

            if (map == null) {
                continue;
            }

            News news = new News(
                    string(map, "title"),
                    string(map, "content"),
                    string(map, "type")
            );

            news.setId(string(map, "id"));
            news.setTargetName(string(map, "targetName"));
            news.setCoinAmount(integer(map, "coinAmount", 0));
            news.setGemAmount(integer(map, "gemAmount", 0));
            setPrivateField(news, "read", bool(map, "read", false));
            setPrivateField(news, "createdAt", dateTime(map, "createdAt"));
            setPrivateField(news, "readAt", dateTime(map, "readAt"));
            setPrivateField(news, "effectsApplied", bool(map, "effectsApplied", false));

            newsList.add(news);
        }

        return newsList;
    }

}
