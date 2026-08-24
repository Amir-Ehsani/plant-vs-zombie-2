package screens.game;

import com.pvz.Main;
import controllers.core.GameController;
import models.account.PlantData;
import models.account.User;
import models.engine.session.GameSession;
import models.level.core.AdventureLevelCatalog;

public final class AdventureSessionBootstrap {
    private static final String DEFAULT_CHAPTER = "ancient-egypt";
    private static final int MAX_PLAYABLE_LEVEL = 3;

    private AdventureSessionBootstrap() {
    }

    public static GameSession prepareAndStart(Main game) {
        if (game == null || game.getAuthController() == null || !game.getAuthController().isLoggedIn()) {
            throw new IllegalStateException("A logged-in user is required to start Adventure.");
        }

        User user = game.getAuthController().getLoggedInUser();
        GameController controller = game.getGameController();
        String chapter = resolveChapter(user);
        int levelNumber = resolvePlayableLevel(user, chapter);

        controller.prepareChapterLevel(chapter, levelNumber);
        requireControllerSuccess(controller);
        autoSelectPlants(user, controller);
        controller.startGame();
        requireControllerSuccess(controller);

        GameSession session = controller.getGameSession();
        if (session == null || !session.isRunning() || session.getBoard() == null) {
            throw new IllegalStateException("Adventure session did not start correctly.");
        }
        return session;
    }

    private static String resolveChapter(User user) {
        String requested = AdventureLevelCatalog.normalizeChapterName(user.getCurrentChapterName());
        if (AdventureLevelCatalog.chapterExists(requested) && user.isChapterUnlocked(requested)) {
            return requested;
        }
        for (String unlocked : user.getUnlockedChapters()) {
            String normalized = AdventureLevelCatalog.normalizeChapterName(unlocked);
            if (AdventureLevelCatalog.chapterExists(normalized)) {
                return normalized;
            }
        }
        return DEFAULT_CHAPTER;
    }

    private static int resolvePlayableLevel(User user, String chapter) {
        int desired = Math.min(MAX_PLAYABLE_LEVEL, Math.max(1, user.getCurrentChapterLevel()));
        for (int level = desired; level >= 1; level--) {
            if (user.isChapterLevelUnlocked(chapter, level)) {
                return level;
            }
        }
        return 1;
    }

    private static void autoSelectPlants(User user, GameController controller) {
        GameSession session = controller.getGameSession();
        if (session == null || session.getCurrentLevel() == null || session.getCurrentLevel().usesConveyorBelt()) {
            return;
        }

        for (PlantData plantData : user.getCollection().getOwnedPlants()) {
            if (plantData == null || session.getSelectedPlantNames().size() >= 8) {
                break;
            }
            controller.addPlantToSelection(plantData.getName());
        }

        if (session.getSelectedPlantNames().isEmpty()) {
            throw new IllegalStateException("No unlocked plant is available for the selected Adventure level.");
        }
    }

    private static void requireControllerSuccess(GameController controller) {
        if (controller == null || !controller.wasSuccessful()) {
            String message = controller == null ? "Game controller is unavailable." : controller.getLastMessage();
            throw new IllegalStateException(message == null || message.isBlank() ? "Adventure setup failed." : message);
        }
    }
}
