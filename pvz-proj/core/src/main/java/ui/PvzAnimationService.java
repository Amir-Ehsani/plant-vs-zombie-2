package ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

public class PvzAnimationService {
    private static final String RESOLUTION = "768";
    private final TextureBank textureBank;
    private final PamPlayer pamPlayer;
    private final boolean available;

    public PvzAnimationService() {
        TextureBank createdTextureBank = null;
        PamPlayer createdPamPlayer = null;
        boolean createdAvailable = false;
        try {
            FileHandle assetsFolder = Gdx.files.internal("pvz-assets");
            if (hasRequiredAssets(assetsFolder)) {
                createdTextureBank = new TextureBank(RESOLUTION, assetsFolder);
                createdPamPlayer = new PamPlayer(createdTextureBank, assetsFolder);
                createdAvailable = true;
            }
        } catch (RuntimeException ignored) {
            if (createdTextureBank != null) {
                createdTextureBank.dispose();
                createdTextureBank = null;
            }
            createdPamPlayer = null;
        }
        textureBank = createdTextureBank;
        pamPlayer = createdPamPlayer;
        available = createdAvailable;
    }

    public PamAnimationActor createPlantActor(String plantName) {
        return new PamAnimationActor(this, PvzAnimationCatalog.plantPath(plantName));
    }

    public PamAnimationActor createZombieActor(String zombieName) {
        return new PamAnimationActor(this, PvzAnimationCatalog.zombiePath(zombieName));
    }

    public boolean isAvailable() {
        return available;
    }

    public PamPlayer getPamPlayer() {
        return pamPlayer;
    }

    public void update() {
        if (textureBank != null) {
            textureBank.update();
        }
    }

    public void dispose() {
        if (textureBank != null) {
            textureBank.dispose();
        }
    }

    private boolean hasRequiredAssets(FileHandle root) {
        if (root == null || !root.exists()) {
            return false;
        }
        boolean hasResources = root.child("RESOURCES.json").exists() || root.child("resources.json").exists();
        boolean hasAtlases = root.child("ATLASES").exists() || root.child("atlases").exists();
        boolean hasImages = root.child("IMAGES").exists() || root.child("pam").exists();
        return hasResources && hasAtlases && hasImages;
    }
}
