package models.account;

public class Quest {
    private String questDescription;
    private String type;
    private String conditionDescription;
    private String rewardDescription;
    private String priority;
    private String variables;
    private String progressKey;
    private String targetKey;
    private int progressAmount;
    private int targetAmount;
    private int coinReward;
    private int gemReward;
    private int seedPacketReward;
    private boolean randomPlantReward;
    private boolean rewardClaimed;

    public Quest() {
        this("", "general", "", "", "medium", "", "", "", 1, 0, 0, 0, false);
    }

    public Quest(String questDescription, int targetAmount) {
        this(questDescription, "general", "", "", "medium", "", "", "", targetAmount, 0, 0, 0, false);
    }

    public Quest(String questDescription, String type, int targetAmount, int coinReward, int gemReward) {
        this(questDescription, type, "", "", "medium", "", "", "", targetAmount, coinReward, gemReward, 0, false);
    }

    public Quest(
            String questDescription,
            String type,
            String conditionDescription,
            String rewardDescription,
            String priority,
            String variables,
            int targetAmount,
            int coinReward,
            int gemReward,
            int seedPacketReward,
            boolean randomPlantReward
    ) {
        this(questDescription, type, conditionDescription, rewardDescription, priority, variables,
                "", "", targetAmount, coinReward, gemReward, seedPacketReward, randomPlantReward);
    }

    public Quest(
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
        this.questDescription = normalizeText(questDescription);
        this.type = normalizeType(type);
        this.conditionDescription = normalizeText(conditionDescription);
        this.rewardDescription = normalizeText(rewardDescription);
        this.priority = normalizePriority(priority);
        this.variables = normalizeText(variables);
        this.progressKey = normalizeKey(progressKey);
        this.targetKey = normalizeKey(targetKey);
        this.progressAmount = 0;
        this.targetAmount = Math.max(1, targetAmount);
        this.coinReward = Math.max(0, coinReward);
        this.gemReward = Math.max(0, gemReward);
        this.seedPacketReward = Math.max(0, seedPacketReward);
        this.randomPlantReward = randomPlantReward;
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

    public String getConditionDescription() {
        return conditionDescription;
    }

    public void setConditionDescription(String conditionDescription) {
        this.conditionDescription = normalizeText(conditionDescription);
    }

    public String getRewardDescription() {
        return rewardDescription;
    }

    public void setRewardDescription(String rewardDescription) {
        this.rewardDescription = normalizeText(rewardDescription);
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = normalizePriority(priority);
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = normalizeText(variables);
    }

    public String getProgressKey() {
        return progressKey;
    }

    public void setProgressKey(String progressKey) {
        this.progressKey = normalizeKey(progressKey);
    }

    public String getTargetKey() {
        return targetKey;
    }

    public void setTargetKey(String targetKey) {
        this.targetKey = normalizeKey(targetKey);
    }

    public int getProgressAmount() {
        return progressAmount;
    }

    public void setProgressAmount(int progressAmount) {
        this.progressAmount = Math.max(0, Math.min(Math.max(1, targetAmount), progressAmount));
    }

    public void addProgress(int amount) {
        if (amount > 0 && !rewardClaimed) {
            progressAmount = Math.min(targetAmount, progressAmount + amount);
        }
    }

    public void complete() {
        if (!rewardClaimed) {
            progressAmount = targetAmount;
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

        if (progressAmount > this.targetAmount) {
            progressAmount = this.targetAmount;
        }
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

    public int getSeedPacketReward() {
        return seedPacketReward;
    }

    public void setSeedPacketReward(int seedPacketReward) {
        this.seedPacketReward = Math.max(0, seedPacketReward);
    }

    public boolean hasRandomPlantReward() {
        return randomPlantReward;
    }

    public void setRandomPlantReward(boolean randomPlantReward) {
        this.randomPlantReward = randomPlantReward;
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

    public boolean matchesProgressKey(String progressKey) {
        return this.progressKey.equals(normalizeKey(progressKey));
    }

    public String rewardText() {
        if (!rewardDescription.isBlank()) {
            return rewardDescription;
        }

        StringBuilder builder = new StringBuilder();

        if (coinReward > 0) {
            builder.append(coinReward).append(" coins");
        }

        if (gemReward > 0) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(gemReward).append(" gems");
        }

        if (seedPacketReward > 0) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(seedPacketReward).append(" seed packets");
        }

        if (randomPlantReward) {
            if (builder.length() > 0) builder.append(", ");
            builder.append("random plant");
        }

        return builder.length() == 0 ? "No reward" : builder.toString();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeType(String value) {
        String normalized = normalizeText(value).toLowerCase()
                .replace("-", "_")
                .replace(" ", "_");

        if (normalized.isEmpty()) return "general";
        if ("daily".equals(normalized) || "challenge".equals(normalized) || "روزانه".equals(normalized)) return "challenges";
        if ("main".equals(normalized) || "story".equals(normalized) || "اصلی".equals(normalized)) return "adventure";
        if ("epic".equals(normalized) || "special_challenge".equals(normalized) || "چالش_(epic)".equals(normalized)) return "special";

        return normalized;
    }

    private String normalizePriority(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? "medium" : normalized;
    }

    private String normalizeKey(String value) {
        return normalizeText(value).toLowerCase()
                .replace("-", "_")
                .replace(" ", "_");
    }
}