package game.render.mower;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.AnimationDefinition;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.entities.LawnMower;
import models.level.core.SeasonType;

import java.util.HashMap;
import java.util.Map;

public final class LawnMowerRenderSystem {
    private static final float MOWER_SCALE = 0.45f;
    private static final float VISUAL_TILES_PER_SECOND = 2.5f;
    private static final float POSITION_EPSILON = 0.001f;

    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final Map<Integer, Float> laneAnimationTimes = new HashMap<>();
    private final Map<Integer, Float> laneVisualPositions = new HashMap<>();
    private final String animationPath;
    private final String idleClip;
    private final String attackClip;

    public LawnMowerRenderSystem(
        BoardGeometry geometry,
        PvzAnimationService animations,
        SeasonType seasonType
    ) {
        if (geometry == null || animations == null || animations.getCatalog() == null) {
            throw new IllegalArgumentException("Mower renderer requires geometry and animations.");
        }
        this.geometry = geometry;
        this.animations = animations;
        AnimationDefinition definition = animations.getCatalog().findByName(animationNameFor(seasonType), null);
        if (definition == null) {
            animationPath = null;
            idleClip = null;
            attackClip = null;
            return;
        }
        animationPath = definition.getPath();
        idleClip = chooseClip(definition, "idle", "idle2", "animation");
        attackClip = chooseClip(definition, "attack", "attack2", "transition", idleClip);
        animations.preload(animationPath);
    }

    public void update(float delta, Board board) {
        if (board == null) {
            clearRuntimeState();
            return;
        }
        float safeDelta = Math.max(0f, delta);
        for (int row = 1; row <= board.getHeight(); row++) {
            LawnMower mower = board.getLaneAt(row).getLawnMower();
            updateVisualPosition(row, mower, safeDelta);
            updateAnimationTime(row, mower, safeDelta);
        }
    }

    public void render(Batch batch, Board board) {
        if (batch == null || board == null || animationPath == null) {
            return;
        }
        batch.begin();
        for (int row = 1; row <= board.getHeight(); row++) {
            Lane lane = board.getLaneAt(row);
            if (lane != null) {
                drawLaneMower(batch, row, lane.getLawnMower());
            }
        }
        batch.end();
    }

    private void updateVisualPosition(int row, LawnMower mower, float delta) {
        float targetX = (float) mower.getPositionX();
        if (!mower.isTriggered()) {
            laneVisualPositions.put(row, targetX);
            return;
        }
        float visualX = laneVisualPositions.getOrDefault(row, (float) LawnMower.START_X);
        if (visualX > targetX) {
            visualX = targetX;
        }
        float maxDistance = VISUAL_TILES_PER_SECOND * delta;
        laneVisualPositions.put(row, Math.min(targetX, visualX + maxDistance));
    }

    private void updateAnimationTime(int row, LawnMower mower, float delta) {
        float current = laneAnimationTimes.getOrDefault(row, 0f);
        boolean active = mower.isMoving() || isFinishingVisualTravel(row, mower);
        laneAnimationTimes.put(row, active ? current + delta : 0f);
    }

    private boolean isFinishingVisualTravel(int row, LawnMower mower) {
        if (!mower.isTriggered()) {
            return false;
        }
        float visualX = laneVisualPositions.getOrDefault(row, (float) mower.getPositionX());
        return visualX + POSITION_EPSILON < (float) mower.getPositionX();
    }

    private void drawLaneMower(Batch batch, int row, LawnMower mower) {
        float animationTime = laneAnimationTimes.getOrDefault(row, 0f);
        float visualX = laneVisualPositions.getOrDefault(row, (float) mower.getPositionX());
        if (mower.isMoving() || isFinishingVisualTravel(row, mower)) {
            drawAt(batch, visualX, row, attackClip, animationTime);
        } else if (mower.isReady()) {
            drawAt(batch, (float) LawnMower.START_X, row, idleClip, animationTime);
        }
    }

    private void clearRuntimeState() {
        laneAnimationTimes.clear();
        laneVisualPositions.clear();
    }

    private void drawAt(Batch batch, float boardX, int row, String clip, float stateTime) {
        Vector2 position = geometry.entityToScreen(boardX, row);
        animations.draw(batch, animationPath, clip, stateTime, position.x, position.y, MOWER_SCALE, true);
    }

    private String animationNameFor(SeasonType seasonType) {
        if (seasonType == SeasonType.FROSTBITE_CAVES) return "MOWER_ICEAGE";
        if (seasonType == SeasonType.BIG_WAVE_BEACH) return "MOWER_BEACH";
        if (seasonType == SeasonType.DARK_AGES) return "MOWER_DARK";
        return "MOWER_EGYPT";
    }

    private String chooseClip(AnimationDefinition definition, String... preferred) {
        for (String candidate : preferred) {
            if (candidate != null && definition.hasClip(candidate)) {
                return candidate;
            }
        }
        return definition.getClips().isEmpty() ? null : definition.getClips().iterator().next();
    }
}
