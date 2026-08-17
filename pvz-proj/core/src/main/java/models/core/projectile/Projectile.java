package models.core.projectile;

import models.core.base.GameEntity;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;


public class Projectile extends GameEntity {
    private static final double DEFAULT_SPEED = 1.0;
    private static final double DEFAULT_DIRECTION_X = 1.0;
    private static final double DEFAULT_DIRECTION_Y = 0.0;
    private static final int DEFAULT_HP = 1;

    private double speed;
    private double directionX;
    private double directionY;
    private Damage damage;
    private Effect effect;
    private boolean alive;
    private int remainingPierces;
    private int remainingBounces;
    private final Set<GameEntity> hitEntities;

    public Projectile() {
        this(0, 0, DEFAULT_SPEED, DEFAULT_DIRECTION_X, DEFAULT_DIRECTION_Y,
                new Damage(20, "normal"), null);
    }

    public Projectile(double x, double y, Damage damage) {
        this(x, y, DEFAULT_SPEED, DEFAULT_DIRECTION_X, DEFAULT_DIRECTION_Y, damage, null);
    }

    public Projectile(double x, double y, double speed, double directionX,
                      double directionY, Damage damage) {
        this(x, y, speed, directionX, directionY, damage, null);
    }

    public Projectile(double x, double y, double speed, double directionX,
                      double directionY, Damage damage, Effect effect) {
        this.x = x;
        this.y = y;
        this.speed = Math.max(0, speed);
        this.damage = damage == null ? new Damage() : damage;
        this.effect = effect;
        this.hp = DEFAULT_HP;
        this.maxHp = DEFAULT_HP;
        this.alive = true;
        this.remainingPierces = 0;
        this.remainingBounces = 0;
        this.hitEntities = Collections.newSetFromMap(new IdentityHashMap<>());
        this.id = "projectile@" + x + "," + y;
        setDirection(directionX, directionY);
    }

    public void move() {
        if (!isAlive()) {
            return;
        }
        x += directionX * speed;
        y += directionY * speed;
    }

    public void onCollision(GameEntity entity) {
        if (!isAlive() || entity == null || !entity.isAlive() || hitEntities.contains(entity)) {
            return;
        }
        hitEntities.add(entity);
        if (damage.getAmount() > 0) {
            entity.takeDamage(damage);
        }
        if (effect != null) {
            effect.applyEffect(entity);
        }

        if (remainingPierces > 0) {
            remainingPierces--;
            return;
        }
        if (remainingBounces > 0) {
            remainingBounces--;
            directionX = -directionX;
            directionY = -directionY;
            hitEntities.clear();
            return;
        }
        destroy();
    }

    @Override
    public void takeDamage(Damage incomingDamage) {
        if (incomingDamage == null || incomingDamage.getAmount() <= 0 || !isAlive()) {
            return;
        }
        hp -= incomingDamage.getAmount();
        if (hp <= 0) {
            destroy();
        }
    }

    @Override
    public boolean isAlive() {
        return alive && hp > 0;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    public double getSpeed() {
        return speed;
    }

    public double getDirectionX() {
        return directionX;
    }

    public double getDirectionY() {
        return directionY;
    }

    public Damage getDamage() {
        return damage;
    }

    public Effect getEffect() {
        return effect;
    }

    public int getRemainingPierces() {
        return remainingPierces;
    }

    public int getRemainingBounces() {
        return remainingBounces;
    }

    public void setSpeed(double speed) {
        this.speed = Math.max(0, speed);
    }

    public void setDamage(Damage damage) {
        this.damage = damage == null ? new Damage() : damage;
    }

    public void setEffect(Effect effect) {
        this.effect = effect;
    }


    public void setPierceCount(int pierceCount) {
        remainingPierces = Math.max(0, pierceCount);
    }

    public void setBounceCount(int bounceCount) {
        remainingBounces = Math.max(0, bounceCount);
    }

    public void destroy() {
        alive = false;
        hp = 0;
    }

    private void setDirection(double directionX, double directionY) {
        double length = Math.sqrt(directionX * directionX + directionY * directionY);
        if (length == 0) {
            this.directionX = DEFAULT_DIRECTION_X;
            this.directionY = DEFAULT_DIRECTION_Y;
            return;
        }
        this.directionX = directionX / length;
        this.directionY = directionY / length;
    }
}
