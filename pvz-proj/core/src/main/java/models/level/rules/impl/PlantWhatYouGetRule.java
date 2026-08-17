package models.level.rules.impl;

import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantType;
import models.level.rules.AbstractLevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.SpecialLevelType;

import java.util.Locale;

public class PlantWhatYouGetRule extends AbstractLevelRule {
    private final int initialSunAmount;
    private boolean zombieWavesStarted;

    public PlantWhatYouGetRule(int initialSunAmount) {
        if (initialSunAmount <= 0) {
            throw new IllegalArgumentException("Initial sun amount must be positive.");
        }
        this.initialSunAmount = initialSunAmount;
        this.zombieWavesStarted = false;
    }

    @Override
    public SpecialLevelType getType() {
        return SpecialLevelType.PLANT_WHAT_YOU_GET;
    }

    @Override
    public void onLevelStart(LevelRuntimeContext context) {
        resetResult();
        zombieWavesStarted = false;
    }

    @Override
    public void onTick(LevelRuntimeContext context) {
    }

    @Override
    public boolean isPlantAllowed(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return false;
        }

        PlantType type = DefaultPlantRegistry.getInstance().getByName(plantName);
        if (type == null) {
            return false;
        }

        String category = type.getCategory().trim().toLowerCase(Locale.ROOT);
        return !category.equals("sun producer");
    }

    @Override
    public int resolveInitialSunAmount(int defaultAmount) {
        return initialSunAmount;
    }

    @Override
    public boolean allowsSkySun() {
        return false;
    }

    @Override
    public boolean startsZombieWavesAutomatically() {
        return false;
    }

    @Override
    public boolean areZombieWavesStarted() {
        return zombieWavesStarted;
    }

    @Override
    public boolean startZombieWaves() {
        if (zombieWavesStarted) {
            return false;
        }
        zombieWavesStarted = true;
        return true;
    }

    @Override
    public boolean ignoresPlantRecharge() {
        return !zombieWavesStarted;
    }
}
