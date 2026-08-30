package models.level.rules.impl;

import models.level.rules.AbstractLevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.SpecialLevelType;
import models.level.rules.TimedWarObjective;

public class TimedWarRule extends AbstractLevelRule {
    private final int durationTicks;
    private final int killTarget;
    private final int sunTarget;
    private final TimedWarObjective legacyObjective;
    private int startTick;
    private int initialKills;
    private int initialSunProduced;

    public TimedWarRule(int durationTicks, int killTarget, int sunTarget) {
        this(durationTicks, killTarget, sunTarget, null);
    }

    public TimedWarRule(TimedWarObjective objective, int durationTicks, int targetAmount) {
        this(
                durationTicks,
                objective == TimedWarObjective.ZOMBIE_KILLS ? targetAmount : 0,
                objective == TimedWarObjective.SUN_PRODUCED ? targetAmount : 0,
                objective
        );
    }

    private TimedWarRule(
            int durationTicks,
            int killTarget,
            int sunTarget,
            TimedWarObjective legacyObjective
    ) {
        if (durationTicks <= 0 || killTarget < 0 || sunTarget < 0
                || (killTarget == 0 && sunTarget == 0)) {
            throw new IllegalArgumentException("Timed war values are invalid.");
        }
        this.durationTicks = durationTicks;
        this.killTarget = killTarget;
        this.sunTarget = sunTarget;
        this.legacyObjective = legacyObjective;
    }

    @Override
    public SpecialLevelType getType() {
        return SpecialLevelType.TIMED_WAR;
    }

    @Override
    public void onLevelStart(LevelRuntimeContext context) {
        resetResult();
        startTick = context.getCurrentTick();
        initialKills = context.getTotalZombiesKilled();
        initialSunProduced = context.getTotalSunProduced();
    }

    @Override
    public void onTick(LevelRuntimeContext context) {
        if (killObjectiveComplete(context) && sunObjectiveComplete(context)) {
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

    public int getKillProgress(LevelRuntimeContext context) {
        return Math.max(0, context.getTotalZombiesKilled() - initialKills);
    }

    public int getSunProgress(LevelRuntimeContext context) {
        return Math.max(0, context.getTotalSunProduced() - initialSunProduced);
    }

    public int getKillTarget() {
        return killTarget;
    }

    public int getSunTarget() {
        return sunTarget;
    }

    public int getRemainingTicks(LevelRuntimeContext context) {
        int elapsed = context.getCurrentTick() - startTick;
        return Math.max(0, durationTicks - elapsed);
    }

    public TimedWarObjective getObjective() {
        return legacyObjective == null ? TimedWarObjective.ZOMBIE_KILLS : legacyObjective;
    }

    public int getProgress(LevelRuntimeContext context) {
        if (legacyObjective == TimedWarObjective.SUN_PRODUCED) {
            return getSunProgress(context);
        }
        if (legacyObjective == TimedWarObjective.ZOMBIE_KILLS) {
            return getKillProgress(context);
        }
        int killPercent = killTarget <= 0 ? 100 : getKillProgress(context) * 100 / killTarget;
        int sunPercent = sunTarget <= 0 ? 100 : getSunProgress(context) * 100 / sunTarget;
        return Math.min(killPercent, sunPercent);
    }

    public int getTargetAmount() {
        if (legacyObjective == TimedWarObjective.SUN_PRODUCED) {
            return sunTarget;
        }
        if (legacyObjective == TimedWarObjective.ZOMBIE_KILLS) {
            return killTarget;
        }
        return 100;
    }

    private boolean killObjectiveComplete(LevelRuntimeContext context) {
        return killTarget <= 0 || getKillProgress(context) >= killTarget;
    }

    private boolean sunObjectiveComplete(LevelRuntimeContext context) {
        return sunTarget <= 0 || getSunProgress(context) >= sunTarget;
    }
}
