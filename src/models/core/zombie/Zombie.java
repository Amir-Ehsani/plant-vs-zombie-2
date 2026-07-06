package models.core.zombie;

import models.core.base.GameEntity;
import models.core.projectile.Damage;

import java.util.Random;

public class Zombie extends GameEntity {
    private static final Random RANDOM = new Random();
    private static final double DROP_CHANCE = 0.10;
    private static final String NO_DROP = "none";

    private double currentSpeed;
    private boolean glowing;
    private boolean dropChecked;
    private boolean droppedReward;
    private String droppedRewardType;
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
        this.x = Math.max(0, x);
        this.y = Math.max(0, y);
        this.maxHp = this.type.getBaseHp();
        this.hp = this.maxHp;
        this.currentSpeed = this.type.getSpeed();
        this.armor = armor;
        this.movementStrategy = movementStrategy;
        this.zombieAbility = zombieAbility;
        this.glowing = false;
        this.dropChecked = false;
        this.droppedReward = false;
        this.droppedRewardType = NO_DROP;
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
            normalizePosition();
            return;
        }

        x -= currentSpeed;
        normalizePosition();
    }

    public void attack(GameEntity target) {
        if (target == null || !target.isAlive() || !isAlive()) {
            return;
        }

        int damageAmount = type.getDamagePerTick();

        if (damageAmount <= 0) {
            return;
        }

        target.takeDamage(new Damage(damageAmount, "bite"));
    }

    public void checkDropOnDeath() {
        if (isAlive() || dropChecked) {
            return;
        }

        dropChecked = true;

        if (RANDOM.nextDouble() > DROP_CHANCE) {
            droppedReward = false;
            droppedRewardType = NO_DROP;
            glowing = false;
            return;
        }

        droppedReward = true;
        droppedRewardType = chooseDroppedRewardType();
        glowing = true;
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

        if (remainingDamage > 0) {
            hp -= remainingDamage;
        }

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
        return glowing;
    }

    public boolean hasDropBeenChecked() {
        return dropChecked;
    }

    public boolean hasDroppedReward() {
        return droppedReward;
    }

    public String getDroppedRewardType() {
        return droppedRewardType;
    }

    public Armor getArmor() {
        return armor;
    }

    public boolean hasArmor() {
        return armor != null && !armor.isBroken();
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

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
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
        normalizePosition();
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

    public void kill() {
        if (!isAlive()) {
            return;
        }

        hp = 0;
        checkDropOnDeath();
    }

    private void normalizePosition() {
        if (x < 0) {
            x = 0;
        }

        if (y < 0) {
            y = 0;
        }
    }

    private String chooseDroppedRewardType() {
        int roll = RANDOM.nextInt(100);

        if (roll < 70) {
            return "coin";
        }

        if (roll < 90) {
            return "diamond";
        }

        return "pot";
    }
}