package models.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Season {
    private final String name;
    private final String specialFeatures;
    private final List<Level> levels;

    public Season(String name, String specialFeatures) {
        this(name, specialFeatures, new ArrayList<>());
    }

    public Season(String name, String specialFeatures, List<Level> levels) {
        this.name = name;
        this.specialFeatures = specialFeatures;
        this.levels = new ArrayList<>(levels);
    }

    public void addLevel(Level level) {
        if (level == null) {
            throw new IllegalArgumentException("Level cannot be null.");
        }

        levels.add(level);
    }

    public Level getLevel(int levelNumber) {
        if (levelNumber <= 0 || levelNumber > levels.size()) {
            return null;
        }

        return levels.get(levelNumber - 1);
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
}