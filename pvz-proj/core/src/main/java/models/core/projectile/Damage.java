package models.core.projectile;

public class Damage {
    private int amount;
    private String type;

    public Damage() {
        this(0, "normal");
    }

    public Damage(int amount, String type) {
        this.amount = Math.max(0, amount);
        this.type = type == null || type.isBlank() ? "normal" : type;
    }

    public int getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }
}