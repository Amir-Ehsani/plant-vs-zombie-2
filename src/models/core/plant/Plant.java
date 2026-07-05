package models.core.plant;

import models.core.base.GameEntity;
import models.core.projectile.Damage;

public class Plant extends GameEntity {
    private int level;
    private int cooldownRemaining;
    private boolean isBoosted;
    private AttackBehavior attackBehavior;
    private final PlantType type;

    public Plant() {
        this(new PlantType(), 0, 0, null);
    }

    public Plant(PlantType type, double x, double y) {
        this(type, x, y, null);
    }

    public Plant(PlantType type, double x, double y, AttackBehavior attackBehavior) {
        this.type = type == null ? new PlantType() : type;
        this.x = x;
        this.y = y;
        this.maxHp = this.type.getBaseHp();
        this.hp = this.maxHp;
        this.level = 1;
        this.cooldownRemaining = 0;
        this.isBoosted = false;
        this.attackBehavior = attackBehavior;
        this.id = buildId();
    }

    private String buildId() {
        return type.getName() + "@" + x + "," + y;
    }

    public void attack() {
        if (!isAlive() || cooldownRemaining > 0) {
            return;
        }

        cooldownRemaining = type.getBaseCooldown();
    }

    public void attack(GameEntity target) {
        if (!canAttack(target)) {
            return;
        }

        if (attackBehavior != null) {
            attackBehavior.attack(this, target);
        }

        cooldownRemaining = type.getBaseCooldown();
    }

    private boolean canAttack(GameEntity target) {
        return isAlive()
                && target != null
                && target.isAlive()
                && cooldownRemaining == 0;
    }

    public void usePlantFood(PlantFood food) {
        if (food == null || !isAlive()) {
            return;
        }

        food.activateBoost(this);
    }

    public void upgrade(PlantUpgrade upgrade) {
        if (upgrade == null || !isAlive()) {
            return;
        }

        upgrade.applyUpgrade(this);
    }

    public void tickCooldown() {
        if (cooldownRemaining > 0) {
            cooldownRemaining--;
        }
    }

    @Override
    public void takeDamage(Damage damage) {
        if (damage == null || damage.getAmount() <= 0 || !isAlive()) {
            return;
        }

        hp -= damage.getAmount();

        if (hp < 0) {
            hp = 0;
        }
    }

    @Override
    public boolean isAlive() {
        return hp > 0;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    public String getId() {
        return id;
    }

    public PlantType getType() {
        return type;
    }

    public String getName() {
        return type.getName();
    }

    public int getLevel() {
        return level;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCooldownRemaining() {
        return cooldownRemaining;
    }

    public boolean isBoosted() {
        return isBoosted;
    }

    public void setBoosted(boolean boosted) {
        isBoosted = boosted;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public void increaseLevel() {
        level++;
    }

    public void heal(int amount) {
        if (amount <= 0 || !isAlive()) {
            return;
        }

        hp += amount;

        if (hp > maxHp) {
            hp = maxHp;
        }
    }

    public void increaseMaxHp(int amount) {
        if (amount <= 0) {
            return;
        }

        maxHp += amount;
        hp += amount;
    }

    public void reduceCooldown(int amount) {
        if (amount <= 0) {
            return;
        }

        cooldownRemaining -= amount;

        if (cooldownRemaining < 0) {
            cooldownRemaining = 0;
        }
    }

    public void resetCooldown() {
        cooldownRemaining = 0;
    }

    public void setAttackBehavior(AttackBehavior attackBehavior) {
        this.attackBehavior = attackBehavior;
    }
}