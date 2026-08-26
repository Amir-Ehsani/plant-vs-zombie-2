package models.core.plant;

import java.util.Locale;

public final class PlantActionTiming {
    public static final int TICKS_PER_SECOND = 10;
    private static final double MIN_PROJECTILE_TRAVEL_SECONDS = 0.20;
    private static final double MAX_PROJECTILE_TRAVEL_SECONDS = 0.90;
    private static final double PROJECTILE_TILES_PER_SECOND = 8.5;
    private static final double SHOT_STAGGER_SECONDS = 0.07;

    private PlantActionTiming() {
    }

    public static int projectileImpactTicks(
            String plantName,
            String attackClip,
            double distance,
            int shotIndex
    ) {
        double release = attackDurationSeconds(plantName, attackClip)
                * projectileReleaseFraction(plantName);
        double travel = clamp(
                Math.abs(distance) / PROJECTILE_TILES_PER_SECOND,
                MIN_PROJECTILE_TRAVEL_SECONDS,
                MAX_PROJECTILE_TRAVEL_SECONDS
        );
        double stagger = Math.max(0, shotIndex) * SHOT_STAGGER_SECONDS;
        return secondsToTicks(release + travel + stagger);
    }

    public static int meleeImpactTicks(String plantName, String attackClip) {
        String name = normalize(plantName);
        if (name.equals("bonk choy")) {
            return normalize(attackClip).equals("attack3") ? 3 : 2;
        }
        if (name.equals("wasabi whip")) {
            return 3;
        }
        if (name.equals("phat beet")) {
            return 5;
        }
        if (name.equals("chomper")) {
            return 4;
        }
        if (name.equals("squash")) {
            return 5;
        }
        if (name.equals("tangle kelp")) {
            return 8;
        }
        if (name.equals("iceberg lettuce")) {
            return 6;
        }
        if (name.equals("kiwibeast")) {
            return 7;
        }
        return Math.max(1, secondsToTicks(attackDurationSeconds(plantName, attackClip) * 0.45));
    }

    public static int specialImpactTicks(String plantName) {
        return switch (normalize(plantName)) {
            case "magnet shroom" -> 4;
            case "caulipower" -> 9;
            case "electric blueberry" -> 9;
            case "gold bloom" -> 13;
            case "grave buster" -> 8;
            case "hot potato" -> 18;
            case "cherry bomb" -> 7;
            case "grapeshot" -> 9;
            case "jalapeno" -> 5;
            case "doom shroom" -> 12;
            case "ice shroom" -> 7;
            case "enlighten mint" -> 5;
            case "appease mint" -> 6;
            case "arma mint" -> 7;
            case "bombard mint" -> 5;
            case "enforce mint" -> 6;
            case "reinforce mint" -> 5;
            case "enchant mint", "cattail mint" -> 5;
            case "pierce mint" -> 5;
            default -> 0;
        };
    }

    public static int plantFoodImpactTicks(String plantName) {
        return switch (normalize(plantName)) {
            case "sunflower", "twin sunflower", "primal sunflower" -> 7;
            case "sun shroom" -> 12;
            case "repeater" -> 5;
            case "snow pea" -> 11;
            case "pea pod" -> 15;
            case "citron" -> 8;
            case "caulipower" -> 5;
            case "electric blueberry" -> 10;
            case "bowling bulb" -> 12;
            case "fire peashooter" -> 5;
            case "goo peashooter" -> 8;
            case "mega gatling pea" -> 4;
            case "sea shroom" -> 6;
            case "fume shroom" -> 10;
            case "cabbage pult" -> 8;
            case "kernel pult" -> 7;
            case "melon pult", "winter melon" -> 11;
            case "pepper pult" -> 12;
            case "potato mine", "primal potato mine" -> 6;
            case "squash" -> 8;
            case "tangle kelp" -> 11;
            case "iceberg lettuce" -> 7;
            case "bonk choy" -> 10;
            case "phat beet" -> 8;
            case "chomper" -> 10;
            case "wasabi whip" -> 5;
            case "kiwibeast" -> 12;
            case "garlic" -> 10;
            case "sweet potato" -> 6;
            case "magnet shroom" -> 8;
            case "hypno shroom" -> 8;
            case "cat tail" -> 5;
            case "ice shroom" -> 8;
            case "lily pad" -> 8;
            default -> 3;
        };
    }

    public static int sunProductionImpactTicks(String plantName) {
        return switch (normalize(plantName)) {
            case "sunflower" -> 10;
            case "twin sunflower" -> 8;
            case "sun shroom" -> 10;
            case "primal sunflower" -> 9;
            default -> 0;
        };
    }

    public static String immediateActionClip(String plantName) {
        return switch (normalize(plantName)) {
            case "gold bloom", "cherry bomb", "grapeshot", "jalapeno", "hot potato" -> "attack";
            case "doom shroom" -> "stage3_explode";
            case "ice shroom" -> "attack";
            case "grave buster" -> "attack";
            case "enlighten mint", "appease mint", "arma mint", "bombard mint",
                    "enforce mint", "reinforce mint", "enchant mint", "cattail mint",
                    "pierce mint" -> "intro";
            default -> "attack";
        };
    }

    public static float immediatePlaybackRate(String plantName) {
        if (normalize(plantName).equals("cherry bomb")) {
            return 0.62f;
        }
        return 1f;
    }

    public static double attackDurationSeconds(String plantName, String attackClip) {
        String name = normalize(plantName);
        String clip = normalize(attackClip);
        if (name.equals("kernel pult")) return clip.equals("attack2") ? 1.8333 : 1.8667;
        if (name.equals("bonk choy")) return clip.equals("attack3") ? 0.6667 : 0.3333;
        if (name.equals("split pea")) return clip.equals("attack2") ? 1.0 : 0.9667;
        if (name.equals("kiwibeast")) return 1.5;
        return switch (name) {
            case "peashooter", "repeater", "threepeater", "mega gatling pea" -> 1.0333;
            case "snow pea" -> 1.4333;
            case "rotobaga" -> 1.7333;
            case "pea pod" -> 1.0333;
            case "citron" -> 1.3;
            case "caulipower" -> 1.7333;
            case "electric blueberry" -> 1.6667;
            case "cactus" -> 1.0333;
            case "fire peashooter" -> 1.0333;
            case "starfruit" -> 1.0;
            case "goo peashooter" -> 1.6;
            case "sea shroom" -> 1.0667;
            case "puff shroom" -> 0.8;
            case "fume shroom" -> 1.8;
            case "cabbage pult" -> 1.6667;
            case "melon pult" -> 1.9667;
            case "winter melon" -> 2.1667;
            case "pepper pult" -> 2.0;
            case "cat tail" -> 1.8667;
            case "wasabi whip" -> 0.6667;
            case "phat beet" -> 0.9;
            case "chomper" -> 0.7333;
            default -> 1.0;
        };
    }

    private static double projectileReleaseFraction(String plantName) {
        String name = normalize(plantName);
        if (name.equals("cabbage pult") || name.equals("kernel pult")
                || name.equals("melon pult") || name.equals("winter melon")
                || name.equals("pepper pult")) {
            return 0.47;
        }
        if (name.equals("fume shroom")) return 0.35;
        if (name.equals("cactus")) return 0.36;
        return 0.36;
    }

    private static int secondsToTicks(double seconds) {
        if (seconds <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(seconds * TICKS_PER_SECOND));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
