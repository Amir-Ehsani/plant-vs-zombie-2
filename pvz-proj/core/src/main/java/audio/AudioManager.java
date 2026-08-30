package audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;
import controllers.auth.AuthController;
import models.account.Settings;
import models.account.User;
import models.level.core.Level;
import models.level.core.SeasonType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Small, event-driven audio service for P1-08.
 *
 * Important: it does no work from Main.render() and never scans game state.
 * Short effects are decoded once during startup; music is streamed only when a screen changes.
 */
public final class AudioManager implements Disposable {
    private static AudioManager active;

    private final AuthController authController;
    private final Map<AudioCue, Sound> sounds;
    private final Map<String, Sound> soundCache;
    private final Map<AudioCue, Long> lastPlayedAt;
    private final Map<AudioCue, Boolean> missingLogged;

    private Music currentMusic;
    private AudioCue currentMusicCue;
    private float musicVolume;
    private float soundVolume;
    private boolean musicEnabled;

    public AudioManager(AuthController authController) {
        this.authController = authController;
        sounds = new EnumMap<>(AudioCue.class);
        soundCache = new HashMap<>();
        lastPlayedAt = new EnumMap<>(AudioCue.class);
        missingLogged = new EnumMap<>(AudioCue.class);
        musicVolume = 1f;
        soundVolume = 1f;
        musicEnabled = true;
        active = this;
        refreshSettings();
        preloadEffects();
    }

    public static AudioManager getActive() {
        return active;
    }

    public static void playGlobal(AudioCue cue) {
        AudioManager manager = active;
        if (manager != null) {
            manager.play(cue);
        }
    }

    public void playMenuMusic() {
        playMusic(AudioCue.MENU_MUSIC);
    }

    public void playMiniGameMusic() {
        playMusic(AudioCue.MENU_MUSIC);
    }

    public void playGameplayMusic(Level level) {
        if (level != null && level.getBossRuntime() != null) {
            playMusic(AudioCue.BOSS_MUSIC);
            return;
        }
        SeasonType seasonType = level == null ? null : level.getSeasonType();
        if (seasonType == null) {
            playMusic(AudioCue.MENU_MUSIC);
            return;
        }
        switch (seasonType) {
            case ANCIENT_EGYPT -> playMusic(AudioCue.ANCIENT_EGYPT_MUSIC);
            case FROSTBITE_CAVES -> playMusic(AudioCue.FROSTBITE_CAVES_MUSIC);
            case BIG_WAVE_BEACH -> playMusic(AudioCue.BIG_WAVE_BEACH_MUSIC);
            case DARK_AGES -> playMusic(AudioCue.DARK_AGES_MUSIC);
        }
    }

    public void play(AudioCue cue) {
        if (cue == null) {
            return;
        }
        if (cue.isMusic()) {
            playMusic(cue);
            return;
        }
        refreshSettings();
        if (soundVolume <= 0f || isRateLimited(cue)) {
            return;
        }
        Sound sound = sounds.get(cue);
        if (sound == null) {
            // This normally only happens when an asset was missing during startup and appears later.
            sound = loadEffect(cue);
        }
        if (sound == null) {
            return;
        }
        try {
            sound.play(soundVolume);
            lastPlayedAt.put(cue, TimeUtils.millis());
        } catch (RuntimeException exception) {
            logLoadFailure(cue, exception);
        }
    }

    public void playPlantAttack(String plantName) {
        String normalized = normalize(plantName);
        if (normalized.contains("cabbage") || normalized.contains("melon")
                || normalized.contains("kernel") || normalized.contains("pepper pult")) {
            play(AudioCue.PROJECTILE_THROW);
            return;
        }
        if (normalized.contains("pea") || normalized.contains("repeater")
                || normalized.contains("threepeater") || normalized.contains("gatling")) {
            play(AudioCue.PLANT);
        }
    }

    public void playExplosionForPlant(String plantName) {
        String normalized = normalize(plantName);
        if (normalized.contains("cherry bomb")
                || normalized.contains("grapeshot")
                || normalized.contains("jalapeno")
                || normalized.contains("doom shroom")
                || normalized.contains("explode o nut")
                || normalized.contains("bomb")) {
            play(AudioCue.EXPLOSION);
        }
    }

