package zombie;

import base.GameEntity;

public class Zombie extends GameEntity {
    private double currentSpeed;
    private boolean isGlowing;
    private MovementStrategy movementStrategy;
    private ZombieAbility zombieAbility;
    private ZombieType type;
    private Armor armor;

    public void move() {
    }

    public void attack(GameEntity target) {
    }

    public void checkDropOnDeath() {
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