package game.render.mower;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
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
    private static final float START_X = 0.15f;
    private static final float END_X = 10.15f;
    private static final float RUN_SECONDS = 2.0f;
    private static final float MOWER_SCALE = 0.34f;

    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final Map<Integer, MowerState> states = new HashMap<>();
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
        AnimationDefinition definition = animations.getCatalog().findByName(
            animationNameFor(seasonType),
            null
        );
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
            states.clear();
            return;
        }
        for (int row = 1; row <= board.getHeight(); row++) {
            Lane lane = board.getLaneAt(row);
            if (lane == null) {
                continue;
            }
            updateLaneState(row, lane.getLawnMower(), delta);
        }
    }

    public void render(Batch batch, Board board) {
        if (batch == null || board == null || animationPath == null) {
            return;
        }
        batch.begin();
        for (int row = 1; row <= board.getHeight(); row++) {
            Lane lane = board.getLaneAt(row);
            if (lane == null) {
                continue;
            }
            drawLaneMower(batch, row, lane.getLawnMower());
        }
        batch.end();
    }

    private void updateLaneState(int row, LawnMower mower, float delta) {
        MowerState state = states.computeIfAbsent(row, ignored -> new MowerState(mower.isReady()));
        if (state.wasReady && mower.isTriggered() && !state.running && !state.finished) {
            state.running = true;
            state.elapsed = 0f;
        }
        state.wasReady = mower.isReady();
        if (!state.running || delta <= 0f) {
            return;
        }
        state.elapsed += delta;
        if (state.elapsed >= RUN_SECONDS) {
            state.running = false;
            state.finished = true;
        }
    }

    private void drawLaneMower(Batch batch, int row, LawnMower mower) {
        MowerState state = states.computeIfAbsent(row, ignored -> new MowerState(mower.isReady()));
        if (state.running) {
            float progress = MathUtils.clamp(state.elapsed / RUN_SECONDS, 0f, 1f);
            drawAt(batch, MathUtils.lerp(START_X, END_X, progress), row, attackClip, state.elapsed, true);
            return;
        }
        if (mower.isReady()) {
            drawAt(batch, START_X, row, idleClip, state.elapsed, true);
        }
    }

    private void drawAt(
        Batch batch,
        float boardX,
        int row,
        String clip,
        float stateTime,
        boolean loop
    ) {
        Vector2 position = geometry.entityToScreen(boardX, row);
        animations.draw(
            batch,
            animationPath,
            clip,
            stateTime,
            position.x,
            position.y,
            MOWER_SCALE,
            loop
        );
    }

    private String animationNameFor(SeasonType seasonType) {
        if (seasonType == SeasonType.FROSTBITE_CAVES) {
            return "MOWER_ICEAGE";
        }
        if (seasonType == SeasonType.BIG_WAVE_BEACH) {
            return "MOWER_BEACH";
        }
        if (seasonType == SeasonType.DARK_AGES) {
            return "MOWER_DARK";
        }
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

    private static final class MowerState {
        private boolean wasReady;
        private boolean running;
        private boolean finished;
        private float elapsed;

        private MowerState(boolean ready) {
            wasReady = ready;
        }
    }
}
