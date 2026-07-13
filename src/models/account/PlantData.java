package models.account;

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
        this.name = normalizeName(name);
        this.price = Math.max(0, price);
        this.level = 1;
        this.unlocked = unlocked;
        this.seedPackets = 0;
        this.boostCount = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalizeName(name);
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
        if (level < 1) {
            this.level = 1;
        } else if (level > 4) {
            this.level = 4;
        } else {
            this.level = level;
        }
    }

    @Override
    public boolean isUnlocked() {
        return unlocked;
    }

    public void unlock() {
        this.unlocked = true;
    }

    public void lock() {
        this.unlocked = false;
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
        if (!canUpgrade()) {
            return false;
        }

        seedPackets -= getRequiredSeedPacketsForNextLevel();
        level++;
        return true;
    }

    public boolean hasName(String plantName) {
        return normalizeName(name).equals(normalizeName(plantName));
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}