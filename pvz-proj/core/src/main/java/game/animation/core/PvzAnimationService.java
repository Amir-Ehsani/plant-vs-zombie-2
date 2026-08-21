package game.animation.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PvzAnimationService implements Disposable {
    public static final String ASSET_ROOT = "pvz-assets";
    public static final String RESOLUTION = "768";

    private final Map<String, Set<String>> partNamesCache = new LinkedHashMap<>();
    private final Map<String, Map<String, Boolean>> visibilityCache = new LinkedHashMap<>();

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
            Gdx.app.error("PvzAnimationService", "Could not preload PAM: " + pamPath, exception);
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
        return draw(batch, pamPath, clip, stateTime, x, y, scale, loop, Collections.emptyMap());
    }

    public boolean draw(
        Batch batch,
        String pamPath,
        String clip,
        float stateTime,
        float x,
        float y,
        float scale,
        boolean loop,
        Map<String, Boolean> visibility
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
            loop,
            visibility == null || visibility.isEmpty() ? null : visibility
        );
        return true;
    }

    public TextureRegion region(String imageResourceId) {
        if (!available || imageResourceId == null || imageResourceId.isBlank()) {
            return null;
        }
        try {
            return textureBank.region(imageResourceId);
        } catch (RuntimeException exception) {
            Gdx.app.error("PvzAnimationService", "Could not load region: " + imageResourceId, exception);
            return null;
        }
    }

    public Map<String, Boolean> visibilityForTokens(String pamPath, Collection<String> tokens) {
        if (!available || pamPath == null || tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> normalizedTokens = new ArrayList<>();
        for (String token : tokens) {
            String normalized = normalizeToken(token);
            if (!normalized.isEmpty() && !normalizedTokens.contains(normalized)) {
                normalizedTokens.add(normalized);
            }
        }
        if (normalizedTokens.isEmpty()) {
            return Collections.emptyMap();
        }
        Collections.sort(normalizedTokens);
        String key = pamPath + "|" + String.join(",", normalizedTokens);
        Map<String, Boolean> cached = visibilityCache.get(key);
        if (cached != null) {
            return cached;
        }

        Map<String, Boolean> visibility = new LinkedHashMap<>();
        for (String partName : partNames(pamPath)) {
            String normalizedPart = normalizeToken(partName);
            for (String token : normalizedTokens) {
                if (normalizedPart.contains(token)) {
                    visibility.put(partName, true);
                    break;
                }
            }
        }
        Map<String, Boolean> result = Collections.unmodifiableMap(visibility);
        visibilityCache.put(key, result);
        return result;
    }

    public AnimationCatalog getCatalog() {
        return catalog;
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
        partNamesCache.clear();
        visibilityCache.clear();
        available = false;
    }

    private Set<String> partNames(String pamPath) {
        Set<String> cached = partNamesCache.get(pamPath);
        if (cached != null) {
            return cached;
        }
        if (!preload(pamPath)) {
            return Collections.emptySet();
        }
        PamPlayer.AnimationPart root = pamPlayer.getParts(pamPath);
        Set<String> names = new LinkedHashSet<>();
        collectPartNames(root, names);
        Set<String> result = Collections.unmodifiableSet(names);
        partNamesCache.put(pamPath, result);
        return result;
    }

    private void collectPartNames(PamPlayer.AnimationPart part, Set<String> names) {
        if (part == null) {
            return;
        }
        if (part.name != null && !part.name.isBlank()) {
            names.add(part.name);
        }
        if (part.children != null) {
            for (PamPlayer.AnimationPart child : part.children) {
                collectPartNames(child, names);
            }
        }
    }

    private void initialize() {
        FileHandle root = resolvePvzAssetsRoot();
        if (root == null) {
            available = false;
            statusMessage = "pvz-assets directory could not be resolved.";
            Gdx.app.error("PvzAnimationService", statusMessage);
            return;
        }

        FileHandle animations = root.child("animations.json");
        if (!animations.exists()) {
            available = false;
            statusMessage = "animations.json was not found in: " + root.file().getAbsolutePath();
            Gdx.app.error("PvzAnimationService", statusMessage);
            return;
        }

        try {
            Gdx.app.log("PvzAnimationService", "Using pvz-assets: " + root.file().getAbsolutePath());
            textureBank = new TextureBank(RESOLUTION, root);
            pamPlayer = new PamPlayer(textureBank, root);
            catalog = AnimationCatalog.load(animations);
            available = true;
            statusMessage = "libPVZ ready. Catalog entries: " + catalog.size();
            Gdx.app.log("PvzAnimationService", statusMessage);
        } catch (RuntimeException exception) {
            available = false;
            statusMessage = "pvz-assets could not be initialized.";
            Gdx.app.error("PvzAnimationService", statusMessage, exception);
            if (textureBank != null) {
                textureBank.dispose();
                textureBank = null;
            }
            pamPlayer = null;
            catalog = null;
        }
    }

    private FileHandle resolvePvzAssetsRoot() {
        File currentDirectory = new File(System.getProperty("user.dir"));
        File[] candidates = {
            new File(currentDirectory, ASSET_ROOT),
            new File(currentDirectory, "assets/" + ASSET_ROOT),
            createParentCandidate(currentDirectory),
            createGrandParentCandidate(currentDirectory)
        };

        for (File candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            FileHandle root = Gdx.files.absolute(candidate.getAbsolutePath());
            if (isValidPvzAssetsRoot(root)) {
                return root;
            }
        }
        logAssetCandidates(candidates);
        return null;
    }

    private File createParentCandidate(File currentDirectory) {
        File parent = currentDirectory.getParentFile();
        return parent == null ? null : new File(parent, "assets/" + ASSET_ROOT);
    }

    private File createGrandParentCandidate(File currentDirectory) {
        File parent = currentDirectory.getParentFile();
        if (parent == null || parent.getParentFile() == null) {
            return null;
        }
        return new File(parent.getParentFile(), "assets/" + ASSET_ROOT);
    }

    private boolean isValidPvzAssetsRoot(FileHandle root) {
        if (!root.exists()) {
            return false;
        }
        boolean hasResources = root.child("RESOURCES.json").exists()
            || root.child("Resources.json").exists()
            || root.child("resources.json").exists();
        boolean hasAnimations = root.child("animations.json").exists();
        boolean hasAtlases = root.child("ATLASES").isDirectory()
            || root.child("atlases").isDirectory();
        boolean hasImages = root.child("IMAGES").isDirectory()
            || root.child("images").isDirectory()
            || root.child("pam").isDirectory();
        return hasResources && hasAnimations && hasAtlases && hasImages;
    }

    private void logAssetCandidates(File[] candidates) {
        Gdx.app.error("PvzAnimationService", "Could not find a valid pvz-assets directory.");
        for (File candidate : candidates) {
            if (candidate != null) {
                Gdx.app.log("PvzAnimationService", "Checked: " + candidate.getAbsolutePath());
            }
        }
    }

    private boolean canDraw(String pamPath, String clip) {
        if (!available || pamPath == null || clip == null || catalog == null) {
            return false;
        }
        AnimationDefinition definition = catalog.findByPath(pamPath);
        return definition != null && definition.hasClip(clip);
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
