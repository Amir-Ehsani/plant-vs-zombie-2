package game.render;

import models.level.core.Level;
import models.level.core.SeasonType;
import models.level.rules.SpecialLevelType;

public final class BoardBackgroundCatalog {
    private static final String EGYPT = "IMAGE_BACKGROUNDS_EGYPT_TEXTURE";
    private static final String FROSTBITE = "IMAGE_BACKGROUNDS_ICEAGE_TEXTURE";
    private static final String BEACH = "IMAGE_BACKGROUNDS_BEACH_TEXTURE";
    private static final String DARK_AGES = "IMAGE_BACKGROUNDS_DARK_TEXTURE";
    private static final String SUMMER_NIGHTS = "IMAGE_BACKGROUNDS_BACKGROUND_LOD_SUMMERNIGHTS_TEXTURE";

    private BoardBackgroundCatalog() {
    }

    public static String resourceId(Level level) {
        if (level == null || level.getSeasonType() == null) {
            return EGYPT;
        }
        if (level.getLevelRule().getType() == SpecialLevelType.NIGHT_OPS) {
            return SUMMER_NIGHTS;
        }
        SeasonType season = level.getSeasonType();
        return switch (season) {
            case ANCIENT_EGYPT -> EGYPT;
            case FROSTBITE_CAVES -> FROSTBITE;
            case BIG_WAVE_BEACH -> BEACH;
            case DARK_AGES -> DARK_AGES;
        };
    }

    public static BackgroundResources resources(Level level) {
        String centerId = resourceId(level);
        return new BackgroundResources(centerId + "_LEFT", centerId, centerId + "_RIGHT");
    }

    public record BackgroundResources(String leftId, String centerId, String rightId) {
    }
}
