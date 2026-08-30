package game.input;

import models.account.PlantData;
import models.account.User;
import models.core.plant.Plant;
import models.core.plant.PlantType;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.session.GameSession;
import models.level.core.Level;

public final class PlantPlacementValidator {
    private static final float TICKS_PER_SECOND = 10f;

    private final GameSession session;
    private final User user;

    public PlantPlacementValidator(GameSession session, User user) {
        if (session == null) {
            throw new IllegalArgumentException("Game session cannot be null.");
        }
        this.session = session;
        this.user = user;
    }

    public InteractionValidation validate(String plantName, Position position) {
        InteractionValidation selection = validateSelection(plantName);
        if (!selection.isValid()) {
            return selection;
        }
        if (position == null || session.getBoard() == null || !session.getBoard().isValidPosition(position)) {
            return InteractionValidation.invalid("Choose a valid board tile.");
        }
        PlantType type = session.getPlantType(plantName);
        InteractionValidation resources = validateResources(type);
        if (!resources.isValid()) {
            return resources;
        }

        return validateTile(type, position);
    }

    public InteractionValidation validateSelection(String plantName) {
        if (!session.isRunning()) {
            return InteractionValidation.invalid("The game is not running.");
        }
        if (session.getTickManager() != null && session.getTickManager().isPaused()) {
            return InteractionValidation.invalid("Resume the game before interacting with the board.");
        }
        if (plantName == null || plantName.isBlank()) {
            return InteractionValidation.invalid("Select a plant first.");
        }
        return validateAvailability(session.getPlantType(plantName));
    }

    private InteractionValidation validateAvailability(PlantType type) {
        if (type == null) {
            return InteractionValidation.invalid("Plant does not exist.");
        }
        if (!session.isPlantSelected(type.getName())) {
            return InteractionValidation.invalid("Plant is not selected for this level.");
        }
        Level level = session.getCurrentLevel();
        boolean conveyorPlant = level != null && level.usesConveyorBelt();
        if (!conveyorPlant && !isUnlocked(type.getName())) {
            return InteractionValidation.invalid("Plant is locked in your collection.");
        }
        if (level != null && !level.isPlantAllowed(type.getName())) {
            return InteractionValidation.invalid("Plant is locked or unavailable in this level.");
        }
        return InteractionValidation.valid();
    }

    private InteractionValidation validateResources(PlantType type) {
        int remainingTicks = session.getPlantRechargeRemainingTicks(type.getName());
        if (remainingTicks > 0) {
            float seconds = remainingTicks / TICKS_PER_SECOND;
            return InteractionValidation.invalid("Plant cooldown: " + formatSeconds(seconds) + "s remaining.");
        }
        int cost = session.getPlantCost(type.getName());
        if (cost > session.getTotalSunAmount()) {
            return InteractionValidation.invalid("Not enough sun. Required: " + cost + ".");
        }
        return InteractionValidation.valid();
    }

    private InteractionValidation validateTile(PlantType type, Position position) {
        Board board = session.getBoard();
        Tile tile = board.getTileAt(position);
        if (tile == null) {
            return InteractionValidation.invalid("Choose a valid board tile.");
        }
        Plant preview = new Plant(type, position.getX(), position.getY());
        if (board.canPlacePlant(preview, position)) {
            return InteractionValidation.valid();
        }
        if (tile.hasPlant()) {
            return InteractionValidation.invalid("Tile is occupied or these plants cannot be stacked.");
        }
        return InteractionValidation.invalid("Plant cannot be placed on " + tile.getTileType() + " terrain.");
    }

    private boolean isUnlocked(String plantName) {
        if (user == null || user.getCollection() == null) {
            return false;
        }
        PlantData data = user.getCollection().findOwnedPlant(plantName);
        return data != null && data.isUnlocked();
    }

    private String formatSeconds(float seconds) {
        if (seconds >= 10f) {
            return String.valueOf(Math.round(seconds));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", seconds);
    }
}
