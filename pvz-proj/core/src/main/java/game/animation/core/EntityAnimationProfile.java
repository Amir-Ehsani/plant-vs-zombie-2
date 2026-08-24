package game.animation.core;

import java.util.Locale;

public final class EntityAnimationProfile {
    private final AnimationDefinition definition;
    private final float scale;

    public EntityAnimationProfile(AnimationDefinition definition, float scale) {
        if (definition == null) {
            throw new IllegalArgumentException("Animation definition cannot be null.");
        }
        this.definition = definition;
        this.scale = scale;
    }

    public AnimationDefinition getDefinition() {
        return definition;
    }

    public String getPath() {
        return definition.getPath();
    }

    public float getScale() {
        return scale;
    }

    public String firstClip(String... preferred) {
        String exact = findExactClip(preferred);
        if (exact != null) {
            return exact;
        }
        String related = findRelatedClip(preferred);
        if (related != null) {
            return related;
        }
        if (!definition.getClips().isEmpty()) {
            return definition.getClips().iterator().next();
        }
        return null;
    }

    private String findExactClip(String[] preferred) {
        if (preferred == null) {
            return null;
        }
        for (String candidate : preferred) {
            for (String clip : definition.getClips()) {
                if (normalize(clip).equals(normalize(candidate))) {
                    return clip;
                }
            }
        }
        return null;
    }

    private String findRelatedClip(String[] preferred) {
        if (preferred == null) {
            return null;
        }
        for (String candidate : preferred) {
            String normalizedCandidate = normalize(candidate);
            if (normalizedCandidate.isEmpty()) {
                continue;
            }
            for (String clip : definition.getClips()) {
                String normalizedClip = normalize(clip);
                if (normalizedClip.startsWith(normalizedCandidate)
                    || normalizedClip.endsWith(normalizedCandidate)) {
                    return clip;
                }
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
