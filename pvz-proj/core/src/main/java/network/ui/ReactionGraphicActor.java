package network.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * In-match reaction sticker. Each catalog value maps to a generated PNG.
 */
public final class ReactionGraphicActor extends Group {
    private static final float ART_SIZE = 120f;

    private final ReactionArt art;
    private final Image image;

    public ReactionGraphicActor(ReactionArt art) {
        this.art = art;
        this.image = new Image();
        image.setScaling(Scaling.fit);
        image.setFillParent(true);
        addActor(image);
        setSize(ART_SIZE, ART_SIZE);
        setOrigin(ART_SIZE / 2f, ART_SIZE / 2f);
        setTransform(true);
        setVisible(false);
    }

    public void setGraphic(String category, String value) {
        TextureRegion region = art == null ? null : art.region(category, value);
        if (region == null) {
            image.setDrawable(null);
            setVisible(false);
            return;
        }
        image.setDrawable(new TextureRegionDrawable(region));
        setVisible(true);
    }

    public void dispose() {
        clearChildren();
    }
}
