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
    private static final String ICE_BEHIND_PATH =
        "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_PLANT_BEHIND/FROSTBITE_ICE_BLOCK_PLANT_BEHIND.PAM";
    private static final String ICE_FRONT_PATH =
        "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_PLANT/FROSTBITE_ICE_BLOCK_PLANT.PAM";
    private static final String OCTOPUS_PATH =
        "768/FULL/EFFECTS/ZOMBIE_OCTOPUS_PROJECTILE/ZOMBIE_OCTOPUS_PROJECTILE.PAM";

    public PlantView(Plant plant, EntityAnimationProfile profile) {
        super(plant, profile);
    }

    @Override
    public void update(float delta, Board board) {
        if (!entity.isDisabled()) {
            super.update(delta, board);
        }
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
        drawFrozenBehind(batch, animations, position);
        drawPlant(batch, animations, position);
        drawFrozenFront(batch, animations, position);
        drawOctopus(batch, animations, position);
    }

    private void drawPlant(Batch batch, PvzAnimationService animations, Vector2 position) {
        String clip = resolveClip();
        batch.setColor(resolveTint());
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

    private void drawFrozenBehind(Batch batch, PvzAnimationService animations, Vector2 position) {
        if (!entity.isFrozenByZombie()) {
            return;
        }
        animations.draw(batch, ICE_BEHIND_PATH, "idle", stateTime, position.x, position.y, 0.48f, true);
    }

    private void drawFrozenFront(Batch batch, PvzAnimationService animations, Vector2 position) {
        if (!entity.isFrozenByZombie()) {
            return;
        }
        animations.draw(batch, ICE_FRONT_PATH, "freeze_idle", stateTime, position.x, position.y, 0.48f, true);
    }

    private void drawOctopus(Batch batch, PvzAnimationService animations, Vector2 position) {
        if (!entity.isCoveredByOctopus()) {
            return;
        }
        animations.draw(batch, OCTOPUS_PATH, "animation3", stateTime, position.x, position.y, 0.42f, true);
    }

    private String resolveClip() {
        return profile.firstClip(
            "idle",
            "idle2",
            "idle_stage1",
            "stage1_idle",
            "loop",
            "animation",
            "charge"
        );
    }

    private Color resolveTint() {
        if (entity.getIceHits() == 1) {
            return new Color(0.82f, 0.92f, 1f, 1f);
        }
        if (entity.getIceHits() == 2) {
            return new Color(0.68f, 0.84f, 1f, 1f);
        }
        if (entity.isTransformedToCat()) {
            return new Color(0.70f, 0.60f, 0.85f, 1f);
        }
        return Color.WHITE;
    }
}
