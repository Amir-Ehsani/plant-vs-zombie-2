package game.render.projectile;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.AnimationDefinition;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.plant.Plant;
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
    private final Map<Plant, Integer> previousCooldowns = new IdentityHashMap<>();
    private final List<ZombieSnapshot> previousZombies = new ArrayList<>();
    private final List<VisualProjectile> projectiles = new ArrayList<>();
    private final List<ImpactVisual> impacts = new ArrayList<>();
    private final Map<ProjectileVisualType, VisualDefinition> definitions = new java.util.EnumMap<>(
        ProjectileVisualType.class
    );
    private int lastObservedTick = -1;
    public ProjectileRenderSystem(BoardGeometry geometry, PvzAnimationService animations) {
        if (geometry == null || animations == null || animations.getCatalog() == null) {
            throw new IllegalArgumentException("Projectile renderer requires geometry and animations.");
        }
        this.geometry = geometry;
        this.animations = animations;
        loadDefinitions();
    }
    public void observe(Board board, int currentTick) {
        if (board == null) {
            clearModelSnapshots();
            lastObservedTick = currentTick;
            return;
        }
        if (lastObservedTick < 0) {
            snapshot(board);
            lastObservedTick = currentTick;
            return;
        }
        if (currentTick == lastObservedTick) {
            return;
        }

        detectPlantShots(board);
        snapshot(board);
        lastObservedTick = currentTick;
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
            Integer previousCooldown = previousCooldowns.get(plant);
            if (baseType == null || previousCooldown == null || !didAttack(plant, previousCooldown)) {
                continue;
            }
            spawnForPlant(board, plant, baseType);
        }
    }

    private boolean didAttack(Plant plant, int previousCooldown) {
        int current = plant.getCooldownRemaining();
        int interval = Math.max(0, plant.getAttackIntervalTicks());
        if (interval <= 0 || current <= 0) {
            return false;
        }
        return current > previousCooldown || current == interval && previousCooldown != interval;
    }

    private void spawnForPlant(Board board, Plant plant, ProjectileVisualType baseType) {
        List<ZombieSnapshot> targets = resolveTargets(plant);
        if (targets.isEmpty()) {
            return;
        }
        int shots = resolveShotCount(board, plant);
        int targetIndex = 0;
        for (int shot = 0; shot < shots; shot++) {
            ZombieSnapshot zombieTarget = targets.get(Math.min(targetIndex, targets.size() - 1));
            ProjectileVisualType type = resolveTorchwoodType(board, plant, zombieTarget, baseType);
            ProjectileTarget visualTarget = resolveProjectileTarget(board, plant, zombieTarget, type);
            spawnProjectile(plant, visualTarget, type, shot * SHOT_STAGGER_SECONDS);
            if (targets.size() > 1) {
                targetIndex = (targetIndex + 1) % targets.size();
            }
        }
    }

    private List<ZombieSnapshot> resolveTargets(Plant plant) {
        String name = normalize(plant.getName());
        int lane = (int) Math.round(plant.getY());
        if (name.equals("threepeater") || name.equals("rotobaga")) {
            List<ZombieSnapshot> targets = new ArrayList<>();
            addNearestTarget(targets, plant, lane - 1, false);
            addNearestTarget(targets, plant, lane, false);
            addNearestTarget(targets, plant, lane + 1, false);
            return targets;
        }
        if (name.equals("split pea")) {
            List<ZombieSnapshot> targets = new ArrayList<>();
            addNearestTarget(targets, plant, lane, false);
            addNearestTarget(targets, plant, lane, true);
            return targets;
        }
        ZombieSnapshot target = nearestTarget(plant, lane, false);
        return target == null ? Collections.emptyList() : Collections.singletonList(target);
    }

    private void addNearestTarget(List<ZombieSnapshot> targets, Plant plant, int lane, boolean behind) {
        ZombieSnapshot target = nearestTarget(plant, lane, behind);
        if (target != null && !targets.contains(target)) {
            targets.add(target);
        }
    }

    private ZombieSnapshot nearestTarget(Plant plant, int lane, boolean behind) {
        if (lane < 1 || lane > BoardGeometry.ROWS) {
            return null;
        }
        ZombieSnapshot selected = null;
        double distance = Double.MAX_VALUE;
        for (ZombieSnapshot zombie : previousZombies) {
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

    private ProjectileVisualType resolveTorchwoodType(
        Board board,
        Plant plant,
        ZombieSnapshot target,
        ProjectileVisualType type
    ) {
        if (type != ProjectileVisualType.NORMAL || !isPeaPlant(plant)) {
            return type;
        }
        Lane lane = board.getLaneAt((int) Math.round(plant.getY()));
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
        if (type.isLobbed() || type == ProjectileVisualType.FUME || type == ProjectileVisualType.CACTUS) {
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
        int end = Math.min(board.getWidth(), (int) Math.ceil(zombieTarget.x));
        for (int x = start; x <= end; x++) {
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

    private boolean isPeaPlant(Plant plant) {
        String name = normalize(plant.getName());
        return name.contains("pea") || name.equals("repeater") || name.equals("threepeater");
    }

    private int resolveShotCount(Board board, Plant plant) {
        String name = normalize(plant.getName());
        if (name.equals("repeater")) return 2;
        if (name.equals("mega gatling pea")) return 4;
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
        float startX = (float) plant.getX() + (type.isLobbed() ? 0.15f : 0.38f);
        float startY = (float) plant.getY() - (type.isLobbed() ? 0.34f : 0.12f);
        float distance = Math.max(0.5f, Math.abs((float) target.x - startX));
        float duration = MathUtils.clamp(distance / TILES_PER_SECOND, MIN_TRAVEL_SECONDS, MAX_TRAVEL_SECONDS);
        projectiles.add(new VisualProjectile(type, definition, startX, startY, target, duration, delay));
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
            iterator.remove();
        }
    }

    private void updateImpacts(float delta) {
        impacts.removeIf(impact -> {
            impact.elapsed += delta;
            return impact.elapsed >= IMPACT_SECONDS;
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

    private void snapshot(Board board) {
        previousCooldowns.clear();
        for (Plant plant : board.getAllPlants()) {
            previousCooldowns.put(plant, plant.getCooldownRemaining());
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
        previousCooldowns.clear();
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
            definitions.put(type, new VisualDefinition(
                projectile.getPath(),
                chooseClip(projectile, "idle", "special", "animation", "animation2"),
                impact == null ? null : impact.getPath(),
                impact == null ? null : chooseClip(impact, "animation", "animation2", "idle")
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

        private VisualDefinition(
            String projectilePath,
            String projectileClip,
            String impactPath,
            String impactClip
        ) {
            this.projectilePath = projectilePath;
            this.projectileClip = projectileClip;
            this.impactPath = impactPath;
            this.impactClip = impactClip;
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
