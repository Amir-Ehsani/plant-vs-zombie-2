package models.account;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Greenhouse {
    public static final int WIDTH = 5;
    public static final int HEIGHT = 4;
    public static final int MARIGOLD_GROW_HOURS = 2;
    public static final int PLANT_GROW_HOURS = 8;
    public static final int DEFAULT_HARVEST_REWARD = 50;

    private final Pot[][] pots;
    private int productionAmount;
    private String lastHarvestTime;

    public Greenhouse() {
        this.pots = new Pot[HEIGHT][WIDTH];
        this.productionAmount = 0;
        this.lastHarvestTime = "";

        for (int y = 1; y <= HEIGHT; y++) {
            for (int x = 1; x <= WIDTH; x++) {
                boolean unlocked = y == 1;
                pots[y - 1][x - 1] = new Pot(x, y, unlocked);
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

    public boolean unlockPot(int x, int y) {
        Pot pot = getPot(x, y);

        if (pot == null || pot.isUnlocked()) {
            return false;
        }

        pot.unlock();
        return true;
    }

    public boolean unlockRow(int row) {
        if (row < 1 || row > HEIGHT) {
            return false;
        }

        boolean changed = false;

        for (int x = 1; x <= WIDTH; x++) {
            Pot pot = getPot(x, row);

            if (pot != null && !pot.isUnlocked()) {
                pot.unlock();
                changed = true;
            }
        }

        return changed;
    }

    public boolean plantSeed(PlantData plant, int x, int y) {
        Pot pot = getPot(x, y);

        if (pot == null || plant == null || !plant.isUnlocked()) {
            return false;
        }

        return pot.plant(plant.getName(), PLANT_GROW_HOURS);
    }

    public boolean plantMarigold(int x, int y) {
        Pot pot = getPot(x, y);

        if (pot == null) {
            return false;
        }

        return pot.plant("Marigold", MARIGOLD_GROW_HOURS);
    }

    public boolean grow(int x, int y) {
        Pot pot = getPot(x, y);

        if (pot == null || !pot.isGrowing()) {
            return false;
        }

        pot.forceReady();
        return true;
    }

    public int harvest(int x, int y) {
        Pot pot = getPot(x, y);

        if (pot == null || !pot.isReady()) {
            return 0;
        }

        int reward = pot.getHarvestReward();
        pot.clear();
        addProduction(reward);
        lastHarvestTime = LocalDateTime.now().toString();
        return reward;
    }

    public int harvestAllReadyPots() {
        int totalReward = 0;

        for (Pot pot : getAllPots()) {
            if (pot.isReady()) {
                totalReward += harvest(pot.getX(), pot.getY());
            }
        }

        return totalReward;
    }

    public void updateGrowth() {
        for (Pot pot : getAllPots()) {
            pot.updateStatus();
        }
    }

    public void addProduction(int productionAmount) {
        if (productionAmount > 0) {
            this.productionAmount += productionAmount;
        }
    }

    public int getProductionAmount() {
        return productionAmount;
    }

    public String getLastHarvestTime() {
        return lastHarvestTime;
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
            this.plantName = "";
            this.plantedAt = null;
            this.readyAt = null;
            this.status = unlocked ? "empty" : "locked";
            this.harvestReward = DEFAULT_HARVEST_REWARD;
        }

        public boolean plant(String plantName, int growHours) {
            if (!unlocked || !isEmpty() || plantName == null || plantName.isBlank()) {
                return false;
            }

            this.plantName = plantName.trim();
            this.plantedAt = LocalDateTime.now();
            this.readyAt = plantedAt.plusHours(Math.max(0, growHours));
            this.status = growHours <= 0 ? "ready" : "growing";
            this.harvestReward = DEFAULT_HARVEST_REWARD;
            return true;
        }

        public void updateStatus() {
            if (!isGrowing()) {
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
            harvestReward = DEFAULT_HARVEST_REWARD;
        }

        public void unlock() {
            unlocked = true;

            if (status.equals("locked")) {
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
            return unlocked && status.equals("empty");
        }

        public boolean isGrowing() {
            updateStatus();
            return unlocked && status.equals("growing");
        }

        public boolean isReady() {
            updateStatus();
            return unlocked && status.equals("ready");
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public String getPlantName() {
            return plantName;
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

        public void setHarvestReward(int harvestReward) {
            this.harvestReward = Math.max(0, harvestReward);
        }
    }
}