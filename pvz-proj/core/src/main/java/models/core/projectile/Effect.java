package models.core.projectile;

import models.core.base.GameEntity;
import models.core.zombie.Zombie;

import java.util.Locale;

public class Effect {
    private static final String DEFAULT_TYPE = "none";

    private final String type;
    private final int duration;
    private final int damagePerTick;
    private final double speedMultiplier;
    private int remainingDuration;
    private GameEntity affectedEntity;
    private boolean applied;
    private boolean expired;

    public Effect() {
        this(DEFAULT_TYPE, 0, 0);
    }

    public Effect(String type, int duration) {
        this(type, duration, 0);
    }

    public Effect(String type, int duration, int damagePerTick) {
        this.type = normalizeType(type);
        this.duration = Math.max(0, duration);
        this.damagePerTick = Math.max(0, damagePerTick);
        this.speedMultiplier = resolveSpeedMultiplier(this.type);
        this.remainingDuration = 0;
        this.affectedEntity = null;
        this.applied = false;
        this.expired = false;
    }

    public void applyEffect(GameEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return;
        }

        if (entity instanceof Zombie zombie && isBlockedByImmunity(zombie)) {
            affectedEntity = null;
            remainingDuration = 0;
            applied = false;
            expired = true;
            return;
        }

        affectedEntity = entity;
        remainingDuration = duration;
        applied = true;
        expired = false;

        applyMovementEffect();

        if (isInstantDamageEffect()) {
            affectedEntity.takeDamage(new Damage(damagePerTick, type));
            finish();
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

        if (remainingDuration <= 0 || !affectedEntity.isAlive()) {
            finish();
        }
    }

    public boolean isActive() {
        return applied
                && !expired
                && affectedEntity != null
                && affectedEntity.isAlive()
                && remainingDuration > 0;
    }

    public boolean isExpired() {
        return expired || remainingDuration <= 0;
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

    public GameEntity getAffectedEntity() {
        return affectedEntity;
    }

    private void applyMovementEffect() {
        if (!(affectedEntity instanceof Zombie zombie)) {
            return;
        }

        if (speedMultiplier < 0) {
            return;
        }

        zombie.setCurrentSpeed(zombie.getCurrentSpeed() * speedMultiplier);
    }

    private void finish() {
        resetMovementEffect();
        remainingDuration = 0;
        expired = true;
    }

    private void resetMovementEffect() {
        if (!(affectedEntity instanceof Zombie zombie)) {
            return;
        }

        if (speedMultiplier < 0) {
            return;
        }

        zombie.resetSpeed();
    }

    private boolean isInstantDamageEffect() {
        return duration == 0 && damagePerTick > 0;
    }

    private boolean isBlockedByImmunity(Zombie zombie) {
        if ((type.equals("freeze") || type.equals("chill") || type.equals("ice"))
                && zombie.getType().hasTag("ice_immune")) {
            return true;
        }
        return (type.equals("fire") || type.equals("burn"))
                && zombie.getType().hasTag("fire_immune");
    }

    private double resolveSpeedMultiplier(String type) {
        if (type.equals("freeze") || type.equals("stun")) {
            return 0.0;
        }

        if (type.equals("slow") || type.equals("chill")) {
            return 0.5;
        }

        return -1.0;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return DEFAULT_TYPE;
        }

        return type.trim().toLowerCase(Locale.ROOT);
    }
}