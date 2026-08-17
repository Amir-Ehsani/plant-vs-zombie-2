package models.account;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


abstract class UserProgress extends UserIdentity {
    protected UserProgress(String username, String password, String nickname, String email, String gender) {
        super(username, password, nickname, email, gender);
    }

    public String getCurrentChapterName() {
        return currentChapterName;
    }

    public void setCurrentChapterName(String currentChapterName) {
        this.currentChapterName = safeText(currentChapterName);
    }

    public List<String> getUnlockedChapters() {
        if (unlockedChapters == null) {
            unlockedChapters = new ArrayList<>();
        }

        return new ArrayList<>(unlockedChapters);
    }

    public void setUnlockedChapters(List<String> unlockedChapters) {
        this.unlockedChapters = new ArrayList<>();

        if (unlockedChapters != null) {
            for (String chapterName : unlockedChapters) {
                unlockChapter(chapterName);
            }
        }

        if (this.unlockedChapters.isEmpty()) {
            unlockChapter("ancient-egypt");
        }
    }

    public void unlockChapter(String chapterName) {
        if (unlockedChapters == null) {
            unlockedChapters = new ArrayList<>();
        }

        String cleanedChapterName = safeText(chapterName);

        if (cleanedChapterName.isEmpty()) {
            return;
        }

        if (!unlockedChapters.contains(cleanedChapterName)) {
            unlockedChapters.add(cleanedChapterName);
        }
    }

    public boolean isChapterUnlocked(String chapterName) {
        String cleanedChapterName = safeText(chapterName);

        for (String unlockedChapter : getUnlockedChapters()) {
            if (unlockedChapter.equalsIgnoreCase(cleanedChapterName)) {
                return true;
            }
        }

        return false;
    }

    public int getCurrentChapterLevel() {
        if (currentChapterLevel < 1 || currentChapterLevel > 4) {
            currentChapterLevel = 1;
        }
        return currentChapterLevel;
    }

    public void setCurrentChapterLevel(int currentChapterLevel) {
        if (currentChapterLevel < 1) {
            this.currentChapterLevel = 1;
            return;
        }
        this.currentChapterLevel = Math.min(4, currentChapterLevel);
    }

    public List<String> getCompletedChapterLevels() {
        if (completedChapterLevels == null) {
            completedChapterLevels = new ArrayList<>();
        }
        return new ArrayList<>(completedChapterLevels);
    }

    public void setCompletedChapterLevels(List<String> completedChapterLevels) {
        this.completedChapterLevels = new ArrayList<>();
        if (completedChapterLevels == null) {
            return;
        }

        for (String levelKey : completedChapterLevels) {
            String normalizedKey = normalizeChapterLevelKey(levelKey);
            if (!normalizedKey.isEmpty() && !this.completedChapterLevels.contains(normalizedKey)) {
                this.completedChapterLevels.add(normalizedKey);
            }
        }
    }

    public boolean completeChapterLevel(String chapterName, int levelNumber) {
        if (levelNumber < 1 || levelNumber > 3) {
            return false;
        }
        if (completedChapterLevels == null) {
            completedChapterLevels = new ArrayList<>();
        }

        String key = chapterLevelKey(chapterName, levelNumber);
        if (key.isEmpty() || completedChapterLevels.contains(key)) {
            return false;
        }
        completedChapterLevels.add(key);
        return true;
    }

    public boolean isChapterLevelCompleted(String chapterName, int levelNumber) {
        if (levelNumber < 1 || levelNumber > 3) {
            return false;
        }
        return getCompletedChapterLevels().contains(chapterLevelKey(chapterName, levelNumber));
    }

    public boolean isChapterLevelUnlocked(String chapterName, int levelNumber) {
        if (levelNumber < 1 || levelNumber > 4 || !isChapterUnlocked(chapterName)) {
            return false;
        }
        if (allAdventureLevelsUnlocked) {
            return true;
        }
        if (levelNumber == 1) {
            return true;
        }
        return isChapterLevelCompleted(chapterName, levelNumber - 1);
    }

    public boolean isAllAdventureLevelsUnlocked() {
        return allAdventureLevelsUnlocked;
    }

    public void setAllAdventureLevelsUnlocked(boolean allAdventureLevelsUnlocked) {
        this.allAdventureLevelsUnlocked = allAdventureLevelsUnlocked;
        if (allAdventureLevelsUnlocked) {
            unlockAllAdventureChapters();
        }
    }

    public void unlockAllAdventureLevels() {
        allAdventureLevelsUnlocked = true;
        unlockAllAdventureChapters();
    }

