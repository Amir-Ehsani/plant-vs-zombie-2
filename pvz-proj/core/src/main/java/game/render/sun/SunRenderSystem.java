package game.render.sun;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.AnimationDefinition;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.engine.sun.Sun;
import models.engine.sun.SunManager;
import models.engine.sun.SunType;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class SunRenderSystem {
    private static final String SUN_ANIMATION_NAME = "SUN";
    private static final int SKY_FALL_TICKS = 50;
    private static final float LOGIC_TICK_SECONDS = 0.1f;
    private static final float SKY_START_MARGIN = 70f;
    private static final float NORMAL_SCALE = 0.64f;
    private static final float SPECIAL_SCALE = 0.88f;
    private static final float RADIOACTIVE_SCALE = 0.74f;
    private static final float HIT_RADIUS_FACTOR = 0.34f;

    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final Map<Sun, FallingVisualState> fallingVisuals = new IdentityHashMap<>();
    private final String animationPath;
    private final String normalClip;

    public SunRenderSystem(BoardGeometry geometry, PvzAnimationService animations) {
        if (geometry == null || animations == null || animations.getCatalog() == null) {
            throw new IllegalArgumentException("Sun renderer requires geometry and animations.");
        }
        this.geometry = geometry;
        this.animations = animations;
        AnimationDefinition definition = animations.getCatalog().findByName(SUN_ANIMATION_NAME, null);
        if (definition == null) {
            animationPath = null;
            normalClip = null;
            return;
        }
        animationPath = definition.getPath();
        normalClip = chooseClip(definition);
        animations.preload(animationPath);
    }

    public void update(float delta, SunManager sunManager) {
        if (sunManager == null) {
            fallingVisuals.clear();
            return;
        }
        List<Sun> suns = sunManager.getSuns();
        fallingVisuals.keySet().removeIf(sun -> !suns.contains(sun));
        float safeDelta = Math.max(0f, delta);
        for (Sun sun : suns) {
            updateVisualState(sun, safeDelta);
        }
    }

    public void render(Batch batch, SunManager sunManager, float stateTime) {
        if (batch == null || sunManager == null || animationPath == null) {
            return;
        }
        batch.begin();
        for (Sun sun : sunManager.getSuns()) {
            drawSun(batch, sun, stateTime);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    public Sun findHoveredSun(float worldX, float worldY, SunManager sunManager) {
        if (sunManager == null) {
            return null;
        }
        for (Sun sun : sunManager.getSuns()) {
            Vector2 position = screenPosition(sun);
            float radius = geometry.getTileWidth() * HIT_RADIUS_FACTOR * scaleFor(sun.getType()) / NORMAL_SCALE;
            if (Vector2.dst2(worldX, worldY, position.x, position.y) <= radius * radius) {
                return sun;
            }
        }
        return null;
    }

    private void updateVisualState(Sun sun, float delta) {
        FallingVisualState state = fallingVisuals.get(sun);
        if (sun.isFalling()) {
            if (state == null) {
                state = new FallingVisualState(sun.getFallingTicksRemaining());
                fallingVisuals.put(sun, state);
            }
            state.update(delta, sun.getFallingTicksRemaining());
            return;
        }
        if (state == null) {
            return;
        }
        state.update(delta, 0);
        if (state.isSettled()) {
            fallingVisuals.remove(sun);
        }
    }

    private void drawSun(Batch batch, Sun sun, float stateTime) {
        Vector2 position = screenPosition(sun);
        batch.setColor(colorFor(sun.getType()));
        animations.draw(
            batch,
            animationPath,
            normalClip,
            stateTime,
            position.x,
            position.y,
            scaleFor(sun.getType()),
            true
        );
    }

    private Vector2 screenPosition(Sun sun) {
        Vector2 target = geometry.boardToScreen(sun.getPosition().getY(), sun.getPosition().getX());
        FallingVisualState state = fallingVisuals.get(sun);
        if (!sun.isFalling() && state == null) {
            return target;
        }
        Rectangle bounds = geometry.getBoardBounds();
        float startY = bounds.y + bounds.height + SKY_START_MARGIN;
        float remainingTicks = state == null
            ? sun.getFallingTicksRemaining()
            : state.getDisplayedTicks();
        float progress = 1f - MathUtils.clamp(remainingTicks / SKY_FALL_TICKS, 0f, 1f);
        float y = MathUtils.lerp(startY, target.y, progress);
        return new Vector2(target.x, y);
    }

    private float scaleFor(SunType type) {
        if (type == SunType.SPECIAL) {
            return SPECIAL_SCALE;
        }
        if (type == SunType.RADIOACTIVE) {
            return RADIOACTIVE_SCALE;
        }
        return NORMAL_SCALE;
    }

    private Color colorFor(SunType type) {
        if (type == SunType.SPECIAL) {
            return new Color(1f, 0.94f, 0.55f, 1f);
        }
        if (type == SunType.RADIOACTIVE) {
            return new Color(0.82f, 0.42f, 1f, 1f);
        }
        return Color.WHITE;
    }

    private String chooseClip(AnimationDefinition definition) {
        if (definition.hasClip("animation")) {
            return "animation";
        }
        return definition.getClips().isEmpty() ? null : definition.getClips().iterator().next();
    }

    private static final class FallingVisualState {
        private float displayedTicks;
        private float segmentStartTicks;
        private int targetTicks;
        private float segmentElapsed;

        private FallingVisualState(int initialTicks) {
            displayedTicks = Math.max(0, initialTicks);
            segmentStartTicks = displayedTicks;
            targetTicks = Math.max(0, initialTicks);
            segmentElapsed = LOGIC_TICK_SECONDS;
        }

        private void update(float delta, int modelTicks) {
            int safeTarget = Math.max(0, modelTicks);
            if (safeTarget != targetTicks) {
                segmentStartTicks = displayedTicks;
                targetTicks = safeTarget;
                segmentElapsed = 0f;
            }
            if (MathUtils.isEqual(displayedTicks, targetTicks, 0.001f)) {
                displayedTicks = targetTicks;
                return;
            }
            segmentElapsed += delta;
            float alpha = MathUtils.clamp(segmentElapsed / LOGIC_TICK_SECONDS, 0f, 1f);
            displayedTicks = MathUtils.lerp(segmentStartTicks, targetTicks, alpha);
        }

        private float getDisplayedTicks() {
            return displayedTicks;
        }

        private boolean isSettled() {
            return targetTicks == 0 && displayedTicks <= 0.001f;
        }
    }
}
