package game.render.projectile;
import audio.AudioCue;
import audio.AudioManager;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.AnimationDefinition;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.EntityAnimationRegistry;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.plant.Plant;
import models.core.plant.PlantActionTiming;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
public final class ProjectileRenderSystem {
    private static final float MIN_TRAVEL_SECONDS = 0.20f;
    private static final float MAX_TRAVEL_SECONDS = 0.90f;
    private static final float TILES_PER_SECOND = 8.5f;
    private static final float LOB_ARC_TILES = 1.30f;
    private static final float IMPACT_SECONDS = 0.55f;
    private static final float SHOT_STAGGER_SECONDS = 0.07f;
    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final EntityAnimationRegistry entityAnimations;
    private final Map<Plant, Integer> previousAttackSerials = new IdentityHashMap<>();
    private final Map<Plant, Integer> previousPlantFoodSerials = new IdentityHashMap<>();
    private final List<ZombieSnapshot> previousZombies = new ArrayList<>();
    private final List<VisualProjectile> projectiles = new ArrayList<>();
    private final List<ImpactVisual> impacts = new ArrayList<>();
    private final Map<ProjectileVisualType, VisualDefinition> definitions = new java.util.EnumMap<>(
        ProjectileVisualType.class
    );
    public ProjectileRenderSystem(BoardGeometry geometry, PvzAnimationService animations) {
        if (geometry == null || animations == null || animations.getCatalog() == null) {
            throw new IllegalArgumentException("Projectile renderer requires geometry and animations.");
        }
        this.geometry = geometry;
        this.animations = animations;
        entityAnimations = new EntityAnimationRegistry(animations.getCatalog());
        loadDefinitions();
    }
    public void observe(Board board, int currentTick) {
        if (board == null) {
            clearModelSnapshots();
            return;
        }
        detectPlantShots(board);
        detectPlantFoodShots(board);
        snapshot(board);
    }

    public void update(float delta) {
        if (delta <= 0f) {
            return;
        }
        updateProjectiles(delta);
        updateImpacts(delta);
    }

    public void render(Batch batch) {
        if (batch == null || (projectiles.isEmpty() && impacts.isEmpty())) {
            return;
        }
        batch.begin();
        for (VisualProjectile projectile : projectiles) {
            drawProjectile(batch, projectile);
        }
        for (ImpactVisual impact : impacts) {
            drawImpact(batch, impact);
        }
        batch.end();
    }

    private void detectPlantShots(Board board) {
        for (Plant plant : board.getAllPlants()) {
            ProjectileVisualType baseType = ProjectileVisualType.fromPlant(plant);
            int previousSerial = previousAttackSerials.getOrDefault(plant, 0);
            if (baseType == null || plant.getVisualAttackSerial() <= previousSerial) {
                continue;
            }
            spawnForPlant(board, plant, baseType);
        }
    }

    private void detectPlantFoodShots(Board board) {
        for (Plant plant : board.getAllPlants()) {
            int previous = previousPlantFoodSerials.getOrDefault(plant, 0);
            if (plant.getVisualPlantFoodSerial() <= previous) {
                continue;
            }
            spawnPlantFoodProjectiles(board, plant);
        }
    }

