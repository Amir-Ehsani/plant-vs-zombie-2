package models.level;

import models.core.zombie.Zombie;

public class DeadLineRule extends AbstractLevelRule {
    private final double deadlineX;

    public DeadLineRule(double deadlineX) {
        if (deadlineX <= 0) {
            throw new IllegalArgumentException("Deadline position must be positive.");
        }
        this.deadlineX = deadlineX;
    }

    @Override
    public SpecialLevelType getType() {
        return SpecialLevelType.DEAD_LINE;
    }

    @Override
    public void onLevelStart(LevelRuntimeContext context) {
        resetResult();
        checkDeadline(context);
    }

    @Override
    public void onTick(LevelRuntimeContext context) {
        checkDeadline(context);
    }

    public double getDeadlineX() {
        return deadlineX;
    }

    private void checkDeadline(LevelRuntimeContext context) {
        for (Zombie zombie : context.getBoard().getAllZombies()) {
            if (zombie.isAlive() && zombie.getX() < deadlineX) {
                markLost();
                return;
            }
        }
    }
}
