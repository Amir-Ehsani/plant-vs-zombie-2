package network.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;

/**
 * In-match reaction art. Emojis are glossy circular badges; stickers prefer the
 * real sun/brain lawn sprites so they match the rest of the Egypt HUD.
 */
public final class ReactionGraphicActor extends Group {
    private static final float ART_WIDTH = 120f;
    private static final float ART_HEIGHT = 104f;

    private final Array<Texture> owned = new Array<>();
    private TextureRegion sunRegion;
    private TextureRegion brainRegion;

    public ReactionGraphicActor() {
        setSize(ART_WIDTH, ART_HEIGHT);
        setOrigin(ART_WIDTH / 2f, ART_HEIGHT / 2f);
        setTransform(true);
        setVisible(false);
    }

    public void setStickerArt(TextureRegion sun, TextureRegion brain) {
        sunRegion = sun;
        brainRegion = brain;
    }

    public void setGraphic(String category, String value) {
        clearChildren();
        disposeOwned();
        if ("EMOJI".equals(category)) {
            drawEmoji(value);
        } else if ("STICKER".equals(category)) {
            drawSticker(value);
        }
        setVisible(getChildren().size > 0);
    }

    private void drawEmoji(String value) {
        if ("HEART".equals(value)) {
            drawHeartBadge();
            return;
        }
        boolean wow = "WOW".equals(value);
        drawFaceBadge(wow);
    }

    private void drawHeartBadge() {
        badgeDisc(new Color(0.55f, 0.08f, 0.22f, 1f), new Color(1f, 0.42f, 0.58f, 1f));
        Color heart = new Color(0.94f, 0.16f, 0.38f, 1f);
        Color shine = new Color(1f, 0.78f, 0.86f, 1f);
        circle(heart, 28f, 44f, 24f);
        circle(heart, 60f, 44f, 24f);
        triangle(heart, 22f, 50f, 90f, 50f, 56f, 14f);
        circle(shine, 36f, 58f, 6f);
        circle(Color.WHITE, 40f, 60f, 3f);
    }

    private void drawFaceBadge(boolean wow) {
        Color face = wow ? new Color(0.46f, 0.82f, 1f, 1f) : new Color(1f, 0.86f, 0.18f, 1f);
        Color rim = wow ? new Color(0.16f, 0.42f, 0.62f, 1f) : new Color(0.72f, 0.48f, 0.04f, 1f);
        badgeDisc(rim, face);
        Color ink = new Color(0.16f, 0.10f, 0.06f, 1f);
        circle(Color.WHITE, 34f, 50f, 10f);
        circle(Color.WHITE, 64f, 50f, 10f);
        if (wow) {
            circle(ink, 37f, 50f, 5f);
            circle(ink, 67f, 50f, 5f);
            oval(ink, 48f, 22f, 16f, 22f);
            oval(face, 52f, 28f, 8f, 12f);
            oval(Color.WHITE, 54f, 32f, 4f, 6f);
        } else {
            circle(ink, 38f, 50f, 5f);
            circle(ink, 68f, 50f, 5f);
            oval(ink, 40f, 26f, 32f, 14f);
            oval(face, 42f, 30f, 28f, 10f);
        }
        circle(new Color(1f, 1f, 1f, 0.35f), 30f, 64f, 8f);
    }

    private void drawSticker(String value) {
        if ("DIZZY_ZOMBIE".equals(value)) {
            drawDizzyZombie();
            return;
        }
        if ("BOUNCING_BRAIN".equals(value)) {
            if (addCenteredSprite(brainRegion, 92f)) {
                return;
            }
            drawBrainFallback();
            return;
        }
        if (addCenteredSprite(sunRegion, 96f)) {
            return;
        }
        drawSunFallback();
    }

    private void drawDizzyZombie() {
        badgeDisc(new Color(0.18f, 0.22f, 0.12f, 1f), new Color(0.42f, 0.62f, 0.28f, 1f));
        Color skin = new Color(0.52f, 0.74f, 0.38f, 1f);
        Color dark = new Color(0.16f, 0.14f, 0.12f, 1f);
        circle(skin, 28f, 22f, 32f);
        oval(new Color(0.28f, 0.22f, 0.18f, 1f), 34f, 8f, 44f, 16f);
        circle(Color.WHITE, 38f, 48f, 11f);
        circle(Color.WHITE, 64f, 48f, 11f);
        circle(dark, 42f, 46f, 5f);
        circle(dark, 68f, 50f, 5f);
        oval(dark, 46f, 26f, 22f, 8f);
        oval(Color.WHITE, 52f, 28f, 5f, 5f);
        Color star = new Color(1f, 0.92f, 0.28f, 1f);
        circle(star, 18f, 72f, 5f);
        circle(star, 88f, 70f, 4f);
        circle(star, 78f, 82f, 3f);
    }

