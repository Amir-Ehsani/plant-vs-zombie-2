package views.menus;

import controllers.core.MenuManager;
import controllers.features.NewsController;
import models.account.News;
import views.core.BaseView;

import java.util.List;

public class NewsView extends BaseView {
    private final MenuManager menuManager;
    private final NewsController controller;

    public NewsView(String viewName, MenuManager menuManager, NewsController controller) {
        super(viewName);
        this.menuManager = menuManager;
        this.controller = controller;
    }

    public NewsView() {
        this("News Menu", null, new NewsController());
    }

    @Override
    public void display() {
        System.out.print(menuText());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);
        if (!isConnected()) {
            printControllerMessage("ERROR: News menu is not connected.");
            return;
        }
        if (!controller.isLoggedIn()) {
            printControllerMessage("ERROR: No user is logged in.");
            menuManager.enterLoginMenu();
            return;
        }

        if (handleNavigation(command) || handleNewsCommand(command)) {
            return;
        }
        controller.invalidCommand("news menu");
        printControllerMessage(controller.getLastMessage());
    }

    public String menuText() {
        return """
                News Menu
                menu news show-unread
                menu news show-all
                menu show current
                menu exit
                """;
    }

    public void showUnread(List<News> newsList) {
        System.out.print(renderUnread(newsList));
    }

    public void showAll(List<News> newsList) {
        System.out.print(renderAll(newsList));
    }

    public String renderUnread(List<News> newsList) {
        return renderNewsList("Unread News", newsList, "No unread news.\n");
    }

    public String renderAll(List<News> newsList) {
        return renderNewsList("All News", newsList, "No news available.\n");
    }

    public String renderNews(News news) {
        if (news == null) {
            return "News is not available.\n";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Title: ").append(news.getTitle()).append("\n");
        builder.append("Status: ").append(news.isRead() ? "read" : "unread").append("\n");
        builder.append("Type: ").append(news.getType()).append("\n");
        if (news.hasTarget()) {
            builder.append("Target: ").append(news.getTargetName()).append("\n");
        }
        if (news.hasReward()) {
            builder.append("Reward: ").append(rewardText(news)).append("\n");
        }
        builder.append("Created at: ").append(news.getCreatedAt()).append("\n");
        if (!news.getContent().isBlank()) {
            builder.append("Content: ").append(news.getContent()).append("\n");
        }
        return builder.toString();
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

    private boolean handleNewsCommand(String command) {
        if ("menu news show-unread".equals(command)) {
            showUnread(controller.showUnreadNews());
            return true;
        }
        if ("menu news show-all".equals(command)) {
            showAll(controller.showAllNews());
            return true;
        }
        return false;
    }

    private String renderNewsList(String title, List<News> newsList, String emptyMessage) {
        StringBuilder builder = new StringBuilder(title).append("\n");
        builder.append("=".repeat(title.length())).append("\n");
        if (newsList == null || newsList.isEmpty()) {
            return builder.append(emptyMessage).toString();
        }

        int index = 1;
        for (News news : newsList) {
            if (news == null) {
                continue;
            }
            builder.append(index++).append(". ").append(news.getTitle()).append("\n");
            builder.append("   ").append(news.getContent()).append("\n");
            if (news.hasReward()) {
                builder.append("   Reward: ").append(rewardText(news)).append("\n");
            }
            builder.append("   Created at: ").append(news.getCreatedAt()).append("\n");
        }
        return builder.toString();
    }

    private String rewardText(News news) {
        StringBuilder builder = new StringBuilder();
        if (news.getCoinAmount() > 0) {
            builder.append(news.getCoinAmount()).append(" coins");
        }
        if (news.getCoinAmount() > 0 && news.getGemAmount() > 0) {
            builder.append(", ");
        }
        if (news.getGemAmount() > 0) {
            builder.append(news.getGemAmount()).append(" gems");
        }
        return builder.toString();
    }

    private boolean isConnected() {
        return menuManager != null && controller != null;
    }
}
