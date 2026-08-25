package models.entities;

import models.core.zombie.Zombie;
import models.core.zombie.ZombieType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LawnMower {
    public static final double START_X = 0.15;
    public static final double END_X = 10.15;
    private static final double TILES_PER_TICK = 0.25;
    private static final double FRONT_HIT_RADIUS = 0.38;
    private static final double BACK_HIT_RADIUS = 0.52;

    private final int assignedRow;
    private boolean triggered;
    private boolean enabled;
    private boolean moving;
    private double positionX;

    public LawnMower(int assignedRow) {
        if (assignedRow <= 0) {
            throw new IllegalArgumentException("Assigned row must be greater than 0.");
        }
        this.assignedRow = assignedRow;
        triggered = false;
        enabled = true;
        moving = false;
        positionX = START_X;
    }

    public int getAssignedRow() {
        return assignedRow;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public boolean isReady() {
        return enabled && !triggered;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMoving() {
        return moving;
    }

    public double getPositionX() {
        return positionX;
    }

    public void disable() {
        enabled = false;
        moving = false;
    }

    public void enable() {
        if (!triggered) {
            enabled = true;
        }
    }

    public boolean trigger() {
        if (!isReady()) {
            return false;
        }
        triggered = true;
        moving = true;
        positionX = START_X;
        return true;
    }

    public List<Zombie> advanceAndDestroy(List<Zombie> zombies) {
        if (!moving) {
            return Collections.emptyList();
        }
        double previousX = positionX;
        positionX = Math.min(END_X, positionX + TILES_PER_TICK);
        List<Zombie> killed = destroyReachedZombies(zombies, previousX, positionX);
        if (positionX >= END_X) {
            moving = false;
            enabled = false;
        }
        return killed;
    }

    public List<Zombie> destroyZombies(List<Zombie> zombies) {
        if (!trigger()) {
            return Collections.emptyList();
        }
        return advanceAndDestroy(zombies);
    }

    private List<Zombie> destroyReachedZombies(
        List<Zombie> zombies,
        double previousX,
        double currentX
    ) {
        if (zombies == null || zombies.isEmpty()) {
            return Collections.emptyList();
        }
        List<Zombie> killed = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (canDestroy(zombie) && mowerReached(zombie, previousX, currentX)) {
                zombie.kill();
                killed.add(zombie);
            }
        }
        return killed;
    }

    private boolean mowerReached(Zombie zombie, double previousX, double currentX) {
        double x = zombie.getX();
        return x >= previousX - BACK_HIT_RADIUS && x <= currentX + FRONT_HIT_RADIUS;
    }

    public boolean canDestroy(Zombie zombie) {
        return zombie != null && zombie.isAlive() && !isBossZombie(zombie);
    }

    private boolean isBossZombie(Zombie zombie) {
        ZombieType type = zombie.getType();
        if (type == null) {
            return false;
        }
        return containsBossKeyword(type.getName()) || containsBossKeyword(type.getId());
    }

    private boolean containsBossKeyword(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains("boss");
    }
}
