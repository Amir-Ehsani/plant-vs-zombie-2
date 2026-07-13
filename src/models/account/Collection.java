package models.account;

import java.util.ArrayList;
import java.util.List;

public class Collection {
    private List<PlantData> ownedPlants;
    private List<PlantData> lockedPlants;
    private List<String> ownedZombies;
    private List<String> lockedZombies;

    public Collection() {
        this.ownedPlants = new ArrayList<>();
        this.lockedPlants = new ArrayList<>();
        this.ownedZombies = new ArrayList<>();
        this.lockedZombies = new ArrayList<>();
    }

    public List<PlantData> getOwnedPlants() {
        return new ArrayList<>(ownedPlants);
    }

    public List<PlantData> getLockedPlants() {
        return new ArrayList<>(lockedPlants);
    }

    public List<PlantData> getAllPlants() {
        List<PlantData> allPlants = new ArrayList<>();
        allPlants.addAll(ownedPlants);
        allPlants.addAll(lockedPlants);
        return allPlants;
    }

    public List<String> getOwnedZombies() {
        return new ArrayList<>(ownedZombies);
    }

    public List<String> getLockedZombies() {
        return new ArrayList<>(lockedZombies);
    }

    public List<String> getAllZombies() {
        List<String> allZombies = new ArrayList<>();
        allZombies.addAll(ownedZombies);
        allZombies.addAll(lockedZombies);
        return allZombies;
    }

    public void addPlant(PlantData plant) {
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
        String normalizedName = normalizeName(zombieName);

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
        String normalizedName = normalizeName(zombieName);

        if (normalizedName.isEmpty()) {
            return false;
        }

        if (ownedZombies.contains(normalizedName)) {
            return true;
        }

        if (lockedZombies.contains(normalizedName)) {
            lockedZombies.remove(normalizedName);
            ownedZombies.add(normalizedName);
            return true;
        }

        ownedZombies.add(normalizedName);
        return true;
    }

    public PlantData findPlant(String plantName) {
        PlantData plant = findOwnedPlant(plantName);

        if (plant != null) {
            return plant;
        }

        return findLockedPlant(plantName);
    }

    public PlantData findOwnedPlant(String plantName) {
        for (PlantData plant : ownedPlants) {
            if (plant.hasName(plantName)) {
                return plant;
            }
        }

        return null;
    }

    public PlantData findLockedPlant(String plantName) {
        for (PlantData plant : lockedPlants) {
            if (plant.hasName(plantName)) {
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
        String normalizedName = normalizeName(zombieName);
        return ownedZombies.contains(normalizedName) || lockedZombies.contains(normalizedName);
    }

    public boolean hasOwnedZombie(String zombieName) {
        return ownedZombies.contains(normalizeName(zombieName));
    }

    public boolean hasLockedZombie(String zombieName) {
        return lockedZombies.contains(normalizeName(zombieName));
    }

    public boolean upgradePlant(String plantName) {
        PlantData plant = findOwnedPlant(plantName);

        if (plant == null) {
            return false;
        }

        return plant.upgrade();
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

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }

        return name.trim();
    }
}