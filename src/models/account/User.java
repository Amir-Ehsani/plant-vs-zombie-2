package models.account;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String password;
    private String passwordHash;
    private String nickname;
    private String email;
    private String gender;
    private int securityQuestionNumber;
    private String securityAnswer;
    private int coins;
    private int gems;
    private int score;
    private int gamesPlayed;
    private int passedLevels;
    private int bestMioPoint;
    private int difficultyLevel;
    private String profileImage;
    private String currentChapterName;
    private List<String> unlockedChapters;
    private List<News> newsList;
    private boolean stayLoggedIn;
    private Collection collection;
    private Greenhouse greenhouse;
    private List<Quest> quests;
    private List<String> completedMiniGameStages;

    public User(String username, String password, String nickname, String email, String gender) {
        this.username = safeText(username);
        this.password = safeText(password);
        this.passwordHash = "";
        this.nickname = safeText(nickname);
        this.email = safeText(email);
        this.gender = safeText(gender);
        this.profileImage = "default";
        this.currentChapterName = "";
        this.unlockedChapters = new ArrayList<>();
        unlockedChapters.add("ancient-egypt");
        this.newsList = new ArrayList<>();
        this.securityQuestionNumber = 0;
        this.securityAnswer = "";
        this.coins = 0;
        this.gems = 0;
        this.score = 0;
        this.gamesPlayed = 0;
        this.passedLevels = 0;
        this.bestMioPoint = 0;
        this.difficultyLevel = 3;
        this.stayLoggedIn = false;
        this.collection = new Collection();
        unlockStarterPlants();
        this.greenhouse = new Greenhouse();
        this.quests = new ArrayList<>();
        this.completedMiniGameStages = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = safeText(username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = safeText(password);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = safeText(passwordHash);
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = safeText(nickname);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = safeText(email);
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = safeText(gender);
    }

    public int getSecurityQuestionNumber() {
        return securityQuestionNumber;
    }

    public void setSecurityQuestionNumber(int securityQuestionNumber) {
        this.securityQuestionNumber = Math.max(0, securityQuestionNumber);
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = safeText(securityAnswer);
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = Math.max(0, coins);
    }

    public void addCoins(int coins) {
        if (coins > 0) {
            this.coins += coins;
        }
    }

    public boolean spendCoins(int coins) {
        if (coins < 0 || this.coins < coins) {
            return false;
        }

        this.coins -= coins;
        return true;
    }

    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = Math.max(0, gems);
    }

    public void addGems(int gems) {
        if (gems > 0) {
            this.gems += gems;
        }
    }

    public boolean spendGems(int gems) {
        if (gems < 0 || this.gems < gems) {
            return false;
        }

        this.gems -= gems;
        return true;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = Math.max(0, score);
    }

    public void addScore(int score) {
        if (score > 0) {
            this.score += score;
        }
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = Math.max(0, gamesPlayed);
    }

    public void increaseGamesPlayed() {
        gamesPlayed++;
    }

    public int getPassedLevels() {
        return passedLevels;
    }

    public void setPassedLevels(int passedLevels) {
        this.passedLevels = Math.max(0, passedLevels);
    }

    public void increasePassedLevels() {
        passedLevels++;
    }

    public int getBestMioPoint() {
        return bestMioPoint;
    }

    public void setBestMioPoint(int bestMioPoint) {
        this.bestMioPoint = Math.max(0, bestMioPoint);
    }

    public void updateBestMioPoint(int mioPoint) {
        if (mioPoint > bestMioPoint) {
            bestMioPoint = mioPoint;
        }
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        if (difficultyLevel < 1) {
            this.difficultyLevel = 1;
            return;
        }

        if (difficultyLevel > 5) {
            this.difficultyLevel = 5;
            return;
        }

        this.difficultyLevel = difficultyLevel;
    }

    public boolean isStayLoggedIn() {
        return stayLoggedIn;
    }

    public void setStayLoggedIn(boolean stayLoggedIn) {
        this.stayLoggedIn = stayLoggedIn;
    }

    public Collection getCollection() {
        if (collection == null) {
            collection = new Collection();
            unlockStarterPlants();
        }

        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;

        if (this.collection == null) {
            this.collection = new Collection();
        }

        unlockStarterPlantsIfCollectionIsEmpty();
    }

    public Greenhouse getGreenhouse() {
        if (greenhouse == null) {
            greenhouse = new Greenhouse();
        }

        return greenhouse;
    }

    public void setGreenhouse(Greenhouse greenhouse) {
        this.greenhouse = greenhouse;
    }

    public List<Quest> getQuests() {
        if (quests == null) {
            quests = new ArrayList<>();
        }

        return quests;
    }

    public void setQuests(List<Quest> quests) {
        if (quests == null) {
            this.quests = new ArrayList<>();
            return;
        }

        this.quests = quests;
    }

    public void addQuest(Quest quest) {
        if (quest != null) {
            getQuests().add(quest);
        }
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        String cleanedProfileImage = safeText(profileImage);

        if (cleanedProfileImage.isEmpty()) {
            this.profileImage = "default";
            return;
        }

        this.profileImage = cleanedProfileImage;
    }

    public String getCurrentChapterName() {
        return currentChapterName;
    }

    public void setCurrentChapterName(String currentChapterName) {
        this.currentChapterName = safeText(currentChapterName);
    }

    public List<String> getUnlockedChapters() {
        if (unlockedChapters == null) {
            unlockedChapters = new ArrayList<>();
        }

        return new ArrayList<>(unlockedChapters);
    }

    public void setUnlockedChapters(List<String> unlockedChapters) {
        this.unlockedChapters = new ArrayList<>();

        if (unlockedChapters == null) {
            return;
        }

        for (String chapterName : unlockedChapters) {
            unlockChapter(chapterName);
        }
    }

    public void unlockChapter(String chapterName) {
        if (unlockedChapters == null) {
            unlockedChapters = new ArrayList<>();
        }

        String cleanedChapterName = safeText(chapterName);

        if (cleanedChapterName.isEmpty()) {
            return;
        }

        if (!unlockedChapters.contains(cleanedChapterName)) {
            unlockedChapters.add(cleanedChapterName);
        }
    }

    public boolean isChapterUnlocked(String chapterName) {
        String cleanedChapterName = safeText(chapterName);

        for (String unlockedChapter : getUnlockedChapters()) {
            if (unlockedChapter.equalsIgnoreCase(cleanedChapterName)) {
                return true;
            }
        }

        return false;
    }

    public List<String> getCompletedMiniGameStages() {
        if (completedMiniGameStages == null) {
            completedMiniGameStages = new ArrayList<>();
        }

        return new ArrayList<>(completedMiniGameStages);
    }

    public void setCompletedMiniGameStages(List<String> completedMiniGameStages) {
        this.completedMiniGameStages = new ArrayList<>();

        if (completedMiniGameStages == null) {
            return;
        }

        for (String stageKey : completedMiniGameStages) {
            String normalizedKey = normalizeMiniGameStageKey(stageKey);

            if (!normalizedKey.isEmpty() && !this.completedMiniGameStages.contains(normalizedKey)) {
                this.completedMiniGameStages.add(normalizedKey);
            }
        }
    }

    public boolean completeMiniGameStage(String miniGameName, int stage) {
        if (stage < 1 || stage > 3) {
            return false;
        }

        if (completedMiniGameStages == null) {
            completedMiniGameStages = new ArrayList<>();
        }

        String stageKey = miniGameStageKey(miniGameName, stage);

        if (stageKey.isEmpty() || completedMiniGameStages.contains(stageKey)) {
            return false;
        }

        completedMiniGameStages.add(stageKey);
        return true;
    }

    public boolean isMiniGameStageCompleted(String miniGameName, int stage) {
        if (stage < 1 || stage > 3) {
            return false;
        }

        if (completedMiniGameStages == null) {
            completedMiniGameStages = new ArrayList<>();
        }

        return completedMiniGameStages.contains(miniGameStageKey(miniGameName, stage));
    }

    public boolean isMiniGameStageUnlocked(String miniGameName, int stage) {
        if (stage < 1 || stage > 3) {
            return false;
        }

        return stage == 1 || isMiniGameStageCompleted(miniGameName, stage - 1);
    }

    public int getCompletedMiniGameStageCount() {
        if (completedMiniGameStages == null) {
            return 0;
        }

        return completedMiniGameStages.size();
    }

    public List<News> getNewsList() {
        if (newsList == null) {
            newsList = new ArrayList<>();
        }

        return new ArrayList<>(newsList);
    }

    public void setNewsList(List<News> newsList) {
        if (newsList == null) {
            this.newsList = new ArrayList<>();
            return;
        }

        this.newsList = newsList;
    }

    public void addNews(News news) {
        if (news != null) {
            if (newsList == null) {
                newsList = new ArrayList<>();
            }

            newsList.add(news);
        }
    }

    private void unlockStarterPlants() {
        getCollection().addPlant(new PlantData("Sunflower", 2000, true));
        getCollection().addPlant(new PlantData("Peashooter", 2000, true));
        getCollection().addPlant(new PlantData("Wall-nut", 2000, true));
        getCollection().addPlant(new PlantData("Potato Mine", 2000, true));
        getCollection().addPlant(new PlantData("Cabbage-pult", 2000, true));
        getCollection().addPlant(new PlantData("Kernel-pult", 2000, true));
        getCollection().addPlant(new PlantData("Iceberg Lettuce", 2000, true));
        getCollection().addPlant(new PlantData("Bonk Choy", 2000, true));
        getCollection().addPlant(new PlantData("Cherry Bomb", 2000, true));
    }

    private void unlockStarterPlantsIfCollectionIsEmpty() {
        if (collection == null) {
            collection = new Collection();
        }

        if (collection.getOwnedPlants().isEmpty()) {
            unlockStarterPlants();
        }
    }

    private String miniGameStageKey(String miniGameName, int stage) {
        String normalizedName = normalizeMiniGameName(miniGameName);

        if (normalizedName.isEmpty()) {
            return "";
        }

        return normalizedName + ":" + stage;
    }

    private String normalizeMiniGameStageKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        int separatorIndex = value.lastIndexOf(':');

        if (separatorIndex <= 0 || separatorIndex >= value.length() - 1) {
            return "";
        }

        String name = value.substring(0, separatorIndex);
        String stageText = value.substring(separatorIndex + 1);

        try {
            int stage = Integer.parseInt(stageText.trim());
            return stage < 1 || stage > 3 ? "" : miniGameStageKey(name, stage);
        } catch (NumberFormatException exception) {
            return "";
        }
    }

    private String normalizeMiniGameName(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim()
                .toLowerCase()
                .replace('_', '-')
                .replace(' ', '-')
                .replace(",", "")
                .replaceAll("-+", "-");

        if ("wall-nut-bowling".equals(normalized)
                || "wallnutbowling".equals(normalized)
                || "bowling".equals(normalized)) {
            return "wallnut-bowling";
        }

        if ("i-zombie".equals(normalized)
                || "izombie".equals(normalized)
                || "i--zombie".equals(normalized)) {
            return "i-zombie";
        }

        if ("vase-breaker".equals(normalized)) {
            return "vasebreaker";
        }

        return normalized;
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}