package models.level;

import java.util.Collections;
import java.util.List;

public interface LevelRule {
    SpecialLevelType getType();

    void onLevelStart(LevelRuntimeContext context);

    void onTick(LevelRuntimeContext context);

    boolean isWinConditionMet(LevelRuntimeContext context);

    boolean isLoseConditionMet(LevelRuntimeContext context);

    default boolean usesCustomWinCondition() {
        return false;
    }

    default boolean isPlantAllowed(String plantName) {
        return true;
    }

    default void onPlantUsed(String plantName) {
    }

    default int resolveInitialSunAmount(int defaultAmount) {
        return defaultAmount;
    }

    default boolean allowsSkySun() {
        return true;
    }

    default boolean startsZombieWavesAutomatically() {
        return true;
    }

    default boolean areZombieWavesStarted() {
        return true;
    }

    default boolean startZombieWaves() {
        return false;
    }

    default boolean ignoresPlantRecharge() {
        return false;
    }

    default boolean usesConveyorBelt() {
        return false;
    }

    default List<String> getConveyorPlants() {
        return Collections.emptyList();
    }

    default boolean consumeConveyorPlant(String plantName) {
        return true;
    }
}
