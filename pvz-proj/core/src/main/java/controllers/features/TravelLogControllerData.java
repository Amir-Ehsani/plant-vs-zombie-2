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


abstract class TravelLogControllerData {
    protected final AuthController authController;
    protected final PlantRegistry plantRegistry;
    protected MiniGameSession activeMiniGame;
    protected String lastMessage;

    protected TravelLogControllerData(AuthController authController) {
        this.authController = authController;
        this.plantRegistry = DefaultPlantRegistry.getInstance();
        this.activeMiniGame = null;
        this.lastMessage = "";
    }


    protected void prepareCollectionPlants(User user) {
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

    protected boolean isValidPage(String pageName) {
        return "all".equals(pageName)
                || "adventure".equals(pageName)
                || "special".equals(pageName)
                || "minigames".equals(pageName)
                || "community".equals(pageName)
                || "challenges".equals(pageName)
                || "mystery".equals(pageName);
    }

    protected String normalizePageName(String pageName) {
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

    protected String displayPageName(String pageName) {
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

    protected String commandName(MiniGameType type) {
        return switch (type) {
            case VASEBREAKER -> "vasebreaker";
            case WALLNUT_BOWLING -> "wallnut-bowling";
            case I_ZOMBIE -> "i-zombie";
            case MATCH_THREE -> "match-3";
            case PLANT_ZOMBIES -> "plant-zombies";
        };
    }

    protected int firstPlayableStage(User user, MiniGameType type) {
        for (int stage = 1; stage <= 3; stage++) {
            if (user.isMiniGameStageUnlocked(type.name(), stage)
                    && !user.isMiniGameStageCompleted(type.name(), stage)) {
                return stage;
            }
        }

        return 3;
    }

    protected String stripMessagePrefix(String message) {
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

    protected String normalizeQuestDescription(String questDescription) {
        if (questDescription == null) {
            return "";
        }

        return questDescription.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    protected User getLoggedInUserOrFail() {
        if (authController == null || authController.getLoggedInUser() == null) {
            fail("No user is logged in.");
            return null;
        }

        return authController.getLoggedInUser();
    }

    protected void saveUsers() {
        if (authController != null) {
            authController.saveUsers();
        }
    }

    protected boolean success(String message) {
        lastMessage = "OK: " + message;
        return true;
    }

    protected boolean fail(String message) {
        lastMessage = "ERROR: " + (message == null ? "Unknown error." : message);
        return false;
    }


    protected static class QuestTemplate {
        protected final String questDescription;
        protected final String type;
        protected final String conditionDescription;
        protected final String rewardDescription;
        protected final String priority;
        protected final String variables;
        protected final String progressKey;
        protected final String targetKey;
        protected final int targetAmount;
        protected final int coinReward;
        protected final int gemReward;
        protected final int seedPacketReward;
        protected final boolean randomPlantReward;

        protected QuestTemplate(
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

        protected Quest createQuest() {
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
        protected final String name;
        protected final String displayName;
        protected final List<MiniGameStageInfo> stages;

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
        protected final int stage;
        protected final boolean unlocked;
        protected final boolean completed;

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
