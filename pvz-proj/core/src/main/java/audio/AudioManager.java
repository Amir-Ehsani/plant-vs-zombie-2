package audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
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

import java.io.File;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AudioManager implements Disposable {
    private static final int AUDIO_LOAD_BUDGET_MS = 3;
    private static AudioManager active;

    private final AuthController authController;
    private final AssetManager assets;
    private final Map<AudioCue, String> assetKeys;
    private final Map<AudioCue, Long> lastPlayedAt;
    private final Map<AudioCue, Boolean> missingLogged;
    private final Set<String> queuedKeys;

    private Music currentMusic;
    private AudioCue currentMusicCue;
    private AudioCue requestedMusicCue;
    private float musicVolume;
    private float soundVolume;
    private boolean musicEnabled;

    public AudioManager(AuthController authController) {
        this.authController = authController;
        assets = new AssetManager(new AbsoluteFileHandleResolver());
        assets.setErrorListener((asset, throwable) ->
                Gdx.app.error("AudioManager", "Could not load audio asset " + asset + ".", throwable));
        assetKeys = new EnumMap<>(AudioCue.class);
        lastPlayedAt = new EnumMap<>(AudioCue.class);
        missingLogged = new EnumMap<>(AudioCue.class);
        queuedKeys = new LinkedHashSet<>();
        musicVolume = 1f;
        soundVolume = 1f;
        musicEnabled = true;
        active = this;
        syncSettings();
        queueAudioAssets();
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
        try {
            assets.update(AUDIO_LOAD_BUDGET_MS);
        } catch (RuntimeException exception) {
            Gdx.app.error("AudioManager", "An audio asset could not be loaded.", exception);
        }
        applyRequestedMusic();
    }

    public void playMenuMusic() {
        requestMusic(AudioCue.MENU_MUSIC);
    }

    public void playMiniGameMusic() {
        requestMusic(AudioCue.MENU_MUSIC);
    }

    public void playGameplayMusic(Level level) {
        if (level != null && level.getBossRuntime() != null) {
            requestMusic(AudioCue.BOSS_MUSIC);
            return;
        }
        SeasonType seasonType = level == null ? null : level.getSeasonType();
        if (seasonType == null) {
            requestMusic(AudioCue.MENU_MUSIC);
            return;
        }
        switch (seasonType) {
            case ANCIENT_EGYPT -> requestMusic(AudioCue.ANCIENT_EGYPT_MUSIC);
            case FROSTBITE_CAVES -> requestMusic(AudioCue.FROSTBITE_CAVES_MUSIC);
            case BIG_WAVE_BEACH -> requestMusic(AudioCue.BIG_WAVE_BEACH_MUSIC);
            case DARK_AGES -> requestMusic(AudioCue.DARK_AGES_MUSIC);
        }
    }

    public void play(AudioCue cue) {
        if (cue == null) {
            return;
        }
        if (cue.isMusic()) {
            requestMusic(cue);
            return;
        }
        syncSettings();
        if (soundVolume <= 0f || isRateLimited(cue)) {
            return;
        }
        String key = assetKeys.get(cue);
        if (key == null || !assets.isLoaded(key, Sound.class)) {
            return;
        }
        try {
            assets.get(key, Sound.class).play(soundVolume);
            lastPlayedAt.put(cue, TimeUtils.millis());
        } catch (RuntimeException exception) {
            Gdx.app.error("AudioManager", "Could not play audio cue " + cue + ".", exception);
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

    private void requestMusic(AudioCue cue) {
        if (cue == null || !cue.isMusic()) {
            return;
        }
        requestedMusicCue = cue;
        applyRequestedMusic();
    }

    private void applyRequestedMusic() {
        AudioCue cue = requestedMusicCue;
        if (cue == null || !cue.isMusic()) {
            return;
        }
        String key = assetKeys.get(cue);
        if (key == null || !assets.isLoaded(key, Music.class)) {
            return;
        }
        if (cue == currentMusicCue && currentMusic != null) {
            currentMusic.setVolume(musicVolume);
            if (musicEnabled && !currentMusic.isPlaying()) {
                currentMusic.play();
            } else if (!musicEnabled && currentMusic.isPlaying()) {
                currentMusic.pause();
            }
            return;
        }
        Music next;
        try {
            next = assets.get(key, Music.class);
        } catch (RuntimeException exception) {
            Gdx.app.error("AudioManager", "Could not activate music cue " + cue + ".", exception);
            return;
        }
        if (currentMusic != null) {
            currentMusic.stop();
        }
        currentMusic = next;
        currentMusicCue = cue;
        currentMusic.setLooping(true);
        currentMusic.setVolume(musicVolume);
        if (musicEnabled && musicVolume > 0f) {
            currentMusic.play();
        }
    }

    private void queueAudioAssets() {
        AudioCue[] priority = {
                AudioCue.MENU_MUSIC,
                AudioCue.ANCIENT_EGYPT_MUSIC,
                AudioCue.FROSTBITE_CAVES_MUSIC,
                AudioCue.BIG_WAVE_BEACH_MUSIC,
                AudioCue.DARK_AGES_MUSIC,
                AudioCue.BOSS_MUSIC
        };
        for (AudioCue cue : priority) {
            queueCue(cue);
        }
        for (AudioCue cue : AudioCue.values()) {
            queueCue(cue);
        }
    }

    private void queueCue(AudioCue cue) {
        if (cue == null || assetKeys.containsKey(cue)) {
            return;
        }
        FileHandle file = resolve(cue);
        if (file == null) {
            logMissingOnce(cue);
            return;
        }
        String key = file.file().getAbsolutePath().replace('\\', '/');
        assetKeys.put(cue, key);
        if (!queuedKeys.add(key)) {
            return;
        }
        if (cue.isMusic()) {
            assets.load(key, Music.class);
        } else {
            assets.load(key, Sound.class);
        }
    }

    private void syncSettings() {
        Settings settings = currentSettings();
        float nextMusicVolume = settings == null ? 1f : settings.getMusicVolume();
        float nextSoundVolume = settings == null ? 1f : settings.getSoundVolume();
        boolean nextMusicEnabled = settings == null || settings.isMusicEnabled();

        float previousMusicVolume = musicVolume;
        boolean enabledChanged = musicEnabled != nextMusicEnabled;
        musicVolume = clamp(nextMusicVolume);
        soundVolume = clamp(nextSoundVolume);
        musicEnabled = nextMusicEnabled;

        if (currentMusic != null) {
            if (Float.compare(previousMusicVolume, musicVolume) != 0) {
                currentMusic.setVolume(musicVolume);
            }
            if (enabledChanged) {
                if (musicEnabled && musicVolume > 0f) {
                    currentMusic.play();
                } else {
                    currentMusic.pause();
                }
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
            FileHandle file = resolvePath(path);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    private FileHandle resolvePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String clean = path.replace('\\', '/');
        FileHandle direct = firstExisting(
                Gdx.files.internal(clean),
                Gdx.files.local(clean),
                Gdx.files.internal("assets/" + clean),
                Gdx.files.local("assets/" + clean)
        );
        if (direct != null) {
            return direct;
        }

        File cwd = new File(System.getProperty("user.dir", "."));
        File parent = cwd.getParentFile();
        FileHandle disk = firstExistingFile(
                new File(cwd, clean),
                new File(cwd, "assets/" + clean),
                parent == null ? null : new File(parent, clean),
                parent == null ? null : new File(parent, "assets/" + clean)
        );
        if (disk != null) {
            return disk;
        }
        return findCaseInsensitive(clean, cwd, parent);
    }

    private FileHandle firstExisting(FileHandle... handles) {
        if (handles == null) {
            return null;
        }
        for (FileHandle handle : handles) {
            if (handle != null && handle.exists() && !handle.isDirectory()) {
                return handle;
            }
        }
        return null;
    }

    private FileHandle firstExistingFile(File... files) {
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (file != null && file.isFile()) {
                return Gdx.files.absolute(file.getAbsolutePath());
            }
        }
        return null;
    }

    private FileHandle findCaseInsensitive(String clean, File cwd, File parent) {
        String fileName = new File(clean).getName();
        File[] directories = {
                new File(cwd, "pvz audio"),
                new File(cwd, "assets/pvz audio"),
                parent == null ? null : new File(parent, "pvz audio"),
                parent == null ? null : new File(parent, "assets/pvz audio")
        };
        for (File directory : directories) {
            if (directory == null || !directory.isDirectory()) {
                continue;
            }
            File[] children = directory.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                if (child.isFile() && child.getName().equalsIgnoreCase(fileName)) {
                    return Gdx.files.absolute(child.getAbsolutePath());
                }
            }
        }
        return null;
    }

    private void logMissingOnce(AudioCue cue) {
        if (Boolean.TRUE.equals(missingLogged.put(cue, true))) {
            return;
        }
        Gdx.app.log("AudioManager", "Missing audio asset for " + cue + " under assets/pvz audio/.");
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }

    @Override
    public void dispose() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
        currentMusicCue = null;
        requestedMusicCue = null;
        assets.dispose();
        assetKeys.clear();
        queuedKeys.clear();
        lastPlayedAt.clear();
        if (active == this) {
            active = null;
        }
    }
}
