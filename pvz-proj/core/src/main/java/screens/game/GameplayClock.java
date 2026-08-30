package screens.game;

import controllers.core.GameController;
import models.account.Settings;
import models.engine.session.GameSession;

import java.util.function.Consumer;

public final class GameplayClock {
    private static final float TICK_SECONDS = 0.1f;
    private static final float MAX_FRAME_DELTA = 0.25f;
    private static final int MAX_TICKS_PER_FRAME = 8;

    private final GameController controller;
    private final GameSession session;
    private float accumulator;
    private int gameSpeed;
    private Consumer<String> tickFeedbackListener;

    public GameplayClock(GameController controller) {
        if (controller == null || controller.getGameSession() == null) {
            throw new IllegalArgumentException("A prepared game controller is required.");
        }
        this.controller = controller;
        session = controller.getGameSession();
        accumulator = 0f;
        gameSpeed = Settings.MIN_GAME_SPEED;
        tickFeedbackListener = null;
    }

    public GameplayClock(GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Game session cannot be null.");
        }
        controller = null;
        this.session = session;
        accumulator = 0f;
        gameSpeed = Settings.MIN_GAME_SPEED;
        tickFeedbackListener = null;
    }

    public void update(float delta) {
        if (isPaused() || !session.isRunning()) {
            return;
        }
        accumulator += Math.min(Math.max(delta, 0f), MAX_FRAME_DELTA);
        float tickInterval = TICK_SECONDS / gameSpeed;
        int processedTicks = 0;
        while (accumulator >= tickInterval
                && session.isRunning()
                && processedTicks < MAX_TICKS_PER_FRAME) {
            if (!advanceGame()) {
                accumulator = 0f;
                break;
            }
            accumulator -= tickInterval;
            processedTicks++;
        }
        if (processedTicks == MAX_TICKS_PER_FRAME && accumulator >= tickInterval) {
            accumulator = Math.min(accumulator, tickInterval);
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

    public void setTickFeedbackListener(Consumer<String> listener) {
        tickFeedbackListener = listener;
    }

    private boolean advanceGame() {
        if (controller == null) {
            return session.advanceTicks(1);
        }
        controller.advanceTime(1);
        if (controller.wasSuccessful() && tickFeedbackListener != null) {
            tickFeedbackListener.accept(controller.getLastMessage());
        }
        return controller.wasSuccessful();
    }
}
