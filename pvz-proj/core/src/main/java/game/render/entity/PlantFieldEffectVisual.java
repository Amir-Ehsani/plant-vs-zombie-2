package game.render.entity;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.AnimationDefinition;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.engine.board.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PlantFieldEffectVisual {
    private static final float MIN_DURATION = 0.05f;

    private final AnimationDefinition definition;
    private final String clip;
    private final List<Position> positions;
    private final float scale;
    private final float delay;
    private final float duration;
    private final boolean loop;
    private float elapsed;

    PlantFieldEffectVisual(
        AnimationDefinition definition,
        String clip,
        List<Position> positions,
        float scale,
        float delay,
        float duration,
        boolean loop
    ) {
        if (definition == null || clip == null || !definition.hasClip(clip)) {
            throw new IllegalArgumentException("Field effect requires a valid animation clip.");
        }
        this.definition = definition;
        this.clip = clip;
        this.positions = positions == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(positions));
        this.scale = Math.max(0.05f, scale);
        this.delay = Math.max(0f, delay);
        float clipDuration = Math.max(MIN_DURATION, definition.getClipDuration(clip));
        this.duration = duration > 0f ? duration : clipDuration;
        this.loop = loop;
    }

    void update(float delta) {
        if (delta > 0f) {
            elapsed += delta;
        }
    }

    boolean isFinished() {
        return elapsed >= delay + duration;
    }

    void render(Batch batch, BoardGeometry geometry, PvzAnimationService animations) {
        if (elapsed < delay || positions.isEmpty()) {
            return;
        }
        float stateTime = elapsed - delay;
        for (Position position : positions) {
            Vector2 screen = geometry.entityToScreen(position.getX(), position.getY());
            animations.draw(
                batch,
                definition.getPath(),
                clip,
                stateTime,
                screen.x,
                screen.y,
                scale,
                loop
            );
        }
    }
}
