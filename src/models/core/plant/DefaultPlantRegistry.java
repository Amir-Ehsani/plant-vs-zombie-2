package models.core.plant;

public final class DefaultPlantRegistry {
    private static final int TICKS_PER_SECOND = 10;
    private static final PlantRegistry INSTANCE = createRegistry();

    private DefaultPlantRegistry() {
    }

    public static PlantRegistry getInstance() {
        return INSTANCE;
    }

    private static PlantRegistry createRegistry() {
        PlantRegistry registry = new PlantRegistry();

        register(registry, "Sunflower", 50, 300, 24, "Sun Producer");
        register(registry, "Twin Sunflower", 125, 300, 24, "Sun Producer");
        register(registry, "Sun-shroom", 25, 300, 24, "Sun Producer");
        register(registry, "Primal Sunflower", 75, 300, 24, "Sun Producer");
        register(registry, "Gold Bloom", 0, 0, 0, "Sun Producer");
        register(registry, "Peashooter", 100, 300, 1.5, "Shooter");
        register(registry, "Repeater", 200, 300, 1.5, "Shooter");
        register(registry, "Threepeater", 300, 300, 1.5, "Shooter");
        register(registry, "Snow Pea", 150, 300, 1.5, "Shooter");
        register(registry, "Rotobaga", 150, 300, 1.5, "Shooter");
        register(registry, "Pea Pod", 125, 300, 1.5, "Shooter");
        register(registry, "Split Pea", 125, 300, 1.5, "Shooter");
        register(registry, "Citron", 350, 300, 9, "Shooter");
        register(registry, "Caulipower", 250, 300, 12, "Homing");
        register(registry, "Electric Blueberry", 150, 300, 12, "Homing");
        register(registry, "Bowling Bulb", 200, 300, 2, "Shooter");
        register(registry, "Cactus", 175, 300, 1.5, "Strike-through");
        register(registry, "Fire Peashooter", 175, 300, 1.5, "Shooter");
        register(registry, "Starfruit", 150, 300, 1.5, "Shooter");
        register(registry, "Goo Peashooter", 125, 300, 1.5, "Shooter");
        register(registry, "Mega Gatling Pea", 400, 300, 1.5, "Shooter");
        register(registry, "Sea-shroom", 0, 300, 1.5, "Shooter");
        register(registry, "Puff-shroom", 0, 300, 1.5, "Shooter");
        register(registry, "Fume-shroom", 125, 300, 1.5, "Strike-through");
        register(registry, "Cabbage-pult", 100, 300, 2.9, "Lobber");
        register(registry, "Kernel-pult", 100, 300, 2.9, "Lobber");
        register(registry, "Melon-pult", 325, 300, 2.9, "Lobber");
        register(registry, "Winter Melon", 500, 300, 2.9, "Lobber");
        register(registry, "Pepper-pult", 200, 300, 2.9, "Lobber");
        register(registry, "Potato Mine", 25, 300, 0, "Explosive");
        register(registry, "Primal Potato Mine", 50, 300, 0, "Explosive");
        register(registry, "Cherry Bomb", 150, 0, 0, "Explosive");
        register(registry, "Squash", 50, 300, 0, "Explosive");
        register(registry, "Grapeshot", 150, 0, 0, "Explosive");
        register(registry, "Jalapeno", 125, 0, 0, "Explosive");
        register(registry, "Doom-shroom", 125, 0, 0, "Explosive");
        register(registry, "Tangle Kelp", 25, 300, 0, "Explosive");
        register(registry, "Iceberg Lettuce", 0, 300, 0, "Explosive");
        register(registry, "Bonk Choy", 150, 300, 0.25, "Melee");
        register(registry, "Phat Beet", 150, 300, 2, "Melee");
        register(registry, "Chomper", 150, 300, 40, "Melee");
        register(registry, "Wasabi Whip", 150, 300, 2, "Melee");
        register(registry, "Kiwibeast", 175, 300, 2, "Melee");
        register(registry, "Wall-nut", 50, 4000, 0, "Wall-nut");
        register(registry, "Tall-nut", 125, 8000, 0, "Wall-nut");
        register(registry, "Endurian", 100, 3000, 0, "Wall-nut");
        register(registry, "Garlic", 50, 300, 0, "Wall-nut");
        register(registry, "Sweet Potato", 150, 3000, 0, "Wall-nut");
        register(registry, "Explode-o-nut", 50, 4000, 0, "Wall-nut");
        register(registry, "Pumpkin", 150, 4000, 0, "Wall-nut");
        register(registry, "Sun Bean", 50, 1000, 0, "Wall-nut");
        register(registry, "Torchwood", 175, 300, 0, "Modifier");
        register(registry, "Magnet-shroom", 100, 300, 10, "Homing");
        register(registry, "Hypno-shroom", 125, 300, 0, "Modifier");
        register(registry, "Cat-tail", 175, 300, 1.5, "Homing");
        register(registry, "Imitater", 0, 0, 0, "Modifier");
        register(registry, "Ice-shroom", 75, 0, 0, "Explosive");
        register(registry, "Lily Pad", 25, 300, 0, "Modifier");
        register(registry, "Hot Potato", 0, 0, 0, "Explosive");
        register(registry, "Grave Buster", 0, 0, 0, "Explosive");
        register(registry, "Enlighten-mint", 0, 0, 0, "Sun Producer");
        register(registry, "Appease-mint", 0, 0, 0, "Shooter");
        register(registry, "Arma-mint", 0, 0, 0, "Lobber");

        return registry;
    }

    private static void register(
            PlantRegistry registry,
            String name,
            int sunCost,
            int baseHp,
            double actionIntervalSeconds,
            String category
    ) {
        int cooldownTicks = toCooldownTicks(actionIntervalSeconds);
        registry.register(new PlantType(name, sunCost, baseHp, cooldownTicks, category));
    }

    private static int toCooldownTicks(double seconds) {
        if (seconds <= 0) {
            return 0;
        }

        return (int) Math.ceil(seconds * TICKS_PER_SECOND);
    }
}
