package game.render.boss;

import boss.core.Boss;
import boss.core.BossAction;
import boss.core.BossRuntime;
import boss.core.BossState;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.AnimationDefinition;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.engine.board.Position;

import java.util.Collections;

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

    private static final String DARK_FIREBALL_PATH =
            "768/FULL/EFFECTS/ZOMBOSS_DARK_FIREBALL/ZOMBOSS_DARK_FIREBALL.PAM";
    private static final String DARK_DIRT_PATH =
            "768/INITIAL/EFFECTS/GRAVEBUSTER_DIRT/GRAVEBUSTER_DIRT.PAM";
    private static final String FIRE_PEA_PATH =
            "768/INITIAL/EFFECTS/FIREPEASHOOTER_FIRE/FIREPEASHOOTER_FIRE.PAM";
    private static final String SNAPDRAGON_FIRE_PATH =
            "768/FULL/EFFECTS/SNAPDRAGON_FIRE/SNAPDRAGON_FIRE.PAM";
    private static final String DARK_ARRIVAL_SHADOW_PATH =
            "768/FULL/EFFECTS/ZOMBOSS_LOSTCITY_AIR_STRIKE_SHADOW/"
                    + "ZOMBOSS_LOSTCITY_AIR_STRIKE_SHADOW.PAM";
    private static final String SHARK_PATH =
            "768/FULL/EFFECTS/ZOMBOSS_SHARK_PROJECTILE/ZOMBOSS_SHARK_PROJECTILE.PAM";
    private static final String WATER_RIPPLE_PATH =
            "768/FULL/BACKGROUNDS/WATER_GARGANTUAR_RIPPLE/WATER_GARGANTUAR_RIPPLE.PAM";
    private static final String TURBINE_PATH =
            "768/FULL/EFFECTS/ZOMBOSS_TURBINE_WIND/ZOMBOSS_TURBINE_WIND.PAM";
    private static final String PULLED_PATH =
            "768/FULL/EFFECTS/ZOMBOSS_PLANT_PULLED/ZOMBOSS_PLANT_PULLED.PAM";

    private static final float DARK_EFFECT_SCALE = 0.72f;
    private static final float DARK_BREATH_SCALE = 0.66f;
    private static final float DARK_ARRIVAL_FIRE_IMPACT_TIME = 7.8f;
    private static final float SHARK_SCALE = 0.68f;
    private static final float IDLE_SHARK_SCALE = 0.54f;
    private static final float TURBINE_SCALE = 0.82f;

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
    private float actionTime;
    private float stageTime;
    private float damageTime;
    private int observedVisualRevision;
    private int observedSectionBreakSerial;
    private int observedActionStartTick;
    private int observedActionStage;
    private boolean damagePlaying;

    public BossRenderSystem(BoardGeometry geometry, PvzAnimationService animations, BossRuntime runtime) {
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
        if (isDarkBoss(boss)) {
            animations.preload(DARK_FIREBALL_PATH);
            animations.preload(DARK_DIRT_PATH);
            animations.preload(FIRE_PEA_PATH);
            animations.preload(SNAPDRAGON_FIRE_PATH);
            animations.preload(DARK_ARRIVAL_SHADOW_PATH);
        }
        if (isBeachBoss(boss)) {
            animations.preload(SHARK_PATH);
            animations.preload(WATER_RIPPLE_PATH);
            animations.preload(TURBINE_PATH);
            animations.preload(PULLED_PATH);
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
        observedActionStartTick = runtime.getActionStartTick();
        observedActionStage = runtime.getActionStage();
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

        if (runtime.getActionStartTick() != observedActionStartTick) {
            observedActionStartTick = runtime.getActionStartTick();
            actionTime = 0f;
        } else {
            actionTime += delta;
        }
        if (runtime.getActionStage() != observedActionStage) {
            observedActionStage = runtime.getActionStage();
            stageTime = 0f;
        } else {
            stageTime += delta;
        }

        int sectionSerial = boss.getHealth().getSectionBreakSerial();
        if (sectionSerial != observedSectionBreakSerial) {
            observedSectionBreakSerial = sectionSerial;
            damageTime = 0f;
            damagePlaying = true;
        }
        if (damagePlaying && (damageTime += delta) >= DAMAGE_CHUNKS_DURATION) {
            damagePlaying = false;
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
        Vector2 position = bossRenderPosition(boss);
        batch.begin();
        renderBeachSharks(batch, boss);
        renderDarkArrivalShadow(batch, boss);
        animations.draw(batch, boss.getAnimationPath(), visualClip(boss), visualClipTime(boss),
                position.x, position.y, boss.getRenderScale(), loopsBossClip(boss));
        renderEgyptMissileEffect(batch, boss);
        renderFrostMissileEffect(batch, boss);
        renderFrostWindEffect(batch, boss);
        renderFrostGlacierEffect(batch, boss);

        renderSummonEffects(batch, boss);
        renderDarkArrivalFire(batch, boss);
        renderDarkEffects(batch, boss);
        renderBeachActionEffects(batch, boss);
        if (damagePlaying) {
            animations.draw(batch, DAMAGE_CHUNKS_PATH, damageClip(), damageTime,
                    position.x, position.y, boss.getRenderScale(), false);
        }
        batch.end();
    }

    private Vector2 bossRenderPosition(Boss boss) {
        Vector2 position = geometry.entityToScreen(boss.getX(), visualBossLane(boss));
        if (isDarkBoss(boss)) {
            position.x += geometry.getTileWidth() * 0.72f;
            // The Dark Dragon PAM already contains its complete sky-entry motion.
            // Do not add a second artificial vertical translation on top of that clip.
        } else if (isBeachBoss(boss)) {
            position.x += geometry.getTileWidth() * 0.34f;
        }
        return position;
    }

    private void renderDarkArrivalShadow(Batch batch, Boss boss) {
        if (!isDarkBoss(boss) || boss.getState() != BossState.INTRO
                || stateTime >= DARK_ARRIVAL_FIRE_IMPACT_TIME) {
            return;
        }
        float progress = MathUtils.clamp(stateTime / DARK_ARRIVAL_FIRE_IMPACT_TIME, 0f, 1f);
        float eased = progress * progress * (3f - 2f * progress);
        Vector2 shadow = geometry.entityToScreen(8.1, boss.getCenterLane());
        shadow.x -= geometry.getTileWidth() * (1.15f - eased * 0.55f);
        float scale = 0.22f + eased * 0.42f;
        batch.setColor(1f, 1f, 1f, 0.25f + eased * 0.55f);
        animations.draw(
                batch, DARK_ARRIVAL_SHADOW_PATH, "animation", stateTime,
                shadow.x, shadow.y, scale, true
        );
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private double visualBossLane(Boss boss) {
        double lane = boss.getCenterLane();
        if (!isDarkBoss(boss) || boss.getState() != BossState.ACTION) {
            return lane;
        }
        if (boss.getAction() == BossAction.MOVE_LANES && runtime.getPendingFirstLane() > 0) {
            float duration = Math.max(0.1f,
                    (runtime.getStateUntilTick() - runtime.getActionStartTick()) / 10f);
            float alpha = MathUtils.clamp(actionTime / duration, 0f, 1f);
            alpha = alpha * alpha * (3f - 2f * alpha);
            double from = runtime.getMoveStartFirstLane() + 0.5;
            double to = runtime.getPendingFirstLane() + 0.5;
            return from + (to - from) * alpha;
        }
        if (boss.getAction() == BossAction.SPAWN_ZOMBIES && !runtime.getSummonTargets().isEmpty()) {
            float duration = Math.max(0.1f,
                    (runtime.getStateUntilTick() - runtime.getActionStartTick()) / 10f);
            float phase = MathUtils.clamp(actionTime / duration, 0f, 1f);
            float focus = MathUtils.sin(phase * MathUtils.PI) * 0.48f;
            return lane + (runtime.getSummonFocusLane() - lane) * focus;
        }
        return lane;
    }

    private String visualClip(Boss boss) {
        if (isDarkBoss(boss) && boss.getState() == BossState.INTRO) {
            return "intro";
        }
        if (boss.getState() == BossState.STUNNED) {
            return stunClip(boss);
        }
        if (boss.getState() != BossState.ACTION) {
            return boss.getVisualClip();
        }
        if (boss.getAction() == BossAction.MOVE_LANES) {
            if (isBeachBoss(boss)) {
                return runtime.getActionStage() == 0 ? "submerge" : "emerge";
            }
            if (runtime.getMoveDirection() != 0) {
                return boss.getMovementClip(runtime.getMoveDirection() < 0);
            }
        }
        if (boss.getAction() == BossAction.MISSILE && isEgyptBoss(boss)) {
            return stateTime < missileStartDuration ? "missile_start" : "rocket_launch";
        }
        if (boss.getAction() == BossAction.CHARGE && isEgyptBoss(boss)) {
            return stateTime < chargeForwardDuration + 0.3f ? "walk_forward" : "walk_backwards";
        }
        if (boss.getAction() == BossAction.DARK_FIREBALLS) {
            if (runtime.getActionStage() > 0) {
                return "fire_bomb_end";
            }
            float attackDuration = clipDuration(boss.getAnimationPath(), "fire_bomb", 1.8333f);
            return actionTime < attackDuration ? "fire_bomb" : "fire_bomb_loop";
        }
        if (boss.getAction() == BossAction.DARK_FIRE_BREATH) {
            if (runtime.getActionStage() > 0) {
                return "fire_attack_end";
            }
            float attackDuration = clipDuration(boss.getAnimationPath(), "fire_attack", 1.8f);
            return actionTime < attackDuration ? "fire_attack" : "fire_attack_idle";
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
        if (boss.getAction() == BossAction.BEACH_BABY_SHARKS) {
            return "spawn";
        }
        if (boss.getAction() == BossAction.BEACH_TURBINE) {
            return switch (runtime.getActionStage()) {
                case 0 -> "suction_on";
                case 1 -> "suction_loop";
                default -> "suction_off";
            };
        }
        return boss.getVisualClip();
    }

    private float visualClipTime(Boss boss) {
        if (boss.getState() == BossState.STUNNED) {
            return stunClipTime(boss);
        }
        if (boss.getState() != BossState.ACTION) {
            return stateTime;
        }
        if (isEgyptBoss(boss)) {
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
        }
        if (boss.getAction() == BossAction.SPAWN_ZOMBIES
                || boss.getAction() == BossAction.BEACH_BABY_SHARKS) {
            return actionTime;
        }
        return stageTime;
    }

    private boolean loopsBossClip(Boss boss) {
        if (boss.getState() == BossState.ACTIVE) {
            return true;
        }
        if (boss.getState() == BossState.STUNNED) {
            if (!isDarkBoss(boss) && !isBeachBoss(boss)) {
                return true;
            }
            String clip = stunClip(boss);
            return clip.endsWith("loop");
        }
        return boss.getState() == BossState.ACTION
                && (visualClip(boss).endsWith("loop") || visualClip(boss).equals("fire_attack_idle"));
    }

    private String stunClip(Boss boss) {
        if (!isDarkBoss(boss) && !isBeachBoss(boss)) {
            return boss.getVisualClip();
        }
        String on = runtime.isBeachTangleStun() && isBeachBoss(boss) ? "tangled_on" : "stun_start";
        String loop = runtime.isBeachTangleStun() && isBeachBoss(boss) ? "tangled_loop" : "stun_loop";
        String off = runtime.isBeachTangleStun() && isBeachBoss(boss) ? "tangled_off" : "stun_end";
        float onDuration = clipDuration(boss.getAnimationPath(), on, 0.35f);
        float offDuration = clipDuration(boss.getAnimationPath(), off, 0.45f);
        float remaining = Math.max(0f, (runtime.getStateUntilTick() - runtime.getCurrentTick()) / 10f);
        if (stateTime < onDuration) {
            return on;
        }
        if (remaining <= offDuration) {
            return off;
        }
        return loop;
    }

    private float stunClipTime(Boss boss) {
        if (!isDarkBoss(boss) && !isBeachBoss(boss)) {
            return stateTime;
        }
        String on = runtime.isBeachTangleStun() && isBeachBoss(boss) ? "tangled_on" : "stun_start";
        String off = runtime.isBeachTangleStun() && isBeachBoss(boss) ? "tangled_off" : "stun_end";
        float onDuration = clipDuration(boss.getAnimationPath(), on, 0.35f);
        float offDuration = clipDuration(boss.getAnimationPath(), off, 0.45f);
        float remaining = Math.max(0f, (runtime.getStateUntilTick() - runtime.getCurrentTick()) / 10f);
        if (stateTime < onDuration) {
            return stateTime;
        }
        if (remaining <= offDuration) {
            return Math.max(0f, offDuration - remaining);
        }
        return Math.max(0f, stateTime - onDuration);
    }

    private void renderEgyptMissileEffect(Batch batch, Boss boss) {
        if (!isEgyptBoss(boss) || boss.getState() != BossState.ACTION
                || boss.getAction() != BossAction.MISSILE) {
            return;
        }
        Position target = runtime.getActionTarget();
        if (target == null) {
            return;
        }
        Vector2 p = geometry.boardToScreen(target.getY(), target.getX());
        float launchTime = missileStartDuration;
        float impactTime = launchTime + missileFlightDuration;
        if (stateTime < impactTime) {
            animations.draw(batch, EGYPT_MISSILE_PATH, "missile_lock_reticle", stateTime,
                    p.x, p.y, EGYPT_MISSILE_SCALE, true);
        }
        if (stateTime >= launchTime && stateTime < impactTime) {
            animations.draw(batch, EGYPT_MISSILE_PATH, "missile", stateTime - launchTime,
                    p.x, p.y, EGYPT_MISSILE_SCALE, false);
        }
        if (stateTime >= impactTime && stateTime < impactTime + missileExplosionDuration) {
            animations.draw(batch, EGYPT_MISSILE_PATH, "missile_explosion", stateTime - impactTime,
                    p.x, p.y, EGYPT_MISSILE_SCALE, false);
        }
    }

    private void renderSummonEffects(Batch batch, Boss boss) {
        if (boss.getState() != BossState.ACTION || boss.getAction() != BossAction.SPAWN_ZOMBIES
                || runtime.getSummonTargets().isEmpty()) {
            return;
        }
        float impactTime = Math.max(0.1f,
                (runtime.getSpecialImpactTick() - runtime.getActionStartTick()) / 10f);
        for (Position target : runtime.getSummonTargets()) {
            Vector2 p = geometry.boardToScreen(target.getY(), target.getX());
            if (isDarkBoss(boss)) {
                if (actionTime < impactTime) {
                    animations.draw(batch, DARK_DIRT_PATH, "gravebuster_dirt_anim", actionTime,
                            p.x, p.y, 0.76f, false);
                } else if (actionTime < impactTime + 0.45f) {
                    animations.draw(batch, DARK_DIRT_PATH, "gravebuster_dirt_fade", actionTime - impactTime,
                            p.x, p.y, 0.76f, false);
                }
            } else if (isBeachBoss(boss)) {
                String clip = actionTime < impactTime ? "ripple" : "ripple_exit";
                float time = actionTime < impactTime ? actionTime : actionTime - impactTime;
                animations.draw(batch, WATER_RIPPLE_PATH, clip, time, p.x, p.y, 0.72f,
                        false);
            }
        }
    }

    private void renderDarkArrivalFire(Batch batch, Boss boss) {
        if (!isDarkBoss(boss) || boss.getState() != BossState.INTRO
                || stateTime < DARK_ARRIVAL_FIRE_IMPACT_TIME - 1.15f
                || stateTime > DARK_ARRIVAL_FIRE_IMPACT_TIME + 0.75f) {
            return;
        }
        float fireTime = stateTime - (DARK_ARRIVAL_FIRE_IMPACT_TIME - 1.15f);
        float scale = Math.min(
                geometry.getTileWidth() / 220f,
                geometry.getTileHeight() / 247f
        );
        for (int lane = 1; lane <= BoardGeometry.ROWS; lane++) {
            for (int column = BoardGeometry.COLUMNS - 1; column <= BoardGeometry.COLUMNS; column++) {
                Vector2 p = geometry.boardToScreen(lane, column);
                float time = fireTime + (BoardGeometry.COLUMNS - column) * 0.08f + lane * 0.03f;
                animations.draw(batch, FIRE_PEA_PATH, "idle2", time,
                        p.x, p.y, -scale, scale, true, Collections.emptyMap());
                animations.draw(batch, SNAPDRAGON_FIRE_PATH, "animation", time,
                        p.x, p.y, -scale, scale, true, Collections.emptyMap());
            }
        }
    }
    private void renderDarkEffects(Batch batch, Boss boss) {
        if (!isDarkBoss(boss) || boss.getState() != BossState.ACTION) {
            return;
        }
        if (boss.getAction() == BossAction.DARK_FIREBALLS) {
            renderDarkFireballs(batch);
        } else if (boss.getAction() == BossAction.DARK_FIRE_BREATH) {
            renderDarkFireBreath(batch, boss);
        }
    }

    private void renderDarkFireballs(Batch batch) {
        float impactTime = Math.max(0.1f,
                (runtime.getSpecialImpactTick() - runtime.getActionStartTick()) / 10f);
        Rectangle boardBounds = geometry.getBoardBounds();
        for (Position target : runtime.getActionTargets()) {
            Vector2 p = geometry.boardToScreen(target.getY(), target.getX());
            if (actionTime < impactTime) {
                float progress = MathUtils.clamp(actionTime / impactTime, 0f, 1f);
                progress = progress * progress;
                float startY = boardBounds.y + boardBounds.height + geometry.getTileHeight() * 3.2f;
                float y = MathUtils.lerp(startY, p.y, progress);
                float x = p.x + MathUtils.sin(actionTime * 5f + target.getX()) * geometry.getTileWidth() * 0.05f;
                animations.draw(batch, DARK_FIREBALL_PATH, "fall", actionTime,
                        x, y, DARK_EFFECT_SCALE, true);
            } else if (stageTime < 1.4f) {
                animations.draw(batch, DARK_FIREBALL_PATH, "impact", stageTime,
                        p.x, p.y, DARK_EFFECT_SCALE, false);
            }
        }
    }

    private void renderDarkFireBreath(Batch batch, Boss boss) {
        final float sweepStart = 0.34f;
        final float columnDelay = 0.12f;
        if (actionTime < sweepStart || actionTime > 3.0f) {
            return;
        }
        for (int lane : new int[]{boss.getFirstLane(), boss.getSecondLane()}) {
            for (int column = BoardGeometry.COLUMNS; column >= 1; column--) {
                float localTime = actionTime - sweepStart
                        - (BoardGeometry.COLUMNS - column) * columnDelay;
                if (localTime < 0f || localTime > 1.34f) {
                    continue;
                }
                Vector2 p = geometry.boardToScreen(lane, column);
                animations.draw(batch, FIRE_PEA_PATH, "idle2", localTime,
                        p.x, p.y, -DARK_BREATH_SCALE, DARK_BREATH_SCALE, true,
                        Collections.emptyMap());
                animations.draw(batch, SNAPDRAGON_FIRE_PATH, "animation", localTime,
                        p.x - geometry.getTileWidth() * 0.08f, p.y,
                        -DARK_BREATH_SCALE, DARK_BREATH_SCALE, false,
                        Collections.emptyMap());
            }
        }
    }

    private void renderBeachSharks(Batch batch, Boss boss) {
        if (!isBeachBoss(boss)) {
            return;
        }
        for (Position shark : runtime.getSharkPositions()) {
            if (boss.getState() == BossState.ACTION && boss.getAction() == BossAction.BEACH_BABY_SHARKS
                    && hasBabySharkTargetInLane(shark.getY())) {
                continue;
            }
            Vector2 p = geometry.boardToScreen(shark.getY(), shark.getX());
            p.x -= geometry.getTileWidth() * 0.16f;
            animations.draw(batch, SHARK_PATH, "idle2", stateTime + shark.getY() * 0.13f,
                    p.x, p.y, IDLE_SHARK_SCALE, true);
        }
    }

    private boolean hasBabySharkTargetInLane(int lane) {
        for (Position target : runtime.getActionTargets()) {
            if (target.getY() == lane) {
                return true;
            }
        }
        return false;
    }

    private void renderBeachActionEffects(Batch batch, Boss boss) {
        if (!isBeachBoss(boss) || boss.getState() != BossState.ACTION) {
            return;
        }
        if (boss.getAction() == BossAction.BEACH_BABY_SHARKS) {
            renderBabySharks(batch);
        } else if (boss.getAction() == BossAction.BEACH_TURBINE) {
            renderTurbine(batch, boss);
        }
    }

    private void renderBabySharks(Batch batch) {
        float impactTime = Math.max(0.1f,
                (runtime.getSpecialImpactTick() - runtime.getActionStartTick()) / 10f);
        for (Position target : runtime.getActionTargets()) {
            Vector2 end = geometry.boardToScreen(target.getY(), target.getX());
            Vector2 start = geometry.boardToScreen(target.getY(), BoardGeometry.COLUMNS);
            if (actionTime < impactTime) {
                float alpha = MathUtils.clamp(actionTime / impactTime, 0f, 1f);
                alpha = alpha * alpha * (3f - 2f * alpha);
                float x = MathUtils.lerp(start.x, end.x, alpha);
                float y = MathUtils.lerp(start.y, end.y, alpha)
                        + MathUtils.sin(alpha * MathUtils.PI) * geometry.getTileHeight() * 0.10f;
                animations.draw(batch, SHARK_PATH, "walk", actionTime,
                        x, y, SHARK_SCALE, true);
            } else if (stageTime < 2.13f) {
                animations.draw(batch, SHARK_PATH, "attack", stageTime,
                        end.x, end.y, SHARK_SCALE, false);
            }
        }
    }

    private void renderTurbine(Batch batch, Boss boss) {
        for (int lane : new int[]{boss.getFirstLane(), boss.getSecondLane()}) {
            Vector2 p = geometry.entityToScreen(6.3, lane);
            animations.draw(batch, TURBINE_PATH, "animation", stageTime,
                    p.x, p.y, TURBINE_SCALE, true);
        }
        if (runtime.getActionStage() < 1) {
            return;
        }
        for (Position target : runtime.getTurbineVictimPositions()) {
            Vector2 p = geometry.boardToScreen(target.getY(), target.getX());
            animations.draw(batch, PULLED_PATH, "animation2", stageTime,
                    p.x, p.y, 0.58f, true);
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
        return boss != null && boss.getId().equals("zomboss-egypt");
    }

    private boolean isDarkBoss(Boss boss) {
        return boss != null && boss.getId().equals("zomboss-dark");
    }

    private boolean isBeachBoss(Boss boss) {
        return boss != null && boss.getId().equals("zomboss-beach");
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
