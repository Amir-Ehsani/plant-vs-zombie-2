package models.level.rules.impl;

import models.level.rules.AbstractLevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.SpecialLevelType;
import models.level.rules.TimedWarObjective;

public class TimedWarRule extends AbstractLevelRule {
    private final TimedWarObjective objective;
    private final int durationTicks;
    private final int targetAmount;
    private int startTick;
    private int initialAmount;

    public TimedWarRule(TimedWarObjective objective, int durationTicks, int targetAmount) {
        if (objective == null) {
            throw new IllegalArgumentException("Timed war objective cannot be null.");
        }
        if (durationTicks <= 0 || targetAmount <= 0) {
            throw new IllegalArgumentException("Timed war values must be positive.");
        }

        this.objective = objective;
        this.durationTicks = durationTicks;
        this.targetAmount = targetAmount;
    }

    @Override
    public SpecialLevelType getType() {
        return SpecialLevelType.TIMED_WAR;
    }

    @Override
    public void onLevelStart(LevelRuntimeContext context) {
        resetResult();
        startTick = context.getCurrentTick();
        initialAmount = getCurrentAmount(context);
    }

    @Override
    public void onTick(LevelRuntimeContext context) {
        if (getProgress(context) >= targetAmount) {
            markWon();
            return;
        }

        if (context.getCurrentTick() - startTick >= durationTicks) {
            markLost();
        }
    }

    @Override
    public boolean usesCustomWinCondition() {
        return true;
    }

    public int getProgress(LevelRuntimeContext context) {
        return Math.max(0, getCurrentAmount(context) - initialAmount);
    }

    public int getRemainingTicks(LevelRuntimeContext context) {
        int elapsed = context.getCurrentTick() - startTick;
        return Math.max(0, durationTicks - elapsed);
    }

    public TimedWarObjective getObjective() {
        return objective;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    private int getCurrentAmount(LevelRuntimeContext context) {
        if (objective == TimedWarObjective.ZOMBIE_KILLS) {
            return context.getTotalZombiesKilled();
        }
        return context.getTotalSunProduced();
    }
}