    private void spawnPlantFoodProjectiles(Board board, Plant plant) {
        String name = normalize(plant.getName());
        List<ZombieSnapshot> zombies = visualTargetSnapshots(board);
        if (zombies.isEmpty()) {
            return;
        }
        float impactSeconds = PlantActionTiming.plantFoodImpactTicks(plant.getName()) / 10f;
        if (name.equals("repeater")) {
            spawnPlantFoodShot(plant, nearestTarget(zombies, plant, (int) Math.round(plant.getY()), false),
                ProjectileVisualType.REPEATER_GIANT, impactSeconds, 0f);
        } else if (name.equals("pea pod")) {
            int heads = countPlantLayers(board, plant);
            ZombieSnapshot target = nearestTarget(zombies, plant, (int) Math.round(plant.getY()), false);
            for (int index = 0; index < heads; index++) {
                spawnPlantFoodShot(plant, target, ProjectileVisualType.PEAPOD_GIANT, impactSeconds, index * 0.07f);
            }
        } else if (name.equals("citron")) {
            spawnPlantFoodShot(plant, nearestTarget(zombies, plant, (int) Math.round(plant.getY()), false),
                ProjectileVisualType.CITRON_PLANT_FOOD, impactSeconds, 0f);
        } else if (name.equals("bowling bulb")) {
            spawnPlantFoodFan(plant, zombies, ProjectileVisualType.BOWLING_PLANT_FOOD, 3, impactSeconds);
        } else if (name.equals("cabbage pult")) {
            spawnPlantFoodFan(plant, zombies, ProjectileVisualType.CABBAGE_PLANT_FOOD, 5, impactSeconds);
        } else if (name.equals("kernel pult")) {
            spawnPlantFoodFan(plant, zombies, ProjectileVisualType.KERNEL_BUTTER, zombies.size(), impactSeconds);
        } else if (name.equals("melon pult")) {
            spawnPlantFoodFan(plant, zombies, ProjectileVisualType.MELON_PLANT_FOOD, 3, impactSeconds);
        } else if (name.equals("winter melon")) {
            spawnPlantFoodFan(plant, zombies, ProjectileVisualType.WINTER_MELON_PLANT_FOOD, 3, impactSeconds);
        } else if (name.equals("pepper pult")) {
            spawnPlantFoodFan(plant, zombies, ProjectileVisualType.PEPPER_PLANT_FOOD, 3, impactSeconds);
        }
    }

    private void spawnPlantFoodFan(
        Plant plant, List<ZombieSnapshot> zombies, ProjectileVisualType type, int count, float impactSeconds
    ) {
        List<ZombieSnapshot> targets = new ArrayList<>(zombies);
        targets.sort(Comparator.comparingDouble(zombie ->
            Math.hypot(zombie.x - plant.getX(), zombie.y - plant.getY())));
        int limit = Math.min(Math.max(0, count), targets.size());
        for (int index = 0; index < limit; index++) {
            spawnPlantFoodShot(plant, targets.get(index), type, impactSeconds, index * 0.05f);
        }
    }

    private void spawnPlantFoodShot(
        Plant plant, ZombieSnapshot target, ProjectileVisualType type, float impactSeconds, float stagger
    ) {
        if (target == null) {
            return;
        }
        VisualDefinition definition = definitions.get(type);
        if (definition == null || definition.projectilePath == null) {
            return;
        }
        float release = Math.min(Math.max(0.05f, impactSeconds * 0.45f + stagger),
            Math.max(0.05f, impactSeconds - 0.08f));
        float duration = Math.max(0.08f, impactSeconds + stagger - release);
        float startX = (float) plant.getX() + type.getSpawnXOffset();
        float startY = (float) plant.getY() + type.getSpawnYOffset();
        projectiles.add(new VisualProjectile(
            type, definition, startX, startY,
            ProjectileTarget.fromZombie(target.zombie, target.x, target.y), duration, release
        ));
    }

