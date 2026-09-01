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
        String normalized = normalizeForMatching(rawEffect);
        int value = extractFirstNumber(rawEffect);
        if (applyDamageOrHealthEffect(plant, normalized, value)) {
            return;
        }
        if (applyCombatStatEffect(plant, normalized, value)) {
            return;
        }
        if (applyTimingEffect(plant, normalized, value)) {
            return;
        }
        if (applyProductionEffect(plant, normalized, value)) {
            return;
        }
        applyBooleanEffect(plant, normalized);
    }

    private boolean applyDamageOrHealthEffect(Plant plant, String effect, int value) {
        if (effect.startsWith("dmg tick")) {
            plant.increaseDamagePerTick(value);
        }
        else if (effect.startsWith("aoe dmg")) {
            plant.increaseAreaDamage(value);
        }
        else if (effect.startsWith("explode dmg")) {
            plant.increaseExplodeDamage(value);
        }
        else if (effect.startsWith("reflect dmg")) {
            plant.increaseReflectDamage(value);
        }
        else if (effect.startsWith("dmg")) {
            plant.increaseAttackDamage(value);
        }
        else if (effect.startsWith("hp")) {
            plant.increaseMaxHp(value);
        }
        else {
            return false;
        }
        return true;
    }

    private boolean applyCombatStatEffect(Plant plant, String effect, int value) {
        if (effect.startsWith("cost")) {
            plant.decreaseSunCost(value);
        }
        else if (effect.startsWith("cooldown")) {
            int ticks = secondsToTicks(value);
            plant.decreaseAttackIntervalByTicks(ticks);
            plant.reduceCooldown(ticks);
        } else if (effect.startsWith("atk speed")) {
            plant.decreaseAttackIntervalByPercent(value);
        } else if (effect.startsWith("range")) {
            plant.increaseRange(value);
        }
        else if (effect.startsWith("targets")) {
            plant.increaseTargetCount(value);
        }
        else if (effect.startsWith("pierce")) {
            plant.increasePierceCount(value);
        }
        else if (effect.startsWith("bounces")) {
            plant.increaseBounces(value);
        }
        else {
            return false;
        }
        return true;
    }

    private boolean applyTimingEffect(Plant plant, String effect, int value) {
        int ticks = secondsToTicks(value);
        if (effect.startsWith("freeze time")) {
            plant.increaseFreezeDuration(ticks);
        }
        else if (effect.startsWith("chill time")) {
            plant.increaseChillDuration(ticks);
        }
        else if (effect.startsWith("duration")) {
            plant.increaseDuration(ticks);
        }
        else if (effect.startsWith("lifespan")) {
            plant.increaseLifespan(ticks);
        }
        else if (effect.startsWith("grow time")) {
            plant.decreaseGrowTime(ticks);
        }
        else if (effect.startsWith("prod time")) {
            plant.decreaseProductionTime(ticks);
        }
        else if (effect.startsWith("charge time")) {
            plant.decreaseChargeTime(ticks);
        }
        else if (effect.startsWith("arm time")) {
            plant.decreaseArmTime(ticks);
        }
        else if (effect.startsWith("digest")) {
            plant.decreaseDigestTime(ticks);
        }
        else if (effect.startsWith("eat time")) {
            plant.decreaseEatTime(ticks);
        }
        else if (effect.startsWith("regen")) {
            plant.decreaseRegenTime(ticks);
        }
        else {
            return false;
        }
        return true;
    }

    private boolean applyProductionEffect(Plant plant, String effect, int value) {
        if (effect.startsWith("warmth radius")) {
            plant.increaseWarmthRadius(value);
        }
        else if (effect.startsWith("max size")) {
            plant.increaseMaxSize(value);
        }
        else if (effect.startsWith("plant food chance")) {
            plant.increasePlantFoodChance(value);
        }
        else if (effect.startsWith("butter")) {
            plant.increaseButterChance(value);
        }
        else if (effect.startsWith("sun drop")) {
            plant.increaseSunDropBonus(value);
        }
        else if (effect.startsWith("sun")) {
            plant.increaseSunProductionBonus(value);
        }
        else {
            return false;
        }
        return true;
    }

    private void applyBooleanEffect(Plant plant, String effect) {
        switch (effect) {
            case "double sun chance" -> plant.enableDoubleSunChance();
            case "target priority up" -> plant.enableTargetPriorityUp();
            case "can crush 2x" -> plant.enableCanCrushTwice();
            case "aoe on death" -> plant.enableAoeOnDeath();
            case "zombie hp buff" -> plant.enableZombieHpBuff();
            case "zombie dmg buff" -> plant.enableZombieDamageBuff();
            case "plant food on enterance", "plant food on entrance" -> plant.enablePlantFoodOnEntrance();
            case "explode on finish" -> plant.enableExplodeOnFinish();
            case "reset family cooldowns" -> plant.enableResetFamilyCooldowns();
            case "melt area 3x3" -> plant.enableMeltAreaThreeByThree();
            default -> { }
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
