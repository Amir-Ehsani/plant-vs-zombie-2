package models.level.rules;

public class NoSpecialRule extends AbstractLevelRule {
    @Override
    public SpecialLevelType getType() {
        return SpecialLevelType.NONE;
    }

    @Override
    public void onLevelStart(LevelRuntimeContext context) {
        resetResult();
    }

    @Override
    public void onTick(LevelRuntimeContext context) {
    }
}
