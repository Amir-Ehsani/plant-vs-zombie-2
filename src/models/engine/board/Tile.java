package models.engine.board;

import models.core.plant.Plant;
import models.core.zombie.Zombie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Tile {
    private static final int MAX_PLANT_LAYERS = 2;
    private static final int GRAVE_HEALTH = 700;
    private static final int ICE_HEALTH = 600;
    private static final int BARREL_HEALTH = 1100;
    private static final int ARCADE_HEALTH = 1100;

    private final Position position;
    private final List<Zombie> zombies;
    private final List<Plant> plants;
    private TileType tileType;
    private int terrainHealth;

    public Tile(Position position) {
        this(position, TileType.NORMAL);
    }

    public Tile(Position position, TileType tileType) {
        if (position == null) {
            throw new IllegalArgumentException("Tile position cannot be null.");
        }
        if (tileType == null) {
            throw new IllegalArgumentException("Tile type cannot be null.");
        }

        this.position = position;
        this.tileType = tileType;
        this.zombies = new ArrayList<>();
        this.plants = new ArrayList<>();
        this.terrainHealth = initialTerrainHealth(tileType);
    }

    public Position getPosition() {
        return position;
    }

    public TileType getTileType() {
        return tileType;
    }

    public Plant getCurrentPlant() {
        if (plants.isEmpty()) {
            return null;
        }
        return plants.get(plants.size() - 1);
    }

    public Plant getBottomPlant() {
        return plants.isEmpty() ? null : plants.get(0);
    }

    public List<Plant> getPlants() {
        return Collections.unmodifiableList(plants);
    }

    public int getPlantLayerCount() {
        return plants.size();
    }

    public boolean hasPlant() {
        return !plants.isEmpty();
    }

    public boolean hasPlant(Plant plant) {
        return plant != null && plants.contains(plant);
    }

    public boolean hasPlantNamed(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return false;
        }
        for (Plant plant : plants) {
            if (plant != null && plant.getName().equalsIgnoreCase(plantName.trim())) {
                return true;
            }
        }
        return false;
    }

    public List<Zombie> getZombies() {
        return Collections.unmodifiableList(zombies);
    }

    public boolean hasZombies() {
        return !zombies.isEmpty();
    }

    public boolean isPlantable() {
        return tileType == TileType.NORMAL
                || tileType == TileType.LOW_TIDE
                || tileType == TileType.NECROMANCY;
    }

    public boolean canPlacePlant(Plant plant) {
        if (plant == null || plants.size() >= maximumPlantLayers(plant)) {
            return false;
        }

        if (tileType == TileType.GRAVE) {
            return plants.isEmpty() && isNamedPlant(plant, "grave buster");
        }
        if (tileType == TileType.ICE) {
            return plants.isEmpty() && isNamedPlant(plant, "hot potato");
        }
        if (tileType == TileType.SLIPPERY_UP
                || tileType == TileType.SLIPPERY_DOWN) {
            return false;
        }

        if (tileType == TileType.WATER) {
            return canPlaceOnWater(plant);
        }

        if (!isPlantable()) {
            return false;
        }

        if (plants.isEmpty()) {
            return true;
        }

        return canStack(plants.get(plants.size() - 1), plant);
    }

    public boolean isFrozenTerrain() {
        return tileType == TileType.ICE && terrainHealth > 0;
    }

    public boolean hasDamageableTerrain() {
        return (tileType == TileType.GRAVE
                || tileType == TileType.ICE
                || tileType == TileType.BARREL
                || tileType == TileType.ARCADE)
                && terrainHealth > 0;
    }

    public int getTerrainHealth() {
        return terrainHealth;
    }

    public int getMaximumTerrainHealth() {
        return initialTerrainHealth(tileType);
    }

    public boolean damageTerrain(int amount, boolean fireDamage) {
        if (!hasDamageableTerrain() || amount <= 0) {
            return false;
        }

        if (fireDamage && tileType == TileType.ICE) {
            terrainHealth = 0;
        } else {
            terrainHealth = Math.max(0, terrainHealth - amount);
        }

        if (terrainHealth == 0) {
            tileType = TileType.NORMAL;
            return true;
        }
        return false;
    }

    public void setTileType(TileType tileType) {
        if (tileType == null) {
            throw new IllegalArgumentException("Tile type cannot be null.");
        }

        this.tileType = tileType;
        this.terrainHealth = initialTerrainHealth(tileType);
    }

    public void placePlant(Plant plant) {
        if (plant == null) {
            throw new IllegalArgumentException("Plant cannot be null.");
        }
        if (plants.size() >= maximumPlantLayers(plant)) {
            throw new IllegalStateException("The tile has reached its plant layer limit.");
        }
        if (!plants.isEmpty() && !canStack(plants.get(plants.size() - 1), plant)) {
            throw new IllegalStateException("The plants cannot be stacked on this tile.");
        }

        plants.add(plant);
    }

    public Plant removePlant() {
        if (plants.isEmpty()) {
            return null;
        }
        return plants.remove(plants.size() - 1);
    }

    public boolean removePlant(Plant plant) {
        return plant != null && plants.remove(plant);
    }

    public List<Plant> removeUnsupportedWaterPlants() {
        if (tileType != TileType.WATER || plants.isEmpty()) {
            return Collections.emptyList();
        }

        boolean hasLilyPad = hasPlantNamed("Lily Pad");
        List<Plant> removed = new ArrayList<>();
        for (Plant plant : new ArrayList<>(plants)) {
            if (isDirectWaterPlant(plant) || hasLilyPad) {
                continue;
            }
            plants.remove(plant);
            removed.add(plant);
        }
        return removed;
    }

    public void removeZombie(Zombie zombie) {
        zombies.remove(zombie);
    }

    public void clearZombies() {
        zombies.clear();
    }

    public void addZombie(Zombie zombie) {
        if (zombie == null) {
            throw new IllegalArgumentException("Zombie cannot be null.");
        }
        if (!zombies.contains(zombie)) {
            zombies.add(zombie);
        }
    }

    private int maximumPlantLayers(Plant incomingPlant) {
        if (incomingPlant != null && normalize(incomingPlant.getName()).equals("pea pod")) {
            for (Plant existing : plants) {
                if (!normalize(existing.getName()).equals("pea pod")) {
                    return MAX_PLANT_LAYERS;
                }
            }
            return 5;
        }
        return MAX_PLANT_LAYERS;
    }

    private boolean canPlaceOnWater(Plant plant) {
        if (plants.isEmpty()) {
            return isDirectWaterPlant(plant);
        }

        if (isLilyPad(plant)) {
            return false;
        }

        if (hasPlantNamed("Lily Pad")) {
            return true;
        }

        return canStack(plants.get(plants.size() - 1), plant)
                && isDirectWaterPlant(plant);
    }

    private boolean canStack(Plant lowerPlant, Plant upperPlant) {
        if (lowerPlant == null || upperPlant == null) {
            return false;
        }
        if (isLilyPad(upperPlant)) {
            return false;
        }
        if (isLilyPad(lowerPlant)) {
            return true;
        }

        String lowerName = normalize(lowerPlant.getName());
        String upperName = normalize(upperPlant.getName());

        if (lowerName.equals("pumpkin")) {
            return false;
        }
        if (upperName.equals("pumpkin")) {
            return true;
        }

        if (lowerName.equals("pea pod") || upperName.equals("pea pod")) {
            return lowerName.equals("pea pod") && upperName.equals("pea pod");
        }

        return containsTag(lowerPlant, "stack") || containsTag(upperPlant, "stack");
    }

    private boolean isDirectWaterPlant(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return false;
        }
        if (containsTag(plant, "water") || containsTag(plant, "aquatic")) {
            return true;
        }
        String name = normalize(plant.getName());
        return name.equals("lily pad")
                || name.equals("tangle kelp")
                || name.equals("sea shroom");
    }

    private boolean isLilyPad(Plant plant) {
        return plant != null && normalize(plant.getName()).equals("lily pad");
    }

    private boolean isNamedPlant(Plant plant, String expectedName) {
        return plant != null && normalize(plant.getName()).equals(normalize(expectedName));
    }

    private boolean containsTag(Plant plant, String expectedTag) {
        String tags = plant.getType().getTags();
        if (tags == null || tags.isBlank()) {
            return false;
        }
        String normalizedTags = tags.toLowerCase(Locale.ROOT)
                .replace('[', ' ')
                .replace(']', ' ')
                .replace(',', ' ')
                .replace('-', ' ')
                .replace('_', ' ');
        for (String tag : normalizedTags.trim().split("\\s+")) {
            if (tag.equals(expectedTag)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }

    private int initialTerrainHealth(TileType type) {
        if (type == TileType.GRAVE) {
            return GRAVE_HEALTH;
        }
        if (type == TileType.ICE) {
            return ICE_HEALTH;
        }
        if (type == TileType.BARREL) {
            return BARREL_HEALTH;
        }
        if (type == TileType.ARCADE) {
            return ARCADE_HEALTH;
        }
        return 0;
    }
}
