package models.core.zombie;

import models.core.base.GameEntity;
import models.core.projectile.Damage;

import java.util.Locale;

public class Zombie extends GameEntity {
    private static final String NO_DROP = "none";
    private static final String PLANT_FOOD_DROP = "plant_food";

    private double currentSpeed;
    private boolean glowing;
    private boolean dropChecked;
    private boolean droppedReward;
    private String droppedRewardType;
    private MovementStrategy movementStrategy;
    private ZombieAbility zombieAbility;
    private ZombieType type;
    private Armor armor;
    private boolean submerged;

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
        this.submerged = false;
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
        if (damageAmount > 0) {
            target.takeDamage(new Damage(damageAmount, "bite"));
        }
    }

    /**
     * A zombie's drop is determined when it spawns. Glowing zombies always
     * drop Plant Food and ordinary zombies do not create random unrelated loot.
     */
    public void checkDropOnDeath() {
        if (isAlive() || dropChecked) {
            return;
        }
        dropChecked = true;
        droppedReward = glowing;
        droppedRewardType = glowing ? PLANT_FOOD_DROP : NO_DROP;
    }

    @Override
    public void takeDamage(Damage damage) {
        if (damage == null || damage.getAmount() <= 0 || !isAlive()) {
            return;
        }

        String damageType = normalizeDamageType(damage.getType());
        if (type.hasTag("fire_immune") && isFireDamage(damageType)) {
            return;
        }
        if (type.hasTag("lobber_immune") && damageType.contains("lobber")) {
            return;
        }
        if (submerged && type.hasTag("submersible") && !damageType.contains("lobber")) {
            return;
        }

        int remainingDamage = damage.getAmount();
        boolean bypassArmor = damageType.contains("poison")
                || damageType.contains("toxic")
                || damageType.contains("true damage")
                || damageType.contains("armor bypass");

        if (!bypassArmor && armor != null && !armor.isBroken()) {
            remainingDamage = armor.reduceDamage(remainingDamage);
        }
        if (remainingDamage > 0) {
            hp = Math.max(0, hp - remainingDamage);
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
        if (!dropChecked) {
            droppedReward = false;
            droppedRewardType = NO_DROP;
        }
    }

    public boolean isSubmerged() {
        return submerged;
    }

    public void setSubmerged(boolean submerged) {
        this.submerged = submerged;
    }

    public void executeAbility() {
        if (zombieAbility != null && isAlive()) {
            zombieAbility.execute(this);
        }
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
        if (amount > 0 && isAlive()) {
            hp = Math.min(maxHp, hp + amount);
        }
    }

    public void kill() {
        if (!isAlive()) {
            return;
        }
        hp = 0;
        checkDropOnDeath();
    }

    private String normalizeDamageType(String value) {
        return value == null ? "normal" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ').replace('_', ' ');
    }

    private boolean isFireDamage(String value) {
        return value.contains("fire") || value.contains("flame") || value.contains("burn");
    }

    private void normalizePosition() {
        x = Math.max(0, x);
        y = Math.max(0, y);
    }
}
