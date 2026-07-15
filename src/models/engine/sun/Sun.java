package models.engine.sun;

import models.engine.board.Position;

public class Sun {
    private final Position position;
    private final int sunAmount;
    private int timeLeft;

    public Sun(Position position, int sunAmount, int timeLeft) {
        if (position == null) {
            throw new IllegalArgumentException("Sun position cannot be null.");
        }
        if (sunAmount <= 0) {
            throw new IllegalArgumentException("Sun amount must be greater than 0.");
        }
        if (timeLeft < 0) {
            throw new IllegalArgumentException("Time left cannot be negative.");
        }

        this.position = position;
        this.sunAmount = sunAmount;
        this.timeLeft = timeLeft;
    }

    public Position getPosition() {
        return position;
    }

    public int getSunAmount() {
        return sunAmount;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public boolean isExpired() {
        return timeLeft == 0;
    }

    public void tick() {
        if (timeLeft > 0) {
            timeLeft--;
        }
    }
}