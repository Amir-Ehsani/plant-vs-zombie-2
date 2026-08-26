package game.render.entity;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;

final class PlantActionVisual {
    private static final float MIN_DURATION = 0.05f;

    private final EntityAnimationProfile profile;
    private final String clip;
    private final int column;
    private final int lane;
    private final float duration;
    private final float playbackRate;
    private float elapsed;

    PlantActionVisual(EntityAnimationProfile profile, String clip, int column, int lane) {
        this(profile, clip, column, lane, 1f);
    }

    PlantActionVisual(
        EntityAnimationProfile profile,
        String clip,
        int column,
        int lane,
        float playbackRate
    ) {
        this.profile = profile;
        this.clip = clip;
        this.column = column;
        this.lane = lane;
        this.playbackRate = Math.max(0.05f, playbackRate);
        float clipDuration = Math.max(MIN_DURATION, profile.getDefinition().getClipDuration(clip));
        duration = clipDuration / this.playbackRate;
    }

    void update(float delta) {
        if (delta > 0f) {
            elapsed += delta;
        }
    }

    boolean isFinished() {
        return elapsed >= duration;
    }

    int getLane() {
        return lane;
    }

    void render(Batch batch, BoardGeometry geometry, PvzAnimationService animations) {
        Vector2 position = geometry.entityToScreen(column, lane);
        animations.draw(
            batch,
            profile.getPath(),
            clip,
            elapsed * playbackRate,
            position.x,
            position.y,
            profile.getScale(),
            false
        );
    }
}
