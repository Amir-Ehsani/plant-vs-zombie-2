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
    private static final float SQUASH_DURATION = 1.20f;
    private static final float SQUASH_SETTLE_SECONDS = 0.18f;
    private static final float SQUASH_HEIGHT_RATIO = 0.22f;

    private final float scaleX;
    private final float scaleY;
    private final float duration;
    private final boolean squashed;
    private final Map<String, Boolean> visibility;
    private float elapsed;

    ZombieDeathVisual(
        EntityAnimationProfile profile,
        String clip,
        double boardX,
        int lane,
        String hiddenPart,
        String hiddenHeadPart
    ) {
        this(
            profile.getDefinition(),
            clip,
            profile.getDefinition().hasClip("particles") ? "particles" : null,
            boardX,
            lane,
            profile.getScale(),
            profile.getScale(),
            false,
            hiddenPart,
            hiddenHeadPart
        );
    }

    private ZombieDeathVisual(
        AnimationDefinition definition,
        String clip,
        String particleClip,
        double boardX,
        int lane,
        float scaleX,
        float scaleY,
        boolean squashed,
        String hiddenPart,
        String hiddenHeadPart
    ) {
        path = definition.getPath();
        this.clip = clip;
        this.particleClip = particleClip;
        this.boardX = boardX;
        this.lane = lane;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.squashed = squashed;
        visibility = hiddenVisibility(hiddenPart, hiddenHeadPart);
        float clipDuration = definition.getClipDuration(clip);
        duration = squashed ? SQUASH_DURATION
            : (clipDuration > 0f ? clipDuration : FALLBACK_DURATION);
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
            scale,
            false,
            null,
            null
        );
    }

    static ZombieDeathVisual squashed(
        EntityAnimationProfile profile,
        String clip,
        double boardX,
        int lane
    ) {
        if (profile == null || clip == null || !profile.getDefinition().hasClip(clip)) {
            return null;
        }
        float scale = profile.getScale();
        return new ZombieDeathVisual(
            profile.getDefinition(),
            clip,
            null,
            boardX,
            lane,
            scale * 1.08f,
            scale * SQUASH_HEIGHT_RATIO,
            true,
            null,
            null
        );
    }

    private static Map<String, Boolean> hiddenVisibility(String hiddenPart, String hiddenHeadPart) {
        if (hiddenPart == null && hiddenHeadPart == null) {
            return Collections.emptyMap();
        }
        Map<String, Boolean> hidden = new java.util.LinkedHashMap<>();
        if (hiddenPart != null) {
            hidden.put(hiddenPart, false);
        }
        if (hiddenHeadPart != null) {
            hidden.put(hiddenHeadPart, false);
        }
        return Collections.unmodifiableMap(hidden);
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
        float renderScaleY = scaleY;
        if (squashed) {
            float progress = Math.min(1f, elapsed / SQUASH_SETTLE_SECONDS);
            renderScaleY = scaleX + (scaleY - scaleX) * progress;
        }
        animations.draw(
            batch,
            path,
            clip,
            elapsed,
            position.x,
            position.y,
            scaleX,
            renderScaleY,
            squashed,
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
                scaleX,
                scaleY,
                false,
                visibility
            );
        }
    }
}
