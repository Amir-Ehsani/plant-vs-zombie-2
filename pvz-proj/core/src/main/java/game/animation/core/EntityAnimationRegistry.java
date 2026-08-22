package game.animation.core;

import models.core.plant.Plant;
import models.core.zombie.Zombie;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EntityAnimationRegistry {
    private static final String PLANT_PATH = "/PLANT/";
    private static final String MINT_PATH = "/EMPOWERMINTS/PLANT/";
    private static final String ZOMBIE_PATH = "/ZOMBIE/";

    private final AnimationCatalog catalog;
    private final Map<String, String> plantAliases = new LinkedHashMap<>();
    private final Map<String, String> zombieAliases = new LinkedHashMap<>();

    public EntityAnimationRegistry(AnimationCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("Animation catalog cannot be null.");
        }
        this.catalog = catalog;
        initializePlantAliases();
        initializeZombieAliases();
    }

    public EntityAnimationProfile forPlant(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return null;
        }
        String normalized = normalize(plant.getName());
        String animationName = plantAliases.getOrDefault(normalized, plant.getName());
        AnimationDefinition definition = catalog.findByName(animationName, PLANT_PATH);
        if (definition == null) {
            definition = catalog.findByName(animationName, MINT_PATH);
        }
        if (definition == null) {
            return null;
        }
        return new EntityAnimationProfile(definition, plantScale(plant.getName()));
    }

    public EntityAnimationProfile forZombie(Zombie zombie) {
        if (zombie == null || zombie.getType() == null) {
            return null;
        }
        String id = zombie.getType().getId();
        String animationName = zombieAliases.getOrDefault(id, zombie.getName());
        AnimationDefinition definition = catalog.findByName(animationName, ZOMBIE_PATH);
        if (definition == null) {
            return null;
        }
        return new EntityAnimationProfile(definition, zombieScale(zombie));
    }

    private void initializePlantAliases() {
        plantAliases.put(normalize("Twin Sunflower"), "SUNFLOWER_TWIN");
        plantAliases.put(normalize("Rotobaga"), "ROTORUTABAGA");
        plantAliases.put(normalize("Mega Gatling Pea"), "MEGAGATLING");
        plantAliases.put(normalize("Phat Beet"), "PHATBEETS");
    }

    private void initializeZombieAliases() {
        zombieAliases.put("ZombieDefault", "ZOMBIE_EGYPT_BASIC");
        zombieAliases.put("ZombieArmor1", "ZOMBIE_EGYPT_BASIC");
        zombieAliases.put("ZombieArmor2", "ZOMBIE_EGYPT_BASIC");
        zombieAliases.put("ZombieArmor4", "ZOMBIE_ICEAGE_BASIC_BRICK");
        zombieAliases.put("ZombieDarkArmor3", "ZOMBIE_DARK_BASIC");
        zombieAliases.put("ZombieGargantuar", "EGYPT_GARGANTUAR");
        zombieAliases.put("ZombieImp", "ZOMBIE_EGYPT_IMP");
        zombieAliases.put("ZombieRa", "ZOMBIE_EGYPT_RA");
        zombieAliases.put("ZombieExplorer", "ZOMBIE_EXPLORER");
        zombieAliases.put("ZombieTombRaiser", "ZOMBIE_EGYPT_TOMBRAISER");
        zombieAliases.put("ZombieIceAgeDodo", "ZOMBIE_ICEAGE_DODORIDER");
        zombieAliases.put("ZombieIceAgeHunter", "ZOMBIE_ICEAGE_HUNTER");
        zombieAliases.put("ZombieIceAgeTroglobite", "ZOMBIE_ICEAGE_TROGLOBITE");
        zombieAliases.put("ZombieBeachFisherman", "ZOMBIE_BEACH_FISHERMAN");
        zombieAliases.put("ZombieBeachOctopus", "ZOMBIE_BEACH_OCTOPUS");
        zombieAliases.put("ZombieBeachSnorkel", "ZOMBIE_BEACH_SNORKELER");
        zombieAliases.put("ZombieDarkJuggler", "ZOMBIE_DARK_JESTER");
        zombieAliases.put("ZombieWizard", "ZOMBIE_DARK_WIZARD");
        zombieAliases.put("ZombieDarkKing", "ZOMBIE_DARK_KING");
        zombieAliases.put("ZombieDarkImpDragon", "ZOMBIE_DARK_IMP_DRAGON");
        zombieAliases.put("ZombieModernAllStar", "ZOMBIE_MODERN_ALLSTAR");
        zombieAliases.put("ZombieArcade", "ZOMBIE_80S_ARCADE");
        zombieAliases.put("ZombieLostCityJane", "ZOMBIE_LOSTCITY_JANE");
        zombieAliases.put("ZombieCrystalSkull", "ZOMBIE_LOSTCITY_CRYSTALSKULL");
        zombieAliases.put("ZombieProspector", "ZOMBIE_PROSPECTOR");
        zombieAliases.put("ZombiePiano", "ZOMBIE_PIANO");
        zombieAliases.put("ZombieNewspaper", "ZOMBIE_MODERN_NEWSPAPER");
        zombieAliases.put("ZombieBarrelRoller", "ZOMBIE_PIRATE_BARREL_PUSHER");
    }

    private float plantScale(String name) {
        String normalized = normalize(name);
        if (normalized.equals(normalize("Lily Pad"))) {
            return 0.44f;
        }
        if (normalized.equals(normalize("Pumpkin"))) {
            return 0.52f;
        }
        return 0.48f;
    }

    private float zombieScale(Zombie zombie) {
        String id = zombie.getType().getId();
        if (id.equals("ZombieGargantuar")) {
            return 0.68f;
        }
        if (id.equals("ZombieImp") || id.equals("ZombieDarkImpDragon")) {
            return 0.42f;
        }
        return 0.52f;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
