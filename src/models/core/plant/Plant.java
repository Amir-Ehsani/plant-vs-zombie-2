package plant;

import base.GameEntity;

public class Plant extends GameEntity {
    private int level;
    private int cooldownRemaining;
    private boolean isBoosted;
    private AttackBehavior attackBehavior;
    private PlantType type;

    public void attack() {
    }

    public void usePlantFood(PlantFood food) {
    }

    public void upgrade(PlantUpgrade upgrade) {
    }

    public void tickCooldown() {
    }

    public void takeDamage(Damage damage) {
    }

    public boolean isAlive() {
        return false;
    }

    public double getX() {
        return 0;
    }

    public double getY() {
        return 0;
    }
}