package game.animation.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class AnimationDefinition {
    private final String name;
    private final String path;
    private final int canvasWidth;
    private final int canvasHeight;
    private final Set<String> clips;
    private final Map<String, Float> clipDurations;

    public AnimationDefinition(
        String name,
        String path,
        int canvasWidth,
        int canvasHeight,
        Map<String, Float> clipDurations
    ) {
        this.name = name;
        this.path = path;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        Map<String, Float> durations = clipDurations == null
            ? Collections.emptyMap()
            : new LinkedHashMap<>(clipDurations);
        this.clipDurations = Collections.unmodifiableMap(durations);
        this.clips = Collections.unmodifiableSet(new LinkedHashSet<>(durations.keySet()));
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public int getCanvasWidth() {
        return canvasWidth;
    }

    public int getCanvasHeight() {
        return canvasHeight;
    }

    public Set<String> getClips() {
        return clips;
    }

    public boolean hasClip(String clip) {
        return clip != null && clips.contains(clip);
    }

    public float getClipDuration(String clip) {
        if (clip == null) {
            return 0f;
        }
        return Math.max(0f, clipDurations.getOrDefault(clip, 0f));
    }
}
