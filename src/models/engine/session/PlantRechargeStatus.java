package models.engine.session;

public final class PlantRechargeStatus {
    private final String plantName;
    private final int sunCost;
    private final int remainingTicks;
    private final boolean plantable;
    private final boolean selected;
    private final boolean allowedByLevel;
    private final boolean enoughSun;

    public PlantRechargeStatus(
            String plantName,
            int sunCost,
            int remainingTicks,
            boolean plantable,
            boolean selected,
            boolean allowedByLevel,
            boolean enoughSun
    ) {
        this.plantName = plantName;
        this.sunCost = Math.max(0, sunCost);
        this.remainingTicks = Math.max(0, remainingTicks);
        this.plantable = plantable;
        this.selected = selected;
        this.allowedByLevel = allowedByLevel;
        this.enoughSun = enoughSun;
    }

    public String getPlantName() {
        return plantName;
    }

    public int getSunCost() {
        return sunCost;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public boolean isPlantable() {
        return plantable;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isAllowedByLevel() {
        return allowedByLevel;
    }

    public boolean hasEnoughSun() {
        return enoughSun;
    }
}
