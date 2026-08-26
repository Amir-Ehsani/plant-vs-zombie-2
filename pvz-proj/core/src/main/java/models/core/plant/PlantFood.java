package models.core.plant;

import java.util.Locale;

public class PlantFood {
    private static final int DEFAULT_DURATION = 50;
    private static final int DEFAULT_HEAL_AMOUNT = 100;
    private static final int DEFAULT_FREEZE_TICKS = 50;
    private static final int DEFAULT_STUN_TICKS = 30;
    private static final int DEFAULT_POISON_TICKS = 50;

    private final int duration;
    private final int healAmount;
    private final int cooldownReduction;
    private int remainingDuration;
    private Plant boostedPlant;
    private boolean temporary;

    public PlantFood() {
        this(DEFAULT_DURATION, DEFAULT_HEAL_AMOUNT, Integer.MAX_VALUE);
    }

    public PlantFood(int duration) {
        this(duration, DEFAULT_HEAL_AMOUNT, Integer.MAX_VALUE);
    }

    public PlantFood(int duration, int healAmount, int cooldownReduction) {
        this.duration = Math.max(0, duration);
        this.remainingDuration = this.duration;
        this.healAmount = Math.max(0, healAmount);
        this.cooldownReduction = Math.max(0, cooldownReduction);
        this.temporary = false;
    }

    public void activateBoost(Plant plant) {
        activateBoost(plant, null);
    }

    public void activateBoost(Plant plant, PlantFoodContext context) {
        if (plant == null || !plant.isAlive()) {
            return;
        }

        deactivateCurrentPlant();
        boostedPlant = plant;
        remainingDuration = duration;
        temporary = applyPlantEffect(plant, context);
        reducePlantCooldown(plant);

        if (temporary) {
            plant.setBoosted(true);
        } else {
            remainingDuration = 0;
            boostedPlant = null;
        }
    }

    public void tick() {
        if (!isActive()) {
            return;
        }

        remainingDuration--;
        if (remainingDuration <= 0) {
            deactivateCurrentPlant();
        }
    }

    public boolean isActive() {
        return temporary
                && boostedPlant != null
                && boostedPlant.isAlive()
                && remainingDuration > 0;
    }

    public boolean isExpired() {
        return remainingDuration <= 0 || boostedPlant == null;
    }

    public int getDuration() {
        return duration;
    }

    public int getHealAmount() {
        return healAmount;
    }

    public int getCooldownReduction() {
        return cooldownReduction;
    }

    public int getRemainingDuration() {
        return remainingDuration;
    }

    public void expire() {
        deactivateCurrentPlant();
    }

    private boolean applyPlantEffect(Plant plant, PlantFoodContext context) {
        String name = normalize(plant.getName());
        int damage = Math.max(1, plant.getAttackDamage());
        Boolean result = applySunAndBasicPeaEffect(name, plant, context, damage);
        if (result == null) result = applyAdvancedPeaEffect(name, plant, context, damage);
        if (result == null) result = applyMushroomAndPultEffect(name, plant, context, damage);
        if (result == null) result = applyInstantAndMeleeEffect(name, plant, context, damage);
        if (result == null) result = applyDefenseAndSupportEffect(name, plant, context, damage);
        if (result != null) return result;
        plant.heal(healAmount);
        plant.setPlantFoodModifiers(2, 2, false);
        return true;
    }

    private Boolean applySunAndBasicPeaEffect(
            String name, Plant plant, PlantFoodContext context, int damage
    ) {
        return switch (name) {
            case "sunflower" -> spawnSunBurstEffect(context, plant, 150);
            case "twin sunflower" -> spawnSunBurstEffect(context, plant, 250);
            case "sun shroom" -> {
                plant.finishGrowth();
                spawnSunBurst(context, plant, 225);
                yield false;
            }
            case "primal sunflower" -> spawnSunBurstEffect(context, plant, 225);
            case "peashooter", "rotobaga", "split pea", "starfruit", "cat tail" ->
                    temporaryModifier(plant, 1, 5, false);
            case "repeater" -> {
                plant.setPlantFoodModifiers(1, 5, false);
                if (context != null) context.damageLane(plant, damage * 20, "plant food giant pea");
                yield true;
            }
            case "threepeater" -> temporaryModifier(plant, 1, 5, false);
            case "snow pea" -> {
                if (context != null) context.freezeLane(plant, DEFAULT_FREEZE_TICKS);
                yield temporaryModifier(plant, 1, 5, false);
            }
            default -> null;
        };
    }

    private Boolean applyAdvancedPeaEffect(
            String name, Plant plant, PlantFoodContext context, int damage
    ) {
        return switch (name) {
            case "pea pod" -> {
                if (context != null) {
                    context.damageLane(plant, damage * 20 * context.countPlantLayers(plant),
                            "plant food giant pea");
                }
                yield false;
            }
            case "citron" -> contextAction(context,
                    () -> context.damageLane(plant, 100000, "plant food plasma"));
            case "caulipower" -> contextAction(context, () -> context.hypnotizeRandom(3));
            case "electric blueberry" -> contextAction(context,
                    () -> context.killRandom(plant, 3, "plant food lightning"));
            case "bowling bulb" -> contextAction(context,
                    () -> context.damageRandom(plant, 3, Math.max(540, damage * 3), "plant food bulb"));
            case "cactus" -> temporaryModifier(plant, 3, 3, true);
            case "fire peashooter" -> {
                if (context != null) context.damageLane(plant, Math.max(200, damage * 5), "plant food fire");
                yield temporaryModifier(plant, 1, 5, false);
            }
            case "goo peashooter" -> {
                if (context != null) {
                    context.poisonLane(plant, Math.max(20, plant.getDamagePerTick()), DEFAULT_POISON_TICKS);
                }
                yield temporaryModifier(plant, 1, 5, false);
            }
            case "mega gatling pea" -> {
                if (context != null) context.damageLane(plant, damage * 80, "plant food mega pea");
                yield temporaryModifier(plant, 2, 8, false);
            }
            default -> null;
        };
    }

