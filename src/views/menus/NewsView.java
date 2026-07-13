package views.menus;

import models.account.News;

import java.util.List;

public class NewsView {
    public void showUnread(List<News> newsList) {
        System.out.print(renderUnread(newsList));
    }

    public void showAll(List<News> newsList) {
        System.out.print(renderAll(newsList));
    }

    public void showMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        System.out.println(message);
    }

    public String renderUnread(List<News> newsList) {
        return renderNewsList("Unread News", newsList, true);
    }

    public String renderAll(List<News> newsList) {
        return renderNewsList("All News", newsList, false);
    }

    public String renderNews(News news) {
        if (news == null) {
            return "News is not available.\n";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Title: ")
                .append(news.getTitle())
                .append("\n");

        builder.append("Status: ")
                .append(news.isRead() ? "read" : "unread")
                .append("\n");

        builder.append("Type: ")
                .append(news.getType())
                .append("\n");

        if (news.getTargetName() != null && !news.getTargetName().isBlank()) {
            builder.append("Target: ")
                    .append(news.getTargetName())
                    .append("\n");
        }

        if (news.hasReward()) {
            builder.append("Reward: ");

            boolean hasCoin = news.getCoinAmount() > 0;
            boolean hasGem = news.getGemAmount() > 0;

            if (hasCoin) {
                builder.append(news.getCoinAmount())
                        .append(" coins");
            }

            if (hasCoin && hasGem) {
                builder.append(", ");
            }

            if (hasGem) {
                builder.append(news.getGemAmount())
                        .append(" gems");
            }

            builder.append("\n");
        }

        builder.append("Created at: ")
                .append(news.getCreatedAt())
                .append("\n");

        if (news.getReadAt() != null) {
            builder.append("Read at: ")
                    .append(news.getReadAt())
                    .append("\n");
        }

        if (news.getContent() != null && !news.getContent().isBlank()) {
            builder.append("Content: ")
                    .append(news.getContent())
                    .append("\n");
        }

        return builder.toString();
    }

    private String renderNewsList(String title, List<News> newsList, boolean unreadOnly) {
        StringBuilder builder = new StringBuilder();

        builder.append(title)
                .append("\n");
        builder.append(repeat("=", title.length()))
                .append("\n");

        if (newsList == null || newsList.isEmpty()) {
            builder.append(unreadOnly ? "No unread news.\n" : "No news available.\n");
            return builder.toString();
        }

        int index = 1;

        for (News news : newsList) {
            if (news == null) {
                continue;
            }

            if (unreadOnly && news.isRead()) {
                continue;
            }

            builder.append(index)
                    .append(". ")
                    .append(news.getTitle())
                    .append(" [")
                    .append(news.isRead() ? "read" : "unread")
                    .append("]\n");

            if (news.getContent() != null && !news.getContent().isBlank()) {
                builder.append("   ")
                        .append(news.getContent())
                        .append("\n");
            }

            if (news.getTargetName() != null && !news.getTargetName().isBlank()) {
                builder.append("   Target: ")
                        .append(news.getTargetName())
                        .append("\n");
            }

            if (news.hasReward()) {
                builder.append("   Reward: ")
                        .append(renderReward(news))
                        .append("\n");
            }

            builder.append("   Created at: ")
                    .append(news.getCreatedAt())
                    .append("\n");

            index++;
        }

        if (index == 1) {
            builder.append(unreadOnly ? "No unread news.\n" : "No news available.\n");
        }

        return builder.toString();
    }

    private String renderReward(News news) {
        StringBuilder builder = new StringBuilder();

        boolean hasCoin = news.getCoinAmount() > 0;
        boolean hasGem = news.getGemAmount() > 0;

        if (hasCoin) {
            builder.append(news.getCoinAmount())
                    .append(" coins");
        }

        if (hasCoin && hasGem) {
            builder.append(", ");
        }

        if (hasGem) {
            builder.append(news.getGemAmount())
                    .append(" gems");
        }

        return builder.toString();
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < count; i++) {
            builder.append(value);
        }

        return builder.toString();
    }
}