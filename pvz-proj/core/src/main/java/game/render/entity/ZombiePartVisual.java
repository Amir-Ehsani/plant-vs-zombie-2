package game.render.entity;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;

final class ZombiePartVisual {
    private static final float LIFE_SECONDS = 0.85f;
    private static final float DRIFT_PIXELS_PER_SECOND = 42f;
    private static final float FALL_PIXELS_PER_SECOND = 34f;
    private static final float FALL_ACCELERATION = 150f;

    private final EntityAnimationProfile profile;
    private final String clip;
    private final String partName;
    private final float sourceTime;
    private final double boardX;
    private final int lane;
    private float elapsed;

    ZombiePartVisual(
        EntityAnimationProfile profile,
        String clip,
        String partName,
        float sourceTime,
        double boardX,
        int lane
    ) {
        this.profile = profile;
        this.clip = clip;
        this.partName = partName;
        this.sourceTime = sourceTime;
        this.boardX = boardX;
        this.lane = lane;
    }

    void update(float delta) {
        if (delta > 0f) {
            elapsed += delta;
        }
    }

    boolean isFinished() {
        return elapsed >= LIFE_SECONDS;
    }

    int getLane() {
        return lane;
    }

    void render(Batch batch, BoardGeometry geometry, PvzAnimationService animations) {
        Vector2 base = geometry.entityToScreen(boardX, lane);
        float x = base.x + elapsed * DRIFT_PIXELS_PER_SECOND;
        float y = base.y - elapsed * FALL_PIXELS_PER_SECOND
            - elapsed * elapsed * FALL_ACCELERATION;
        animations.drawPart(
            batch,
            profile.getPath(),
            clip,
            sourceTime + elapsed,
            x,
            y,
            profile.getScale(),
            partName
        );
    }
}
