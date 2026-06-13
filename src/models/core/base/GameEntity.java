package base;

import projectile.Damage;

public abstract class GameEntity {
    protected String id;
    protected int hp;
    protected int maxHp;
    protected double x;
    protected double y;

    public abstract void takeDamage(Damage damage);
    public abstract boolean isAlive();
    public abstract double getX();
    public abstract double getY();
}