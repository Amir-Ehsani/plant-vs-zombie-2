package models.engine.session;

public class GameState {
    private Status status;

    public GameState() {
        this.status = Status.NOT_STARTED;
    }

    public void setStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Game status cannot be null.");
        }

        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    public boolean isFinished() {
        return status == Status.WON || status == Status.LOST;
    }

    public enum Status {
        NOT_STARTED,
        RUNNING,
        WON,
        LOST
    }
}
