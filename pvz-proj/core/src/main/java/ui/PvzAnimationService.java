package ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import models.core.zombie.ZombieType;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    public PamAnimationActor createZombieActor(ZombieType zombieType) {
        String pamPath = PvzAnimationCatalog.zombiePath(zombieType);
        PamAnimationActor actor = new PamAnimationActor(this, pamPath);
        applyArmorVisibility(actor, zombieType == null ? null : zombieType.getName(),
                zombieType == null ? null : zombieType.getDefaultArmorName());
        return actor;
    }

    public PamAnimationActor createZombieActor(String zombieName) {
        PamAnimationActor actor = new PamAnimationActor(this, PvzAnimationCatalog.zombiePath(zombieName));
        applyArmorVisibility(actor, zombieName, zombieName);
        return actor;
    }

    private void applyArmorVisibility(PamAnimationActor actor, String zombieName, String armorName) {
        if (actor == null) {
            return;
        }
        String normalizedName = normalize(zombieName);
        String normalizedArmor = normalize(armorName);
        if (normalizedArmor.contains("cone") || normalizedName.contains("cone")) {
            actor.setArmorVisibilityTokens("cone", "armor", "armor1", "helmet");
        } else if (normalizedArmor.contains("bucket") || normalizedName.contains("bucket")) {
            actor.setArmorVisibilityTokens("bucket", "armor", "armor2", "helmet");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public PamPlayer getPamPlayer() {
        return pamPlayer;
    }


    public TextureRegion region(String imageResourceId) {
        if (!available || textureBank == null || imageResourceId == null || imageResourceId.isBlank()) {
            return null;
        }
        try {
            return textureBank.region(imageResourceId);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    Map<String, Boolean> armorVisibility(String pamPath, List<String> tokens) {
        if (!available || pamPlayer == null || pamPath == null || pamPath.isBlank()
                || tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            PamPlayer.AnimationPart root = pamPlayer.getParts(pamPath);
            Set<String> partNames = new LinkedHashSet<>();
            collectPartNames(root, partNames);
            List<String> normalizedTokens = new ArrayList<>();
            for (String token : tokens) {
                String normalized = normalize(token);
                if (!normalized.isEmpty()) {
                    normalizedTokens.add(normalized);
                }
            }
            List<String> candidates = new ArrayList<>();
            for (String partName : partNames) {
                String normalizedPart = normalize(partName);
                for (String token : normalizedTokens) {
                    if (normalizedPart.contains(token)) {
                        candidates.add(partName);
                        break;
                    }
                }
            }
            if (candidates.isEmpty()) {
                return Collections.emptyMap();
            }
            boolean hasDamageVariants = false;
            for (String candidate : candidates) {
                if (armorDamageStage(candidate, normalizedTokens) > 0) {
                    hasDamageVariants = true;
                    break;
                }
            }
            Map<String, Boolean> visibility = new LinkedHashMap<>();
            for (String candidate : candidates) {
                visibility.put(candidate, !hasDamageVariants || armorDamageStage(candidate, normalizedTokens) == 0);
            }
            return visibility;
        } catch (RuntimeException ignored) {
            return Collections.emptyMap();
        }
    }

    private void collectPartNames(PamPlayer.AnimationPart part, Set<String> result) {
        if (part == null) {
            return;
        }
        if (part.name != null && !part.name.isBlank()) {
            result.add(part.name);
        }
        if (part.children != null) {
            for (PamPlayer.AnimationPart child : part.children) {
                collectPartNames(child, result);
            }
        }
    }

    private int armorDamageStage(String partName, List<String> tokens) {
        String normalized = normalize(partName);
        if (containsDamageMarker(normalized, 2)) {
            return 2;
        }
        if (containsDamageMarker(normalized, 1)) {
            return 1;
        }
        for (String token : tokens) {
            if (normalized.endsWith(token + "3") || normalized.endsWith(token + "03")) {
                return 2;
            }
            if (normalized.endsWith(token + "2") || normalized.endsWith(token + "02")) {
                return 1;
            }
        }
        return 0;
    }

    private boolean containsDamageMarker(String value, int stage) {
        String marker = Integer.toString(stage);
        return value.contains("damage" + marker)
                || value.contains("damaged" + marker)
                || value.contains("dmg" + marker)
                || value.contains("crack" + marker)
                || value.contains("broken" + marker);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
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
