package models.level.rules.impl;

import models.level.rules.AbstractLevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.SpecialLevelType;

public class LoveYourPlantsRule extends AbstractLevelRule {
    private final int maximumPlantLosses;

    public LoveYourPlantsRule(int maximumPlantLosses) {
        if (maximumPlantLosses <= 0) {
            throw new IllegalArgumentException("Maximum plant losses must be positive.");
        }
        this.maximumPlantLosses = maximumPlantLosses;
    }

    @Override
    public SpecialLevelType getType() {
        return SpecialLevelType.LOVE_YOUR_PLANTS;
    }

    @Override
    public void onLevelStart(LevelRuntimeContext context) {
        resetResult();
    }

    @Override
    public void onTick(LevelRuntimeContext context) {
        if (getDestroyedPlantCount(context) >= maximumPlantLosses) {
            markLost();
        }
    }

    public int getDestroyedPlantCount(LevelRuntimeContext context) {
        return context.getTotalPlantsDestroyed();
    }

    public int getMaximumPlantLosses() {
        return maximumPlantLosses;
    }
}