    private int countPlantLayers(Board board, Plant source) {
        Position position = new Position((int) Math.round(source.getX()), (int) Math.round(source.getY()));
        Tile tile = board.getTileAt(position);
        if (tile == null) {
            return 1;
        }
        int count = 0;
        for (Plant candidate : tile.getPlants()) {
            if (normalize(candidate.getName()).equals(normalize(source.getName()))) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private void spawnForPlant(Board board, Plant plant, ProjectileVisualType baseType) {
        List<ZombieSnapshot> targets = resolveTargets(plant, visualTargetSnapshots(board));
        if (targets.isEmpty()) {
            return;
        }
        int shots = Math.max(resolveShotCount(board, plant), targets.size());
        int targetIndex = 0;
        for (int shot = 0; shot < shots; shot++) {
            ZombieSnapshot zombieTarget = targets.get(Math.min(targetIndex, targets.size() - 1));
            ProjectileVisualType type = resolveVisualType(board, plant, zombieTarget, baseType);
            ProjectileTarget visualTarget = resolveProjectileTarget(board, plant, zombieTarget, type);
            float delay = releaseDelaySeconds(plant, type) + shot * SHOT_STAGGER_SECONDS;
            spawnProjectile(plant, visualTarget, type, delay);
            if (targets.size() > 1) {
                targetIndex = (targetIndex + 1) % targets.size();
            }
        }
    }

    private List<ZombieSnapshot> resolveTargets(
        Plant plant,
        List<ZombieSnapshot> availableZombies
    ) {
        String name = normalize(plant.getName());
        int lane = (int) Math.round(plant.getY());
        if (name.equals("threepeater")) {
            List<ZombieSnapshot> targets = new ArrayList<>();
            addNearestTarget(targets, availableZombies, plant, lane - 1, false);
            addNearestTarget(targets, availableZombies, plant, lane, false);
            addNearestTarget(targets, availableZombies, plant, lane + 1, false);
            return targets;
        }
        if (name.equals("rotobaga")) {
            List<ZombieSnapshot> targets = new ArrayList<>();
            addNearestTarget(targets, availableZombies, plant, lane - 1, false);
            addNearestTarget(targets, availableZombies, plant, lane - 1, true);
            addNearestTarget(targets, availableZombies, plant, lane + 1, false);
            addNearestTarget(targets, availableZombies, plant, lane + 1, true);
            return targets;
        }
        if (name.equals("starfruit")) {
            return starfruitTargets(plant, availableZombies);
        }
        if (name.equals("bowling bulb")) {
            return bowlingTargets(plant, availableZombies);
        }
        if (name.equals("split pea")) {
            List<ZombieSnapshot> targets = new ArrayList<>();
            addNearestTarget(targets, availableZombies, plant, lane, false);
            addNearestTarget(targets, availableZombies, plant, lane, true);
            return targets;
        }
        ZombieSnapshot target = nearestTarget(availableZombies, plant, lane, false);
        return target == null ? Collections.emptyList() : Collections.singletonList(target);
    }

    private List<ZombieSnapshot> starfruitTargets(
        Plant plant, List<ZombieSnapshot> zombies
    ) {
        ZombieSnapshot[] selected = new ZombieSnapshot[5];
        double[] distances = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
            Double.MAX_VALUE, Double.MAX_VALUE};
        for (ZombieSnapshot zombie : zombies) {
            double dx = zombie.x - plant.getX();
            int dy = (int) Math.round(zombie.y - plant.getY());
            int direction = -1;
            if (dy == 0 && dx >= 0) direction = 0;
            else if (dy < 0 && dx >= 0) direction = 1;
            else if (dy > 0 && dx >= 0) direction = 2;
            else if (dy < 0 && dx < 0) direction = 3;
            else if (dy > 0 && dx < 0) direction = 4;
            if (direction < 0) continue;
            double distance = Math.hypot(dx, dy);
            if (distance < distances[direction]) {
                distances[direction] = distance;
                selected[direction] = zombie;
            }
        }
        List<ZombieSnapshot> result = new ArrayList<>();
        for (ZombieSnapshot target : selected) {
            if (target != null && !result.contains(target)) result.add(target);
        }
        return result;
    }

    private List<ZombieSnapshot> bowlingTargets(
        Plant plant, List<ZombieSnapshot> zombies
    ) {
        int lane = (int) Math.round(plant.getY());
        List<ZombieSnapshot> result = new ArrayList<>();
        ZombieSnapshot first = nearestTarget(zombies, plant, lane, false);
        if (first == null) return result;
        result.add(first);
        int nextLane = lane - 1 >= 1 ? lane - 1 : lane + 1;
        ZombieSnapshot second = nearestToPoint(zombies, nextLane, first.x);
        if (second != null) result.add(second);
        int thirdLane = nextLane < lane ? nextLane + 1 : nextLane - 1;
        if (thirdLane == lane) thirdLane = lane + (nextLane < lane ? 1 : -1);
        ZombieSnapshot third = nearestToPoint(zombies, thirdLane, second == null ? first.x : second.x);
        if (third != null && !result.contains(third)) result.add(third);
        return result;
    }

    private ZombieSnapshot nearestToPoint(
        List<ZombieSnapshot> zombies, int lane, double x
    ) {
        if (lane < 1 || lane > BoardGeometry.ROWS) return null;
        ZombieSnapshot selected = null;
        double best = Double.MAX_VALUE;
        for (ZombieSnapshot zombie : zombies) {
            if (zombie.lane != lane) continue;
            double distance = Math.abs(zombie.x - x);
            if (distance < best) {
                best = distance;
                selected = zombie;
            }
        }
        return selected;
    }

    private void addNearestTarget(
        List<ZombieSnapshot> targets,
        List<ZombieSnapshot> availableZombies,
        Plant plant,
        int lane,
        boolean behind
    ) {
        ZombieSnapshot target = nearestTarget(availableZombies, plant, lane, behind);
        if (target != null && !targets.contains(target)) {
            targets.add(target);
        }
    }

    private ZombieSnapshot nearestTarget(
        List<ZombieSnapshot> availableZombies,
        Plant plant,
        int lane,
        boolean behind
    ) {
        if (lane < 1 || lane > BoardGeometry.ROWS) {
            return null;
        }
        ZombieSnapshot selected = null;
        double distance = Double.MAX_VALUE;
        for (ZombieSnapshot zombie : availableZombies) {
            if (zombie.lane != lane) {
                continue;
            }
            double signed = zombie.x - plant.getX();
            if (behind ? signed >= 0 : signed < -0.01) {
                continue;
            }
            double currentDistance = Math.abs(signed);
            if (currentDistance < distance) {
                selected = zombie;
                distance = currentDistance;
            }
        }
        return selected;
    }

    private ProjectileVisualType resolveVisualType(
        Board board,
        Plant plant,
        ZombieSnapshot target,
        ProjectileVisualType baseType
    ) {
        if (baseType == ProjectileVisualType.KERNEL
            && "attack2".equalsIgnoreCase(plant.getVisualAttackClip())) {
            return ProjectileVisualType.KERNEL_BUTTER;
        }
        if (normalize(plant.getName()).equals("bowling bulb")) {
            String clip = normalize(plant.getVisualAttackClip());
            if (clip.equals("special2")) return ProjectileVisualType.BOWLING_MEDIUM;
            if (clip.equals("special3")) return ProjectileVisualType.BOWLING_LARGE;
            return ProjectileVisualType.BOWLING_SMALL;
        }
        return resolveTorchwoodType(board, plant, target, baseType);
    }

    private float releaseDelaySeconds(Plant plant, ProjectileVisualType type) {
        EntityAnimationProfile profile = entityAnimations.forPlant(plant);
        if (profile == null) {
            return 0f;
        }
        String clip = plant.getVisualAttackClip();
        if (clip == null || !profile.getDefinition().hasClip(clip)) {
            clip = "attack";
        }
        float duration = profile.getDefinition().getClipDuration(clip);
        return Math.max(0f, duration * type.getReleaseFraction());
    }

    private ProjectileVisualType resolveTorchwoodType(
        Board board,
        Plant plant,
        ZombieSnapshot target,
        ProjectileVisualType type
    ) {
        if (!isTorchwoodConvertiblePea(type) || !isGreenPeaPlant(plant)) {
            return type;
        }
        Lane lane = board.getLaneAt(target.lane);
        if (lane == null) {
            return type;
        }
        int start = Math.max(1, (int) Math.floor(plant.getX()) + 1);
        int end = Math.min(board.getWidth(), (int) Math.ceil(target.x) - 1);
        for (int x = start; x <= end; x++) {
            Tile tile = lane.getTileAt(x);
            if (tile != null && hasTorchwood(tile)) {
                return ProjectileVisualType.FIRE;
            }
        }
        return type;
    }

    private ProjectileTarget resolveProjectileTarget(
        Board board,
        Plant plant,
        ZombieSnapshot zombieTarget,
        ProjectileVisualType type
    ) {
        if (type.isLobbed()
            || type == ProjectileVisualType.FUME
            || type == ProjectileVisualType.CACTUS
            || type == ProjectileVisualType.HOMING_THISTLE
            || type == ProjectileVisualType.ROTOBAGA
            || type == ProjectileVisualType.STARFRUIT
            || type == ProjectileVisualType.BOWLING_SMALL
            || type == ProjectileVisualType.BOWLING_MEDIUM
            || type == ProjectileVisualType.BOWLING_LARGE) {
            return ProjectileTarget.fromZombie(
                zombieTarget.zombie,
                zombieTarget.x,
                zombieTarget.y
            );
        }
        Lane lane = board.getLaneAt((int) Math.round(plant.getY()));
        if (lane == null) {
            return ProjectileTarget.fromZombie(
                zombieTarget.zombie,
                zombieTarget.x,
                zombieTarget.y
            );
        }
        int start = Math.max(1, (int) Math.floor(plant.getX()) + 1);
        for (int x = start; x <= board.getWidth(); x++) {
            if (x >= zombieTarget.x) {
                break;
            }
            Tile tile = lane.getTileAt(x);
            if (tile != null && tile.hasDamageableTerrain()) {
                return ProjectileTarget.fixed(x, lane.getLaneId());
            }
        }
        return ProjectileTarget.fromZombie(
            zombieTarget.zombie,
            zombieTarget.x,
            zombieTarget.y
        );
    }

    private boolean hasTorchwood(Tile tile) {
        for (Plant candidate : tile.getPlants()) {
            if (normalize(candidate.getName()).equals("torchwood")) {
                return true;
            }
        }
        return false;
    }

    private boolean isTorchwoodConvertiblePea(ProjectileVisualType type) {
        return type == ProjectileVisualType.NORMAL || type == ProjectileVisualType.MEGA;
    }

    private boolean isGreenPeaPlant(Plant plant) {
        String name = normalize(plant.getName());
        return name.equals("peashooter")
            || name.equals("repeater")
            || name.equals("threepeater")
            || name.equals("split pea")
            || name.equals("pea pod")
            || name.equals("mega gatling pea");
    }

    private int resolveShotCount(Board board, Plant plant) {
        String name = normalize(plant.getName());
        if (name.equals("repeater")) return 2;
        if (name.equals("mega gatling pea")) return 4;
        if (name.equals("rotobaga")) return 4;
        if (name.equals("starfruit")) return 5;
        if (name.equals("split pea")) return 3;
        if (name.equals("bowling bulb")) return 1;
        if (!name.equals("pea pod")) return 1;

        Position position = new Position((int) Math.round(plant.getX()), (int) Math.round(plant.getY()));
        Tile tile = board.getTileAt(position);
        if (tile == null) return 1;
        int count = 0;
        for (Plant layer : tile.getPlants()) {
            if (normalize(layer.getName()).equals("pea pod")) count++;
        }
        return Math.max(1, count);
    }

    private void spawnProjectile(
        Plant plant,
        ProjectileTarget target,
        ProjectileVisualType type,
        float delay
    ) {
        VisualDefinition definition = definitions.get(type);
        if (definition == null || definition.projectilePath == null) {
            return;
        }
        // Spawn coordinates are independent from visual scale so enlarging a projectile
        // never moves its trajectory away from the plant's mouth / launch point.
        float startX = (float) plant.getX() + type.getSpawnXOffset();
        float startY = (float) plant.getY() + type.getSpawnYOffset();
        float distance = Math.max(0.5f, Math.abs((float) target.x - startX));
        float duration = MathUtils.clamp(distance / TILES_PER_SECOND, MIN_TRAVEL_SECONDS, MAX_TRAVEL_SECONDS);
        projectiles.add(new VisualProjectile(type, definition, startX, startY, target, duration, delay));
        AudioManager manager = AudioManager.getActive();
        if (manager != null) {
            manager.playPlantAttack(plant.getName());
        }
    }

    private void updateProjectiles(float delta) {
        Iterator<VisualProjectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            VisualProjectile projectile = iterator.next();
            projectile.elapsed += delta;
            if (projectile.elapsed < projectile.duration) {
                continue;
            }
            Vector2 impactPosition = projectile.currentPosition(geometry);
            impacts.add(new ImpactVisual(projectile.type, projectile.definition, impactPosition));
            AudioManager.playGlobal(AudioCue.PROJECTILE_HIT);
            iterator.remove();
        }
    }

