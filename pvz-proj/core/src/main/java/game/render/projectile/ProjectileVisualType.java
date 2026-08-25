package game.render.projectile;

import models.core.plant.Plant;

import java.util.Locale;

public enum ProjectileVisualType {
    NORMAL("T_PEA_PROJECTILE", "SPLAT_PEA", "animation", false, 0.56f, 0.46f, 0.43f, -0.20f, 0.36f),
    FIRE("T_FIRE_PEA", "T_SPLAT_FIRE_PEA", "animation", false, 0.58f, 0.48f, 0.43f, -0.20f, 0.36f),
    ICE("T_SNOW_PEA", "T_SPLAT_SNOW_PEA", "animation", false, 0.58f, 0.48f, 0.43f, -0.20f, 0.36f),
    CABBAGE("T_CABBAGEPULT_PROJECTILE", "SPLAT_CABBAGEPULT", "animation", true,
        0.48f, 0.44f, 0.18f, -0.45f, 0.47f),
    KERNEL("T_KERNALPULT_PROJECTILE", "SPLAT_KERNALPULT_KERNAL", "animation", true,
        0.50f, 0.44f, 0.15f, -0.44f, 0.47f),
    KERNEL_BUTTER("T_KERNALPULT_PROJECTILE", "SPLAT_KERNALPULT_BUTTER", "animation3", true,
        0.56f, 0.46f, 0.15f, -0.44f, 0.47f),
    PEPPER("T_PEPPERPULT_PROJECTILE", "T_PEPPERPULT_PROJECTILE_SPLAT", "animation", true,
        0.50f, 0.46f, 0.18f, -0.43f, 0.47f),
    MELON("T_MELON_PROJECTILE", "T_SPLAT_MELONPULT", "animation", true,
        0.56f, 0.50f, 0.16f, -0.46f, 0.48f),
    WINTER_MELON("T_WINTERMELON_PROJECTILE", "T_SPLAT_WINTERMELON", "animation", true,
        0.56f, 0.50f, 0.16f, -0.46f, 0.48f),
    FUME("FUMESHROOM_BUBBLES", "FUMESHROOM_BUBBLES_HIT", "animation", false,
        0.56f, 0.48f, 0.36f, -0.13f, 0.35f),
    CACTUS("T_CACTUS_PROJECTILE", "CACTUS_PROJECTILE_HIT", "animation", false,
        0.52f, 0.46f, 0.38f, -0.16f, 0.36f);

    private final String animationName;
    private final String impactAnimationName;
    private final String projectileClip;
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
        if (name.equals("fire peashooter")) return FIRE;
        if (name.equals("snow pea")) return ICE;
        if (name.equals("cabbage pult")) return CABBAGE;
        if (name.equals("kernel pult")) return KERNEL;
        if (name.equals("pepper pult")) return PEPPER;
        if (name.equals("melon pult")) return MELON;
        if (name.equals("winter melon")) return WINTER_MELON;
        if (name.equals("fume shroom")) return FUME;
        if (name.equals("cactus")) return CACTUS;

        String category = normalize(plant.getType().getCategory());
        if (category.equals("shooter") || category.equals("strike through")) {
            return NORMAL;
        }
        return null;
    }

    public String getAnimationName() {
        return animationName;
    }

    public String getImpactAnimationName() {
        return impactAnimationName;
    }

    public String getProjectileClip() {
        return projectileClip;
    }

    public boolean isLobbed() {
        return lobbed;
    }

    public float getScale() {
        return scale;
    }

    public float getImpactScale() {
        return impactScale;
    }

    public float getSpawnXOffset() {
        return spawnXOffset;
    }

    public float getSpawnYOffset() {
        return spawnYOffset;
    }

    public float getReleaseFraction() {
        return releaseFraction;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
            .replace('-', ' ')
            .replace('_', ' ')
            .replaceAll("\\s+", " ");
    }
}
