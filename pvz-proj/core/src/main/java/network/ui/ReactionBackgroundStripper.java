package network.ui;

import com.badlogic.gdx.graphics.Pixmap;

import java.util.ArrayDeque;

/**
 * Turns generated sticker backdrops (white squares, gray checkerboards) into real alpha.
 */
final class ReactionBackgroundStripper {
    private ReactionBackgroundStripper() { }

    static void strip(Pixmap pixmap) {
        if (pixmap == null) {
            return;
        }
        int width = pixmap.getWidth();
        int height = pixmap.getHeight();
        if (width <= 2 || height <= 2) {
            return;
        }
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = pixmap.getPixel(x, y);
            }
        }
        boolean[] background = new boolean[pixels.length];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int x = 0; x < width; x++) {
            offer(pixels, background, queue, width, x, 0);
            offer(pixels, background, queue, width, x, height - 1);
        }
        for (int y = 0; y < height; y++) {
            offer(pixels, background, queue, width, 0, y);
            offer(pixels, background, queue, width, width - 1, y);
        }
        while (!queue.isEmpty()) {
            int index = queue.removeFirst();
            int x = index % width;
            int y = index / width;
            tryOffer(pixels, background, queue, width, height, x - 1, y);
            tryOffer(pixels, background, queue, width, height, x + 1, y);
            tryOffer(pixels, background, queue, width, height, x, y - 1);
            tryOffer(pixels, background, queue, width, height, x, y + 1);
        }
        pixmap.setBlending(Pixmap.Blending.None);
        for (int i = 0; i < pixels.length; i++) {
            int x = i % width;
            int y = i / width;
            if (background[i] || looksLikeBackdrop(pixels[i])) {
                pixmap.drawPixel(x, y, 0);
                continue;
            }
            float alpha = neighborTransparency(background, width, height, x, y);
            if (alpha < 1f) {
                int packed = pixels[i];
                int a = Math.round(((packed & 0xFF) * alpha));
                pixmap.drawPixel(x, y, (packed & 0xFFFFFF00) | a);
            }
        }
    }

    private static void tryOffer(
            int[] pixels,
            boolean[] background,
            ArrayDeque<Integer> queue,
            int width,
            int height,
            int x,
            int y
    ) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        offer(pixels, background, queue, width, x, y);
    }

    private static void offer(
            int[] pixels,
            boolean[] background,
            ArrayDeque<Integer> queue,
            int width,
            int x,
            int y
    ) {
        int index = y * width + x;
        if (background[index] || !looksLikeBackdrop(pixels[index])) {
            return;
        }
        background[index] = true;
        queue.addLast(index);
    }

    private static boolean looksLikeBackdrop(int rgba) {
        float red = ((rgba >>> 24) & 0xFF) / 255f;
        float green = ((rgba >>> 16) & 0xFF) / 255f;
        float blue = ((rgba >>> 8) & 0xFF) / 255f;
        float alpha = (rgba & 0xFF) / 255f;
        if (alpha < 0.08f) {
            return true;
        }
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float saturation = max - min;
        float luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue;
        if (saturation < 0.16f && luminance > 0.42f) {
            return true;
        }
        return green > 0.42f
                && green > red + 0.12f
                && green > blue + 0.08f
                && saturation < 0.42f
                && luminance > 0.46f
                && luminance < 0.84f;
    }

    private static float neighborTransparency(boolean[] background, int width, int height, int x, int y) {
        int transparent = 0;
        int total = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                    continue;
                }
                total++;
                if (background[ny * width + nx]) {
                    transparent++;
                }
            }
        }
        if (total == 0 || transparent == 0) {
            return 1f;
        }
        return Math.max(0.15f, 1f - transparent / (float) total);
    }
}
