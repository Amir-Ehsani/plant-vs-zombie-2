package models.level.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public enum SeasonType {
    ANCIENT_EGYPT(
            "Ancient Egypt",
            Arrays.asList("Ra", "Explorer", "Tomb raiser")
    ),
    FROSTBITE_CAVES(
            "Frostbite Caves",
            Arrays.asList("brick head", "Dodo", "Hunter", "Troglobite")
    ),
    BIG_WAVE_BEACH(
            "Big Wave Beach",
            Arrays.asList("Octopus", "Snorkel")
    ),
    DARK_AGES(
            "Dark Ages",
            Arrays.asList("knight", "Juggler", "Wizard", "Imp Dragon")
    );

    private static List<String> commonZombies() {
        return Arrays.asList(
                "Default",
                "cone head",
                "bucket head",
                "Gargantuar",
                "Imp"
        );
    }


    private final String displayName;
    private final List<String> allowedZombieNames;

    SeasonType(String displayName, List<String> seasonZombieNames) {
        this.displayName = displayName;
        List<String> names = new ArrayList<>(commonZombies());
        names.addAll(seasonZombieNames);
        this.allowedZombieNames = Collections.unmodifiableList(names);
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getAllowedZombieNames() {
        return allowedZombieNames;
    }

    public boolean isZombieAllowed(String zombieName) {
        String normalized = normalize(zombieName);
        for (String allowedName : allowedZombieNames) {
            if (normalize(allowedName).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static SeasonType fromName(String name) {
        String normalized = normalize(name);
        for (SeasonType seasonType : values()) {
            if (normalize(seasonType.name()).equals(normalized)
                    || normalize(seasonType.displayName).equals(normalized)) {
                return seasonType;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}
