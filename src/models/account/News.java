package models.account;

import java.time.LocalDateTime;
import java.util.Objects;

public class News {
    private String id;
    private String title;
    private String content;
    private String type;
    private String targetName;
    private int coinAmount;
    private int gemAmount;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public News() {
        this("", "", "general");
    }

    public News(String title, String content) {
        this(title, content, "general");
    }

    public News(String title, String content, String type) {
        this.id = buildId(title, content);
        this.title = normalizeText(title);
        this.content = normalizeText(content);
        this.type = normalizeType(type);
        this.targetName = "";
        this.coinAmount = 0;
        this.gemAmount = 0;
        this.read = false;
        this.createdAt = LocalDateTime.now();
        this.readAt = null;
    }

    public static News plantUnlocked(String plantName) {
        News news = new News(
                "Plant unlocked",
                "A new plant has been unlocked: " + normalizeStaticText(plantName),
                "plant_unlock"
        );
        news.setTargetName(plantName);
        return news;
    }

    public static News zombieDiscovered(String zombieName) {
        News news = new News(
                "Zombie discovered",
                "A new zombie has been discovered: " + normalizeStaticText(zombieName),
                "zombie_unlock"
        );
        news.setTargetName(zombieName);
        return news;
    }

    public static News reward(String title, String content, int coinAmount, int gemAmount) {
        News news = new News(title, content, "reward");
        news.setCoinAmount(coinAmount);
        news.setGemAmount(gemAmount);
        return news;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        String normalized = normalizeText(id);

        if (!normalized.isEmpty()) {
            this.id = normalized;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = normalizeText(title);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = normalizeText(content);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = normalizeType(type);
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = normalizeText(targetName);
    }

    public int getCoinAmount() {
        return coinAmount;
    }

    public void setCoinAmount(int coinAmount) {
        this.coinAmount = Math.max(0, coinAmount);
    }

    public int getGemAmount() {
        return gemAmount;
    }

    public void setGemAmount(int gemAmount) {
        this.gemAmount = Math.max(0, gemAmount);
    }

    public boolean isRead() {
        return read;
    }

    public boolean isUnread() {
        return !read;
    }

    public void markAsRead() {
        if (read) {
            return;
        }

        read = true;
        readAt = LocalDateTime.now();
    }

    public void markAsUnread() {
        read = false;
        readAt = null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        if (createdAt != null) {
            this.createdAt = createdAt;
        }
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public boolean isPlantUnlockNews() {
        return type.equals("plant_unlock");
    }

    public boolean isZombieUnlockNews() {
        return type.equals("zombie_unlock");
    }

    public boolean isRewardNews() {
        return type.equals("reward");
    }

    public boolean hasTarget() {
        return !targetName.isEmpty();
    }

    public boolean hasReward() {
        return coinAmount > 0 || gemAmount > 0;
    }

    private String buildId(String title, String content) {
        String raw = normalizeText(title) + ":" + normalizeText(content) + ":" + System.nanoTime();
        return Integer.toHexString(Objects.hash(raw));
    }

    private String normalizeText(String value) {
        return normalizeStaticText(value);
    }

    private String normalizeType(String value) {
        String normalized = normalizeStaticText(value);

        if (normalized.isEmpty()) {
            return "general";
        }

        return normalized.trim().toLowerCase().replace("-", "_").replace(" ", "_");
    }

    private static String normalizeStaticText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}