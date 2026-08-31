package ui;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * Center-crops a wide atlas region into the actor bounds and rounds the corners
 * so event banners cannot spill out of menu cards.
 */
public final class CroppedImage extends Image {
    private final TextureRegion source;
    private final int radius;
    private Texture mask;
    private int maskWidth;
    private int maskHeight;

    public CroppedImage(TextureRegion source, float radius) {
        super(source);
        this.source = source;
        this.radius = Math.max(0, Math.round(radius));
        setScaling(Scaling.stretch);
    }

    @Override
    public float getPrefWidth() {
        return 0f;
    }

    @Override
    public float getPrefHeight() {
        return 0f;
    }

    @Override
    public float getMinWidth() {
        return 0f;
    }

    @Override
    public float getMinHeight() {
        return 0f;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        applyCrop();
        batch.flush();
        if (!clipBegin()) {
            return;
        }
        super.draw(batch, parentAlpha);
        drawRoundedMask(batch);
        batch.flush();
        clipEnd();
    }

    @Override
    public boolean remove() {
        disposeMask();
        return super.remove();
    }

    private void applyCrop() {
        if (source == null || getWidth() <= 1f || getHeight() <= 1f) {
            return;
        }
        setDrawable(new TextureRegionDrawable(centerCrop(source, getWidth() / getHeight())));
    }

    private void drawRoundedMask(Batch batch) {
        if (radius <= 0 || batch == null) {
            return;
        }
        Texture rounded = maskFor((int) Math.max(2f, getWidth()), (int) Math.max(2f, getHeight()));
        if (rounded == null) {
            return;
        }
        batch.flush();
        batch.setBlendFunctionSeparate(GL20.GL_ZERO, GL20.GL_ONE, GL20.GL_ZERO, GL20.GL_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(rounded, getX(), getY(), getWidth(), getHeight());
        batch.flush();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(getColor());
    }

    private Texture maskFor(int width, int height) {
        if (mask != null && maskWidth == width && maskHeight == height) {
            return mask;
        }
        disposeMask();
        int corner = Math.min(radius, Math.min(width, height) / 2);
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(1f, 1f, 1f, 1f);
        if (width > corner * 2) {
            pixmap.fillRectangle(corner, 0, width - corner * 2, height);
        }
        if (height > corner * 2) {
            pixmap.fillRectangle(0, corner, width, height - corner * 2);
        }
        pixmap.fillCircle(corner, corner, corner);
        pixmap.fillCircle(width - 1 - corner, corner, corner);
        pixmap.fillCircle(corner, height - 1 - corner, corner);
        pixmap.fillCircle(width - 1 - corner, height - 1 - corner, corner);
        mask = new Texture(pixmap);
        pixmap.dispose();
        maskWidth = width;
        maskHeight = height;
        return mask;
    }

    private void disposeMask() {
        if (mask != null) {
            mask.dispose();
            mask = null;
        }
        maskWidth = 0;
        maskHeight = 0;
    }

    static TextureRegion centerCrop(TextureRegion source, float targetAspect) {
        if (source == null || targetAspect <= 0f) {
            return source;
        }
        int x = source.getRegionX();
        int y = source.getRegionY();
        int width = source.getRegionWidth();
        int height = source.getRegionHeight();
        if (width <= 0 || height <= 0) {
            return source;
        }
        float sourceAspect = width / (float) height;
        if (sourceAspect > targetAspect) {
            int croppedWidth = Math.max(1, Math.round(height * targetAspect));
            x += Math.max(0, (width - croppedWidth) / 2);
            width = Math.min(width, croppedWidth);
        } else if (sourceAspect < targetAspect) {
            int croppedHeight = Math.max(1, Math.round(width / targetAspect));
            y += Math.max(0, (height - croppedHeight) / 2);
            height = Math.min(height, croppedHeight);
        }
        return new TextureRegion(source.getTexture(), x, y, width, height);
    }
}
