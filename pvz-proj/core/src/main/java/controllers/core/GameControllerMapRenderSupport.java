package controllers.core;

import controllers.features.TravelLogController;
import models.account.Collection;
import models.account.News;
import models.account.PlantData;
import models.account.Quest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import controllers.auth.AuthController;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.core.zombie.Armor;
import models.core.zombie.DefaultZombieRegistry;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.core.zombie.ZombieRegistry;
import models.core.zombie.ZombieType;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.engine.events.GameEvent;
import models.engine.session.GameSession;
import models.engine.session.GameState;
import models.engine.session.PlantRechargeStatus;
import models.engine.sun.Sun;
import models.level.core.AdventureContentCatalog;
import models.level.core.AdventureLevelCatalog;
import models.level.core.Level;
import models.level.core.LevelType;
import models.level.rules.LevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.NoSpecialRule;
import models.level.rules.SpecialLevelType;
import models.level.rules.TimedWarObjective;
import models.level.rules.impl.ConveyorBeltRule;
import models.level.rules.impl.DeadLineRule;
import models.level.rules.impl.LockedPlantsRule;
import models.level.rules.impl.LoveYourPlantsRule;
import models.level.rules.impl.NightOpsRule;
import models.level.rules.impl.PlantWhatYouGetRule;
import models.level.rules.impl.SaveOurSeedsRule;
import models.level.rules.impl.TimedWarRule;
import models.level.wave.AttackPattern;
import models.level.wave.Wave;
import models.level.wave.WaveManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


abstract class GameControllerMapRenderSupport extends GameControllerQuestSupport {
    protected GameControllerMapRenderSupport(AuthController authController) {
        super(authController);
    }

    protected void appendDetailedMap(StringBuilder builder, Board board) {
        List<Tile> mapTiles = new ArrayList<>();
        int baseContentWidth = 18;

        for (Lane lane : board.getLanes()) {
            for (Tile tile : lane.getTiles()) {
                mapTiles.add(tile);
                baseContentWidth = Math.max(
                        baseContentWidth,
                        detailedTileBaseText(tile).length()
                );
            }
        }

        int cellWidth = baseContentWidth + 4;
        String samplePrefix = rowPrefix(1, false);
        builder.append("\n")
                .append(" ".repeat(samplePrefix.length()));

        for (int column = 1; column <= board.getWidth(); column++) {
            builder.append(centerText(Integer.toString(column), cellWidth));
        }

        int cellIndex = 0;
        for (Lane lane : board.getLanes()) {
            boolean mowerReady = lane.getLawnMower().isReady();
            builder.append("\n").append(rowPrefix(lane.getLaneId(), mowerReady));

            for (int column = 1; column <= board.getWidth(); column++) {
                Tile tile = mapTiles.get(cellIndex++);
                builder.append("[")
                        .append(padRight(detailedTileBaseText(tile), baseContentWidth))
                        .append("|")
                        .append(hasSunAt(tile.getPosition()) ? "*" : "-")
                        .append("]");
            }
        }

        builder.append("\nLegend: [terrain|P:plant names|Z:zombie names|sun], ")
                .append("the final marker is *=sun and -=no sun; ")
                .append("terrain .=normal, G=grave, S=sun-grave, P=plant-food-grave, ")
                .append("W=water, F=ice, B=barrel, A=arcade, ")
                .append("L=low-tide, N=necromancy, ^/v=slip; [LM]=active lawn mower.");
    }

    protected String detailedTileBaseText(Tile tile) {
        return terrainSymbol(tile.getTileType())
                + "|P:" + plantNamesInTile(tile)
                + "|Z:" + zombieNamesAt(tile.getPosition());
    }

    protected String plantNamesInTile(Tile tile) {
        List<String> names = new ArrayList<>();
        for (Plant plant : tile.getPlants()) {
            if (plant != null && plant.isAlive()) {
                names.add(plant.getName());
            }
        }
        return groupedNames(names);
    }

    protected String zombieNamesAt(Position position) {
        List<String> names = new ArrayList<>();
        for (Zombie zombie : zombiesDisplayedAt(position)) {
            names.add(zombie.getName());
        }
        return groupedNames(names);
    }

    protected List<Zombie> zombiesDisplayedAt(Position position) {
        List<Zombie> zombies = new ArrayList<>();
        if (position == null || gameSession == null || gameSession.getBoard() == null) {
            return zombies;
        }

        Board board = gameSession.getBoard();
        for (Lane lane : board.getLanes()) {
            for (Tile tile : lane.getTiles()) {
                for (Zombie zombie : tile.getZombies()) {
                    if (zombie == null || !zombie.isAlive() || zombies.contains(zombie)) {
                        continue;
                    }
                    int displayedX = Math.max(
                            1,
                            Math.min(board.getWidth(), (int) Math.ceil(zombie.getX()))
                    );
                    int displayedY = Math.max(
                            1,
                            Math.min(board.getHeight(), (int) Math.round(zombie.getY()))
                    );
                    if (displayedX == position.getX() && displayedY == position.getY()) {
                        zombies.add(zombie);
                    }
                }
            }
        }
        return zombies;
    }

    protected List<Sun> sunsAt(Position position) {
        List<Sun> suns = new ArrayList<>();
        if (position == null || gameSession == null || gameSession.getSunManager() == null) {
            return suns;
        }
        for (Sun sun : gameSession.getSunManager().getSuns()) {
            if (sun != null && position.equals(sun.getPosition())) {
                suns.add(sun);
            }
        }
        return suns;
    }

    protected boolean hasSunAt(Position position) {
        return gameSession != null
                && gameSession.getSunManager() != null
                && gameSession.getSunManager().hasSunAt(position);
    }

    protected String groupedNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return "-";
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String name : names) {
            String safeName = name == null || name.isBlank() ? "Unknown" : name.trim();
            counts.put(safeName, counts.getOrDefault(safeName, 0) + 1);
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (builder.length() > 0) {
                builder.append("+");
            }
            builder.append(entry.getKey());
            if (entry.getValue() > 1) {
                builder.append(" x").append(entry.getValue());
            }
        }
        return builder.toString();
    }

    protected String rowPrefix(int rowNumber, boolean mowerReady) {
        return String.format(
                Locale.ROOT,
                "row %-2d %-4s ",
                rowNumber,
                mowerReady ? "[LM]" : ""
        );
    }

    protected String centerText(String text, int width) {
        String safeText = text == null ? "" : text;
        if (safeText.length() >= width) {
            return safeText;
        }
        int totalPadding = width - safeText.length();
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;
        return " ".repeat(leftPadding) + safeText + " ".repeat(rightPadding);
    }

    protected String padRight(String text, int width) {
        String safeText = text == null ? "" : text;
        if (safeText.length() >= width) {
            return safeText;
        }
        return safeText + " ".repeat(width - safeText.length());
    }

    protected char terrainSymbol(TileType tileType) {
        switch (tileType) {
            case GRAVE:
                return 'G';
            case SUN_GRAVE:
                return 'S';
            case PLANT_FOOD_GRAVE:
                return 'P';
            case WATER:
                return 'W';
            case ICE:
                return 'F';
            case BARREL:
                return 'B';
            case ARCADE:
                return 'A';
            case LOW_TIDE:
                return 'L';
            case NECROMANCY:
                return 'N';
            case SLIPPERY_UP:
                return '^';
            case SLIPPERY_DOWN:
                return 'v';
            default:
                return '.';
        }
    }

}
