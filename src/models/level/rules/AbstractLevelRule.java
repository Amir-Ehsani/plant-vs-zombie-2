package models.level.rules;

public abstract class AbstractLevelRule implements LevelRule {
    private boolean won;
    private boolean lost;

    protected AbstractLevelRule() {
        this.won = false;
        this.lost = false;
    }

    protected void markWon() {
        won = true;
    }

    protected void markLost() {
        lost = true;
    }

    protected void resetResult() {
        won = false;
        lost = false;
    }

    @Override
    public boolean isWinConditionMet(LevelRuntimeContext context) {
        return won;
    }

    @Override
    public boolean isLoseConditionMet(LevelRuntimeContext context) {
        return lost;
    }
}
