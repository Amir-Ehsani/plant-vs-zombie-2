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
        this.greenhouse = new Greenhouse();
        this.quests = new ArrayList<>();
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
        }

        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
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
            quests.add(quest);
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

        for (String unlockedChapter : unlockedChapters) {
            if (unlockedChapter.equalsIgnoreCase(cleanedChapterName)) {
                return true;
            }
        }

        return false;
    }

    public List<News> getNewsList() {
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
            newsList.add(news);
        }
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}