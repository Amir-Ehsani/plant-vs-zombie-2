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
 * P1-08 audio service.
 *
 * All audio handles are created once during application startup. Gameplay only calls play/stop on
 * already-created handles, so entering a level or spawning projectiles/zombies never decodes MP3s
 * and never disposes/recreates a Music object on the gameplay thread.
 */
public final class AudioManager implements Disposable {
    private static AudioManager active;

    private final AuthController authController;
    private final Map<AudioCue, Sound> sounds;
    private final Map<String, Sound> soundCache;
    private final Map<AudioCue, Music> musicTracks;
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
        musicTracks = new EnumMap<>(AudioCue.class);
        lastPlayedAt = new EnumMap<>(AudioCue.class);
        missingLogged = new EnumMap<>(AudioCue.class);
        musicVolume = 1f;
        soundVolume = 1f;
        musicEnabled = true;
        active = this;
        refreshSettings();
        preloadAllAudio();
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

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
        }
        currentMusic = null;
        currentMusicCue = null;
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
        if (soundVolume <= 0f || isRateLimited(cue)) {
            return;
        }

        // Never allocate/decode audio from gameplay. Missing files simply stay silent.
        Sound sound = sounds.get(cue);
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

    /** Applies Settings immediately; no restart is required. */
    public void refreshSettings() {
        Settings settings = currentSettings();
        musicVolume = clamp(settings == null ? 1f : settings.getMusicVolume());
        soundVolume = clamp(settings == null ? 1f : settings.getSoundVolume());
        musicEnabled = settings == null || settings.isMusicEnabled();

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

    private void preloadAllAudio() {
        for (AudioCue cue : AudioCue.values()) {
            if (cue.isMusic()) {
                preloadMusic(cue);
            } else {
                preloadEffect(cue);
            }
        }
    }

    private void preloadEffect(AudioCue cue) {
        FileHandle file = resolve(cue);
        if (file == null) {
            logMissingOnce(cue);
            return;
        }
        String cacheKey = file.path();
        Sound shared = soundCache.get(cacheKey);
        if (shared != null) {
            sounds.put(cue, shared);
            return;
        }
        try {
            Sound sound = Gdx.audio.newSound(file);
            soundCache.put(cacheKey, sound);
            sounds.put(cue, sound);
        } catch (RuntimeException exception) {
            logLoadFailure(cue, exception);
        }
    }

    private void preloadMusic(AudioCue cue) {
        FileHandle file = resolve(cue);
        if (file == null) {
            logMissingOnce(cue);
            return;
        }
        try {
            Music music = Gdx.audio.newMusic(file);
            music.setLooping(true);
            music.setVolume(musicVolume);
            musicTracks.put(cue, music);
        } catch (RuntimeException exception) {
            logLoadFailure(cue, exception);
        }
    }

    private void playMusic(AudioCue cue) {
        if (cue == null || !cue.isMusic()) {
            return;
        }
        refreshSettings();
        Music nextMusic = musicTracks.get(cue);
        if (nextMusic == null) {
            stopMusic();
            return;
        }

        if (cue == currentMusicCue && currentMusic == nextMusic) {
            currentMusic.setVolume(musicVolume);
            if (musicEnabled && musicVolume > 0f && !currentMusic.isPlaying()) {
                currentMusic.play();
            }
            return;
        }

        // Switching tracks never creates/disposes audio handles. It is just stop -> play.
        if (currentMusic != null) {
            currentMusic.stop();
        }
        currentMusic = nextMusic;
        currentMusicCue = cue;
        currentMusic.setLooping(true);
        currentMusic.setVolume(musicVolume);
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
            // Normal LibGDX path for <project>/assets/pvz audio/...
            FileHandle internal = Gdx.files.internal(path);
            if (internal.exists() && !internal.isDirectory()) {
                return internal;
            }
            // IntelliJ / alternate working-directory fallbacks.
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
        Gdx.app.error("AudioManager", "Could not load/play audio cue " + cue + ".", exception);
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
        if (currentMusic != null) {
            currentMusic.stop();
        }
        currentMusic = null;
        currentMusicCue = null;

        for (Music music : musicTracks.values()) {
            if (music != null) {
                music.dispose();
            }
        }
        musicTracks.clear();

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
