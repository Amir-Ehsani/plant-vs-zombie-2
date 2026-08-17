package models.core.plant;
import models.core.base.GameEntity;

public interface AttackBehavior {
    void attack(GameEntity attacker, GameEntity target);
}