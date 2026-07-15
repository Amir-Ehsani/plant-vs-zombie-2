package models.level.rules.impl;

import models.level.rules.AbstractLevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.SpecialLevelType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LockedPlantsRule extends AbstractLevelRule {
    private static final int DEFAULT_TOTAL_SELECTION_SLOTS = 8;
    private static final int DEFAULT_LOCKED_SELECTION_SLOTS = 3;

    private final int totalSelectionSlots;
    private final int lockedSelectionSlots;

    private final Set<String> unavailablePlants;
    private final Map<String, Set<String>> plantFamilies;

    private final Set<String> selectedPlants;
    private final Map<String, String> selectedPlantByFamily;

    public LockedPlantsRule(
            List<String> unavailablePlants,
            Map<String, List<String>> plantFamilies
    ) {
        this(
                DEFAULT_TOTAL_SELECTION_SLOTS,
                DEFAULT_LOCKED_SELECTION_SLOTS,
                unavailablePlants,
                plantFamilies
        );
    }

    public LockedPlantsRule(
            int totalSelectionSlots,
            int lockedSelectionSlots,
            List<String> unavailablePlants,
            Map<String, List<String>> plantFamilies
    ) {
        if (totalSelectionSlots <= 0) {
            throw new IllegalArgumentException(
                    "Total selection slots must be positive."
            );
        }

        if (lockedSelectionSlots < 0
                || lockedSelectionSlots >= totalSelectionSlots) {
            throw new IllegalArgumentException(
                    "Locked slot count is invalid."
            );
        }

        if (unavailablePlants == null) {
            throw new IllegalArgumentException(
                    "Unavailable plants cannot be null."
            );
        }

        if (plantFamilies == null) {
            throw new IllegalArgumentException(
                    "Plant families cannot be null."
            );
        }

        this.totalSelectionSlots = totalSelectionSlots;
        this.lockedSelectionSlots = lockedSelectionSlots;
        this.unavailablePlants = copyPlantNames(unavailablePlants);
        this.plantFamilies = copyPlantFamilies(plantFamilies);

        this.selectedPlants = new LinkedHashSet<>();
        this.selectedPlantByFamily = new LinkedHashMap<>();
    }

    @Override
    public SpecialLevelType getType() {
        return SpecialLevelType.LOCKED_PLANTS;
    }

    @Override
    public void onLevelStart(LevelRuntimeContext context) {
        resetResult();
        selectedPlants.clear();
        selectedPlantByFamily.clear();
    }

    @Override
    public void onTick(LevelRuntimeContext context) {
    }

    @Override
    public boolean isPlantAllowed(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return false;
        }

        String normalizedName = plantName.trim();

        if (containsIgnoreCase(unavailablePlants, normalizedName)) {
            return false;
        }

        if (containsIgnoreCase(selectedPlants, normalizedName)) {
            return true;
        }

        String familyName = findFamily(normalizedName);

        if (familyName != null) {
            String selectedFamilyPlant =
                    selectedPlantByFamily.get(familyName);

            if (selectedFamilyPlant != null
                    && !selectedFamilyPlant.equalsIgnoreCase(normalizedName)) {
                return false;
            }
        }

        return selectedPlants.size() < getAvailableSelectionSlotCount();
    }

    public boolean selectPlant(String plantName) {
        if (!isPlantAllowed(plantName)) {
            return false;
        }

        String normalizedName = plantName.trim();

        if (containsIgnoreCase(selectedPlants, normalizedName)) {
            return true;
        }

        selectedPlants.add(normalizedName);

        String familyName = findFamily(normalizedName);

        if (familyName != null) {
            selectedPlantByFamily.putIfAbsent(
                    familyName,
                    normalizedName
            );
        }

        return true;
    }

    @Override
    public void onPlantUsed(String plantName) {
        selectPlant(plantName);
    }

    public int getTotalSelectionSlotCount() {
        return totalSelectionSlots;
    }

    public int getLockedSelectionSlotCount() {
        return lockedSelectionSlots;
    }

    public int getAvailableSelectionSlotCount() {
        return totalSelectionSlots - lockedSelectionSlots;
    }

    public int getRemainingSelectionSlotCount() {
        return Math.max(
                0,
                getAvailableSelectionSlotCount() - selectedPlants.size()
        );
    }

    public boolean areAllAvailableSlotsFilled() {
        return selectedPlants.size() >= getAvailableSelectionSlotCount();
    }

    public Set<String> getUnavailablePlants() {
        return Collections.unmodifiableSet(unavailablePlants);
    }

    public Set<String> getSelectedPlants() {
        return Collections.unmodifiableSet(selectedPlants);
    }

    public Map<String, Set<String>> getPlantFamilies() {
        Map<String, Set<String>> copy = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry
                : plantFamilies.entrySet()) {
            copy.put(
                    entry.getKey(),
                    Collections.unmodifiableSet(entry.getValue())
            );
        }

        return Collections.unmodifiableMap(copy);
    }

    public Map<String, String> getSelectedPlantByFamily() {
        return Collections.unmodifiableMap(selectedPlantByFamily);
    }

    private Set<String> copyPlantNames(List<String> source) {
        Set<String> copy = new LinkedHashSet<>();

        for (String plantName : source) {
            if (plantName == null || plantName.isBlank()) {
                throw new IllegalArgumentException(
                        "Plant names cannot be empty."
                );
            }

            String normalizedName = plantName.trim();

            if (!containsIgnoreCase(copy, normalizedName)) {
                copy.add(normalizedName);
            }
        }

        return copy;
    }

    private Map<String, Set<String>> copyPlantFamilies(
            Map<String, List<String>> source
    ) {
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        Set<String> registeredPlants = new LinkedHashSet<>();

        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            String familyName = entry.getKey();
            List<String> familyPlants = entry.getValue();

            if (familyName == null || familyName.isBlank()) {
                throw new IllegalArgumentException(
                        "Family name cannot be empty."
                );
            }

            if (familyPlants == null || familyPlants.isEmpty()) {
                throw new IllegalArgumentException(
                        "Plant family cannot be empty."
                );
            }

            Set<String> plantsCopy = new LinkedHashSet<>();

            for (String plantName : familyPlants) {
                if (plantName == null || plantName.isBlank()) {
                    throw new IllegalArgumentException(
                            "Plant family cannot contain empty names."
                    );
                }

                String normalizedName = plantName.trim();

                if (containsIgnoreCase(registeredPlants, normalizedName)) {
                    throw new IllegalArgumentException(
                            "A plant cannot belong to multiple families: "
                                    + normalizedName
                    );
                }

                registeredPlants.add(normalizedName);
                plantsCopy.add(normalizedName);
            }

            copy.put(familyName.trim(), plantsCopy);
        }

        return copy;
    }

    private String findFamily(String plantName) {
        for (Map.Entry<String, Set<String>> entry
                : plantFamilies.entrySet()) {
            if (containsIgnoreCase(entry.getValue(), plantName)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private boolean containsIgnoreCase(
            Collection<String> values,
            String target
    ) {
        for (String value : values) {
            if (value.equalsIgnoreCase(target)) {
                return true;
            }
        }

        return false;
    }
}