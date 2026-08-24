package game.render;

import models.level.core.Level;
import models.level.core.SeasonType;

public final class BoardBackgroundCatalog {
    private static final String EGYPT = "IMAGE_BACKGROUNDS_EGYPT_TEXTURE";
    private static final String FROSTBITE = "IMAGE_BACKGROUNDS_ICEAGE_TEXTURE";
    private static final String BEACH = "IMAGE_BACKGROUNDS_BEACH_TEXTURE";
    private static final String DARK_AGES = "IMAGE_BACKGROUNDS_DARK_TEXTURE";

    private BoardBackgroundCatalog() {
    }

    public static String resourceId(Level level) {
        if (level == null || level.getSeasonType() == null) {
            return EGYPT;
        }
        SeasonType season = level.getSeasonType();
        return switch (season) {
            case ANCIENT_EGYPT -> EGYPT;
            case FROSTBITE_CAVES -> FROSTBITE;
            case BIG_WAVE_BEACH -> BEACH;
            case DARK_AGES -> DARK_AGES;
        };
    }
}
