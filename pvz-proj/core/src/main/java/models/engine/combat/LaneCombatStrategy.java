package models.engine.combat;

import models.engine.board.Lane;

public interface LaneCombatStrategy {
    LaneTickResult updateLane(Lane lane);
}
