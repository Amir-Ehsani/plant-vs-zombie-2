package game.render.projectile;

import models.core.plant.Plant;

import java.util.Locale;

public enum ProjectileVisualType {
    NORMAL("T_PEA_PROJECTILE", "SPLAT_PEA", "animation", "animation", false,
        0.56f, 0.46f, 0.43f, -0.20f, 0.36f),
    FIRE("T_FIRE_PEA", "T_SPLAT_FIRE_PEA", "animation", "animation", false,
        0.58f, 0.48f, 0.43f, -0.20f, 0.36f),
    ICE("T_SNOW_PEA", "T_SPLAT_SNOW_PEA", "animation", "animation", false,
        0.58f, 0.48f, 0.43f, -0.20f, 0.36f),
    ROTOBAGA("ROTORUTABAGA_PROJECTILE1", "ROTORUTABAGA_PROJECTILE_HIT", "animation", "animation", false,
        0.50f, 0.44f, 0.30f, -0.20f, 0.38f),
    STARFRUIT("T_STARFRUIT_PROJECTILE", "T_STARFRUIT_PROJECTILE_HIT", "animation", "idle", false,
        0.54f, 0.46f, 0.30f, -0.18f, 0.38f),
    GOO("GOOPEASHOOTER_PROJECTILES", "GOOPEASHOOTER_PROJECTILES", "projectile_t1", "hit_t1", false,
        0.54f, 0.48f, 0.43f, -0.20f, 0.38f),
    MEGA("MEGAGATLING_PROJECTILE", "SPLAT_PEA", "animation", "animation", false,
        0.60f, 0.46f, 0.43f, -0.20f, 0.34f),
    SEA_SHROOM("SEASHROOM_PROJECTILE", null, "animation", null, false,
        0.55f, 0f, 0.34f, -0.18f, 0.38f),
    PUFF_SHROOM("T_PUFFSHROOM_PROJECTILE", "T_PUFFSHROOM_HIT", "animation", "animation", false,
        0.55f, 0.46f, 0.34f, -0.18f, 0.38f),
    CITRON("CITRON_CITRUS_ORB", "CITRON_CITRUS_ORB_HIT", "Citron_Citrus_Orb", "animation", false,
        0.64f, 0.56f, 0.43f, -0.20f, 0.48f),
    BOWLING_SMALL("BOWLINGBULB_PROJECTILE1", null, "animation", null, false,
        0.52f, 0f, 0.32f, -0.16f, 0.47f),
    BOWLING_MEDIUM("BOWLINGBULB_PROJECTILE2", null, "animation", null, false,
        0.58f, 0f, 0.32f, -0.16f, 0.47f),
    BOWLING_LARGE("BOWLINGBULB_PROJECTILE3", null, "animation", null, false,
        0.65f, 0f, 0.32f, -0.16f, 0.47f),
    HOMING_THISTLE("HOMING_THISTLE_PROJECTILE", "HOMING_THISTLE_PROJECTILE_HIT", "animation", "animation", false,
        0.52f, 0.46f, 0.32f, -0.18f, 0.42f),
    CABBAGE("T_CABBAGEPULT_PROJECTILE", "SPLAT_CABBAGEPULT", "animation", "animation", true,
        0.48f, 0.44f, 0.18f, -0.45f, 0.47f),
    KERNEL("T_KERNALPULT_PROJECTILE", "SPLAT_KERNALPULT_KERNAL", "animation", "animation", true,
        0.50f, 0.44f, 0.15f, -0.44f, 0.47f),
    KERNEL_BUTTER("T_KERNALPULT_PROJECTILE", "SPLAT_KERNALPULT_BUTTER", "animation3", "animation", true,
        0.56f, 0.46f, 0.15f, -0.44f, 0.47f),
    PEPPER("T_PEPPERPULT_PROJECTILE", "T_PEPPERPULT_PROJECTILE_SPLAT", "animation", "animation", true,
        0.50f, 0.46f, 0.18f, -0.43f, 0.47f),
    MELON("T_MELON_PROJECTILE", "T_SPLAT_MELONPULT", "animation", "animation", true,
        0.56f, 0.50f, 0.16f, -0.46f, 0.48f),
    WINTER_MELON("T_WINTERMELON_PROJECTILE", "T_SPLAT_WINTERMELON", "animation", "animation", true,
        0.56f, 0.50f, 0.16f, -0.46f, 0.48f),
    FUME("FUMESHROOM_BUBBLES", "FUMESHROOM_BUBBLES_HIT", "animation", "animation", false,
        0.56f, 0.48f, 0.36f, -0.13f, 0.35f),
    CACTUS("T_CACTUS_PROJECTILE", "CACTUS_PROJECTILE_HIT", "animation", "animation", false,
        0.52f, 0.46f, 0.38f, -0.16f, 0.36f),
    REPEATER_GIANT("REPEATER_PLANTFOOD_GIANTPEA", "SPLAT_PEA", "animation", "animation", false,
        0.72f, 0.52f, 0.43f, -0.20f, 0.34f),
    PEAPOD_GIANT("PEAPOD_PLANTFOOD_GIANTPEA", "SPLAT_PEA", "animation", "animation", false,
        0.74f, 0.52f, 0.43f, -0.20f, 0.34f),
    CITRON_PLANT_FOOD("CITRON_PLANTFOOD_ORB", "CITRON_PLANTFOOD_ORB_HIT",
        "Plantfood_Citron_Plasma_Orb", "animation", false, 0.78f, 0.64f, 0.43f, -0.20f, 0.30f),
    BOWLING_PLANT_FOOD("BOWLINGBULB_PLANTFOOD_PROJECTILE",
        "BOWLINGBULB_PLANTFOOD_PROJECTILE", "animation", "explosion", false,
        0.76f, 0.72f, 0.32f, -0.16f, 0.32f),
    CABBAGE_PLANT_FOOD("CABBAGEPULT_PLANTFOOD_PROJECTILE",
        "CABBAGEPULT_PLANTFOOD_PROJECTILE", "plantfood_cabbage", "plantfood_cabbageExplode", true,
        0.62f, 0.62f, 0.18f, -0.45f, 0.40f),
    MELON_PLANT_FOOD("T_MELON_PROJECTILE", "MELON_EXPLODE", "animation",
        "plantfood_MelonExplode", true, 0.74f, 0.70f, 0.16f, -0.46f, 0.40f),
    WINTER_MELON_PLANT_FOOD("T_WINTERMELON_PROJECTILE", "WINTERMELON_EXPLODE",
        "animation", "plantfood_WintermelonExplode", true, 0.74f, 0.70f, 0.16f, -0.46f, 0.40f),
    PEPPER_PLANT_FOOD("T_PEPPERPULT_PROJECTILE", "PEPPERPULT_PROJECTILE_PF_SPLAT",
        "animation", "animation", true, 0.68f, 0.62f, 0.18f, -0.43f, 0.40f);

