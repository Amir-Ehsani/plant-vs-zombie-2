package models.engine.session;

import models.engine.board.Position;

public final class GroundRewardDrop {
    private final int id;
    private final String type;
    private final int amount;
    private final Position position;
    private int remainingTicks;

    public GroundRewardDrop(int id, String type, int amount, Position position, int remainingTicks) {
        this.id = id;
        this.type = type;
        this.amount = Math.max(1, amount);
        this.position = position;
        this.remainingTicks = Math.max(1, remainingTicks);
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public Position getPosition() {
        return position;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public boolean tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
        return remainingTicks <= 0;
    }
}
