package game.animation.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public final class AnimationDefinition {
    private final String name;
    private final String path;
    private final int canvasWidth;
    private final int canvasHeight;
    private final Set<String> clips;

    public AnimationDefinition(
        String name,
        String path,
        int canvasWidth,
        int canvasHeight,
        Set<String> clips
    ) {
        this.name = name;
        this.path = path;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.clips = Collections.unmodifiableSet(new LinkedHashSet<>(clips));
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
}
