package models.account;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Greenhouse {
    public static final int WIDTH = 5;
    public static final int HEIGHT = 4;
    public static final int MARIGOLD_GROW_HOURS = 2;
    public static final int PLANT_GROW_HOURS = 8;
    public static final int DEFAULT_HARVEST_REWARD = 500;

    private final Pot[][] pots;
    private int productionAmount;
    private String lastHarvestTime;

    public Greenhouse() {
        pots = new Pot[HEIGHT][WIDTH];
        productionAmount = 0;
        lastHarvestTime = "";

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                pots[y - 1][x - 1] = new Pot(x, y, y == 1);
            }
        }
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 1 && x <= WIDTH && y >= 1 && y <= HEIGHT;
    }

    public Pot getPot(int x, int y) {
        if (!isValidPosition(x, y)) {
            return null;
        }
        return pots[y - 1][x - 1];
    }

    public List<Pot> getAllPots() {
        List<Pot> allPots = new ArrayList<>();
        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                allPots.add(getPot(x, y));
            }
        }
        return allPots;
    }

    public int getUnlockedPotCount() {
        int count = 0;
        for (Pot pot : getAllPots()) {
            if (pot.isUnlocked()) {
                count++;
            }
        }
        return count;
    }

    public int getLockedPotCount() {
        return WIDTH * HEIGHT - getUnlockedPotCount();
    }

    public boolean unlockPot(int x, int y) {
        Pot pot = getPot(x, y);
        if (pot == null || pot.isUnlocked()) {
            return false;
        }

        pot.unlock();
        return true;
    }

    public boolean unlockNextPot() {
        for (Pot pot : getAllPots()) {
            if (pot.isLocked()) {
                pot.unlock();
                return true;
            }
        }
        return false;
    }

    public boolean unlockRow(int row) {
        if (row < 1 || row > HEIGHT) {
            return false;
        }

        boolean changed = false;
        for (int x = 1; x <= WIDTH; x++) {
            Pot pot = getPot(x, row);
            if (pot != null && pot.isLocked()) {
                pot.unlock();
                changed = true;
            }
        }
        return changed;
    }

    public boolean plantSeed(PlantData plant, int x, int y) {
        if (plant == null || !plant.isUnlocked()) {
            return false;
        }
        return plantSeed(plant.getName(), x, y);
    }

    public boolean plantSeed(String plantName, int x, int y) {
        Pot pot = getPot(x, y);
        return pot != null && pot.plant(plantName, PLANT_GROW_HOURS);
    }

    public boolean plantMarigold(int x, int y) {
        Pot pot = getPot(x, y);
        return pot != null && pot.plant("Marigold", MARIGOLD_GROW_HOURS);
    }

    public boolean grow(int x, int y) {
        Pot pot = getPot(x, y);
        if (pot == null || !pot.isGrowing()) {
            return false;
        }

        pot.forceReady();
        return true;
    }

    public HarvestResult collect(int x, int y) {
        Pot pot = getPot(x, y);
        if (pot == null || !pot.isReady()) {
            return HarvestResult.failed();
        }

        String plantName = pot.getPlantName();
        boolean marigold = pot.isMarigold();
        int coinReward = marigold ? DEFAULT_HARVEST_REWARD : 0;
        pot.clear();
        addProduction(coinReward);
        lastHarvestTime = LocalDateTime.now().toString();
        return HarvestResult.success(plantName, marigold, coinReward);
    }

    public int harvest(int x, int y) {
        return collect(x, y).getCoinReward();
    }

    public int harvestAllReadyPots() {
        int totalReward = 0;
        for (Pot pot : getAllPots()) {
            if (pot.isReady()) {
                totalReward += collect(pot.getX(), pot.getY()).getCoinReward();
            }
        }
        return totalReward;
    }

    public void updateGrowth() {
        for (Pot pot : getAllPots()) {
            pot.updateStatus();
        }
    }

    public void addProduction(int amount) {
        if (amount > 0) {
            productionAmount += amount;
        }
    }

    public int getProductionAmount() {
        return productionAmount;
    }

    public String getLastHarvestTime() {
        return lastHarvestTime == null ? "" : lastHarvestTime;
    }

    public static final class HarvestResult {
        private final boolean successful;
        private final String plantName;
        private final boolean marigold;
        private final int coinReward;

        private HarvestResult(boolean successful, String plantName, boolean marigold, int coinReward) {
            this.successful = successful;
            this.plantName = plantName;
            this.marigold = marigold;
            this.coinReward = coinReward;
        }

        public static HarvestResult failed() {
            return new HarvestResult(false, "", false, 0);
        }

        public static HarvestResult success(String plantName, boolean marigold, int coinReward) {
            return new HarvestResult(true, plantName, marigold, coinReward);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getPlantName() {
            return plantName;
        }

        public boolean isMarigold() {
            return marigold;
        }

        public int getCoinReward() {
            return coinReward;
        }
    }

    public static class Pot {
        private final int x;
        private final int y;
        private boolean unlocked;
        private String plantName;
        private LocalDateTime plantedAt;
        private LocalDateTime readyAt;
        private String status;
        private int harvestReward;

        public Pot(int x, int y, boolean unlocked) {
            this.x = x;
            this.y = y;
            this.unlocked = unlocked;
            plantName = "";
            plantedAt = null;
            readyAt = null;
            status = unlocked ? "empty" : "locked";
            harvestReward = 0;
        }

        public boolean plant(String name, int growHours) {
            if (!unlocked || !isEmpty() || name == null || name.isBlank()) {
                return false;
            }

            plantName = name.trim();
            plantedAt = LocalDateTime.now();
            readyAt = plantedAt.plusHours(Math.max(0, growHours));
            status = growHours <= 0 ? "ready" : "growing";
            harvestReward = isMarigold() ? DEFAULT_HARVEST_REWARD : 0;
            return true;
        }

        public void updateStatus() {
            if (!"growing".equals(status)) {
                return;
            }

            if (readyAt == null || !LocalDateTime.now().isBefore(readyAt)) {
                status = "ready";
            }
        }

        public void forceReady() {
            if (!unlocked || isEmpty() || isLocked()) {
                return;
            }

            readyAt = LocalDateTime.now();
            status = "ready";
        }

        public void clear() {
            if (!unlocked) {
                return;
            }

            plantName = "";
            plantedAt = null;
            readyAt = null;
            status = "empty";
            harvestReward = 0;
        }

        public void unlock() {
            unlocked = true;
            if ("locked".equals(status)) {
                status = "empty";
            }
        }

        public boolean isUnlocked() {
            return unlocked;
        }

        public boolean isLocked() {
            return !unlocked;
        }

        public boolean isEmpty() {
            return unlocked && "empty".equals(status);
        }

        public boolean isGrowing() {
            updateStatus();
            return unlocked && "growing".equals(status);
        }

        public boolean isReady() {
            updateStatus();
            return unlocked && "ready".equals(status);
        }

        public boolean isMarigold() {
            return "marigold".equalsIgnoreCase(plantName);
        }

        public long getRemainingMinutes() {
            updateStatus();
            if (!isGrowing() || readyAt == null) {
                return 0;
            }

            long seconds = Math.max(0, Duration.between(LocalDateTime.now(), readyAt).getSeconds());
            return (seconds + 59) / 60;
        }

        public int getRemainingHoursRoundedUp() {
            long minutes = getRemainingMinutes();
            if (minutes <= 0) {
                return 0;
            }
            return (int) ((minutes + 59) / 60);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public String getPlantName() {
            return plantName == null ? "" : plantName;
        }

        public LocalDateTime getPlantedAt() {
            return plantedAt;
        }

        public LocalDateTime getReadyAt() {
            return readyAt;
        }

        public String getStatus() {
            updateStatus();
            return status;
        }

        public int getHarvestReward() {
            return harvestReward;
        }

        public void setHarvestReward(int reward) {
            harvestReward = Math.max(0, reward);
        }
    }
}
