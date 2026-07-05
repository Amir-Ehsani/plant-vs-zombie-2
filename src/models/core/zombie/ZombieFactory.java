package models.core.zombie;

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

        Armor armor = createArmorFor(type);
        MovementStrategy movementStrategy = createMovementStrategyFor(type);
        ZombieAbility zombieAbility = createAbilityFor(type);

        return new Zombie(type, x, y, armor, movementStrategy, zombieAbility);
    }

    public Zombie createZombie(String zombieName, double x, double y) {
        ZombieType type = zombieRegistry.getZombieTypeByName(zombieName);

        if (type == null) {
            throw new IllegalArgumentException("Unknown zombie type: " + zombieName);
        }

        return createZombie(type, x, y);
    }

    private Armor createArmorFor(ZombieType type) {
        if (!type.hasDefaultArmor()) {
            return null;
        }

        return zombieRegistry.createArmorByName(type.getDefaultArmorName());
    }

    private MovementStrategy createMovementStrategyFor(ZombieType type) {
        return null;
    }

    private ZombieAbility createAbilityFor(ZombieType type) {
        return null;
    }
}