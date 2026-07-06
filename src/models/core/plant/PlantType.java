package models.core.plant;

public class PlantType {
    private static final String DEFAULT_NAME = "Peashooter";
    private static final int DEFAULT_SUN_COST = 100;
    private static final int DEFAULT_BASE_HP = 300;
    private static final int DEFAULT_BASE_COOLDOWN = 15;
    private static final String DEFAULT_CATEGORY = "Shooter";
    private static final String NO_EFFECT = "none";

    private final String name;
    private final String category;
    private final String tags;
    private final int sunCost;
    private final int baseHp;
    private final String damage;
    private final String baseAbility;
    private final String plantFoodEffect;
    private final String level2Upgrade;
    private final String level3Upgrade;
    private final String level4Upgrade;
    private final int actionInterval;
    private final int recharge;

    public PlantType() {
        this(
                DEFAULT_NAME,
                DEFAULT_CATEGORY,
                "",
                DEFAULT_SUN_COST,
                DEFAULT_BASE_HP,
                "20",
                "",
                NO_EFFECT,
                "",
                "",
                "",
                DEFAULT_BASE_COOLDOWN,
                DEFAULT_BASE_COOLDOWN
        );
    }

    public PlantType(String name, int sunCost, int baseHp, int actionInterval, String category) {
        this(
                name,
                category,
                "",
                sunCost,
                baseHp,
                "0",
                "",
                NO_EFFECT,
                "",
                "",
                "",
                actionInterval,
                actionInterval
        );
    }

    public PlantType(
            String name,
            int sunCost,
            int baseHp,
            int actionInterval,
            String category,
            String level2Upgrade,
            String level3Upgrade,
            String level4Upgrade
    ) {
        this(
                name,
                category,
                "",
                sunCost,
                baseHp,
                "0",
                "",
                NO_EFFECT,
                level2Upgrade,
                level3Upgrade,
                level4Upgrade,
                actionInterval,
                actionInterval
        );
    }

    public PlantType(
            String name,
            String category,
            String tags,
            int sunCost,
            int baseHp,
            String damage,
            String baseAbility,
            String plantFoodEffect,
            String level2Upgrade,
            String level3Upgrade,
            String level4Upgrade,
            int actionInterval,
            int recharge
    ) {
        this.name = normalizeText(name, DEFAULT_NAME);
        this.category = normalizeText(category, DEFAULT_CATEGORY);
        this.tags = normalizeText(tags, "");
        this.sunCost = Math.max(0, sunCost);
        this.baseHp = Math.max(0, baseHp);
        this.damage = normalizeText(damage, "0");
        this.baseAbility = normalizeText(baseAbility, "");
        this.plantFoodEffect = normalizeText(plantFoodEffect, NO_EFFECT);
        this.level2Upgrade = normalizeText(level2Upgrade, "");
        this.level3Upgrade = normalizeText(level3Upgrade, "");
        this.level4Upgrade = normalizeText(level4Upgrade, "");
        this.actionInterval = Math.max(0, actionInterval);
        this.recharge = Math.max(0, recharge);
    }

    private String normalizeText(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getTags() {
        return tags;
    }

    public int getSunCost() {
        return sunCost;
    }

    public int getBaseHp() {
        return baseHp;
    }

    public String getDamage() {
        return damage;
    }

    public String getBaseAbility() {
        return baseAbility;
    }

    public String getPlantFoodEffect() {
        return plantFoodEffect;
    }

    public String getLevel2Upgrade() {
        return level2Upgrade;
    }

    public String getLevel3Upgrade() {
        return level3Upgrade;
    }

    public String getLevel4Upgrade() {
        return level4Upgrade;
    }

    public int getActionInterval() {
        return actionInterval;
    }

    public int getBaseCooldown() {
        return actionInterval;
    }

    public int getRecharge() {
        return recharge;
    }

    public String getUpgradeForLevel(int level) {
        if (level == 2) {
            return level2Upgrade;
        }

        if (level == 3) {
            return level3Upgrade;
        }

        if (level == 4) {
            return level4Upgrade;
        }

        return "";
    }

    public boolean hasUpgradeForLevel(int level) {
        return !getUpgradeForLevel(level).isEmpty();
    }

    public boolean hasPlantFoodEffect() {
        return !plantFoodEffect.isEmpty() && !plantFoodEffect.equalsIgnoreCase(NO_EFFECT);
    }
}