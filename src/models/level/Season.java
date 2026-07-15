package models.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Season {
    private final SeasonType seasonType;
    private final String name;
    private final String specialFeatures;
    private final List<Level> levels;

    public Season(SeasonType seasonType, String specialFeatures) {
        this(seasonType, seasonType == null ? null : seasonType.getDisplayName(), specialFeatures, new ArrayList<>());
    }


    private Season(SeasonType seasonType, String name, String specialFeatures, List<Level> levels) {

        this.seasonType = seasonType;
        this.name = name.trim();
        this.specialFeatures = specialFeatures.trim();
        this.levels = new ArrayList<>();

        for (Level level : levels) {
            addLevel(level);
        }
    }

    public void addLevel(Level level) {
        if (level == null) {
            throw new IllegalArgumentException("Level cannot be null.");
        }
        validateLevelZombies(level);
        levels.add(level);
    }

    public Level getLevel(int levelNumber) {
        if (levelNumber <= 0 || levelNumber > levels.size()) {
            return null;
        }
        return levels.get(levelNumber - 1);
    }

    public SeasonType getSeasonType() {
        return seasonType;
    }

    public String getName() {
        return name;
    }

    public String getSpecialFeatures() {
        return specialFeatures;
    }

    public int getLevelCount() {
        return levels.size();
    }

    public List<Level> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    public List<String> getAllowedZombieNames() {
        return seasonType.getAllowedZombieNames();
    }

    public boolean isZombieAllowed(String zombieName) {
        return seasonType.isZombieAllowed(zombieName);
    }

    private void validateLevelZombies(Level level) {
        for (String zombieName : level.getAllowedZombieNames()) {
            if (!isZombieAllowed(zombieName)) {
                throw new IllegalArgumentException(
                        "Zombie type " + zombieName + " is not allowed in " + name + "."
                );
            }
        }
    }

    private static SeasonType resolveSeasonType(String name) {
        return SeasonType.fromName(name);
    }
}
