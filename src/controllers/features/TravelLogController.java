package controllers.features;

import controllers.auth.AuthController;
import models.account.Quest;
import models.account.User;
import models.engine.board.Position;
import models.minigame.IZombieGame;
import models.minigame.MiniGameSession;
import models.minigame.MiniGameType;
import models.minigame.VasebreakerGame;
import models.minigame.WallNutBowlingGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TravelLogController {
    private final AuthController authController;
    private MiniGameSession activeMiniGame;
    private String lastMessage;

    public TravelLogController(AuthController authController) {
        this.authController = authController;
        this.activeMiniGame = null;
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

    public boolean enterMiniGame(String miniGameName) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return false;
        }

        MiniGameType type = MiniGameType.fromText(miniGameName);
        if (type == null) {
            return fail("Mini-game " + miniGameName + " does not exist.");
        }

        return enterMiniGame(type, firstPlayableStage(user, type));
    }

    public boolean enterMiniGame(String miniGameName, int stage) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return false;
        }

        MiniGameType type = MiniGameType.fromText(miniGameName);
        if (type == null) {
            return fail("Mini-game " + miniGameName + " does not exist.");
        }

        return enterMiniGame(type, stage);
    }

    public boolean advanceMiniGameTime(int ticks) {
        MiniGameSession session = requireActiveMiniGame();
        if (session == null) {
            return false;
        }

        boolean result = session.advanceTicks(ticks);
        synchronizeMiniGameMessage();
        return result;
    }

    public boolean breakVase(Position position) {
        MiniGameSession session = requireActiveMiniGame();
        if (!(session instanceof VasebreakerGame game)) {
            return fail("break vase is only available in Vasebreaker.");
        }

        boolean result = game.breakVase(position);
        synchronizeMiniGameMessage();
        return result;
    }

    public boolean plantVasebreakerPacket(int packetId, Position position) {
        MiniGameSession session = requireActiveMiniGame();
        if (!(session instanceof VasebreakerGame game)) {
            return fail("plant packet is only available in Vasebreaker.");
        }

        boolean result = game.plantPacket(packetId, position);
        synchronizeMiniGameMessage();
        return result;
    }

    public boolean launchNut(String nutType, Position position) {
        MiniGameSession session = requireActiveMiniGame();
        if (!(session instanceof WallNutBowlingGame game)) {
            return fail("launch nut is only available in Wall-nut Bowling.");
        }

        boolean result = game.launchNut(nutType, position);
        synchronizeMiniGameMessage();
        return result;
    }

    public boolean spawnIZombie(String zombieType, Position position) {
        MiniGameSession session = requireActiveMiniGame();
        if (!(session instanceof IZombieGame game)) {
            return fail("spawn zombie is only available in I, Zombie.");
        }

        boolean result = game.spawnZombie(zombieType, position);
        synchronizeMiniGameMessage();
        return result;
    }

    public String showActiveMiniGameMap() {
        MiniGameSession session = requireActiveMiniGame();
        if (session == null) {
            return lastMessage;
        }

        success(session.renderMap());
        return lastMessage;
    }

    public String showActiveMiniGameStatus() {
        MiniGameSession session = requireActiveMiniGame();
        if (session == null) {
            return lastMessage;
        }

        success(session.renderStatus());
        return lastMessage;
    }

    public String showActiveMiniGameHelp() {
        MiniGameSession session = requireActiveMiniGame();
        if (session == null) {
            return lastMessage;
        }

        success(session.renderHelp()
                + "\nabandon minigame"
                + "\nmenu show current"
                + "\nmenu exit");
        return lastMessage;
    }

    public String showVasebreakerPackets() {
        MiniGameSession session = requireActiveMiniGame();
        if (!(session instanceof VasebreakerGame game)) {
            fail("show packets is only available in Vasebreaker.");
            return lastMessage;
        }

        success(game.renderPackets());
        return lastMessage;
    }

    public String showBowlingNuts() {
        MiniGameSession session = requireActiveMiniGame();
        if (!(session instanceof WallNutBowlingGame game)) {
            fail("show nuts is only available in Wall-nut Bowling.");
            return lastMessage;
        }

        success(game.renderInventory());
        return lastMessage;
    }

    public String showIZombieOptions() {
        MiniGameSession session = requireActiveMiniGame();
        if (!(session instanceof IZombieGame game)) {
            fail("show zombies is only available in I, Zombie.");
            return lastMessage;
        }

        success(game.renderAvailableZombies());
        return lastMessage;
    }

    public String showIZombieSun() {
        MiniGameSession session = requireActiveMiniGame();
        if (!(session instanceof IZombieGame game)) {
            fail("show sun is only available in I, Zombie.");
            return lastMessage;
        }

        success("Current sun: " + game.getSunAmount());
        return lastMessage;
    }

    public boolean abandonMiniGame() {
        if (activeMiniGame == null) {
            return fail("No mini-game is active.");
        }

        String gameName = activeMiniGame.getType().getDisplayName();
        int stage = activeMiniGame.getStage();
        boolean finished = !activeMiniGame.isRunning();
        activeMiniGame = null;

        return success((finished ? "Left" : "Abandoned")
                + " " + gameName + " stage " + stage + ".");
    }

    public boolean hasActiveMiniGame() {
        return activeMiniGame != null;
    }

    public boolean isActiveMiniGameRunning() {
        return activeMiniGame != null && activeMiniGame.isRunning();
    }

    public MiniGameType getActiveMiniGameType() {
        return activeMiniGame == null ? null : activeMiniGame.getType();
    }

    public int getActiveMiniGameStage() {
        return activeMiniGame == null ? 0 : activeMiniGame.getStage();
    }

    public String getActiveMiniGameTitle() {
        if (activeMiniGame == null) {
            return "No active mini-game";
        }

        return activeMiniGame.getType().getDisplayName()
                + " - Stage " + activeMiniGame.getStage();
    }

    public List<MiniGameInfo> getMiniGames() {
        User user = authController == null ? null : authController.getLoggedInUser();
        List<MiniGameInfo> miniGames = new ArrayList<>();

        for (MiniGameType type : MiniGameType.values()) {
            List<MiniGameStageInfo> stages = new ArrayList<>();
            for (int stage = 1; stage <= 3; stage++) {
                boolean completed = user != null
                        && user.isMiniGameStageCompleted(type.name(), stage);
                boolean unlocked = user != null
                        && user.isMiniGameStageUnlocked(type.name(), stage);
                stages.add(new MiniGameStageInfo(stage, unlocked, completed));
            }

            miniGames.add(new MiniGameInfo(
                    commandName(type),
                    type.getDisplayName(),
                    stages
            ));
        }

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

    private boolean enterMiniGame(MiniGameType type, int stage) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return false;
        }

        if (stage < 1 || stage > 3) {
            return fail("Mini-game stage must be between 1 and 3.");
        }

        if (activeMiniGame != null && activeMiniGame.isRunning()) {
            return fail("Finish or abandon the current mini-game first.");
        }

        if (!user.isMiniGameStageUnlocked(type.name(), stage)) {
            return fail(type.getDisplayName() + " stage " + stage + " is locked.");
        }

        ensureDefaultQuests(user);

        try {
            activeMiniGame = switch (type) {
                case VASEBREAKER -> new VasebreakerGame(stage);
                case WALLNUT_BOWLING -> new WallNutBowlingGame(stage);
                case I_ZOMBIE -> new IZombieGame(stage);
            };
        } catch (IllegalArgumentException | IllegalStateException exception) {
            activeMiniGame = null;
            return fail(exception.getMessage());
        }

        user.setCurrentChapterName(
                "minigame:" + commandName(type) + ":stage-" + stage
        );
        authController.saveUsers();

        success("Entered " + type.getDisplayName() + " stage " + stage
                + ".\n" + stripMessagePrefix(activeMiniGame.getLastMessage()));
        return true;
    }

    private void synchronizeMiniGameMessage() {
        if (activeMiniGame == null) {
            fail("No mini-game is active.");
            return;
        }

        lastMessage = activeMiniGame.getLastMessage();

        if (!activeMiniGame.consumeCompletionSignal()) {
            return;
        }

        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }

        boolean newCompletion = user.completeMiniGameStage(
                activeMiniGame.getType().name(),
                activeMiniGame.getStage()
        );

        if (newCompletion) {
            updateMiniGameQuest(user, activeMiniGame.getType());
            authController.saveUsers();
        }

        StringBuilder builder = new StringBuilder(lastMessage == null ? "" : lastMessage);
        builder.append("\nStage progress recorded.");

        if (activeMiniGame.getStage() < 3) {
            builder.append(" Stage ")
                    .append(activeMiniGame.getStage() + 1)
                    .append(" is now unlocked.");
        } else {
            builder.append(" All stages of ")
                    .append(activeMiniGame.getType().getDisplayName())
                    .append(" are complete.");
        }

        lastMessage = builder.toString();
    }

    private MiniGameSession requireActiveMiniGame() {
        if (activeMiniGame == null) {
            fail("No mini-game is active.");
            return null;
        }
        return activeMiniGame;
    }

    private void updateMiniGameQuest(User user, MiniGameType type) {
        if (type != MiniGameType.VASEBREAKER) {
            return;
        }

        for (Quest quest : user.getQuests()) {
            if (quest == null || quest.isCompleted()) {
                continue;
            }

            if (quest.matchesType("minigames")
                    && quest.getQuestDescription().equalsIgnoreCase("Win Vasebreaker")) {
                quest.addProgress(1);
                return;
            }
        }
    }

    private int firstPlayableStage(User user, MiniGameType type) {
        for (int stage = 1; stage <= 3; stage++) {
            if (user.isMiniGameStageUnlocked(type.name(), stage)
                    && !user.isMiniGameStageCompleted(type.name(), stage)) {
                return stage;
            }
        }

        return 3;
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

            return firstQuest.getQuestDescription()
                    .compareToIgnoreCase(secondQuest.getQuestDescription());
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

    private String commandName(MiniGameType type) {
        return switch (type) {
            case VASEBREAKER -> "vasebreaker";
            case WALLNUT_BOWLING -> "wallnut-bowling";
            case I_ZOMBIE -> "i-zombie";
        };
    }

    private String stripMessagePrefix(String message) {
        if (message == null) {
            return "";
        }
        if (message.startsWith("OK: ")) {
            return message.substring(4);
        }
        if (message.startsWith("ERROR: ")) {
            return message.substring(7);
        }
        return message;
    }

    private User getLoggedInUserOrFail() {
        if (authController == null || authController.getLoggedInUser() == null) {
            fail("No user is logged in.");
            return null;
        }

        return authController.getLoggedInUser();
    }

    private boolean success(String message) {
        lastMessage = "OK: " + message;
        return true;
    }

    private boolean fail(String message) {
        lastMessage = "ERROR: " + (message == null ? "Unknown error." : message);
        return false;
    }

    public static class MiniGameInfo {
        private final String name;
        private final String displayName;
        private final List<MiniGameStageInfo> stages;

        public MiniGameInfo(
                String name,
                String displayName,
                List<MiniGameStageInfo> stages
        ) {
            this.name = name;
            this.displayName = displayName;
            this.stages = stages == null ? new ArrayList<>() : new ArrayList<>(stages);
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<MiniGameStageInfo> getStages() {
            return new ArrayList<>(stages);
        }
    }

    public static class MiniGameStageInfo {
        private final int stage;
        private final boolean unlocked;
        private final boolean completed;

        public MiniGameStageInfo(int stage, boolean unlocked, boolean completed) {
            this.stage = stage;
            this.unlocked = unlocked;
            this.completed = completed;
        }

        public int getStage() {
            return stage;
        }

        public boolean isUnlocked() {
            return unlocked;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
}
