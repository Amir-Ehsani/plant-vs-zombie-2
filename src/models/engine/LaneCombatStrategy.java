package models.engine;

public interface LaneCombatStrategy {
    LaneTickResult updateLane(Lane lane);
}
