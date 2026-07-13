package models.core.plant.behaviours;

import models.core.base.GameEntity;
import models.core.plant.AttackBehavior;

public class NoAttackBehavior implements AttackBehavior {
    @Override
    public void attack(GameEntity attacker, GameEntity target) {
    }
}