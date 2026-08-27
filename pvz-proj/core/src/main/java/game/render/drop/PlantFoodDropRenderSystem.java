package game.render.drop;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.AnimationDefinition;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.engine.session.GameSession;
import models.engine.session.PlantFoodDrop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class PlantFoodDropRenderSystem {
    private static final String PLANT_FOOD_PICKUP_PATH =
        "768/INITIAL/EFFECTS/PLANTFOOD_PICKUP/PLANTFOOD_PICKUP.PAM";
    private static final String IDLE_CLIP = "idle";
    private static final String PICKUP_CLIP = "animation";
    private static final float SCALE = 0.46f;
    private static final float HIT_RADIUS_X = 0.34f;
    private static final float HIT_RADIUS_Y = 0.42f;

    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final List<PickupVisual> pickupVisuals = new ArrayList<>();
    private final float pickupDuration;
    private float idleTime;

    public PlantFoodDropRenderSystem(BoardGeometry geometry, PvzAnimationService animations) {
        if (geometry == null || animations == null || animations.getCatalog() == null) {
            throw new IllegalArgumentException("Plant Food drop renderer requires geometry and animations.");
        }
        this.geometry = geometry;
        this.animations = animations;
        AnimationDefinition definition = animations.getCatalog().findByPath(PLANT_FOOD_PICKUP_PATH);
        pickupDuration = definition == null ? 0.7f : definition.getClipDuration(PICKUP_CLIP);
        animations.preload(PLANT_FOOD_PICKUP_PATH);
    }

    public void update(float delta) {
        float safeDelta = Math.max(0f, delta);
        idleTime += safeDelta;
        Iterator<PickupVisual> iterator = pickupVisuals.iterator();
        while (iterator.hasNext()) {
            PickupVisual visual = iterator.next();
            visual.elapsed += safeDelta;
            if (visual.elapsed >= pickupDuration) {
                iterator.remove();
            }
        }
    }

    public void render(Batch batch, GameSession session) {
        if (batch == null || session == null) {
            return;
        }
        batch.begin();
        for (PlantFoodDrop drop : session.getPlantFoodDrops()) {
            Vector2 position = position(drop);
            animations.draw(
                batch,
                PLANT_FOOD_PICKUP_PATH,
                IDLE_CLIP,
                idleTime,
                position.x,
                position.y,
                SCALE,
                true
            );
        }
        for (PickupVisual visual : pickupVisuals) {
            Vector2 position = geometry.entityToScreen(visual.x, visual.y);
            animations.draw(
                batch,
                PLANT_FOOD_PICKUP_PATH,
                PICKUP_CLIP,
                visual.elapsed,
                position.x,
                position.y,
                SCALE,
                false
            );
        }
        batch.end();
    }

    public PlantFoodDrop findAt(GameSession session, float worldX, float worldY) {
        if (session == null) {
            return null;
        }
        float radiusX = geometry.getTileWidth() * HIT_RADIUS_X;
        float radiusY = geometry.getTileHeight() * HIT_RADIUS_Y;
        for (PlantFoodDrop drop : session.getPlantFoodDrops()) {
            Vector2 position = position(drop);
            float dx = (worldX - position.x) / Math.max(1f, radiusX);
            float dy = (worldY - position.y) / Math.max(1f, radiusY);
            if (dx * dx + dy * dy <= 1f) {
                return drop;
            }
        }
        return null;
    }

    public void playCollection(PlantFoodDrop drop) {
        if (drop != null) {
            pickupVisuals.add(new PickupVisual(drop.getX(), drop.getY()));
        }
    }

    private Vector2 position(PlantFoodDrop drop) {
        return geometry.entityToScreen(drop.getX(), drop.getY());
    }

    private static final class PickupVisual {
        private final double x;
        private final double y;
        private float elapsed;

        private PickupVisual(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
