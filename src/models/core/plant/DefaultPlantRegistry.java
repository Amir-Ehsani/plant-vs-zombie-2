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

        register(registry, "Sunflower", 50, 300, 24.0, "Sun Producer", "Prod. Time -2s", "HP +150", "Double Sun Chance");
        register(registry, "Twin Sunflower", 125, 300, 24.0, "Sun Producer", "Prod. Time -2s", "HP +150", "Cost -25");
        register(registry, "Sun-shroom", 25, 300, 24.0, "Sun Producer", "Grow Time -5s", "HP +150", "Double Sun Chance");
        register(registry, "Primal Sunflower", 75, 300, 24.0, "Sun Producer", "Prod. Time -2s", "HP +150", "Cost -25");
        register(registry, "Gold Bloom", 0, 0, 0.0, "Sun Producer", "Cooldown -5s", "Sun +50", "Cost -25");
        register(registry, "Peashooter", 100, 300, 1.5, "Shooter", "Dmg +10", "HP +150", "Cost -25");
        register(registry, "Repeater", 200, 300, 1.5, "Shooter", "Dmg +10", "HP +200", "Cost -25");
        register(registry, "Threepeater", 300, 300, 1.5, "Shooter", "Cost -25", "Dmg +10", "HP +200");
        register(registry, "Snow Pea", 150, 300, 1.5, "Shooter", "Dmg +10", "Chill Time +2s", "Cost -25");
        register(registry, "Rotobaga", 150, 300, 1.5, "Shooter", "Dmg +10", "HP +150", "Cost -25");
        register(registry, "Pea Pod", 125, 300, 1.5, "Shooter", "Dmg +10", "HP +200", "Cost -25");
        register(registry, "Split Pea", 125, 300, 1.5, "Shooter", "Dmg +10", "HP +200", "Cost -25");
        register(registry, "Citron", 350, 300, 9.0, "Shooter", "Charge Time -1s", "Dmg +150", "Cost -50");
        register(registry, "Caulipower", 250, 300, 12.0, "Homing", "Cooldown -2s", "HP +150", "Cost -50");
        register(registry, "Electric Blueberry", 150, 300, 12.0, "Homing", "Cooldown -2s", "Target Priority Up", "Cost -25");
        register(registry, "Bowling Bulb", 200, 300, 2.0, "Shooter", "Regen -1s", "Dmg +15", "Cost -25");
        register(registry, "Cactus", 175, 300, 1.5, "Strike-through", "Pierce +1", "Dmg +10", "Cost -25");
        register(registry, "Fire Peashooter", 175, 300, 1.5, "Shooter", "Dmg +10", "HP +200", "Cost -25");
        register(registry, "Starfruit", 150, 300, 1.5, "Shooter", "Atk Speed +10%", "Dmg +10", "Cost -25");
        register(registry, "Goo Peashooter", 125, 300, 1.5, "Shooter", "Dmg/Tick +5", "HP +150", "Cost -25");
        register(registry, "Mega Gatling Pea", 400, 300, 1.5, "Shooter", "Dmg +10", "Plant Food Chance +5%", "Cost -50");
        register(registry, "Sea-shroom", 0, 300, 1.5, "Shooter", "Range +1 Tile", "Dmg +5", "Lifespan +10s");
        register(registry, "Puff-shroom", 0, 300, 1.5, "Shooter", "Lifespan +10s", "Dmg +10", "Range +1 Tile");
        register(registry, "Fume-shroom", 125, 300, 1.5, "Strike-through", "Range +1 Tile", "Dmg +10", "Cost -25");
        register(registry, "Cabbage-pult", 100, 300, 2.9, "Lobber", "Dmg +10", "Atk Speed +15%", "HP +150");
        register(registry, "Kernel-pult", 100, 300, 2.9, "Lobber", "Butter +5%", "Dmg +10", "HP +150");
        register(registry, "Melon-pult", 325, 300, 2.9, "Lobber", "Cost -25", "AoE Dmg +15", "Dmg +30");
        register(registry, "Winter Melon", 500, 300, 2.9, "Lobber", "Cost -50", "AoE Dmg +15", "Cost -25");
        register(registry, "Pepper-pult", 200, 300, 2.9, "Lobber", "Dmg +15", "Warmth Radius +1", "Cost -25");
        register(registry, "Potato Mine", 25, 300, 0.0, "Explosive", "Arm Time -3s", "Cooldown -5s", "Dmg +600");
        register(registry, "Primal Potato Mine", 50, 300, 0.0, "Explosive", "Arm Time -1s", "Cooldown -3s", "Dmg +400");
        register(registry, "Cherry Bomb", 150, 0, 0.0, "Explosive", "Cooldown -5s", "Dmg +600", "Cost -25");
        register(registry, "Squash", 50, 300, 0.0, "Explosive", "Cooldown -3s", "Dmg +600", "Can crush 2x");
        register(registry, "Grapeshot", 150, 0, 0.0, "Explosive", "Dmg +600", "Bounces +1", "Cost -25");
        register(registry, "Jalapeno", 125, 0, 0.0, "Explosive", "Cooldown -5s", "Dmg +600", "Cost -25");
        register(registry, "Doom-shroom", 125, 0, 0.0, "Explosive", "Cooldown -5s", "Dmg +800", "Cost -50");
        register(registry, "Tangle Kelp", 25, 300, 0.0, "Explosive", "Cooldown -5s", "Targets +1", "Cost -25");
        register(registry, "Iceberg Lettuce", 0, 300, 0.0, "Explosive", "Cooldown -2s", "Freeze Time +2s", "Cost -0");
        register(registry, "Bonk Choy", 150, 300, 0.25, "Melee", "Dmg +5", "Atk Speed +10%", "HP +200");
        register(registry, "Phat Beet", 150, 300, 2.0, "Melee", "Dmg +10", "Atk Speed +10%", "HP +200");
        register(registry, "Chomper", 150, 300, 40.0, "Melee", "Digest -2s", "HP +200", "Digest -3s");
        register(registry, "Wasabi Whip", 150, 300, 2.0, "Melee", "Dmg +10", "Range +1 Tile", "HP +200");
        register(registry, "Kiwibeast", 175, 300, 2.0, "Melee", "HP +200", "Dmg +15", "Max Size +1");
        register(registry, "Wall-nut", 50, 4000, 0.0, "Wall-nut", "HP +1000", "Cooldown -5s", "HP +1500");
        register(registry, "Tall-nut", 125, 8000, 0.0, "Wall-nut", "HP +2000", "Cooldown -5s", "HP +3000");
        register(registry, "Endurian", 100, 3000, 0.0, "Wall-nut", "Reflect Dmg +5", "HP +1000", "Cost -25");
        register(registry, "Garlic", 50, 300, 0.0, "Wall-nut", "HP +150", "Cooldown -3s", "HP +250");
        register(registry, "Sweet Potato", 150, 3000, 0.0, "Wall-nut", "HP +1000", "Cooldown -5s", "HP +1500");
        register(registry, "Explode-o-nut", 50, 4000, 0.0, "Wall-nut", "HP +1000", "Explode Dmg +200", "Cost -25");
        register(registry, "Pumpkin", 150, 4000, 0.0, "Wall-nut", "HP +1000", "Cooldown -5s", "HP +1500");
        register(registry, "Sun Bean", 50, 1000, 0.0, "Wall-nut", "Sun Drop +5", "HP +150", "Cost -25");
        register(registry, "Torchwood", 175, 300, 0.0, "Modifier", "HP +300", "AoE on Death", "Cost -25");
        register(registry, "Magnet-shroom", 100, 300, 10.0, "Homing", "Range +1 Tile", "Cooldown -5s", "HP +200");
        register(registry, "Hypno-shroom", 125, 300, 0.0, "Modifier", "Cost -25", "Zombie HP Buff", "Zombie Dmg Buff");
        register(registry, "Cat-tail", 175, 300, 1.5, "Homing", "Dmg +10", "HP +200", "Cost -25");
        register(registry, "Imitater", 0, 0, 0.0, "Modifier", "Cooldown -2s", "Cost -25", "plant food on enterance");
        register(registry, "Ice-shroom", 75, 0, 0.0, "Explosive", "Freeze Time +2s", "Cooldown -5s", "Dmg +50");
        register(registry, "Lily Pad", 25, 300, 0.0, "Modifier", "Cost -25", "HP +200", "Cooldown -2s");
        register(registry, "Hot Potato", 0, 0, 0.0, "Explosive", "Cooldown -2s", "Melt Area 3x3", "Explode on Finish");
        register(registry, "Grave Buster", 0, 0, 0.0, "Explosive", "Eat Time -1s", "Cooldown -2s", "Explode on Finish");
        register(registry, "Enlighten-mint", 0, 0, 0.0, "Sun Producer", "Duration +1s", "Cooldown -5s", "reset family cooldowns");
        register(registry, "Appease-mint", 0, 0, 0.0, "Shooter", "Duration +1s", "Cooldown -5s", "reset family cooldowns");
        register(registry, "Arma-mint", 0, 0, 0.0, "Lobber", "Duration +1s", "Cooldown -5s", "reset family cooldowns");
        register(registry, "Bombard-mint", 0, 0, 0.0, "Explosive", "Duration +1s", "Cooldown -5s", "reset family cooldowns");
        register(registry, "Enforce-mint", 0, 0, 0.0, "Melee", "Duration +1s", "Cooldown -5s", "reset family cooldowns");
        register(registry, "Reinforce-mint", 0, 0, 0.0, "Wall-nut", "Duration +1s", "Cooldown -5s", "reset family cooldowns");
        register(registry, "Enchant-mint", 0, 0, 0.0, "Modifier", "Duration +1s", "Cooldown -5s", "reset family cooldowns");
        register(registry, "Pierce-mint", 0, 0, 0.0, "Strike-through", "Duration +1s", "Cooldown -5s", "reset family cooldowns");
        register(registry, "catTail-mint", 0, 0, 0.0, "Homing", "Duration +1s", "Cooldown -5s", "reset family cooldowns");

        return registry;
    }

    private static void register(
            PlantRegistry registry,
            String name,
            int sunCost,
            int baseHp,
            double actionIntervalSeconds,
            String category,
            String level2Upgrade,
            String level3Upgrade,
            String level4Upgrade
    ) {
        registry.register(new PlantType(
                name,
                sunCost,
                baseHp,
                toTicks(actionIntervalSeconds),
                category,
                level2Upgrade,
                level3Upgrade,
                level4Upgrade
        ));
    }

    private static int toTicks(double seconds) {
        if (seconds <= 0) {
            return 0;
        }

        return (int) Math.ceil(seconds * TICKS_PER_SECOND);
    }
}