package models.level.core;

import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.core.zombie.ZombieRegistry;
import models.core.zombie.ZombieType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AdventureContentCatalog {
    private static final int FINAL_STAGE_ORDINAL = 11;
    private static final Map<String, Integer> PLANT_UNLOCK_STAGES = createPlantUnlockStages();
    private static final Map<String, Integer> ZOMBIE_UNLOCK_STAGES = createZombieUnlockStages();

    private AdventureContentCatalog() {
    }

    public static int stageOrdinal(String chapterName, int levelNumber) {
        int chapterIndex = AdventureLevelCatalog.chapterIndex(chapterName);
        if (chapterIndex < 0 || !AdventureLevelCatalog.isPlayableLevel(levelNumber)) {
            return -1;
        }
        int contentLevel = Math.min(levelNumber, AdventureLevelCatalog.LAST_CONTENT_LEVEL);
        return chapterIndex * AdventureLevelCatalog.LAST_CONTENT_LEVEL + contentLevel - 1;
    }

    public static List<String> plantNamesUnlockedThrough(
            String chapterName,
            int levelNumber,
            PlantRegistry registry
    ) {
        int targetStage = stageOrdinal(chapterName, levelNumber);
        List<String> names = new ArrayList<>();
        if (targetStage < 0 || registry == null) {
            return names;
        }

        for (PlantType type : registry.getAllPlantTypes()) {
            if (type != null && assignedPlantStage(type) <= targetStage) {
                names.add(type.getName());
            }
        }
        return names;
    }

    public static List<String> plantNamesUnlockedAt(
            String chapterName,
            int levelNumber,
            PlantRegistry registry
    ) {
        int targetStage = stageOrdinal(chapterName, levelNumber);
        List<String> names = new ArrayList<>();
        if (targetStage < 0 || registry == null) {
            return names;
        }

        for (PlantType type : registry.getAllPlantTypes()) {
            if (type != null && assignedPlantStage(type) == targetStage) {
                names.add(type.getName());
            }
        }
        return names;
    }

    public static List<String> zombieNamesUnlockedThrough(
            String chapterName,
            int levelNumber,
            ZombieRegistry registry
    ) {
        int targetStage = stageOrdinal(chapterName, levelNumber);
        SeasonType seasonType = seasonTypeForChapter(chapterName);
        List<String> names = new ArrayList<>();
        if (targetStage < 0 || registry == null || seasonType == null) {
            return names;
        }

        for (ZombieType type : registry.getAllZombieTypes()) {
            if (type != null
                    && seasonType.isZombieAllowed(type.getName())
                    && assignedZombieStage(type) <= targetStage) {
                names.add(type.getName());
            }
        }
        return names;
    }

    public static List<String> zombieNamesUnlockedAt(
            String chapterName,
            int levelNumber,
            ZombieRegistry registry
    ) {
        int targetStage = stageOrdinal(chapterName, levelNumber);
        SeasonType seasonType = seasonTypeForChapter(chapterName);
        List<String> names = new ArrayList<>();
        if (targetStage < 0 || registry == null || seasonType == null) {
            return names;
        }

        for (ZombieType type : registry.getAllZombieTypes()) {
            if (type != null
                    && seasonType.isZombieAllowed(type.getName())
                    && assignedZombieStage(type) == targetStage) {
                names.add(type.getName());
            }
        }
        return names;
    }


    public static List<String> plantNamesForLevel(
            String chapterName,
            int levelNumber,
            PlantRegistry registry
    ) {
        String chapter = AdventureLevelCatalog.normalizeChapterName(chapterName);
        if (levelNumber != AdventureLevelCatalog.BOSS_LEVEL) {
            List<String> plants = broadChapterPlantPool(chapter, levelNumber, registry);
            if (chapter.equals("ancient-egypt") && levelNumber == 2) {
                for (String conveyorPlant : conveyorPlantNamesForLevel(chapter, levelNumber, registry)) {
                    if (!containsIgnoreCase(plants, conveyorPlant)) {
                        plants.add(conveyorPlant);
                    }
                }
            }
            return plants;
        }
        List<String> requested = switch (chapter) {
            case "ancient-egypt" -> ancientEgyptPlants(levelNumber);
            case "ice-cave" -> frostbitePlants(levelNumber);
            case "wave-beach" -> bigWaveBeachPlants(levelNumber);
            case "wild-west" -> darkAgesPlants(levelNumber);
            default -> List.of();
        };
        return existingPlantNames(requested, registry);
    }

    public static List<String> conveyorPlantNamesForLevel(
            String chapterName,
            int levelNumber,
            PlantRegistry registry
    ) {
        String chapter = AdventureLevelCatalog.normalizeChapterName(chapterName);
        if (chapter.equals("ancient-egypt") && levelNumber == 2) {
            return existingPlantNames(List.of(
                    "Repeater", "Cabbage-pult", "Kernel-pult", "Bonk Choy",
                    "Wall-nut", "Potato Mine", "Iceberg Lettuce",
                    "Grave Buster", "Squash"
            ), registry);
        }
        return plantNamesForLevel(chapterName, levelNumber, registry);
    }

    public static List<String> zombieNamesForLevel(
            String chapterName,
            int levelNumber,
            ZombieRegistry registry
    ) {
        List<String> requested = switch (AdventureLevelCatalog.normalizeChapterName(chapterName)) {
            case "ancient-egypt" -> ancientEgyptZombies(levelNumber);
            case "ice-cave" -> frostbiteZombies(levelNumber);
            case "wave-beach" -> bigWaveBeachZombies(levelNumber);
            case "wild-west" -> darkAgesZombies(levelNumber);
            default -> List.of();
        };
        return existingZombieNames(requested, registry);
    }

    public static List<String> zombieNamesIntroducedAt(
            String chapterName,
            int levelNumber,
            ZombieRegistry registry
    ) {
        List<String> current = zombieNamesForLevel(chapterName, levelNumber, registry);
        if (levelNumber <= 1) {
            return current;
        }
        LinkedHashSet<String> previous = new LinkedHashSet<>(
                zombieNamesForLevel(chapterName, levelNumber - 1, registry)
        );
        List<String> result = new ArrayList<>();
        for (String name : current) {
            if (!containsIgnoreCase(previous, name)) {
                result.add(name);
            }
        }
        return result;
    }

    private static List<String> ancientEgyptPlants(int levelNumber) {
        if (levelNumber == AdventureLevelCatalog.BOSS_LEVEL) {
            return List.of(
                    "Repeater", "Bonk Choy", "Iceberg Lettuce", "Grave Buster",
                    "Wall-nut", "Potato Mine", "Cabbage-pult", "Kernel-pult"
            );
        }
        return List.of(
                "Sunflower", "Twin Sunflower", "Peashooter", "Repeater", "Threepeater",
                "Snow Pea", "Wall-nut", "Tall-nut", "Potato Mine", "Cherry Bomb",
                "Cabbage-pult", "Kernel-pult", "Bonk Choy", "Iceberg Lettuce",
                "Grave Buster", "Squash", "Jalapeno", "Torchwood", "Chomper",
                "Starfruit", "Split Pea", "Pea Pod", "Citron", "Endurian"
        );
    }

    private static List<String> frostbitePlants(int levelNumber) {
        if (levelNumber == AdventureLevelCatalog.BOSS_LEVEL) {
            return List.of(
                    "Hot Potato", "Pepper-pult", "Fire Peashooter", "Rotobaga",
                    "Threepeater", "Wall-nut", "Primal Potato Mine", "Winter Melon"
            );
        }
        return List.of(
                "Sunflower", "Twin Sunflower", "Primal Sunflower", "Peashooter",
                "Repeater", "Threepeater", "Snow Pea", "Wall-nut", "Tall-nut",
                "Potato Mine", "Primal Potato Mine", "Cherry Bomb", "Cabbage-pult",
                "Kernel-pult", "Melon-pult", "Winter Melon", "Hot Potato",
                "Pepper-pult", "Fire Peashooter", "Rotobaga", "Iceberg Lettuce",
                "Ice-shroom", "Bonk Choy", "Wasabi Whip", "Endurian", "Squash"
        );
    }

    private static List<String> bigWaveBeachPlants(int levelNumber) {
        if (levelNumber == AdventureLevelCatalog.BOSS_LEVEL) {
            return List.of(
                    "Lily Pad", "Banana Launcher", "Homing Thistle", "Guacodile",
                    "Tangle Kelp", "Bowling Bulb"
            );
        }
        return List.of(
                "Sunflower", "Twin Sunflower", "Peashooter", "Repeater", "Threepeater",
                "Wall-nut", "Tall-nut", "Potato Mine", "Cherry Bomb", "Cabbage-pult",
                "Kernel-pult", "Melon-pult", "Lily Pad", "Tangle Kelp", "Sea-shroom",
                "Bowling Bulb", "Guacodile", "Banana Launcher", "Homing Thistle",
                "Cat-tail", "Rotobaga", "Citron", "Chomper", "Squash", "Jalapeno"
        );
    }

    private static List<String> darkAgesPlants(int levelNumber) {
        if (levelNumber == AdventureLevelCatalog.BOSS_LEVEL) {
            return List.of(
                    "Puff-shroom", "Fume-shroom", "Pea-nut", "Kernel-pult",
                    "Magnet-shroom"
            );
        }
        return List.of(
                "Sun-shroom", "Sunflower", "Twin Sunflower", "Puff-shroom",
                "Fume-shroom", "Sun Bean", "Magnet-shroom", "Hypno-shroom",
                "Grave Buster", "Pea-nut", "Peashooter", "Repeater", "Threepeater",
                "Wall-nut", "Tall-nut", "Potato Mine", "Cherry Bomb", "Kernel-pult",
                "Cabbage-pult", "Melon-pult", "Bonk Choy", "Squash", "Jalapeno",
                "Torchwood", "Starfruit"
        );
    }

    private static List<String> ancientEgyptZombies(int levelNumber) {
        return List.of(
                "Default", "cone head", "bucket head", "Ra", "Explorer",
                "Tomb raiser", "Gargantuar", "Imp"
        );
    }

    private static List<String> frostbiteZombies(int levelNumber) {
        return List.of(
                "Default", "cone head", "bucket head", "brick head", "Dodo",
                "Hunter", "Troglobite", "Gargantuar", "Imp"
        );
    }

    private static List<String> bigWaveBeachZombies(int levelNumber) {
        return List.of(
                "Default", "cone head", "bucket head", "Snorkel",
                "Octopus", "Gargantuar", "Imp"
        );
    }

    private static List<String> darkAgesZombies(int levelNumber) {
        return List.of(
                "Default", "cone head", "bucket head", "knight", "Juggler",
                "Wizard", "Imp Dragon", "Gargantuar", "Imp"
        );
    }

    private static List<String> broadChapterPlantPool(
            String chapter,
            int levelNumber,
            PlantRegistry registry
    ) {
        List<String> result = new ArrayList<>();
        if (registry == null) {
            return result;
        }
        List<String> unlocked = plantNamesUnlockedThrough(chapter, levelNumber, registry);
        for (String plantName : unlocked) {
            if (!isChapterIncompatiblePlant(chapter, plantName)) {
                result.add(plantName);
            }
        }
        return result;
    }

    private static boolean isChapterIncompatiblePlant(String chapter, String plantName) {
        String plant = normalize(plantName);
        if (!chapter.equals("wave-beach") && isWaterOnlyPlant(plant)) {
            return true;
        }
        if (chapter.equals("ancient-egypt")) {
            return isFrostbitePlant(plant) || isBeachPlant(plant) || isDarkAgesPlant(plant);
        }
        if (chapter.equals("ice-cave")) {
            return isBeachPlant(plant) || isDarkAgesPlant(plant);
        }
        if (chapter.equals("wave-beach")) {
            return isDarkAgesPlant(plant);
        }
        return chapter.equals("wild-west") && plant.equals("hot potato");
    }

    private static boolean isWaterOnlyPlant(String plant) {
        return plant.equals("sea shroom")
                || plant.equals("tangle kelp")
                || plant.equals("cat tail")
                || plant.equals("lily pad");
    }

    private static boolean isFrostbitePlant(String plant) {
        return plant.equals("hot potato")
                || plant.equals("pepper pult")
                || plant.equals("fire peashooter")
                || plant.equals("rotobaga");
    }

    private static boolean isBeachPlant(String plant) {
        return isWaterOnlyPlant(plant)
                || plant.equals("bowling bulb")
                || plant.equals("guacodile")
                || plant.equals("banana launcher")
                || plant.equals("homing thistle");
    }

    private static boolean isDarkAgesPlant(String plant) {
        return plant.equals("sun shroom")
                || plant.equals("puff shroom")
                || plant.equals("fume shroom")
                || plant.equals("sun bean")
                || plant.equals("magnet shroom")
                || plant.equals("hypno shroom");
    }

    private static List<String> existingPlantNames(List<String> requested, PlantRegistry registry) {
        List<String> result = new ArrayList<>();
        if (registry == null) {
            return result;
        }
        for (String name : requested) {
            PlantType type = registry.getByName(name);
            if (type != null) {
                result.add(type.getName());
            }
        }
        return result;
    }

    private static List<String> existingZombieNames(List<String> requested, ZombieRegistry registry) {
        List<String> result = new ArrayList<>();
        if (registry == null) {
            return result;
        }
        for (String name : requested) {
            ZombieType type = registry.getZombieTypeByName(name);
            if (type != null) {
                result.add(type.getName());
            }
        }
        return result;
    }

    private static boolean containsIgnoreCase(Iterable<String> values, String target) {
        String normalizedTarget = normalize(target);
        for (String value : values) {
            if (normalize(value).equals(normalizedTarget)) {
                return true;
            }
        }
        return false;
    }

    private static SeasonType seasonTypeForChapter(String chapterName) {
        return switch (AdventureLevelCatalog.normalizeChapterName(chapterName)) {
            case "ancient-egypt" -> SeasonType.ANCIENT_EGYPT;
            case "ice-cave" -> SeasonType.FROSTBITE_CAVES;
            case "wave-beach" -> SeasonType.BIG_WAVE_BEACH;
            case "wild-west" -> SeasonType.DARK_AGES;
            default -> null;
        };
    }

    private static int assignedPlantStage(PlantType type) {
        String key = normalize(type.getName());
        Integer explicitStage = PLANT_UNLOCK_STAGES.get(key);
        if (explicitStage != null) {
            return explicitStage;
        }

        if (key.contains("mint")) {
            return FINAL_STAGE_ORDINAL;
        }

        String category = normalize(type.getCategory());
        int[] candidates;
        if (category.contains("sun producer")) {
            candidates = new int[]{1, 3, 6, 7, 9, 11};
        } else if (category.contains("shooter")) {
            candidates = new int[]{1, 3, 5, 6, 9, 10, 11};
        } else if (category.contains("strike")) {
            candidates = new int[]{1, 3, 7, 9, 11};
        } else if (category.contains("lobber")) {
            candidates = new int[]{1, 3, 5, 9, 11};
        } else if (category.contains("explosive")) {
            candidates = new int[]{2, 3, 5, 8, 10, 11};
        } else if (category.contains("melee")) {
            candidates = new int[]{2, 4, 9, 11};
        } else if (category.contains("wall")) {
            candidates = new int[]{2, 4, 8, 10, 11};
        } else if (category.contains("homing")) {
            candidates = new int[]{4, 6, 9, 11};
        } else if (category.contains("modifier")) {
            candidates = new int[]{2, 6, 8, 10, 11};
        } else {
            candidates = new int[]{2, 5, 8, 11};
        }
        return candidates[Math.floorMod(key.hashCode(), candidates.length)];
    }

    private static int assignedZombieStage(ZombieType type) {
        String key = normalize(type.getName());
        Integer explicitStage = ZOMBIE_UNLOCK_STAGES.get(key);
        if (explicitStage != null) {
            return explicitStage;
        }

        String searchable = key + " " + normalize(type.getId()) + " "
                + normalize(String.join(" ", type.getTags())) + " "
                + normalize(type.getAbility());

        if (containsAny(searchable, "egypt", "desert", "mummy", "tomb", "pharaoh")) {
            return 2;
        }
        if (containsAny(searchable, "ice", "frost", "dodo", "hunter", "troglo")) {
            return 4;
        }
        if (containsAny(searchable, "beach", "water", "snorkel", "fisher", "octo", "surfer")) {
            return 7;
        }
        if (containsAny(searchable, "dark", "wizard", "king", "juggler", "jester", "dragon", "medieval")) {
            return 9;
        }
        if (containsAny(searchable, "west", "cowboy", "prospector", "piano", "bull", "chicken")) {
            return 9;
        }
        if (containsAny(searchable, "modern", "arcade", "allstar", "umbrella", "turquoise", "newspaper")) {
            return 11;
        }

        int waveCost = type.getWaveCost();
        if (waveCost <= 150) {
            return 1;
        }
        if (waveCost <= 300) {
            return 3;
        }
        if (waveCost <= 500) {
            return 5;
        }
        if (waveCost <= 700) {
            return 8;
        }
        if (waveCost <= 1000) {
            return 10;
        }
        return FINAL_STAGE_ORDINAL;
    }

    private static Map<String, Integer> createPlantUnlockStages() {
        Map<String, Integer> stages = new LinkedHashMap<>();
        assign(stages, 0,
                "Sunflower", "Peashooter", "Wall-nut", "Potato Mine",
                "Cabbage-pult", "Kernel-pult", "Iceberg Lettuce", "Bonk Choy", "Cherry Bomb");
        assign(stages, 1,
                "Twin Sunflower", "Repeater", "Threepeater", "Snow Pea", "Grave Buster");
        assign(stages, 2,
                "Tall-nut", "Jalapeno", "Squash", "Torchwood", "Starfruit");
        assign(stages, 3,
                "Primal Sunflower", "Primal Potato Mine", "Hot Potato",
                "Pepper-pult", "Fire Peashooter", "Ice-shroom");
        assign(stages, 4,
                "Winter Melon", "Rotobaga", "Chomper", "Wasabi Whip", "Endurian");
        assign(stages, 5,
                "Citron", "Electric Blueberry", "Mega Gatling Pea", "Kiwibeast", "Caulipower");
        assign(stages, 6,
                "Lily Pad", "Tangle Kelp", "Sea-shroom", "Bowling Bulb", "Cat-tail");
        assign(stages, 7,
                "Sun-shroom", "Puff-shroom", "Fume-shroom", "Doom-shroom", "Magnet-shroom");
        assign(stages, 8,
                "Hypno-shroom", "Garlic", "Sweet Potato", "Pumpkin", "Sun Bean");
        assign(stages, 9,
                "Split Pea", "Pea Pod", "Melon-pult", "Cactus", "Phat Beet");
        assign(stages, 10,
                "Grapeshot", "Explode-o-nut", "Goo Peashooter", "Gold Bloom", "Imitater");
        assign(stages, 11,
                "Enlighten-mint", "Appease-mint", "Arma-mint", "Bombard-mint",
                "Enforce-mint", "Reinforce-mint", "Enchant-mint", "Pierce-mint", "catTail-mint");
        return stages;
    }

    private static Map<String, Integer> createZombieUnlockStages() {
        Map<String, Integer> stages = new LinkedHashMap<>();
        assign(stages, 0, "Default", "Normal", "cone head", "Conehead", "bucket head", "Buckethead");
        assign(stages, 1, "Ra", "Explorer");
        assign(stages, 2, "Tomb raiser", "brick head", "Brickhead");
        assign(stages, 3, "Dodo", "Hunter");
        assign(stages, 4, "Troglobite", "Imp");
        assign(stages, 5, "Gargantuar", "knight");
        assign(stages, 6, "Snorkel");
        assign(stages, 7, "Octopus", "Octo");
        assign(stages, 9, "Juggler", "Jester", "Wizard", "Imp Dragon");
        assign(stages, 10, "Prospector", "Piano");
        assign(stages, 11, "Allstar", "All-Star", "News Paper", "Newspaper", "Barrel Roller");
        assign(stages, 11, "Arcade", "Umbrella", "Turquoise");
        return stages;
    }

    private static void assign(Map<String, Integer> stages, int stage, String... names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            stages.put(normalize(name), stage);
        }
    }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}
