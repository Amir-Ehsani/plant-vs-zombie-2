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

    public ZombieRegistry getZombieRegistry() {
        return zombieRegistry;
    }

    private Armor createArmorFor(ZombieType type) {
        if (type == null || !type.hasDefaultArmor()) {
            return null;
        }

        return zombieRegistry.createArmorByName(type.getDefaultArmorName());
    }

    private MovementStrategy createMovementStrategyFor(ZombieType type) {
        String name = normalize(type.getName());

        if (name.equals("allstar")) {
            return new MovementStrategy() {
                private boolean charged = false;

                @Override
                public void move(Zombie zombie) {
                    if (!charged) {
                        zombie.moveBy(-zombie.getCurrentSpeed() * 3, 0);
                        charged = true;
                        return;
                    }

                    zombie.moveBy(-zombie.getCurrentSpeed(), 0);
                }
            };
        }

        if (name.equals("prospector")) {
            return new MovementStrategy() {
                private boolean entered = false;

                @Override
                public void move(Zombie zombie) {
                    if (!entered) {
                        zombie.moveBy(-zombie.getCurrentSpeed() * 2, 0);
                        entered = true;
                        return;
                    }

                    zombie.moveBy(-zombie.getCurrentSpeed(), 0);
                }
            };
        }

        if (name.equals("dodo")) {
            return zombie -> zombie.moveBy(-zombie.getCurrentSpeed() * 1.5, 0);
        }

        if (name.equals("piano")) {
            return zombie -> zombie.moveBy(-zombie.getCurrentSpeed(), 0);
        }

        return null;
    }

    private ZombieAbility createAbilityFor(ZombieType type) {
        String name = normalize(type.getName());

        if (name.equals("gargantuar")) {
            return zombie -> zombie.setCurrentSpeed(zombie.getCurrentSpeed() * 0.75);
        }

        if (name.equals("imp")) {
            return zombie -> zombie.setCurrentSpeed(zombie.getCurrentSpeed() * 1.25);
        }

        if (name.equals("king")) {
            return zombie -> zombie.heal(50);
        }

        if (name.equals("newspaper") || name.equals("news paper")) {
            return zombie -> {
                if (!zombie.hasArmor()) {
                    zombie.setCurrentSpeed(zombie.getCurrentSpeed() * 1.5);
                }
            };
        }

        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", " ")
                .replace("_", " ")
                .replaceAll("\\s+", " ");
    }
}