package game.render.entity;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.plant.Plant;
import models.core.plant.PlantActionTiming;
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
    private static final String WIZARD_SHEEP_PATH =
        "768/FULL/EFFECTS/DARK_WIZARD_SHEEPENING/DARK_WIZARD_SHEEPENING.PAM";
    private static final float SHEEP_TRANSFORM_SECONDS = 1.70f;
    private static final float SHEEP_RESTORE_SECONDS = 1.10f;
    private static final float BONK_CHOY_PLANT_FOOD_LOOPS = 3f;
    private static final float CHOMPER_DIGEST_VISUAL_SECONDS = 40f;
    private static final Color NUT_ARMOR_TINT = new Color(0.72f, 0.78f, 0.86f, 0.92f);
    private static final float NUT_ARMOR_SHELL_SCALE = 1.08f;
    private static final String PLANT_FOOD_GLOW_PATH =
        "768/INITIAL/EFFECTS/PLANTFOOD_FX/PLANTFOOD_FX.PAM";
    private static final float PLANT_FOOD_GLOW_SCALE = 0.52f;
    private static final float PLANT_FOOD_GLOW_ON_SECONDS = 0.2333f;
    private static final float PLANT_FOOD_GLOW_OFF_START = 4.75f;
    private static final String FIRE_PEA_ROW_PATH =
        "768/INITIAL/EFFECTS/FIREPEASHOOTER_FIRE/FIREPEASHOOTER_FIRE.PAM";
    private static final String PHAT_BEET_ATTACK_PULSE_PATH =
        "768/FULL/EFFECTS/PHATBEETS_ATTACK_PULSE/PHATBEETS_ATTACK_PULSE.PAM";
    private static final String PHAT_BEET_PF_PULSE_PATH =
        "768/FULL/EFFECTS/PHATBEETS_PF_PULSE/PHATBEETS_PF_PULSE.PAM";
    private static final String KIWIBEAST_ATTACK_PULSE_PATH =
        "768/INITIAL/EFFECTS/KIWIBEAST_ATTACK_PULSE/KIWIBEAST_ATTACK_PULSE.PAM";
    private static final String KIWIBEAST_PF_PULSE_PATH =
        "768/INITIAL/EFFECTS/KIWIBEAST_PF_PULSE/KIWIBEAST_PF_PULSE.PAM";
    private static final String POTATO_MINE_EXPLOSION_PATH =
        "768/INITIAL/EFFECTS/POTATOMINE_EXPLOSION/POTATOMINE_EXPLOSION.PAM";
    private static final String PRIMAL_POTATO_MINE_EXPLOSION_PATH =
        "768/INITIAL/EFFECTS/PRIMAL_POTATOMINE_EXPLOSION/PRIMAL_POTATOMINE_EXPLOSION.PAM";
    private static final String SNOW_PEA_PLANT_FOOD_SLOW_PATH =
        "768/INITIAL/EFFECTS/SNOWPEA_PLANTFOOD_SLOW/SNOWPEA_PLANTFOOD_SLOW.PAM";
    private static final String GOO_PEA_PLANT_FOOD_TILE_PATH =
        "768/INITIAL/EFFECTS/GOOPEASHOOTER_PLANTFOOD_TILE/GOOPEASHOOTER_PLANTFOOD_TILE.PAM";
    private static final String FUME_SHROOM_BUBBLES_PATH =
        "768/INITIAL/EFFECTS/FUMESHROOM_BUBBLES/FUMESHROOM_BUBBLES.PAM";
    private static final String CITRON_PLANT_FOOD_SHOCK_PATH =
        "768/FULL/EFFECTS/CITRON_PLANTFOOD_SHOCK/CITRON_PLANTFOOD_SHOCK.PAM";
    private static final String SUN_BEAN_OVERLAY_ONE_PATH =
        "768/FULL/EFFECTS/SUNBEAN_PLANTFOOD_EFFECT_OVERLAY1/SUNBEAN_PLANTFOOD_EFFECT_OVERLAY1.PAM";
    private static final String SUN_BEAN_OVERLAY_TWO_PATH =
        "768/FULL/EFFECTS/SUNBEAN_PLANTFOOD_EFFECT_OVERLAY2/SUNBEAN_PLANTFOOD_EFFECT_OVERLAY2.PAM";
    private static final String GENERIC_EXPLOSION_BACK_PATH =
        "768/INITIAL/EFFECTS/GENERIC_EXPLOSION_BACK/GENERIC_EXPLOSION_BACK.PAM";
    private static final String GENERIC_EXPLOSION_FRONT_PATH =
        "768/INITIAL/EFFECTS/GENERIC_EXPLOSION_FRONT/GENERIC_EXPLOSION_FRONT.PAM";
    private static final float GENERIC_EXPLOSION_SECONDS = 3f;

    private final List<String> specialSequence = new ArrayList<>();
    private int previousAttackSerial;
    private int previousPlantFoodSerial;
    private int previousSpecialSerial;
    private int specialIndex;
    private float specialTime;
    private float plantFoodEffectTime = -1f;
    private float armorExplosionTime = -1f;
    private int squashLandingDirection;
    private boolean previousSheepState;
    private float sheepTransformTime = -1f;
    private float sheepRestoreTime = -1f;
    private float sheepIdleTime;
    private final boolean showDamageAppearance;
    private final boolean smoothMovement;
    private double visualX;
    private double visualY;

    public PlantView(Plant plant, EntityAnimationProfile profile) {
        this(plant, profile, true, false);
    }

    public PlantView(Plant plant, EntityAnimationProfile profile, boolean showDamageAppearance) {
        this(plant, profile, showDamageAppearance, false);
    }

    public PlantView(
            Plant plant,
            EntityAnimationProfile profile,
            boolean showDamageAppearance,
            boolean smoothMovement
    ) {
        super(plant, profile);
        this.showDamageAppearance = showDamageAppearance;
        this.smoothMovement = smoothMovement;
        visualX = plant.getX();
        visualY = smoothMovement ? 0.15 : plant.getY();
        previousAttackSerial = plant.getVisualAttackSerial();
        previousPlantFoodSerial = plant.getVisualPlantFoodSerial();
        previousSpecialSerial = plant.getVisualSpecialSerial();
        previousSheepState = plant.isTransformedToSheep();
    }

    @Override
    public void update(float delta, Board board) {
        updateVisualPosition(delta);
        updateHitFlash(delta, entity.getHp() + entity.getArmorHp());
        detectVisualActions();
        updateWizardSheepState(delta);
        updateSpecialAnimation(delta);
        if (plantFoodEffectTime >= 0f && delta > 0f) {
            plantFoodEffectTime += delta;
            if (plantFoodEffectTime > 6f) {
                plantFoodEffectTime = -1f;
            }
        }
        if (armorExplosionTime >= 0f && delta > 0f) {
            armorExplosionTime += delta;
            if (armorExplosionTime >= GENERIC_EXPLOSION_SECONDS) {
                armorExplosionTime = -1f;
            }
        }
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
        Vector2 position = geometry.entityToScreen(visualX, visualY);
        Vector2 squashOffset = squashVisualOffset(geometry);
        position.x += squashOffset.x;
        position.y += squashOffset.y;
        if (drawWizardSheep(batch, animations, position)) {
            return;
        }
        drawFrozenBehind(batch, animations, position);
        drawPlantFoodGlow(batch, animations, position);
        drawArmorExplosion(batch, animations, position, false);
        drawPlant(batch, animations, position, board);
        drawActionEffects(batch, geometry, animations, board, position);
        drawArmorExplosion(batch, animations, position, true);
        drawFrozenFront(batch, animations, position);
        drawOctopus(batch, animations, position);
    }


    private void updateVisualPosition(float delta) {
        if (!smoothMovement) {
            visualX = entity.getX();
            visualY = entity.getY();
            return;
        }
        float alpha = 1f - (float) Math.exp(-7f * Math.max(0f, delta));
        visualX += (entity.getX() - visualX) * alpha;
        visualY += (entity.getY() - visualY) * alpha;
        if (Math.abs(entity.getX() - visualX) < 0.01) {
            visualX = entity.getX();
        }
        if (Math.abs(entity.getY() - visualY) < 0.01) {
            visualY = entity.getY();
        }
    }

    private void detectVisualActions() {
        int specialSerial = entity.getVisualSpecialSerial();
        if (specialSerial != previousSpecialSerial) {
            previousSpecialSerial = specialSerial;
            if (normalize(entity.getName()).equals("explodeonut")
                    && normalize(entity.getVisualSpecialClip()).equals("attack")) {
                armorExplosionTime = 0f;
            }
            startSpecialAnimation();
        }
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

    private void startSpecialAnimation() {
        String requested = entity.getVisualSpecialClip();
        String name = normalize(entity.getName());
        if (name.equals("squash") && normalize(requested).startsWith("jumpup")) {
            boolean left = normalize(requested).contains("left");
            squashLandingDirection = left ? -1 : 1;
            startSequence(
                firstClip("size_up", "turn"),
                firstClip(left ? "jump_up_left" : "jump_up_right", "jump_up_right"),
                firstClip(left ? "jump_down_left" : "jump_down_right", "jump_down_right")
            );
            return;
        }
        if (name.equals("tanglekelp") && normalize(requested).equals("attacksubmerge")) {
            startSequence(
                firstClip("attack_submerge", "attack"),
                findClip("attack"),
                firstClip("attack_emerge", "idle")
            );
            return;
        }
        if (name.equals("endurian") && normalize(requested).startsWith("attackstart")) {
            String suffix = resolveEndurianDamageSuffix();
            startSequence(
                firstClip("attack_start" + suffix, "attack_start"),
                firstClip("attack_loop" + suffix, "attack_loop"),
                firstClip("attack_end" + suffix, "attack_end")
            );
            return;
        }
        if (name.equals("chomper") && normalize(requested).equals("special")) {
            startSequence(
                findClip("special"),
                firstClip("special_idle", "special"),
                firstClip("special_end", "idle")
            );
            return;
        }
        if (name.equals("gravebuster")) {
            startSequence(firstClip("attack"), firstClip("attack1"));
            return;
        }
        String clip = findClip(requested);
        if (clip == null) {
            clip = firstClip("special", "attack", "intro");
        }
        startSequence(clip);
    }

    private void startAttackAnimation() {
        String requested = entity.getVisualAttackClip();
        String name = normalize(entity.getName());
        String clip = findClip(requested);
        String persistentBoost = persistentPeaBoostClip();
        if (persistentBoost != null) {
            clip = persistentBoost;
        } else if (name.equals("megagatlingpea") && entity.isBoosted()
                && findClip("attack_stage2") != null) {
            clip = findClip("attack_stage2");
        } else if (entity.isBoosted() && findClip("attack_plantfood") != null) {
            clip = findClip("attack_plantfood");
        }
        if (clip == null) {
            clip = findClip("attack");
        }
        if (name.equals("citron") && normalize(clip).equals("attack")) {
            startSequence(clip, findClip("recovery"));
            return;
        }
        if (name.equals("chomper") && normalize(clip).equals("bite")) {
            startSequence(clip, findClip("bite_end"));
            return;
        }
        if (name.equals("bowlingbulb")) {
            String normalizedClip = normalize(clip);
            String reload = normalizedClip.equals("special3") ? findClip("reload3")
                    : normalizedClip.equals("special2") ? findClip("reload2")
                    : normalizedClip.equals("special") ? findClip("reload") : null;
            if (reload != null) {
                startSequence(clip, reload);
                return;
            }
        }
        startSequence(clip);
    }

    private void startPlantFoodAnimation() {
        plantFoodEffectTime = 0f;
        List<String> clips = buildPlantFoodSequence();
        startSequence(clips.toArray(new String[0]));
    }

    private List<String> buildPlantFoodSequence() {
        List<String> clips = new ArrayList<>();
        String name = normalize(entity.getName());
        if (name.equals("caulipower")) {
            addUnique(clips, firstClip("plantfood_start", "plantfood_on"));
            addUnique(clips, findClip("plantfood_loop"));
            addUnique(clips, findClip("plantfood_loop2"));
            addUnique(clips, firstClip("plantfood_end", "plantfood_off"));
            return clips;
        }
        if (name.equals("magnetshroom")) {
            addUnique(clips, findClip("plantfood_on"));
            addUnique(clips, findClip("plantfood_collection"));
            addUnique(clips, findClip("plantfood"));
            addUnique(clips, findClip("plantfood_off"));
            return clips;
        }
        if (name.equals("bowlingbulb")) {
            addUnique(clips, findClip("plantfood_on"));
            addUnique(clips, firstClip("plantfood_idle", "plantfood"));
            addUnique(clips, findClip("plantfood1"));
            addUnique(clips, findClip("plantfood2"));
            addUnique(clips, findClip("plantfood3"));
            return clips;
        }
        if (name.equals("chomper")) {
            addUnique(clips, findClip("plantfood_on"));
            addUnique(clips, findClip("plantfood"));
            addUnique(clips, findClip("plantfood_off"));
            addUnique(clips, findClip("burp"));
            addUnique(clips, findClip("burp_end"));
            return clips;
        }
        if (name.equals("cattail")) {
            addUnique(clips, findClip("plantfood"));
            addUnique(clips, findClip("plantfood_loop"));
            addUnique(clips, findClip("plantfood_end"));
            return clips;
        }
        if (name.equals("repeater")) {
            addUnique(clips, findClip("plantfood"));
            addUnique(clips, findClip("plantfood2"));
            return clips;
        }
        if (name.equals("squash")) {
            addUnique(clips, firstClip("size_up", "turn"));
            addUnique(clips, firstClip("jump_up_right", "jump_up_left"));
            addUnique(clips, firstClip("plantfood_jump_down_right", "plantfood_jump_down_left"));
            return clips;
        }
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
        String name = normalize(entity.getName());
        if (name.equals("seashroom")) {
            return firstClip("pf", "plantfood");
        }
        if (name.equals("kiwibeast")) {
            return firstClip("plantfood_stage3", "plantfood");
        }
        double ratio = entity.getMaxHp() <= 0 ? 1.0 : entity.getHp() / (double) entity.getMaxHp();
        if (ratio <= 0.33 && findClip("plantfood3") != null) {
            return findClip("plantfood3");
        }
        if (ratio <= 0.66 && findClip("plantfood2") != null) {
            return findClip("plantfood2");
        }
        return firstClip("plantfood", "plantfood_stage3", "plantfood1", "pf");
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
        float duration = specialClipDuration(clip);
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

    private float specialClipDuration(String clip) {
        float duration = Math.max(0.05f, profile.getDefinition().getClipDuration(clip));
        if (isBonkChoyPlantFoodCore(clip)) {
            duration *= BONK_CHOY_PLANT_FOOD_LOOPS;
        }
        if (normalize(entity.getName()).equals("chomper")
                && normalize(clip).equals("specialidle")) {
            duration = CHOMPER_DIGEST_VISUAL_SECONDS;
        }
        return duration;
    }

    private boolean shouldLoopSpecialClip(String clip) {
        return isBonkChoyPlantFoodCore(clip)
            || isChomperDigestLoop(clip);
    }

    private boolean isChomperDigestLoop(String clip) {
        return normalize(entity.getName()).equals("chomper")
            && normalize(clip).equals("specialidle");
    }

    private boolean isBonkChoyPlantFoodCore(String clip) {
        return normalize(entity.getName()).equals("bonkchoy")
            && normalize(clip).equals("plantfood");
    }

    private String currentSpecialClip() {
        if (specialSequence.isEmpty() || specialIndex >= specialSequence.size()) {
            return null;
        }
        return specialSequence.get(specialIndex);
    }

    private Vector2 squashVisualOffset(BoardGeometry geometry) {
        if (!normalize(entity.getName()).equals("squash") || squashLandingDirection == 0) {
            return new Vector2();
        }
        String clip = currentSpecialClip();
        if (clip == null) {
            return new Vector2(geometry.getTileWidth() * squashLandingDirection, 0f);
        }
        String normalized = normalize(clip);
        if (normalized.equals("sizeup") || normalized.equals("turn")) {
            return new Vector2();
        }
        float duration = Math.max(0.05f, profile.getDefinition().getClipDuration(clip));
        float progress = Math.min(1f, Math.max(0f, specialTime / duration));
        if (normalized.startsWith("jumpup")) {
            float eased = progress * progress * (3f - 2f * progress);
            float x = geometry.getTileWidth() * squashLandingDirection * eased;
            float y = geometry.getTileHeight() * 0.72f
                * (float) Math.sin(Math.PI * progress);
            return new Vector2(x, y);
        }
        if (normalized.startsWith("jumpdown") || normalized.startsWith("plantfoodjumpdown")) {
            float remaining = 1f - progress;
            float y = geometry.getTileHeight() * 0.50f * remaining * remaining;
            return new Vector2(geometry.getTileWidth() * squashLandingDirection, y);
        }
        return new Vector2();
    }

    private void updateWizardSheepState(float delta) {
        boolean sheep = entity.isTransformedToSheep();
        if (sheep && !previousSheepState) {
            sheepTransformTime = 0f;
            sheepRestoreTime = -1f;
            sheepIdleTime = 0f;
        } else if (!sheep && previousSheepState) {
            sheepRestoreTime = 0f;
            sheepTransformTime = -1f;
            sheepIdleTime = 0f;
        }
        if (delta > 0f && sheepTransformTime >= 0f) {
            sheepTransformTime += delta;
        }
        if (delta > 0f && sheep) {
            sheepIdleTime += delta;
        }
        if (delta > 0f && sheepRestoreTime >= 0f) {
            sheepRestoreTime += delta;
            if (sheepRestoreTime > SHEEP_RESTORE_SECONDS) {
                sheepRestoreTime = -1f;
            }
        }
        previousSheepState = sheep;
    }

    private boolean drawWizardSheep(
            Batch batch, PvzAnimationService animations, Vector2 position
    ) {
        if (sheepRestoreTime >= 0f) {
            animations.draw(
                batch, WIZARD_SHEEP_PATH, "animation2", sheepRestoreTime,
                position.x, position.y, 0.50f, false
            );
            return true;
        }
        if (!entity.isTransformedToSheep()) {
            return false;
        }
        if (sheepTransformTime >= 0f && sheepTransformTime < SHEEP_TRANSFORM_SECONDS) {
            animations.draw(
                batch, WIZARD_SHEEP_PATH, "animation", sheepTransformTime,
                position.x, position.y, 0.50f, false
            );
            return true;
        }
        animations.draw(
            batch, WIZARD_SHEEP_PATH, "idle", sheepIdleTime,
            position.x, position.y, 0.50f, true
        );
        return true;
    }

    private void drawPlant(
        Batch batch,
        PvzAnimationService animations,
        Vector2 position,
        Board board
    ) {
        String specialClip = currentSpecialClip();
        String clip = specialClip == null ? resolveIdleOrDamageClip(board) : specialClip;
        float clipTime = specialClip == null ? timeForClip(clip) : specialTime;
        boolean loop = specialClip == null || shouldLoopSpecialClip(specialClip);
        if (shouldDrawFallbackNutArmor()) {
            batch.setColor(NUT_ARMOR_TINT);
            animations.draw(
                batch,
                profile.getPath(),
                clip,
                clipTime,
                position.x,
                position.y,
                profile.getScale() * NUT_ARMOR_SHELL_SCALE,
                loop
            );
        }
        batch.setColor(resolveTint());
        animations.draw(
            batch,
            profile.getPath(),
            clip,
            clipTime,
            position.x,
            position.y,
            profile.getScale(),
            loop
        );
        batch.setColor(Color.WHITE);
        if (beginHitFlash(batch)) {
            animations.draw(
                batch,
                profile.getPath(),
                clip,
                clipTime,
                position.x,
                position.y,
                profile.getScale(),
                loop
            );
            endHitFlash(batch);
        }
    }

    private void drawActionEffects(
        Batch batch,
        BoardGeometry geometry,
        PvzAnimationService animations,
        Board board,
        Vector2 position
    ) {
        if (board == null) {
            return;
        }
        String name = normalize(entity.getName());
        if (name.equals("firepeashooter") && plantFoodEffectTime >= 0.40f && plantFoodEffectTime <= 3.40f) {
            drawFirePeashooterLane(batch, geometry, animations, board);
        }
        drawPlantFoodFieldEffects(batch, geometry, animations, board, position, name);
        String clip = currentSpecialClip();
        if (clip == null) {
            return;
        }
        if ((name.equals("potatomine") || name.equals("primalpotatomine"))
                && normalize(clip).equals("attack")) {
            float impactDelay = PlantActionTiming.meleeImpactTicks(entity.getName(), clip) / 10f;
            if (specialTime >= impactDelay) {
                String path = name.equals("primalpotatomine")
                    ? PRIMAL_POTATO_MINE_EXPLOSION_PATH : POTATO_MINE_EXPLOSION_PATH;
                animations.draw(batch, path, "animation", specialTime - impactDelay,
                    position.x, position.y, name.equals("primalpotatomine") ? 0.62f : 0.56f, false);
            }
        } else if (name.equals("phatbeet")) {
            String path = normalize(clip).contains("plantfood")
                ? PHAT_BEET_PF_PULSE_PATH : PHAT_BEET_ATTACK_PULSE_PATH;
            animations.draw(batch, path, "animation", specialTime, position.x, position.y, 0.52f, false);
        } else if (name.equals("kiwibeast")) {
            String path = normalize(clip).contains("plantfood")
                ? KIWIBEAST_PF_PULSE_PATH : KIWIBEAST_ATTACK_PULSE_PATH;
            animations.draw(batch, path, "animation", specialTime, position.x, position.y, 0.54f, false);
        }
    }

    private void drawPlantFoodFieldEffects(
        Batch batch,
        BoardGeometry geometry,
        PvzAnimationService animations,
        Board board,
        Vector2 position,
        String name
    ) {
        if (plantFoodEffectTime < 0f) {
            return;
        }
        if (name.equals("snowpea") && plantFoodEffectTime <= 3.5f) {
            drawLanePlantFoodEffect(batch, geometry, animations, board,
                SNOW_PEA_PLANT_FOOD_SLOW_PATH, snowPeaPlantFoodClip(), 0.47f, true);
        } else if (name.equals("goopeashooter") && plantFoodEffectTime >= 0.5f
                && plantFoodEffectTime <= 3.2f) {
            drawLanePlantFoodEffect(batch, geometry, animations, board,
                GOO_PEA_PLANT_FOOD_TILE_PATH, "animation", 0.46f, true);
        } else if (name.equals("fumeshroom") && plantFoodEffectTime >= 0.8f
                && plantFoodEffectTime <= 4.8f) {
            animations.draw(batch, FUME_SHROOM_BUBBLES_PATH, "plantfood",
                plantFoodEffectTime - 0.8f, position.x, position.y, 0.58f, true);
        } else if (name.equals("citron") && plantFoodEffectTime >= 0.8f
                && plantFoodEffectTime <= 1.8f) {
            drawLanePlantFoodEffect(batch, geometry, animations, board,
                CITRON_PLANT_FOOD_SHOCK_PATH, "animation", 0.52f, false);
        } else if (name.equals("sunbean") && plantFoodEffectTime <= 2.3f) {
            animations.draw(batch, SUN_BEAN_OVERLAY_ONE_PATH, "animation", plantFoodEffectTime,
                position.x, position.y, 0.50f, false);
            animations.draw(batch, SUN_BEAN_OVERLAY_TWO_PATH, "animation", plantFoodEffectTime,
                position.x, position.y, 0.50f, false);
        }
    }

    private String snowPeaPlantFoodClip() {
        if (plantFoodEffectTime < 0.5f) {
            return "plantfood_on";
        }
        if (plantFoodEffectTime > 3.0f) {
            return "plantfood_off";
        }
        return "plantfood_idle";
    }

    private void drawLanePlantFoodEffect(
        Batch batch,
        BoardGeometry geometry,
        PvzAnimationService animations,
        Board board,
        String path,
        String clip,
        float scale,
        boolean loop
    ) {
        int lane = Math.max(1, Math.min(board.getHeight(), (int) Math.round(entity.getY())));
        int startColumn = Math.max(1, (int) Math.floor(entity.getX()) + 1);
        for (int column = startColumn; column <= board.getWidth(); column++) {
            Vector2 tile = geometry.entityToScreen(column, lane);
            animations.draw(batch, path, clip, plantFoodEffectTime, tile.x, tile.y, scale, loop);
        }
    }

    private void drawFirePeashooterLane(
        Batch batch,
        BoardGeometry geometry,
        PvzAnimationService animations,
        Board board
    ) {
        int lane = Math.max(1, Math.min(board.getHeight(), (int) Math.round(entity.getY())));
        int startColumn = Math.max(1, (int) Math.floor(entity.getX()) + 1);
        float effectTime = Math.max(0f, plantFoodEffectTime - 0.40f);
        String clip = effectTime < 0.66f ? "idle" : effectTime < 2.65f ? "idle2" : "idle3";
        for (int column = startColumn; column <= board.getWidth(); column++) {
            Vector2 tile = geometry.entityToScreen(column, lane);
            animations.draw(batch, FIRE_PEA_ROW_PATH, clip, effectTime, tile.x, tile.y, 0.48f, clip.equals("idle2"));
        }
    }

    private void drawArmorExplosion(
        Batch batch,
        PvzAnimationService animations,
        Vector2 position,
        boolean front
    ) {
        if (armorExplosionTime < 0f) {
            return;
        }
        String path = front ? GENERIC_EXPLOSION_FRONT_PATH : GENERIC_EXPLOSION_BACK_PATH;
        animations.draw(batch, path, "animation", armorExplosionTime,
            position.x, position.y, 0.56f, false);
    }

    private void drawPlantFoodGlow(
        Batch batch,
        PvzAnimationService animations,
        Vector2 position
    ) {
        if (plantFoodEffectTime < 0f) {
            return;
        }
        String clip;
        float clipTime;
        boolean loop;
        if (plantFoodEffectTime < PLANT_FOOD_GLOW_ON_SECONDS) {
            clip = "plantfood_on";
            clipTime = plantFoodEffectTime;
            loop = false;
        } else if (plantFoodEffectTime < PLANT_FOOD_GLOW_OFF_START) {
            clip = "plantfood";
            clipTime = plantFoodEffectTime - PLANT_FOOD_GLOW_ON_SECONDS;
            loop = true;
        } else {
            clip = "plantfood_off";
            clipTime = plantFoodEffectTime - PLANT_FOOD_GLOW_OFF_START;
            loop = false;
        }
        animations.draw(
            batch,
            PLANT_FOOD_GLOW_PATH,
            clip,
            clipTime,
            position.x,
            position.y,
            PLANT_FOOD_GLOW_SCALE,
            loop
        );
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

    private String resolveIdleOrDamageClip(Board board) {
        String name = normalize(entity.getName());
        String persistentBoost = persistentPeaBoostClip();
        if (persistentBoost != null) {
            return persistentBoost;
        }
        if (name.equals("torchwood") && entity.hasBlueFlame()) {
            return firstClip("plantfood_t2", "plantfood", "plantfood_on_t2", "plantfood_on");
        }
        if (normalize(entity.getName()).equals("megagatlingpea") && entity.isBoosted()) {
            String stage2 = findClip("idle_stage2");
            if (stage2 != null) {
                return stage2;
            }
        }
        String staged = resolveStageClip(board);
        if (staged != null) {
            return staged;
        }
        String armored = resolveArmorClip();
        if (armored != null) {
            return armored;
        }
        if (entity.isBoosted()) {
            String boostedIdle = firstClip("idle_plantfood", "plantfood_idle");
            if (boostedIdle != null) {
                return boostedIdle;
            }
        }
        if (showDamageAppearance) {
            String damageClip = resolveDamageClip();
            if (damageClip != null) {
                return damageClip;
            }
        }
        return profile.firstClip(
            "idle", "idle2", "idle_stage1", "stage1_idle", "idle1_1", "loop", "animation", "charge"
        );
    }

    private String resolveStageClip(Board board) {
        String name = normalize(entity.getName());
        if (name.equals("peapod") && board != null) {
            int x = Math.max(1, Math.min(board.getWidth(), (int) Math.round(entity.getX())));
            int y = Math.max(1, Math.min(board.getHeight(), (int) Math.round(entity.getY())));
            models.engine.board.Tile tile = board.getTileAt(new models.engine.board.Position(x, y));
            int heads = 0;
            if (tile != null) {
                for (Plant layer : tile.getPlants()) {
                    if (normalize(layer.getName()).equals("peapod")) {
                        heads++;
                    }
                }
            }
            if (heads > 1) {
                return firstClip("idle" + Math.min(5, heads), "idle");
            }
            return findClip("idle");
        }
        if (name.equals("sunshroom")) {
            if (entity.isGrowthFinished() || stateTime >= 72f) {
                return firstClip("idle_stage3", "idle2_stage3");
            }
            if (stateTime >= 24f) {
                return firstClip("idle_stage2", "idle2_stage2");
            }
            return firstClip("idle_stage1", "idle2_stage1");
        }
        if (name.equals("kiwibeast")) {
            if (stateTime >= 72f) {
                return firstClip("idle_stage3_", "idle_stage3_2");
            }
            if (stateTime >= 24f) {
                return firstClip("idle_stage2_", "idle_stage2_2");
            }
            return firstClip("idle_stage1_", "idle_stage1_2");
        }
        if (name.equals("puffshroom")) {
            if (stateTime >= 50f) {
                return findClip("idle_stage4");
            }
            if (stateTime >= 35f) {
                return firstClip("idle_stage3", "idle2_stage3");
            }
            if (stateTime >= 20f) {
                return firstClip("idle_stage2", "idle2_stage2");
            }
            return firstClip("idle_stage1", "idle2_stage1");
        }
        if (name.equals("potatomine") || name.equals("primalpotatomine")) {
            float armSeconds = name.equals("primalpotatomine") ? 5f : 15f;
            if (!entity.isArmingFinished() && stateTime < armSeconds) {
                return firstClip("plant_idle", "plant");
            }
            return firstClip("idle", "idle2");
        }
        return null;
    }

    private String resolveArmorClip() {
        if (entity.getArmorHp() <= 0) {
            return null;
        }
        String name = normalize(entity.getName());
        if (name.equals("pumpkin")) {
            return firstClip("idle_plantfood", "idle_plantfood2", "idle");
        }
        if (name.equals("explodeonut")) {
            return firstClip("plantfood", "plantfood2", "plantfood3", "idle");
        }
        return null;
    }

    private boolean shouldDrawFallbackNutArmor() {
        if (entity.getArmorHp() <= 0 || resolveArmorClip() != null) {
            return false;
        }
        String name = normalize(entity.getName());
        return name.equals("tallnut");
    }

    private String persistentPeaBoostClip() {
        if (!entity.isBoosted()) {
            return null;
        }
        String name = normalize(entity.getName());
        if (name.equals("peashooter")) {
            return findClip("plantfood");
        }
        if (name.equals("repeater")) {
            return firstClip("plantfood2", "plantfood");
        }
        return null;
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

    private String resolveEndurianDamageSuffix() {
        if (entity.getMaxHp() <= 0) {
            return "";
        }
        double ratio = entity.getHp() / (double) entity.getMaxHp();
        if (ratio <= 0.33 && findClip("attack_start_damage3") != null) {
            return "_damage3";
        }
        if (ratio <= 0.66 && findClip("attack_start_damage2") != null) {
            return "_damage2";
        }
        if (ratio <= 0.85 && findClip("attack_start_damage") != null) {
            return "_damage";
        }
        return "";
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
        return Color.WHITE;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
