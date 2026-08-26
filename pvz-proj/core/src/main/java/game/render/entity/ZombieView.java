package game.render.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.AnimationDefinition;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.zombie.Armor;
import models.core.zombie.Zombie;
import models.engine.board.Board;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ZombieView extends EntityView<Zombie> {
    private static final double POSITION_EPSILON = 0.0001;
    private static final float EATING_DELAY = 0.16f;
    private static final float EATING_X_OFFSET = 0.10f;
    private static final float VISUAL_FOLLOW_RATE = 18f;
    private static final float TELEPORT_SNAP_DISTANCE = 1.25f;
    private static final double ARM_DETACH_HEALTH_RATIO = 0.50;
    private static final float ASH_VISUAL_SCALE = 0.64f;
    private static final String ASH_PATH =
        "768/FULL/EFFECTS/ZOMBIE_BIGHEAD_ASH/ZOMBIE_BIGHEAD_ASH.PAM";
    private static final String GARGANTUAR_ASH_PATH =
        "768/FULL/EFFECTS/ZOMBIE_BIGHEAD_GARGANTUAR_ASH/ZOMBIE_BIGHEAD_GARGANTUAR_ASH.PAM";
    private static final String IMP_ASH_PATH =
        "768/FULL/EFFECTS/ZOMBIE_BIGHEAD_IMP_ASH/ZOMBIE_BIGHEAD_IMP_ASH.PAM";

    private double lastX;
    private float visualX;
    private boolean visualInitialized;
    private float stationaryTime;
    private boolean armDetached;
    private boolean armDetachPending;
    private String detachedArmPart;
    private String detachedHeadPart;
    private String lastClip;

    public ZombieView(Zombie zombie, EntityAnimationProfile profile) {
        super(zombie, profile);
        lastX = zombie.getX();
        visualX = (float) zombie.getX();
    }

    @Override
    public void update(float delta, Board board) {
        super.update(delta, board);
        updateMovementState(delta);
        updateVisualPosition(delta);
        detectArmDetach();
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

        List<String> effects = board.getZombieEffects(entity);
        String clip = resolveClip(effects);
        lastClip = clip;
        Vector2 position = geometry.entityToScreen(visualX, entity.getY());
        float renderX = position.x + eatingOffset(geometry, clip);

        batch.setColor(resolveTint(effects));
        animations.draw(
            batch,
            profile.getPath(),
            clip,
            timeForClip(clip),
            renderX,
            position.y,
            profile.getScale(),
            true,
            resolveVisibility(animations, effects)
        );
        batch.setColor(Color.WHITE);
    }

    ZombiePartVisual takeDetachedArmVisual(PvzAnimationService animations) {
        if (!armDetachPending || animations == null) {
            return null;
        }
        armDetachPending = false;
        detachedArmPart = resolveDetachedArmPart(animations);
        if (detachedArmPart == null) {
            return null;
        }
        String clip = lastClip == null ? profile.firstClip("walk", "idle", "eat") : lastClip;
        if (clip == null) {
            return null;
        }
        return new ZombiePartVisual(
            profile,
            clip,
            detachedArmPart,
            stateTime,
            visualX,
            (int) Math.round(entity.getY())
        );
    }

    ZombieDeathVisual createDeathVisual(PvzAnimationService animations) {
        if (isExplosiveDeath()) {
            ZombieDeathVisual ash = createAshDeathVisual(animations);
            if (ash != null) {
                return ash;
            }
        }
        String clip = deathClip();
        if (clip == null) {
            return null;
        }
        return new ZombieDeathVisual(
            profile,
            clip,
            visualX,
            (int) Math.round(entity.getY()),
            armDetached ? detachedArmPart : null,
            resolveDetachedHeadPart(animations)
        );
    }

    ZombieHeadVisual createDeathHeadVisual(PvzAnimationService animations) {
        if (isExplosiveDeath() || animations == null || !profile.getDefinition().hasClip("particles")) {
            return null;
        }
        String headPart = resolveDetachedHeadPart(animations);
        if (headPart == null) {
            return null;
        }
        return new ZombieHeadVisual(
            profile,
            headPart,
            visualX,
            (int) Math.round(entity.getY())
        );
    }

    private ZombieDeathVisual createAshDeathVisual(PvzAnimationService animations) {
        if (animations == null || animations.getCatalog() == null) {
            return null;
        }
        AnimationDefinition definition = animations.getCatalog().findByPath(resolveAshPath());
        if (definition == null) {
            definition = animations.getCatalog().findByPath(ASH_PATH);
        }
        if (definition == null) {
            return null;
        }
        animations.preload(definition.getPath());
        return ZombieDeathVisual.effect(
            definition,
            "animation",
            visualX,
            (int) Math.round(entity.getY()),
            profile.getScale() * ASH_VISUAL_SCALE
        );
    }

    private String resolveAshPath() {
        String id = entity.getType() == null ? "" : normalize(entity.getType().getId());
        if (id.contains("gargantuar")) {
            return GARGANTUAR_ASH_PATH;
        }
        if (id.contains("imp")) {
            return IMP_ASH_PATH;
        }
        return ASH_PATH;
    }

    private boolean isExplosiveDeath() {
        String category = normalize(entity.getLastDamageSourcePlantCategory());
        String damageType = normalize(entity.getLastDamageType());
        return category.contains("explosive")
            || damageType.contains("explosion")
            || damageType.contains("cherrybomb")
            || damageType.contains("grapeshot")
            || damageType.contains("potatomine")
            || damageType.contains("doomshroom")
            || damageType.contains("jalapeno");
    }

    private void detectArmDetach() {
        if (armDetached || !entity.isAlive() || entity.getMaxHp() <= 0) {
            return;
        }
        double ratio = entity.getHp() / (double) entity.getMaxHp();
        if (ratio <= ARM_DETACH_HEALTH_RATIO) {
            armDetached = true;
            armDetachPending = true;
        }
    }

    private void updateMovementState(float delta) {
        double movement = Math.abs(entity.getX() - lastX);
        if (movement > POSITION_EPSILON) {
            stationaryTime = 0f;
        } else if (delta > 0f) {
            stationaryTime += delta;
        }
        lastX = entity.getX();
    }

    private void updateVisualPosition(float delta) {
        float targetX = (float) entity.getX();
        if (!visualInitialized || Math.abs(targetX - visualX) >= TELEPORT_SNAP_DISTANCE) {
            visualX = targetX;
            visualInitialized = true;
            return;
        }
        float safeDelta = Math.max(0f, delta);
        float follow = 1f - (float) Math.exp(-VISUAL_FOLLOW_RATE * safeDelta);
        visualX += (targetX - visualX) * follow;
    }

    private String resolveClip(List<String> effects) {
        if (hasNewspaperArmor()) {
            return resolveNewspaperClip(effects);
        }
        if (entity.getType().hasTag("stationary") || isMovementBlocked(effects)) {
            return profile.firstClip("idle", "walk", "eat", "play");
        }
        if (stationaryTime >= EATING_DELAY) {
            return profile.firstClip("eat", "idle", "walk", "play");
        }
        return profile.firstClip("walk", "idle", "eat", "play");
    }

    private String resolveNewspaperClip(List<String> effects) {
        if (isMovementBlocked(effects)) {
            return profile.firstClip("idle_newspaper", "walk_newspaper", "idle");
        }
        if (stationaryTime >= EATING_DELAY) {
            return profile.firstClip("eat_newspaper", "idle_newspaper", "walk_newspaper");
        }
        return profile.firstClip("walk_newspaper", "idle_newspaper", "eat_newspaper");
    }

    private boolean isMovementBlocked(List<String> effects) {
        return hasEffect(effects, "frozen") || hasEffect(effects, "buttered");
    }

    private boolean hasNewspaperArmor() {
        Armor armor = entity.getArmor();
        return armor != null && !armor.isBroken()
            && normalize(armor.getName()).contains("newspaper");
    }

    private float eatingOffset(BoardGeometry geometry, String clip) {
        return isEatingClip(clip) ? geometry.getTileWidth() * EATING_X_OFFSET : 0f;
    }

    private Color resolveTint(List<String> effects) {
        if (hasEffect(effects, "frozen")) {
            return new Color(0.50f, 0.72f, 1f, 1f);
        }
        if (hasEffect(effects, "buttered")) {
            return new Color(1f, 0.88f, 0.40f, 1f);
        }
        if (hasEffect(effects, "chilled")) {
            return new Color(0.70f, 0.90f, 1f, 1f);
        }
        if (hasEffect(effects, "poisoned")) {
            return new Color(0.70f, 1f, 0.65f, 1f);
        }
        if (hasEffect(effects, "hypnotized")) {
            return new Color(0.82f, 0.62f, 1f, 1f);
        }
        return Color.WHITE;
    }

    private Map<String, Boolean> resolveVisibility(
        PvzAnimationService animations,
        List<String> effects
    ) {
        List<String> tokens = new ArrayList<>();
        Armor armor = entity.getArmor();
        if (armor != null && !armor.isBroken()) {
            addArmorTokens(tokens, armor.getName());
            addArmorTokens(tokens, armor.getArmorType());
        }
        if (hasEffect(effects, "buttered")) {
            tokens.add("butter");
        }
        Map<String, Boolean> visibility = new LinkedHashMap<>(
            animations.visibilityForTokens(profile.getPath(), tokens)
        );
        if (armDetached) {
            String armPart = resolveDetachedArmPart(animations);
            if (armPart != null) visibility.put(armPart, false);
        }
        return visibility;
    }

    private String resolveDetachedHeadPart(PvzAnimationService animations) {
        if (detachedHeadPart == null) {
            detachedHeadPart = animations.findDetachableHeadPart(profile.getPath());
        }
        return detachedHeadPart;
    }

    private String resolveDetachedArmPart(PvzAnimationService animations) {
        if (detachedArmPart == null) {
            detachedArmPart = animations.findDetachableArmPart(profile.getPath());
        }
        return detachedArmPart;
    }

    private String deathClip() {
        String exact = findExactClip("die", "death", "zombie die", "final_die", "rare_death");
        if (exact != null) {
            return exact;
        }
        for (String clip : profile.getDefinition().getClips()) {
            String normalized = normalize(clip);
            if (normalized.contains("die") || normalized.contains("death")) {
                return clip;
            }
        }
        return null;
    }

    private String findExactClip(String... candidates) {
        for (String candidate : candidates) {
            String normalizedCandidate = normalize(candidate);
            for (String clip : profile.getDefinition().getClips()) {
                if (normalize(clip).equals(normalizedCandidate)) {
                    return clip;
                }
            }
        }
        return null;
    }

    private void addArmorTokens(List<String> tokens, String armorName) {
        String normalized = normalize(armorName);
        if (normalized.contains("cone")) {
            addTokens(tokens, "armor1", "cone");
        } else if (normalized.contains("bucket")) {
            addTokens(tokens, "armor2", "bucket");
        } else if (normalized.contains("brick")) {
            addTokens(tokens, "armor4", "brick");
        } else if (normalized.contains("shoulder") || normalized.contains("crown")
            || normalized.contains("knight")) {
            addTokens(tokens, "armor3", "crown", "shoulder");
        } else if (normalized.contains("newspaper")) {
            addTokens(tokens, "newspaper", "paper");
        } else if (normalized.contains("barrel")) {
            addTokens(tokens, "barrel");
        } else if (normalized.contains("arcade") || normalized.contains("machine")) {
            addTokens(tokens, "arcade", "machine");
        }
    }

    private void addTokens(List<String> tokens, String... values) {
        for (String value : values) {
            if (!tokens.contains(value)) {
                tokens.add(value);
            }
        }
    }

    private boolean hasEffect(List<String> effects, String prefix) {
        if (effects == null) {
            return false;
        }
        for (String effect : effects) {
            if (normalize(effect).startsWith(normalize(prefix))) {
                return true;
            }
        }
        return false;
    }

    private boolean isEatingClip(String clip) {
        return clip != null && normalize(clip).contains("eat");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
