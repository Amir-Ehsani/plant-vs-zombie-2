package models.core.plant.behaviours;

import models.core.base.GameEntity;
import models.core.plant.AttackBehavior;
import models.core.projectile.Damage;

public class SimpleDamageAttackBehavior implements AttackBehavior {
    private static final String DEFAULT_DAMAGE_TYPE = "normal";

    private final int damageAmount;
    private final String damageType;

    public SimpleDamageAttackBehavior(int damageAmount) {
        this(damageAmount, DEFAULT_DAMAGE_TYPE);
    }

    public SimpleDamageAttackBehavior(int damageAmount, String damageType) {
        this.damageAmount = Math.max(0, damageAmount);
        this.damageType = normalizeDamageType(damageType);
    }

    @Override
    public void attack(GameEntity attacker, GameEntity target) {
        if (!canAttack(attacker, target)) {
            return;
        }

        target.takeDamage(new Damage(damageAmount, damageType));
    }

    public int getDamageAmount() {
        return damageAmount;
    }

    public String getDamageType() {
        return damageType;
    }

    private boolean canAttack(GameEntity attacker, GameEntity target) {
        return attacker != null
                && attacker.isAlive()
                && target != null
                && target.isAlive()
                && damageAmount > 0;
    }

    private String normalizeDamageType(String damageType) {
        if (damageType == null || damageType.isBlank()) {
            return DEFAULT_DAMAGE_TYPE;
        }

        return damageType.trim().toLowerCase();
    }
}
