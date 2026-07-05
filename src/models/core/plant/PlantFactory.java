package models.core.plant;

import models.core.plant.behaviours.NoAttackBehavior;
import models.core.plant.behaviours.SimpleDamageAttackBehavior;

public class PlantFactory {
    private final PlantRegistry plantRegistry;

    public PlantFactory() {
        this(DefaultPlantRegistry.getInstance());
    }

    public PlantFactory(PlantRegistry plantRegistry) {
        if (plantRegistry == null) {
            throw new IllegalArgumentException("Plant registry cannot be null.");
        }

        this.plantRegistry = plantRegistry;
    }

    public Plant createPlant(PlantType type, double x, double y) {
        if (type == null) {
            throw new IllegalArgumentException("Plant type cannot be null.");
        }

        AttackBehavior attackBehavior = createAttackBehavior(type);
        return new Plant(type, x, y, attackBehavior);
    }

    public Plant createPlant(String plantName, double x, double y) {
        PlantType type = plantRegistry.getByName(plantName);

        if (type == null) {
            throw new IllegalArgumentException("Unknown plant type: " + plantName);
        }

        return createPlant(type, x, y);
    }

    private AttackBehavior createAttackBehavior(PlantType type) {
        String category = type.getCategory();

        if (isNonAttackingCategory(category)) {
            return new NoAttackBehavior();
        }

        return createDamageBehavior(category);
    }

    private boolean isNonAttackingCategory(String category) {
        return category.equals("sun producer")
                || category.equals("wall-nut")
                || category.equals("modifier")
                || category.equals("normal");
    }

    private AttackBehavior createDamageBehavior(String category) {
        if (category.equals("shooter")) {
            return new SimpleDamageAttackBehavior(20, "normal");
        }

        if (category.equals("strike-through")) {
            return new SimpleDamageAttackBehavior(20, "piercing");
        }

        if (category.equals("homing")) {
            return new SimpleDamageAttackBehavior(30, "homing");
        }

        if (category.equals("lobber")) {
            return new SimpleDamageAttackBehavior(40, "lobber");
        }

        if (category.equals("melee")) {
            return new SimpleDamageAttackBehavior(15, "melee");
        }

        if (category.equals("explosive")) {
            return new SimpleDamageAttackBehavior(1800, "explosive");
        }

        return new NoAttackBehavior();
    }
}