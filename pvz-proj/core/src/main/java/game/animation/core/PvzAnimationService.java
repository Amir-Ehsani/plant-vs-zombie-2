package game.animation.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;


public final class PvzAnimationService implements Disposable {
    public static final String ASSET_ROOT = "pvz-assets";
    public static final String RESOLUTION = "768";

    private TextureBank textureBank;
    private PamPlayer pamPlayer;
    private AnimationCatalog catalog;
    private boolean available;
    private String statusMessage;

    public PvzAnimationService() {
        initialize();
    }

    public void update() {
        if (available) {
            textureBank.update();
        }
    }

    public boolean preload(String pamPath) {
        if (!available || pamPath == null || pamPath.isBlank()) {
            return false;
        }
        try {
            pamPlayer.loadSync(pamPath);
            return true;
        } catch (RuntimeException exception) {
            statusMessage = "Could not preload PAM: " + pamPath;
            Gdx.app.error("PvzAnimationService", statusMessage, exception);
            return false;
        }
    }

    public boolean draw(
        Batch batch,
        String pamPath,
        String clip,
        float stateTime,
        float x,
        float y,
        float scale,
        boolean loop
    ) {
        if (!canDraw(pamPath, clip)) {
            return false;
        }
        pamPlayer.draw(batch, pamPath, clip, stateTime, x, y, scale, scale, loop);
        return true;
    }

    public TextureRegion region(String imageResourceId) {
        if (!available || imageResourceId == null || imageResourceId.isBlank()) {
            return null;
        }
        try {
            return textureBank.region(imageResourceId);
        } catch (RuntimeException exception) {
            Gdx.app.error("PvzAnimationService", "Could not load region " + imageResourceId, exception);
            return null;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getCatalogSize() {
        return catalog == null ? 0 : catalog.size();
    }

    @Override
    public void dispose() {
        if (textureBank != null) {
            textureBank.dispose();
            textureBank = null;
        }
        pamPlayer = null;
        catalog = null;
        available = false;
    }

    private void initialize() {
        FileHandle root = Gdx.files.internal(ASSET_ROOT);
        FileHandle resources = root.child("RESOURCES.json");
        FileHandle animations = root.child("animations.json");
        if (!root.exists() || !resources.exists() || !animations.exists()) {
            available = false;
            statusMessage = "pvz-assets is missing; running the stage-one fallback view.";
            return;
        }

        try {
            textureBank = new TextureBank(RESOLUTION, root);
            pamPlayer = new PamPlayer(textureBank, root);
            catalog = AnimationCatalog.load(animations);
            available = true;
            statusMessage = "libPVZ ready. Catalog entries: " + catalog.size();
        } catch (RuntimeException exception) {
            available = false;
            statusMessage = "pvz-assets could not be initialized.";
            Gdx.app.error("PvzAnimationService", statusMessage, exception);
            if (textureBank != null) {
                textureBank.dispose();
                textureBank = null;
            }
        }
    }

    private boolean canDraw(String pamPath, String clip) {
        if (!available || pamPath == null || clip == null) {
            return false;
        }
        AnimationDefinition definition = catalog.findByPath(pamPath);
        return definition != null && definition.hasClip(clip);
    }
}
