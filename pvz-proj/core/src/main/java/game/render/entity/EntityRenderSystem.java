package game.render.entity;

import com.badlogic.gdx.graphics.g2d.Batch;
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
    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final EntityAnimationRegistry registry;
    private final Map<Plant, PlantView> plantViews = new IdentityHashMap<>();
    private final Map<Zombie, ZombieView> zombieViews = new IdentityHashMap<>();
    private final List<ZombiePartVisual> detachedParts = new ArrayList<>();
    private final List<ZombieDeathVisual> deathVisuals = new ArrayList<>();
    private final List<ZombieHeadVisual> deathHeads = new ArrayList<>();
    private final List<PlantActionVisual> plantActionVisuals = new ArrayList<>();
    private final List<PlantFieldEffectVisual> fieldEffectVisuals = new ArrayList<>();

    public EntityRenderSystem(BoardGeometry geometry, PvzAnimationService animations) {
        if (geometry == null || animations == null || animations.getCatalog() == null) {
            throw new IllegalArgumentException("Entity renderer requires board geometry and animation catalog.");
        }
        this.geometry = geometry;
        this.animations = animations;
        registry = new EntityAnimationRegistry(animations.getCatalog());
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
            profile,
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
            activeZombies.add(zombie);
            ZombieView view = zombieViews.computeIfAbsent(zombie, this::createZombieView);
            if (view == null) {
                continue;
            }
            view.update(delta, board);
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
        return new PlantView(plant, profile);
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
