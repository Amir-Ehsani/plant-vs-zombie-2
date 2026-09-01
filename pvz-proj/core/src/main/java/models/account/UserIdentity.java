package models.account;

import java.util.ArrayList;
import java.util.List;

abstract class UserIdentity {
    protected String username;
    protected String password;
    protected String passwordHash;
    protected String nickname;
    protected String email;
    protected String gender;
    protected int securityQuestionNumber;
    protected String securityAnswer;
    protected int coins;
    protected int gems;
    protected int score;
    protected int gamesPlayed;
    protected int passedLevels;
    protected int bestMioPoint;
    protected Settings settings;
    protected String profileImage;
    protected String currentChapterName;
    protected List<String> unlockedChapters;
    protected List<News> newsList;
    protected boolean stayLoggedIn;
    protected Collection collection;
    protected Greenhouse greenhouse;
    protected List<Quest> quests;
    protected List<String> completedMiniGameStages;
    protected List<String> completedChapterLevels;
    protected int currentChapterLevel;
    protected boolean allAdventureLevelsUnlocked;

    protected UserIdentity(String username, String password, String nickname, String email, String gender) {
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
        this.settings = new Settings();
        this.stayLoggedIn = false;
        this.collection = new Collection();
        unlockStarterPlants();
        this.greenhouse = new Greenhouse();
        this.quests = new ArrayList<>();
        this.completedMiniGameStages = new ArrayList<>();
        this.completedChapterLevels = new ArrayList<>();
        this.currentChapterLevel = 1;
        this.allAdventureLevelsUnlocked = false;
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
        return getSettings().getDifficulty();
    }

    public void setDifficultyLevel(int difficultyLevel) {
        getSettings().setDifficulty(difficultyLevel);
    }

    public Settings getSettings() {
        if (settings == null) {
            settings = new Settings();
        }
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings == null ? new Settings() : settings;
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

    protected void unlockStarterPlants() {
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

    protected void unlockStarterPlantsIfCollectionIsEmpty() {
        if (collection == null) {
            collection = new Collection();
        }

        if (collection.getOwnedPlants().isEmpty()) {
            unlockStarterPlants();
        }
    }

    protected String safeText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
