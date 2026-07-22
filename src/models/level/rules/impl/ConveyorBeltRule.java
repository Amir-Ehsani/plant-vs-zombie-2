package models.level.rules.impl;

import models.level.rules.AbstractLevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.SpecialLevelType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ConveyorBeltRule extends AbstractLevelRule {
    private static final int DEFAULT_INTERVAL_TICKS = 120;

    private final List<String> plantPool;
    private final List<String> conveyorPlants;
    private final Random random;
    private final int intervalTicks;
    private int nextPlantTick;

    public ConveyorBeltRule(List<String> plantPool) {
        this(plantPool, DEFAULT_INTERVAL_TICKS, new Random());
    }

    public ConveyorBeltRule(List<String> plantPool, int intervalTicks, Random random) {
        if (plantPool == null || plantPool.isEmpty()) {
            throw new IllegalArgumentException("Conveyor plant pool cannot be empty.");
        }
        if (intervalTicks <= 0) {
            throw new IllegalArgumentException("Conveyor interval must be positive.");
        }
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null.");
        }

        this.plantPool = copyPlantNames(plantPool);
        this.conveyorPlants = new ArrayList<>();
        this.intervalTicks = intervalTicks;
        this.random = random;
        this.nextPlantTick = 0;
    }

    @Override
    public SpecialLevelType getType() {
        return SpecialLevelType.CONVEYOR_BELT;
    }

    @Override
    public void onLevelStart(LevelRuntimeContext context) {
        resetResult();
        conveyorPlants.clear();
        addRandomPlant();
        nextPlantTick = context.getCurrentTick() + intervalTicks;
    }

    @Override
    public void onTick(LevelRuntimeContext context) {
        while (context.getCurrentTick() >= nextPlantTick) {
            addRandomPlant();
            nextPlantTick += intervalTicks;
        }
    }

    @Override
    public boolean isPlantAllowed(String plantName) {
        return containsIgnoreCase(conveyorPlants, plantName);
    }

    @Override
    public int resolveInitialSunAmount(int defaultAmount) {
        return 0;
    }

    @Override
    public boolean allowsSkySun() {
        return false;
    }

    @Override
    public boolean usesConveyorBelt() {
        return true;
    }

    @Override
    public List<String> getConveyorPlants() {
        return Collections.unmodifiableList(conveyorPlants);
    }

    @Override
    public boolean consumeConveyorPlant(String plantName) {
        if (plantName == null) {
            return false;
        }

        String normalized = normalize(plantName);
        for (int i = 0; i < conveyorPlants.size(); i++) {
            if (normalize(conveyorPlants.get(i)).equals(normalized)) {
                conveyorPlants.remove(i);
                return true;
            }
        }

        return false;
    }

    private void addRandomPlant() {
        conveyorPlants.add(plantPool.get(random.nextInt(plantPool.size())));
    }

    private List<String> copyPlantNames(List<String> source) {
        List<String> copy = new ArrayList<>();
        for (String plantName : source) {
            if (plantName == null || plantName.isBlank()) {
                throw new IllegalArgumentException("Plant pool cannot contain empty names.");
            }
            copy.add(plantName.trim());
        }
        return copy;
    }

    private boolean containsIgnoreCase(List<String> values, String value) {
        if (value == null) {
            return false;
        }
        String normalized = normalize(value);
        for (String item : values) {
            if (normalize(item).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}