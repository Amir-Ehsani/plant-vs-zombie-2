package models.core.plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlantRegistry {
    private final Map<String, PlantType> plantTypesByName;

    public PlantRegistry() {
        this.plantTypesByName = new HashMap<>();
    }

    public void register(PlantType plantType) {
        if (plantType == null) {
            throw new IllegalArgumentException("Plant type cannot be null.");
        }

        String key = normalizeName(plantType.getName());

        if (key.isEmpty()) {
            throw new IllegalArgumentException("Plant type name cannot be empty.");
        }

        if (plantTypesByName.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate plant type: " + plantType.getName());
        }

        plantTypesByName.put(key, plantType);
    }

    public PlantType getByName(String plantName) {
        return plantTypesByName.get(normalizeName(plantName));
    }

    public boolean contains(String plantName) {
        return plantTypesByName.containsKey(normalizeName(plantName));
    }

    public List<PlantType> getAllPlantTypes() {
        List<PlantType> plantTypes = new ArrayList<>(plantTypesByName.values());
        plantTypes.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return Collections.unmodifiableList(plantTypes);
    }

    public List<String> getAllPlantNames() {
        List<String> plantNames = new ArrayList<>();

        for (PlantType plantType : getAllPlantTypes()) {
            plantNames.add(plantType.getName());
        }

        return Collections.unmodifiableList(plantNames);
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }

        return name
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}
