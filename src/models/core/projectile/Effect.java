package models.core.projectile;

import models.core.base.GameEntity;

public class Effect {
    private static final String DEFAULT_TYPE = "none";

    private final String type;
    private final int duration;
    private final int damagePerTick;
    private int remainingDuration;
    private GameEntity affectedEntity;

    public Effect() {
        this(DEFAULT_TYPE, 0, 0);
    }

    public Effect(String type, int duration) {
        this(type, duration, 0);
    }

    public Effect(String type, int duration, int damagePerTick) {
        this.type = normalizeType(type);
        this.duration = Math.max(0, duration);
        this.remainingDuration = this.duration;
        this.damagePerTick = Math.max(0, damagePerTick);
    }

    public void applyEffect(GameEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return;
        }

        affectedEntity = entity;
        remainingDuration = duration;

        if (isInstantDamageEffect()) {
            entity.takeDamage(new Damage(damagePerTick, type));
            remainingDuration = 0;
        }
    }

    public void tick() {
        if (!isActive()) {
            return;
        }

        if (damagePerTick > 0 && affectedEntity.isAlive()) {
            affectedEntity.takeDamage(new Damage(damagePerTick, type));
        }

        remainingDuration--;
    }

    public boolean isActive() {
        return affectedEntity != null
                && affectedEntity.isAlive()
                && remainingDuration > 0;
    }

    public boolean isExpired() {
        return remainingDuration <= 0;
    }

    public String getType() {
        return type;
    }

    public int getDuration() {
        return duration;
    }

    public int getRemainingDuration() {
        return remainingDuration;
    }

    public int getDamagePerTick() {
        return damagePerTick;
    }

    private boolean isInstantDamageEffect() {
        return duration == 0 && damagePerTick > 0;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return DEFAULT_TYPE;
        }

        return type.trim().toLowerCase();
    }
}