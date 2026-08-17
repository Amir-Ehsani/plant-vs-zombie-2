package models.level.core;

import models.engine.board.Position;
import models.engine.board.TileType;

public final class AdventureChapterConfigurator {
    private AdventureChapterConfigurator() {
    }

    public static void configure(Level level, String chapterName, int levelNumber) {
        if (level == null) {
            throw new IllegalArgumentException("Level cannot be null.");
        }
        String chapter = AdventureLevelCatalog.normalizeChapterName(chapterName);
        switch (chapter) {
            case "ancient-egypt" -> configureAncientEgypt(level, levelNumber);
            case "ice-cave" -> configureFrostbiteCaves(level, levelNumber);
            case "wave-beach" -> configureBigWaveBeach(level, levelNumber);
            case "wild-west" -> configureDarkAges(level, levelNumber);
            default -> throw new IllegalArgumentException("Unknown chapter: " + chapterName);
        }
    }

    private static void configureAncientEgypt(Level level, int levelNumber) {
        level.bindSeasonType(SeasonType.ANCIENT_EGYPT);
        level.setTerrainTile(position(5, 1), TileType.GRAVE);
        level.setTerrainTile(position(6, 3), TileType.GRAVE);
        level.setTerrainTile(position(4, 5), TileType.GRAVE);
        if (levelNumber > 1) {
            level.setTerrainTile(position(7, 2), TileType.GRAVE);
        }
    }

    private static void configureFrostbiteCaves(Level level, int levelNumber) {
        level.bindSeasonType(SeasonType.FROSTBITE_CAVES);
        level.setTerrainTile(position(6, 1), TileType.ICE);
        level.setTerrainTile(position(7, 3), TileType.ICE);
        level.setTerrainTile(position(6, 5), TileType.ICE);
        level.setTerrainTile(position(4, 2), TileType.SLIPPERY_UP);
        level.setTerrainTile(position(5, 4), TileType.SLIPPERY_DOWN);
        level.scheduleInitialTerrainZombie(position(6, 1), "Default");
        if (levelNumber > 1) {
            level.scheduleInitialTerrainZombie(position(7, 3), "Default");
        }
    }

    private static void configureBigWaveBeach(Level level, int levelNumber) {
        level.bindSeasonType(SeasonType.BIG_WAVE_BEACH);
        for (int row = 1; row <= 5; row++) {
            level.setTerrainTile(position(8, row), TileType.WATER);
            level.setTerrainTile(position(9, row), TileType.WATER);
            level.setTerrainTile(position(6, row), TileType.LOW_TIDE);
            level.setTerrainTile(position(7, row), TileType.LOW_TIDE);
            if (levelNumber > 1) {
                level.setTerrainTile(position(5, row), TileType.LOW_TIDE);
            }
        }
        if (levelNumber > 1) {
            level.setHighTideWaterColumns(3);
        }
    }

    private static void configureDarkAges(Level level, int levelNumber) {
        level.bindSeasonType(SeasonType.DARK_AGES);
        level.setTerrainTile(position(5, 1), TileType.SUN_GRAVE);
        level.setTerrainTile(position(6, 3), TileType.PLANT_FOOD_GRAVE);
        level.setTerrainTile(position(4, 5), TileType.GRAVE);
        level.setTerrainTile(position(7, 2), TileType.NECROMANCY);
        level.setTerrainTile(position(7, 4), TileType.NECROMANCY);
        if (levelNumber > 1) {
            level.setTerrainTile(position(5, 3), TileType.GRAVE);
        }
    }

    private static Position position(int x, int y) {
        return new Position(x, y);
    }
}
