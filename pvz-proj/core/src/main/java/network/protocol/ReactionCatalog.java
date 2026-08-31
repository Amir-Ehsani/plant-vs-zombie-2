package network.protocol;

import java.util.List;
import java.util.Locale;

/**
 * Shared whitelist for the phase-three in-match reaction system.
 *
 * The assignment requires exactly three preset text messages and three emojis.
 * Three animated stickers are implemented as the bonus extension. Keeping the
 * accepted values in the shared protocol package prevents a modified client
 * from injecting arbitrary text through REACTION_SEND.
 */
public final class ReactionCatalog {
    public static final long COOLDOWN_MILLIS = 750L;

    public static final String TEXT_GOOD_LUCK = "Good luck!";
    public static final String TEXT_NICE_MOVE = "Nice move!";
    public static final String TEXT_WELL_PLAYED = "Well played!";

    public static final String EMOJI_SMILE = "SMILE";
    public static final String EMOJI_HEART = "HEART";
    public static final String EMOJI_WOW = "WOW";

    public static final String STICKER_DANCING_SUN = "DANCING_SUN";
    public static final String STICKER_DIZZY_ZOMBIE = "DIZZY_ZOMBIE";
    public static final String STICKER_BOUNCING_BRAIN = "BOUNCING_BRAIN";

    private static final List<String> TEXTS = List.of(
            TEXT_GOOD_LUCK,
            TEXT_NICE_MOVE,
            TEXT_WELL_PLAYED);
    private static final List<String> EMOJIS = List.of(
            EMOJI_SMILE,
            EMOJI_HEART,
            EMOJI_WOW);
    private static final List<String> STICKERS = List.of(
            STICKER_DANCING_SUN,
            STICKER_DIZZY_ZOMBIE,
            STICKER_BOUNCING_BRAIN);

    private ReactionCatalog() { }

    public static List<String> texts() { return TEXTS; }
    public static List<String> emojis() { return EMOJIS; }
    public static List<String> stickers() { return STICKERS; }

    /** Returns the canonical wire value or null when the value is not allowed. */
    public static String canonicalValue(ReactionCategory category, String value) {
        if (category == null || value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 64) return null;
        return switch (category) {
            case TEXT -> canonicalText(trimmed);
            case EMOJI -> canonicalToken(EMOJIS, trimmed);
            case STICKER -> canonicalToken(STICKERS, trimmed);
        };
    }

    public static boolean isAllowed(ReactionCategory category, String value) {
        return canonicalValue(category, value) != null;
    }

    private static String canonicalText(String value) {
        for (String candidate : TEXTS) {
            if (candidate.equalsIgnoreCase(value)) return candidate;
        }
        return null;
    }

    private static String canonicalToken(List<String> allowed, String value) {
        String normalized = value.toUpperCase(Locale.ROOT).replace(' ', '_');
        for (String candidate : allowed) {
            if (candidate.equals(normalized)) return candidate;
        }
        return null;
    }
}
