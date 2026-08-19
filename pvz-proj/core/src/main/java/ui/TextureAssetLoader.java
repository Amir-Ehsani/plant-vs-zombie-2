package ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public final class TextureAssetLoader {
    private TextureAssetLoader() {
    }

    public static Texture load(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        FileHandle file = Gdx.files.internal(path);
        if (!file.exists()) {
            return null;
        }
        String lower = path.toLowerCase();
        if (lower.endsWith(".webp")) {
            return loadWebp(file);
        }
        try {
            return new Texture(file);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Texture loadWebp(FileHandle file) {
        try {
            BufferedImage image = ImageIO.read(file.read());
            if (image == null) {
                return null;
            }
            Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight(), Pixmap.Format.RGBA8888);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int rgba = ((argb << 8) & 0xFFFFFF00) | ((argb >>> 24) & 0xFF);
                    pixmap.drawPixel(x, y, rgba);
                }
            }
            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            return texture;
        } catch (Exception ignored) {
            return null;
        }
    }
}
