package models.core.plant;

import models.core.base.GameEntity;
import models.core.projectile.Damage;

public class Plant extends GameEntity {
    private int level;
    private int cooldownRemaining;
    private boolean boosted;
    private AttackBehavior attackBehavior;
    private final PlantType type;
    private int currentSunCost;
    private int attackDamage;
    private int damagePerTick;
    private int areaDamage;
    private int explodeDamage;
    private int reflectDamage;
    private int sunProductionBonus;
    private int sunDropBonus;
    private int attackIntervalTicks;
    private int range;
    private int targetCount;
    private int pierceCount;
    private int bounces;
    private int freezeDurationTicks;
    private int chillDurationTicks;
    private int durationTicks;
    private int lifespanTicks;
    private int growTimeTicks;
    private int productionTimeTicks;
    private int armTimeTicks;
    private int chargeTimeTicks;
    private int digestTimeTicks;
    private int eatTimeTicks;
    private int regenTimeTicks;
    private int warmthRadius;
    private int maxSize;
    private int plantFoodChancePercent;
    private int butterChancePercent;
    private boolean doubleSunChance;
    private boolean targetPriorityUp;
    private boolean canCrushTwice;
    private boolean aoeOnDeath;
    private boolean zombieHpBuff;
    private boolean zombieDamageBuff;
    private boolean plantFoodOnEntrance;
    private boolean explodeOnFinish;
    private boolean resetFamilyCooldowns;
    private boolean meltAreaThreeByThree;

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
        this.boosted = false;
        this.attackBehavior = attackBehavior;
        this.currentSunCost = this.type.getSunCost();
        this.attackIntervalTicks = this.type.getBaseCooldown();
        this.attackDamage = resolveInitialDamage(this.type.getCategory());
        this.damagePerTick = 0;
        this.areaDamage = 0;
        this.explodeDamage = 0;
        this.reflectDamage = 0;
        this.sunProductionBonus = 0;
        this.sunDropBonus = 0;
        this.range = 1;
        this.targetCount = 1;
        this.pierceCount = 0;
        this.bounces = 0;
        this.freezeDurationTicks = 0;
        this.chillDurationTicks = 0;
        this.durationTicks = 0;
        this.lifespanTicks = 0;
        this.growTimeTicks = 0;
        this.productionTimeTicks = this.type.getBaseCooldown();
        this.armTimeTicks = 0;
        this.chargeTimeTicks = this.type.getBaseCooldown();
        this.digestTimeTicks = this.type.getBaseCooldown();
        this.eatTimeTicks = 0;
        this.regenTimeTicks = 0;
        this.warmthRadius = 0;
        this.maxSize = 1;
        this.plantFoodChancePercent = 0;
        this.butterChancePercent = 0;
        this.doubleSunChance = false;
        this.targetPriorityUp = false;
        this.canCrushTwice = false;
        this.aoeOnDeath = false;
        this.zombieHpBuff = false;
        this.zombieDamageBuff = false;
        this.plantFoodOnEntrance = false;
        this.explodeOnFinish = false;
        this.resetFamilyCooldowns = false;
        this.meltAreaThreeByThree = false;
        this.id = buildId();
    }

    private String buildId() {
        return type.getName() + "@" + x + "," + y;
    }

    public void attack() {
        if (!isAlive() || cooldownRemaining > 0) {
            return;
        }

        cooldownRemaining = attackIntervalTicks;
    }

    public void attack(GameEntity target) {
        if (!canAttack(target)) {
            return;
        }

        if (attackBehavior != null) {
            attackBehavior.attack(this, target);
        } else if (attackDamage > 0) {
            target.takeDamage(new Damage(getEffectiveAttackDamage(), resolveDamageType()));
        }

        cooldownRemaining = attackIntervalTicks;
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
        return boosted;
    }

    public int getCurrentSunCost() {
        return currentSunCost;
    }

    public int getAttackDamage() {
        return attackDamage;
    }

    public int getDamagePerTick() {
        return damagePerTick;
    }

    public int getAreaDamage() {
        return areaDamage;
    }

    public int getExplodeDamage() {
        return explodeDamage;
    }

    public int getReflectDamage() {
        return reflectDamage;
    }

    public int getSunProductionBonus() {
        return sunProductionBonus;
    }

    public int getSunDropBonus() {
        return sunDropBonus;
    }

    public int getAttackIntervalTicks() {
        return attackIntervalTicks;
    }

    public int getRange() {
        return range;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public int getPierceCount() {
        return pierceCount;
    }

    public int getBounces() {
        return bounces;
    }

    public int getFreezeDurationTicks() {
        return freezeDurationTicks;
    }

    public int getChillDurationTicks() {
        return chillDurationTicks;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getLifespanTicks() {
        return lifespanTicks;
    }

    public int getGrowTimeTicks() {
        return growTimeTicks;
    }

    public int getProductionTimeTicks() {
        return productionTimeTicks;
    }

    public int getArmTimeTicks() {
        return armTimeTicks;
    }

    public int getChargeTimeTicks() {
        return chargeTimeTicks;
    }

    public int getDigestTimeTicks() {
        return digestTimeTicks;
    }

    public int getEatTimeTicks() {
        return eatTimeTicks;
    }

    public int getRegenTimeTicks() {
        return regenTimeTicks;
    }

    public int getWarmthRadius() {
        return warmthRadius;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getPlantFoodChancePercent() {
        return plantFoodChancePercent;
    }

    public int getButterChancePercent() {
        return butterChancePercent;
    }

    public boolean hasDoubleSunChance() {
        return doubleSunChance;
    }

    public boolean hasTargetPriorityUp() {
        return targetPriorityUp;
    }

    public boolean canCrushTwice() {
        return canCrushTwice;
    }

    public boolean hasAoeOnDeath() {
        return aoeOnDeath;
    }

    public boolean hasZombieHpBuff() {
        return zombieHpBuff;
    }

    public boolean hasZombieDamageBuff() {
        return zombieDamageBuff;
    }

    public boolean hasPlantFoodOnEntrance() {
        return plantFoodOnEntrance;
    }

    public boolean hasExplodeOnFinish() {
        return explodeOnFinish;
    }

    public boolean shouldResetFamilyCooldowns() {
        return resetFamilyCooldowns;
    }

    public boolean hasMeltAreaThreeByThree() {
        return meltAreaThreeByThree;
    }

    public void setBoosted(boolean boosted) {
        this.boosted = boosted;
    }

    public void setLevel(int level) {
        if (level < 1) {
            this.level = 1;
        } else if (level > 4) {
            this.level = 4;
        } else {
            this.level = level;
        }
    }

    public void increaseLevel() {
        setLevel(level + 1);
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

    public void reduceCooldown(int ticks) {
        if (ticks <= 0) {
            return;
        }

        cooldownRemaining -= ticks;

        if (cooldownRemaining < 0) {
            cooldownRemaining = 0;
        }
    }

    public void resetCooldown() {
        cooldownRemaining = 0;
    }

    public void decreaseSunCost(int amount) {
        if (amount <= 0) {
            return;
        }

        currentSunCost -= amount;

        if (currentSunCost < 0) {
            currentSunCost = 0;
        }
    }

    public void increaseAttackDamage(int amount) {
        if (amount > 0) {
            attackDamage += amount;
        }
    }

    public void increaseDamagePerTick(int amount) {
        if (amount > 0) {
            damagePerTick += amount;
        }
    }

    public void increaseAreaDamage(int amount) {
        if (amount > 0) {
            areaDamage += amount;
        }
    }

    public void increaseExplodeDamage(int amount) {
        if (amount > 0) {
            explodeDamage += amount;
        }
    }

    public void increaseReflectDamage(int amount) {
        if (amount > 0) {
            reflectDamage += amount;
        }
    }

    public void increaseSunProductionBonus(int amount) {
        if (amount > 0) {
            sunProductionBonus += amount;
        }
    }

    public void increaseSunDropBonus(int amount) {
        if (amount > 0) {
            sunDropBonus += amount;
        }
    }

    public void increaseRange(int amount) {
        if (amount > 0) {
            range += amount;
        }
    }

    public void increaseTargetCount(int amount) {
        if (amount > 0) {
            targetCount += amount;
        }
    }

    public void increasePierceCount(int amount) {
        if (amount > 0) {
            pierceCount += amount;
        }
    }

    public void increaseBounces(int amount) {
        if (amount > 0) {
            bounces += amount;
        }
    }

    public void increaseFreezeDuration(int ticks) {
        if (ticks > 0) {
            freezeDurationTicks += ticks;
        }
    }

    public void increaseChillDuration(int ticks) {
        if (ticks > 0) {
            chillDurationTicks += ticks;
        }
    }

    public void increaseDuration(int ticks) {
        if (ticks > 0) {
            durationTicks += ticks;
        }
    }

    public void increaseLifespan(int ticks) {
        if (ticks > 0) {
            lifespanTicks += ticks;
        }
    }

    public void decreaseGrowTime(int ticks) {
        growTimeTicks = decreaseNonNegative(growTimeTicks, ticks);
    }

    public void decreaseProductionTime(int ticks) {
        productionTimeTicks = decreaseNonNegative(productionTimeTicks, ticks);
    }

    public void decreaseAttackIntervalByTicks(int ticks) {
        attackIntervalTicks = decreaseNonNegative(attackIntervalTicks, ticks);

        if (attackIntervalTicks == 0 && type.getBaseCooldown() > 0) {
            attackIntervalTicks = 1;
        }
    }

    public void decreaseAttackIntervalByPercent(int percent) {
        if (percent <= 0 || attackIntervalTicks <= 0) {
            return;
        }

        int reduction = (int) Math.ceil(attackIntervalTicks * percent / 100.0);
        decreaseAttackIntervalByTicks(reduction);
    }

    public void decreaseArmTime(int ticks) {
        armTimeTicks = decreaseNonNegative(armTimeTicks, ticks);
    }

    public void decreaseChargeTime(int ticks) {
        chargeTimeTicks = decreaseNonNegative(chargeTimeTicks, ticks);
    }

    public void decreaseDigestTime(int ticks) {
        digestTimeTicks = decreaseNonNegative(digestTimeTicks, ticks);
    }

    public void decreaseEatTime(int ticks) {
        eatTimeTicks = decreaseNonNegative(eatTimeTicks, ticks);
    }

    public void decreaseRegenTime(int ticks) {
        regenTimeTicks = decreaseNonNegative(regenTimeTicks, ticks);
    }

    public void increaseWarmthRadius(int amount) {
        if (amount > 0) {
            warmthRadius += amount;
        }
    }

    public void increaseMaxSize(int amount) {
        if (amount > 0) {
            maxSize += amount;
        }
    }

    public void increasePlantFoodChance(int percent) {
        if (percent > 0) {
            plantFoodChancePercent += percent;
        }
    }

    public void increaseButterChance(int percent) {
        if (percent > 0) {
            butterChancePercent += percent;
        }
    }

    public void enableDoubleSunChance() {
        doubleSunChance = true;
    }

    public void enableTargetPriorityUp() {
        targetPriorityUp = true;
    }

    public void enableCanCrushTwice() {
        canCrushTwice = true;
    }

    public void enableAoeOnDeath() {
        aoeOnDeath = true;
    }

    public void enableZombieHpBuff() {
        zombieHpBuff = true;
    }

    public void enableZombieDamageBuff() {
        zombieDamageBuff = true;
    }

    public void enablePlantFoodOnEntrance() {
        plantFoodOnEntrance = true;
    }

    public void enableExplodeOnFinish() {
        explodeOnFinish = true;
    }

    public void enableResetFamilyCooldowns() {
        resetFamilyCooldowns = true;
    }

    public void enableMeltAreaThreeByThree() {
        meltAreaThreeByThree = true;
    }

    public void setAttackBehavior(AttackBehavior attackBehavior) {
        this.attackBehavior = attackBehavior;
    }

    private int decreaseNonNegative(int value, int amount) {
        if (amount <= 0) {
            return value;
        }

        int result = value - amount;
        return Math.max(0, result);
    }

    private int getEffectiveAttackDamage() {
        if (boosted) {
            return attackDamage * 2;
        }

        return attackDamage;
    }

    private String resolveDamageType() {
        String category = type.getCategory();

        if (category.equals("strike-through")) {
            return "piercing";
        }

        if (category.equals("homing")) {
            return "homing";
        }

        if (category.equals("lobber")) {
            return "lobber";
        }

        if (category.equals("melee")) {
            return "melee";
        }

        if (category.equals("explosive")) {
            return "explosive";
        }

        return "normal";
    }

    private int resolveInitialDamage(String category) {
        if (category == null) {
            return 0;
        }

        if (category.equals("shooter")) {
            return 20;
        }

        if (category.equals("strike-through")) {
            return 20;
        }

        if (category.equals("homing")) {
            return 30;
        }

        if (category.equals("lobber")) {
            return 40;
        }

        if (category.equals("melee")) {
            return 15;
        }

        if (category.equals("explosive")) {
            return 1800;
        }

        return 0;
    }
}