    private void updateImpacts(float delta) {
        impacts.removeIf(impact -> {
            impact.elapsed += delta;
            float duration = impact.definition.impactDuration > 0f
                ? impact.definition.impactDuration : IMPACT_SECONDS;
            return impact.elapsed >= duration;
        });
    }

    private void drawProjectile(Batch batch, VisualProjectile projectile) {
        if (projectile.elapsed < 0f) {
            return;
        }
        Vector2 position = projectile.currentPosition(geometry);
        animations.draw(
            batch,
            projectile.definition.projectilePath,
            projectile.definition.projectileClip,
            projectile.elapsed,
            position.x,
            position.y,
            projectile.type.getScale(),
            true
        );
    }

    private void drawImpact(Batch batch, ImpactVisual impact) {
        if (impact.definition.impactPath == null) {
            return;
        }
        animations.draw(
            batch,
            impact.definition.impactPath,
            impact.definition.impactClip,
            impact.elapsed,
            impact.position.x,
            impact.position.y,
            impact.type.getImpactScale(),
            false
        );
    }

    private List<ZombieSnapshot> visualTargetSnapshots(Board board) {
        List<ZombieSnapshot> result = new ArrayList<>();
        Set<Zombie> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        if (board != null) {
            for (Zombie zombie : board.getAllZombies()) {
                if (zombie != null && zombie.isAlive() && seen.add(zombie)) {
                    result.add(new ZombieSnapshot(zombie));
                }
            }
        }
        // Keep a removed target as a fallback so the projectile that delivered a
        // killing blow can still be visualized, but always prefer the current
        // position of zombies that are still alive.
        for (ZombieSnapshot previous : previousZombies) {
            if (previous.zombie != null && seen.add(previous.zombie)) {
                result.add(previous);
            }
        }
        return result;
    }

