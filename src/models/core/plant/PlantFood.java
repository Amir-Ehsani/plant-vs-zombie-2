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

        switch (name) {
            case "sunflower" -> addSun(context, 150);
            case "twin sunflower" -> addSun(context, 250);
            case "sun shroom" -> {
                plant.finishGrowth();
                addSun(context, 225);
            }
            case "primal sunflower" -> addSun(context, 225);
            case "peashooter", "rotobaga", "split pea", "starfruit", "cat tail" -> {
                plant.setPlantFoodModifiers(1, 5, false);
                return true;
            }
            case "repeater" -> {
                plant.setPlantFoodModifiers(1, 5, false);
                if (context != null) {
                    context.damageLane(plant, damage * 20, "plant food giant pea");
                }
                return true;
            }
            case "threepeater" -> {
                plant.setPlantFoodModifiers(1, 5, false);
                return true;
            }
            case "snow pea" -> {
                if (context != null) {
                    context.freezeLane(plant, DEFAULT_FREEZE_TICKS);
                }
                plant.setPlantFoodModifiers(1, 5, false);
                return true;
            }
            case "pea pod" -> {
                if (context != null) {
                    context.damageLane(
                            plant,
                            damage * 20 * context.countPlantLayers(plant),
                            "plant food giant pea"
                    );
                }
            }
            case "citron" -> {
                if (context != null) {
                    context.damageLane(plant, 100000, "plant food plasma");
                }
            }
            case "caulipower" -> {
                if (context != null) {
                    context.hypnotizeRandom(3);
                }
            }
            case "electric blueberry" -> {
                if (context != null) {
                    context.killRandom(plant, 3, "plant food lightning");
                }
            }
            case "bowling bulb" -> {
                if (context != null) {
                    context.damageRandom(plant, 3, Math.max(540, damage * 3), "plant food bulb");
                }
            }
            case "cactus" -> {
                plant.setPlantFoodModifiers(3, 3, true);
                return true;
            }
            case "fire peashooter" -> {
                if (context != null) {
                    context.damageLane(plant, Math.max(200, damage * 5), "plant food fire");
                }
                plant.setPlantFoodModifiers(1, 5, false);
                return true;
            }
            case "goo peashooter" -> {
                if (context != null) {
                    context.poisonLane(plant, Math.max(20, plant.getDamagePerTick()), DEFAULT_POISON_TICKS);
                }
                plant.setPlantFoodModifiers(1, 5, false);
                return true;
            }
            case "mega gatling pea" -> {
                if (context != null) {
                    context.damageLane(plant, damage * 80, "plant food mega pea");
                }
                plant.setPlantFoodModifiers(2, 8, false);
                return true;
            }
            case "sea shroom", "puff shroom" -> {
                if (context != null) {
                    context.resetPlantAges(plant.getName());
                }
                plant.setPlantFoodModifiers(1, 5, false);
                return true;
            }
            case "fume shroom" -> {
                if (context != null) {
                    context.pushLane(plant, 2.0);
                }
            }
            case "cabbage pult" -> {
                if (context != null) {
                    context.damageRandom(plant, 5, Math.max(40, damage), "plant food cabbage");
                }
            }
            case "kernel pult" -> {
                if (context != null) {
                    context.butterAll(DEFAULT_STUN_TICKS);
                }
            }
            case "melon pult" -> {
                if (context != null) {
                    context.damageRandom(plant, 3, Math.max(240, damage * 3), "plant food melon");
                }
            }
            case "winter melon" -> {
                if (context != null) {
                    context.damageRandom(plant, 3, Math.max(240, damage * 3), "plant food winter melon");
                    context.freezeAll(DEFAULT_FREEZE_TICKS);
                }
            }
            case "pepper pult" -> {
                if (context != null) {
                    context.damageRandom(plant, 3, Math.max(150, damage * 3), "plant food pepper");
                }
            }
            case "potato mine", "primal potato mine" -> {
                plant.finishArming();
                if (context != null) {
                    context.clonePlant(plant, 2);
                }
            }
            case "squash" -> {
                if (context != null) {
                    context.killRandom(plant, 2, "plant food squash");
                }
            }
            case "tangle kelp" -> {
                if (context != null) {
                    context.killRandom(plant, 3, "plant food tangle");
                }
            }
            case "iceberg lettuce" -> {
                if (context != null) {
                    context.freezeAll(DEFAULT_FREEZE_TICKS);
                }
            }
            case "bonk choy" -> {
                if (context != null) {
                    context.damageArea(plant, 1, 1, Math.max(150, damage * 10), "plant food punch");
                }
                plant.setPlantFoodModifiers(1, 5, false);
                return true;
            }
            case "phat beet" -> {
                if (context != null) {
                    context.damageArea(plant, 1, 1, Math.max(300, damage * 20), "plant food sonic");
                }
            }
            case "chomper" -> {
                if (context != null) {
                    context.killRandom(plant, 3, "plant food chomp");
                }
            }
            case "wasabi whip" -> {
                if (context != null) {
                    context.damageArea(plant, 1, 1, Math.max(400, damage * 10), "plant food whip");
                }
            }
            case "kiwibeast" -> {
                if (context != null) {
                    context.damageArea(plant, 1, 1, Math.max(450, damage * 10), "plant food slam");
                }
            }
            case "wall nut" -> plant.addArmor(4000);
            case "tall nut" -> plant.addArmor(8000);
            case "endurian" -> {
                plant.addArmor(3000);
                plant.increaseReflectDamage(Math.max(20, damage));
            }
            case "garlic" -> {
                if (context != null) {
                    context.shiftLaneZombies(plant);
                }
            }
            case "sweet potato" -> {
                if (context != null) {
                    context.attractNearbyZombies(plant);
                }
                plant.healToFull();
            }
            case "explode o nut" -> plant.addArmor(4000);
            case "pumpkin" -> plant.addArmor(4000);
            case "sun bean" -> plant.addArmor(1000);
            case "torchwood" -> plant.enableBlueFlame();
            case "magnet shroom" -> {
                if (context != null) {
                    context.removeArmorFromRandom(5);
                }
            }
            case "hypno shroom" -> {
                plant.enablePlantFoodHypnoGargantuar();
                return true;
            }
            case "lily pad" -> {
                if (context != null) {
                    context.cloneLilyPads(3);
                }
            }
            default -> {
                plant.heal(healAmount);
                plant.setPlantFoodModifiers(2, 2, false);
                return true;
            }
        }
        return false;
    }

    private void addSun(PlantFoodContext context, int amount) {
        if (context != null) {
            context.addSun(amount);
        }
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
