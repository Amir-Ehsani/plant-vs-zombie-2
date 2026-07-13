package models.account;

public class Quest {
    private String questDescription;
    private String type;
    private int progressAmount;
    private int targetAmount;
    private int coinReward;
    private int gemReward;
    private boolean rewardClaimed;

    public Quest() {
        this("", "general", 1, 0, 0);
    }

    public Quest(String questDescription, int targetAmount) {
        this(questDescription, "general", targetAmount, 0, 0);
    }

    public Quest(String questDescription, String type, int targetAmount, int coinReward, int gemReward) {
        this.questDescription = normalizeText(questDescription);
        this.type = normalizeType(type);
        this.progressAmount = 0;
        this.targetAmount = Math.max(1, targetAmount);
        this.coinReward = Math.max(0, coinReward);
        this.gemReward = Math.max(0, gemReward);
        this.rewardClaimed = false;
    }

    public boolean isCompleted() {
        return progressAmount >= targetAmount;
    }

    public String getQuestDescription() {
        return questDescription;
    }

    public void setQuestDescription(String questDescription) {
        this.questDescription = normalizeText(questDescription);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = normalizeType(type);
    }

    public int getProgressAmount() {
        return progressAmount;
    }

    public void setProgressAmount(int progressAmount) {
        this.progressAmount = Math.max(0, progressAmount);
    }

    public void addProgress(int amount) {
        if (amount > 0) {
            progressAmount += amount;
        }
    }

    public void resetProgress() {
        progressAmount = 0;
        rewardClaimed = false;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(int targetAmount) {
        this.targetAmount = Math.max(1, targetAmount);
    }

    public int getCoinReward() {
        return coinReward;
    }

    public void setCoinReward(int coinReward) {
        this.coinReward = Math.max(0, coinReward);
    }

    public int getGemReward() {
        return gemReward;
    }

    public void setGemReward(int gemReward) {
        this.gemReward = Math.max(0, gemReward);
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public boolean canClaimReward() {
        return isCompleted() && !rewardClaimed;
    }

    public boolean claimReward() {
        if (!canClaimReward()) {
            return false;
        }

        rewardClaimed = true;
        return true;
    }

    public boolean matchesType(String type) {
        return this.type.equals(normalizeType(type));
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private String normalizeType(String value) {
        String normalized = normalizeText(value);

        if (normalized.isEmpty()) {
            return "general";
        }

        return normalized.toLowerCase().replace("-", "_").replace(" ", "_");
    }
}