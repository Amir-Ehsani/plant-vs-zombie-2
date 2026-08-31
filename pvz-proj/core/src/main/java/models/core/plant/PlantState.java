package models.core.plant;

import models.core.base.GameEntity;
import models.core.projectile.Damage;
import models.core.zombie.Zombie;

import java.util.Locale;


abstract class PlantState extends GameEntity {
    protected int level;
    protected int cooldownRemaining;
    protected boolean boosted;
    protected AttackBehavior attackBehavior;
    protected final PlantType type;
    protected int currentSunCost;
    protected int attackDamage;
    protected int damagePerTick;
    protected int areaDamage;
    protected int explodeDamage;
    protected int reflectDamage;
    protected int sunProductionBonus;
    protected int sunDropBonus;
    protected int attackIntervalTicks;
    protected int range;
    protected int targetCount;
    protected int pierceCount;
    protected int bounces;
    protected int freezeDurationTicks;
    protected int chillDurationTicks;
    protected int durationTicks;
    protected int lifespanTicks;
    protected int growTimeTicks;
    protected int productionTimeTicks;
    protected int armTimeTicks;
    protected int chargeTimeTicks;
    protected int digestTimeTicks;
    protected int eatTimeTicks;
    protected int regenTimeTicks;
    protected int warmthRadius;
    protected int maxSize;
    protected int plantFoodChancePercent;
    protected int butterChancePercent;
    protected boolean doubleSunChance;
    protected boolean targetPriorityUp;
    protected boolean canCrushTwice;
    protected boolean aoeOnDeath;
    protected boolean zombieHpBuff;
    protected boolean zombieDamageBuff;
    protected boolean plantFoodOnEntrance;
    protected boolean explodeOnFinish;
    protected boolean resetFamilyCooldowns;
    protected boolean meltAreaThreeByThree;
    protected int armorHp;
    protected int plantFoodDamageMultiplier;
    protected int plantFoodCooldownRate;
    protected boolean plantFoodUnlimitedPierce;
    protected boolean blueFlame;
    protected boolean plantFoodHypnoGargantuar;
    protected boolean explosiveArmor;
    protected boolean explosiveArmorBreakPending;
    protected boolean growthFinished;
    protected boolean armingFinished;
    protected int iceHits;
    protected int iceHealth;
    protected int octopusHits;
    protected Zombie transformedByWizard;
    protected int visualAttackSerial;
    protected int visualPlantFoodSerial;
    protected int visualSpecialSerial;
    protected String visualAttackClip;
    protected String visualSpecialClip;
    protected String pendingVisualAttackClip;


