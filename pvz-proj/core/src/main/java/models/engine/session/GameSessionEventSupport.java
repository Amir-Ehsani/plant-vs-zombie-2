package models.engine.session;

import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.plant.PlantFood;
import models.core.plant.PlantFoodContext;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.BoardResourceHandler;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.engine.combat.BoardTickResult;
import models.engine.events.GameEvent;
import models.engine.events.GameEventType;
import models.engine.sun.Sun;
import models.engine.sun.SunManager;
import models.engine.sun.SunType;
import models.engine.time.TickManager;
import models.level.core.Level;
import models.level.core.Season;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.impl.LockedPlantsRule;
import models.level.wave.Wave;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;


abstract class GameSessionEventSupport extends GameSessionState {
    protected GameSessionEventSupport() {
        super();
    }

    protected GameSessionEventSupport(Random random) {
        super(random);
    }

    protected void updateSkySunProduction() {
        if (!skySunIsAllowed()) {
            nextSkySunTick = -1;
            return;
        }
        if (nextSkySunTick < 0) {
            scheduleNextSkySun();
            return;
        }
        if (tickManager.getCurrentTick() < nextSkySunTick) {
            return;
        }

        Position position = randomSunPosition();
        SunType type = randomSkySunType();
        spawnSkySun(position, type);
        scheduleNextSkySun();
    }

    protected void scheduleNextSkySun() {
        if (tickManager == null || !skySunIsAllowed()) {
            nextSkySunTick = -1;
            return;
        }

        double elapsedSeconds = tickManager.getCurrentTick() / (double) TICKS_PER_SECOND;
        double intervalSeconds = Math.max(6.0 + 0.05 * elapsedSeconds, 12.0);
        int intervalTicks = Math.max(1, (int) Math.ceil(intervalSeconds * TICKS_PER_SECOND));
        nextSkySunTick = tickManager.getCurrentTick() + intervalTicks;
    }

    protected boolean skySunIsAllowed() {
        return currentLevel == null || currentLevel.allowsSkySun();
    }

    protected Position randomSunPosition() {
        Position fallback = new Position(
            random.nextInt(board.getWidth()) + 1,
            random.nextInt(board.getHeight()) + 1
        );

        for (int attempt = 0; attempt < board.getWidth() * board.getHeight(); attempt++) {
            Position candidate = new Position(
                random.nextInt(board.getWidth()) + 1,
                random.nextInt(board.getHeight()) + 1
            );
            if (!sunManager.hasSunAt(candidate)) {
                return candidate;
            }
        }
        return fallback;
    }

    protected SunType randomSkySunType() {
        int roll = random.nextInt(100);
        if (roll < NORMAL_SKY_SUN_PERCENT) {
            return SunType.NORMAL;
        }
        if (roll < NORMAL_SKY_SUN_PERCENT + SPECIAL_SKY_SUN_PERCENT) {
            return SunType.SPECIAL;
        }
        return SunType.RADIOACTIVE;
    }

    protected void recordWaveEvents(Wave wave) {
        if (wave == null) {
            return;
        }

        boolean finalWave = currentLevel != null
            && wave.getWaveNumber() == currentLevel.getWaveManager().getTotalWaves();
        pendingEvents.add(GameEvent.waveStarted(wave.getWaveNumber(), finalWave));

        for (Zombie zombie : wave.getZombiesList()) {
            if (zombie != null) {
                registerSpawnedZombie(zombie);
                pendingEvents.add(GameEvent.zombieSpawned(
                    zombie,
                    wave.getWaveNumber()
                ));
            }
        }
    }

    protected void registerSpawnedZombie(Zombie zombie) {
        boolean glowing = random.nextInt(100) < GLOWING_ZOMBIE_PERCENT;
        glowingZombies.put(zombie, glowing);
        zombie.setGlowing(glowing);
    }

    protected void recordBoardEvents(BoardTickResult result) {
        if (result == null) {
            return;
        }

        pendingEvents.addAll(result.getEvents());

        for (GameEvent event : result.getEvents()) {
            if (event.getType() == GameEventType.ZOMBIE_KILLED) {
                handleZombieDeath(event.getZombie());
            } else if (event.getType() == GameEventType.PLANT_DESTROYED
                && normalizeName(event.getEntityName()).equals("sun bean")) {
                int releasedSun = 50;
                totalSunAmount += releasedSun;
                totalSunProduced += releasedSun;
            }
        }
    }

    protected void recordTerrainSpawnEvents(List<Zombie> zombies) {
        if (zombies == null || zombies.isEmpty()) {
            return;
        }
        int waveNumber = currentLevel == null
            ? 0
            : currentLevel.getWaveManager().getCurrentWaveNumber();
        for (Zombie zombie : zombies) {
            if (zombie == null) {
                continue;
            }
            registerSpawnedZombie(zombie);
            pendingEvents.add(GameEvent.zombieSpawned(zombie, waveNumber));
        }
    }

    protected void handleZombieDeath(Zombie zombie) {
        if (zombie == null) {
            return;
        }

        awardGlowingZombiePlantFood(zombie);
        restoreStolenSun(zombie);
        rollZombieResourceDrop();
    }

    private void awardGlowingZombiePlantFood(Zombie zombie) {
        if (!Boolean.TRUE.equals(glowingZombies.remove(zombie))) {
            return;
        }
        plantFoodDrops.add(new PlantFoodDrop(zombie.getX(), zombie.getY()));
        pendingEvents.add(GameEvent.plantFoodDropped(plantFoodCount));
    }

    private void restoreStolenSun(Zombie zombie) {
        String zombieName = normalizeName(zombie.getType() == null ? "" : zombie.getType().getName());
        int stolenSun = zombie.takeStolenSun();
        if (stolenSun <= 0) {
            return;
        }
        int restored = zombieName.equals("turquoise") ? stolenSun / 2 : stolenSun;
        if (restored > 0) {
            totalSunAmount += restored;
        }
    }

    private void rollZombieResourceDrop() {
        if (random.nextInt(100) >= 10) {
            return;
        }
        int reward = random.nextInt(3);
        if (reward == 0) {
            pendingEvents.add(GameEvent.rewardDropped("diamond", 1));
        } else if (reward == 1) {
            pendingEvents.add(GameEvent.rewardDropped("coin", 50));
        } else {
            pendingEvents.add(GameEvent.rewardDropped("pot", 1));
        }
    }

    protected void startPlantRecharge(PlantType type) {
        if (type == null || isPlantRechargeIgnored()
            || currentLevel != null && currentLevel.usesConveyorBelt()) {
            return;
        }

        int rechargeTicks = Math.max(0, type.getRecharge());
        if (rechargeTicks == 0) {
            plantRechargeUntilTick.remove(normalizeName(type.getName()));
            return;
        }

        plantRechargeUntilTick.put(
            normalizeName(type.getName()),
            tickManager.getCurrentTick() + rechargeTicks
        );
    }


    public abstract boolean spawnPlantSun(Position position, int amount);

    public abstract boolean spawnSkySun(Position position, SunType type);

}
