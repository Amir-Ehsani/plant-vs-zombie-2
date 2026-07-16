package models.account;

import java.util.Locale;

public class PlantData implements IPurchasable {
    private String name;
    private int price;
    private int level;
    private boolean unlocked;
    private int seedPackets;
    private int boostCount;

    public PlantData() {
        this("", 0, false);
    }

    public PlantData(String name, int price) {
        this(name, price, false);
    }

    public PlantData(String name, int price, boolean unlocked) {
        this.name = normalizeDisplayName(name);
        this.price = Math.max(0, price);
        level = 1;
        this.unlocked = unlocked;
        seedPackets = 0;
        boostCount = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalizeDisplayName(name);
    }

    @Override
    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = Math.max(0, price);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(4, level));
    }

    @Override
    public boolean isUnlocked() {
        return unlocked;
    }

    public void unlock() {
        unlocked = true;
    }

    public void lock() {
        unlocked = false;
    }

    public int getSeedPackets() {
        return seedPackets;
    }

    public void addSeedPackets(int amount) {
        if (amount > 0) {
            seedPackets += amount;
        }
    }

    public boolean spendSeedPackets(int amount) {
        if (amount < 0 || seedPackets < amount) {
            return false;
        }

        seedPackets -= amount;
        return true;
    }

    public int getBoostCount() {
        return boostCount;
    }

    public void addBoost(int amount) {
        if (amount > 0) {
            boostCount += amount;
        }
    }

    public boolean addStoredGreenhouseBoost() {
        if (boostCount > 0) {
            return false;
        }

        boostCount = 1;
        return true;
    }

    public boolean useBoost() {
        if (boostCount <= 0) {
            return false;
        }

        boostCount--;
        return true;
    }

    public int getRequiredSeedPacketsForNextLevel() {
        if (level == 1) {
            return 10;
        }
        if (level == 2) {
            return 25;
        }
        if (level == 3) {
            return 50;
        }
        return Integer.MAX_VALUE;
    }

    public int getUpgradePrice() {
        if (level >= 4) {
            return Integer.MAX_VALUE;
        }
        return price * level;
    }

    public boolean canUpgrade() {
        return unlocked && level < 4 && seedPackets >= getRequiredSeedPacketsForNextLevel();
    }

    public boolean upgrade() {
        int requiredPackets = getRequiredSeedPacketsForNextLevel();
        if (!canUpgrade()) {
            return false;
        }

        seedPackets -= requiredPackets;
        level++;
        return true;
    }

    public boolean hasName(String plantName) {
        return normalizeKey(name).equals(normalizeKey(plantName));
    }

    private String normalizeDisplayName(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeKey(String value) {
        return normalizeDisplayName(value)
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}
