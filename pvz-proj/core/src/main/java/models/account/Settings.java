package models.account;

public class Settings {
    public static final int MIN_DIFFICULTY = 1;
    public static final int MAX_DIFFICULTY = 5;
    public static final int MIN_GAME_SPEED = 1;
    public static final int MAX_GAME_SPEED = 3;

    private int difficulty;
    private int gameSpeed;
    private boolean showGrid;
    private boolean debugMode;
    private float musicVolume;
    private float soundVolume;
    private boolean musicEnabled;

    public Settings() {
        difficulty = 3;
        gameSpeed = 1;
        showGrid = false;
        debugMode = false;
        musicVolume = 1f;
        soundVolume = 1f;
        musicEnabled = true;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = clamp(difficulty, MIN_DIFFICULTY, MAX_DIFFICULTY);
    }

    public int getGameSpeed() {
        return gameSpeed;
    }

    public void setGameSpeed(int gameSpeed) {
        this.gameSpeed = clamp(gameSpeed, MIN_GAME_SPEED, MAX_GAME_SPEED);
    }

    public boolean isGridVisible() {
        return showGrid;
    }

    public void setGridVisible(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = clampVolume(musicVolume);
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = clampVolume(soundVolume);
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void setMusicEnabled(boolean musicEnabled) {
        this.musicEnabled = musicEnabled;
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private float clampVolume(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
