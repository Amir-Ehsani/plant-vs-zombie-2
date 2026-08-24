package ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SeedPacketCatalog {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("twin sunflower", "TWINSUNFLOWER"),
            Map.entry("sun shroom", "SUNSHROOM"),
            Map.entry("primal sunflower", "PRIMALSUNFLOWER"),
            Map.entry("gold bloom", "GOLDBLOOM"),
            Map.entry("snow pea", "SNOWPEA"),
            Map.entry("rotobaga", "ROTORUTABAGA"),
            Map.entry("pea pod", "PEAPOD"),
            Map.entry("split pea", "SPLITPEA"),
            Map.entry("electric blueberry", "ELECTRICBLUEBERRY"),
            Map.entry("bowling bulb", "BOWLINGBULB"),
            Map.entry("fire peashooter", "FIREPEASHOOTER"),
            Map.entry("goo peashooter", "POISONPEASHOOTER"),
            Map.entry("mega gatling pea", "MEGAGATLING"),
            Map.entry("sea shroom", "SEASHROOM"),
            Map.entry("puff shroom", "PUFFSHROOM"),
            Map.entry("fume shroom", "FUMESHROOM"),
            Map.entry("cabbage pult", "CABBAGEPULT"),
            Map.entry("kernel pult", "KERNELPULT"),
            Map.entry("melon pult", "MELONPULT"),
            Map.entry("winter melon", "WINTERMELON"),
            Map.entry("pepper pult", "PEPPERPULT"),
            Map.entry("potato mine", "POTATOMINE"),
            Map.entry("primal potato mine", "PRIMALPOTATOMINE"),
            Map.entry("cherry bomb", "CHERRYBOMB"),
            Map.entry("tangle kelp", "TANGLEKELP"),
            Map.entry("iceberg lettuce", "ICEBURG"),
            Map.entry("bonk choy", "BONKCHOY"),
            Map.entry("phat beet", "PHATBEET"),
            Map.entry("wasabi whip", "WASABIWHIP"),
            Map.entry("wall nut", "WALLNUT"),
            Map.entry("tall nut", "TALLNUT"),
            Map.entry("sweet potato", "SWEETPOTATO"),
            Map.entry("explode o nut", "EXPLODEONUT"),
            Map.entry("sun bean", "SUNBEAN"),
            Map.entry("magnet shroom", "MAGNETSHROOM"),
            Map.entry("hypno shroom", "HYPNOSHROOM"),
            Map.entry("ice shroom", "ICESHROOM"),
            Map.entry("lily pad", "LILYPAD"),
            Map.entry("hot potato", "HOTPOTATO"),
            Map.entry("grave buster", "GRAVEBUSTER"),
            Map.entry("pierce mint", "SPEARMINT"),
            Map.entry("cattail mint", "AILMINT"),
            Map.entry("cat tail mint", "AILMINT")
    );

    private SeedPacketCatalog() {
    }

    public static TextureRegion region(PvzAnimationService animations, String plantName) {
        if (animations == null || plantName == null || plantName.isBlank()) {
            return null;
        }
        for (String token : candidates(plantName)) {
            TextureRegion region = animations.region("IMAGE_UI_PACKETS_" + token);
            if (region != null) {
                return region;
            }
        }
        return null;
    }

    private static Set<String> candidates(String plantName) {
        Set<String> candidates = new LinkedHashSet<>();
        String normalized = normalize(plantName);
        String alias = ALIASES.get(normalized);
        if (alias != null) {
            candidates.add(alias);
        }
        candidates.add(compact(plantName));
        candidates.add(underscored(plantName));
        return candidates;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String compact(String value) {
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static String underscored(String value) {
        return value.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
