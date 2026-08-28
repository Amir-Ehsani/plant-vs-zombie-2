package models.minigame;

import java.util.Locale;

public enum MiniGameType {
    VASEBREAKER("Vasebreaker"),
    WALLNUT_BOWLING("Wall-nut Bowling"),
    I_ZOMBIE("I, Zombie"),
    MATCH_THREE("Beghouled"),
    PLANT_ZOMBIES("Zombotany");

    private final String displayName;

    MiniGameType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MiniGameType fromText(String value) {
        String normalized = normalize(value);
        if (normalized.equals("vasebreaker") || normalized.equals("vase breaker")) {
            return VASEBREAKER;
        }
        if (normalized.equals("wallnut bowling")
                || normalized.equals("wall nut bowling")
                || normalized.equals("bowling")) {
            return WALLNUT_BOWLING;
        }
        if (normalized.equals("i zombie")
                || normalized.equals("izombie")
                || normalized.equals("i, zombie")) {
            return I_ZOMBIE;
        }
        if (normalized.equals("match 3")
                || normalized.equals("match three")
                || normalized.equals("beghouled")) {
            return MATCH_THREE;
        }
        if (normalized.equals("plant zombies")
                || normalized.equals("plant zombie")
                || normalized.equals("zombotany")) {
            return PLANT_ZOMBIES;
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}
