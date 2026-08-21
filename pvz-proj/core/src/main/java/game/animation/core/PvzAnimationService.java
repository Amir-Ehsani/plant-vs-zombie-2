package game.animation.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.io.File;

/**
 * Project-side adapter around libPVZ.
 * Keeps graphical asset handling outside models and controllers.
 */
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
        if (available && textureBank != null) {
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

            Gdx.app.error(
                "PvzAnimationService",
                statusMessage,
                exception
            );

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

        pamPlayer.draw(
            batch,
            pamPath,
            clip,
            stateTime,
            x,
            y,
            scale,
            scale,
            loop
        );

        return true;
    }

    public TextureRegion region(String imageResourceId) {
        if (!available
            || imageResourceId == null
            || imageResourceId.isBlank()) {
            return null;
        }

        try {
            return textureBank.region(imageResourceId);
        } catch (RuntimeException exception) {
            Gdx.app.error(
                "PvzAnimationService",
                "Could not load region: " + imageResourceId,
                exception
            );

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
        if (catalog == null) {
            return 0;
        }

        return catalog.size();
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
        FileHandle root = resolvePvzAssetsRoot();

        if (root == null) {
            available = false;
            statusMessage = "pvz-assets directory could not be resolved.";

            Gdx.app.error(
                "PvzAnimationService",
                statusMessage
            );

            return;
        }

        FileHandle animations = root.child("animations.json");

        if (!animations.exists()) {
            available = false;
            statusMessage =
                "animations.json was not found in: "
                    + root.file().getAbsolutePath();

            Gdx.app.error(
                "PvzAnimationService",
                statusMessage
            );

            return;
        }

        try {
            Gdx.app.log(
                "PvzAnimationService",
                "Using pvz-assets: "
                    + root.file().getAbsolutePath()
            );

            textureBank = new TextureBank(
                RESOLUTION,
                root
            );

            pamPlayer = new PamPlayer(
                textureBank,
                root
            );

            catalog = AnimationCatalog.load(animations);

            available = true;
            statusMessage =
                "libPVZ ready. Catalog entries: "
                    + catalog.size();

            Gdx.app.log(
                "PvzAnimationService",
                statusMessage
            );
        } catch (RuntimeException exception) {
            available = false;
            statusMessage =
                "pvz-assets could not be initialized.";

            Gdx.app.error(
                "PvzAnimationService",
                statusMessage,
                exception
            );

            if (textureBank != null) {
                textureBank.dispose();
                textureBank = null;
            }

            pamPlayer = null;
            catalog = null;
        }
    }

    private FileHandle resolvePvzAssetsRoot() {
        String workingDirectory =
            System.getProperty("user.dir");

        File currentDirectory =
            new File(workingDirectory);

        File[] candidates = {
            new File(
                currentDirectory,
                ASSET_ROOT
            ),
            new File(
                currentDirectory,
                "assets/" + ASSET_ROOT
            ),
            createParentCandidate(
                currentDirectory
            ),
            createGrandParentCandidate(
                currentDirectory
            )
        };

        for (File candidate : candidates) {
            if (candidate == null) {
                continue;
            }

            FileHandle root = Gdx.files.absolute(
                candidate.getAbsolutePath()
            );

            if (isValidPvzAssetsRoot(root)) {
                return root;
            }
        }

        logAssetCandidates(candidates);

        return null;
    }

    private File createParentCandidate(
        File currentDirectory
    ) {
        File parent = currentDirectory.getParentFile();

        if (parent == null) {
            return null;
        }

        return new File(
            parent,
            "assets/" + ASSET_ROOT
        );
    }

    private File createGrandParentCandidate(
        File currentDirectory
    ) {
        File parent = currentDirectory.getParentFile();

        if (parent == null) {
            return null;
        }

        File grandParent = parent.getParentFile();

        if (grandParent == null) {
            return null;
        }

        return new File(
            grandParent,
            "assets/" + ASSET_ROOT
        );
    }

    private boolean isValidPvzAssetsRoot(
        FileHandle root
    ) {
        if (!root.exists()) {
            return false;
        }

        boolean hasResources =
            hasResourcesJson(root);

        boolean hasAnimations =
            root.child("animations.json").exists();

        boolean hasAtlases =
            root.child("ATLASES").isDirectory()
                || root.child("atlases").isDirectory();

        boolean hasImages =
            root.child("IMAGES").isDirectory()
                || root.child("images").isDirectory()
                || root.child("pam").isDirectory();

        return hasResources
            && hasAnimations
            && hasAtlases
            && hasImages;
    }

    private boolean hasResourcesJson(
        FileHandle root
    ) {
        return root.child("RESOURCES.json").exists()
            || root.child("Resources.json").exists()
            || root.child("resources.json").exists();
    }

    private void logAssetCandidates(
        File[] candidates
    ) {
        Gdx.app.error(
            "PvzAnimationService",
            "Could not find a valid pvz-assets directory."
        );

        for (File candidate : candidates) {
            if (candidate == null) {
                continue;
            }

            Gdx.app.log(
                "PvzAnimationService",
                "Checked: "
                    + candidate.getAbsolutePath()
            );
        }
    }

    private boolean canDraw(
        String pamPath,
        String clip
    ) {
        if (!available
            || pamPath == null
            || clip == null
            || catalog == null) {
            return false;
        }

        AnimationDefinition definition =
            catalog.findByPath(pamPath);

        return definition != null
            && definition.hasClip(clip);
    }
}
