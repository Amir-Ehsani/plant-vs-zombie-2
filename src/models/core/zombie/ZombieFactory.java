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
        if (type == null || !type.hasDefaultArmor()) {
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
        String name = normalize(type.getName());
        if (name.equals("dodo")) {
            return zombie -> zombie.moveBy(-zombie.getCurrentSpeed() * 1.5, 0);
        }
        return null;
    }

    private ZombieAbility createAbilityFor(ZombieType type) {
        String name = normalize(type.getName());
        if (name.equals("imp")) {
            return zombie -> zombie.setCurrentSpeed(zombie.getCurrentSpeed() * 1.25);
        }
        if (name.equals("news paper") || name.equals("newspaper")) {
            return zombie -> {
                if (!zombie.hasArmor()) {
                    zombie.setCurrentSpeed(zombie.getType().getSpeed() * 2.0);
                }
            };
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }
}
