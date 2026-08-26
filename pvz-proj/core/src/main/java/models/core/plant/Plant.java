package models.core.plant;

import models.core.base.GameEntity;
import models.core.projectile.Damage;
import models.core.zombie.Zombie;

import java.util.Locale;


public class Plant extends PlantState {
    public Plant() {
        super();
    }

    public Plant(PlantType type, double x, double y) {
        super(type, x, y);
    }

    public Plant(PlantType type, double x, double y, AttackBehavior attackBehavior) {
        super(type, x, y, attackBehavior);
    }


    public void attack() {
        if (!isAlive() || cooldownRemaining > 0) {
            return;
        }

        cooldownRemaining = attackIntervalTicks;
        registerAttackVisual();
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
        registerAttackVisual();
    }

    private boolean canAttack(GameEntity target) {
        return isAlive()
                && target != null
                && target.isAlive()
                && cooldownRemaining == 0;
    }

    public void usePlantFood(PlantFood food) {
        usePlantFood(food, null);
    }

    public void usePlantFood(PlantFood food, PlantFoodContext context) {
        if (food == null || !isAlive()) {
            return;
        }
        food.activateBoost(this, context);
        visualPlantFoodSerial++;
    }

    public void prepareAttackAnimation(String clip) {
        pendingVisualAttackClip = clip;
    }

    public void triggerSpecialAnimation(String clip) {
        if (clip == null || clip.isBlank()) {
            return;
        }
        visualSpecialClip = clip;
        visualSpecialSerial++;
    }

    private void registerAttackVisual() {
        visualAttackClip = pendingVisualAttackClip == null ? "attack" : pendingVisualAttackClip;
        pendingVisualAttackClip = null;
        visualAttackSerial++;
    }

    public void upgrade(PlantUpgrade upgrade) {
        if (upgrade == null || !isAlive()) {
            return;
        }

        upgrade.applyUpgrade(this);
    }

    public void tickCooldown() {
        if (cooldownRemaining > 0) {
            cooldownRemaining = Math.max(0, cooldownRemaining - Math.max(1, plantFoodCooldownRate));
        }
    }

    @Override
    public void takeDamage(Damage damage) {
        if (damage == null || damage.getAmount() <= 0 || !isAlive()) {
            return;
        }

        int remainingDamage = damage.getAmount();
        if (armorHp > 0) {
            int armorBefore = armorHp;
            int absorbed = Math.min(armorHp, remainingDamage);
            armorHp -= absorbed;
            remainingDamage -= absorbed;
            if (explosiveArmor && armorBefore > 0 && armorHp == 0) {
                explosiveArmorBreakPending = true;
                explosiveArmor = false;
            }
        }
        if (remainingDamage > 0) {
            hp = Math.max(0, hp - remainingDamage);
        }
    }


    public void setBoosted(boolean boosted) {
        this.boosted = boosted;
    }

    public void setPlantFoodModifiers(int damageMultiplier, int cooldownRate, boolean unlimitedPierce) {
        plantFoodDamageMultiplier = Math.max(1, damageMultiplier);
        plantFoodCooldownRate = Math.max(1, cooldownRate);
        plantFoodUnlimitedPierce = unlimitedPierce;
    }

    public void resetPlantFoodModifiers() {
        plantFoodDamageMultiplier = 1;
        plantFoodCooldownRate = 1;
        plantFoodUnlimitedPierce = false;
        plantFoodHypnoGargantuar = false;
    }

    public void addArmor(int amount) {
        if (amount > 0) {
            armorHp += amount;
        }
    }

    public void addExplosiveArmor(int amount) {
        if (amount > 0) {
            armorHp += amount;
            explosiveArmor = true;
            explosiveArmorBreakPending = false;
        }
    }

    public boolean consumeExplosiveArmorBreak() {
        boolean pending = explosiveArmorBreakPending;
        explosiveArmorBreakPending = false;
        return pending;
    }

    public void healToFull() {
        if (isAlive()) {
            hp = maxHp;
        }
    }

    public void kill() {
        hp = 0;
        armorHp = 0;
    }

    public void finishGrowth() {
        growTimeTicks = 0;
    }

    public void finishArming() {
        armTimeTicks = 0;
    }

    public void enableBlueFlame() {
        blueFlame = true;
    }

    public void enablePlantFoodHypnoGargantuar() {
        plantFoodHypnoGargantuar = true;
    }

    public void addIceHit() {
        iceHits = Math.min(3, iceHits + 1);
    }

    public void removeIceHit() {
        iceHits = Math.max(0, iceHits - 1);
    }

    public void addOctopus() {
        octopusHits = Math.max(3, octopusHits);
    }

    public void damageOctopus() {
        octopusHits = Math.max(0, octopusHits - 1);
    }

    public void transformToCat(Zombie wizard) {
        if (wizard != null) {
            transformedByWizard = wizard;
        }
    }

    public void restoreFromCat(Zombie wizard) {
        if (wizard == null || transformedByWizard == wizard) {
            transformedByWizard = null;
        }
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

}
