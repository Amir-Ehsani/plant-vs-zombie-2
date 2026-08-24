package game.render.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
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

    private double lastX;
    private float visualX;
    private boolean visualInitialized;
    private float stationaryTime;

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
        return new LinkedHashMap<>(animations.visibilityForTokens(profile.getPath(), tokens));
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
