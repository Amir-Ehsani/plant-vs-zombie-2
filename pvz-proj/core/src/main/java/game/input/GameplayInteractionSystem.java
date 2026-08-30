package game.input;

import audio.AudioCue;
import audio.AudioManager;
import controllers.core.GameController;
import models.account.User;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.session.GameSession;

public final class GameplayInteractionSystem {
    private final GameController controller;
    private final GameSession session;
    private final PlantPlacementValidator placementValidator;

    private GameplayInputMode mode;
    private String selectedPlantName;
    private String lastMessage;
    private boolean lastSuccessful;

    public GameplayInteractionSystem(GameController controller, User user) {
        if (controller == null || controller.getGameSession() == null) {
            throw new IllegalArgumentException("A prepared game controller is required.");
        }
        this.controller = controller;
        session = controller.getGameSession();
        placementValidator = new PlantPlacementValidator(session, user);
        mode = GameplayInputMode.NORMAL;
        selectedPlantName = null;
        lastMessage = "Normal mode";
        lastSuccessful = true;
    }

    public boolean selectPlant(String plantName) {
        if (isSamePlantSelection(plantName)) {
            cancel();
            return true;
        }
        InteractionValidation validation = placementValidator.validateSelection(plantName);
        if (!validation.isValid()) {
            return fail(validation.getMessage());
        }
        selectedPlantName = plantName;
        mode = GameplayInputMode.PLANTING;
        return succeed("Planting " + plantName + ". Choose a tile.");
    }

    private boolean isSamePlantSelection(String plantName) {
        return mode == GameplayInputMode.PLANTING
                && selectedPlantName != null
                && plantName != null
                && selectedPlantName.equalsIgnoreCase(plantName);
    }

    public boolean selectShovel() {
        if (!canInteract()) {
            return false;
        }
        selectedPlantName = null;
        mode = GameplayInputMode.SHOVEL;
        return succeed("Shovel selected. Choose a planted tile.");
    }

    public boolean selectPlantFood() {
        if (!canInteract()) {
            return false;
        }
        if (session.getPlantFoodCount() <= 0) {
            return fail("No plant food is available.");
        }
        selectedPlantName = null;
        mode = GameplayInputMode.PLANT_FOOD;
        return succeed("Plant food selected. Choose a plant.");
    }

    public boolean handleTileClick(Position position) {
        if (mode == GameplayInputMode.NORMAL) {
            return false;
        }
        if (!canInteract()) {
            return true;
        }
        if (mode == GameplayInputMode.PLANTING) {
            plantAt(position);
        } else if (mode == GameplayInputMode.SHOVEL) {
            shovelAt(position);
        } else if (mode == GameplayInputMode.PLANT_FOOD) {
            feedAt(position);
        }
        return true;
    }

    public InteractionValidation validateTarget(Position position) {
        if (mode == GameplayInputMode.PLANTING) {
            return placementValidator.validate(selectedPlantName, position);
        }
        if (!canInteractSilently()) {
            return InteractionValidation.invalid("Board interaction is unavailable.");
        }
        Tile tile = position == null || session.getBoard() == null
                ? null : session.getBoard().getTileAt(position);
        if (tile == null || !tile.hasPlant()) {
            return InteractionValidation.invalid("Choose a planted tile.");
        }
        if (mode == GameplayInputMode.PLANT_FOOD && session.getPlantFoodCount() <= 0) {
            return InteractionValidation.invalid("No plant food is available.");
        }
        return InteractionValidation.valid();
    }

    public void cancel() {
        mode = GameplayInputMode.NORMAL;
        selectedPlantName = null;
        succeed("Normal mode");
    }

    public GameplayInputMode getMode() {
        return mode;
    }

    public String getSelectedPlantName() {
        return selectedPlantName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastSuccessful;
    }

    public boolean isActive() {
        return mode != GameplayInputMode.NORMAL;
    }

    private void plantAt(Position position) {
        InteractionValidation validation = placementValidator.validate(selectedPlantName, position);
        if (!validation.isValid()) {
            fail(validation.getMessage());
            return;
        }
        controller.plant(selectedPlantName, position);
        copyControllerResult();
        if (lastSuccessful) {
            AudioManager.playGlobal(AudioCue.PLANT);
            AudioManager manager = AudioManager.getActive();
            if (manager != null) {
                manager.playExplosionForPlant(selectedPlantName);
            }
        }
        cancelAfterSuccess();
    }

    private void shovelAt(Position position) {
        InteractionValidation validation = validateTarget(position);
        if (!validation.isValid()) {
            fail(validation.getMessage());
            return;
        }
        controller.pluck(position);
        copyControllerResult();
        cancelAfterSuccess();
    }

    private void feedAt(Position position) {
        InteractionValidation validation = validateTarget(position);
        if (!validation.isValid()) {
            fail(validation.getMessage());
            return;
        }
        controller.feedPlant(position);
        copyControllerResult();
        cancelAfterSuccess();
    }

    private boolean canInteract() {
        if (!session.isRunning()) {
            return fail("The game is not running.");
        }
        if (session.getTickManager() != null && session.getTickManager().isPaused()) {
            return fail("Resume the game before interacting with the board.");
        }
        return true;
    }

    private boolean canInteractSilently() {
        return session.isRunning()
                && (session.getTickManager() == null || !session.getTickManager().isPaused());
    }

    private void copyControllerResult() {
        lastSuccessful = controller.wasSuccessful();
        lastMessage = stripMessagePrefix(controller.getLastMessage());
    }

    private void cancelAfterSuccess() {
        if (lastSuccessful) {
            mode = GameplayInputMode.NORMAL;
            selectedPlantName = null;
        }
    }

    private boolean succeed(String message) {
        lastSuccessful = true;
        lastMessage = message;
        return true;
    }

    private boolean fail(String message) {
        lastSuccessful = false;
        lastMessage = message;
        return false;
    }

    private String stripMessagePrefix(String message) {
        if (message == null || message.isBlank()) {
            return "Interaction completed.";
        }
        int separator = message.indexOf(':');
        if (separator < 0 || separator + 1 >= message.length()) {
            return message;
        }
        return message.substring(separator + 1).trim();
    }
}
