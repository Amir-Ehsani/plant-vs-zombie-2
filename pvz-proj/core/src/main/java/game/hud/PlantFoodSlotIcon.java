package game.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import game.animation.core.PvzAnimationService;

public final class PlantFoodSlotIcon extends Actor {
    public static final String PAM_PATH = "768/INITIAL/EFFECTS/PLANTFOOD_PICKUP/PLANTFOOD_PICKUP.PAM";
    private static final String IDLE_CLIP = "idle";
    private static final float SCALE = 0.50f;

    private final PvzAnimationService animations;
    private float stateTime;

    public PlantFoodSlotIcon(PvzAnimationService animations) {
        this.animations = animations;
        setTouchable(Touchable.disabled);
        if (animations != null && animations.isAvailable()) {
            animations.preload(PAM_PATH);
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (isVisible()) {
            stateTime += Math.max(0f, delta);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (animations == null || !animations.isAvailable() || !isVisible()) {
            return;
        }
        Color previous = batch.getColor();
        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        animations.draw(
                batch,
                PAM_PATH,
                IDLE_CLIP,
                stateTime,
                getX() + getWidth() * 0.50f,
                getY() + getHeight() * 0.42f,
                SCALE,
                true
        );
        batch.setColor(previous);
    }
}