    private void drawBrainFallback() {
        badgeDisc(new Color(0.42f, 0.08f, 0.24f, 1f), new Color(0.98f, 0.58f, 0.78f, 1f));
        Color brain = new Color(0.96f, 0.36f, 0.66f, 1f);
        Color crease = new Color(0.62f, 0.10f, 0.36f, 1f);
        circle(brain, 22f, 24f, 30f);
        circle(brain, 48f, 28f, 28f);
        circle(brain, 30f, 50f, 16f);
        circle(brain, 58f, 52f, 14f);
        oval(crease, 52f, 22f, 8f, 38f);
        oval(crease, 28f, 38f, 22f, 6f);
    }

    private void drawSunFallback() {
        badgeDisc(new Color(0.72f, 0.42f, 0.04f, 1f), new Color(1f, 0.84f, 0.18f, 1f));
        Color sun = new Color(1f, 0.78f, 0.10f, 1f);
        Color ray = new Color(1f, 0.58f, 0.06f, 1f);
        oval(ray, 52f, 4f, 12f, 16f);
        oval(ray, 52f, 80f, 12f, 16f);
        oval(ray, 10f, 44f, 16f, 12f);
        oval(ray, 90f, 44f, 16f, 12f);
        circle(sun, 30f, 24f, 30f);
        circle(Color.DARK_GRAY, 44f, 50f, 5f);
        circle(Color.DARK_GRAY, 64f, 50f, 5f);
        oval(Color.DARK_GRAY, 48f, 34f, 18f, 6f);
        oval(sun, 50f, 36f, 14f, 5f);
    }

    private boolean addCenteredSprite(TextureRegion region, float size) {
        if (region == null) {
            return false;
        }
        badgeDisc(new Color(0.18f, 0.12f, 0.06f, 1f), new Color(0.98f, 0.90f, 0.62f, 1f));
        Image image = new Image(new TextureRegionDrawable(region));
        image.setScaling(Scaling.fit);
        float x = (ART_WIDTH - size) / 2f;
        float y = (ART_HEIGHT - size) / 2f + 2f;
        image.setBounds(x, y, size, size);
        addActor(image);
        return true;
    }

    private void badgeDisc(Color rim, Color fill) {
        circle(new Color(0f, 0f, 0f, 0.28f), 14f, 4f, 48f);
        circle(rim, 16f, 10f, 44f);
        circle(fill, 20f, 14f, 40f);
        circle(new Color(1f, 1f, 1f, 0.18f), 28f, 48f, 14f);
    }

    private void circle(Color color, float x, float y, float radius) {
        int size = Math.max(8, Math.round(radius * 2f));
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        pixmap.setColor(color);
        pixmap.fillCircle(size / 2, size / 2, Math.max(1, size / 2 - 1));
        addTexture(pixmap, x, y, radius * 2f, radius * 2f);
    }

    private void oval(Color color, float x, float y, float width, float height) {
        int w = Math.max(6, Math.round(width));
        int h = Math.max(6, Math.round(height));
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        pixmap.setColor(color);
        pixmap.fillCircle(w / 2, h / 2, Math.max(1, Math.min(w, h) / 2 - 1));
        addTexture(pixmap, x, y, width, height);
    }

    private void triangle(Color color, float x1, float y1, float x2, float y2, float x3, float y3) {
        float minX = Math.min(x1, Math.min(x2, x3));
        float minY = Math.min(y1, Math.min(y2, y3));
        float maxX = Math.max(x1, Math.max(x2, x3));
        float maxY = Math.max(y1, Math.max(y2, y3));
        int w = Math.max(6, Math.round(maxX - minX + 2f));
        int h = Math.max(6, Math.round(maxY - minY + 2f));
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        pixmap.setColor(color);
        pixmap.fillTriangle(
                Math.round(x1 - minX), h - Math.round(y1 - minY),
                Math.round(x2 - minX), h - Math.round(y2 - minY),
                Math.round(x3 - minX), h - Math.round(y3 - minY)
        );
        addTexture(pixmap, minX, minY, w, h);
    }

    private void addTexture(Pixmap pixmap, float x, float y, float width, float height) {
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        owned.add(texture);
        Image image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        image.setBounds(x, y, width, height);
        addActor(image);
    }

    private void disposeOwned() {
        for (Texture texture : owned) {
            texture.dispose();
        }
        owned.clear();
    }

    public void dispose() {
        clearChildren();
        disposeOwned();
    }
}
