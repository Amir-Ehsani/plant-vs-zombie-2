package game.animation.core;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class AnimationCatalog {
    private final Map<String, AnimationDefinition> byPath;

    private AnimationCatalog(Map<String, AnimationDefinition> byPath) {
        this.byPath = Collections.unmodifiableMap(new LinkedHashMap<>(byPath));
    }

    public static AnimationCatalog load(FileHandle file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("animations.json was not found.");
        }

        JsonValue root = new JsonReader().parse(file);
        JsonValue animations = root.get("animations");
        if (animations == null || !animations.isArray()) {
            throw new IllegalArgumentException("animations.json has no animations array.");
        }

        Map<String, AnimationDefinition> entries = new LinkedHashMap<>();
        for (JsonValue item = animations.child; item != null; item = item.next) {
            AnimationDefinition definition = parseDefinition(item);
            if (definition != null) {
                entries.put(definition.getPath(), definition);
            }
        }
        return new AnimationCatalog(entries);
    }

    public AnimationDefinition findByPath(String path) {
        return byPath.get(path);
    }

    public int size() {
        return byPath.size();
    }

    private static AnimationDefinition parseDefinition(JsonValue item) {
        String name = item.getString("name", "");
        String path = item.getString("path", "");
        if (path.isBlank()) {
            return null;
        }

        int[] canvas = readCanvas(item.get("canvas"));
        Set<String> clips = readClips(item.get("clips"));
        return new AnimationDefinition(name, path, canvas[0], canvas[1], clips);
    }

    private static int[] readCanvas(JsonValue canvas) {
        int width = 0;
        int height = 0;
        if (canvas != null && canvas.isArray()) {
            JsonValue first = canvas.child;
            JsonValue second = first == null ? null : first.next;
            width = first == null ? 0 : first.asInt();
            height = second == null ? 0 : second.asInt();
        }
        return new int[]{width, height};
    }

    private static Set<String> readClips(JsonValue clipsObject) {
        Set<String> clips = new LinkedHashSet<>();
        if (clipsObject == null || !clipsObject.isObject()) {
            return clips;
        }
        for (JsonValue clip = clipsObject.child; clip != null; clip = clip.next) {
            clips.add(clip.name);
        }
        return clips;
    }
}
