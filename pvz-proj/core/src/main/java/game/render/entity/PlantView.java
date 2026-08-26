package game.render.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.plant.Plant;
import models.engine.board.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlantView extends EntityView<Plant> {
    private static final String ICE_BEHIND_PATH =
        "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_PLANT_BEHIND/FROSTBITE_ICE_BLOCK_PLANT_BEHIND.PAM";
    private static final String ICE_FRONT_PATH =
        "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_PLANT/FROSTBITE_ICE_BLOCK_PLANT.PAM";
    private static final String OCTOPUS_PATH =
        "768/FULL/EFFECTS/ZOMBIE_OCTOPUS_PROJECTILE/ZOMBIE_OCTOPUS_PROJECTILE.PAM";

    private final List<String> specialSequence = new ArrayList<>();
    private int previousAttackSerial;
    private int previousPlantFoodSerial;
    private int specialIndex;
    private float specialTime;

    public PlantView(Plant plant, EntityAnimationProfile profile) {
        super(plant, profile);
        previousAttackSerial = plant.getVisualAttackSerial();
        previousPlantFoodSerial = plant.getVisualPlantFoodSerial();
    }

    @Override
    public void update(float delta, Board board) {
        detectVisualActions();
        updateSpecialAnimation(delta);
        if (!entity.isDisabled()) {
            super.update(delta, board);
        }
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
        Vector2 position = geometry.entityToScreen(entity.getX(), entity.getY());
        drawFrozenBehind(batch, animations, position);
        drawPlant(batch, animations, position);
        drawFrozenFront(batch, animations, position);
        drawOctopus(batch, animations, position);
    }

    private void detectVisualActions() {
        int plantFoodSerial = entity.getVisualPlantFoodSerial();
        if (plantFoodSerial != previousPlantFoodSerial) {
            previousPlantFoodSerial = plantFoodSerial;
            startPlantFoodAnimation();
        }
        int attackSerial = entity.getVisualAttackSerial();
        if (attackSerial != previousAttackSerial) {
            previousAttackSerial = attackSerial;
            if (specialSequence.isEmpty()) {
                startAttackAnimation();
            }
        }
    }

    private void startAttackAnimation() {
        String requested = entity.getVisualAttackClip();
        String clip = findClip(requested);
        if (usesPersistentPeashooterPlantFood()) {
            clip = findClip("plantfood");
        } else if (entity.isBoosted() && findClip("attack_plantfood") != null) {
            clip = findClip("attack_plantfood");
        }
        if (clip == null) {
            clip = findClip("attack");
        }
        startSequence(clip);
    }

    private void startPlantFoodAnimation() {
        List<String> clips = buildPlantFoodSequence();
        startSequence(clips.toArray(new String[0]));
    }

    private List<String> buildPlantFoodSequence() {
        List<String> clips = new ArrayList<>();
        String start = firstClip("plantfood_on", "plantfood_start", "plantfoodON");
        String core = resolvePlantFoodCoreClip();
        String loop = firstClip("plantfood_loop", "plantfood_idle");
        String end = firstClip("plantfood_off", "plantfood_end", "plantfoodOFF");
        addUnique(clips, start);
        addUnique(clips, core);
        addUnique(clips, loop);
        addUnique(clips, end);
        if (clips.isEmpty()) {
            addUnique(clips, findFirstPlantFoodClip());
        }
        return clips;
    }

    private String resolvePlantFoodCoreClip() {
        double ratio = entity.getMaxHp() <= 0 ? 1.0 : entity.getHp() / (double) entity.getMaxHp();
        if (ratio <= 0.33 && findClip("plantfood3") != null) {
            return findClip("plantfood3");
        }
        if (ratio <= 0.66 && findClip("plantfood2") != null) {
            return findClip("plantfood2");
        }
        return firstClip("plantfood", "plantfood_stage3", "plantfood1");
    }

    private void addUnique(List<String> clips, String clip) {
        if (clip != null && !clips.contains(clip)) {
            clips.add(clip);
        }
    }

    private void startSequence(String... clips) {
        specialSequence.clear();
        if (clips != null) {
            for (String clip : clips) {
                if (clip != null && !specialSequence.contains(clip)) {
                    specialSequence.add(clip);
                }
            }
        }
        specialIndex = 0;
        specialTime = 0f;
    }

    private void updateSpecialAnimation(float delta) {
        if (specialSequence.isEmpty() || delta <= 0f) {
            return;
        }
        specialTime += delta;
        String clip = currentSpecialClip();
        float duration = Math.max(0.05f, profile.getDefinition().getClipDuration(clip));
        if (specialTime < duration) {
            return;
        }
        specialTime = 0f;
        specialIndex++;
        if (specialIndex >= specialSequence.size()) {
            specialSequence.clear();
            specialIndex = 0;
        }
    }

    private String currentSpecialClip() {
        if (specialSequence.isEmpty() || specialIndex >= specialSequence.size()) {
            return null;
        }
        return specialSequence.get(specialIndex);
    }

    private void drawPlant(Batch batch, PvzAnimationService animations, Vector2 position) {
        String specialClip = currentSpecialClip();
        String clip = specialClip == null ? resolveIdleOrDamageClip() : specialClip;
        float clipTime = specialClip == null ? timeForClip(clip) : specialTime;
        batch.setColor(resolveTint());
        animations.draw(
            batch,
            profile.getPath(),
            clip,
            clipTime,
            position.x,
            position.y,
            profile.getScale(),
            specialClip == null
        );
        batch.setColor(Color.WHITE);
    }

    private void drawFrozenBehind(Batch batch, PvzAnimationService animations, Vector2 position) {
        if (entity.isFrozenByZombie()) {
            animations.draw(batch, ICE_BEHIND_PATH, "idle", stateTime, position.x, position.y, 0.48f, true);
        }
    }

    private void drawFrozenFront(Batch batch, PvzAnimationService animations, Vector2 position) {
        if (entity.isFrozenByZombie()) {
            animations.draw(batch, ICE_FRONT_PATH, "freeze_idle", stateTime, position.x, position.y, 0.48f, true);
        }
    }

    private void drawOctopus(Batch batch, PvzAnimationService animations, Vector2 position) {
        if (entity.isCoveredByOctopus()) {
            animations.draw(batch, OCTOPUS_PATH, "animation3", stateTime, position.x, position.y, 0.42f, true);
        }
    }

    private String resolveIdleOrDamageClip() {
        if (usesPersistentPeashooterPlantFood()) {
            return findClip("plantfood");
        }
        if (entity.isBoosted()) {
            String boostedIdle = firstClip("idle_plantfood", "plantfood_idle");
            if (boostedIdle != null) {
                return boostedIdle;
            }
        }
        String damageClip = resolveDamageClip();
        if (damageClip != null) {
            return damageClip;
        }
        return profile.firstClip("idle", "idle2", "idle_stage1", "stage1_idle", "loop", "animation", "charge");
    }

    private boolean usesPersistentPeashooterPlantFood() {
        return entity.isBoosted()
            && normalize(entity.getName()).equals("peashooter")
            && findClip("plantfood") != null;
    }

    private String resolveDamageClip() {
        if (entity.getMaxHp() <= 0) {
            return null;
        }
        double ratio = entity.getHp() / (double) entity.getMaxHp();
        if (ratio <= 0.25) {
            return firstClip("idle_damage3", "idle2_damage3", "damage3");
        }
        if (ratio <= 0.50) {
            return firstClip("idle_damage2", "idle2_damage2", "damage2");
        }
        if (ratio <= 0.75) {
            return firstClip("idle_damage", "idle2_damage", "damage");
        }
        return null;
    }

    private String firstClip(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            String clip = findClip(candidate);
            if (clip != null) {
                return clip;
            }
        }
        return null;
    }

    private String findClip(String candidate) {
        String wanted = normalize(candidate);
        if (wanted.isEmpty()) {
            return null;
        }
        for (String clip : profile.getDefinition().getClips()) {
            if (normalize(clip).equals(wanted)) {
                return clip;
            }
        }
        return null;
    }

    private String findFirstPlantFoodClip() {
        for (String clip : profile.getDefinition().getClips()) {
            if (normalize(clip).contains("plantfood")) {
                return clip;
            }
        }
        return null;
    }

    private Color resolveTint() {
        if (entity.getIceHits() == 1) {
            return new Color(0.82f, 0.92f, 1f, 1f);
        }
        if (entity.getIceHits() == 2) {
            return new Color(0.68f, 0.84f, 1f, 1f);
        }
        if (entity.isTransformedToCat()) {
            return new Color(0.70f, 0.60f, 0.85f, 1f);
        }
        return Color.WHITE;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