    private final String animationName;
    private final String impactAnimationName;
    private final String projectileClip;
    private final String impactClip;
    private final boolean lobbed;
    private final float scale;
    private final float impactScale;
    private final float spawnXOffset;
    private final float spawnYOffset;
    private final float releaseFraction;

    ProjectileVisualType(
        String animationName,
        String impactAnimationName,
        String projectileClip,
        String impactClip,
        boolean lobbed,
        float scale,
        float impactScale,
        float spawnXOffset,
        float spawnYOffset,
        float releaseFraction
    ) {
        this.animationName = animationName;
        this.impactAnimationName = impactAnimationName;
        this.projectileClip = projectileClip;
        this.impactClip = impactClip;
        this.lobbed = lobbed;
        this.scale = scale;
        this.impactScale = impactScale;
        this.spawnXOffset = spawnXOffset;
        this.spawnYOffset = spawnYOffset;
        this.releaseFraction = releaseFraction;
    }

    public static ProjectileVisualType fromPlant(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return null;
        }
        String name = normalize(plant.getName());
        return switch (name) {
            case "fire peashooter" -> FIRE;
            case "snow pea" -> ICE;
            case "rotobaga" -> ROTOBAGA;
            case "starfruit" -> STARFRUIT;
            case "goo peashooter" -> GOO;
            case "mega gatling pea" -> MEGA;
            case "sea shroom" -> SEA_SHROOM;
            case "puff shroom" -> PUFF_SHROOM;
            case "citron" -> CITRON;
            case "bowling bulb" -> BOWLING_SMALL;
            case "cat tail" -> HOMING_THISTLE;
            case "cabbage pult" -> CABBAGE;
            case "kernel pult" -> KERNEL;
            case "pepper pult" -> PEPPER;
            case "melon pult" -> MELON;
            case "winter melon" -> WINTER_MELON;
            case "fume shroom" -> FUME;
            case "cactus" -> CACTUS;
            default -> {
                String category = normalize(plant.getType().getCategory());
                if (category.equals("shooter") || category.equals("strike through")) {
                    yield NORMAL;
                }
                yield null;
            }
        };
    }

    public String getAnimationName() { return animationName; }
    public String getImpactAnimationName() { return impactAnimationName; }
    public String getProjectileClip() { return projectileClip; }
    public String getImpactClip() { return impactClip; }
    public boolean isLobbed() { return lobbed; }
    public float getScale() { return scale; }
    public float getImpactScale() { return impactScale; }
    public float getSpawnXOffset() { return spawnXOffset; }
    public float getSpawnYOffset() { return spawnYOffset; }
    public float getReleaseFraction() { return releaseFraction; }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT)
            .replace('-', ' ')
            .replace('_', ' ')
            .replaceAll("\\s+", " ");
    }
}
