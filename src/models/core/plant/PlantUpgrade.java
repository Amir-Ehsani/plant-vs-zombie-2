package models.core.plant;

import java.util.Locale;

public class PlantUpgrade {
    private static final int TICKS_PER_SECOND = 10;

    private int requiredSeedPackets;
    private int coinCost;
    private int nextLevel;
    private String effect;

    public PlantUpgrade() {
        this(0, 0, 0, "");
    }

    public PlantUpgrade(int requiredSeedPackets, int coinCost, int nextLevel) {
        this(requiredSeedPackets, coinCost, nextLevel, "");
    }

    public PlantUpgrade(int nextLevel, String effect) {
        this(0, 0, nextLevel, effect);
    }

    public PlantUpgrade(int requiredSeedPackets, int coinCost, int nextLevel, String effect) {
        this.requiredSeedPackets = Math.max(0, requiredSeedPackets);
        this.coinCost = Math.max(0, coinCost);
        this.nextLevel = normalizeLevel(nextLevel);
        this.effect = normalizeEffect(effect);
    }

    public static PlantUpgrade fromPlantType(PlantType type, int targetLevel) {
        if (type == null) {
            return new PlantUpgrade();
        }

        return new PlantUpgrade(targetLevel, type.getUpgradeForLevel(targetLevel));
    }

    public void applyUpgrade(Plant plant) {
        if (!canApplyTo(plant)) {
            return;
        }

        int targetLevel = resolveTargetLevel(plant);

        if (!effect.isEmpty()) {
            applyEffect(plant, effect);
        }

        plant.setLevel(targetLevel);
    }

    public boolean canApplyTo(Plant plant) {
        return plant != null && plant.isAlive() && plant.getLevel() < 4;
    }

    public int getRequiredSeedPackets() {
        return requiredSeedPackets;
    }

    public void setRequiredSeedPackets(int requiredSeedPackets) {
        this.requiredSeedPackets = Math.max(0, requiredSeedPackets);
    }

    public int getCoinCost() {
        return coinCost;
    }

    public void setCoinCost(int coinCost) {
        this.coinCost = Math.max(0, coinCost);
    }

    public int getNextLevel() {
        return nextLevel;
    }

    public void setNextLevel(int nextLevel) {
        this.nextLevel = normalizeLevel(nextLevel);
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = normalizeEffect(effect);
    }

    private int resolveTargetLevel(Plant plant) {
        if (nextLevel <= plant.getLevel()) {
            return Math.min(4, plant.getLevel() + 1);
        }

        return Math.min(4, nextLevel);
    }

    private int normalizeLevel(int level) {
        if (level < 0) {
            return 0;
        }

        if (level > 4) {
            return 4;
        }

        return level;
    }

    private String normalizeEffect(String effect) {
        if (effect == null) {
            return "";
        }

        return effect.trim();
    }

    private void applyEffect(Plant plant, String rawEffect) {
        String effectText = rawEffect.trim();
        String normalized = normalizeForMatching(effectText);
        int value = extractFirstNumber(effectText);

        if (normalized.startsWith("dmg tick")) {
            plant.increaseDamagePerTick(value);
            return;
        }

        if (normalized.startsWith("aoe dmg")) {
            plant.increaseAreaDamage(value);
            return;
        }

        if (normalized.startsWith("explode dmg")) {
            plant.increaseExplodeDamage(value);
            return;
        }

        if (normalized.startsWith("reflect dmg")) {
            plant.increaseReflectDamage(value);
            return;
        }

        if (normalized.startsWith("dmg")) {
            plant.increaseAttackDamage(value);
            return;
        }

        if (normalized.startsWith("hp")) {
            plant.increaseMaxHp(value);
            return;
        }

        if (normalized.startsWith("cost")) {
            plant.decreaseSunCost(value);
            return;
        }

        if (normalized.startsWith("cooldown")) {
            plant.decreaseAttackIntervalByTicks(secondsToTicks(value));
            plant.reduceCooldown(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("atk speed")) {
            plant.decreaseAttackIntervalByPercent(value);
            return;
        }

        if (normalized.startsWith("range")) {
            plant.increaseRange(value);
            return;
        }

        if (normalized.startsWith("targets")) {
            plant.increaseTargetCount(value);
            return;
        }

        if (normalized.startsWith("pierce")) {
            plant.increasePierceCount(value);
            return;
        }

        if (normalized.startsWith("bounces")) {
            plant.increaseBounces(value);
            return;
        }

        if (normalized.startsWith("freeze time")) {
            plant.increaseFreezeDuration(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("chill time")) {
            plant.increaseChillDuration(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("duration")) {
            plant.increaseDuration(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("lifespan")) {
            plant.increaseLifespan(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("grow time")) {
            plant.decreaseGrowTime(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("prod time")) {
            plant.decreaseProductionTime(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("charge time")) {
            plant.decreaseChargeTime(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("arm time")) {
            plant.decreaseArmTime(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("digest")) {
            plant.decreaseDigestTime(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("eat time")) {
            plant.decreaseEatTime(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("regen")) {
            plant.decreaseRegenTime(secondsToTicks(value));
            return;
        }

        if (normalized.startsWith("warmth radius")) {
            plant.increaseWarmthRadius(value);
            return;
        }

        if (normalized.startsWith("max size")) {
            plant.increaseMaxSize(value);
            return;
        }

        if (normalized.startsWith("plant food chance")) {
            plant.increasePlantFoodChance(value);
            return;
        }

        if (normalized.startsWith("butter")) {
            plant.increaseButterChance(value);
            return;
        }

        if (normalized.startsWith("sun drop")) {
            plant.increaseSunDropBonus(value);
            return;
        }

        if (normalized.startsWith("sun")) {
            plant.increaseSunProductionBonus(value);
            return;
        }

        if (normalized.equals("double sun chance")) {
            plant.enableDoubleSunChance();
            return;
        }

        if (normalized.equals("target priority up")) {
            plant.enableTargetPriorityUp();
            return;
        }

        if (normalized.equals("can crush 2x")) {
            plant.enableCanCrushTwice();
            return;
        }

        if (normalized.equals("aoe on death")) {
            plant.enableAoeOnDeath();
            return;
        }

        if (normalized.equals("zombie hp buff")) {
            plant.enableZombieHpBuff();
            return;
        }

        if (normalized.equals("zombie dmg buff")) {
            plant.enableZombieDamageBuff();
            return;
        }

        if (normalized.equals("plant food on enterance") || normalized.equals("plant food on entrance")) {
            plant.enablePlantFoodOnEntrance();
            return;
        }

        if (normalized.equals("explode on finish")) {
            plant.enableExplodeOnFinish();
            return;
        }

        if (normalized.equals("reset family cooldowns")) {
            plant.enableResetFamilyCooldowns();
            return;
        }

        if (normalized.equals("melt area 3x3")) {
            plant.enableMeltAreaThreeByThree();
        }
    }

    private String normalizeForMatching(String value) {
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(".", "")
                .replace("/", " ")
                .replace("-", " ")
                .replace("+", "")
                .replace("%", "")
                .replaceAll("\\s+", " ");
    }

    private int extractFirstNumber(String value) {
        StringBuilder number = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);

            if (Character.isDigit(current)) {
                number.append(current);
            } else if (number.length() > 0) {
                break;
            }
        }

        if (number.length() == 0) {
            return 0;
        }

        return Integer.parseInt(number.toString());
    }

    private int secondsToTicks(int seconds) {
        return Math.max(0, seconds * TICKS_PER_SECOND);
    }
}