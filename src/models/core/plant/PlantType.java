package models.core.plant;

public class PlantType {
    private static final String DEFAULT_NAME = "pea shooter";
    private static final int DEFAULT_SUN_COST = 100;
    private static final int DEFAULT_BASE_HP = 100;
    private static final int DEFAULT_BASE_COOLDOWN = 15;
    private static final String DEFAULT_CATEGORY = "shooter";

    private final String name;
    private final int sunCost;
    private final int baseHp;
    private final int baseCooldown;
    private final String category;
    private final String level2Upgrade;
    private final String level3Upgrade;
    private final String level4Upgrade;

    public PlantType() {
        this(DEFAULT_NAME, DEFAULT_SUN_COST, DEFAULT_BASE_HP, DEFAULT_BASE_COOLDOWN, DEFAULT_CATEGORY);
    }

    public PlantType(String name, int sunCost, int baseHp, int baseCooldown, String category) {
        this(name, sunCost, baseHp, baseCooldown, category, "", "", "");
    }

    public PlantType(
            String name,
            int sunCost,
            int baseHp,
            int baseCooldown,
            String category,
            String level2Upgrade,
            String level3Upgrade,
            String level4Upgrade
    ) {
        this.name = normalizeName(name);
        this.sunCost = Math.max(0, sunCost);
        this.baseHp = Math.max(0, baseHp);
        this.baseCooldown = Math.max(0, baseCooldown);
        this.category = normalizeCategory(category);
        this.level2Upgrade = normalizeUpgrade(level2Upgrade);
        this.level3Upgrade = normalizeUpgrade(level3Upgrade);
        this.level4Upgrade = normalizeUpgrade(level4Upgrade);
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_NAME;
        }

        return name.trim();
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "normal";
        }

        return category.trim().toLowerCase();
    }

    private String normalizeUpgrade(String upgrade) {
        if (upgrade == null || upgrade.isBlank()) {
            return "";
        }

        return upgrade.trim();
    }

    public String getName() {
        return name;
    }

    public int getSunCost() {
        return sunCost;
    }

    public int getBaseHp() {
        return baseHp;
    }

    public int getBaseCooldown() {
        return baseCooldown;
    }

    public String getCategory() {
        return category;
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
}