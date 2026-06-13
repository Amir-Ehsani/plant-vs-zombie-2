package Model;

import java.util.List;

public class Level {
    private int levelId;
    private WaveManager waveManager;
    private String levelType;
    private List<String> allowedPlants;
    private List<String> attackPatterns;
    private String specialRules;

    public void startLevel() {
    }

    public boolean checkWinCondition() {
        return false;
    }

    public void applySpecialRules() {
    }
}
