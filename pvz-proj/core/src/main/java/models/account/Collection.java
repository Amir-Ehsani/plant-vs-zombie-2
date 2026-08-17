package models.account;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Collection {
    private static final int MAX_STORED_PLANT_FOOD = 3;

    private List<PlantData> ownedPlants;
    private List<PlantData> lockedPlants;
    private List<String> ownedZombies;
    private List<String> lockedZombies;
    private int storedPlantFood;
    private String dailyOfferDate;
    private String dailyOfferPlantName;
    private boolean dailyOfferPurchased;
    private Map<String, Integer> shopItemAmounts;

    public Collection() {
        ownedPlants = new ArrayList<>();
        lockedPlants = new ArrayList<>();
        ownedZombies = new ArrayList<>();
        lockedZombies = new ArrayList<>();
        storedPlantFood = 0;
        dailyOfferDate = "";
        dailyOfferPlantName = "";
        dailyOfferPurchased = false;
        shopItemAmounts = new LinkedHashMap<>();
    }

    public List<PlantData> getOwnedPlants() {
        ensureLists();
        return new ArrayList<>(ownedPlants);
    }

    public List<PlantData> getLockedPlants() {
        ensureLists();
        return new ArrayList<>(lockedPlants);
    }

    public List<PlantData> getAllPlants() {
        List<PlantData> allPlants = getOwnedPlants();
        allPlants.addAll(getLockedPlants());
        return allPlants;
    }

    public List<String> getOwnedZombies() {
        ensureLists();
        return new ArrayList<>(ownedZombies);
    }

    public List<String> getLockedZombies() {
        ensureLists();
        return new ArrayList<>(lockedZombies);
    }

    public List<String> getAllZombies() {
        List<String> allZombies = getOwnedZombies();
        allZombies.addAll(getLockedZombies());
        return allZombies;
    }

    public void addPlant(PlantData plant) {
        ensureLists();
        if (plant == null || hasPlant(plant.getName())) {
            return;
        }

        if (plant.isUnlocked()) {
            ownedPlants.add(plant);
        } else {
            lockedPlants.add(plant);
        }
    }

    public void addZombie(String zombieName, boolean unlocked) {
        ensureLists();
        String normalizedName = normalizeDisplayName(zombieName);
        if (normalizedName.isEmpty() || hasZombie(normalizedName)) {
            return;
        }

        if (unlocked) {
            ownedZombies.add(normalizedName);
        } else {
            lockedZombies.add(normalizedName);
        }
    }

    public void unlockPlant(PlantData plant) {
        ensureLists();
        if (plant == null) {
            return;
        }

        PlantData existingPlant = findPlant(plant.getName());
        if (existingPlant == null) {
            plant.unlock();
            ownedPlants.add(plant);
            return;
        }

        existingPlant.unlock();
        lockedPlants.remove(existingPlant);
        if (!ownedPlants.contains(existingPlant)) {
            ownedPlants.add(existingPlant);
        }
    }

    public boolean unlockPlant(String plantName) {
        PlantData plant = findPlant(plantName);
        if (plant == null) {
            return false;
        }

        unlockPlant(plant);
        return true;
    }

    public boolean unlockZombie(String zombieName) {
        ensureLists();
        String normalizedName = normalizeDisplayName(zombieName);
        if (normalizedName.isEmpty()) {
            return false;
        }

        String ownedName = findMatchingName(ownedZombies, normalizedName);
        if (ownedName != null) {
            return true;
        }

        String lockedName = findMatchingName(lockedZombies, normalizedName);
        if (lockedName != null) {
            lockedZombies.remove(lockedName);
            ownedZombies.add(lockedName);
            return true;
        }

        ownedZombies.add(normalizedName);
        return true;
    }

    public PlantData findPlant(String plantName) {
        PlantData plant = findOwnedPlant(plantName);
        return plant == null ? findLockedPlant(plantName) : plant;
    }

    public PlantData findOwnedPlant(String plantName) {
        ensureLists();
        for (PlantData plant : ownedPlants) {
            if (plant != null && plant.hasName(plantName)) {
                return plant;
            }
        }
        return null;
    }

    public PlantData findLockedPlant(String plantName) {
        ensureLists();
        for (PlantData plant : lockedPlants) {
            if (plant != null && plant.hasName(plantName)) {
                return plant;
            }
        }
        return null;
    }

    public boolean hasPlant(String plantName) {
        return findPlant(plantName) != null;
    }

    public boolean hasOwnedPlant(String plantName) {
        return findOwnedPlant(plantName) != null;
    }

    public boolean hasLockedPlant(String plantName) {
        return findLockedPlant(plantName) != null;
    }

    public boolean hasZombie(String zombieName) {
        ensureLists();
        return findMatchingName(ownedZombies, zombieName) != null
                || findMatchingName(lockedZombies, zombieName) != null;
    }

    public boolean hasOwnedZombie(String zombieName) {
        ensureLists();
        return findMatchingName(ownedZombies, zombieName) != null;
    }

    public boolean hasLockedZombie(String zombieName) {
        ensureLists();
        return findMatchingName(lockedZombies, zombieName) != null;
    }

    public boolean upgradePlant(String plantName) {
        PlantData plant = findOwnedPlant(plantName);
        return plant != null && plant.upgrade();
    }

    public boolean addSeedPackets(String plantName, int amount) {
        PlantData plant = findPlant(plantName);
        if (plant == null || amount <= 0) {
            return false;
        }

        plant.addSeedPackets(amount);
        return true;
    }

    public boolean addBoost(String plantName, int amount) {
        PlantData plant = findOwnedPlant(plantName);
        if (plant == null || amount <= 0) {
            return false;
        }

        plant.addBoost(amount);
        return true;
    }

    public int getStoredPlantFood() {
        return storedPlantFood;
    }

    public int getMaxStoredPlantFood() {
        return MAX_STORED_PLANT_FOOD;
    }

    public int getRemainingPlantFoodCapacity() {
        return MAX_STORED_PLANT_FOOD - storedPlantFood;
    }

    public boolean addStoredPlantFood(int amount) {
        if (amount <= 0 || storedPlantFood + amount > MAX_STORED_PLANT_FOOD) {
            return false;
        }

        storedPlantFood += amount;
        return true;
    }

    public boolean useStoredPlantFood() {
        if (storedPlantFood <= 0) {
            return false;
        }

        storedPlantFood--;
        return true;
    }

    public int takeStoredPlantFood() {
        int amount = Math.max(0, Math.min(MAX_STORED_PLANT_FOOD, storedPlantFood));
        storedPlantFood = 0;
        return amount;
    }


    public Map<String, Integer> getShopItemAmounts() {
        ensureShopItemAmounts();
        return new LinkedHashMap<>(shopItemAmounts);
    }

    public void setShopItemAmounts(Map<String, Integer> amounts) {
        shopItemAmounts = new LinkedHashMap<>();
        if (amounts == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : amounts.entrySet()) {
            String key = normalizeShopItemId(entry.getKey());
            Integer value = entry.getValue();
            if (!key.isBlank() && value != null) {
                shopItemAmounts.put(key, Math.max(0, value));
            }
        }
    }

    public int getShopItemAmount(String itemId, int defaultAmount) {
        ensureShopItemAmounts();
        String key = normalizeShopItemId(itemId);
        if (key.isBlank()) {
            return 0;
        }
        int fallback = Math.max(0, defaultAmount);
        return Math.max(0, shopItemAmounts.getOrDefault(key, fallback));
    }

    public boolean reduceShopItemAmount(String itemId, int amount, int defaultAmount) {
        if (amount <= 0) {
            return false;
        }
        int currentAmount = getShopItemAmount(itemId, defaultAmount);
        if (currentAmount < amount) {
            return false;
        }
        shopItemAmounts.put(normalizeShopItemId(itemId), currentAmount - amount);
        return true;
    }

    public void restoreShopItemAmount(String itemId, int amount, int defaultAmount) {
        if (amount <= 0) {
            return;
        }
        int currentAmount = getShopItemAmount(itemId, defaultAmount);
        shopItemAmounts.put(normalizeShopItemId(itemId), currentAmount + amount);
    }

    public void refreshDailyOffer(String plantName, LocalDate date) {
        if (date == null) {
            return;
        }

        String dateText = date.toString();
        if (dateText.equals(dailyOfferDate)
                && dailyOfferPlantName != null
                && !dailyOfferPlantName.isBlank()) {
            return;
        }

        dailyOfferDate = dateText;
        dailyOfferPlantName = normalizeDisplayName(plantName);
        dailyOfferPurchased = false;
    }

    public String getDailyOfferDate() {
        return dailyOfferDate == null ? "" : dailyOfferDate;
    }

    public String getDailyOfferPlantName() {
        return dailyOfferPlantName == null ? "" : dailyOfferPlantName;
    }

    public boolean isDailyOfferPurchased() {
        return dailyOfferPurchased;
    }

    public void markDailyOfferPurchased() {
        dailyOfferPurchased = true;
    }

    private String findMatchingName(List<String> names, String targetName) {
        String targetKey = normalizeKey(targetName);
        for (String name : names) {
            if (normalizeKey(name).equals(targetKey)) {
                return name;
            }
        }
        return null;
    }

    private String normalizeShopItemId(String itemId) {
        return normalizeDisplayName(itemId)
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private String normalizeDisplayName(String name) {
        return name == null ? "" : name.trim();
    }

    private String normalizeKey(String name) {
        return normalizeDisplayName(name)
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }

    private void ensureShopItemAmounts() {
        if (shopItemAmounts == null) {
            shopItemAmounts = new LinkedHashMap<>();
        }
    }

    private void ensureLists() {
        if (ownedPlants == null) {
            ownedPlants = new ArrayList<>();
        }
        if (lockedPlants == null) {
            lockedPlants = new ArrayList<>();
        }
        if (ownedZombies == null) {
            ownedZombies = new ArrayList<>();
        }
        if (lockedZombies == null) {
            lockedZombies = new ArrayList<>();
        }
    }
}
