package models.core.zombie;

public class ZombieType {
    private String name;
    private int baseHp;
    private double speed;
    private int damagePerTick;
    private int waveCost;

    public ZombieType() {
        this("normal zombie", 100, 0.25, 10, 1);
    }

    public ZombieType(String name, int baseHp, double speed, int damagePerTick, int waveCost) {
        this.name = name == null || name.isBlank() ? "normal zombie" : name;
        this.baseHp = Math.max(1, baseHp);
        this.speed = Math.max(0, speed);
        this.damagePerTick = Math.max(0, damagePerTick);
        this.waveCost = Math.max(0, waveCost);
    }

    public String getName() {
        return name;
    }

    public int getBaseHp() {
        return baseHp;
    }

    public double getSpeed() {
        return speed;
    }

    public int getDamagePerTick() {
        return damagePerTick;
    }

    public int getWaveCost() {
        return waveCost;
    }
}