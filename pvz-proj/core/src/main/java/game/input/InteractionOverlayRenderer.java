package game.input;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.EntityAnimationRegistry;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.plant.PlantType;
import models.engine.board.Position;
import models.engine.session.GameSession;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InteractionOverlayRenderer {
    private static final Color VALID_TILE = new Color(0.45f, 1f, 0.45f, 0.28f);
    private static final Color INVALID_TILE = new Color(1f, 0.25f, 0.20f, 0.28f);
    private static final Color TOOL_COLOR = new Color(0.94f, 0.91f, 0.72f, 0.95f);
    private static final Color TOOL_ACCENT = new Color(0.30f, 0.65f, 0.25f, 0.95f);
    private static final Color SHOVEL_HANDLE = new Color(0.43f, 0.24f, 0.11f, 1f);
    private static final Color SHOVEL_METAL = new Color(0.78f, 0.82f, 0.84f, 1f);
    private static final Color SHOVEL_EDGE = new Color(0.36f, 0.39f, 0.42f, 1f);
    private static final float GHOST_SCALE_MULTIPLIER = 0.72f;

    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final EntityAnimationRegistry registry;
    private final Map<String, EntityAnimationProfile> plantProfiles;

    public InteractionOverlayRenderer(BoardGeometry geometry, PvzAnimationService animations) {
        if (geometry == null || animations == null) {
            throw new IllegalArgumentException("Interaction overlay requires board geometry and animations.");
        }
        this.geometry = geometry;
        this.animations = animations;
        registry = animations.getCatalog() == null
            ? null
            : new EntityAnimationRegistry(animations.getCatalog());
        plantProfiles = new LinkedHashMap<>();
    }

    public void drawTileHighlight(
            ShapeRenderer shapes,
            GameplayInteractionSystem interactions,
            Position tile
    ) {
        if (shapes == null || interactions == null || tile == null || !interactions.isActive()) {
            return;
        }
        InteractionValidation validation = interactions.validateTarget(tile);
        Rectangle bounds = geometry.getTileBounds(tile.getY(), tile.getX());
        shapes.setColor(validation.isValid() ? VALID_TILE : INVALID_TILE);
        shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public void drawPlantGhost(
            Batch batch,
            GameSession session,
            GameplayInteractionSystem interactions,
            float cursorX,
            float cursorY,
            float stateTime
    ) {
        if (batch == null || session == null || interactions == null
                || interactions.getMode() != GameplayInputMode.PLANTING) {
            return;
        }
        EntityAnimationProfile profile = profileFor(session, interactions.getSelectedPlantName());
        if (profile == null) {
            return;
        }
        String clip = profile.firstClip("idle", "play", "walk");
        animations.draw(
                batch,
                profile.getPath(),
                clip,
                stateTime,
                cursorX,
                cursorY,
                profile.getScale() * GHOST_SCALE_MULTIPLIER,
                true
        );
    }

    public void drawToolCursor(
            ShapeRenderer shapes,
            GameplayInteractionSystem interactions,
            float cursorX,
            float cursorY
    ) {
        if (shapes == null || interactions == null) {
            return;
        }
        if (interactions.getMode() == GameplayInputMode.SHOVEL) {
            drawShovel(shapes, cursorX, cursorY);
        } else if (interactions.getMode() == GameplayInputMode.PLANT_FOOD) {
            drawLeaf(shapes, cursorX, cursorY);
        }
    }

    private EntityAnimationProfile profileFor(GameSession session, String plantName) {
        if (plantName == null) {
            return null;
        }
        if (registry == null) {
            return null;
        }
        EntityAnimationProfile cached = plantProfiles.get(plantName);
        if (cached != null) {
            return cached;
        }
        PlantType type = session.getPlantType(plantName);
        EntityAnimationProfile profile = registry.forPlantType(type);
        if (profile != null) {
            animations.preload(profile.getPath());
            plantProfiles.put(plantName, profile);
        }
        return profile;
    }

    private void drawShovel(ShapeRenderer shapes, float x, float y) {
        // A compact D-grip shovel silhouette: handle, shaft, collar and pointed blade.
        // It stays centered around the pointer so the clicked tile remains obvious.
        shapes.setColor(SHOVEL_HANDLE);
        shapes.rectLine(x - 14f, y + 28f, x + 7f, y - 12f, 5f);
        shapes.rectLine(x - 22f, y + 31f, x - 8f, y + 36f, 4f);
        shapes.rectLine(x - 22f, y + 31f, x - 17f, y + 21f, 4f);
        shapes.rectLine(x - 8f, y + 36f, x - 4f, y + 26f, 4f);

        shapes.setColor(SHOVEL_EDGE);
        shapes.rectLine(x + 4f, y - 8f, x + 10f, y - 18f, 7f);
        shapes.triangle(x + 7f, y - 16f, x - 9f, y - 31f, x + 27f, y - 28f);

        shapes.setColor(SHOVEL_METAL);
        shapes.triangle(x + 8f, y - 15f, x - 5f, y - 29f, x + 23f, y - 27f);
        shapes.triangle(x - 5f, y - 29f, x + 23f, y - 27f, x + 8f, y - 39f);
    }

    private void drawLeaf(ShapeRenderer shapes, float x, float y) {
        shapes.setColor(TOOL_ACCENT);
        shapes.ellipse(x - 22f, y - 12f, 42f, 26f, 28);
        shapes.setColor(TOOL_COLOR);
        shapes.rectLine(x - 13f, y - 8f, x + 18f, y + 17f, 3f);
    }
}
