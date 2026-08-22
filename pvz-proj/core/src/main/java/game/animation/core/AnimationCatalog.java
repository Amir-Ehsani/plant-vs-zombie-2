package game.animation.core;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AnimationCatalog {
    private final Map<String, AnimationDefinition> byPath;
    private final Map<String, List<AnimationDefinition>> byNormalizedName;

    private AnimationCatalog(Map<String, AnimationDefinition> byPath) {
        this.byPath = Collections.unmodifiableMap(new LinkedHashMap<>(byPath));
        Map<String, List<AnimationDefinition>> names = new LinkedHashMap<>();
        for (AnimationDefinition definition : byPath.values()) {
            String key = normalize(definition.getName());
            names.computeIfAbsent(key, ignored -> new ArrayList<>()).add(definition);
        }
        Map<String, List<AnimationDefinition>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, List<AnimationDefinition>> entry : names.entrySet()) {
            immutable.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        byNormalizedName = Collections.unmodifiableMap(immutable);
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

    public AnimationDefinition findByName(String name, String requiredPathPart) {
        List<AnimationDefinition> definitions = byNormalizedName.get(normalize(name));
        if (definitions == null || definitions.isEmpty()) {
            return null;
        }
        if (requiredPathPart == null || requiredPathPart.isBlank()) {
            return definitions.get(0);
        }
        String required = requiredPathPart.toUpperCase(Locale.ROOT);
        for (AnimationDefinition definition : definitions) {
            if (definition.getPath().toUpperCase(Locale.ROOT).contains(required)) {
                return definition;
            }
        }
        return null;
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

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
