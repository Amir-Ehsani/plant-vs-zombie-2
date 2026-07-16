package controllers.features;

import controllers.auth.AuthController;
import models.account.Collection;
import models.account.News;
import models.account.PlantData;
import models.account.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NewsController {
    private final AuthController authController;
    private final List<News> localNews;
    private String lastMessage;
    private int pendingCoinReward;
    private int pendingGemReward;

    public NewsController() {
        this(null);
    }

    public NewsController(AuthController authController) {
        this.authController = authController;
        localNews = new ArrayList<>();
        lastMessage = "";
        pendingCoinReward = 0;
        pendingGemReward = 0;
    }

    public void addNewNews(News news) {
        if (news == null) {
            fail("News is not available.");
            return;
        }

        User user = currentUser();
        if (user == null) {
            localNews.add(news);
        } else {
            user.addNews(news);
            saveUsers();
        }
        success("News added.");
    }

    public List<News> showUnreadNews() {
        List<News> unread = getUnreadNews();
        for (News news : unread) {
            news.markAsRead();
        }
        saveUsers();
        success(unread.isEmpty() ? "No unread news." : "Unread news shown and marked as read.");
        return unread;
    }

    public List<News> showAllNews() {
        List<News> all = getAllNews();
        success(all.isEmpty() ? "No news available." : "All news shown.");
        return all;
    }

    public List<News> getAllNews() {
        List<News> result = new ArrayList<>(sourceNews());
        result.sort(Comparator.comparing(News::getCreatedAt).reversed());
        return result;
    }

    public List<News> getUnreadNews() {
        List<News> result = new ArrayList<>();
        for (News news : sourceNews()) {
            if (news != null && news.isUnread()) {
                result.add(news);
            }
        }
        result.sort(Comparator.comparing(News::getCreatedAt).reversed());
        return result;
    }

    public List<News> getReadNews() {
        List<News> result = new ArrayList<>();
        for (News news : sourceNews()) {
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
        saveUsers();
        success("News marked as read.");
    }

    public void markAllAsRead() {
        for (News news : sourceNews()) {
            if (news != null) {
                news.markAsRead();
            }
        }
        saveUsers();
        success("All news marked as read.");
    }

    public void applyNewsEffects(News news) {
        User user = currentUser();
        if (user == null) {
            applyNewsEffects(news, null);
            return;
        }
        applyNewsEffects(news, user.getCollection());
    }

    public void applyNewsEffects(News news, Collection collection) {
        if (news == null) {
            fail("News is not available.");
            return;
        }
        if (news.areEffectsApplied()) {
            news.markAsRead();
            success("News effects were already applied.");
            return;
        }

        pendingCoinReward = news.getCoinAmount();
        pendingGemReward = news.getGemAmount();
        User user = currentUser();
        if (user != null) {
            user.addCoins(pendingCoinReward);
            user.addGems(pendingGemReward);
        }
        if (collection != null) {
            applyCollectionEffect(news, collection);
        }

        news.markEffectsApplied();
        news.markAsRead();
        saveUsers();
        success("News effects applied.");
    }

    public boolean removeNews(News news) {
        if (news == null) {
            fail("News is not available.");
            return false;
        }

        boolean removed;
        User user = currentUser();
        if (user == null) {
            removed = localNews.remove(news);
        } else {
            List<News> newsList = user.getNewsList();
            removed = newsList.remove(news);
            user.setNewsList(newsList);
            saveUsers();
        }
        if (!removed) {
            fail("News was not found.");
            return false;
        }
        success("News removed.");
        return true;
    }

    public boolean hasUnreadNews() {
        return !getUnreadNews().isEmpty();
    }

    public int getUnreadCount() {
        return getUnreadNews().size();
    }

    public int getTotalCount() {
        return sourceNews().size();
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

    private void applyCollectionEffect(News news, Collection collection) {
        if (news.isPlantUnlockNews() && news.hasTarget()) {
            PlantData plant = collection.findPlant(news.getTargetName());
            if (plant == null) {
                plant = new PlantData(news.getTargetName(), CollectionController.PLANT_PURCHASE_PRICE, true);
            }
            collection.unlockPlant(plant);
        } else if (news.isZombieUnlockNews() && news.hasTarget()) {
            collection.unlockZombie(news.getTargetName());
        }
    }

    private List<News> sourceNews() {
        User user = currentUser();
        return user == null ? localNews : user.getNewsList();
    }

    private User currentUser() {
        return authController == null ? null : authController.getLoggedInUser();
    }

    private void saveUsers() {
        if (authController != null) {
            authController.saveUsers();
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
