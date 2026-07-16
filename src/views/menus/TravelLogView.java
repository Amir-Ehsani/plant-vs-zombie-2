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
            "^travel\\s+log\\s+page\\s+(.+)\\s*$"
    );

    private static final Pattern COLLECT_PATTERN = Pattern.compile(
            "^travel\\s+log\\s+collect\\s+-q\\s+(\\d+)\\s*$"
    );

    private static final Pattern ENTER_MINIGAME_PATTERN = Pattern.compile(
            "^enter\\s+minigame\\s+-n\\s+(.+)\\s*$"
    );

    private final MenuManager menuManager;
    private final TravelLogController controller;
    private String currentPageName;

    public TravelLogView(String viewName, MenuManager menuManager, TravelLogController controller) {
        super(viewName);
        this.menuManager = menuManager;
        this.controller = controller;
        this.currentPageName = "adventure";
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
            printControllerMessage("ERROR: No user is logged in.");
            menuManager.enterLoginMenu();
            return;
        }

        if (handleNavigation(command)) {
            return;
        }

        if (handlePageCommand(command)) {
            return;
        }

        if (handleCollectCommand(command)) {
            return;
        }

        if (handleEnterMiniGameCommand(command)) {
            return;
        }

        controller.invalidCommand("travel log menu");
        printControllerMessage(controller.getLastMessage());
    }

    private boolean handleNavigation(String command) {
        if ("menu show current".equals(command)) {
            menuManager.showCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        if ("menu exit".equals(command)) {
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
        if ("travel log collect all".equals(command)) {
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

    private boolean handleEnterMiniGameCommand(String command) {
        Matcher matcher = ENTER_MINIGAME_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        controller.enterMiniGame(matcher.group(1));
        printControllerMessage(controller.getLastMessage());
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
            builder.append("enter minigame -n <mini_game_name>\n");
        }

        builder.append("menu show current\n");
        builder.append("menu exit\n");

        return builder.toString();
    }

    private String renderQuests(List<Quest> quests) {
        StringBuilder builder = new StringBuilder();

        builder.append("Quests\n");
        builder.append("------\n");

        if (quests == null || quests.isEmpty()) {
            builder.append("No quests on this page.\n");
            return builder.toString();
        }

        int index = 1;

        for (Quest quest : quests) {
            builder.append(index).append(". ").append(quest.getQuestDescription()).append("\n");
            builder.append("   Page: ").append(quest.getType()).append("\n");
            builder.append("   Progress: ")
                    .append(quest.getProgressAmount())
                    .append("/")
                    .append(quest.getTargetAmount())
                    .append("\n");

            builder.append("   Status: ");

            if (quest.isRewardClaimed()) {
                builder.append("reward collected");
            } else if (quest.canClaimReward()) {
                builder.append("done - reward available");
            } else if (quest.isCompleted()) {
                builder.append("done");
            } else {
                builder.append("not done");
            }

            builder.append("\n");

            builder.append("   Reward: ")
                    .append(quest.getCoinReward())
                    .append(" coins, ")
                    .append(quest.getGemReward())
                    .append(" gems\n");

            index++;
        }

        return builder.toString();
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
            builder.append("- ")
                    .append(miniGame.getDisplayName())
                    .append(" | command: enter minigame -n ")
                    .append(miniGame.getName())
                    .append("\n");
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