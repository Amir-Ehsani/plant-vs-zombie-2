package models.core.plant;

import models.core.base.JsonDataLoader;

import java.util.Map;

public final class DefaultPlantRegistry {
    private static final int TICKS_PER_SECOND = 10;
    private static final String PLANT_DATA_PATH = "models/core/plant/plants.json";
    private static final PlantRegistry INSTANCE = createRegistry();

    private DefaultPlantRegistry() {
    }

    public static PlantRegistry getInstance() {
        return INSTANCE;
    }

    private static PlantRegistry createRegistry() {
        PlantRegistry registry = new PlantRegistry();
        Map<String, Object> root = JsonDataLoader.loadObject(PLANT_DATA_PATH);

        for (Object value : JsonDataLoader.getArray(root, "plants")) {
            Map<String, Object> data = JsonDataLoader.asObject(value);
            registry.register(new PlantType(
                    JsonDataLoader.getString(data, "name", "Peashooter"),
                    JsonDataLoader.getString(data, "category", "Shooter"),
                    JsonDataLoader.getString(data, "tags", ""),
                    JsonDataLoader.getInt(data, "sunCost", 100),
                    JsonDataLoader.getInt(data, "baseHp", 300),
                    JsonDataLoader.getString(data, "damage", "20"),
                    JsonDataLoader.getString(data, "baseAbility", ""),
                    JsonDataLoader.getString(data, "plantFoodEffect", "none"),
                    JsonDataLoader.getString(data, "level2Upgrade", ""),
                    JsonDataLoader.getString(data, "level3Upgrade", ""),
                    JsonDataLoader.getString(data, "level4Upgrade", ""),
                    toTicks(JsonDataLoader.getDouble(data, "actionIntervalSeconds", 0)),
                    toTicks(JsonDataLoader.getDouble(data, "rechargeSeconds", 0))
            ));
        }

        if (registry.getAllPlantTypes().isEmpty()) {
            throw new IllegalStateException("No plant types were loaded from " + PLANT_DATA_PATH + ".");
        }
        return registry;
    }

    private static int toTicks(double seconds) {
        return seconds <= 0 ? 0 : (int) Math.ceil(seconds * TICKS_PER_SECOND);
    }
}
