package models.entities;

import models.core.zombie.Zombie;
import models.core.zombie.ZombieType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LawnMower {
    private final int assignedRow;
    private boolean triggered;
    private boolean enabled;

    public LawnMower(int assignedRow) {
        if (assignedRow <= 0) {
            throw new IllegalArgumentException("Assigned row must be greater than 0.");
        }

        this.assignedRow = assignedRow;
        this.triggered = false;
        this.enabled = true;
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

    public void disable() {
        enabled = false;
    }

    public void enable() {
        if (!triggered) {
            enabled = true;
        }
    }

    public List<Zombie> destroyZombies(List<Zombie> zombies) {
        if (!enabled || triggered) {
            return Collections.emptyList();
        }

        triggered = true;

        if (zombies == null || zombies.isEmpty()) {
            return Collections.emptyList();
        }

        List<Zombie> killedZombies = new ArrayList<>();

        for (Zombie zombie : zombies) {
            if (canDestroy(zombie)) {
                zombie.kill();
                killedZombies.add(zombie);
            }
        }

        return killedZombies;
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
        if (value == null) {
            return false;
        }

        return value.toLowerCase(Locale.ROOT).contains("boss");
    }
}