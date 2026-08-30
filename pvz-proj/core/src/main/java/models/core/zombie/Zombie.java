package models.core.zombie;

import models.core.base.GameEntity;
import models.core.projectile.Damage;

import java.util.Locale;

public class Zombie extends GameEntity {
    private static final String NO_DROP = "none";
    private static final String PLANT_FOOD_DROP = "plant_food";
    private static final double MOVEMENT_SPEED_SCALE = 0.5;

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
    private String lastDamageSourcePlantName;
    private String lastDamageSourcePlantCategory;
    private String lastDamageType;
    private int damageRevision;
    private int stolenSun;
    private boolean seasonalIceImmune;
    private double visualVerticalOffset;
    private String visualActionClip;
    private int visualActionRevision;

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
        this.currentSpeed = this.type.getSpeed() * MOVEMENT_SPEED_SCALE;
        this.armor = armor;
        this.movementStrategy = movementStrategy;
        this.zombieAbility = zombieAbility;
        this.glowing = false;
        this.dropChecked = false;
        this.droppedReward = false;
        this.droppedRewardType = NO_DROP;
        this.submerged = false;
        this.lastDamageSourcePlantName = "";
        this.lastDamageSourcePlantCategory = "";
        this.lastDamageType = "";
        this.damageRevision = 0;
        this.stolenSun = 0;
        this.seasonalIceImmune = false;
        this.visualVerticalOffset = 0;
        this.visualActionClip = "";
        this.visualActionRevision = 0;
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
        attack(target, 1.0);
    }

    public void attack(GameEntity target, double multiplier) {
        if (target == null || !target.isAlive() || !isAlive()) {
            return;
        }
        double typeMultiplier = type.hasTag("fast_eater") ? 2.0 : 1.0;
        int damageAmount = (int) Math.ceil(
                type.getDamagePerTick() * Math.max(0, multiplier) * typeMultiplier
        );
        if (damageAmount > 0) {
            target.takeDamage(new Damage(damageAmount, "bite"));
        }
    }

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
        lastDamageType = damageType;
        damageRevision++;
        if (type.hasTag("fire_immune") && isFireDamage(damageType)) {
            return;
        }
        if (type.hasTag("lobber_immune") && damageType.contains("lobber")) {
            return;
        }
        String zombieName = normalizeDamageType(getName());
        if ((zombieName.contains("parasol") || zombieName.contains("umbrella"))
                && damageType.contains("lobber")) {
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
            if (lastDamageType == null || lastDamageType.isBlank()) {
                lastDamageType = damageType;
            }

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
        currentSpeed = type.getSpeed() * MOVEMENT_SPEED_SCALE;
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

    public boolean isIceImmune() {
        return seasonalIceImmune || type.hasTag("ice_immune");
    }

    public void setSeasonalIceImmune(boolean seasonalIceImmune) {
        this.seasonalIceImmune = seasonalIceImmune;
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

    public void recordDamageSource(String plantName, String plantCategory, String damageType) {
        this.lastDamageSourcePlantName = safeText(plantName);
        this.lastDamageSourcePlantCategory = safeText(plantCategory);
        this.lastDamageType = safeText(damageType);
    }

    public String getLastDamageSourcePlantName() {
        return lastDamageSourcePlantName == null ? "" : lastDamageSourcePlantName;
    }

    public String getLastDamageSourcePlantCategory() {
        return lastDamageSourcePlantCategory == null ? "" : lastDamageSourcePlantCategory;
    }

    public String getLastDamageType() {
        return lastDamageType == null ? "" : lastDamageType;
    }

    public int getDamageRevision() {
        return damageRevision;
    }

    public double getVisualVerticalOffset() {
        return visualVerticalOffset;
    }

    public void setVisualVerticalOffset(double visualVerticalOffset) {
        this.visualVerticalOffset = Math.max(0, visualVerticalOffset);
    }

    public void triggerVisualAction(String clip) {
        if (clip == null || clip.isBlank()) {
            return;
        }
        visualActionClip = clip.trim();
        visualActionRevision++;
    }

    public String getVisualActionClip() {
        return visualActionClip;
    }

    public int getVisualActionRevision() {
        return visualActionRevision;
    }

    public int getStolenSun() {
        return stolenSun;
    }

    public void addStolenSun(int amount) {
        if (amount > 0) {
            stolenSun += amount;
        }
    }

    public int takeStolenSun() {
        int result = stolenSun;
        stolenSun = 0;
        return result;
    }

    public void moveTo(double x, double y) {
        if (!isAlive()) {
            return;
        }
        this.x = x;
        this.y = y;
        normalizePosition();
    }

    public void transformTo(ZombieType newType, Armor newArmor) {
        if (newType == null || !isAlive()) {
            return;
        }
        double ratio = maxHp <= 0 ? 1.0 : hp / (double) maxHp;
        type = newType;
        maxHp = newType.getBaseHp();
        hp = Math.max(1, Math.min(maxHp, (int) Math.ceil(maxHp * ratio)));
        currentSpeed = newType.getSpeed();
        armor = newArmor;
        id = buildId();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
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
