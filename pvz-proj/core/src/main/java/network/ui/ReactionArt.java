package network.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import network.protocol.ReactionCatalog;
import network.protocol.ReactionCategory;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads the generated in-match reaction stickers from {@code reactions/}.
 * Wire values stay in {@link ReactionCatalog}; this class is display-only.
 */
public final class ReactionArt implements Disposable {
    private final Map<String, Texture> textures = new HashMap<>();

    public TextureRegion region(String category, String value) {
        String path = assetPath(category, value);
        if (path == null) {
            return null;
        }
        Texture texture = textures.get(path);
        if (texture == null) {
            try {
                if (!Gdx.files.internal(path).exists()) {
                    return null;
                }
                Pixmap pixmap = new Pixmap(Gdx.files.internal(path));
                ReactionBackgroundStripper.strip(pixmap);
                texture = new Texture(pixmap);
                pixmap.dispose();
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                textures.put(path, texture);
            } catch (Exception ignored) {
                return null;
            }
        }
        return new TextureRegion(texture);
    }

    public static String caption(String category, String value) {
        if ("TEXT".equals(category)) {
            if (ReactionCatalog.TEXT_NICE_MOVE.equalsIgnoreCase(value)) {
                return "Nice move";
            }
            if (ReactionCatalog.TEXT_WELL_PLAYED.equalsIgnoreCase(value)) {
                return "Well played";
            }
            return "Good luck";
        }
        if ("EMOJI".equals(category)) {
            return switch (value == null ? "" : value.toUpperCase()) {
                case "HEART" -> "Heart";
                case "WOW" -> "Wow";
                default -> "Smile";
            };
        }
        return switch (value == null ? "" : value.toUpperCase()) {
            case "DIZZY_ZOMBIE" -> "Dizzy";
            case "BOUNCING_BRAIN" -> "Brain";
            default -> "Sun";
        };
    }

    public static String assetPath(String category, String value) {
        if (category == null || value == null) {
            return null;
        }
        String file = switch (category.toUpperCase()) {
            case "TEXT" -> textFile(value);
            case "EMOJI" -> emojiFile(value);
            case "STICKER" -> stickerFile(value);
            default -> null;
        };
        return file == null ? null : "reactions/" + file;
    }

    public static String assetPath(ReactionCategory category, String value) {
        return category == null ? null : assetPath(category.name(), value);
    }

    private static String textFile(String value) {
        if (ReactionCatalog.TEXT_NICE_MOVE.equalsIgnoreCase(value)) {
            return "reaction-nice-move.png";
        }
        if (ReactionCatalog.TEXT_WELL_PLAYED.equalsIgnoreCase(value)) {
            return "reaction-well-played.png";
        }
        if (ReactionCatalog.TEXT_GOOD_LUCK.equalsIgnoreCase(value)) {
            return "reaction-good-luck.png";
        }
        return null;
    }

    private static String emojiFile(String value) {
        return switch (value.toUpperCase()) {
            case "SMILE" -> "reaction-smile.png";
            case "HEART" -> "reaction-heart.png";
            case "WOW" -> "reaction-wow.png";
            default -> null;
        };
    }

    private static String stickerFile(String value) {
        return switch (value.toUpperCase()) {
            case "DANCING_SUN" -> "reaction-dancing-sun.png";
            case "DIZZY_ZOMBIE" -> "reaction-dizzy-zombie.png";
            case "BOUNCING_BRAIN" -> "reaction-bouncing-brain.png";
            default -> null;
        };
    }

    @Override
    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
    }
}
