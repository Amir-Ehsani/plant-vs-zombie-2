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

public final class SunRenderSystem {
    private static final String SUN_ANIMATION_NAME = "SUN";
    private static final int SKY_FALL_TICKS = 50;
    private static final float SKY_START_MARGIN = 70f;
    private static final float NORMAL_SCALE = 0.32f;
    private static final float SPECIAL_SCALE = 0.44f;
    private static final float RADIOACTIVE_SCALE = 0.37f;
    private static final float HIT_RADIUS_FACTOR = 0.34f;

    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
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
        if (!sun.isFalling()) {
            return target;
        }
        Rectangle bounds = geometry.getBoardBounds();
        float startY = bounds.y + bounds.height + SKY_START_MARGIN;
        float progress = 1f - MathUtils.clamp(
            sun.getFallingTicksRemaining() / (float) SKY_FALL_TICKS,
            0f,
            1f
        );
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
}
