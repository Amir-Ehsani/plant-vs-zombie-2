package game.render.entity;


import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.EntityAnimationRegistry;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.plant.Plant;
import models.core.plant.PlantType;
import models.core.zombie.Zombie;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.level.core.SeasonType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EntityRenderSystem {
    private static final float CHERRY_BOMB_PLAYBACK_RATE = 0.62f;
    private static final float DOOM_SHROOM_ACTION_SCALE = 0.80f;
    private static final float CRATER_WIDTH_RATIO = 0.64f;
    private static final String CRATER_REGION = "IMAGE_EFFECTS_CRATER_CRATER_84X53";
    private static final String EGYPT_GRAVE_PATH =
        "768/INITIAL/GRAVESTONES/EGYPT_HIEROGLYPH/EGYPT_HIEROGLYPH.PAM";
    private static final String DARK_GRAVE_PATH =
        "768/FULL/GRAVESTONES/DARK_NOOP/DARK_NOOP.PAM";
    private static final String DARK_SUN_GRAVE_PATH =
        "768/FULL/GRAVESTONES/DARK_SUN/DARK_SUN.PAM";
    private static final String DARK_PLANT_FOOD_GRAVE_PATH =
        "768/FULL/GRAVESTONES/DARK_PLANTFOOD/DARK_PLANTFOOD.PAM";
    private static final float GRAVE_SCALE = 0.52f;
    private static final String FROSTBITE_ICE_BLOCK_PATH =
        "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_ZOMBIE/FROSTBITE_ICE_BLOCK_ZOMBIE.PAM";
    private static final String ARCADE_CABINET_PATH =
        "768/FULL/EFFECTS/80S_ARCADE_CABINET/80S_ARCADE_CABINET.PAM";
    private static final String BARREL_PATH =
        "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER_BARREL/"
            + "ZOMBIE_PIRATE_BARREL_PUSHER_BARREL.PAM";
    private static final float ICE_BLOCK_SCALE = 0.47f;
    private static final float ARCADE_CABINET_SCALE = 0.50f;
    private static final float BARREL_SCALE = 0.48f;
    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final EntityAnimationRegistry registry;
    private final boolean showPlantDamageAppearance;
    private final SeasonType seasonType;
    private final Map<Plant, PlantView> plantViews = new IdentityHashMap<>();
    private final Map<Zombie, ZombieView> zombieViews = new IdentityHashMap<>();
    private final List<ZombiePartVisual> detachedParts = new ArrayList<>();
    private final List<ZombieDeathVisual> deathVisuals = new ArrayList<>();
    private final List<ZombieHeadVisual> deathHeads = new ArrayList<>();
    private final List<PlantActionVisual> plantActionVisuals = new ArrayList<>();
    private final List<PlantFieldEffectVisual> fieldEffectVisuals = new ArrayList<>();
    private TextureRegion craterRegion;
    private float terrainObjectTime;

    public EntityRenderSystem(BoardGeometry geometry, PvzAnimationService animations) {
        this(geometry, animations, true, null);
    }

    public EntityRenderSystem(
            BoardGeometry geometry,
            PvzAnimationService animations,
            boolean showPlantDamageAppearance
    ) {
        this(geometry, animations, showPlantDamageAppearance, null);
    }

    public EntityRenderSystem(
            BoardGeometry geometry,
            PvzAnimationService animations,
            SeasonType seasonType
    ) {
        this(geometry, animations, true, seasonType);
    }

    private EntityRenderSystem(
            BoardGeometry geometry,
            PvzAnimationService animations,
            boolean showPlantDamageAppearance,
            SeasonType seasonType
    ) {
        if (geometry == null || animations == null || animations.getCatalog() == null) {
            throw new IllegalArgumentException("Entity renderer requires board geometry and animation catalog.");
        }
        this.geometry = geometry;
        this.animations = animations;
        this.showPlantDamageAppearance = showPlantDamageAppearance;
        this.seasonType = seasonType;
        registry = new EntityAnimationRegistry(animations.getCatalog());
        preloadGraveAnimations();
        preloadInteractiveTerrainAnimations();
    }

    public void playPlantAction(PlantType type, Position position, String clip) {
        if (type == null || position == null || clip == null || clip.isBlank()) {
            return;
        }
        EntityAnimationProfile profile = registry.forPlantType(type);
        if (profile == null || !profile.getDefinition().hasClip(clip)) {
            return;
        }
        animations.preload(profile.getPath());
        plantActionVisuals.add(new PlantActionVisual(
            actionProfile(type, profile),
            clip,
            position.getX(),
            position.getY(),
            actionPlaybackRate(type)
        ));
    }

    public void playFieldEffect(
        String path,
        String clip,
        List<Position> positions,
        float scale,
        float delay,
        float duration,
        boolean loop
    ) {
        if (path == null || clip == null || positions == null || positions.isEmpty()) {
            return;
        }
        game.animation.core.AnimationDefinition definition = animations.getCatalog().findByPath(path);
        if (definition == null || !definition.hasClip(clip)) {
            return;
        }
        animations.preload(definition.getPath());
        fieldEffectVisuals.add(new PlantFieldEffectVisual(
            definition, clip, positions, scale, delay, duration, loop
        ));
    }

    private EntityAnimationProfile actionProfile(
        PlantType type, EntityAnimationProfile profile
    ) {
        if (type != null && type.getName() != null
                && type.getName().trim().equalsIgnoreCase("Doom Shroom")) {
            return new EntityAnimationProfile(
                profile.getDefinition(), profile.getScale() * DOOM_SHROOM_ACTION_SCALE
            );
        }
        return profile;
    }

    private float actionPlaybackRate(PlantType type) {
        if (type != null && type.getName() != null
            && type.getName().trim().equalsIgnoreCase("Cherry Bomb")) {
            return CHERRY_BOMB_PLAYBACK_RATE;
        }
        return 1f;
    }

    public void update(float delta, Board board) {
        if (board == null) {
            return;
        }
        if (delta > 0f) {
            terrainObjectTime += delta;
        }
        Set<Plant> activePlants = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Zombie> activeZombies = Collections.newSetFromMap(new IdentityHashMap<>());

        updatePlantViews(delta, board, activePlants);
        updateZombieViews(delta, board, activeZombies);
        captureRemovedZombieDeaths(activeZombies);
        updateDetachedParts(delta);
        updateDeathVisuals(delta);
        updateDeathHeads(delta);
        updatePlantActionVisuals(delta);
        updateFieldEffectVisuals(delta);

        plantViews.keySet().removeIf(plant -> !activePlants.contains(plant));
        zombieViews.keySet().removeIf(zombie -> !activeZombies.contains(zombie));
    }

    public void render(Batch batch, Board board) {
        if (batch == null || board == null) {
            return;
        }
        batch.begin();
        renderTerrainOverlays(batch, board);
        renderFieldEffects(batch);
        for (int row = 1; row <= board.getHeight(); row++) {
            Lane lane = board.getLaneAt(row);
            if (lane == null) {
                continue;
            }
            renderPlantsInLane(batch, board, lane);
            renderPlantActionsInLane(batch, row);
            renderZombiesInLane(batch, board, lane);
            renderDetachedPartsInLane(batch, row);
            renderDeathsInLane(batch, row);
            renderDeathHeadsInLane(batch, row);
        }
        batch.end();
    }

    private void updatePlantViews(float delta, Board board, Set<Plant> activePlants) {
        for (Plant plant : board.getAllPlants()) {
            activePlants.add(plant);
            PlantView view = plantViews.computeIfAbsent(plant, this::createPlantView);
            if (view != null) {
                view.update(delta, board);
            }
        }
    }

    private void updateZombieViews(float delta, Board board, Set<Zombie> activeZombies) {
        for (Zombie zombie : board.getAllZombies()) {
            if (isBossHitbox(zombie)) {
                continue;
            }
            activeZombies.add(zombie);
            ZombieView view = zombieViews.computeIfAbsent(zombie, this::createZombieView);
            if (view == null) {
                continue;
            }
            view.update(delta, board);
            detachedParts.addAll(view.takeDetachedArmorVisuals(animations));
            ZombiePartVisual part = view.takeDetachedArmVisual(animations);
            if (part != null) {
                detachedParts.add(part);
            }
        }
    }

    private void captureRemovedZombieDeaths(Set<Zombie> activeZombies) {
        for (Map.Entry<Zombie, ZombieView> entry : zombieViews.entrySet()) {
            Zombie zombie = entry.getKey();
            if (activeZombies.contains(zombie) || zombie == null || zombie.isAlive()) {
                continue;
            }
            ZombieView view = entry.getValue();
            detachedParts.addAll(view.takeDetachedArmorVisuals(animations));
            ZombieDeathVisual death = view.createDeathVisual(animations);
            if (death != null) {
                deathVisuals.add(death);
            }
            ZombieHeadVisual head = view.createDeathHeadVisual(animations);
            if (head != null) {
                deathHeads.add(head);
            }
        }
    }

    private void updateDetachedParts(float delta) {
        Iterator<ZombiePartVisual> iterator = detachedParts.iterator();
        while (iterator.hasNext()) {
            ZombiePartVisual part = iterator.next();
            part.update(delta);
            if (part.isFinished()) iterator.remove();
        }
    }

    private void updateDeathVisuals(float delta) {
        Iterator<ZombieDeathVisual> iterator = deathVisuals.iterator();
        while (iterator.hasNext()) {
            ZombieDeathVisual death = iterator.next();
            death.update(delta);
            if (death.isFinished()) iterator.remove();
        }
    }

    private void updateDeathHeads(float delta) {
        Iterator<ZombieHeadVisual> iterator = deathHeads.iterator();
        while (iterator.hasNext()) {
            ZombieHeadVisual head = iterator.next();
            head.update(delta);
            if (head.isFinished()) {
                iterator.remove();
            }
        }
    }

    private void updatePlantActionVisuals(float delta) {
        Iterator<PlantActionVisual> iterator = plantActionVisuals.iterator();
        while (iterator.hasNext()) {
            PlantActionVisual visual = iterator.next();
            visual.update(delta);
            if (visual.isFinished()) {
                iterator.remove();
            }
        }
    }

    private void updateFieldEffectVisuals(float delta) {
        Iterator<PlantFieldEffectVisual> iterator = fieldEffectVisuals.iterator();
        while (iterator.hasNext()) {
            PlantFieldEffectVisual visual = iterator.next();
            visual.update(delta);
            if (visual.isFinished()) {
                iterator.remove();
            }
        }
    }

    private void renderTerrainOverlays(Batch batch, Board board) {
        renderInteractiveTerrainObjects(batch, board);
        renderCraters(batch, board);
        renderGraves(batch, board);
    }

    private void renderInteractiveTerrainObjects(Batch batch, Board board) {
        for (int row = 1; row <= board.getHeight(); row++) {
            Lane lane = board.getLaneAt(row);
            if (lane == null) {
                continue;
            }
            for (Tile tile : lane.getTiles()) {
                TileType type = tile.getTileType();
                if (type != TileType.ICE && type != TileType.ARCADE && type != TileType.BARREL) {
                    continue;
                }
                Vector2 position = geometry.entityToScreen(
                    tile.getPosition().getX(), tile.getPosition().getY()
                );
                if (type == TileType.ICE) {
                    animations.draw(
                        batch, FROSTBITE_ICE_BLOCK_PATH, "idle", terrainObjectTime,
                        position.x, position.y, ICE_BLOCK_SCALE, true
                    );
                } else if (type == TileType.ARCADE) {
                    animations.draw(
                        batch, ARCADE_CABINET_PATH, "idle", terrainObjectTime,
                        position.x, position.y, ARCADE_CABINET_SCALE, true
                    );
                } else {
                    animations.draw(
                        batch, BARREL_PATH, "roll", terrainObjectTime,
                        position.x, position.y, BARREL_SCALE, true
                    );
                }
            }
        }
    }

    private void renderCraters(Batch batch, Board board) {
        if (craterRegion == null) {
            craterRegion = animations.region(CRATER_REGION);
        }
        if (craterRegion == null) {
            return;
        }
        for (int row = 1; row <= board.getHeight(); row++) {
            Lane lane = board.getLaneAt(row);
            if (lane == null) {
                continue;
            }
            for (Tile tile : lane.getTiles()) {
                if (tile.getTileType() != TileType.CRATER) {
                    continue;
                }
                Rectangle bounds = geometry.getTileBounds(
                    tile.getPosition().getY(), tile.getPosition().getX()
                );
                float width = bounds.width * CRATER_WIDTH_RATIO;
                float aspect = craterRegion.getRegionHeight()
                    / (float) Math.max(1, craterRegion.getRegionWidth());
                float height = Math.min(bounds.height * 0.48f, width * aspect);
                batch.draw(
                    craterRegion,
                    bounds.x + (bounds.width - width) * 0.5f,
                    bounds.y + bounds.height * 0.16f,
                    width,
                    height
                );
            }
        }
    }

    private void renderGraves(Batch batch, Board board) {
        for (int row = 1; row <= board.getHeight(); row++) {
            Lane lane = board.getLaneAt(row);
            if (lane == null) {
                continue;
            }
            for (Tile tile : lane.getTiles()) {
                if (!tile.isGraveTerrain()) {
                    continue;
                }
                String path = gravePath(tile.getTileType());
                String clip = graveDamageClip(tile);
                Vector2 position = geometry.entityToScreen(
                    tile.getPosition().getX(), tile.getPosition().getY()
                );
                animations.draw(batch, path, clip, 0f, position.x, position.y, GRAVE_SCALE, false);
            }
        }
    }

    private String gravePath(TileType type) {
        if (type == TileType.SUN_GRAVE) {
            return DARK_SUN_GRAVE_PATH;
        }
        if (type == TileType.PLANT_FOOD_GRAVE) {
            return DARK_PLANT_FOOD_GRAVE_PATH;
        }
        return seasonType == SeasonType.DARK_AGES ? DARK_GRAVE_PATH : EGYPT_GRAVE_PATH;
    }

    private String graveDamageClip(Tile tile) {
        int maximum = Math.max(1, tile.getMaximumTerrainHealth());
        double ratio = tile.getTerrainHealth() / (double) maximum;
        if (ratio > 0.80) {
            return "undamaged";
        }
        if (ratio > 0.60) {
            return "damage1";
        }
        if (ratio > 0.40) {
            return "damage2";
        }
        if (ratio > 0.20) {
            return "damage3";
        }
        return "damage4";
    }

    private void preloadGraveAnimations() {
        animations.preload(EGYPT_GRAVE_PATH);
        animations.preload(DARK_GRAVE_PATH);
        animations.preload(DARK_SUN_GRAVE_PATH);
        animations.preload(DARK_PLANT_FOOD_GRAVE_PATH);
    }

    private void preloadInteractiveTerrainAnimations() {
        animations.preload(FROSTBITE_ICE_BLOCK_PATH);
        animations.preload(ARCADE_CABINET_PATH);
        animations.preload(BARREL_PATH);
    }

    private void renderFieldEffects(Batch batch) {
        for (PlantFieldEffectVisual visual : fieldEffectVisuals) {
            visual.render(batch, geometry, animations);
        }
    }

    private void renderPlantsInLane(Batch batch, Board board, Lane lane) {
        for (Tile tile : lane.getTiles()) {
            List<Plant> plants = new ArrayList<>(tile.getPlants());
            plants.sort(Comparator.comparingInt(this::plantLayerOrder));
            for (Plant plant : plants) {
                PlantView view = plantViews.get(plant);
                if (view != null) {
                    view.render(batch, geometry, animations, board);
                }
            }
        }
    }

    private void renderPlantActionsInLane(Batch batch, int row) {
        for (PlantActionVisual visual : plantActionVisuals) {
            if (visual.getLane() == row) {
                visual.render(batch, geometry, animations);
            }
        }
    }

    private void renderZombiesInLane(Batch batch, Board board, Lane lane) {
        for (Zombie zombie : lane.getAllZombies()) {
            ZombieView view = zombieViews.get(zombie);
            if (view != null) {
                view.render(batch, geometry, animations, board);
            }
        }
    }

    private void renderDetachedPartsInLane(Batch batch, int row) {
        for (ZombiePartVisual part : detachedParts) {
            if (part.getLane() == row) {
                part.render(batch, geometry, animations);
            }
        }
    }

    private void renderDeathsInLane(Batch batch, int row) {
        for (ZombieDeathVisual death : deathVisuals) {
            if (death.getLane() == row) {
                death.render(batch, geometry, animations);
            }
        }
    }

    private void renderDeathHeadsInLane(Batch batch, int row) {
        for (ZombieHeadVisual head : deathHeads) {
            if (head.getLane() == row) {
                head.render(batch, geometry, animations);
            }
        }
    }

    private PlantView createPlantView(Plant plant) {
        EntityAnimationProfile profile = registry.forPlant(plant);
        if (profile == null) {
            return null;
        }
        animations.preload(profile.getPath());
        return new PlantView(plant, profile, showPlantDamageAppearance);
    }

    private boolean isBossHitbox(Zombie zombie) {
        return zombie != null && zombie.getType() != null
                && zombie.getType().hasTag("boss_hitbox");
    }

    private ZombieView createZombieView(Zombie zombie) {
        EntityAnimationProfile profile = registry.forZombie(zombie);
        if (profile == null) {
            return null;
        }
        animations.preload(profile.getPath());
        return new ZombieView(zombie, profile);
    }

    private int plantLayerOrder(Plant plant) {
        String name = plant == null ? "" : plant.getName().toLowerCase();
        if (name.equals("lily pad")) {
            return 0;
        }
        if (name.equals("pumpkin")) {
            return 2;
        }
        return 1;
    }
}