    private void snapshot(Board board) {
        previousAttackSerials.clear();
        previousPlantFoodSerials.clear();
        for (Plant plant : board.getAllPlants()) {
            previousAttackSerials.put(plant, plant.getVisualAttackSerial());
            previousPlantFoodSerials.put(plant, plant.getVisualPlantFoodSerial());
        }
        previousZombies.clear();
        Set<Zombie> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie != null && zombie.isAlive() && seen.add(zombie)) {
                previousZombies.add(new ZombieSnapshot(zombie));
            }
        }
        previousZombies.sort(Comparator.comparingInt(snapshot -> snapshot.lane));
    }

    private void clearModelSnapshots() {
        previousAttackSerials.clear();
        previousPlantFoodSerials.clear();
        previousZombies.clear();
    }

    private void loadDefinitions() {
        for (ProjectileVisualType type : ProjectileVisualType.values()) {
            AnimationDefinition projectile = animations.getCatalog().findByName(type.getAnimationName(), null);
            AnimationDefinition impact = animations.getCatalog().findByName(type.getImpactAnimationName(), null);
            if (projectile == null) {
                continue;
            }
            animations.preload(projectile.getPath());
            if (impact != null) {
                animations.preload(impact.getPath());
            }
            String projectileClip = chooseClip(
                projectile, type.getProjectileClip(), "animation", "animation2", "idle"
            );
            String impactClip = impact == null || type.getImpactClip() == null
                ? null : chooseClip(impact, type.getImpactClip(), "animation", "animation2", "idle");
            float impactDuration = impact == null || impactClip == null
                ? 0f : Math.max(0.05f, impact.getClipDuration(impactClip));
            definitions.put(type, new VisualDefinition(
                projectile.getPath(), projectileClip,
                impact == null ? null : impact.getPath(), impactClip, impactDuration
            ));
        }
    }

    private String chooseClip(AnimationDefinition definition, String... preferred) {
        for (String candidate : preferred) {
            if (definition.hasClip(candidate)) {
                return candidate;
            }
        }
        return definition.getClips().isEmpty() ? null : definition.getClips().iterator().next();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
            .replace('-', ' ')
            .replace('_', ' ')
            .replaceAll("\\s+", " ");
    }

    private static final class ZombieSnapshot {
        private final Zombie zombie;
        private final double x;
        private final double y;
        private final int lane;

        private ZombieSnapshot(Zombie zombie) {
            this.zombie = zombie;
            x = zombie.getX();
            y = zombie.getY();
            lane = (int) Math.round(y);
        }

        private Vector2 screenPosition(BoardGeometry geometry) {
            if (zombie != null && zombie.isAlive()) {
                return geometry.entityToScreen(zombie.getX(), zombie.getY());
            }
            return geometry.entityToScreen(x, y);
        }
    }

    private static final class VisualDefinition {
        private final String projectilePath;
        private final String projectileClip;
        private final String impactPath;
        private final String impactClip;
        private final float impactDuration;

        private VisualDefinition(
            String projectilePath,
            String projectileClip,
            String impactPath,
            String impactClip,
            float impactDuration
        ) {
            this.projectilePath = projectilePath;
            this.projectileClip = projectileClip;
            this.impactPath = impactPath;
            this.impactClip = impactClip;
            this.impactDuration = impactDuration;
        }
    }

    private static final class VisualProjectile {
        private final ProjectileVisualType type;
        private final VisualDefinition definition;
        private final float startX;
        private final float startY;
        private final ProjectileTarget target;
        private final float duration;
        private float elapsed;

        private VisualProjectile(
            ProjectileVisualType type,
            VisualDefinition definition,
            float startX,
            float startY,
            ProjectileTarget target,
            float duration,
            float delay
        ) {
            this.type = type;
            this.definition = definition;
            this.startX = startX;
            this.startY = startY;
            this.target = target;
            this.duration = duration;
            elapsed = -Math.max(0f, delay);
        }

        private Vector2 currentPosition(BoardGeometry geometry) {
            Vector2 start = geometry.entityToScreen(startX, startY);
            Vector2 end = target.screenPosition(geometry);
            float progress = duration <= 0f ? 1f : MathUtils.clamp(elapsed / duration, 0f, 1f);
            float x = MathUtils.lerp(start.x, end.x, progress);
            float y = MathUtils.lerp(start.y, end.y, progress);
            if (type.isLobbed()) {
                y += MathUtils.sin(MathUtils.PI * progress) * geometry.getTileHeight() * LOB_ARC_TILES;
            }
            return new Vector2(x, y);
        }
    }

    private static final class ImpactVisual {
        private final ProjectileVisualType type;
        private final VisualDefinition definition;
        private final Vector2 position;
        private float elapsed;

        private ImpactVisual(
            ProjectileVisualType type,
            VisualDefinition definition,
            Vector2 position
        ) {
            this.type = type;
            this.definition = definition;
            this.position = position;
        }
    }
}
