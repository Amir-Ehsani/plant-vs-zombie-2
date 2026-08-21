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

    private double lastX;
    private float stationaryTime;

    public ZombieView(Zombie zombie, EntityAnimationProfile profile) {
        super(zombie, profile);
        lastX = zombie.getX();
    }

    @Override
    public void update(float delta, Board board) {
        super.update(delta, board);
        double movement = Math.abs(entity.getX() - lastX);
        if (movement > POSITION_EPSILON) {
            stationaryTime = 0f;
        } else if (delta > 0f) {
            stationaryTime += delta;
        }
        lastX = entity.getX();
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
        Vector2 position = geometry.entityToScreen(entity.getX(), entity.getY());
        batch.setColor(resolveTint(effects));
        animations.draw(
            batch,
            profile.getPath(),
            clip,
            timeForClip(clip),
            position.x,
            position.y,
            profile.getScale(),
            true,
            resolveVisibility(animations, effects)
        );
        batch.setColor(Color.WHITE);
    }

    private String resolveClip(List<String> effects) {
        if (entity.getType().hasTag("stationary")) {
            return profile.firstClip("idle", "walk", "eat");
        }
        if (hasEffect(effects, "frozen") || hasEffect(effects, "buttered")) {
            return profile.firstClip("idle", "walk", "eat");
        }
        if (stationaryTime >= EATING_DELAY) {
            return profile.firstClip("eat", "idle", "walk");
        }
        return profile.firstClip("walk", "idle", "eat");
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
        Map<String, Boolean> result = new LinkedHashMap<>();
        List<String> tokens = new ArrayList<>();
        Armor armor = entity.getArmor();
        if (armor != null && !armor.isBroken()) {
            addArmorTokens(tokens, armor.getName());
        }
        if (hasEffect(effects, "buttered")) {
            tokens.add("butter");
        }
        result.putAll(animations.visibilityForTokens(profile.getPath(), tokens));
        return result;
    }

    private void addArmorTokens(List<String> tokens, String armorName) {
        String normalized = armorName == null ? "" : armorName.toLowerCase(Locale.ROOT);
        if (normalized.contains("cone")) {
            tokens.add("armor1");
            tokens.add("cone");
        } else if (normalized.contains("bucket")) {
            tokens.add("armor2");
            tokens.add("bucket");
        } else if (normalized.contains("brick")) {
            tokens.add("armor4");
            tokens.add("brick");
        } else if (normalized.contains("shoulder")
            || normalized.contains("crown")
            || normalized.contains("knight")) {
            tokens.add("armor3");
            tokens.add("crown");
            tokens.add("shoulder");
        } else if (normalized.contains("newspaper")) {
            tokens.add("newspaper");
            tokens.add("paper");
        } else if (normalized.contains("barrel")) {
            tokens.add("barrel");
        }
    }

    private boolean hasEffect(List<String> effects, String prefix) {
        if (effects == null) {
            return false;
        }
        for (String effect : effects) {
            if (effect != null && effect.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
