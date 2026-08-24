package ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PvzAnimationCatalog {
    private static final String IMAGES_ROOT = "pvz-assets/IMAGES/";
    private static final String PLANT_FALLBACK = "768/INITIAL/PLANT/SUNFLOWER/SUNFLOWER.PAM";
    private static final String ZOMBIE_FALLBACK = "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM";
    private static final Map<String, String> PLANT_CACHE = new HashMap<>();
    private static final Map<String, String> ZOMBIE_CACHE = new HashMap<>();
    private static final Map<String, String> PLANT_ALIASES = Map.ofEntries(
            Map.entry("twin sunflower", "SUNFLOWER_TWIN"),
            Map.entry("rotobaga", "ROTORUTABAGA"),
            Map.entry("mega gatling pea", "MEGAGATLING"),
            Map.entry("kernel pult", "KERNALPULT"),
            Map.entry("iceberg lettuce", "HEADBUTTER_LETTUCE"),
            Map.entry("phat beet", "PHATBEETS"),
            Map.entry("primal sunflower", "PRIMAL_SUNFLOWER"),
            Map.entry("primal potato mine", "PRIMAL_POTATOMINE"),
            Map.entry("pierce mint", "SPEARMINT"),
            Map.entry("cattail mint", "AILMINT"),
            Map.entry("cat tail mint", "AILMINT"),
            Map.entry("cat tail", "PEASHOOTER")
    );
    private static final Map<String, String> ZOMBIE_NAME_ALIASES = Map.ofEntries(
            Map.entry("default", "ZOMBIE_EGYPT_BASIC"),
            Map.entry("cone head", "ZOMBIE_CARNIE_CONEHEAD"),
            Map.entry("bucket head", "ZOMBIE_CARNIE_BUCKETHEAD"),
            Map.entry("brick head", "ZOMBIE_ICEAGE_BASIC_BRICK"),
            Map.entry("knight", "ZOMBIE_DARK_BASIC"),
            Map.entry("gargantuar", "EGYPT_GARGANTUAR"),
            Map.entry("imp", "ZOMBIE_EGYPT_IMP"),
            Map.entry("ra", "ZOMBIE_EGYPT_RA"),
            Map.entry("explorer", "ZOMBIE_EXPLORER"),
            Map.entry("tomb raiser", "ZOMBIE_EGYPT_TOMBRAISER"),
            Map.entry("dodo", "ZOMBIE_ICEAGE_DODORIDER"),
            Map.entry("hunter", "ZOMBIE_ICEAGE_HUNTER"),
            Map.entry("troglobite", "ZOMBIE_ICEAGE_TROGLOBITE"),
            Map.entry("fisherman", "ZOMBIE_BEACH_FISHERMAN"),
            Map.entry("octopus", "ZOMBIE_BEACH_OCTOPUS"),
            Map.entry("snorkel", "ZOMBIE_BEACH_SNORKELER"),
            Map.entry("juggler", "ZOMBIE_DARK_JESTER"),
            Map.entry("wizard", "ZOMBIE_DARK_WIZARD"),
            Map.entry("king", "ZOMBIE_DARK_KING"),
            Map.entry("imp dragon", "ZOMBIE_DARK_IMP_DRAGON"),
            Map.entry("allstar", "ZOMBIE_MODERN_ALLSTAR"),
            Map.entry("arcade", "ZOMBIE_80S_ARCADE"),
            Map.entry("umbrella", "ZOMBIE_LOSTCITY_JANE"),
            Map.entry("turquoise", "ZOMBIE_LOSTCITY_CRYSTALSKULL"),
            Map.entry("prospector", "ZOMBIE_PROSPECTOR"),
            Map.entry("piano", "ZOMBIE_PIANO"),
            Map.entry("news paper", "ZOMBIE_MODERN_NEWSPAPER"),
            Map.entry("barrel roller", "ZOMBIE_PIRATE_BARREL_PUSHER")
    );
    private static final Map<String, String> ZOMBIE_ID_ALIASES = Map.ofEntries(
            Map.entry("ZombieDefault", "ZOMBIE_EGYPT_BASIC"),
            Map.entry("ZombieArmor1", "ZOMBIE_CARNIE_CONEHEAD"),
            Map.entry("ZombieArmor2", "ZOMBIE_CARNIE_BUCKETHEAD"),
            Map.entry("ZombieArmor4", "ZOMBIE_ICEAGE_BASIC_BRICK"),
            Map.entry("ZombieDarkArmor3", "ZOMBIE_DARK_BASIC"),
            Map.entry("ZombieGargantuar", "EGYPT_GARGANTUAR"),
            Map.entry("ZombieImp", "ZOMBIE_EGYPT_IMP"),
            Map.entry("ZombieRa", "ZOMBIE_EGYPT_RA"),
            Map.entry("ZombieExplorer", "ZOMBIE_EXPLORER"),
            Map.entry("ZombieTombRaiser", "ZOMBIE_EGYPT_TOMBRAISER"),
            Map.entry("ZombieIceAgeDodo", "ZOMBIE_ICEAGE_DODORIDER"),
            Map.entry("ZombieIceAgeHunter", "ZOMBIE_ICEAGE_HUNTER"),
            Map.entry("ZombieIceAgeTroglobite", "ZOMBIE_ICEAGE_TROGLOBITE"),
            Map.entry("ZombieBeachFisherman", "ZOMBIE_BEACH_FISHERMAN"),
            Map.entry("ZombieBeachOctopus", "ZOMBIE_BEACH_OCTOPUS"),
            Map.entry("ZombieBeachSnorkel", "ZOMBIE_BEACH_SNORKELER"),
            Map.entry("ZombieDarkJuggler", "ZOMBIE_DARK_JESTER"),
            Map.entry("ZombieWizard", "ZOMBIE_DARK_WIZARD"),
            Map.entry("ZombieDarkKing", "ZOMBIE_DARK_KING"),
            Map.entry("ZombieDarkImpDragon", "ZOMBIE_DARK_IMP_DRAGON"),
            Map.entry("ZombieModernAllStar", "ZOMBIE_MODERN_ALLSTAR"),
            Map.entry("ZombieArcade", "ZOMBIE_80S_ARCADE"),
            Map.entry("ZombieLostCityJane", "ZOMBIE_LOSTCITY_JANE"),
            Map.entry("ZombieCrystalSkull", "ZOMBIE_LOSTCITY_CRYSTALSKULL"),
            Map.entry("ZombieProspector", "ZOMBIE_PROSPECTOR"),
            Map.entry("ZombiePiano", "ZOMBIE_PIANO"),
            Map.entry("ZombieNewspaper", "ZOMBIE_MODERN_NEWSPAPER"),
            Map.entry("ZombieBarrelRoller", "ZOMBIE_PIRATE_BARREL_PUSHER")
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

    public static String zombiePath(String zombieName) {
        return zombiePath(zombieName, null);
    }

    public static String zombiePath(String zombieName, String zombieId) {
        String cacheKey = normalizeName(zombieName) + "|" + normalizeId(zombieId);
        if (ZOMBIE_CACHE.containsKey(cacheKey)) {
            return ZOMBIE_CACHE.get(cacheKey);
        }
        List<String> tokens = new ArrayList<>();
        String idAlias = ZOMBIE_ID_ALIASES.get(normalizeId(zombieId));
        String nameAlias = ZOMBIE_NAME_ALIASES.get(normalizeName(zombieName));
        if (idAlias != null) {
            tokens.add(idAlias);
        }
        if (nameAlias != null && !tokens.contains(nameAlias)) {
            tokens.add(nameAlias);
        }
        tokens.add(underscoredToken(zombieName));
        tokens.add(compactToken(zombieName));
        tokens.add(underscoredToken(zombieId));
        tokens.add(compactToken(zombieId));
        String path = findZombiePath(tokens);
        ZOMBIE_CACHE.put(cacheKey, path);
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

    private static String findZombiePath(List<String> tokens) {
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            for (String path : zombieCandidates(token)) {
                if (exists(path)) {
                    return path;
                }
            }
        }
        return ZOMBIE_FALLBACK;
    }

    private static List<String> plantCandidates(String token) {
        return List.of(
                pamPath("768/INITIAL/PLANT", token),
                pamPath("768/FULL/PLANT", token),
                pamPath("768/INITIAL/EMPOWERMINTS/PLANT", token),
                pamPath("768/FULL/EMPOWERMINTS/PLANT", token)
        );
    }

    private static List<String> zombieCandidates(String token) {
        return List.of(
                pamPath("768/FULL/ZOMBIE", token),
                pamPath("768/INITIAL/ZOMBIE", token)
        );
    }

    private static String pamPath(String parent, String token) {
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

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim();
    }
}
