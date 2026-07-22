package controllers.features;

import controllers.auth.AuthController;
import models.account.PlantData;
import models.account.Quest;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.engine.board.Position;
import models.minigame.IZombieGame;
import models.minigame.MiniGameSession;
import models.minigame.MiniGameType;
import models.minigame.VasebreakerGame;
import models.minigame.WallNutBowlingGame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TravelLogController {
    private final AuthController authController;
    private final PlantRegistry plantRegistry;
    private MiniGameSession activeMiniGame;
    private String lastMessage;

    public TravelLogController(AuthController authController) {
        this.authController = authController;
        this.plantRegistry = DefaultPlantRegistry.getInstance();
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
        String rewardText = applyQuestReward(user, quest);
        authController.saveUsers();

        success("Quest reward collected: " + rewardText + ".");
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
        int totalSeedPackets = 0;
        int randomPlants = 0;

        for (Quest quest : quests) {
            if (!quest.canClaimReward()) {
                continue;
            }

            quest.claimReward();
            applyQuestReward(user, quest);

            totalCoins += quest.getCoinReward();
            totalGems += quest.getGemReward();
            totalSeedPackets += quest.getSeedPacketReward();

            if (quest.hasRandomPlantReward()) {
                randomPlants++;
            }

            collectedCount++;
        }

        if (collectedCount == 0) {
            fail("There are no done quests with uncollected rewards.");
            return;
        }

        authController.saveUsers();

        success("Collected rewards from " + collectedCount + " quests: "
                + totalCoins + " coins, "
                + totalGems + " gems, "
                + totalSeedPackets + " seed packets, "
                + randomPlants + " random plants.");
    }

    public void recordQuestProgress(User user, String progressKey, int amount) {
        if (user == null || progressKey == null || progressKey.isBlank() || amount <= 0) {
            return;
        }

        ensureDefaultQuests(user);

        for (Quest quest : user.getQuests()) {
            if (quest == null || quest.isCompleted() || quest.isRewardClaimed()) {
                continue;
            }

            if (quest.matchesProgressKey(progressKey)) {
                quest.addProgress(amount);
            }
        }

        saveUsers();
    }

    public void completeQuest(User user, String progressKey) {
        if (user == null || progressKey == null || progressKey.isBlank()) {
            return;
        }

        ensureDefaultQuests(user);

        for (Quest quest : user.getQuests()) {
            if (quest == null || quest.isCompleted() || quest.isRewardClaimed()) {
                continue;
            }

            if (quest.matchesProgressKey(progressKey)) {
                quest.complete();
            }
        }

        saveUsers();
    }

    public void completeQuestWithSeedReward(User user, String progressKey, int seedPacketReward) {
        if (user == null || progressKey == null || progressKey.isBlank()) {
            return;
        }

        ensureDefaultQuests(user);

        for (Quest quest : user.getQuests()) {
            if (quest == null || quest.isCompleted() || quest.isRewardClaimed()) {
                continue;
            }

            if (quest.matchesProgressKey(progressKey)) {
                quest.setSeedPacketReward(seedPacketReward);
                quest.complete();
            }
        }

        saveUsers();
    }

    public void completeQuestWithGemReward(User user, String progressKey, int gemReward) {
        if (user == null || progressKey == null || progressKey.isBlank()) {
            return;
        }

        ensureDefaultQuests(user);

        for (Quest quest : user.getQuests()) {
            if (quest == null || quest.isCompleted() || quest.isRewardClaimed()) {
                continue;
            }

            if (quest.matchesProgressKey(progressKey)) {
                quest.setGemReward(gemReward);
                quest.complete();
            }
        }

        saveUsers();
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
                boolean completed = user != null && user.isMiniGameStageCompleted(type.name(), stage);
                boolean unlocked = user != null && user.isMiniGameStageUnlocked(type.name(), stage);
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

        user.setCurrentChapterName("minigame:" + commandName(type) + ":stage-" + stage);
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
            recordQuestProgress(user, "minigame_stage_completed", 1);
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

    private int pagePriority(Quest quest) {
        if (quest.matchesType("adventure")) return 1;
        if (quest.matchesType("special")) return 2;
        if (quest.matchesType("challenges")) return 3;
        if (quest.matchesType("minigames")) return 4;
        if (quest.matchesType("community")) return 5;
        if (quest.matchesType("mystery")) return 6;

        return 100;
    }

    private int questPriority(Quest quest) {
        String priority = quest.getPriority();

        if (priority == null) {
            return 50;
        }

        String normalizedPriority = priority.trim().toLowerCase(Locale.ROOT);

        if ("critical".equals(normalizedPriority) || "بحرانی".equals(normalizedPriority)) return 1;
        if ("high".equals(normalizedPriority) || "بالا".equals(normalizedPriority)) return 2;
        if ("medium".equals(normalizedPriority) || "متوسط".equals(normalizedPriority)) return 3;
        if ("low".equals(normalizedPriority) || "کم".equals(normalizedPriority)) return 4;

        return 50;
    }

    private void ensureDefaultQuests(User user) {
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

    private boolean removeOldPlaceholderQuests(List<Quest> quests) {
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

    private Quest findQuestByDescription(List<Quest> quests, String questDescription) {
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

    private boolean updateQuestFromTemplate(Quest quest, QuestTemplate template) {
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

    private List<QuestTemplate> getQuestTemplates() {
        List<QuestTemplate> templates = new ArrayList<>();

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

        return templates;
    }

    private String applyQuestReward(User user, Quest quest) {
        user.addCoins(quest.getCoinReward());
        user.addGems(quest.getGemReward());

        StringBuilder reward = new StringBuilder();

        if (quest.getCoinReward() > 0) {
            reward.append(quest.getCoinReward()).append(" coins");
        }

        if (quest.getGemReward() > 0) {
            if (reward.length() > 0) reward.append(", ");
            reward.append(quest.getGemReward()).append(" gems");
        }

        if (quest.getSeedPacketReward() > 0) {
            String plantName = addSeedPacketsToRewardPlant(user, quest.getSeedPacketReward());

            if (reward.length() > 0) reward.append(", ");
            reward.append(quest.getSeedPacketReward()).append(" seed packets");

            if (!plantName.isBlank()) {
                reward.append(" for ").append(plantName);
            }
        }

        if (quest.hasRandomPlantReward()) {
            String unlockedPlantName = unlockRandomPlantReward(user);

            if (reward.length() > 0) reward.append(", ");

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

    private String addSeedPacketsToRewardPlant(User user, int amount) {
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

    private String unlockRandomPlantReward(User user) {
        prepareCollectionPlants(user);

        List<PlantData> lockedPlants = user.getCollection().getLockedPlants();

        if (lockedPlants.isEmpty()) {
            return "";
        }

        String plantName = lockedPlants.get(0).getName();
        user.getCollection().unlockPlant(plantName);
        return plantName;
    }

    private void prepareCollectionPlants(User user) {
        for (PlantType type : plantRegistry.getAllPlantTypes()) {
            if (!user.getCollection().hasPlant(type.getName())) {
                user.getCollection().addPlant(new PlantData(
                        type.getName(),
                        CollectionController.PLANT_PURCHASE_PRICE,
                        CollectionController.getStarterPlantNames().contains(type.getName())
                ));
            }
        }
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

        if ("story".equals(normalized) || "main".equals(normalized) || "اصلی".equals(normalized)) {
            return "adventure";
        }

        if ("epic".equals(normalized)
                || "special-challenge".equals(normalized)
                || "چالش-(epic)".equals(normalized)) {
            return "special";
        }

        if ("daily".equals(normalized)
                || "challenge".equals(normalized)
                || "daily-challenges".equals(normalized)
                || "روزانه".equals(normalized)) {
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
            case "adventure" -> "Adventure / Main";
            case "special" -> "Special / Epic";
            case "minigames" -> "Minigames";
            case "community" -> "Community";
            case "challenges" -> "Daily Challenges";
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

    private int firstPlayableStage(User user, MiniGameType type) {
        for (int stage = 1; stage <= 3; stage++) {
            if (user.isMiniGameStageUnlocked(type.name(), stage)
                    && !user.isMiniGameStageCompleted(type.name(), stage)) {
                return stage;
            }
        }

        return 3;
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

    private String normalizeQuestDescription(String questDescription) {
        if (questDescription == null) {
            return "";
        }

        return questDescription.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private User getLoggedInUserOrFail() {
        if (authController == null || authController.getLoggedInUser() == null) {
            fail("No user is logged in.");
            return null;
        }

        return authController.getLoggedInUser();
    }

    private void saveUsers() {
        if (authController != null) {
            authController.saveUsers();
        }
    }

    private boolean success(String message) {
        lastMessage = "OK: " + message;
        return true;
    }

    private boolean fail(String message) {
        lastMessage = "ERROR: " + (message == null ? "Unknown error." : message);
        return false;
    }

    private static class QuestTemplate {
        private final String questDescription;
        private final String type;
        private final String conditionDescription;
        private final String rewardDescription;
        private final String priority;
        private final String variables;
        private final String progressKey;
        private final String targetKey;
        private final int targetAmount;
        private final int coinReward;
        private final int gemReward;
        private final int seedPacketReward;
        private final boolean randomPlantReward;

        private QuestTemplate(
                String questDescription,
                String type,
                String conditionDescription,
                String rewardDescription,
                String priority,
                String variables,
                String progressKey,
                String targetKey,
                int targetAmount,
                int coinReward,
                int gemReward,
                int seedPacketReward,
                boolean randomPlantReward
        ) {
            this.questDescription = questDescription;
            this.type = type;
            this.conditionDescription = conditionDescription;
            this.rewardDescription = rewardDescription;
            this.priority = priority;
            this.variables = variables;
            this.progressKey = progressKey;
            this.targetKey = targetKey;
            this.targetAmount = targetAmount;
            this.coinReward = coinReward;
            this.gemReward = gemReward;
            this.seedPacketReward = seedPacketReward;
            this.randomPlantReward = randomPlantReward;
        }

        private Quest createQuest() {
            return new Quest(
                    questDescription,
                    type,
                    conditionDescription,
                    rewardDescription,
                    priority,
                    variables,
                    progressKey,
                    targetKey,
                    targetAmount,
                    coinReward,
                    gemReward,
                    seedPacketReward,
                    randomPlantReward
            );
        }
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