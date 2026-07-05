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

    public PlantType() {
        this(DEFAULT_NAME, DEFAULT_SUN_COST, DEFAULT_BASE_HP, DEFAULT_BASE_COOLDOWN, DEFAULT_CATEGORY);
    }

    public PlantType(String name, int sunCost, int baseHp, int baseCooldown, String category) {
        this.name = normalizeName(name);
        this.sunCost = Math.max(0, sunCost);
        this.baseHp = Math.max(0, baseHp);
        this.baseCooldown = Math.max(0, baseCooldown);
        this.category = normalizeCategory(category);
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
}