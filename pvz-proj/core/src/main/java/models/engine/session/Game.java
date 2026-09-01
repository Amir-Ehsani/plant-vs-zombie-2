package models.engine.session;

public class Game {
    private GameSession currentSession;

    public Game() {
    }

    public Game(GameSession currentSession) {
        setCurrentSession(currentSession);
    }

    public void setCurrentSession(GameSession currentSession) {
        if (currentSession == null) {
            throw new IllegalArgumentException("Game session cannot be null.");
        }

        if (hasRunningSession()) {
            throw new IllegalStateException(
                    "Cannot replace a running game session."
            );
        }

        this.currentSession = currentSession;
    }

    public boolean startGame() {
        if (currentSession == null || currentSession.isRunning()) {
            return false;
        }

        GameState state = currentSession.getState();

        if (state != null && state.isFinished()) {
            return false;
        }

        currentSession.initSession();
        return true;
    }

    public GameSession saveProgress() {
        if (currentSession == null || currentSession.getState() == null) {
            return null;
        }

        return currentSession;
    }

    public boolean clearCurrentSession() {
        if (hasRunningSession()) {
            return false;
        }

        currentSession = null;
        return true;
    }

    public boolean hasCurrentSession() {
        return currentSession != null;
    }

    public boolean hasRunningSession() {
        return currentSession != null && currentSession.isRunning();
    }

    public GameSession getCurrentSession() {
        return currentSession;
    }
}
