package game.input;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
    private static final float GHOST_SCALE_MULTIPLIER = 0.72f;
    private static final float SHOVEL_CURSOR_HEIGHT = 50f;
    private static final String SHOVEL_ICON_ID = "IMAGE_UI_HUD_INGAME_SHOVEL_ICON";

    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final EntityAnimationRegistry registry;
    private final Map<String, EntityAnimationProfile> plantProfiles;
    private final TextureRegion shovelCursor;

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
        shovelCursor = animations.region(SHOVEL_ICON_ID);
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

    public void drawSpriteToolCursor(
            Batch batch,
            GameplayInteractionSystem interactions,
            float cursorX,
            float cursorY
    ) {
        if (batch == null || interactions == null
                || interactions.getMode() != GameplayInputMode.SHOVEL
                || shovelCursor == null) {
            return;
        }
        float aspect = shovelCursor.getRegionWidth() / (float) Math.max(1, shovelCursor.getRegionHeight());
        float width = SHOVEL_CURSOR_HEIGHT * aspect;
        batch.draw(
            shovelCursor,
            cursorX - width * 0.28f,
            cursorY - SHOVEL_CURSOR_HEIGHT * 0.72f,
            width,
            SHOVEL_CURSOR_HEIGHT
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
        if (interactions.getMode() == GameplayInputMode.PLANT_FOOD) {
            drawLeaf(shapes, cursorX, cursorY);
        }
    }

    private EntityAnimationProfile profileFor(GameSession session, String plantName) {
        if (plantName == null || registry == null) {
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

    private void drawLeaf(ShapeRenderer shapes, float x, float y) {
        shapes.setColor(TOOL_ACCENT);
        shapes.ellipse(x - 22f, y - 12f, 42f, 26f, 28);
        shapes.setColor(TOOL_COLOR);
        shapes.rectLine(x - 13f, y - 8f, x + 18f, y + 17f, 3f);
    }
}
