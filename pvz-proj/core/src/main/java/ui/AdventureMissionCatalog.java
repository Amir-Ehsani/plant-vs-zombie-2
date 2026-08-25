package ui;

import models.level.core.AdventureLevelCatalog;
import models.level.rules.SpecialLevelType;

public final class AdventureMissionCatalog {
    private AdventureMissionCatalog() {
    }

    public static String title(String chapterName, int levelNumber) {
        return AdventureLevelCatalog.displayChapterName(chapterName)
                + " - Level " + levelNumber;
    }

    public static String mission(String chapterName, int levelNumber) {
        SpecialLevelType type = AdventureLevelCatalog.specialTypeFor(chapterName, levelNumber);
        return switch (type) {
            case CONVEYOR_BELT -> "Survive using the plants delivered by the conveyor belt.";
            case LOCKED_PLANTS -> "Defend the lawn with the limited plant selection.";
            case SAVE_OUR_SEEDS -> "Protect the endangered plants and defeat every zombie.";
            case TIMED_WAR -> "Complete the objective before the timer runs out.";
            case NIGHT_OPS -> "Defend the lawn during the night with limited sun.";
            case DEAD_LINE -> "Don't let zombies cross the deadline.";
            case LOVE_YOUR_PLANTS -> "Keep the required plants alive until the battle ends.";
            case PLANT_WHAT_YOU_GET -> "Use the plants you receive and survive every wave.";
            default -> "Don't let zombies reach your house.";
        };
    }
}
