package game.render.entity;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;

import java.util.Collections;
import java.util.Map;

final class ZombieDeathVisual {
    private static final float FALLBACK_DURATION = 1.8f;

    private final EntityAnimationProfile profile;
    private final String clip;
    private final double boardX;
    private final int lane;
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
        this.profile = profile;
        this.clip = clip;
        this.boardX = boardX;
        this.lane = lane;
        visibility = hiddenPart == null
            ? Collections.emptyMap()
            : Collections.singletonMap(hiddenPart, false);
        float clipDuration = profile.getDefinition().getClipDuration(clip);
        duration = clipDuration > 0f ? clipDuration : FALLBACK_DURATION;
    }

    void update(float delta) {
        if (delta > 0f) elapsed += delta;
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
            profile.getPath(),
            clip,
            elapsed,
            position.x,
            position.y,
            profile.getScale(),
            false,
            visibility
        );
    }
}
