package ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import models.core.zombie.ZombieType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PvzAnimationCatalog {
    private static final String IMAGES_ROOT = "pvz-assets/IMAGES/";
    private static final String PLANT_FALLBACK = "768/INITIAL/PLANT/SUNFLOWER/SUNFLOWER.PAM";
    private static final String ZOMBIE_FALLBACK = "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL/ZOMBIE_TUTORIAL.PAM";
    private static final Map<String, String> PLANT_CACHE = new HashMap<>();
    private static final Map<String, String> ZOMBIE_CACHE = new HashMap<>();
    private static final Map<String, String> PLANT_ALIASES = Map.ofEntries(
            Map.entry("twin sunflower", "SUNFLOWER_TWIN"),
            Map.entry("rotobaga", "ROTORUTABAGA"),
            Map.entry("mega gatling pea", "MEGAGATLING"),
            Map.entry("kernel pult", "KERNALPULT"),
            Map.entry("iceberg lettuce", "ICEBURG"),
            Map.entry("phat beet", "PHATBEETS"),
            Map.entry("primal sunflower", "PRIMAL_SUNFLOWER"),
            Map.entry("primal potato mine", "PRIMAL_POTATOMINE"),
            Map.entry("pierce mint", "SPEARMINT"),
            Map.entry("cattail mint", "AILMINT"),
            Map.entry("cat tail mint", "AILMINT"),
            Map.entry("cat tail", "HOMINGTHISTLE")
    );
    private static final Map<String, String> ZOMBIE_ID_PATHS = Map.ofEntries(
            Map.entry("ZombieDefault", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL/ZOMBIE_TUTORIAL.PAM"),
            Map.entry("ZombieArmor1", "768/FULL/ZOMBIE/LNY_CONEHEAD_ZOMBIE/LNY_CONEHEAD_ZOMBIE.PAM"),
            Map.entry("ZombieArmor2", "768/FULL/ZOMBIE/LNY_BUCKETHEAD_ZOMBIE/LNY_BUCKETHEAD_ZOMBIE.PAM"),
            Map.entry("ZombieArmor4", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC_BRICK/ZOMBIE_DARK_BASIC_BRICK.PAM"),
            Map.entry("ZombieDarkArmor3", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM"),
            Map.entry("ZombieGargantuar", "768/FULL/ZOMBIE/GARGANTUAR/GARGANTUAR.PAM"),
            Map.entry("ZombieImp", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL_IMP/ZOMBIE_TUTORIAL_IMP.PAM"),
            Map.entry("ZombieRa", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM"),
            Map.entry("ZombieExplorer", "768/INITIAL/ZOMBIE/ZOMBIE_EXPLORER/ZOMBIE_EXPLORER.PAM"),
            Map.entry("ZombieTombRaiser", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_TOMBRAISER/ZOMBIE_EGYPT_TOMBRAISER.PAM"),
            Map.entry("ZombieIceAgeDodo", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_DODORIDER/ZOMBIE_ICEAGE_DODORIDER.PAM"),
            Map.entry("ZombieIceAgeHunter", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_HUNTER/ZOMBIE_ICEAGE_HUNTER.PAM"),
            Map.entry("ZombieIceAgeTroglobite", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_TROGLOBITE/ZOMBIE_ICEAGE_TROGLOBITE.PAM"),
            Map.entry("ZombieBeachFisherman", "768/FULL/ZOMBIE/ZOMBIE_BEACH_FISHERMAN/ZOMBIE_BEACH_FISHERMAN.PAM"),
            Map.entry("ZombieBeachOctopus", "768/FULL/ZOMBIE/ZOMBIE_BEACH_OCTOPUS/ZOMBIE_BEACH_OCTOPUS.PAM"),
            Map.entry("ZombieBeachSnorkel", "768/FULL/ZOMBIE/ZOMBIE_BEACH_SNORKELER/ZOMBIE_BEACH_SNORKELER.PAM"),
            Map.entry("ZombieDarkJuggler", "768/FULL/ZOMBIE/ZOMBIE_DARK_JESTER/ZOMBIE_DARK_JESTER.PAM"),
            Map.entry("ZombieWizard", "768/FULL/ZOMBIE/ZOMBIE_DARK_WIZARD/ZOMBIE_DARK_WIZARD.PAM"),
            Map.entry("ZombieDarkKing", "768/FULL/ZOMBIE/ZOMBIE_DARK_KING/ZOMBIE_DARK_KING.PAM"),
            Map.entry("ZombieDarkImpDragon", "768/FULL/ZOMBIE/ZOMBIE_DARK_IMP_DRAGON/ZOMBIE_DARK_IMP_DRAGON.PAM"),
            Map.entry("ZombieModernAllStar", "768/FULL/ZOMBIE/ZOMBIE_MODERN_ALLSTAR/ZOMBIE_MODERN_ALLSTAR.PAM"),
            Map.entry("ZombieArcade", "768/FULL/ZOMBIE/ZOMBIE_80S_ARCADE/ZOMBIE_80S_ARCADE.PAM"),
            Map.entry("ZombieLostCityJane", "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_JANE/ZOMBIE_LOSTCITY_JANE.PAM"),
            Map.entry("ZombieCrystalSkull", "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_CRYSTALSKULL/ZOMBIE_LOSTCITY_CRYSTALSKULL.PAM"),
            Map.entry("ZombieProspector", "768/FULL/ZOMBIE/ZOMBIE_PROSPECTOR/ZOMBIE_PROSPECTOR.PAM"),
            Map.entry("ZombiePiano", "768/FULL/ZOMBIE/ZOMBIE_PIANO/ZOMBIE_PIANO.PAM"),
            Map.entry("ZombieNewspaper", "768/FULL/ZOMBIE/ZOMBIE_MODERN_NEWSPAPER/ZOMBIE_MODERN_NEWSPAPER.PAM"),
            Map.entry("ZombieBarrelRoller", "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER/ZOMBIE_PIRATE_BARREL_PUSHER.PAM")
    );
    private static final Map<String, String> ZOMBIE_PATHS = Map.ofEntries(
            Map.entry("default", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL/ZOMBIE_TUTORIAL.PAM"),
            Map.entry("cone head", "768/FULL/ZOMBIE/LNY_CONEHEAD_ZOMBIE/LNY_CONEHEAD_ZOMBIE.PAM"),
            Map.entry("bucket head", "768/FULL/ZOMBIE/LNY_BUCKETHEAD_ZOMBIE/LNY_BUCKETHEAD_ZOMBIE.PAM"),
            Map.entry("brick head", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC_BRICK/ZOMBIE_DARK_BASIC_BRICK.PAM"),
            Map.entry("knight", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM"),
            Map.entry("gargantuar", "768/FULL/ZOMBIE/GARGANTUAR/GARGANTUAR.PAM"),
            Map.entry("imp", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL_IMP/ZOMBIE_TUTORIAL_IMP.PAM"),
            Map.entry("ra", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM"),
            Map.entry("explorer", "768/INITIAL/ZOMBIE/ZOMBIE_EXPLORER/ZOMBIE_EXPLORER.PAM"),
            Map.entry("tomb raiser", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_TOMBRAISER/ZOMBIE_EGYPT_TOMBRAISER.PAM"),
            Map.entry("dodo", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_DODORIDER/ZOMBIE_ICEAGE_DODORIDER.PAM"),
            Map.entry("hunter", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_HUNTER/ZOMBIE_ICEAGE_HUNTER.PAM"),
            Map.entry("troglobite", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_TROGLOBITE/ZOMBIE_ICEAGE_TROGLOBITE.PAM"),
            Map.entry("fisherman", "768/FULL/ZOMBIE/ZOMBIE_BEACH_FISHERMAN/ZOMBIE_BEACH_FISHERMAN.PAM"),
            Map.entry("octopus", "768/FULL/ZOMBIE/ZOMBIE_BEACH_OCTOPUS/ZOMBIE_BEACH_OCTOPUS.PAM"),
            Map.entry("snorkel", "768/FULL/ZOMBIE/ZOMBIE_BEACH_SNORKELER/ZOMBIE_BEACH_SNORKELER.PAM"),
            Map.entry("juggler", "768/FULL/ZOMBIE/ZOMBIE_DARK_JESTER/ZOMBIE_DARK_JESTER.PAM"),
            Map.entry("wizard", "768/FULL/ZOMBIE/ZOMBIE_DARK_WIZARD/ZOMBIE_DARK_WIZARD.PAM"),
            Map.entry("king", "768/FULL/ZOMBIE/ZOMBIE_DARK_KING/ZOMBIE_DARK_KING.PAM"),
            Map.entry("imp dragon", "768/FULL/ZOMBIE/ZOMBIE_DARK_IMP_DRAGON/ZOMBIE_DARK_IMP_DRAGON.PAM"),
            Map.entry("allstar", "768/FULL/ZOMBIE/ZOMBIE_MODERN_ALLSTAR/ZOMBIE_MODERN_ALLSTAR.PAM"),
            Map.entry("arcade", "768/FULL/ZOMBIE/ZOMBIE_80S_ARCADE/ZOMBIE_80S_ARCADE.PAM"),
            Map.entry("umbrella", "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_JANE/ZOMBIE_LOSTCITY_JANE.PAM"),
            Map.entry("turquoise", "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_CRYSTALSKULL/ZOMBIE_LOSTCITY_CRYSTALSKULL.PAM"),
            Map.entry("prospector", "768/FULL/ZOMBIE/ZOMBIE_PROSPECTOR/ZOMBIE_PROSPECTOR.PAM"),
            Map.entry("piano", "768/FULL/ZOMBIE/ZOMBIE_PIANO/ZOMBIE_PIANO.PAM"),
            Map.entry("news paper", "768/FULL/ZOMBIE/ZOMBIE_MODERN_NEWSPAPER/ZOMBIE_MODERN_NEWSPAPER.PAM"),
            Map.entry("barrel roller", "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER/ZOMBIE_PIRATE_BARREL_PUSHER.PAM")
    );

    private PvzAnimationCatalog() {
    }

    public static String plantPath(String plantName) {
        String key = normalizeName(plantName);
        if (PLANT_CACHE.containsKey(key)) {
            return PLANT_CACHE.get(key);
        }
        String alias = PLANT_ALIASES.get(key);
        List<String> tokens = new ArrayList<>();
        if (alias != null) {
            tokens.add(alias);
        }
        tokens.add(compactToken(plantName));
        tokens.add(underscoredToken(plantName));
        String path = findPlantPath(tokens);
        PLANT_CACHE.put(key, path);
        return path;
    }

    public static String zombiePath(ZombieType zombieType) {
        if (zombieType == null) {
            return ZOMBIE_FALLBACK;
        }
        String mappedPath = ZOMBIE_ID_PATHS.get(zombieType.getId());
        if (mappedPath != null && exists(mappedPath)) {
            return mappedPath;
        }
        return zombiePath(zombieType.getName());
    }

    public static String zombiePath(String zombieName) {
        String key = normalizeName(zombieName);
        if (ZOMBIE_CACHE.containsKey(key)) {
            return ZOMBIE_CACHE.get(key);
        }
        String mappedPath = ZOMBIE_PATHS.get(key);
        String path = mappedPath != null && exists(mappedPath) ? mappedPath : ZOMBIE_FALLBACK;
        ZOMBIE_CACHE.put(key, path);
        return path;
    }

    private static String findPlantPath(List<String> tokens) {
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            for (String path : plantCandidates(token)) {
                if (exists(path)) {
                    return path;
                }
            }
        }
        return PLANT_FALLBACK;
    }

    private static List<String> plantCandidates(String token) {
        return List.of(
                plantPath("768/INITIAL/PLANT", token),
                plantPath("768/FULL/PLANT", token),
                plantPath("768/INITIAL/EMPOWERMINTS/PLANT", token),
                plantPath("768/FULL/EMPOWERMINTS/PLANT", token)
        );
    }

    private static String plantPath(String parent, String token) {
        return parent + "/" + token + "/" + token + ".PAM";
    }

    private static boolean exists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        FileHandle file = Gdx.files.internal(IMAGES_ROOT + relativePath);
        return file.exists();
    }

    private static String compactToken(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static String underscoredToken(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}
