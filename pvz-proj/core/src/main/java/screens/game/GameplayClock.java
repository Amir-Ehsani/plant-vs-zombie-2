package screens.game;

import models.engine.session.GameSession;


public final class GameplayClock {
    private static final float TICK_SECONDS = 0.1f;
    private static final float MAX_FRAME_DELTA = 0.25f;

    private final GameSession session;
    private float accumulator;
    private int gameSpeed;

    public GameplayClock(GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Game session cannot be null.");
        }
        this.session = session;
        this.accumulator = 0f;
        this.gameSpeed = 1;
    }

    public void update(float delta) {
        if (isPaused() || !session.isRunning()) {
            return;
        }

        accumulator += Math.min(delta, MAX_FRAME_DELTA);
        while (accumulator >= TICK_SECONDS) {
            session.advanceTicks(gameSpeed);
            accumulator -= TICK_SECONDS;
            if (!session.isRunning()) {
                break;
            }
        }
    }

    public void togglePause() {
        if (isPaused()) {
            session.getTickManager().resume();
        } else {
            session.getTickManager().pause();
        }
    }

    public void setGameSpeed(int gameSpeed) {
        if (gameSpeed <= 0) {
            throw new IllegalArgumentException("Game speed must be positive.");
        }
        this.gameSpeed = gameSpeed;
    }

    public int getGameSpeed() {
        return gameSpeed;
    }

    public boolean isPaused() {
        return session.getTickManager().isPaused();
    }

    public int getCurrentTick() {
        return session.getTickManager().getCurrentTick();
    }
}
