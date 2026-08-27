package game.render.boss;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import boss.core.Boss;
import boss.core.BossAction;
import boss.core.BossRuntime;
import boss.core.BossState;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;

public final class BossRenderSystem {
    private static final String DAMAGE_CHUNKS_PATH =
            "768/INITIAL/EFFECTS/ZOMBOSS_DAMAGE_CHUNKS/ZOMBOSS_DAMAGE_CHUNKS.PAM";
    private static final float DAMAGE_CHUNKS_DURATION = 1.24f;

    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final BossRuntime runtime;
    private float stateTime;
    private float damageTime;
    private int observedVisualRevision;
    private int observedSectionBreakSerial;
    private boolean damagePlaying;

    public BossRenderSystem(
            BoardGeometry geometry,
            PvzAnimationService animations,
            BossRuntime runtime
    ) {
        if (geometry == null || animations == null || runtime == null) {
            throw new IllegalArgumentException("Boss renderer requires geometry, animations and runtime.");
        }
        this.geometry = geometry;
        this.animations = animations;
        this.runtime = runtime;
        Boss boss = runtime.getBoss();
        animations.preload(boss.getAnimationPath());
        animations.preload(DAMAGE_CHUNKS_PATH);
        observedVisualRevision = boss.getVisualRevision();
        observedSectionBreakSerial = boss.getHealth().getSectionBreakSerial();
        stateTime = 0f;
        damageTime = 0f;
        damagePlaying = false;
    }

    public void update(float delta) {
        if (delta <= 0f) {
            return;
        }
        Boss boss = runtime.getBoss();
        if (boss.getVisualRevision() != observedVisualRevision) {
            observedVisualRevision = boss.getVisualRevision();
            stateTime = 0f;
        } else {
            stateTime += delta;
        }
        int sectionSerial = boss.getHealth().getSectionBreakSerial();
        if (sectionSerial != observedSectionBreakSerial) {
            observedSectionBreakSerial = sectionSerial;
            damageTime = 0f;
            damagePlaying = true;
        }
        if (damagePlaying) {
            damageTime += delta;
            if (damageTime >= DAMAGE_CHUNKS_DURATION) {
                damagePlaying = false;
            }
        }
    }

    public void render(Batch batch) {
        if (batch == null || !runtime.isStarted()) {
            return;
        }
        Boss boss = runtime.getBoss();
        if (boss.getState() == BossState.DEFEATED) {
            return;
        }
        Vector2 position = geometry.entityToScreen(boss.getX(), boss.getCenterLane());
        String clip = visualClip(boss);
        boolean loop = boss.getState() == BossState.ACTIVE || boss.getState() == BossState.STUNNED;
        batch.begin();
        animations.draw(
                batch,
                boss.getAnimationPath(),
                clip,
                stateTime,
                position.x,
                position.y,
                boss.getRenderScale(),
                loop
        );
        if (damagePlaying) {
            animations.draw(
                    batch,
                    DAMAGE_CHUNKS_PATH,
                    damageClip(),
                    damageTime,
                    position.x,
                    position.y,
                    boss.getRenderScale(),
                    false
            );
        }
        batch.end();
    }

    private String visualClip(Boss boss) {
        if (boss.getState() == BossState.ACTION
                && boss.getAction() == BossAction.MOVE_LANES
                && runtime.getMoveDirection() != 0) {
            return boss.getMovementClip(runtime.getMoveDirection() < 0);
        }
        return boss.getVisualClip();
    }

    private String damageClip() {
        return switch (Math.min(3, Math.max(1, observedSectionBreakSerial))) {
            case 2 -> "animation2";
            case 3 -> "animation3";
            default -> "animation";
        };
    }
}
