package network.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/** Small code-drawn emoji/sticker canvas; its Group transform makes sticker animations graphical. */
public final class ReactionGraphicActor extends Group {
    private final Texture pixel;
    private final TextureRegionDrawable drawable;

    public ReactionGraphicActor() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
        drawable = new TextureRegionDrawable(new TextureRegion(pixel));
        setSize(112f, 96f);
        setOrigin(getWidth() / 2f, getHeight() / 2f);
        setTransform(true);
        setVisible(false);
    }

    public void setGraphic(String category, String value) {
        clearChildren();
        if ("EMOJI".equals(category)) drawEmoji(value);
        else if ("STICKER".equals(category)) drawSticker(value);
        setVisible(getChildren().size > 0);
    }

    private void drawEmoji(String value) {
        if ("HEART".equals(value)) {
            block(Color.SCARLET, 22f, 50f, 30f, 30f);
            block(Color.SCARLET, 60f, 50f, 30f, 30f);
            block(Color.SCARLET, 14f, 34f, 84f, 30f);
            block(Color.SCARLET, 30f, 18f, 52f, 24f);
            block(Color.SCARLET, 46f, 6f, 20f, 20f);
            return;
        }
        Color face = "WOW".equals(value) ? new Color(0.45f, 0.82f, 1f, 1f) : Color.GOLD;
        block(face, 18f, 8f, 76f, 76f);
        block(Color.DARK_GRAY, 35f, 58f, 10f, 14f);
        block(Color.DARK_GRAY, 67f, 58f, 10f, 14f);
        if ("WOW".equals(value)) {
            block(Color.DARK_GRAY, 46f, 24f, 20f, 24f);
            block(face, 52f, 30f, 8f, 12f);
        } else {
            block(Color.DARK_GRAY, 34f, 27f, 10f, 7f);
            block(Color.DARK_GRAY, 68f, 27f, 10f, 7f);
            block(Color.DARK_GRAY, 43f, 20f, 26f, 7f);
        }
    }

    private void drawSticker(String value) {
        if ("DIZZY_ZOMBIE".equals(value)) {
            block(new Color(0.40f, 0.62f, 0.36f, 1f), 18f, 10f, 76f, 74f);
            block(Color.YELLOW, 31f, 57f, 17f, 15f);
            block(Color.YELLOW, 65f, 57f, 17f, 15f);
            block(Color.DARK_GRAY, 36f, 61f, 8f, 7f);
            block(Color.DARK_GRAY, 70f, 61f, 8f, 7f);
            block(Color.DARK_GRAY, 42f, 25f, 30f, 8f);
            block(Color.WHITE, 49f, 25f, 5f, 8f);
            block(Color.WHITE, 62f, 25f, 5f, 8f);
            return;
        }
        if ("BOUNCING_BRAIN".equals(value)) {
            Color brain = new Color(0.96f, 0.35f, 0.67f, 1f);
            block(brain, 20f, 24f, 72f, 48f);
            block(brain, 28f, 66f, 24f, 20f);
            block(brain, 60f, 66f, 24f, 20f);
            block(new Color(0.62f, 0.12f, 0.38f, 1f), 52f, 27f, 7f, 54f);
            block(new Color(0.76f, 0.18f, 0.48f, 1f), 31f, 45f, 19f, 6f);
            block(new Color(0.76f, 0.18f, 0.48f, 1f), 62f, 53f, 19f, 6f);
            return;
        }
        Color sun = new Color(1f, 0.72f, 0.08f, 1f);
        block(sun, 34f, 25f, 44f, 44f);
        block(sun, 48f, 3f, 16f, 20f);
        block(sun, 48f, 71f, 16f, 20f);
        block(sun, 10f, 39f, 22f, 16f);
        block(sun, 80f, 39f, 22f, 16f);
        block(Color.DARK_GRAY, 45f, 49f, 6f, 7f);
        block(Color.DARK_GRAY, 62f, 49f, 6f, 7f);
        block(Color.DARK_GRAY, 50f, 35f, 14f, 5f);
    }

    private void block(Color color, float x, float y, float width, float height) {
        Image image = new Image(drawable);
        image.setColor(color);
        image.setBounds(x, y, width, height);
        addActor(image);
    }

    public void dispose() { pixel.dispose(); }
}
