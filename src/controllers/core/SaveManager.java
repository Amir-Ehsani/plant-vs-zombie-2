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

public class SaveManager {
    private static final Path SAVE_DIRECTORY = Path.of("data");
    private static final Path USERS_FILE = SAVE_DIRECTORY.resolve("users.json");

    public void saveAllUsers(List<User> users) {
        try {
            Files.createDirectories(SAVE_DIRECTORY);
            Files.writeString(USERS_FILE, usersToJson(users), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public List<User> loadAllUsers() {
        if (!Files.exists(USERS_FILE)) {
            return new ArrayList<>();
        }

        try {
            String json = Files.readString(USERS_FILE, StandardCharsets.UTF_8);
            Object parsed = new JsonParser(json).parse();

            if (!(parsed instanceof List<?> parsedUsers)) {
                return new ArrayList<>();
            }

            List<User> users = new ArrayList<>();

            for (Object parsedUser : parsedUsers) {
                User user = mapToUser(parsedUser);

                if (user != null) {
                    users.add(user);
                }
            }

            return users;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private String usersToJson(List<User> users) {
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

    private String userToJson(User user) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;

        builder.append("{");

        first = appendField(builder, "username", jsonString(user.getUsername()), first);
        first = appendField(builder, "password", jsonString(user.getPassword()), first);
        first = appendField(builder, "passwordHash", jsonString(user.getPasswordHash()), first);
        first = appendField(builder, "nickname", jsonString(user.getNickname()), first);
        first = appendField(builder, "email", jsonString(user.getEmail()), first);
        first = appendField(builder, "gender", jsonString(user.getGender()), first);
        first = appendField(builder, "securityQuestionNumber", String.valueOf(user.getSecurityQuestionNumber()), first);
        first = appendField(builder, "securityAnswer", jsonString(user.getSecurityAnswer()), first);
        first = appendField(builder, "coins", String.valueOf(user.getCoins()), first);
        first = appendField(builder, "gems", String.valueOf(user.getGems()), first);
        first = appendField(builder, "score", String.valueOf(user.getScore()), first);
        first = appendField(builder, "gamesPlayed", String.valueOf(user.getGamesPlayed()), first);
        first = appendField(builder, "passedLevels", String.valueOf(user.getPassedLevels()), first);
        first = appendField(builder, "bestMioPoint", String.valueOf(user.getBestMioPoint()), first);
        first = appendField(builder, "difficultyLevel", String.valueOf(user.getDifficultyLevel()), first);
        first = appendField(builder, "profileImage", jsonString(user.getProfileImage()), first);
        first = appendField(builder, "currentChapterName", jsonString(user.getCurrentChapterName()), first);
        first = appendField(builder, "stayLoggedIn", String.valueOf(user.isStayLoggedIn()), first);
        first = appendField(builder, "unlockedChapters", stringListToJson(user.getUnlockedChapters()), first);
        first = appendField(builder, "completedMiniGameStages", stringListToJson(user.getCompletedMiniGameStages()), first);
        first = appendField(builder, "collection", collectionToJson(user.getCollection()), first);
        first = appendField(builder, "greenhouse", greenhouseToJson(user.getGreenhouse()), first);
        first = appendField(builder, "quests", questsToJson(user.getQuests()), first);
        appendField(builder, "newsList", newsListToJson(user.getNewsList()), first);

        builder.append("\n}");
        return builder.toString();
    }

    private String collectionToJson(Collection collection) {
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
        appendField(builder, "dailyOfferPurchased", String.valueOf(collection.isDailyOfferPurchased()), first);
        builder.append("\n}");

        return builder.toString();
    }

    private String plantsToJson(List<PlantData> plants) {
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

    private String plantToJson(PlantData plant) {
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

    private String greenhouseToJson(Greenhouse greenhouse) {
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

    private String potsToJson(List<Greenhouse.Pot> pots) {
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

    private String potToJson(Greenhouse.Pot pot) {
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

    private String questsToJson(List<Quest> quests) {
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

    private String questToJson(Quest quest) {
        if (quest == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;

        builder.append("{");
        first = appendField(builder, "questDescription", jsonString(quest.getQuestDescription()), first);
        first = appendField(builder, "type", jsonString(quest.getType()), first);
        first = appendField(builder, "progressAmount", String.valueOf(quest.getProgressAmount()), first);
        first = appendField(builder, "targetAmount", String.valueOf(quest.getTargetAmount()), first);
        first = appendField(builder, "coinReward", String.valueOf(quest.getCoinReward()), first);
        first = appendField(builder, "gemReward", String.valueOf(quest.getGemReward()), first);
        appendField(builder, "rewardClaimed", String.valueOf(quest.isRewardClaimed()), first);
        builder.append("\n}");

        return builder.toString();
    }

    private String newsListToJson(List<News> newsList) {
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

    private String newsToJson(News news) {
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

    private User mapToUser(Object object) {
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
        user.setCollection(mapToCollection(map.get("collection")));
        user.setGreenhouse(mapToGreenhouse(map.get("greenhouse")));
        user.setQuests(mapToQuests(map.get("quests")));
        user.setNewsList(mapToNewsList(map.get("newsList")));

        return user;
    }

    private Collection mapToCollection(Object object) {
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

        return collection;
    }

    private PlantData mapToPlant(Object object, boolean defaultUnlocked) {
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

    private Greenhouse mapToGreenhouse(Object object) {
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

    private List<Quest> mapToQuests(Object object) {
        List<Quest> quests = new ArrayList<>();

        for (Object questObject : asList(object)) {
            Map<String, Object> map = asMap(questObject);

            if (map == null) {
                continue;
            }

            Quest quest = new Quest(
                    string(map, "questDescription"),
                    string(map, "type"),
                    integer(map, "targetAmount", 1),
                    integer(map, "coinReward", 0),
                    integer(map, "gemReward", 0)
            );

            quest.setProgressAmount(integer(map, "progressAmount", 0));
            setPrivateField(quest, "rewardClaimed", bool(map, "rewardClaimed", false));

            quests.add(quest);
        }

        return quests;
    }

    private List<News> mapToNewsList(Object object) {
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

    private boolean appendField(StringBuilder builder, String name, String value, boolean first) {
        if (!first) {
            builder.append(",");
        }

        builder.append("\n  ")
                .append(jsonString(name))
                .append(": ")
                .append(value);

        return false;
    }

    private String stringListToJson(List<String> values) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }

                builder.append(jsonString(values.get(i)));
            }
        }

        builder.append("]");
        return builder.toString();
    }

    private String dateTimeToJson(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "null";
        }

        return jsonString(dateTime.toString());
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\"");

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            switch (character) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 32) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }

        builder.append("\"");
        return builder.toString();
    }

    private Map<String, Object> asMap(Object object) {
        if (object instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null) {
                    map.put(entry.getKey().toString(), entry.getValue());
                }
            }

            return map;
        }

        return null;
    }

    private List<?> asList(Object object) {
        if (object instanceof List<?> list) {
            return list;
        }

        return new ArrayList<>();
    }

    private List<?> list(Map<String, Object> map, String key) {
        return asList(map.get(key));
    }

    private List<String> stringsFromList(List<?> list) {
        List<String> strings = new ArrayList<>();

        for (Object object : list) {
            if (object != null) {
                strings.add(object.toString());
            }
        }

        return strings;
    }

    private String string(Map<String, Object> map, String key) {
        Object value = map.get(key);

        if (value == null) {
            return "";
        }

        return value.toString();
    }

    private int integer(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
            }
        }

        return defaultValue;
    }

    private boolean bool(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);

        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }

        return defaultValue;
    }

    private LocalDateTime dateTime(Map<String, Object> map, String key) {
        String value = string(map, key);

        if (value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        if (target == null) {
            return;
        }

        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ignored) {
        }
    }

    private static final class JsonParser {
        private final String text;
        private int index;

        private JsonParser(String text) {
            this.text = text == null ? "" : text;
            this.index = 0;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            return value;
        }

        private Object parseValue() {
            skipWhitespace();

            if (index >= text.length()) {
                return null;
            }

            char character = text.charAt(index);

            if (character == '{') {
                return parseObject();
            }

            if (character == '[') {
                return parseArray();
            }

            if (character == '"') {
                return parseString();
            }

            if (startsWith("true")) {
                index += 4;
                return true;
            }

            if (startsWith("false")) {
                index += 5;
                return false;
            }

            if (startsWith("null")) {
                index += 4;
                return null;
            }

            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++;
            skipWhitespace();

            if (peek('}')) {
                index++;
                return map;
            }

            while (index < text.length()) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();

                if (peek('}')) {
                    index++;
                    break;
                }

                expect(',');
            }

            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            index++;
            skipWhitespace();

            if (peek(']')) {
                index++;
                return list;
            }

            while (index < text.length()) {
                list.add(parseValue());
                skipWhitespace();

                if (peek(']')) {
                    index++;
                    break;
                }

                expect(',');
            }

            return list;
        }

        private String parseString() {
            StringBuilder builder = new StringBuilder();
            expect('"');

            while (index < text.length()) {
                char character = text.charAt(index++);

                if (character == '"') {
                    break;
                }

                if (character != '\\') {
                    builder.append(character);
                    continue;
                }

                if (index >= text.length()) {
                    break;
                }

                char escaped = text.charAt(index++);

                switch (escaped) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        String hex = text.substring(index, Math.min(index + 4, text.length()));
                        index += Math.min(4, hex.length());

                        try {
                            builder.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    default -> builder.append(escaped);
                }
            }

            return builder.toString();
        }

        private Number parseNumber() {
            int start = index;

            while (index < text.length()) {
                char character = text.charAt(index);

                if ((character >= '0' && character <= '9')
                        || character == '-'
                        || character == '+'
                        || character == '.'
                        || character == 'e'
                        || character == 'E') {
                    index++;
                } else {
                    break;
                }
            }

            String number = text.substring(start, index);

            try {
                if (number.contains(".") || number.contains("e") || number.contains("E")) {
                    return Double.parseDouble(number);
                }

                return Long.parseLong(number);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        private void expect(char expected) {
            skipWhitespace();

            if (index < text.length() && text.charAt(index) == expected) {
                index++;
            }
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return index < text.length() && text.charAt(index) == expected;
        }

        private boolean startsWith(String value) {
            return text.startsWith(value, index);
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }
    }
}
