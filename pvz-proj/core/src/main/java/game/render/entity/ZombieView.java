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
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;

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
    private static final double ARMOR_DAMAGE_STAGE_TWO_RATIO = 0.66;
    private static final double ARMOR_DAMAGE_STAGE_THREE_RATIO = 0.33;
    private static final double NEAR_FINISH_BOARD_X = 1.65;
    private static final Color NEAR_FINISH_COLOR = new Color(1f, 0.28f, 0.24f, 1f);
    private static final float ASH_VISUAL_SCALE = 0.50f;
    private static final float ELECTRIC_ASH_VISUAL_SCALE = 0.72f;
    private static final float ELECTRIC_CLOUD_SCALE = 0.58f;
    private static final String ELECTRIC_CLOUD_PATH =
        "768/INITIAL/EFFECTS/ELECTRICBLUEBERRY_CLOUD_PROJECTILE/"
            + "ELECTRICBLUEBERRY_CLOUD_PROJECTILE.PAM";
    private static final String ELECTRIC_ASH_PATH =
        "768/INITIAL/EFFECTS/ZOMBIE_ASH/ZOMBIE_ASH.PAM";
    private static final String ELECTRIC_BIG_ASH_PATH =
        "768/INITIAL/EFFECTS/ZOMBIE_BIG_ASH/ZOMBIE_BIG_ASH.PAM";
    private static final String ELECTRIC_GARGANTUAR_ASH_PATH =
        "768/INITIAL/EFFECTS/ZOMBIE_GARGANTUAR_ASH/ZOMBIE_GARGANTUAR_ASH.PAM";
    private static final String ELECTRIC_IMP_ASH_PATH =
        "768/INITIAL/EFFECTS/ZOMBIE_IMP_ASH/ZOMBIE_IMP_ASH.PAM";
    private static final String ASH_PATH =
        "768/FULL/EFFECTS/ZOMBIE_BIGHEAD_ASH/ZOMBIE_BIGHEAD_ASH.PAM";
    private static final String GARGANTUAR_ASH_PATH =
        "768/FULL/EFFECTS/ZOMBIE_BIGHEAD_GARGANTUAR_ASH/ZOMBIE_BIGHEAD_GARGANTUAR_ASH.PAM";
    private static final String IMP_ASH_PATH =
        "768/FULL/EFFECTS/ZOMBIE_BIGHEAD_IMP_ASH/ZOMBIE_BIGHEAD_IMP_ASH.PAM";

    private double lastX;
    private double lastY;
    private float visualX;
    private float visualY;
    private boolean visualInitialized;
    private float stationaryTime;
    private boolean armDetached;
    private boolean armDetachPending;
    private String detachedArmPart;
    private String detachedHeadPart;
    private String lastClip;
    private int previousArmorHp;
    private int armorMaximumHp;
    private String armorSignature;
    private boolean armorBreakPending;
    private int detachedArmorStage;
    private float electricStrikeTime;
    private boolean electricStrikeActive;
    private boolean electricCloudPreloaded;

    public ZombieView(Zombie zombie, EntityAnimationProfile profile) {
        super(zombie, profile);
        lastX = zombie.getX();
        lastY = zombie.getY();
        visualX = (float) zombie.getX();
        visualY = (float) zombie.getY();
    }

    @Override
    public void update(float delta, Board board) {
        updateHitFlash(delta, totalVisualHealth());
        super.update(delta, board);
        updateMovementState(delta);
        updateVisualPosition(delta);
        updateElectricStrike(delta, board);
        updateArmorTracking();
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
        String clip = resolveClip(effects, board);
        lastClip = clip;
        Vector2 position = geometry.entityToScreen(visualX, visualY);
        boolean reversed = isReversed(effects);
        float direction = reversed ? -1f : 1f;
        float renderX = position.x + eatingOffset(geometry, clip) * direction;

        float clipTime = timeForClip(clip);
        Map<String, Boolean> visibility = resolveVisibility(animations, effects);
        batch.setColor(resolveTint(effects));
        animations.draw(
            batch,
            profile.getPath(),
            clip,
            clipTime,
            renderX,
            position.y,
            profile.getScale() * direction,
            profile.getScale(),
            true,
            visibility
        );
        batch.setColor(Color.WHITE);
        if (beginHitFlash(batch)) {
            animations.draw(
                batch,
                profile.getPath(),
                clip,
                clipTime,
                renderX,
                position.y,
                profile.getScale() * direction,
                profile.getScale(),
                true,
                visibility
            );
            endHitFlash(batch);
        }
        drawElectricStrike(batch, geometry, animations, position);
    }

    private int totalVisualHealth() {
        Armor armor = entity.getArmor();
        int armorHp = armor == null ? 0 : Math.max(0, armor.getHp());
        return Math.max(0, entity.getHp()) + armorHp;
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

    List<ZombiePartVisual> takeDetachedArmorVisuals(PvzAnimationService animations) {
        detectArmorBreakAfterRemoval();
        if (!armorBreakPending || animations == null) {
            return List.of();
        }
        armorBreakPending = false;
        List<String> tokens = currentArmorTokens();
        if (tokens.isEmpty()) {
            return List.of();
        }
        List<String> parts = animations.findArmorPartsForStage(
            profile.getPath(), tokens, detachedArmorStage
        );
        if (parts.isEmpty()) {
            return List.of();
        }
        String clip = lastClip == null ? profile.firstClip("walk", "idle", "eat") : lastClip;
        if (clip == null) {
            return List.of();
        }
        List<ZombiePartVisual> visuals = new ArrayList<>();
        for (String part : parts) {
            visuals.add(new ZombiePartVisual(
                profile, clip, part, stateTime, visualX, (int) Math.round(entity.getY())
            ));
        }
        return visuals;
    }

    ZombieDeathVisual createDeathVisual(PvzAnimationService animations) {
        if (isCrushDeath()) {
            String flattenedClip = profile.firstClip("idle", "walk", "eat", "play");
            ZombieDeathVisual squashed = ZombieDeathVisual.squashed(
                profile, flattenedClip, visualX, (int) Math.round(entity.getY())
            );
            if (squashed != null) {
                return squashed;
            }
        }
        if (isElectricBurnDeath()) {
            ZombieDeathVisual burn = createElectricBurnDeathVisual(animations);
            if (burn != null) {
                return burn;
            }
        }
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
        if (isCrushDeath() || isExplosiveDeath() || isElectricBurnDeath() || animations == null
                || !profile.getDefinition().hasClip("particles")) {
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

    private ZombieDeathVisual createElectricBurnDeathVisual(PvzAnimationService animations) {
        if (animations == null || animations.getCatalog() == null) {
            return null;
        }
        AnimationDefinition definition = animations.getCatalog().findByPath(resolveElectricAshPath());
        if (definition == null) {
            definition = animations.getCatalog().findByPath(ELECTRIC_ASH_PATH);
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
            profile.getScale() * ELECTRIC_ASH_VISUAL_SCALE
        );
    }

    private String resolveElectricAshPath() {
        String id = entity.getType() == null ? "" : normalize(entity.getType().getId());
        if (id.contains("gargantuar")) {
            return ELECTRIC_GARGANTUAR_ASH_PATH;
        }
        if (id.contains("imp")) {
            return ELECTRIC_IMP_ASH_PATH;
        }
        if (profile.getScale() > 0.58f) {
            return ELECTRIC_BIG_ASH_PATH;
        }
        return ELECTRIC_ASH_PATH;
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

    private boolean isElectricBurnDeath() {
        String damageType = normalize(entity.getLastDamageType());
        return damageType.contains("electric") || damageType.contains("lightning");
    }

    private boolean isCrushDeath() {
        String damageType = normalize(entity.getLastDamageType());
        return damageType.contains("crush") || damageType.contains("squash");
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

    private void updateElectricStrike(float delta, Board board) {
        List<String> effects = board == null ? List.of() : board.getZombieEffects(entity);
        boolean active = hasEffect(effects, "electric-strike");
        if (active && !electricStrikeActive) {
            electricStrikeTime = 0f;
        }
        if (active && delta > 0f) {
            electricStrikeTime += delta;
        }
        electricStrikeActive = active;
    }

    private void drawElectricStrike(
        Batch batch, BoardGeometry geometry, PvzAnimationService animations, Vector2 position
    ) {
        if (!electricStrikeActive || animations == null) {
            return;
        }
        if (!electricCloudPreloaded) {
            animations.preload(ELECTRIC_CLOUD_PATH);
            electricCloudPreloaded = true;
        }
        String clip = electricStrikeTime < 0.9f ? "start" : "attack";
        float clipTime = clip.equals("start") ? electricStrikeTime : electricStrikeTime - 0.9f;
        animations.draw(
            batch,
            ELECTRIC_CLOUD_PATH,
            clip,
            clipTime,
            position.x,
            position.y + geometry.getTileHeight() * 0.42f,
            ELECTRIC_CLOUD_SCALE,
            false
        );
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

    private void detectArmorBreakAfterRemoval() {
        Armor armor = entity.getArmor();
        if (!armorBreakPending && armor != null && previousArmorHp > 0 && armor.isBroken()) {
            armorBreakPending = true;
            detachedArmorStage = 2;
            previousArmorHp = 0;
        }
    }

    private void initializeArmorTracking() {
        Armor armor = entity.getArmor();
        previousArmorHp = armor == null ? 0 : Math.max(0, armor.getHp());
        armorSignature = armorSignature(armor);
        armorMaximumHp = resolveArmorMaximumHp(armor);
    }

    private void updateArmorTracking() {
        Armor armor = entity.getArmor();
        String signature = armorSignature(armor);
        int currentHp = armor == null ? 0 : Math.max(0, armor.getHp());
        if (!signature.equals(armorSignature)) {
            armorSignature = signature;
            previousArmorHp = currentHp;
            armorMaximumHp = resolveArmorMaximumHp(armor);
            return;
        }
        if (currentHp > armorMaximumHp) {
            armorMaximumHp = currentHp;
        }
        if (previousArmorHp > 0 && currentHp == 0) {
            armorBreakPending = true;
            detachedArmorStage = 2;
        }
        previousArmorHp = currentHp;
    }

    private int armorDamageStage(Armor armor) {
        if (armor == null || armor.isBroken()) {
            return 2;
        }
        int maximumHp = Math.max(Math.max(1, armorMaximumHp), armor.getHp());
        double ratio = armor.getHp() / (double) maximumHp;
        if (ratio > ARMOR_DAMAGE_STAGE_TWO_RATIO) {
            return 0;
        }
        if (ratio > ARMOR_DAMAGE_STAGE_THREE_RATIO) {
            return 1;
        }
        return 2;
    }

    private int resolveArmorMaximumHp(Armor armor) {
        if (armor == null) {
            return 0;
        }
        String normalized = normalize(armor.getName());
        if (normalized.contains("cone")) {
            return Math.max(370, armor.getHp());
        }
        if (normalized.contains("bucket")) {
            return Math.max(1100, armor.getHp());
        }
        if (normalized.contains("brick")) {
            return Math.max(2200, armor.getHp());
        }
        if (normalized.contains("newspaper")) {
            return Math.max(800, armor.getHp());
        }
        if (normalized.contains("crown") && normalized.contains("shoulder")) {
            return Math.max(3200, armor.getHp());
        }
        if (normalized.contains("crown") || normalized.contains("shoulder")) {
            return Math.max(1600, armor.getHp());
        }
        if (normalized.contains("barrel") || normalized.contains("arcade")) {
            return Math.max(1100, armor.getHp());
        }
        return Math.max(1, armor.getHp());
    }

    private String armorSignature(Armor armor) {
        if (armor == null) {
            return "";
        }
        return normalize(armor.getName()) + "|" + normalize(armor.getArmorType());
    }

    private List<String> currentArmorTokens() {
        Armor armor = entity.getArmor();
        if (armor == null) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        addArmorTokens(tokens, armor.getName());
        addArmorTokens(tokens, armor.getArmorType());
        return tokens;
    }

    private void updateMovementState(float delta) {
        double movement = Math.abs(entity.getX() - lastX) + Math.abs(entity.getY() - lastY);
        if (movement > POSITION_EPSILON) {
            stationaryTime = 0f;
        } else if (delta > 0f) {
            stationaryTime += delta;
        }
        lastX = entity.getX();
        lastY = entity.getY();
    }

    private void updateVisualPosition(float delta) {
        float targetX = (float) entity.getX();
        float targetY = (float) entity.getY();
        if (!visualInitialized
                || Math.abs(targetX - visualX) >= TELEPORT_SNAP_DISTANCE
                || Math.abs(targetY - visualY) >= 1.1f) {
            visualX = targetX;
            visualY = targetY;
            visualInitialized = true;
            return;
        }
        float safeDelta = Math.max(0f, delta);
        float follow = 1f - (float) Math.exp(-VISUAL_FOLLOW_RATE * safeDelta);
        visualX += (targetX - visualX) * follow;
        visualY += (targetY - visualY) * follow;
    }

    private String resolveClip(List<String> effects, Board board) {
        if (hasNewspaperArmor()) {
            return resolveNewspaperClip(effects);
        }
        String pushClip = resolvePushClip(effects, board);
        if (pushClip != null) {
            return pushClip;
        }
        if (entity.getType().hasTag("stationary") || isMovementBlocked(effects)) {
            return profile.firstClip("idle", "walk", "eat", "play");
        }
        if (stationaryTime >= EATING_DELAY) {
            return profile.firstClip("eat", "idle", "walk", "play");
        }
        return profile.firstClip("walk", "idle", "eat", "play");
    }

    private String resolvePushClip(List<String> effects, Board board) {
        if (isMovementBlocked(effects) || stationaryTime >= EATING_DELAY) {
            return null;
        }
        String name = normalize(entity.getName());
        if (name.equals("arcade") && entity.hasArmor()) {
            return profile.firstClip("push", "walk", "idle");
        }
        if (name.equals("troglobite") && hasIceBlockAhead(board)) {
            return profile.firstClip("push", "walk", "idle");
        }
        return null;
    }

    private boolean hasIceBlockAhead(Board board) {
        if (board == null) {
            return false;
        }
        int lane = Math.max(1, Math.min(board.getHeight(), (int) Math.round(entity.getY())));
        int currentX = Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(entity.getX())));
        if (currentX <= 1) {
            return false;
        }
        Tile tile = board.getTileAt(new Position(currentX - 1, lane));
        return tile != null && tile.getTileType() == TileType.ICE;
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

    private boolean isReversed(List<String> effects) {
        return hasEffect(effects, "hypnotized")
            || hasEffect(effects, "charmed")
            || hasEffect(effects, "confused");
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
        Color tint = Color.WHITE;
        if (hasEffect(effects, "frozen")) {
            tint = new Color(0.50f, 0.72f, 1f, 1f);
        } else if (hasEffect(effects, "buttered")) {
            tint = new Color(1f, 0.88f, 0.40f, 1f);
        } else if (hasEffect(effects, "chilled")) {
            tint = new Color(0.70f, 0.90f, 1f, 1f);
        } else if (hasEffect(effects, "poisoned")) {
            tint = new Color(0.70f, 1f, 0.65f, 1f);
        } else if (hasEffect(effects, "electric-strike")) {
            tint = ((int) (electricStrikeTime * 14f) & 1) == 0
                ? new Color(0.82f, 0.94f, 1f, 1f)
                : new Color(1f, 1f, 1f, 1f);
        } else if (hasEffect(effects, "hypnotized")) {
            tint = new Color(0.82f, 0.62f, 1f, 1f);
        }
        return applyNearFinishWarning(tint, effects);
    }

    private Color applyNearFinishWarning(Color baseTint, List<String> effects) {
        if (entity.getX() > NEAR_FINISH_BOARD_X || isReversed(effects)) {
            return baseTint;
        }
        float pulse = 0.46f + 0.12f * (float) Math.sin(stateTime * 8f);
        return new Color(baseTint).lerp(NEAR_FINISH_COLOR, pulse);
    }

    private Map<String, Boolean> resolveVisibility(
        PvzAnimationService animations,
        List<String> effects
    ) {
        Map<String, Boolean> visibility = new LinkedHashMap<>();
        Armor armor = entity.getArmor();
        if (armor != null && !armor.isBroken()) {
            List<String> armorTokens = new ArrayList<>();
            addArmorTokens(armorTokens, armor.getName());
            addArmorTokens(armorTokens, armor.getArmorType());
            visibility.putAll(animations.visibilityForArmorStage(
                profile.getPath(), armorTokens, armorDamageStage(armor)
            ));
        }
        if (hasEffect(effects, "buttered")) {
            visibility.putAll(animations.visibilityForTokens(
                profile.getPath(), List.of("butter")
            ));
        }
        if (armDetached) {
            String armPart = resolveDetachedArmPart(animations);
            if (armPart != null) {
                visibility.put(armPart, false);
            }
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
