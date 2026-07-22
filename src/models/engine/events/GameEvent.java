package models.engine.events;

import models.core.zombie.Zombie;
import models.engine.board.Position;
import models.engine.sun.SunType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameEvent {
    private final GameEventType type;
    private final String entityName;
    private final String sourcePlantName;
    private final String sourcePlantCategory;
    private final String damageType;
    private final double x;
    private final double y;
    private final int laneNumber;
    private final int waveNumber;
    private final int waveCost;
    private final int amount;
    private final int secondaryAmount;
    private final int currentCount;
    private final boolean finalWave;
    private final boolean groupedByLawnMower;
    private final List<String> entityNames;
    private final SunType sunType;
    private final Zombie zombie;

    private GameEvent(
            GameEventType type,
            String entityName,
            String sourcePlantName,
            String sourcePlantCategory,
            String damageType,
            double x,
            double y,
            int laneNumber,
            int waveNumber,
            int waveCost,
            int amount,
            int secondaryAmount,
            int currentCount,
            boolean finalWave,
            boolean groupedByLawnMower,
            List<String> entityNames,
            SunType sunType,
            Zombie zombie
    ) {
        if (type == null) {
            throw new IllegalArgumentException("Event type cannot be null.");
        }

        this.type = type;
        this.entityName = entityName;
        this.sourcePlantName = sourcePlantName;
        this.sourcePlantCategory = sourcePlantCategory;
        this.damageType = damageType;
        this.x = x;
        this.y = y;
        this.laneNumber = laneNumber;
        this.waveNumber = waveNumber;
        this.waveCost = waveCost;
        this.amount = amount;
        this.secondaryAmount = secondaryAmount;
        this.currentCount = currentCount;
        this.finalWave = finalWave;
        this.groupedByLawnMower = groupedByLawnMower;
        this.entityNames = entityNames == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(entityNames));
        this.sunType = sunType;
        this.zombie = zombie;
    }

    public static GameEvent waveStarted(int waveNumber, boolean finalWave) {
        return new GameEvent(GameEventType.WAVE_STARTED, null, null, null, null,
                0, 0, 0, waveNumber, 0, 0, 0, 0,
                finalWave, false, null, null, null);
    }

    public static GameEvent zombieSpawned(Zombie zombie, int waveNumber) {
        if (zombie == null) {
            throw new IllegalArgumentException("Zombie cannot be null.");
        }

        int cost = zombie.getType() == null ? 0 : zombie.getType().getWaveCost();

        return new GameEvent(GameEventType.ZOMBIE_SPAWNED, zombie.getName(), null, null, null,
                zombie.getX(), zombie.getY(), (int) zombie.getY(), waveNumber,
                cost, 0, 0, 0, false, false, null, null, zombie);
    }

    public static GameEvent zombieKilled(Zombie zombie, boolean groupedByLawnMower) {
        if (zombie == null) {
            throw new IllegalArgumentException("Zombie cannot be null.");
        }

        return new GameEvent(GameEventType.ZOMBIE_KILLED, zombie.getName(),
                zombie.getLastDamageSourcePlantName(),
                zombie.getLastDamageSourcePlantCategory(),
                zombie.getLastDamageType(),
                zombie.getX(), zombie.getY(), (int) zombie.getY(), 0, 0,
                0, 0, 0, false, groupedByLawnMower, null, null, zombie);
    }

    public static GameEvent plantDestroyed(String plantName, Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null.");
        }

        return new GameEvent(GameEventType.PLANT_DESTROYED, plantName, null, null, null,
                position.getX(), position.getY(), position.getY(), 0, 0,
                0, 0, 0, false, false, null, null, null);
    }

    public static GameEvent lawnMowerTriggered(int laneNumber, List<String> zombieNames) {
        return new GameEvent(GameEventType.LAWN_MOWER_TRIGGERED, null, null, null, null,
                0, 0, laneNumber, 0, 0, 0, 0, 0, false, false,
                zombieNames, null, null);
    }

    public static GameEvent plantSunProduced(String plantName, Position position, int amount) {
        return new GameEvent(GameEventType.PLANT_SUN_PRODUCED, plantName, plantName, "sun producer", "sun",
                position.getX(), position.getY(), position.getY(), 0, 0,
                amount, 0, 0, false, false, null, SunType.NORMAL, null);
    }

    public static GameEvent skySunDropping(SunType sunType, Position position) {
        return new GameEvent(GameEventType.SKY_SUN_DROPPING, null, null, null, null,
                position.getX(), position.getY(), position.getY(), 0, 0,
                0, 0, 0, false, false, null, sunType, null);
    }

    public static GameEvent skySunLanded(SunType sunType, Position position) {
        return new GameEvent(GameEventType.SKY_SUN_LANDED, null, null, null, null,
                position.getX(), position.getY(), position.getY(), 0, 0,
                0, 0, 0, false, false, null, sunType, null);
    }

    public static GameEvent radioactiveSunExploded(Position position, int zombiesKilled, int plantsDestroyed) {
        return new GameEvent(GameEventType.RADIOACTIVE_SUN_EXPLODED, null, null, null, "radioactive sun",
                position.getX(), position.getY(), position.getY(), 0, 0,
                zombiesKilled, plantsDestroyed, 0, false, false,
                null, SunType.RADIOACTIVE, null);
    }

    public static GameEvent plantFoodDropped(int currentCount) {
        return new GameEvent(GameEventType.PLANT_FOOD_DROPPED, null, null, null, null,
                0, 0, 0, 0, 0, 0, 0, currentCount, false, false,
                null, null, null);
    }

    public static GameEvent rewardDropped(String rewardType) {
        return new GameEvent(GameEventType.REWARD_DROPPED, rewardType, null, null, null,
                0, 0, 0, 0, 0, 0, 0, 0, false, false,
                null, null, null);
    }

    public GameEventType getType() {
        return type;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getSourcePlantName() {
        return sourcePlantName == null ? "" : sourcePlantName;
    }

    public String getSourcePlantCategory() {
        return sourcePlantCategory == null ? "" : sourcePlantCategory;
    }

    public String getDamageType() {
        return damageType == null ? "" : damageType;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getLaneNumber() {
        return laneNumber;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public int getWaveCost() {
        return waveCost;
    }

    public int getAmount() {
        return amount;
    }

    public int getSecondaryAmount() {
        return secondaryAmount;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public boolean isFinalWave() {
        return finalWave;
    }

    public boolean isGroupedByLawnMower() {
        return groupedByLawnMower;
    }

    public List<String> getEntityNames() {
        return entityNames;
    }

    public SunType getSunType() {
        return sunType;
    }

    public Zombie getZombie() {
        return zombie;
    }
}