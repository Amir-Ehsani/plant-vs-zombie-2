package models.core.zombie;

import models.core.base.JsonDataLoader;

import java.util.Map;

public final class DefaultZombieRegistry {
    private static final int TICKS_PER_SECOND = 10;
    private static final String ZOMBIE_DATA_PATH = "models/core/zombie/zombies.json";
    private static final ZombieRegistry INSTANCE = createRegistry();

    private DefaultZombieRegistry() {
    }

    public static ZombieRegistry getInstance() {
        return INSTANCE;
    }

    private static ZombieRegistry createRegistry() {
        ZombieRegistry registry = new ZombieRegistry();
        Map<String, Object> root = JsonDataLoader.loadObject(ZOMBIE_DATA_PATH);

        for (Object value : JsonDataLoader.getArray(root, "armors")) {
            Map<String, Object> data = JsonDataLoader.asObject(value);
            registry.registerArmor(new Armor(
                    JsonDataLoader.getString(data, "name", "Armor"),
                    JsonDataLoader.getInt(data, "hp", 1),
                    JsonDataLoader.getString(data, "type", "normal")
            ));
        }

        for (Object value : JsonDataLoader.getArray(root, "zombies")) {
            Map<String, Object> data = JsonDataLoader.asObject(value);
            registry.registerZombieType(new ZombieType(
                    JsonDataLoader.getString(data, "name", "Default"),
                    JsonDataLoader.getInt(data, "baseHp", 190),
                    JsonDataLoader.getDouble(data, "speed", 0.185),
                    toDamagePerTick(JsonDataLoader.getInt(data, "eatDps", 100)),
                    JsonDataLoader.getInt(data, "waveCost", 100),
                    JsonDataLoader.getString(data, "id", "ZombieDefault"),
                    nullableText(JsonDataLoader.getString(data, "defaultArmor", "")),
                    JsonDataLoader.getStringList(data, "tags"),
                    JsonDataLoader.getString(data, "ability", "")
            ));
        }

        if (registry.getAllZombieTypes().isEmpty()) {
            throw new IllegalStateException("No zombie types were loaded from " + ZOMBIE_DATA_PATH + ".");
        }
        return registry;
    }

    private static String nullableText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int toDamagePerTick(int eatDps) {
        return eatDps <= 0 ? 0 : (int) Math.ceil((double) eatDps / TICKS_PER_SECOND);
    }
}
