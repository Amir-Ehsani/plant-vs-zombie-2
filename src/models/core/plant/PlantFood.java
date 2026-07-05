package models.core.plant;

public class PlantFood {
    private static final int DEFAULT_DURATION = 50;
    private static final int DEFAULT_HEAL_AMOUNT = 100;

    private final int duration;
    private final int healAmount;
    private final int cooldownReduction;
    private int remainingDuration;
    private Plant boostedPlant;

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
    }

    public void activateBoost(Plant plant) {
        if (plant == null || !plant.isAlive()) {
            return;
        }

        boostedPlant = plant;
        remainingDuration = duration;

        plant.setBoosted(true);
        plant.heal(healAmount);
        reducePlantCooldown(plant);
    }

    public void tick() {
        if (!isActive()) {
            return;
        }

        remainingDuration--;

        if (remainingDuration == 0) {
            boostedPlant.setBoosted(false);
            boostedPlant = null;
        }
    }

    public boolean isActive() {
        return boostedPlant != null
                && boostedPlant.isAlive()
                && remainingDuration > 0;
    }

    public boolean isExpired() {
        return remainingDuration <= 0;
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

    private void reducePlantCooldown(Plant plant) {
        if (cooldownReduction >= plant.getCooldownRemaining()) {
            plant.resetCooldown();
            return;
        }

        plant.reduceCooldown(cooldownReduction);
    }
}