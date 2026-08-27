package game.render;

import models.level.core.SeasonType;


public final class ChapterBackgroundLayout {

    public static final float FROSTBITE_CENTER_Y_OFFSET = -5f;

    private ChapterBackgroundLayout() {
    }

    public static float centerYOffset(SeasonType seasonType) {
        return seasonType == SeasonType.FROSTBITE_CAVES
            ? FROSTBITE_CENTER_Y_OFFSET
            : 0f;
    }
}
