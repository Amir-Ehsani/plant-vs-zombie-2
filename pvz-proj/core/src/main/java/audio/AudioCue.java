package audio;

/**
 * Audio cues used by the P1-08 beauty layer.
 * Long tracks are streamed as Music; short effects are preloaded as Sound once at startup.
 */
public enum AudioCue {
    BUTTON(false, 55L,
            "pvz audio/PLANT_CABBAGEPULT_hit.mp3"),
    PLANT(false, 100L,
            "pvz audio/PLANT_REPEATER_1.mp3"),
    PROJECTILE_THROW(false, 75L,
            "pvz audio/PLANT_CABBAGEPULT_throwing.mp3"),
    PROJECTILE_HIT(false, 75L,
            "pvz audio/PLANT_CABBAGEPULT_hit.mp3"),
    EXPLOSION(false, 160L,
            "pvz audio/explosion audio.mp3"),
    SUN_PICKUP(false, 55L,
            "pvz audio/SUN_PICKUP.mp3"),
    PURCHASE(false, 100L,
            "pvz audio/SUN_PICKUP.mp3"),
    ZOMBIE(false, 550L,
            "pvz audio/zombies audio.mp3"),
    ZOMBIES_COMING(false, 1100L,
            "pvz audio/zombies are comming.mp3",
            "pvz audio/zombies are coming.mp3"),
    SANDSTORM(false, 1100L,
            "pvz audio/ZOMBIE_EGYPT_SANDSTORM_2.mp3"),
    LAWN_MOWER(false, 450L,
            "pvz audio/lownmower.mp3",
            "pvz audio/lawnmower.mp3"),
    WIN(false, 1200L,
            "pvz audio/win audio.mp3"),
    LOSE(false, 1200L,
            "pvz audio/loss audio.mp3"),

    MENU_MUSIC(true, 0L,
            "pvz audio/menu background audio.mp3"),
    ANCIENT_EGYPT_MUSIC(true, 0L,
            "pvz audio/ancient egypt chapter.mp3"),
    FROSTBITE_CAVES_MUSIC(true, 0L,
            "pvz audio/frostbite caves chapter.mp3"),
    BIG_WAVE_BEACH_MUSIC(true, 0L,
            "pvz audio/big wave beach chapter.mp3"),
    DARK_AGES_MUSIC(true, 0L,
            "pvz audio/dark ages chapter.mp3"),
    BOSS_MUSIC(true, 0L,
            "pvz audio/zomboss levels.mp3");

    private final boolean music;
    private final long minimumIntervalMs;
    private final String[] candidates;

    AudioCue(boolean music, long minimumIntervalMs, String... candidates) {
        this.music = music;
        this.minimumIntervalMs = Math.max(0L, minimumIntervalMs);
        this.candidates = candidates == null ? new String[0] : candidates.clone();
    }

    public boolean isMusic() {
        return music;
    }

    public long getMinimumIntervalMs() {
        return minimumIntervalMs;
    }

    public String[] getCandidates() {
        return candidates.clone();
    }
}
