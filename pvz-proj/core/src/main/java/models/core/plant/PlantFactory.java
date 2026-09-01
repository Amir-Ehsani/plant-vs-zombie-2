package models.core.plant;

public class PlantFactory {
    private final PlantRegistry plantRegistry;

    public PlantFactory() {
        this(DefaultPlantRegistry.getInstance());
    }

    public PlantFactory(PlantRegistry plantRegistry) {
        if (plantRegistry == null) {
            throw new IllegalArgumentException("Plant registry cannot be null.");
        }

        this.plantRegistry = plantRegistry;
    }

    public Plant createPlant(PlantType type, double x, double y) {
        if (type == null) {
            throw new IllegalArgumentException("Plant type cannot be null.");
        }

        return new Plant(type, x, y);
    }

    public Plant createPlant(String plantName, double x, double y) {
        PlantType type = plantRegistry.getByName(plantName);

        if (type == null) {
            throw new IllegalArgumentException("Unknown plant type: " + plantName);
        }

        return createPlant(type, x, y);
    }

    public PlantRegistry getPlantRegistry() {
        return plantRegistry;
    }
}
