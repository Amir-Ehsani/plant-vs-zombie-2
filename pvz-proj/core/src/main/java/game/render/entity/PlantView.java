package game.render.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.plant.Plant;
import models.engine.board.Board;

public final class PlantView extends EntityView<Plant> {
    public PlantView(Plant plant, EntityAnimationProfile profile) {
        super(plant, profile);
    }

    @Override
    public void render(
        Batch batch,
        BoardGeometry geometry,
        PvzAnimationService animations,
        Board board
    ) {
        if (!entity.isAlive()) {
            return;
        }

        Vector2 position = geometry.entityToScreen(entity.getX(), entity.getY());
        String clip = resolveClip();
        Color tint = resolveTint();
        batch.setColor(tint);
        animations.draw(
            batch,
            profile.getPath(),
            clip,
            timeForClip(clip),
            position.x,
            position.y,
            profile.getScale(),
            true
        );
        batch.setColor(Color.WHITE);
    }

    private String resolveClip() {
        if (entity.isDisabled()) {
            return profile.firstClip("idle", "idle2", "animation");
        }
        return profile.firstClip("idle", "idle2", "animation");
    }

    private Color resolveTint() {
        if (entity.isFrozenByZombie()) {
            return new Color(0.55f, 0.78f, 1f, 1f);
        }
        if (entity.isCoveredByOctopus()) {
            return new Color(0.78f, 0.58f, 0.90f, 1f);
        }
        if (entity.isTransformedToCat()) {
            return new Color(0.70f, 0.60f, 0.85f, 1f);
        }
        return Color.WHITE;
    }
}
