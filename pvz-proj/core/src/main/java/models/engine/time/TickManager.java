package models.engine.time;

public class TickManager {
    private int currentTick;
    private int speedMultiplier;
    private boolean running;
    private boolean paused;

    public TickManager() {
        this.currentTick = 0;
        this.speedMultiplier = 1;
        this.running = false;
        this.paused = false;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public int getSpeedMultiplier() {
        return speedMultiplier;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setSpeedMultiplier(int speedMultiplier) {
        if (speedMultiplier <= 0) {
            throw new IllegalArgumentException("Speed multiplier must be greater than 0.");
        }

        this.speedMultiplier = speedMultiplier;
    }

    public void advanceTicks(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Tick count cannot be negative.");
        }

        if (!running || paused) {
            return;
        }

        currentTick += count *  speedMultiplier;
    }


    public void start() {
        running = true;
        paused = false;
    }

    public void pause() {
        if (running) {
            paused = true;
        }
    }

    public void resume() {
        if (running) {
            paused = false;
        }
    }


}
