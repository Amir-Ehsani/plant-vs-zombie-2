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
        if (levelNumber != 2) {
            level.setTerrainTile(position(4, 2), TileType.SLIPPERY_UP);
            level.setTerrainTile(position(5, 4), TileType.SLIPPERY_DOWN);
        }

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
            setWaterColumns(level, 3, 9);
            level.setTideWaterRange(7, 7);
            return;
        }

        // Start with three ocean columns and let waves push the tide as far as five.
        // This keeps Night Ops (level 2) visibly aquatic from the first frame too.
        setWaterColumns(level, 7, 9);
        level.setTideWaterRange(3, 5);
        markRandomLowTideTiles(level, levelNumber);
    }

    private static void markRandomLowTideTiles(Level level, int levelNumber) {
        List<Position> candidates = new ArrayList<>();
        // Columns 1-4 counted from the right on a 9-column board are columns 9..6.
        for (int row = 1; row <= 5; row++) {
            for (int column = 6; column <= 9; column++) {
                candidates.add(position(column, row));
            }
        }
        Random random = new Random(level.getLevelId() * 3571L + levelNumber * 211L);
        Collections.shuffle(candidates, random);
        int wanted = 3 + random.nextInt(2);
        List<Position> chosen = new ArrayList<>();

        Position first = candidates.get(0);
        chosen.add(first);
        for (Position candidate : candidates) {
            if (candidate.getX() != first.getX()) {
                chosen.add(candidate);
                break;
            }
        }
        for (Position candidate : candidates) {
            if (chosen.size() >= wanted) {
                break;
            }
            if (!chosen.contains(candidate)) {
                chosen.add(candidate);
            }
        }
        for (Position position : chosen) {
            level.markLowTidePosition(position);
        }
    }


    private static void setWaterColumns(Level level, int firstColumn, int lastColumn) {
        for (int row = 1; row <= 5; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                level.setTerrainTile(position(column, row), TileType.WATER);
            }
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
