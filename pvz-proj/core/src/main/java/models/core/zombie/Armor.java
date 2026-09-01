package models.core.zombie;

public class Armor {
    private String name;
    private int hp;
    private String armorType;

    public Armor() {
        this("basic", 0, "normal");
    }

    public Armor(String name, int hp, String armorType) {
        this.name = name == null || name.isBlank() ? "basic" : name;
        this.hp = Math.max(0, hp);
        this.armorType = armorType == null || armorType.isBlank() ? "normal" : armorType;
    }

    public int reduceDamage(int amount) {
        if (amount <= 0) {
            return 0;
        }

        int absorbed = Math.min(hp, amount);
        hp -= absorbed;

        return amount - absorbed;
    }

    public int getHp() {
        return hp;
    }

    public String getName() {
        return name;
    }

    public String getArmorType() {
        return armorType;
    }

    public boolean isBroken() {
        return hp <= 0;
    }
}