    private void unlockAllAdventureChapters() {
        unlockChapter("ancient-egypt");
        unlockChapter("ice-cave");
        unlockChapter("wave-beach");
        unlockChapter("wild-west");
    }

    public List<String> getCompletedMiniGameStages() {
        if (completedMiniGameStages == null) {
            completedMiniGameStages = new ArrayList<>();
        }

        return new ArrayList<>(completedMiniGameStages);
    }

    public void setCompletedMiniGameStages(List<String> completedMiniGameStages) {
        this.completedMiniGameStages = new ArrayList<>();
        this.completedChapterLevels = new ArrayList<>();
        this.currentChapterLevel = 1;
        this.allAdventureLevelsUnlocked = false;

        if (completedMiniGameStages == null) {
            return;
        }

        for (String stageKey : completedMiniGameStages) {
            String normalizedKey = normalizeMiniGameStageKey(stageKey);

            if (!normalizedKey.isEmpty() && !this.completedMiniGameStages.contains(normalizedKey)) {
                this.completedMiniGameStages.add(normalizedKey);
            }
        }
    }

    public boolean completeMiniGameStage(String miniGameName, int stage) {
        if (stage < 1 || stage > 3) {
            return false;
        }

        if (completedMiniGameStages == null) {
            completedMiniGameStages = new ArrayList<>();
        }

        String stageKey = miniGameStageKey(miniGameName, stage);

        if (stageKey.isEmpty() || completedMiniGameStages.contains(stageKey)) {
            return false;
        }

        completedMiniGameStages.add(stageKey);
        return true;
    }

    public boolean isMiniGameStageCompleted(String miniGameName, int stage) {
        if (stage < 1 || stage > 3) {
            return false;
        }

        if (completedMiniGameStages == null) {
            completedMiniGameStages = new ArrayList<>();
        }

        return completedMiniGameStages.contains(miniGameStageKey(miniGameName, stage));
    }

    public boolean isMiniGameStageUnlocked(String miniGameName, int stage) {
        if (stage < 1 || stage > 3) {
            return false;
        }

        return stage == 1 || isMiniGameStageCompleted(miniGameName, stage - 1);
    }

    public int getCompletedMiniGameStageCount() {
        if (completedMiniGameStages == null) {
            return 0;
        }

        return completedMiniGameStages.size();
    }

    protected String miniGameStageKey(String miniGameName, int stage) {
        String normalizedName = normalizeMiniGameName(miniGameName);

        if (normalizedName.isEmpty()) {
            return "";
        }

        return normalizedName + ":" + stage;
    }

    protected String normalizeMiniGameStageKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        int separatorIndex = value.lastIndexOf(':');

        if (separatorIndex <= 0 || separatorIndex >= value.length() - 1) {
            return "";
        }

        String name = value.substring(0, separatorIndex);
        String stageText = value.substring(separatorIndex + 1);

        try {
            int stage = Integer.parseInt(stageText.trim());
            return stage < 1 || stage > 3 ? "" : miniGameStageKey(name, stage);
        } catch (NumberFormatException exception) {
            return "";
        }
    }

    protected String normalizeMiniGameName(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim()
                .toLowerCase()
                .replace('_', '-')
                .replace(' ', '-')
                .replace(",", "")
                .replaceAll("-+", "-");

        if ("wall-nut-bowling".equals(normalized)
                || "wallnutbowling".equals(normalized)
                || "bowling".equals(normalized)) {
            return "wallnut-bowling";
        }

        if ("i-zombie".equals(normalized)
                || "izombie".equals(normalized)
                || "i--zombie".equals(normalized)) {
            return "i-zombie";
        }

        if ("vase-breaker".equals(normalized)) {
            return "vasebreaker";
        }

        return normalized;
    }

    protected String chapterLevelKey(String chapterName, int levelNumber) {
        String normalizedChapter = normalizeChapterName(chapterName);
        if (normalizedChapter.isEmpty() || levelNumber < 1 || levelNumber > 3) {
            return "";
        }
        return normalizedChapter + ":" + levelNumber;
    }

    protected String normalizeChapterLevelKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int separatorIndex = value.lastIndexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= value.length() - 1) {
            return "";
        }
        try {
            int levelNumber = Integer.parseInt(value.substring(separatorIndex + 1).trim());
            return chapterLevelKey(value.substring(0, separatorIndex), levelNumber);
        } catch (NumberFormatException exception) {
            return "";
        }
    }

    protected String normalizeChapterName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-')
                .replaceAll("-+", "-");
    }



}
