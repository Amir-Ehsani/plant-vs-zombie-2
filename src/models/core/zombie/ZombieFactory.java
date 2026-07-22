package models.core.zombie;

import java.util.Locale;

public class ZombieFactory {
    private final ZombieRegistry zombieRegistry;

    public ZombieFactory() {
        this(DefaultZombieRegistry.getInstance());
    }

    public ZombieFactory(ZombieRegistry zombieRegistry) {
        if (zombieRegistry == null) {
            throw new IllegalArgumentException("Zombie registry cannot be null.");
        }
        this.zombieRegistry = zombieRegistry;
    }

    public Zombie createZombie(ZombieType type, double x, double y) {
        if (type == null) {
            throw new IllegalArgumentException("Zombie type cannot be null.");
        }
        return new Zombie(
                type,
                x,
                y,
                createArmorFor(type),
                createMovementStrategyFor(type),
                createAbilityFor(type)
        );
    }

    public Zombie createZombie(String zombieName, double x, double y) {
        ZombieType type = zombieRegistry.getZombieTypeByName(zombieName);
        if (type == null) {
            throw new IllegalArgumentException("Unknown zombie type: " + zombieName);
        }
        return createZombie(type, x, y);
    }

    public ZombieRegistry getZombieRegistry() {
        return zombieRegistry;
    }

    private Armor createArmorFor(ZombieType type) {
        if (type == null) {
            return null;
        }
        String normalizedName = normalize(type.getName());
        if (normalizedName.equals("arcade")) {
            return new Armor("Arcade Machine", 1100, "normal");
        }
        if (normalizedName.equals("barrel roller")) {
            return new Armor("Barrel", 1100, "normal");
        }
        if (!type.hasDefaultArmor()) {
            return null;
        }
        String armorName = type.getDefaultArmorName();
        if (!armorName.contains("+")) {
            return zombieRegistry.createArmorByName(armorName);
        }

        int totalHp = 0;
        boolean metallic = false;
        for (String part : armorName.split("\\+")) {
            Armor armor = zombieRegistry.createArmorByName(part.trim());
            if (armor == null) {
                continue;
            }
            totalHp += armor.getHp();
            metallic |= "metallic".equalsIgnoreCase(armor.getArmorType());
        }
        return totalHp == 0 ? null : new Armor(armorName, totalHp, metallic ? "metallic" : "normal");
    }

    private MovementStrategy createMovementStrategyFor(ZombieType type) {
        if (type != null && type.hasTag("stationary")) {
            return zombie -> {
            };
        }
        return null;
    }

    private ZombieAbility createAbilityFor(ZombieType type) {
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }
}
