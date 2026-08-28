package game.render.boss;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import boss.core.Boss;
import boss.core.BossAction;
import boss.core.BossRuntime;
import boss.core.BossState;
import game.animation.core.AnimationDefinition;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.engine.board.Position;

public final class BossRenderSystem {
    private static final String DAMAGE_CHUNKS_PATH =
            "768/INITIAL/EFFECTS/ZOMBOSS_DAMAGE_CHUNKS/ZOMBOSS_DAMAGE_CHUNKS.PAM";
    private static final String EGYPT_MISSILE_PATH =
            "768/INITIAL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_EGYPT/ZOMBOSS_MISSILE_EXPLOSION_EGYPT.PAM";
    private static final String FROST_MISSILE_PATH =
            "768/FULL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_ICEAGE/ZOMBOSS_MISSILE_EXPLOSION_ICEAGE.PAM";
    private static final String FROST_WIND_PATH =
            "768/FULL/EFFECTS/FROSTBITE_CHILL_WIND/FROSTBITE_CHILL_WIND.PAM";
    private static final String FROST_GLACIER_FOG_PATH =
            "768/FULL/EFFECTS/ZOMBOSS_GLACIER_FOGGING/ZOMBOSS_GLACIER_FOGGING.PAM";
    private static final String FROST_GLACIER_TOP_PATH =
            "768/FULL/EFFECTS/ZOMBOSS_GLACIER_TOP/ZOMBOSS_GLACIER_TOP.PAM";
    private static final String FROST_GLACIER_MIDDLE_PATH =
            "768/FULL/EFFECTS/ZOMBOSS_GLACIER_MIDDLE/ZOMBOSS_GLACIER_MIDDLE.PAM";
    private static final String FROST_GLACIER_BOTTOM_PATH =
            "768/FULL/EFFECTS/ZOMBOSS_GLACIER_BOTTOM/ZOMBOSS_GLACIER_BOTTOM.PAM";
    private static final float DAMAGE_CHUNKS_DURATION = 1.24f;
    private static final float EGYPT_MISSILE_SCALE = 0.58f;
    private static final float FROST_MISSILE_SCALE = 0.58f;
    private static final float FROST_WIND_SCALE = 0.92f;
    private static final float FROST_GLACIER_SCALE = 0.74f;
    private static final float FROST_MISSILE_LAUNCH_TIME = 2.9f;
    private static final float FROST_MISSILE_IMPACT_TIME = 3.0f;
    private static final float FROST_WIND_EFFECT_START_TIME = 0.3f;
    private static final float FROST_GLACIER_FOG_START_TIME = 1.1f;
    private static final float FROST_GLACIER_RESOLVE_TIME = 3.1f;

    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final BossRuntime runtime;
    private final float missileStartDuration;
    private final float missileFlightDuration;
    private final float missileExplosionDuration;
    private final float chargeForwardDuration;
    private final float chargeBackwardDuration;
    private final float frostMissileExplosionDuration;
    private final float frostWindDuration;
    private final float frostGlacierFogDuration;
    private final float frostGlacierBuildDuration;

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
        if (isEgyptBoss(boss)) {
            animations.preload(EGYPT_MISSILE_PATH);
        } else if (isFrostbiteBoss(boss)) {
            animations.preload(FROST_MISSILE_PATH);
            animations.preload(FROST_WIND_PATH);
            animations.preload(FROST_GLACIER_FOG_PATH);
            animations.preload(FROST_GLACIER_TOP_PATH);
            animations.preload(FROST_GLACIER_MIDDLE_PATH);
            animations.preload(FROST_GLACIER_BOTTOM_PATH);
        }
        missileStartDuration = clipDuration(boss.getAnimationPath(), "missile_start", 3.3333f);
        missileFlightDuration = clipDuration(EGYPT_MISSILE_PATH, "missile", 0.3f);
        missileExplosionDuration = clipDuration(EGYPT_MISSILE_PATH, "missile_explosion", 1.7f);
        chargeForwardDuration = clipDuration(boss.getAnimationPath(), "walk_forward", 1.2333f);
        chargeBackwardDuration = clipDuration(boss.getAnimationPath(), "walk_backwards", 1.2333f);
        frostMissileExplosionDuration = clipDuration(
                FROST_MISSILE_PATH, "missile_explosion", 0.6667f
        );
        frostWindDuration = clipDuration(FROST_WIND_PATH, "animation", 2.5667f);
        frostGlacierFogDuration = clipDuration(FROST_GLACIER_FOG_PATH, "animation", 2f);
        frostGlacierBuildDuration = clipDuration(FROST_GLACIER_MIDDLE_PATH, "animation3", 1.6667f);
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
        float clipTime = visualClipTime(boss);
        boolean loop = boss.getState() == BossState.ACTIVE || boss.getState() == BossState.STUNNED;
        batch.begin();
        animations.draw(
                batch,
                boss.getAnimationPath(),
                clip,
                clipTime,
                position.x,
                position.y,
                boss.getRenderScale(),
                loop
        );
        renderEgyptMissileEffect(batch, boss);
        renderFrostMissileEffect(batch, boss);
        renderFrostWindEffect(batch, boss);
        renderFrostGlacierEffect(batch, boss);
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
        if (boss.getState() == BossState.ACTION) {
            if (boss.getAction() == BossAction.MOVE_LANES && runtime.getMoveDirection() != 0) {
                return boss.getMovementClip(runtime.getMoveDirection() < 0);
            }
            if (boss.getAction() == BossAction.MISSILE && isEgyptBoss(boss)) {
                return stateTime < missileStartDuration ? "missile_start" : "rocket_launch";
            }
            if (boss.getAction() == BossAction.CHARGE && isEgyptBoss(boss)) {
                float returnStart = chargeForwardDuration + 0.3f;
                return stateTime < returnStart ? "walk_forward" : "walk_backwards";
            }
            if (boss.getAction() == BossAction.ICE_MISSILE && isFrostbiteBoss(boss)) {
                return "slingshot";
            }
            if (boss.getAction() == BossAction.ICE_WIND && isFrostbiteBoss(boss)) {
                return "wind_" + Math.min(4, Math.max(1, runtime.getFrostVisualVariant()));
            }
            if (boss.getAction() == BossAction.FREEZE_COLUMN && isFrostbiteBoss(boss)) {
                return "glacier_column_" + Math.min(6, Math.max(1, runtime.getFrostVisualVariant()));
            }
        }
        return boss.getVisualClip();
    }

    private float visualClipTime(Boss boss) {
        if (boss.getState() != BossState.ACTION || !isEgyptBoss(boss)) {
            return stateTime;
        }
        if (boss.getAction() == BossAction.MISSILE && stateTime >= missileStartDuration) {
            return stateTime - missileStartDuration;
        }
        if (boss.getAction() == BossAction.CHARGE) {
            float returnStart = chargeForwardDuration + 0.3f;
            if (stateTime >= returnStart) {
                return Math.min(chargeBackwardDuration, stateTime - returnStart);
            }
            return Math.min(chargeForwardDuration, stateTime);
        }
        return stateTime;
    }

    private void renderEgyptMissileEffect(Batch batch, Boss boss) {
        if (!isEgyptBoss(boss)
                || boss.getState() != BossState.ACTION
                || boss.getAction() != BossAction.MISSILE) {
            return;
        }
        Position target = runtime.getActionTarget();
        if (target == null) {
            return;
        }
        Vector2 targetPosition = geometry.boardToScreen(target.getY(), target.getX());
        float launchTime = missileStartDuration;
        float impactTime = launchTime + missileFlightDuration;

        if (stateTime < impactTime) {
            animations.draw(
                    batch,
                    EGYPT_MISSILE_PATH,
                    "missile_lock_reticle",
                    stateTime,
                    targetPosition.x,
                    targetPosition.y,
                    EGYPT_MISSILE_SCALE,
                    true
            );
        }
        if (stateTime >= launchTime && stateTime < impactTime) {
            animations.draw(
                    batch,
                    EGYPT_MISSILE_PATH,
                    "missile",
                    stateTime - launchTime,
                    targetPosition.x,
                    targetPosition.y,
                    EGYPT_MISSILE_SCALE,
                    false
            );
        }
        if (stateTime >= impactTime && stateTime < impactTime + missileExplosionDuration) {
            animations.draw(
                    batch,
                    EGYPT_MISSILE_PATH,
                    "missile_explosion",
                    stateTime - impactTime,
                    targetPosition.x,
                    targetPosition.y,
                    EGYPT_MISSILE_SCALE,
                    false
            );
        }
    }


    private void renderFrostMissileEffect(Batch batch, Boss boss) {
        if (!isFrostbiteBoss(boss)
                || boss.getState() != BossState.ACTION
                || boss.getAction() != BossAction.ICE_MISSILE) {
            return;
        }
        Position target = runtime.getActionTarget();
        if (target == null) {
            return;
        }
        Vector2 targetPosition = geometry.boardToScreen(target.getY(), target.getX());
        if (stateTime < FROST_MISSILE_IMPACT_TIME) {
            animations.draw(
                    batch, FROST_MISSILE_PATH, "missile_lock_reticle", stateTime,
                    targetPosition.x, targetPosition.y, FROST_MISSILE_SCALE, true
            );
        }
        if (stateTime >= FROST_MISSILE_LAUNCH_TIME
                && stateTime < FROST_MISSILE_IMPACT_TIME) {
            animations.draw(
                    batch, FROST_MISSILE_PATH, "missile",
                    stateTime - FROST_MISSILE_LAUNCH_TIME,
                    targetPosition.x, targetPosition.y, FROST_MISSILE_SCALE, true
            );
        }
        if (stateTime >= FROST_MISSILE_IMPACT_TIME
                && stateTime < FROST_MISSILE_IMPACT_TIME + frostMissileExplosionDuration) {
            animations.draw(
                    batch, FROST_MISSILE_PATH, "missile_explosion",
                    stateTime - FROST_MISSILE_IMPACT_TIME,
                    targetPosition.x, targetPosition.y, FROST_MISSILE_SCALE, false
            );
        }
    }

    private void renderFrostWindEffect(Batch batch, Boss boss) {
        if (!isFrostbiteBoss(boss)
                || boss.getState() != BossState.ACTION
                || boss.getAction() != BossAction.ICE_WIND
                || stateTime < FROST_WIND_EFFECT_START_TIME) {
            return;
        }
        float effectTime = stateTime - FROST_WIND_EFFECT_START_TIME;
        if (effectTime > frostWindDuration) {
            return;
        }
        for (int lane : runtime.getFrostWindLanes()) {
            Vector2 position = geometry.entityToScreen(5.2, lane);
            animations.draw(
                    batch, FROST_WIND_PATH, "animation", effectTime,
                    position.x, position.y, FROST_WIND_SCALE, false
            );
        }
    }

    private void renderFrostGlacierEffect(Batch batch, Boss boss) {
        if (!isFrostbiteBoss(boss)
                || boss.getState() != BossState.ACTION
                || boss.getAction() != BossAction.FREEZE_COLUMN) {
            return;
        }
        int column = runtime.getFrostFreezeColumn();
        if (column < 1 || column > BoardGeometry.COLUMNS) {
            return;
        }

        if (stateTime >= FROST_GLACIER_FOG_START_TIME
                && stateTime < FROST_GLACIER_RESOLVE_TIME) {
            float fogTime = Math.min(
                    frostGlacierFogDuration, stateTime - FROST_GLACIER_FOG_START_TIME
            );
            Vector2 center = geometry.boardToScreen(3, column);
            animations.draw(
                    batch, FROST_GLACIER_FOG_PATH, "animation", fogTime,
                    center.x, center.y, 1.08f, false
            );
        }

        if (stateTime < FROST_GLACIER_RESOLVE_TIME
                || stateTime > FROST_GLACIER_RESOLVE_TIME + frostGlacierBuildDuration) {
            return;
        }
        float buildTime = stateTime - FROST_GLACIER_RESOLVE_TIME;
        Vector2 top = geometry.boardToScreen(1, column);
        Vector2 middle = geometry.boardToScreen(3, column);
        Vector2 bottom = geometry.boardToScreen(5, column);
        animations.draw(
                batch, FROST_GLACIER_TOP_PATH, "animation", buildTime,
                top.x, top.y, FROST_GLACIER_SCALE, true
        );
        animations.draw(
                batch, FROST_GLACIER_MIDDLE_PATH, "animation3", buildTime,
                middle.x, middle.y, FROST_GLACIER_SCALE, false
        );
        animations.draw(
                batch, FROST_GLACIER_BOTTOM_PATH, "animation", buildTime,
                bottom.x, bottom.y, FROST_GLACIER_SCALE, true
        );
    }

    private float clipDuration(String path, String clip, float fallback) {
        if (animations.getCatalog() == null) {
            return fallback;
        }
        AnimationDefinition definition = animations.getCatalog().findByPath(path);
        if (definition == null) {
            return fallback;
        }
        float duration = definition.getClipDuration(clip);
        return duration > 0f ? duration : fallback;
    }

    private boolean isEgyptBoss(Boss boss) {
        return boss != null && (boss.getId().equals("zomboss-egypt")
                || boss.getChapterName().equals("ancient-egypt"));
    }

    private boolean isFrostbiteBoss(Boss boss) {
        return boss != null && (boss.getId().equals("zomboss-frostbite")
                || boss.getChapterName().equals("ice-cave"));
    }

    private String damageClip() {
        return switch (Math.min(3, Math.max(1, observedSectionBreakSerial))) {
            case 2 -> "animation2";
            case 3 -> "animation3";
            default -> "animation";
        };
    }
}
