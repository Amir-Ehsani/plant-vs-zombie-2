package models.level.core;

import models.engine.board.Position;
import models.engine.board.TileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
        level.setTerrainTile(position(4, 2), TileType.SLIPPERY_UP);
        level.setTerrainTile(position(5, 4), TileType.SLIPPERY_DOWN);

        List<Position> candidates = new ArrayList<>();
        for (int row = 1; row <= 5; row++) {
            for (int column = 6; column <= 8; column++) {
                candidates.add(position(column, row));
            }
        }
        Collections.shuffle(candidates, new Random(level.getLevelId() * 701L + levelNumber * 97L));
        int frozenCount = Math.min(4, Math.max(2, levelNumber + 1));
        for (int index = 0; index < frozenCount; index++) {
            level.scheduleInitialTerrainZombie(candidates.get(index), "Default");
        }
    }

    private static void configureBigWaveBeach(Level level, int levelNumber) {
        level.bindSeasonType(SeasonType.BIG_WAVE_BEACH);
        if (levelNumber == AdventureLevelCatalog.BOSS_LEVEL) {
            for (int row = 1; row <= 5; row++) {
                for (int column = 3; column <= 9; column++) {
                    level.setTerrainTile(position(column, row), TileType.WATER);
                }
            }
            return;
        }
        for (int row = 1; row <= 5; row++) {
            level.setTerrainTile(position(8, row), TileType.WATER);
            level.setTerrainTile(position(9, row), TileType.WATER);
        }

        int firstLowBeachColumn = levelNumber > 1 ? 5 : 6;
        List<Position> candidates = new ArrayList<>();
        for (int row = 1; row <= 5; row++) {
            for (int column = firstLowBeachColumn; column <= 7; column++) {
                candidates.add(position(column, row));
            }
        }
        Collections.shuffle(candidates, new Random(level.getLevelId() * 911L + levelNumber * 131L));
        int lowBeachCount = Math.min(5, Math.max(3, levelNumber + 2));
        for (int index = 0; index < lowBeachCount; index++) {
            level.setTerrainTile(candidates.get(index), TileType.LOW_TIDE);
        }
        if (levelNumber > 1) {
            level.setHighTideWaterColumns(3);
        }
    }

    private static void configureDarkAges(Level level, int levelNumber) {
        level.bindSeasonType(SeasonType.DARK_AGES);
        if (levelNumber == AdventureLevelCatalog.BOSS_LEVEL) {
            for (int row = 1; row <= 5; row++) {
                level.setTerrainTile(position(6, row), TileType.GRAVE);
                level.setTerrainTile(position(7, row), TileType.GRAVE);
            }
            return;
        }
        level.setTerrainTile(position(5, 1), TileType.SUN_GRAVE);
        level.setTerrainTile(position(6, 3), TileType.PLANT_FOOD_GRAVE);
        level.setTerrainTile(position(4, 5), TileType.GRAVE);
        if (levelNumber > 1) {
            level.setTerrainTile(position(5, 3), TileType.GRAVE);
        }
    }

    private static Position position(int x, int y) {
        return new Position(x, y);
    }
}
