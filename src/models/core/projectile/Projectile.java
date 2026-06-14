package models.core.projectile;


import models.core.base.GameEntity;

public class Projectile extends GameEntity {
    private double speed;
    private double directionX;
    private double directionY;

    public void move() {
    }

    public void onCollision(GameEntity entity) {
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