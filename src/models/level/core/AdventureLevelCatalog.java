package models.level.core;

import models.level.rules.SpecialLevelType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public final class AdventureLevelCatalog {
    public static final int FIRST_PLAYABLE_LEVEL = 1;
    public static final int LAST_PLAYABLE_LEVEL = 3;
    public static final int BOSS_LEVEL = 4;

    private static final List<String> CHAPTERS = Collections.unmodifiableList(Arrays.asList(
            "ancient-egypt",
            "ice-cave",
            "wave-beach",
            "wild-west"
    ));

    private AdventureLevelCatalog() {
    }

    public static List<String> getChapterNames() {
        return CHAPTERS;
    }

    public static boolean chapterExists(String chapterName) {
        return CHAPTERS.contains(normalizeChapterName(chapterName));
    }

    public static String normalizeChapterName(String chapterName) {
        if (chapterName == null) {
            return "";
        }

        String normalized = chapterName.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-')
                .replaceAll("-+", "-");

        if ("ancientegypt".equals(normalized)) {
            return "ancient-egypt";
        }
        if ("icecave".equals(normalized)) {
            return "ice-cave";
        }
        if ("wavebeach".equals(normalized)) {
            return "wave-beach";
        }
        if ("wildwest".equals(normalized)) {
            return "wild-west";
        }

        return normalized;
    }

    public static String displayChapterName(String chapterName) {
        return switch (normalizeChapterName(chapterName)) {
            case "ancient-egypt" -> "Ancient Egypt";
            case "ice-cave" -> "Ice Cave";
            case "wave-beach" -> "Wave Beach";
            case "wild-west" -> "Wild West";
            default -> chapterName == null ? "" : chapterName.trim();
        };
    }

    public static String nextChapter(String chapterName) {
        int index = CHAPTERS.indexOf(normalizeChapterName(chapterName));
        if (index < 0 || index + 1 >= CHAPTERS.size()) {
            return null;
        }
        return CHAPTERS.get(index + 1);
    }

    public static int chapterIndex(String chapterName) {
        return CHAPTERS.indexOf(normalizeChapterName(chapterName));
    }

    public static boolean isPlayableLevel(int levelNumber) {
        return levelNumber >= FIRST_PLAYABLE_LEVEL && levelNumber <= LAST_PLAYABLE_LEVEL;
    }

    public static SpecialLevelType specialTypeFor(String chapterName, int levelNumber) {
        if (levelNumber == 1) {
            return SpecialLevelType.NONE;
        }

        return switch (normalizeChapterName(chapterName)) {
            case "ancient-egypt" -> levelNumber == 2
                    ? SpecialLevelType.CONVEYOR_BELT
                    : levelNumber == 3 ? SpecialLevelType.LOCKED_PLANTS : SpecialLevelType.NONE;
            case "ice-cave" -> levelNumber == 2
                    ? SpecialLevelType.SAVE_OUR_SEEDS
                    : levelNumber == 3 ? SpecialLevelType.TIMED_WAR : SpecialLevelType.NONE;
            case "wave-beach" -> levelNumber == 2
                    ? SpecialLevelType.NIGHT_OPS
                    : levelNumber == 3 ? SpecialLevelType.DEAD_LINE : SpecialLevelType.NONE;
            case "wild-west" -> levelNumber == 2
                    ? SpecialLevelType.LOVE_YOUR_PLANTS
                    : levelNumber == 3 ? SpecialLevelType.PLANT_WHAT_YOU_GET : SpecialLevelType.NONE;
            default -> SpecialLevelType.NONE;
        };
    }

    public static String levelTitle(String chapterName, int levelNumber) {
        if (levelNumber == 1) {
            return "Normal Defense";
        }
        if (levelNumber == BOSS_LEVEL) {
            return "Boss Battle (not implemented)";
        }

        return switch (specialTypeFor(chapterName, levelNumber)) {
            case CONVEYOR_BELT -> "Conveyor Belt";
            case LOCKED_PLANTS -> "Locked Plants";
            case SAVE_OUR_SEEDS -> "Save Our Seeds";
            case TIMED_WAR -> "Timed War";
            case NIGHT_OPS -> "Night Ops";
            case DEAD_LINE -> "Dead Line";
            case LOVE_YOUR_PLANTS -> "Love Your Plants";
            case PLANT_WHAT_YOU_GET -> "Plant What You Get";
            default -> "Normal Defense";
        };
    }

    public static String levelKind(String chapterName, int levelNumber) {
        if (levelNumber == 1) {
            return "NORMAL";
        }
        if (levelNumber == BOSS_LEVEL) {
            return "BOSS";
        }
        return "SPECIAL";
    }

    public static int levelId(String chapterName, int levelNumber) {
        int chapterIndex = chapterIndex(chapterName);
        if (chapterIndex < 0 || levelNumber < 1) {
            return levelNumber;
        }
        return (chapterIndex + 1) * 10 + levelNumber;
    }
}
