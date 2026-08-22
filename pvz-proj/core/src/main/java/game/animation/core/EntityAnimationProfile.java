package game.animation.core;

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
        if (preferred != null) {
            for (String clip : preferred) {
                if (definition.hasClip(clip)) {
                    return clip;
                }
            }
        }
        if (!definition.getClips().isEmpty()) {
            return definition.getClips().iterator().next();
        }
        return null;
    }
}
