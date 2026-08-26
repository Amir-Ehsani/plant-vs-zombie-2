package game.render.entity;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;

final class ZombieHeadVisual {
    private static final float FALL_DURATION = 0.68f;
    private static final float GROUND_HOLD_DURATION = 0.72f;
    private static final float FORWARD_TILE_OFFSET = 0.24f;
    private static final float GROUND_DROP_TILES = 0.72f;
    private static final float INITIAL_LIFT_TILES = 0.10f;

    private final EntityAnimationProfile profile;
    private final String partName;
    private final double boardX;
    private final int lane;
    private float elapsed;

    ZombieHeadVisual(
        EntityAnimationProfile profile,
        String partName,
        double boardX,
        int lane
    ) {
        this.profile = profile;
        this.partName = partName;
        this.boardX = boardX;
        this.lane = lane;
    }

    void update(float delta) {
        if (delta > 0f) {
            elapsed += delta;
        }
    }

    boolean isFinished() {
        return elapsed >= FALL_DURATION + GROUND_HOLD_DURATION;
    }

    int getLane() {
        return lane;
    }

    void render(Batch batch, BoardGeometry geometry, PvzAnimationService animations) {
        Vector2 base = geometry.entityToScreen(boardX, lane);
        float progress = MathUtils.clamp(elapsed / FALL_DURATION, 0f, 1f);
        float eased = 1f - (1f - progress) * (1f - progress);
        float lift = (float) Math.sin(progress * Math.PI) * geometry.getTileHeight() * INITIAL_LIFT_TILES;
        float x = base.x - geometry.getTileWidth() * FORWARD_TILE_OFFSET * eased;
        float y = base.y + lift - geometry.getTileHeight() * GROUND_DROP_TILES * eased;
        animations.drawPart(
            batch,
            profile.getPath(),
            "particles",
            0f,
            x,
            y,
            profile.getScale(),
            partName
        );
    }
}
