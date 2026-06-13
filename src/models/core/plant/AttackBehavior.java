package plant;

import base.GameEntity;

public interface AttackBehavior {
    void attack(GameEntity attacker, GameEntity target);
}