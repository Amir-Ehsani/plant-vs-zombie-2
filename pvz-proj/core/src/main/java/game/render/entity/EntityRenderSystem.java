package game.render.entity;

import com.badlogic.gdx.graphics.g2d.Batch;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.EntityAnimationRegistry;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.plant.Plant;
import models.core.zombie.Zombie;
import models.engine.board.Board;
import models.engine.board.Lane;
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
    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final EntityAnimationRegistry registry;
    private final Map<Plant, PlantView> plantViews = new IdentityHashMap<>();
    private final Map<Zombie, ZombieView> zombieViews = new IdentityHashMap<>();
    private final List<ZombiePartVisual> detachedParts = new ArrayList<>();
    private final List<ZombieDeathVisual> deathVisuals = new ArrayList<>();

    public EntityRenderSystem(BoardGeometry geometry, PvzAnimationService animations) {
        if (geometry == null || animations == null || animations.getCatalog() == null) {
            throw new IllegalArgumentException("Entity renderer requires board geometry and animation catalog.");
        }
        this.geometry = geometry;
        this.animations = animations;
        registry = new EntityAnimationRegistry(animations.getCatalog());
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

        plantViews.keySet().removeIf(plant -> !activePlants.contains(plant));
        zombieViews.keySet().removeIf(zombie -> !activeZombies.contains(zombie));
    }

    public void render(Batch batch, Board board) {
        if (batch == null || board == null) {
            return;
        }
        batch.begin();
        for (int row = 1; row <= board.getHeight(); row++) {
            Lane lane = board.getLaneAt(row);
            if (lane == null) {
                continue;
            }
            renderPlantsInLane(batch, board, lane);
            renderZombiesInLane(batch, board, lane);
            renderDetachedPartsInLane(batch, row);
            renderDeathsInLane(batch, row);
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
            ZombieDeathVisual death = entry.getValue().createDeathVisual();
            if (death != null) {
                deathVisuals.add(death);
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
