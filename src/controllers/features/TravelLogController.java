package controllers.features;

import controllers.auth.AuthController;
import models.account.Quest;
import models.account.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TravelLogController {
    private final AuthController authController;
    private String lastMessage;

    public TravelLogController(AuthController authController) {
        this.authController = authController;
        this.lastMessage = "";
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
        user.addCoins(quest.getCoinReward());
        user.addGems(quest.getGemReward());
        authController.saveUsers();

        success("Quest reward collected: "
                + quest.getCoinReward() + " coins and "
                + quest.getGemReward() + " gems.");
    }

    public void collectAllDoneRewards(String pageName) {
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

        int collectedCount = 0;
        int totalCoins = 0;
        int totalGems = 0;

        for (Quest quest : quests) {
            if (quest.canClaimReward()) {
                quest.claimReward();
                user.addCoins(quest.getCoinReward());
                user.addGems(quest.getGemReward());

                totalCoins += quest.getCoinReward();
                totalGems += quest.getGemReward();
                collectedCount++;
            }
        }

        if (collectedCount == 0) {
            fail("There are no done quests with uncollected rewards.");
            return;
        }

        authController.saveUsers();

        success("Collected rewards from " + collectedCount + " quests: "
                + totalCoins + " coins and " + totalGems + " gems.");
    }

    public void enterMiniGame(String miniGameName) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        String normalizedMiniGameName = normalizeMiniGameName(miniGameName);

        if (normalizedMiniGameName.isEmpty()) {
            fail("Mini-game name is required.");
            return;
        }

        if (!miniGameExists(normalizedMiniGameName)) {
            fail("Mini-game " + miniGameName + " does not exist.");
            return;
        }

        user.setCurrentChapterName("minigame:" + normalizedMiniGameName);
        authController.saveUsers();

        success("Entered mini-game " + displayMiniGameName(normalizedMiniGameName) + ".");
    }

    public List<MiniGameInfo> getMiniGames() {
        List<MiniGameInfo> miniGames = new ArrayList<>();

        miniGames.add(new MiniGameInfo("vasebreaker", "Vasebreaker"));
        miniGames.add(new MiniGameInfo("wallnut-bowling", "Wall-nut Bowling"));
        miniGames.add(new MiniGameInfo("mio-score", "Mio Score"));

        return miniGames;
    }

    public boolean isLoggedIn() {
        return authController != null && authController.isLoggedIn();
    }

    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private List<Quest> getQuestsByPage(User user, String pageName) {
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
            int firstPriority = questPriority(firstQuest);
            int secondPriority = questPriority(secondQuest);

            if (firstPriority != secondPriority) {
                return Integer.compare(firstPriority, secondPriority);
            }

            if (firstQuest.canClaimReward() && !secondQuest.canClaimReward()) {
                return -1;
            }

            if (!firstQuest.canClaimReward() && secondQuest.canClaimReward()) {
                return 1;
            }

            return firstQuest.getQuestDescription().compareToIgnoreCase(secondQuest.getQuestDescription());
        });

        return quests;
    }

    private int questPriority(Quest quest) {
        if (quest.matchesType("adventure")) return 1;
        if (quest.matchesType("special")) return 2;
        if (quest.matchesType("challenges")) return 3;
        if (quest.matchesType("minigames")) return 4;
        if (quest.matchesType("community")) return 5;
        if (quest.matchesType("mystery")) return 6;

        return 100;
    }

    private void ensureDefaultQuests(User user) {
        if (user.getQuests() != null && !user.getQuests().isEmpty()) {
            return;
        }

        user.addQuest(new Quest(
                "Finish Ancient Egypt Part 1",
                "adventure",
                3,
                2000,
                10
        ));

        user.addQuest(new Quest(
                "Adventure Extra: Daytime Dark Ages",
                "special",
                10,
                4000,
                0
        ));

        user.addQuest(new Quest(
                "Win Vasebreaker",
                "minigames",
                1,
                1000,
                5
        ));

        user.addQuest(new Quest(
                "Collect from the greenhouse",
                "challenges",
                1,
                500,
                0
        ));

        user.addQuest(new Quest(
                "Find a mystery reward",
                "mystery",
                1,
                0,
                3
        ));

        authController.saveUsers();
    }

    private boolean isValidPage(String pageName) {
        return "all".equals(pageName)
                || "adventure".equals(pageName)
                || "special".equals(pageName)
                || "minigames".equals(pageName)
                || "community".equals(pageName)
                || "challenges".equals(pageName)
                || "mystery".equals(pageName);
    }

    private String normalizePageName(String pageName) {
        if (pageName == null || pageName.isBlank()) {
            return "";
        }

        String normalized = pageName.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "-")
                .replace(" ", "-");

        if ("story".equals(normalized)) {
            return "adventure";
        }

        if ("epic".equals(normalized)) {
            return "special";
        }

        if ("daily".equals(normalized) || "challenge".equals(normalized)) {
            return "challenges";
        }

        if ("minigame".equals(normalized)
                || "mini-game".equals(normalized)
                || "mini-games".equals(normalized)) {
            return "minigames";
        }

        return normalized;
    }

    private String normalizeMiniGameName(String miniGameName) {
        if (miniGameName == null || miniGameName.isBlank()) {
            return "";
        }

        return miniGameName.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "-")
                .replace(" ", "-");
    }

    private boolean miniGameExists(String miniGameName) {
        for (MiniGameInfo miniGame : getMiniGames()) {
            if (miniGame.getName().equals(miniGameName)) {
                return true;
            }
        }

        return false;
    }

    private String displayPageName(String pageName) {
        return switch (pageName) {
            case "adventure" -> "Adventure";
            case "special" -> "Special";
            case "minigames" -> "Minigames";
            case "community" -> "Community";
            case "challenges" -> "Challenges";
            case "mystery" -> "Mystery";
            case "all" -> "All";
            default -> pageName;
        };
    }

    private String displayMiniGameName(String miniGameName) {
        for (MiniGameInfo miniGame : getMiniGames()) {
            if (miniGame.getName().equals(miniGameName)) {
                return miniGame.getDisplayName();
            }
        }

        return miniGameName;
    }

    private User getLoggedInUserOrFail() {
        if (authController == null || authController.getLoggedInUser() == null) {
            fail("No user is logged in.");
            return null;
        }

        return authController.getLoggedInUser();
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }

    public static class MiniGameInfo {
        private final String name;
        private final String displayName;

        public MiniGameInfo(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}