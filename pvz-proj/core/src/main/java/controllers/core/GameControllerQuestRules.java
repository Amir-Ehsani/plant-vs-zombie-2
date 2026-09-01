package controllers.core;

import java.util.HashSet;
import controllers.auth.AuthController;
import models.core.plant.Plant;
import models.core.plant.PlantType;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;

import java.util.Set;

abstract class GameControllerQuestRules extends GameControllerState {
    protected GameControllerQuestRules(AuthController authController) {
        super(authController);
    }

    protected boolean hasSingleKillingPlantWithAtLeastTenKills() {
        return killingPlantNamesThisLevel.size() == 1
                && !killsByPlantName.isEmpty()
                && killsByPlantName.values().iterator().next() >= 10;
    }

    protected boolean hasSingleKillingFamily() {
        return killingPlantFamiliesThisLevel.size() == 1
                && !killsByPlantFamily.isEmpty();
    }

    protected boolean hasUnusedPlantFamily() {
        Set<String> allFamilies = new HashSet<>();

        for (PlantType type : plantRegistry.getAllPlantTypes()) {
            if (type != null && type.getCategory() != null && !type.getCategory().isBlank()) {
                allFamilies.add(normalizeName(type.getCategory()));
            }
        }

        allFamilies.removeAll(plantedPlantFamiliesThisLevel);
        return !allFamilies.isEmpty();
    }

    protected boolean usedOnlyMushroomPlants() {
        if (plantedPlantNamesThisLevel.isEmpty()) {
            return false;
        }

        for (String plantName : plantedPlantNamesThisLevel) {
            PlantType type = plantRegistry.getByName(plantName);

            if (type == null || !isMushroomPlant(type)) {
                return false;
            }
        }

        return true;
    }

    protected boolean isFinalLawnSymmetric() {
        if (!hasInitializedSession()) {
            return false;
        }

        Board board = gameSession.getBoard();

        for (int y = 1; y <= board.getHeight(); y++) {
            int mirrorY = board.getHeight() + 1 - y;

            if (!rowsHaveSamePlants(y, mirrorY)) {
                return false;
            }
        }

        return true;
    }

    protected boolean isFinalLawnNonSymmetricExceptMiddleRow() {
        if (!hasInitializedSession()) {
            return false;
        }

        Board board = gameSession.getBoard();
        int middleRow = board.getHeight() % 2 == 1 ? board.getHeight() / 2 + 1 : -1;

        for (int y = 1; y <= board.getHeight(); y++) {
            int mirrorY = board.getHeight() + 1 - y;

            if (y == middleRow || y >= mirrorY) {
                continue;
            }

            if (rowsHaveSamePlants(y, mirrorY)) {
                return false;
            }
        }

        return true;
    }

    protected boolean rowsHaveSamePlants(int firstRow, int secondRow) {
        Board board = gameSession.getBoard();

        for (int x = 1; x <= board.getWidth(); x++) {
            Tile firstTile = board.getTileAt(new Position(x, firstRow));
            Tile secondTile = board.getTileAt(new Position(x, secondRow));

            if (!plantSignature(firstTile).equals(plantSignature(secondTile))) {
                return false;
            }
        }

        return true;
    }

    protected String plantSignature(Tile tile) {
        if (tile == null || tile.getPlants().isEmpty()) {
            return "-";
        }

        StringBuilder builder = new StringBuilder();

        for (Plant plant : tile.getPlants()) {
            if (plant == null) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append("+");
            }

            builder.append(normalizeName(plant.getName()));
        }

        return builder.toString();
    }

    protected boolean hasEmptyColumnByPlantHistory() {
        if (!hasInitializedSession()) {
            return false;
        }

        for (int x = 1; x <= gameSession.getBoard().getWidth(); x++) {
            boolean hasPlantInColumn = false;

            for (Position position : plantedPositionsThisLevel) {
                if (position.getX() == x) {
                    hasPlantInColumn = true;
                    break;
                }
            }

            if (!hasPlantInColumn) {
                return true;
            }
        }

        return false;
    }

    protected boolean hasEmptyRowByPlantHistory() {
        if (!hasInitializedSession()) {
            return false;
        }

        for (int y = 1; y <= gameSession.getBoard().getHeight(); y++) {
            boolean hasPlantInRow = false;

            for (Position position : plantedPositionsThisLevel) {
                if (position.getY() == y) {
                    hasPlantInRow = true;
                    break;
                }
            }

            if (!hasPlantInRow) {
                return true;
            }
        }

        return false;
    }

    protected boolean hasEmptyCrossByPlantHistory() {
        if (!hasInitializedSession()) {
            return false;
        }

        int limit = Math.min(gameSession.getBoard().getWidth(), gameSession.getBoard().getHeight());

        for (int n = 1; n <= limit; n++) {
            boolean rowHasPlant = false;
            boolean columnHasPlant = false;

            for (Position position : plantedPositionsThisLevel) {
                if (position.getY() == n) {
                    rowHasPlant = true;
                }

                if (position.getX() == n) {
                    columnHasPlant = true;
                }
            }

            if (!rowHasPlant && !columnHasPlant) {
                return true;
            }
        }

        return false;
    }

    protected boolean isLawnMowerUsed(int laneNumber) {
        if (!hasInitializedSession()) {
            return false;
        }

        Lane lane = gameSession.getBoard().getLaneAt(laneNumber);

        return lane != null && !lane.getLawnMower().isReady();
    }

    protected boolean isExplosivePlant(PlantType type) {
        if (type == null) {
            return false;
        }

        String text = normalizeName(type.getName()
                + " "
                + type.getCategory()
                + " "
                + type.getTags()
                + " "
                + type.getBaseAbility());

        return text.contains("bomb")
                || text.contains("explosive")
                || text.contains("explode")
                || text.contains("jalapeno")
                || text.contains("doom shroom")
                || text.contains("grapeshot")
                || text.contains("cherry");
    }

    protected boolean isSunProducerPlant(PlantType type) {
        if (type == null) {
            return false;
        }

        String text = normalizeName(type.getName()
                + " "
                + type.getCategory()
                + " "
                + type.getTags()
                + " "
                + type.getBaseAbility());

        return text.contains("sunflower")
                || text.contains("sun shroom")
                || text.contains("sun producer")
                || text.contains("produce sun")
                || text.contains("sun production");
    }

    protected boolean isMushroomPlant(PlantType type) {
        if (type == null) {
            return false;
        }

        String text = normalizeName(type.getName()
                + " "
                + type.getCategory()
                + " "
                + type.getTags());

        return text.contains("shroom") || text.contains("mushroom");
    }

}
