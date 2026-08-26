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
            if (context != null) {
                remainingDuration += PlantActionTiming.plantFoodImpactTicks(plant.getName());
            }
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
        if (hasNoPlantFoodEffect(name)) {
            return false;
        }
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


    private boolean hasNoPlantFoodEffect(String name) {
        return switch (name) {
            case "gold bloom", "cherry bomb", "grapeshot", "jalapeno", "doom shroom",
                    "imitater", "ice shroom", "hot potato", "grave buster",
                    "enlighten mint", "appease mint", "arma mint", "bombard mint",
                    "enforce mint", "reinforce mint", "enchant mint", "pierce mint",
                    "cattail mint" -> true;
            default -> false;
        };
    }

    private Boolean applySunAndBasicPeaEffect(
            String name, Plant plant, PlantFoodContext context, int damage
    ) {
        return switch (name) {
            case "sunflower" -> spawnSunBurstEffect(context, plant, 150);
            case "twin sunflower" -> spawnSunBurstEffect(context, plant, 250);
            case "sun shroom" -> {
                if (context != null) {
                    runAtPlantFoodImpact(context, plant, () -> {
                        plant.finishGrowth();
                        context.spawnSunBurst(plant, 225);
                    });
                } else {
                    plant.finishGrowth();
                }
                yield false;
            }
            case "primal sunflower" -> spawnSunBurstEffect(context, plant, 225);
            case "peashooter", "rotobaga", "split pea", "starfruit", "cat tail" ->
                    temporaryModifier(context, plant, 1, 5, false);
            case "repeater" -> {
                applyTemporaryModifiers(context, plant, 1, 5, false);
                if (context != null) runAtPlantFoodImpact(context, plant,
                        () -> context.damageLane(plant, damage * 20, "plant food giant pea"));
                yield true;
            }
            case "threepeater" -> temporaryModifier(context, plant, 1, 5, false);
            case "snow pea" -> {
                if (context != null) runAtPlantFoodImpact(context, plant, () -> context.freezeLane(plant, DEFAULT_FREEZE_TICKS));
                yield temporaryModifier(context, plant, 1, 5, false);
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
                    runAtPlantFoodImpact(context, plant, () -> context.damageLane(plant,
                            damage * 20 * context.countPlantLayers(plant), "plant food giant pea"));
                }
                yield false;
            }
            case "citron" -> contextAction(context, plant,
                    () -> context.damageLane(plant, 100000, "plant food plasma"));
            case "caulipower" -> contextAction(context, plant, () -> context.hypnotizeRandom(3));
            case "electric blueberry" -> contextAction(context, plant,
                    () -> context.killRandom(plant, 3, "plant food lightning"));
            case "bowling bulb" -> contextAction(context, plant,
                    () -> context.damageRandom(plant, 3, Math.max(540, damage * 3), "plant food bulb"));
            case "cactus" -> temporaryModifier(context, plant, 3, 3, true);
            case "fire peashooter" -> {
                if (context != null) {
                    int start = PlantActionTiming.plantFoodImpactTicks(plant.getName());
                    int pulseDamage = Math.max(300, damage * 3);
                    for (int pulse = 0; pulse < 6; pulse++) {
                        int delay = start + pulse * 5;
                        context.runDelayed(delay, () -> context.damageLane(
                                plant, pulseDamage, "plant food fire"));
                    }
                }
                yield temporaryModifier(context, plant, 1, 5, false);
            }
            case "goo peashooter" -> {
                if (context != null) {
                    runAtPlantFoodImpact(context, plant, () -> context.poisonLane(plant,
                            Math.max(20, plant.getDamagePerTick()), DEFAULT_POISON_TICKS));
                }
                yield temporaryModifier(context, plant, 1, 5, false);
            }
            case "mega gatling pea" -> {
                if (context != null) runAtPlantFoodImpact(context, plant, () -> context.damageLane(plant, damage * 80, "plant food mega pea"));
                yield temporaryModifier(context, plant, 2, 8, false);
            }
            default -> null;
        };
    }

    private Boolean applyMushroomAndPultEffect(
            String name, Plant plant, PlantFoodContext context, int damage
    ) {
        return switch (name) {
            case "sea shroom", "puff shroom" -> {
                if (context != null) runAtPlantFoodImpact(context, plant, () -> context.resetPlantAges(plant.getName()));
                yield temporaryModifier(context, plant, 1, 5, false);
            }
            case "fume shroom" -> {
                if (context != null) {
                    int start = PlantActionTiming.plantFoodImpactTicks(plant.getName());
                    for (int pulse = 0; pulse < 5; pulse++) {
                        int delay = start + pulse * 10;
                        context.runDelayed(delay, () -> context.damageLane(
                                plant, 300, "plant food fumes"));
                    }
                    context.runDelayed(start + 40, () -> context.pushLane(plant, 2.0));
                }
                yield false;
            }
            case "cabbage pult" -> contextAction(context, plant,
                    () -> context.damageRandom(plant, 5, Math.max(40, damage), "plant food cabbage"));
            case "kernel pult" -> contextAction(context, plant, () -> context.butterAll(DEFAULT_STUN_TICKS));
            case "melon pult" -> contextAction(context, plant,
                    () -> context.damageRandom(plant, 3, Math.max(240, damage * 3), "plant food melon"));
            case "winter melon" -> {
                if (context != null) {
                    runAtPlantFoodImpact(context, plant, () -> {
                        context.damageRandom(plant, 3, Math.max(240, damage * 3), "plant food winter melon");
                        context.freezeAll(DEFAULT_FREEZE_TICKS);
                    });
                }
                yield false;
            }
            case "pepper pult" -> contextAction(context, plant,
                    () -> context.damageRandom(plant, 3, Math.max(150, damage * 3), "plant food pepper"));
            default -> null;
        };
    }

    private Boolean applyInstantAndMeleeEffect(
            String name, Plant plant, PlantFoodContext context, int damage
    ) {
        return switch (name) {
            case "potato mine", "primal potato mine" -> {
                if (context != null) {
                    runAtPlantFoodImpact(context, plant, () -> {
                        plant.finishArming();
                        context.clonePlant(plant, 2);
                    });
                } else {
                    plant.finishArming();
                }
                yield false;
            }
            case "squash" -> contextAction(context, plant,
                    () -> context.killRandom(plant, 2, "plant food squash"));
            case "tangle kelp" -> contextAction(context, plant,
                    () -> context.killRandom(plant, 3, "plant food tangle"));
            case "iceberg lettuce" -> contextAction(context, plant,
                    () -> context.freezeAll(DEFAULT_FREEZE_TICKS));
            case "bonk choy" -> {
                if (context != null) {
                    int start = PlantActionTiming.plantFoodImpactTicks(plant.getName());
                    int totalDamage = Math.max(1500, damage * 100);
                    int pulseDamage = Math.max(1, totalDamage / 6);
                    for (int pulse = 0; pulse < 6; pulse++) {
                        int delay = start + pulse * 4;
                        context.runDelayed(delay, () -> context.damageArea(
                                plant, 1, 1, pulseDamage, "plant food punch"));
                    }
                }
                yield temporaryModifier(context, plant, 1, 5, false);
            }
            case "phat beet" -> contextAction(context, plant,
                    () -> context.damageArea(plant, 1, 1, Math.max(300, damage * 20), "plant food sonic"));
            case "chomper" -> contextAction(context, plant,
                    () -> context.killRandom(plant, 3, "plant food chomp"));
            case "wasabi whip" -> {
                if (context != null) {
                    int start = PlantActionTiming.plantFoodImpactTicks(plant.getName());
                    int pulseDamage = Math.max(300, damage * 8);
                    for (int pulse = 0; pulse < 5; pulse++) {
                        int delay = start + pulse * 2;
                        context.runDelayed(delay, () -> context.damageArea(
                                plant, 1, 1, pulseDamage, "plant food whip"));
                    }
                }
                yield false;
            }
            case "kiwibeast" -> contextAction(context, plant,
                    () -> context.damageArea(plant, 1, 1, Math.max(450, damage * 10), "plant food slam"));
            default -> null;
        };
    }

    private Boolean applyDefenseAndSupportEffect(
            String name, Plant plant, PlantFoodContext context, int damage
    ) {
        return switch (name) {
            case "wall nut" -> contextAction(context, plant, () -> plant.addArmor(4000));
            case "tall nut" -> contextAction(context, plant, () -> plant.addArmor(8000));
            case "endurian" -> contextAction(context, plant, () -> {
                plant.addArmor(3000);
                plant.increaseReflectDamage(Math.max(20, damage));
            });
            case "garlic" -> contextAction(context, plant, () -> context.shiftLaneZombies(plant));
            case "sweet potato" -> contextAction(context, plant, () -> {
                context.attractNearbyZombies(plant);
                plant.healToFull();
            });
            case "explode o nut" -> contextAction(context, plant, () -> plant.addExplosiveArmor(4000));
            case "pumpkin" -> contextAction(context, plant, () -> plant.addArmor(4000));
            case "sun bean" -> contextAction(context, plant, () -> plant.addArmor(1000));
            case "torchwood" -> contextAction(context, plant, plant::enableBlueFlame);
            case "magnet shroom" -> contextAction(context, plant, () -> context.removeArmorFromRandom(5));
            case "hypno shroom" -> {
                if (context != null) {
                    runAtPlantFoodImpact(context, plant, plant::enablePlantFoodHypnoGargantuar);
                } else {
                    plant.enablePlantFoodHypnoGargantuar();
                }
                yield true;
            }
            case "lily pad" -> contextAction(context, plant, () -> context.cloneLilyPads(3));
            default -> null;
        };
    }

    private boolean temporaryModifier(
            PlantFoodContext context,
            Plant plant,
            int multiplier,
            int cooldownRate,
            boolean pierce
    ) {
        applyTemporaryModifiers(context, plant, multiplier, cooldownRate, pierce);
        return true;
    }

    private void applyTemporaryModifiers(
            PlantFoodContext context,
            Plant plant,
            int multiplier,
            int cooldownRate,
            boolean pierce
    ) {
        Runnable apply = () -> plant.setPlantFoodModifiers(multiplier, cooldownRate, pierce);
        if (context == null) {
            apply.run();
        } else {
            runAtPlantFoodImpact(context, plant, apply);
        }
    }

    private boolean spawnSunBurstEffect(PlantFoodContext context, Plant plant, int amount) {
        spawnSunBurst(context, plant, amount);
        return false;
    }

    private void spawnSunBurst(PlantFoodContext context, Plant plant, int amount) {
        if (context != null) {
            runAtPlantFoodImpact(context, plant, () -> context.spawnSunBurst(plant, amount));
        }
    }

    private boolean addArmorEffect(Plant plant, int amount) {
        plant.addArmor(amount);
        return false;
    }

    private boolean contextAction(PlantFoodContext context, Plant plant, Runnable action) {
        if (context != null) {
            runAtPlantFoodImpact(context, plant, action);
        }
        return false;
    }

    private void runAtPlantFoodImpact(
            PlantFoodContext context, Plant plant, Runnable action
    ) {
        if (context == null || action == null) {
            return;
        }
        context.runDelayed(PlantActionTiming.plantFoodImpactTicks(plant.getName()), action);
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
