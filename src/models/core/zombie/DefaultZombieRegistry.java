package models.core.zombie;

public final class DefaultZombieRegistry {
    private static final int TICKS_PER_SECOND = 10;
    private static final ZombieRegistry INSTANCE = createRegistry();

    private DefaultZombieRegistry() {
    }

    public static ZombieRegistry getInstance() {
        return INSTANCE;
    }

    private static ZombieRegistry createRegistry() {
        ZombieRegistry registry = new ZombieRegistry();

        registerArmors(registry);
        registerZombies(registry);

        return registry;
    }

    private static void registerArmors(ZombieRegistry registry) {
        registry.registerArmor(new Armor("Cone", 370, "normal"));
        registry.registerArmor(new Armor("Bucket", 1100, "metallic"));
        registry.registerArmor(new Armor("Brick", 2200, "normal"));
        registry.registerArmor(new Armor("Shoulder Armor", 1600, "metallic"));
        registry.registerArmor(new Armor("Crown", 1600, "metallic"));
        registry.registerArmor(new Armor("Crown knights helmet", 1600, "metallic"));
        registry.registerArmor(new Armor("Newspaper", 800, "normal"));
    }

    private static void registerZombies(ZombieRegistry registry) {
        register(registry, "Default", 100, 190, 0.185, 100, "ZombieDefault", null);
        register(registry, "cone head", 100, 190, 0.185, 200, "ZombieArmor1", "Cone");
        register(registry, "bucket head", 100, 190, 0.185, 400, "ZombieArmor2", "Bucket");
        register(registry, "brick head", 100, 190, 0.185, 700, "ZombieArmor4", "Brick");
        register(registry, "knight", 100, 190, 0.185, 550, "ZombieDarkArmor3", "Crown + Shoulder Armor");
        register(registry, "Gargantuar", 1500, 3600, 0.24, 1500, "ZombieGargantuar", null);
        register(registry, "Imp", 100, 190, 0.22, 100, "ZombieImp", null);
        register(registry, "Ra", 100, 190, 0.2, 100, "ZombieRa", null);
        register(registry, "Explorer", 100, 250, 0.25, 250, "ZombieExplorer", null);
        register(registry, "Tomb raiser", 100, 380, 0.185, 300, "ZombieTombRaiser", null);
        register(registry, "Dodo", 100, 490, 0.3, 600, "ZombieIceAgeDodo", null);
        register(registry, "Hunter", 100, 700, 0.12, 500, "ZombieIceAgeHunter", null);
        register(registry, "Troglobite", 100, 470, 0.185, 600, "ZombieIceAgeTroglobite", null);
        register(registry, "Fisherman", 100, 1000, 0.185, 700, "ZombieBeachFisherman", null);
        register(registry, "Octopus", 100, 910, 0.12, 900, "ZombieBeachOctopus", null);
        register(registry, "Snorkel", 100, 350, 0.185, 200, "ZombieBeachSnorkel", null);
        register(registry, "Juggler", 100, 420, 0.2, 450, "ZombieDarkJuggler", null);
        register(registry, "Wizard", 100, 490, 0.12, 800, "ZombieWizard", null);
        register(registry, "King", 100, 1000, 0, 750, "ZombieDarkKing", null);
        register(registry, "Imp Dragon", 100, 190, 0.185, 150, "ZombieDarkImpDragon", null);
        register(registry, "Allstar", 100, 1100, 0.16, 1000, "ZombieModernAllStar", null);
        register(registry, "Arcade", 100, 490, 0.19, 600, "ZombieArcade", null);
        register(registry, "Umbrella", 100, 350, 0.25, 200, "ZombieLostCityJane", null);
        register(registry, "Turquoise", 100, 250, 0.185, 500, "ZombieCrystalSkull", null);
        register(registry, "Prospector", 100, 190, 0.16, 200, "ZombieProspector", null);
        register(registry, "Piano", 4000, 840, 0.12, 450, "ZombiePiano", null);
        register(registry, "News Paper", 200, 460, 0.22, 700, "ZombieNewspaper", "Newspaper");
    }

    private static void register(
            ZombieRegistry registry,
            String name,
            int eatDps,
            int hitpoints,
            double speed,
            int waveCost,
            String id,
            String armorName
    ) {
        int damagePerTick = toDamagePerTick(eatDps);
        ZombieType zombieType = new ZombieType(name, hitpoints, speed, damagePerTick, waveCost, id, armorName);
        registry.registerZombieType(zombieType);
    }

    private static int toDamagePerTick(int eatDps) {
        if (eatDps <= 0) {
            return 0;
        }

        return (int) Math.ceil((double) eatDps / TICKS_PER_SECOND);
    }
}