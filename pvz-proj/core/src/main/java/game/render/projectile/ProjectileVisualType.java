package game.render.projectile;

import models.core.plant.Plant;

import java.util.Locale;

public enum ProjectileVisualType {
    NORMAL("T_PEA_PROJECTILE", "SPLAT_PEA", false, 0.20f, 0.22f),
    FIRE("T_FIRE_PEA", "T_SPLAT_FIRE_PEA", false, 0.22f, 0.24f),
    ICE("T_SNOW_PEA", "T_SPLAT_SNOW_PEA", false, 0.22f, 0.24f),
    CABBAGE("T_CABBAGEPULT_PROJECTILE", "SPLAT_CABBAGEPULT", true, 0.24f, 0.28f),
    KERNEL("T_KERNALPULT_PROJECTILE", "SPLAT_KERNALPULT_KERNAL", true, 0.23f, 0.26f),
    PEPPER("T_PEPPERPULT_PROJECTILE", "T_PEPPERPULT_PROJECTILE_SPLAT", true, 0.24f, 0.28f),
    MELON("T_MELON_PROJECTILE", "T_SPLAT_MELONPULT", true, 0.27f, 0.30f),
    WINTER_MELON("T_WINTERMELON_PROJECTILE", "T_SPLAT_WINTERMELON", true, 0.27f, 0.30f),
    FUME("FUMESHROOM_BUBBLES", "FUMESHROOM_BUBBLES_HIT", false, 0.30f, 0.28f),
    CACTUS("T_CACTUS_PROJECTILE", "CACTUS_PROJECTILE_HIT", false, 0.24f, 0.24f);

    private final String animationName;
    private final String impactAnimationName;
    private final boolean lobbed;
    private final float scale;
    private final float impactScale;

    ProjectileVisualType(
        String animationName,
        String impactAnimationName,
        boolean lobbed,
        float scale,
        float impactScale
    ) {
        this.animationName = animationName;
        this.impactAnimationName = impactAnimationName;
        this.lobbed = lobbed;
        this.scale = scale;
        this.impactScale = impactScale;
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

    public boolean isLobbed() {
        return lobbed;
    }

    public float getScale() {
        return scale;
    }

    public float getImpactScale() {
        return impactScale;
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
