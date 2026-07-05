package models.core.zombie;

import models.core.base.GameEntity;
import models.core.projectile.Damage;

public class Zombie extends GameEntity {
    private double currentSpeed;
    private boolean isGlowing;
    private boolean droppedReward;
    private MovementStrategy movementStrategy;
    private ZombieAbility zombieAbility;
    private ZombieType type;
    private Armor armor;

    public Zombie() {
        this(new ZombieType(), 9, 1, null, null, null);
    }

    public Zombie(ZombieType type, double x, double y) {
        this(type, x, y, null, null, null);
    }

    public Zombie(ZombieType type, double x, double y, Armor armor) {
        this(type, x, y, armor, null, null);
    }

    public Zombie(
            ZombieType type,
            double x,
            double y,
            Armor armor,
            MovementStrategy movementStrategy,
            ZombieAbility zombieAbility
    ) {
        this.type = type == null ? new ZombieType() : type;
        this.x = x;
        this.y = y;
        this.maxHp = this.type.getBaseHp();
        this.hp = this.maxHp;
        this.currentSpeed = this.type.getSpeed();
        this.armor = armor;
        this.movementStrategy = movementStrategy;
        this.zombieAbility = zombieAbility;
        this.isGlowing = false;
        this.droppedReward = false;
        this.id = buildId();
    }

    private String buildId() {
        return type.getName() + "@" + x + "," + y;
    }

    public void move() {
        if (!isAlive()) {
            return;
        }

        if (movementStrategy != null) {
            movementStrategy.move(this);
            return;
        }

        x -= currentSpeed;

        if (x < 0) {
            x = 0;
        }
    }

    public void attack(GameEntity target) {
        if (target == null || !target.isAlive() || !isAlive()) {
            return;
        }

        target.takeDamage(new Damage(type.getDamagePerTick(), "bite"));
    }

    public void checkDropOnDeath() {
        if (isAlive() || droppedReward) {
            return;
        }

        droppedReward = true;
        isGlowing = true;
    }

    @Override
    public void takeDamage(Damage damage) {
        if (damage == null || damage.getAmount() <= 0 || !isAlive()) {
            return;
        }

        int remainingDamage = damage.getAmount();

        if (armor != null && !armor.isBroken()) {
            remainingDamage = armor.reduceDamage(remainingDamage);
        }

        hp -= remainingDamage;

        if (hp < 0) {
            hp = 0;
        }

        checkDropOnDeath();
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

    public ZombieType getType() {
        return type;
    }

    public String getName() {
        return type.getName();
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public boolean isGlowing() {
        return isGlowing;
    }

    public boolean hasDroppedReward() {
        return droppedReward;
    }

    public Armor getArmor() {
        return armor;
    }

    public void setMovementStrategy(MovementStrategy movementStrategy) {
        this.movementStrategy = movementStrategy;
    }

    public void setZombieAbility(ZombieAbility zombieAbility) {
        this.zombieAbility = zombieAbility;
    }

    public void setArmor(Armor armor) {
        this.armor = armor;
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = Math.max(0, currentSpeed);
    }

    public void resetSpeed() {
        currentSpeed = type.getSpeed();
    }

    public void executeAbility() {
        if (zombieAbility == null || !isAlive()) {
            return;
        }

        zombieAbility.execute(this);
    }

    public void moveBy(double deltaX, double deltaY) {
        if (!isAlive()) {
            return;
        }

        x += deltaX;
        y += deltaY;

        if (x < 0) {
            x = 0;
        }

        if (y < 0) {
            y = 0;
        }
    }
}