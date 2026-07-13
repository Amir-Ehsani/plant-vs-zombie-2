package controllers.features;

import models.account.Collection;
import models.account.News;
import models.account.PlantData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NewsController {
    private final List<News> newsList;
    private String lastMessage;
    private int pendingCoinReward;
    private int pendingGemReward;

    public NewsController() {
        this.newsList = new ArrayList<>();
        this.lastMessage = "";
        this.pendingCoinReward = 0;
        this.pendingGemReward = 0;
    }

    public void addNewNews(News news) {
        if (news == null) {
            fail("News is not available.");
            return;
        }

        newsList.add(news);
        success("News added.");
    }

    public List<News> getAllNews() {
        List<News> result = new ArrayList<>(newsList);
        result.sort(Comparator.comparing(News::getCreatedAt).reversed());
        return result;
    }

    public List<News> getUnreadNews() {
        List<News> result = new ArrayList<>();

        for (News news : newsList) {
            if (news != null && news.isUnread()) {
                result.add(news);
            }
        }

        result.sort(Comparator.comparing(News::getCreatedAt).reversed());
        return result;
    }

    public List<News> getReadNews() {
        List<News> result = new ArrayList<>();

        for (News news : newsList) {
            if (news != null && news.isRead()) {
                result.add(news);
            }
        }

        result.sort(Comparator.comparing(News::getCreatedAt).reversed());
        return result;
    }

    public void markAsRead(News news) {
        if (news == null) {
            fail("News is not available.");
            return;
        }

        news.markAsRead();
        success("News marked as read.");
    }

    public void markAllAsRead() {
        for (News news : newsList) {
            if (news != null) {
                news.markAsRead();
            }
        }

        success("All news marked as read.");
    }

    public void applyNewsEffects(News news) {
        if (news == null) {
            fail("News is not available.");
            return;
        }

        pendingCoinReward = news.getCoinAmount();
        pendingGemReward = news.getGemAmount();
        news.markAsRead();
        success("News effects applied.");
    }

    public void applyNewsEffects(News news, Collection collection) {
        if (news == null) {
            fail("News is not available.");
            return;
        }

        pendingCoinReward = news.getCoinAmount();
        pendingGemReward = news.getGemAmount();

        if (collection != null) {
            applyCollectionEffect(news, collection);
        }

        news.markAsRead();
        success("News effects applied.");
    }

    public boolean removeNews(News news) {
        if (news == null) {
            fail("News is not available.");
            return false;
        }

        boolean removed = newsList.remove(news);

        if (!removed) {
            fail("News was not found.");
            return false;
        }

        success("News removed.");
        return true;
    }

    public boolean hasUnreadNews() {
        for (News news : newsList) {
            if (news != null && news.isUnread()) {
                return true;
            }
        }

        return false;
    }

    public int getUnreadCount() {
        return getUnreadNews().size();
    }

    public int getTotalCount() {
        return newsList.size();
    }

    public int getPendingCoinReward() {
        return pendingCoinReward;
    }

    public int getPendingGemReward() {
        return pendingGemReward;
    }

    public void clearPendingRewards() {
        pendingCoinReward = 0;
        pendingGemReward = 0;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private void applyCollectionEffect(News news, Collection collection) {
        if (news.isPlantUnlockNews() && news.hasTarget()) {
            PlantData plant = collection.findPlant(news.getTargetName());

            if (plant == null) {
                plant = new PlantData(news.getTargetName(), 0, true);
            }

            collection.unlockPlant(plant);
            return;
        }

        if (news.isZombieUnlockNews() && news.hasTarget()) {
            collection.unlockZombie(news.getTargetName());
        }
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
        pendingCoinReward = 0;
        pendingGemReward = 0;
    }
}