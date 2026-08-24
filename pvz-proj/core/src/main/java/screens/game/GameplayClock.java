package screens.game;

import controllers.core.GameController;
import models.account.Settings;
import models.engine.session.GameSession;

public final class GameplayClock {
    private static final float TICK_SECONDS = 0.1f;
    private static final float MAX_FRAME_DELTA = 0.25f;

    private final GameController controller;
    private final GameSession session;
    private float accumulator;
    private int gameSpeed;

    public GameplayClock(GameController controller) {
        if (controller == null || controller.getGameSession() == null) {
            throw new IllegalArgumentException("A prepared game controller is required.");
        }
        this.controller = controller;
        session = controller.getGameSession();
        accumulator = 0f;
        gameSpeed = Settings.MIN_GAME_SPEED;
    }

    public GameplayClock(GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Game session cannot be null.");
        }
        controller = null;
        this.session = session;
        accumulator = 0f;
        gameSpeed = Settings.MIN_GAME_SPEED;
    }

    public void update(float delta) {
        if (isPaused() || !session.isRunning()) {
            return;
        }
        accumulator += Math.min(Math.max(delta, 0f), MAX_FRAME_DELTA);
        while (accumulator >= TICK_SECONDS && session.isRunning()) {
            if (!advanceGame()) {
                accumulator = 0f;
                break;
            }
            accumulator -= TICK_SECONDS;
        }
    }

    public void togglePause() {
        if (!session.isRunning()) {
            return;
        }
        controller.handlePause();
    }

    public void setGameSpeed(int gameSpeed) {
        if (gameSpeed < Settings.MIN_GAME_SPEED || gameSpeed > Settings.MAX_GAME_SPEED) {
            throw new IllegalArgumentException("Game speed must be between 1 and 3.");
        }
        this.gameSpeed = gameSpeed;
    }

    public int getGameSpeed() {
        return gameSpeed;
    }

    public boolean isPaused() {
        return session.getTickManager() != null && session.getTickManager().isPaused();
    }

    public int getCurrentTick() {
        return session.getTickManager() == null ? 0 : session.getTickManager().getCurrentTick();
    }

    private boolean advanceGame() {
        if (controller == null) {
            return session.advanceTicks(gameSpeed);
        }
        controller.advanceTime(gameSpeed);
        return controller.wasSuccessful();
    }
}
