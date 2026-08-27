package game.render.entity;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.base.GameEntity;
import models.engine.board.Board;

public abstract class EntityView<T extends GameEntity> {
    private static final float HIT_FLASH_SECONDS = 0.13f;
    private static final float HIT_FLASH_ALPHA = 0.68f;

    protected final T entity;
    protected final EntityAnimationProfile profile;
    protected float stateTime;
    private String activeClip;
    private int previousVisualHealth = Integer.MIN_VALUE;
    private float hitFlashRemaining;

    protected EntityView(T entity, EntityAnimationProfile profile) {
        this.entity = entity;
        this.profile = profile;
        this.stateTime = 0f;
    }

    public T getEntity() {
        return entity;
    }

    public void update(float delta, Board board) {
        if (delta > 0f) {
            stateTime += delta;
        }
    }

    public abstract void render(
        Batch batch,
        BoardGeometry geometry,
        PvzAnimationService animations,
        Board board
    );

    protected void updateHitFlash(float delta, int visualHealth) {
        int current = Math.max(0, visualHealth);
        if (previousVisualHealth != Integer.MIN_VALUE && current < previousVisualHealth) {
            hitFlashRemaining = HIT_FLASH_SECONDS;
        }
        previousVisualHealth = current;
        if (delta > 0f && hitFlashRemaining > 0f) {
            hitFlashRemaining = Math.max(0f, hitFlashRemaining - delta);
        }
    }

    protected boolean beginHitFlash(Batch batch) {
        if (batch == null || hitFlashRemaining <= 0f) {
            return false;
        }
        float progress = hitFlashRemaining / HIT_FLASH_SECONDS;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(1f, 1f, 1f, HIT_FLASH_ALPHA * progress);
        return true;
    }

    protected void endHitFlash(Batch batch) {
        if (batch == null) {
            return;
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    protected float timeForClip(String clip) {
        if (clip == null) {
            return stateTime;
        }
        if (!clip.equals(activeClip)) {
            activeClip = clip;
            stateTime = 0f;
        }
        return stateTime;
    }
}
