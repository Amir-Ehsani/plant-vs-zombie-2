package game.render.entity;

import com.badlogic.gdx.graphics.g2d.Batch;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.base.GameEntity;
import models.engine.board.Board;

public abstract class EntityView<T extends GameEntity> {
    protected final T entity;
    protected final EntityAnimationProfile profile;
    protected float stateTime;
    private String activeClip;

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
