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

    private boolean selectionLocked;

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
                    "Locked selection slot count is invalid."
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
        this.selectionLocked = false;
    }

    @Override
    public SpecialLevelType getType() {
        return SpecialLevelType.LOCKED_PLANTS;
    }

    @Override
    public void onLevelStart(LevelRuntimeContext context) {
        resetResult();

        if (selectedPlants.isEmpty()) {
            throw new IllegalStateException(
                    "At least one plant must be selected before starting the level."
            );
        }

        selectionLocked = true;
    }

    @Override
    public void onTick(LevelRuntimeContext context) {
    }

    @Override
    public boolean isPlantAllowed(String plantName) {
        if (!isValidName(plantName)) {
            return false;
        }

        String normalizedName = plantName.trim();

        if (selectionLocked) {
            return containsIgnoreCase(selectedPlants, normalizedName);
        }

        return canSelectPlant(normalizedName);
    }

    public boolean selectPlant(String plantName) {
        if (selectionLocked || !isValidName(plantName)) {
            return false;
        }

        String normalizedName = plantName.trim();

        if (containsIgnoreCase(selectedPlants, normalizedName)) {
            return true;
        }

        if (!canSelectPlant(normalizedName)) {
            return false;
        }

        selectedPlants.add(normalizedName);

        String familyName = findFamily(normalizedName);
        if (familyName != null) {
            selectedPlantByFamily.put(familyName, normalizedName);
        }

        return true;
    }

    public boolean deselectPlant(String plantName) {
        if (selectionLocked || !isValidName(plantName)) {
            return false;
        }

        String selectedName = findSelectedName(plantName);
        if (selectedName == null) {
            return false;
        }

        selectedPlants.remove(selectedName);

        String familyName = findFamily(selectedName);
        if (familyName != null) {
            selectedPlantByFamily.remove(familyName);
        }

        return true;
    }

    public boolean resetSelections() {
        if (selectionLocked) {
            return false;
        }

        selectedPlants.clear();
        selectedPlantByFamily.clear();
        return true;
    }

    @Override
    public void onPlantUsed(String plantName) {

    }

    public boolean isSelectionLocked() {
        return selectionLocked;
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
        return selectedPlants.size() == getAvailableSelectionSlotCount();
    }

    public Set<String> getUnavailablePlants() {
        return Collections.unmodifiableSet(unavailablePlants);
    }

    public Set<String> getSelectedPlants() {
        return Collections.unmodifiableSet(selectedPlants);
    }

    public Map<String, String> getSelectedPlantByFamily() {
        return Collections.unmodifiableMap(selectedPlantByFamily);
    }

    public Map<String, Set<String>> getPlantFamilies() {
        Map<String, Set<String>> readOnlyFamilies = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : plantFamilies.entrySet()) {
            readOnlyFamilies.put(
                    entry.getKey(),
                    Collections.unmodifiableSet(entry.getValue())
            );
        }

        return Collections.unmodifiableMap(readOnlyFamilies);
    }

    private boolean canSelectPlant(String plantName) {
        if (containsIgnoreCase(unavailablePlants, plantName)) {
            return false;
        }

        if (containsIgnoreCase(selectedPlants, plantName)) {
            return true;
        }

        if (selectedPlants.size() >= getAvailableSelectionSlotCount()) {
            return false;
        }

        String familyName = findFamily(plantName);
        if (familyName == null) {
            return true;
        }

        String selectedFamilyPlant = selectedPlantByFamily.get(familyName);

        return selectedFamilyPlant == null
                || selectedFamilyPlant.equalsIgnoreCase(plantName);
    }

    private Set<String> copyPlantNames(List<String> source) {
        Set<String> copiedNames = new LinkedHashSet<>();

        for (String plantName : source) {
            if (!isValidName(plantName)) {
                throw new IllegalArgumentException(
                        "Plant names cannot be null or empty."
                );
            }

            String normalizedName = plantName.trim();

            if (!containsIgnoreCase(copiedNames, normalizedName)) {
                copiedNames.add(normalizedName);
            }
        }

        return copiedNames;
    }

    private Map<String, Set<String>> copyPlantFamilies(Map<String, List<String>> source) {
        Map<String, Set<String>> copiedFamilies = new LinkedHashMap<>();
        Set<String> registeredPlants = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            copyPlantFamily(entry, copiedFamilies, registeredPlants);
        }
        return copiedFamilies;
    }

    private void copyPlantFamily(
            Map.Entry<String, List<String>> entry,
            Map<String, Set<String>> copiedFamilies,
            Set<String> registeredPlants
    ) {
        String familyName = entry.getKey();
        List<String> familyPlants = entry.getValue();
        validateFamily(familyName, familyPlants);
        String normalizedFamilyName = familyName.trim();
        if (containsIgnoreCase(copiedFamilies.keySet(), normalizedFamilyName)) {
            throw new IllegalArgumentException("Duplicate plant family: " + normalizedFamilyName);
        }
        Set<String> copiedPlants = new LinkedHashSet<>();
        for (String plantName : familyPlants) {
            copiedPlants.add(registerFamilyPlant(plantName, registeredPlants));
        }
        copiedFamilies.put(normalizedFamilyName, copiedPlants);
    }

    private void validateFamily(String familyName, List<String> familyPlants) {
        if (!isValidName(familyName)) {
            throw new IllegalArgumentException("Plant family name cannot be null or empty.");
        }
        if (familyPlants == null || familyPlants.isEmpty()) {
            throw new IllegalArgumentException("Plant family cannot be null or empty.");
        }
    }

    private String registerFamilyPlant(String plantName, Set<String> registeredPlants) {
        if (!isValidName(plantName)) {
            throw new IllegalArgumentException("Plant family cannot contain null or empty names.");
        }
        String normalizedName = plantName.trim();
        if (containsIgnoreCase(registeredPlants, normalizedName)) {
            throw new IllegalArgumentException(
                    "A plant cannot belong to multiple families: " + normalizedName
            );
        }
        registeredPlants.add(normalizedName);
        return normalizedName;
    }


    private String findSelectedName(String plantName) {
        for (String selectedPlant : selectedPlants) {
            if (selectedPlant.equalsIgnoreCase(plantName.trim())) {
                return selectedPlant;
            }
        }

        return null;
    }

    private String findFamily(String plantName) {
        for (Map.Entry<String, Set<String>> entry : plantFamilies.entrySet()) {
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
        if (target == null) {
            return false;
        }

        String normalizedTarget = target.trim();

        for (String value : values) {
            if (value.equalsIgnoreCase(normalizedTarget)) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidName(String value) {
        return value != null && !value.isBlank();
    }
}