    private Boolean applyMushroomAndPultEffect(
            String name, Plant plant, PlantFoodContext context, int damage
    ) {
        return switch (name) {
            case "sea shroom", "puff shroom" -> {
                if (context != null) context.resetPlantAges(plant.getName());
                yield temporaryModifier(plant, 1, 5, false);
            }
            case "fume shroom" -> contextAction(context, () -> context.pushLane(plant, 2.0));
            case "cabbage pult" -> contextAction(context,
                    () -> context.damageRandom(plant, 5, Math.max(40, damage), "plant food cabbage"));
            case "kernel pult" -> contextAction(context, () -> context.butterAll(DEFAULT_STUN_TICKS));
            case "melon pult" -> contextAction(context,
                    () -> context.damageRandom(plant, 3, Math.max(240, damage * 3), "plant food melon"));
            case "winter melon" -> {
                if (context != null) {
                    context.damageRandom(plant, 3, Math.max(240, damage * 3), "plant food winter melon");
                    context.freezeAll(DEFAULT_FREEZE_TICKS);
                }
                yield false;
            }
            case "pepper pult" -> contextAction(context,
                    () -> context.damageRandom(plant, 3, Math.max(150, damage * 3), "plant food pepper"));
            default -> null;
        };
    }

    private Boolean applyInstantAndMeleeEffect(
            String name, Plant plant, PlantFoodContext context, int damage
    ) {
        return switch (name) {
            case "potato mine", "primal potato mine" -> {
                plant.finishArming();
                if (context != null) context.clonePlant(plant, 2);
                yield false;
            }
            case "squash" -> contextAction(context,
                    () -> context.killRandom(plant, 2, "plant food squash"));
            case "tangle kelp" -> contextAction(context,
                    () -> context.killRandom(plant, 3, "plant food tangle"));
            case "iceberg lettuce" -> contextAction(context,
                    () -> context.freezeAll(DEFAULT_FREEZE_TICKS));
            case "bonk choy" -> {
                if (context != null) {
                    context.damageArea(plant, 1, 1, Math.max(150, damage * 10), "plant food punch");
                }
                yield temporaryModifier(plant, 1, 5, false);
            }
            case "phat beet" -> contextAction(context,
                    () -> context.damageArea(plant, 1, 1, Math.max(300, damage * 20), "plant food sonic"));
            case "chomper" -> contextAction(context,
                    () -> context.killRandom(plant, 3, "plant food chomp"));
            case "wasabi whip" -> contextAction(context,
                    () -> context.damageArea(plant, 1, 1, Math.max(400, damage * 10), "plant food whip"));
            case "kiwibeast" -> contextAction(context,
                    () -> context.damageArea(plant, 1, 1, Math.max(450, damage * 10), "plant food slam"));
            default -> null;
        };
    }

    private Boolean applyDefenseAndSupportEffect(
            String name, Plant plant, PlantFoodContext context, int damage
    ) {
        return switch (name) {
            case "wall nut" -> addArmorEffect(plant, 4000);
            case "tall nut" -> addArmorEffect(plant, 8000);
            case "endurian" -> {
                plant.addArmor(3000);
                plant.increaseReflectDamage(Math.max(20, damage));
                yield false;
            }
            case "garlic" -> contextAction(context, () -> context.shiftLaneZombies(plant));
            case "sweet potato" -> {
                if (context != null) context.attractNearbyZombies(plant);
                plant.healToFull();
                yield false;
            }
            case "explode o nut", "pumpkin" -> addArmorEffect(plant, 4000);
            case "sun bean" -> addArmorEffect(plant, 1000);
            case "torchwood" -> {
                plant.enableBlueFlame();
                yield false;
            }
            case "magnet shroom" -> contextAction(context, () -> context.removeArmorFromRandom(5));
            case "hypno shroom" -> {
                plant.enablePlantFoodHypnoGargantuar();
                yield true;
            }
            case "lily pad" -> contextAction(context, () -> context.cloneLilyPads(3));
            default -> null;
        };
    }

    private boolean temporaryModifier(Plant plant, int multiplier, int cooldownRate, boolean pierce) {
        plant.setPlantFoodModifiers(multiplier, cooldownRate, pierce);
        return true;
    }

    private boolean spawnSunBurstEffect(PlantFoodContext context, Plant plant, int amount) {
        spawnSunBurst(context, plant, amount);
        return false;
    }

    private void spawnSunBurst(PlantFoodContext context, Plant plant, int amount) {
        if (context != null) {
            context.spawnSunBurst(plant, amount);
        }
    }

    private boolean addArmorEffect(Plant plant, int amount) {
        plant.addArmor(amount);
        return false;
    }

    private boolean contextAction(PlantFoodContext context, Runnable action) {
        if (context != null) action.run();
        return false;
    }

    private void reducePlantCooldown(Plant plant) {
        if (cooldownReduction >= plant.getCooldownRemaining()) {
            plant.resetCooldown();
        } else {
            plant.reduceCooldown(cooldownReduction);
        }
    }

    private void deactivateCurrentPlant() {
        if (boostedPlant != null) {
            boostedPlant.setBoosted(false);
            boostedPlant.resetPlantFoodModifiers();
        }
        boostedPlant = null;
        remainingDuration = 0;
        temporary = false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }
}