    /** Apply changed settings immediately without restarting or touching the render loop. */
    public void refreshSettings() {
        Settings settings = currentSettings();
        float nextMusicVolume = settings == null ? 1f : settings.getMusicVolume();
        float nextSoundVolume = settings == null ? 1f : settings.getSoundVolume();
        boolean nextMusicEnabled = settings == null || settings.isMusicEnabled();

        musicVolume = clamp(nextMusicVolume);
        soundVolume = clamp(nextSoundVolume);
        musicEnabled = nextMusicEnabled;

        if (currentMusic == null) {
            return;
        }
        currentMusic.setVolume(musicVolume);
        if (musicEnabled && musicVolume > 0f) {
            if (!currentMusic.isPlaying()) {
                currentMusic.play();
            }
        } else if (currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    private void preloadEffects() {
        for (AudioCue cue : AudioCue.values()) {
            if (!cue.isMusic()) {
                loadEffect(cue);
            }
        }
    }

    private Sound loadEffect(AudioCue cue) {
        if (cue == null || cue.isMusic()) {
            return null;
        }
        Sound existing = sounds.get(cue);
        if (existing != null) {
            return existing;
        }
        FileHandle file = resolve(cue);
        if (file == null) {
            logMissingOnce(cue);
            return null;
        }
        String cacheKey = file.path();
        Sound shared = soundCache.get(cacheKey);
        if (shared != null) {
            sounds.put(cue, shared);
            return shared;
        }
        try {
            Sound sound = Gdx.audio.newSound(file);
            soundCache.put(cacheKey, sound);
            sounds.put(cue, sound);
            return sound;
        } catch (RuntimeException exception) {
            logLoadFailure(cue, exception);
            return null;
        }
    }

    private void playMusic(AudioCue cue) {
        if (cue == null || !cue.isMusic()) {
            return;
        }
        refreshSettings();
        if (cue == currentMusicCue && currentMusic != null) {
            currentMusic.setVolume(musicVolume);
            if (musicEnabled && musicVolume > 0f && !currentMusic.isPlaying()) {
                currentMusic.play();
            }
            return;
        }

        FileHandle file = resolve(cue);
        if (file == null) {
            logMissingOnce(cue);
            return;
        }

        Music nextMusic;
        try {
            nextMusic = Gdx.audio.newMusic(file);
            nextMusic.setLooping(true);
            nextMusic.setVolume(musicVolume);
        } catch (RuntimeException exception) {
            logLoadFailure(cue, exception);
            return;
        }

        disposeCurrentMusic();
        currentMusic = nextMusic;
        currentMusicCue = cue;
        if (musicEnabled && musicVolume > 0f) {
            currentMusic.play();
        }
    }

    private Settings currentSettings() {
        if (authController == null) {
            return null;
        }
        User user = authController.getLoggedInUser();
        return user == null ? null : user.getSettings();
    }

    private boolean isRateLimited(AudioCue cue) {
        long interval = cue.getMinimumIntervalMs();
        if (interval <= 0L) {
            return false;
        }
        Long last = lastPlayedAt.get(cue);
        return last != null && TimeUtils.timeSinceMillis(last) < interval;
    }

    private FileHandle resolve(AudioCue cue) {
        for (String path : cue.getCandidates()) {
            if (path == null || path.isBlank()) {
                continue;
            }
            // Correct LibGDX path for <project>/assets/pvz audio/...
            FileHandle internal = Gdx.files.internal(path);
            if (internal.exists() && !internal.isDirectory()) {
                return internal;
            }
            // Fallbacks make the same build work from IntelliJ and from a different desktop cwd.
            FileHandle localAssets = Gdx.files.local("assets/" + path);
            if (localAssets.exists() && !localAssets.isDirectory()) {
                return localAssets;
            }
            FileHandle localDirect = Gdx.files.local(path);
            if (localDirect.exists() && !localDirect.isDirectory()) {
                return localDirect;
            }
        }
        return null;
    }

    private void logMissingOnce(AudioCue cue) {
        if (Boolean.TRUE.equals(missingLogged.put(cue, true))) {
            return;
        }
        Gdx.app.log("AudioManager", "Missing audio asset for " + cue
                + ". Expected under assets/pvz audio/.");
    }

    private void logLoadFailure(AudioCue cue, RuntimeException exception) {
        Gdx.app.error("AudioManager", "Could not play audio cue " + cue + ".", exception);
    }

    private void disposeCurrentMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic.dispose();
            currentMusic = null;
        }
        currentMusicCue = null;
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase()
                .replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }

    @Override
    public void dispose() {
        disposeCurrentMusic();
        for (Sound sound : soundCache.values()) {
            if (sound != null) {
                sound.dispose();
            }
        }
        soundCache.clear();
        sounds.clear();
        lastPlayedAt.clear();
        if (active == this) {
            active = null;
        }
    }
}