    protected PlantState() {
        this(new PlantType(), 0, 0, null);
    }
    protected PlantState(PlantType type, double x, double y) {
        this(type, x, y, null);
    }
    protected PlantState(PlantType type, double x, double y, AttackBehavior attackBehavior) {
        this.type = type == null ? new PlantType() : type;
        this.x = x;
        this.y = y;
        this.maxHp = this.type.getBaseHp();
        this.hp = this.maxHp;
        this.level = 1;
        this.cooldownRemaining = 0;
        this.boosted = false;
        this.attackBehavior = attackBehavior;
        initializeCombatStats();
        initializeTimingStats();
        initializeSpecialStats();
        this.id = buildId();
    }
    private void initializeCombatStats() {
        currentSunCost = type.getSunCost();
        attackIntervalTicks = type.getBaseCooldown();
        attackDamage = resolveInitialDamage(type);
        damagePerTick = 0;
        areaDamage = 0;
        explodeDamage = 0;
        reflectDamage = 0;
        sunProductionBonus = 0;
        sunDropBonus = 0;
        range = 1;
        targetCount = 1;
        pierceCount = 0;
        bounces = 0;
    }
    private void initializeTimingStats() {
        freezeDurationTicks = 0;
        chillDurationTicks = 0;
        durationTicks = 0;
        lifespanTicks = 0;
        growTimeTicks = 0;
        productionTimeTicks = type.getBaseCooldown();
        armTimeTicks = 0;
        chargeTimeTicks = type.getBaseCooldown();
        digestTimeTicks = type.getBaseCooldown();
        eatTimeTicks = 0;
        regenTimeTicks = 0;
        warmthRadius = 0;
        maxSize = 1;
    }
    private void initializeSpecialStats() {
        plantFoodChancePercent = 0;
        butterChancePercent = 0;
        doubleSunChance = false;
        targetPriorityUp = false;
        canCrushTwice = false;
        aoeOnDeath = false;
        zombieHpBuff = false;
        zombieDamageBuff = false;
        plantFoodOnEntrance = false;
        explodeOnFinish = false;
        resetFamilyCooldowns = false;
        meltAreaThreeByThree = false;
        armorHp = 0;
        plantFoodDamageMultiplier = 1;
        plantFoodCooldownRate = 1;
        plantFoodUnlimitedPierce = false;
        blueFlame = false;
        plantFoodHypnoGargantuar = false;
        explosiveArmor = false;
        explosiveArmorBreakPending = false;
        growthFinished = false;
        armingFinished = false;
        iceHits = 0;
        iceHealth = 0;
        octopusHits = 0;
        transformedByWizard = null;
        visualAttackSerial = 0;
        visualPlantFoodSerial = 0;
        visualSpecialSerial = 0;
        visualAttackClip = "attack";
        visualSpecialClip = "special";
        pendingVisualAttackClip = null;
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
    public void moveTo(double x, double y) {
        this.x = x;
        this.y = y;
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
    public int getVisualAttackSerial() {
        return visualAttackSerial;
    }
    public int getVisualPlantFoodSerial() {
        return visualPlantFoodSerial;
    }
    public int getVisualSpecialSerial() {
        return visualSpecialSerial;
    }
    public String getVisualAttackClip() {
        return visualAttackClip;
    }
    public String getVisualSpecialClip() {
        return visualSpecialClip;
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
    public int getArmorHp() {
        return armorHp;
    }
    public int getPlantFoodDamageMultiplier() {
        return plantFoodDamageMultiplier;
    }
    public boolean hasPlantFoodUnlimitedPierce() {
        return plantFoodUnlimitedPierce;
    }
    public boolean hasBlueFlame() {
        return blueFlame;
    }
    public boolean hasPlantFoodHypnoGargantuar() {
        return plantFoodHypnoGargantuar;
    }
    public int getIceHits() {
        return iceHits;
    }
    public int getIceHealth() {
        return iceHealth;
    }
    public int getOctopusHits() {
        return octopusHits;
    }
    public Zombie getTransformedByWizard() {
        return transformedByWizard;
    }
    public boolean isFrozenByZombie() {
        return iceHits >= 3 && iceHealth > 0;
    }
    public boolean isCoveredByOctopus() {
        return octopusHits > 0;
    }
    public boolean isTransformedToSheep() {
        return transformedByWizard != null;
    }
    /** @deprecated Kept for compatibility with older save/runtime code. */
    @Deprecated
    public boolean isTransformedToCat() {
        return isTransformedToSheep();
    }
    public boolean isDisabled() {
        // An octopus cover disables the plant until the cover loses all of its durability.
        return isFrozenByZombie() || isCoveredByOctopus() || isTransformedToSheep();
    }
    public int getIceVisualLevel() {
        if (iceHits <= 0) {
            return 0;
        }
        if (iceHits < 3) {
            return iceHits;
        }
        if (iceHealth <= 0) {
            return 0;
        }
        if (iceHealth > 400) {
            return 3;
        }
        if (iceHealth > 200) {
            return 2;
        }
        return 1;
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
    public boolean isGrowthFinished() {
        return growthFinished;
    }

    public boolean isArmingFinished() {
        return armingFinished;
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
    protected int decreaseNonNegative(int value, int amount) {
        if (amount <= 0) {
            return value;
        }

        int result = value - amount;
        return Math.max(0, result);
    }
    protected int getEffectiveAttackDamage() {
        return attackDamage * Math.max(1, plantFoodDamageMultiplier);
    }
    protected String resolveDamageType() {
        String category = normalizeCategory(type.getCategory());

        if (category.equals("strike through")) {
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
    protected int resolveInitialDamage(PlantType plantType) {
        if (plantType == null) {
            return 0;
        }

        int configuredDamage = extractFirstNumber(plantType.getDamage());
        if (configuredDamage > 0) {
            return configuredDamage;
        }

        String normalizedCategory = normalizeCategory(plantType.getCategory());
        if (normalizedCategory.equals("shooter") || normalizedCategory.equals("strike through")) {
            return 20;
        }
        if (normalizedCategory.equals("homing")) {
            return 30;
        }
        if (normalizedCategory.equals("lobber")) {
            return 40;
        }
        if (normalizedCategory.equals("melee")) {
            return 15;
        }
        if (normalizedCategory.equals("explosive")) {
            return 1800;
        }
        return 0;
    }
    protected int extractFirstNumber(String value) {
        if (value == null) {
            return 0;
        }
        StringBuilder digits = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isDigit(current)) {
                digits.append(current);
            } else if (digits.length() > 0) {
                break;
            }
        }
        return digits.length() == 0 ? 0 : Integer.parseInt(digits.toString());
    }
    protected String normalizeCategory(String category) {
        if (category == null) {
            return "";
        }

        return category
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", " ")
                .replace("_", " ")
                .replaceAll("\\s+", " ");
    }
    protected String buildId() {
        return type.getName() + "@" + x + "," + y;
    }
}
