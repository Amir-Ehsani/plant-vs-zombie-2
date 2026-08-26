package game.render.entity;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.AnimationDefinition;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;

import java.util.Collections;
import java.util.Map;

final class ZombieDeathVisual {
    private static final float FALLBACK_DURATION = 1.8f;

    private final String path;
    private final String clip;
    private final String particleClip;
    private final double boardX;
    private final int lane;
    private final float scale;
    private final float duration;
    private final Map<String, Boolean> visibility;
    private float elapsed;

    ZombieDeathVisual(
        EntityAnimationProfile profile,
        String clip,
        double boardX,
        int lane,
        String hiddenPart
    ) {
        this(
            profile.getDefinition(),
            clip,
            profile.getDefinition().hasClip("particles") ? "particles" : null,
            boardX,
            lane,
            profile.getScale(),
            hiddenPart
        );
    }

    private ZombieDeathVisual(
        AnimationDefinition definition,
        String clip,
        String particleClip,
        double boardX,
        int lane,
        float scale,
        String hiddenPart
    ) {
        path = definition.getPath();
        this.clip = clip;
        this.particleClip = particleClip;
        this.boardX = boardX;
        this.lane = lane;
        this.scale = scale;
        visibility = hiddenPart == null
            ? Collections.emptyMap()
            : Collections.singletonMap(hiddenPart, false);
        float clipDuration = definition.getClipDuration(clip);
        duration = clipDuration > 0f ? clipDuration : FALLBACK_DURATION;
    }

    static ZombieDeathVisual effect(
        AnimationDefinition definition,
        String clip,
        double boardX,
        int lane,
        float scale
    ) {
        if (definition == null || clip == null || !definition.hasClip(clip)) {
            return null;
        }
        return new ZombieDeathVisual(
            definition,
            clip,
            null,
            boardX,
            lane,
            scale,
            null
        );
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
        Vector2 position = geometry.entityToScreen(boardX, lane);
        animations.draw(
            batch,
            path,
            clip,
            elapsed,
            position.x,
            position.y,
            scale,
            false,
            visibility
        );
        if (particleClip != null) {
            // Most PvZ zombie PAMs expose their detached-head/body particle emitter
            // through a one-frame `particles` clip. Drawing that clip with the same
            // advancing state time lets libPVZ animate the emitter during death.
            animations.draw(
                batch,
                path,
                particleClip,
                elapsed,
                position.x,
                position.y,
                scale,
                false,
                visibility
            );
        }
    }
}
