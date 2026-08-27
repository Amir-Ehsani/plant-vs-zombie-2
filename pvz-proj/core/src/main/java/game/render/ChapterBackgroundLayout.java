package game.render;

import models.level.core.SeasonType;

/**
 * Chapter-specific visual offsets for the gameplay background.
 *
 * <p>Only the center Frostbite Caves image is shifted. Left/right background
 * pieces and every other chapter keep their original position.</p>
 */
public final class ChapterBackgroundLayout {
    /**
     * Edit only this value to move the Frostbite Caves center image vertically.
     * Negative values move it down; positive values move it up.
     */
    public static final float FROSTBITE_CENTER_Y_OFFSET = -18f;

    private ChapterBackgroundLayout() {
    }

    public static float centerYOffset(SeasonType seasonType) {
        return seasonType == SeasonType.FROSTBITE_CAVES
            ? FROSTBITE_CENTER_Y_OFFSET
            : 0f;
    }
}
