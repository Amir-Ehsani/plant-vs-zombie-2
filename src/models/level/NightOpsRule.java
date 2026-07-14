package models.level;

public class NightOpsRule extends AbstractLevelRule {
    @Override
    public SpecialLevelType getType() {
        return SpecialLevelType.NIGHT_OPS;
    }

    @Override
    public void onLevelStart(LevelRuntimeContext context) {
        resetResult();
    }

    @Override
    public void onTick(LevelRuntimeContext context) {
    }

    @Override
    public boolean allowsSkySun() {
        return false;
    }
}
