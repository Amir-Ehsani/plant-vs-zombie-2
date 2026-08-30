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
import java.util.Map;

public final class AudioManager implements Disposable {
    private static AudioManager active;

    private final AuthController authController;
    private final Map<AudioCue, Sound> sounds;
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
        lastPlayedAt = new EnumMap<>(AudioCue.class);
        missingLogged = new EnumMap<>(AudioCue.class);
        musicVolume = 1f;
        soundVolume = 1f;
        musicEnabled = true;
        active = this;
        syncSettings();
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

    public void update() {
        syncSettings();
    }

    public void playMenuMusic() {
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
        syncSettings();
        if (soundVolume <= 0f || isRateLimited(cue)) {
            return;
        }
        Sound sound = sounds.get(cue);
        if (sound == null) {
            FileHandle file = resolve(cue);
            if (file == null) {
                logMissingOnce(cue);
                return;
            }
            try {
                sound = Gdx.audio.newSound(file);
                sounds.put(cue, sound);
            } catch (RuntimeException exception) {
                logLoadFailure(cue, exception);
                return;
            }
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

    private void playMusic(AudioCue cue) {
        if (cue == null || !cue.isMusic()) {
            return;
        }
        syncSettings();
        if (cue == currentMusicCue && currentMusic != null) {
            currentMusic.setVolume(musicVolume);
            if (musicEnabled && !currentMusic.isPlaying()) {
                currentMusic.play();
            } else if (!musicEnabled && currentMusic.isPlaying()) {
                currentMusic.pause();
            }
            return;
        }

        disposeCurrentMusic();
        currentMusicCue = cue;
        FileHandle file = resolve(cue);
        if (file == null) {
            logMissingOnce(cue);
            return;
        }
        try {
            currentMusic = Gdx.audio.newMusic(file);
            currentMusic.setLooping(true);
            currentMusic.setVolume(musicVolume);
            if (musicEnabled) {
                currentMusic.play();
            }
        } catch (RuntimeException exception) {
            currentMusic = null;
            logLoadFailure(cue, exception);
        }
    }

    private void syncSettings() {
        Settings settings = currentSettings();
        float nextMusicVolume = settings == null ? 1f : settings.getMusicVolume();
        float nextSoundVolume = settings == null ? 1f : settings.getSoundVolume();
        boolean nextMusicEnabled = settings == null || settings.isMusicEnabled();

        musicVolume = clamp(nextMusicVolume);
        soundVolume = clamp(nextSoundVolume);
        boolean enabledChanged = musicEnabled != nextMusicEnabled;
        musicEnabled = nextMusicEnabled;

        if (currentMusic != null) {
            currentMusic.setVolume(musicVolume);
            if (!musicEnabled && currentMusic.isPlaying()) {
                currentMusic.pause();
            } else if (musicEnabled && enabledChanged && !currentMusic.isPlaying()) {
                currentMusic.play();
            }
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
            FileHandle file = Gdx.files.internal(path);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    private void logMissingOnce(AudioCue cue) {
        if (Boolean.TRUE.equals(missingLogged.put(cue, true))) {
            return;
        }
        Gdx.app.log("AudioManager", "Missing audio asset for " + cue + ". Expected under assets/pvz audio/.");
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
        currentMusicCue = null;
        for (Sound sound : sounds.values()) {
            if (sound != null) {
                sound.dispose();
            }
        }
        sounds.clear();
        lastPlayedAt.clear();
        if (active == this) {
            active = null;
        }
    }
}
