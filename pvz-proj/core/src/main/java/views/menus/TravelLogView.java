package views.menus;

import controllers.core.MenuManager;
import controllers.features.TravelLogController;
import models.account.Quest;
import views.core.BaseView;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TravelLogView extends BaseView {
    private static final Pattern PAGE_PATTERN = Pattern.compile(
            "^travel\\s+log\\s+page\\s+(.+)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COLLECT_PATTERN = Pattern.compile(
            "^travel\\s+log\\s+collect\\s+-q\\s+(\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ENTER_MINIGAME_PATTERN = Pattern.compile(
            "^enter\\s+minigame\\s+-n\\s+(.+?)(?:\\s+-s\\s+(\\d+))?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private final MenuManager menuManager;
    private final TravelLogController controller;
    private String currentPageName;

    public TravelLogView(
            String viewName,
            MenuManager menuManager,
            TravelLogController controller
    ) {
        super(viewName);
        this.menuManager = menuManager;
        this.controller = controller;
        this.currentPageName = "all";
    }

    @Override
    public void display() {
        menuManager.showTravelLogMenuText();
        printControllerMessage(menuManager.getLastMessage());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);

        if (!controller.isLoggedIn()) {
            controller.invalidCommand("travel log menu");
            printControllerMessage(controller.getLastMessage());
            menuManager.enterLoginMenu();
            return;
        }

        if (handleNavigation(command)
                || handlePageCommand(command)
                || handleCollectCommand(command)
                || handleMiniGameListCommand(command)
                || handleEnterMiniGameCommand(command)) {
            return;
        }

        controller.invalidCommand("travel log menu");
        printControllerMessage(controller.getLastMessage());
    }

    private boolean handleNavigation(String command) {
        if ("menu show current".equalsIgnoreCase(command)) {
            menuManager.showCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        if ("menu exit".equalsIgnoreCase(command)) {
            menuManager.exitCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        return false;
    }

    private boolean handlePageCommand(String command) {
        Matcher matcher = PAGE_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        currentPageName = matcher.group(1).trim();
        List<Quest> quests = controller.showPage(currentPageName);

        if (controller.wasSuccessful()) {
            System.out.print(renderPage(currentPageName, quests));
        }

        printControllerMessage(controller.getLastMessage());
        return true;
    }

    private boolean handleCollectCommand(String command) {
        if ("travel log collect all".equalsIgnoreCase(command)) {
            controller.collectAllDoneRewards(currentPageName);
            printControllerMessage(controller.getLastMessage());

            if (controller.wasSuccessful()) {
                List<Quest> quests = controller.showPage(currentPageName);
                System.out.print(renderPage(currentPageName, quests));
            }

            return true;
        }

        Matcher matcher = COLLECT_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        Integer questNumber = parseInteger(matcher.group(1));

        if (questNumber == null) {
            controller.invalidCommand("travel log menu");
            printControllerMessage(controller.getLastMessage());
            return true;
        }

        controller.collectQuestReward(currentPageName, questNumber);
        printControllerMessage(controller.getLastMessage());

        if (controller.wasSuccessful()) {
            List<Quest> quests = controller.showPage(currentPageName);
            System.out.print(renderPage(currentPageName, quests));
        }

        return true;
    }

    private boolean handleMiniGameListCommand(String command) {
        if (!"show minigames".equalsIgnoreCase(command)) {
            return false;
        }

        System.out.print(renderMiniGames(controller.getMiniGames()));
        return true;
    }

    private boolean handleEnterMiniGameCommand(String command) {
        Matcher matcher = ENTER_MINIGAME_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        String miniGameName = matcher.group(1).trim();
        String stageText = matcher.group(2);

        boolean started;

        if (stageText == null) {
            started = controller.enterMiniGame(miniGameName);
        } else {
            Integer stage = parseInteger(stageText);

            if (stage == null) {
                controller.invalidCommand("travel log menu");
                printControllerMessage(controller.getLastMessage());
                return true;
            }

            started = controller.enterMiniGame(miniGameName, stage);
        }

        printControllerMessage(controller.getLastMessage());

        if (started) {
            menuManager.enterMiniGameMenu();
        }

        return true;
    }

    private String renderPage(String pageName, List<Quest> quests) {
        String title = "Travel Log - " + pageName.trim();
        StringBuilder builder = new StringBuilder();

        builder.append(title).append("\n");
        builder.append("=".repeat(title.length())).append("\n");
        builder.append(renderQuests(quests));

        if (isMinigamesPage(pageName)) {
            builder.append("\n");
            builder.append(renderMiniGames(controller.getMiniGames()));
        }

        builder.append("\nCommands:\n");
        builder.append("travel log page <page_name>\n");
        builder.append("travel log collect -q <quest_number>\n");
        builder.append("travel log collect all\n");

        if (isMinigamesPage(pageName)) {
            builder.append("show minigames\n");
            builder.append("enter minigame -n <mini_game_name> [-s <stage>]\n");
        }

        builder.append("menu show current\n");
        builder.append("menu exit\n");

        return builder.toString();
    }

    private String renderQuests(List<Quest> quests) {
        StringBuilder builder = new StringBuilder("Quests\n------\n");
        if (quests == null || quests.isEmpty()) {
            return builder.append("No quests on this page.\n").toString();
        }
        for (int index = 0; index < quests.size(); index++) {
            appendQuest(builder, quests.get(index), index + 1);
        }
        return builder.toString();
    }

    private void appendQuest(StringBuilder builder, Quest quest, int index) {
        builder.append(index).append(". ").append(quest.getQuestDescription()).append("\n");
        builder.append("   Page: ").append(quest.getType()).append("\n");
        builder.append("   Priority: ").append(quest.getPriority()).append("\n");
        appendOptionalQuestLine(builder, "Condition", quest.getConditionDescription());
        appendOptionalQuestLine(builder, "Variables", quest.getVariables());
        builder.append("   Progress: ").append(quest.getProgressAmount())
                .append("/").append(quest.getTargetAmount()).append("\n");
        builder.append("   Status: ").append(questStatus(quest)).append("\n");
        builder.append("   Reward: ").append(quest.rewardText()).append("\n");
        builder.append("   Applied reward: ").append(quest.getCoinReward()).append(" coins, ")
                .append(quest.getGemReward()).append(" gems, ")
                .append(quest.getSeedPacketReward()).append(" seed packets");
        if (quest.hasRandomPlantReward()) builder.append(", random plant");
        builder.append("\n");
    }

    private void appendOptionalQuestLine(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append("   ").append(label).append(": ").append(value).append("\n");
        }
    }

    private String questStatus(Quest quest) {
        if (quest.isRewardClaimed()) return "reward collected";
        if (quest.canClaimReward()) return "done - reward available";
        if (quest.isCompleted()) return "done";
        return "not done";
    }


    private String renderMiniGames(List<TravelLogController.MiniGameInfo> miniGames) {
        StringBuilder builder = new StringBuilder();

        builder.append("Mini-games\n");
        builder.append("----------\n");

        if (miniGames == null || miniGames.isEmpty()) {
            builder.append("No mini-games available.\n");
            return builder.toString();
        }

        for (TravelLogController.MiniGameInfo miniGame : miniGames) {
            builder.append(miniGame.getDisplayName())
                    .append(" | name=")
                    .append(miniGame.getName())
                    .append("\n");

            for (TravelLogController.MiniGameStageInfo stage : miniGame.getStages()) {
                builder.append("  stage ")
                        .append(stage.getStage())
                        .append(": ");

                if (stage.isCompleted()) {
                    builder.append("completed");
                } else if (stage.isUnlocked()) {
                    builder.append("unlocked");
                } else {
                    builder.append("locked");
                }

                builder.append("\n");
            }

            builder.append("  command: enter minigame -n ")
                    .append(miniGame.getName())
                    .append(" -s <1|2|3>\n");
        }

        return builder.toString();
    }

    private boolean isMinigamesPage(String pageName) {
        if (pageName == null) {
            return false;
        }

        String cleanedPageName = pageName.trim()
                .toLowerCase()
                .replace("_", "-")
                .replace(" ", "-");

        return "minigames".equals(cleanedPageName)
                || "minigame".equals(cleanedPageName)
                || "mini-game".equals(cleanedPageName)
                || "mini-games".equals(cleanedPageName);
    }
}
