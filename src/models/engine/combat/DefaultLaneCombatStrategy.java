package models.engine.combat;

import models.core.plant.Plant;
import models.core.projectile.Damage;
import models.core.zombie.Armor;
import models.core.zombie.Zombie;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.engine.events.GameEvent;
import models.entities.LawnMower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class DefaultLaneCombatStrategy implements LaneCombatStrategy {
    private static final double MELEE_RANGE = 1.0;
    private static final int TICKS_PER_SECOND = 10;
    private static final int DEFAULT_CHILL_TICKS = 3 * TICKS_PER_SECOND;
    private static final int DEFAULT_FREEZE_TICKS = 5 * TICKS_PER_SECOND;
    private static final int DEFAULT_POISON_TICKS = 5 * TICKS_PER_SECOND;
    private static final int DEFAULT_BUTTER_TICKS = 3 * TICKS_PER_SECOND;
    private static final int DEFAULT_POTATO_ARM_TICKS = 14 * TICKS_PER_SECOND;
    private static final int DEFAULT_PRIMAL_POTATO_ARM_TICKS = 5 * TICKS_PER_SECOND;
    private static final int DEFAULT_SHROOM_LIFESPAN_TICKS = 60 * TICKS_PER_SECOND;
    private static final int DEFAULT_CHOMPER_DIGEST_TICKS = 40 * TICKS_PER_SECOND;

    private static final class ZombieRuntimeState {
        private int frozenTicks;
        private int chilledTicks;
        private int poisonTicks;
        private int poisonDamage;
        private String poisonSourcePlantName;
        private String poisonSourcePlantCategory;
        private int butterTicks;
        private boolean hypnotized;
        private int pendingLaneShift;
        private int ageTicks;
        private boolean externalAbilityExecuted;
    }

    private static final class PlantRuntimeState {
        private int ageTicks;
        private int digestTicks;
        private int shotCycle;
        private int crushCount;
        private boolean hasAttacked;
        private boolean deathEffectHandled;
    }

    private final Board board;
    private final Random random;
    private final Map<Zombie, ZombieRuntimeState> zombieStates;
    private final Map<Plant, PlantRuntimeState> plantStates;
    private final Map<String, Integer> familyBoostTicks;
    private final Set<Zombie> processedZombiesThisBoardTick;

    public DefaultLaneCombatStrategy() {
        this(null, new Random());
    }

    public DefaultLaneCombatStrategy(Board board) {
        this(board, new Random());
    }

    public DefaultLaneCombatStrategy(Board board, Random random) {
        this.board = board;
        this.random = random == null ? new Random() : random;
        this.zombieStates = new IdentityHashMap<>();
        this.plantStates = new IdentityHashMap<>();
        this.familyBoostTicks = new LinkedHashMap<>();
        this.processedZombiesThisBoardTick = Collections.newSetFromMap(new IdentityHashMap<>());
    }


    public void beginBoardTick() {
        processedZombiesThisBoardTick.clear();
        for (String category : new ArrayList<>(familyBoostTicks.keySet())) {
            int remaining = familyBoostTicks.get(category) - 1;
            if (remaining <= 0) {
                familyBoostTicks.remove(category);
            } else {
                familyBoostTicks.put(category, remaining);
            }
        }
        zombieStates.keySet().removeIf(zombie -> zombie == null || !zombie.isAlive());
        plantStates.keySet().removeIf(plant -> plant == null || !plant.isAlive());
    }

    public void applyFreeze(Zombie zombie, int ticks) {
        if (zombie == null || !zombie.isAlive() || ticks <= 0) {
            return;
        }
        ZombieRuntimeState state = stateOf(zombie);
        state.frozenTicks = Math.max(state.frozenTicks, ticks);
        zombie.setCurrentSpeed(0);
    }

    public void applyChill(Zombie zombie, int ticks) {
        if (zombie == null || !zombie.isAlive() || ticks <= 0) {
            return;
        }
        ZombieRuntimeState state = stateOf(zombie);
        state.chilledTicks = Math.max(state.chilledTicks, ticks);
    }

    public void applyPoison(Zombie zombie, int damagePerTick, int ticks) {
        applyPoison(zombie, damagePerTick, ticks, null);
    }

    private void applyPoison(Zombie zombie, int damagePerTick, int ticks, Plant source) {
        if (zombie == null || !zombie.isAlive() || damagePerTick <= 0 || ticks <= 0) {
            return;
        }

        ZombieRuntimeState state = stateOf(zombie);
        state.poisonDamage = Math.max(state.poisonDamage, damagePerTick);
        state.poisonTicks = Math.max(state.poisonTicks, ticks);

        if (source != null) {
            state.poisonSourcePlantName = source.getName();
            state.poisonSourcePlantCategory = source.getType() == null ? "" : source.getType().getCategory();
        }
    }

    public void applyButterStun(Zombie zombie, int ticks) {
        if (zombie == null || !zombie.isAlive() || ticks <= 0) {
            return;
        }
        ZombieRuntimeState state = stateOf(zombie);
        state.butterTicks = Math.max(state.butterTicks, ticks);
        zombie.setCurrentSpeed(0);
    }

    public void hypnotize(Zombie zombie) {
        if (zombie == null || !zombie.isAlive()) {
            return;
        }
        stateOf(zombie).hypnotized = true;
    }

    public boolean isHypnotized(Zombie zombie) {
        ZombieRuntimeState state = zombieStates.get(zombie);
        return state != null && state.hypnotized;
    }

    public List<String> getActiveEffects(Zombie zombie) {
        ZombieRuntimeState state = zombieStates.get(zombie);
        if (state == null || zombie == null || !zombie.isAlive()) {
            return Collections.emptyList();
        }
        List<String> effects = new ArrayList<>();
        if (state.frozenTicks > 0) {
            effects.add("frozen(" + state.frozenTicks + " ticks)");
        }
        if (state.chilledTicks > 0) {
            effects.add("chilled(" + state.chilledTicks + " ticks)");
        }
        if (state.poisonTicks > 0) {
            effects.add("poisoned(" + state.poisonTicks + " ticks, "
                    + state.poisonDamage + " damage/tick)");
        }
        if (state.butterTicks > 0) {
            effects.add("buttered(" + state.butterTicks + " ticks)");
        }
        if (state.hypnotized) {
            effects.add("hypnotized");
        }
        return Collections.unmodifiableList(effects);
    }

    public void activateFamilyBoost(String category, int ticks) {
        String normalized = normalizeText(category);
        if (normalized.isEmpty() || ticks <= 0) {
            return;
        }
        familyBoostTicks.put(normalized,
                Math.max(familyBoostTicks.getOrDefault(normalized, 0), ticks));
    }

    public boolean isFamilyBoosted(String category) {
        return familyBoostTicks.getOrDefault(normalizeText(category), 0) > 0;
    }

    @Override
    public LaneTickResult updateLane(Lane lane) {
        if (lane == null) {
            throw new IllegalArgumentException("Lane cannot be null.");
        }

        List<Zombie> livingAtStart = collectLivingZombies(lane);
        Map<Plant, Position> plantPositions = capturePlantPositions(lane);
        Set<Plant> consumedPlants = Collections.newSetFromMap(new IdentityHashMap<>());
        List<GameEvent> events = new ArrayList<>();

        updatePlants(lane, consumedPlants);
        updateZombies(lane, livingAtStart);

        boolean mowerWasReady = lane.getLawnMower().isReady();
        List<Zombie> mowerKilled = handleLaneEnd(lane, livingAtStart);
        boolean mowerTriggered = mowerWasReady && lane.getLawnMower().isTriggered();
        boolean brainEaten = hasLivingZombieAtLaneEnd(livingAtStart);

        int plantsDestroyed = removeDeadPlants(
                lane,
                consumedPlants,
                plantPositions,
                events
        );

        Set<Zombie> mowerKilledSet = Collections.newSetFromMap(new IdentityHashMap<>());
        mowerKilledSet.addAll(mowerKilled);

        int zombiesKilled = 0;
        for (Zombie zombie : livingAtStart) {
            if (!zombie.isAlive()) {
                zombiesKilled++;
                events.add(GameEvent.zombieKilled(zombie, mowerKilledSet.contains(zombie)));
            }
        }

        if (mowerTriggered) {
            List<String> killedNames = new ArrayList<>();
            for (Zombie zombie : mowerKilled) {
                killedNames.add(zombie.getName());
            }
            events.add(GameEvent.lawnMowerTriggered(lane.getLaneId(), killedNames));
        }

        redistributeLivingZombies(lane, livingAtStart);

        return new LaneTickResult(
                zombiesKilled,
                plantsDestroyed,
                mowerTriggered,
                brainEaten,
                events
        );
    }

    public void handleExternalPlantDeath(Plant plant, Position position, List<GameEvent> events) {
        if (plant == null || position == null) {
            return;
        }
        triggerPlantDeathEffect(plant, position, events == null ? new ArrayList<>() : events);
    }

    private Map<Plant, Position> capturePlantPositions(Lane lane) {
        Map<Plant, Position> positions = new IdentityHashMap<>();
        for (Tile tile : lane.getTiles()) {
            for (Plant plant : tile.getPlants()) {
                if (plant != null) {
                    positions.put(plant, tile.getPosition());
                }
            }
        }
        return positions;
    }

    private void updatePlants(Lane lane, Set<Plant> consumedPlants) {
        for (Tile tile : lane.getTiles()) {
            for (Plant plant : new ArrayList<>(tile.getPlants())) {
                if (plant == null || !plant.isAlive()) {
                    continue;
                }

                PlantRuntimeState state = plantStateOf(plant);
                state.ageTicks++;
                plant.tickCooldown();

                if (isLifespanExpired(plant, state)) {
                    plant.takeDamage(new Damage(plant.getMaxHp(), "lifespan"));
                    continue;
                }
                if (tile.isFrozenTerrain()) {
                    continue;
                }
                if (state.digestTicks > 0) {
                    state.digestTicks--;
                    continue;
                }

                if (handlePassiveOrSpecialPlant(lane, tile, plant, state, consumedPlants)) {
                    continue;
                }
                if (plant.getCooldownRemaining() > 0 || !canPlantAttack(plant)) {
                    continue;
                }

                performPlantAttack(lane, tile, plant, state);
            }
        }
    }


    private boolean handlePassiveOrSpecialPlant(
            Lane lane,
            Tile tile,
            Plant plant,
            PlantRuntimeState state,
            Set<Plant> consumedPlants
    ) {
        String name = normalizeText(plant.getName());

        if (name.equals("potato mine") || name.equals("primal potato mine")) {
            int armTicks = plant.getArmTimeTicks() > 0
                    ? plant.getArmTimeTicks()
                    : name.equals("primal potato mine")
                    ? DEFAULT_PRIMAL_POTATO_ARM_TICKS
                    : DEFAULT_POTATO_ARM_TICKS;
            if (state.ageTicks < armTicks) {
                return true;
            }
            Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
            if (target != null && Math.abs(target.getX() - plant.getX()) <= 0.75) {
                int damage = effectiveDamage(plant,
                        name.equals("primal potato mine") ? 2400 : 1800);
                damageArea(new Position((int) Math.round(plant.getX()), lane.getLaneId()),
                        1, 1, damage, "mine", plant, null);
                consumePlant(tile, plant, consumedPlants);
            }
            return true;
        }

        if (name.equals("squash")) {
            Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
            if (target != null && Math.abs(target.getX() - plant.getX()) <= 1.25) {
                int limit = plant.canCrushTwice() ? 2 : 1;
                for (Zombie zombie : closestTargets(plant, collectCandidateZombies(plant, lane), limit, true)) {
                    zombie.recordDamageSource(
                            plant.getName(),
                            plant.getType() == null ? "" : plant.getType().getCategory(),
                            "crush"
                    );

                    zombie.kill();
                    state.crushCount++;
                }
                consumePlant(tile, plant, consumedPlants);
            }
            return true;
        }

        if (name.equals("tangle kelp")) {
            List<Zombie> targets = closestTargets(
                    plant,
                    collectCandidateZombies(plant, lane),
                    Math.max(1, plant.getTargetCount()),
                    true
            );
            int killed = 0;
            for (Zombie target : targets) {
                if (Math.abs(target.getX() - plant.getX()) <= 0.75) {
                    target.recordDamageSource(
                            plant.getName(),
                            plant.getType() == null ? "" : plant.getType().getCategory(),
                            "drag"
                    );

                    target.kill();
                    killed++;
                }
            }
            if (killed > 0) {
                consumePlant(tile, plant, consumedPlants);
            }
            return true;
        }

        if (name.equals("iceberg lettuce")) {
            Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
            if (target != null && Math.abs(target.getX() - plant.getX()) <= 0.9) {
                applyFreeze(target, Math.max(DEFAULT_FREEZE_TICKS, plant.getFreezeDurationTicks()));
                consumePlant(tile, plant, consumedPlants);
            }
            return true;
        }

        if (name.equals("magnet shroom")) {
            if (plant.getCooldownRemaining() == 0) {
                Zombie target = findArmoredTarget(plant, lane);
                if (target != null && target.getArmor() != null) {
                    target.getArmor().reduceDamage(Integer.MAX_VALUE);
                    plant.attack();
                }
            }
            return true;
        }

        if (name.equals("caulipower")) {
            if (plant.getCooldownRemaining() == 0) {
                Zombie target = strongestTarget(collectCandidateZombies(plant, lane), false);
                if (target != null) {
                    hypnotize(target);
                    plant.attack();
                }
            }
            return true;
        }

        if (name.equals("electric blueberry")) {
            if (plant.getCooldownRemaining() == 0) {
                Zombie target = strongestTarget(collectCandidateZombies(plant, lane), false);
                if (target != null) {
                    target.recordDamageSource(
                            plant.getName(),
                            plant.getType() == null ? "" : plant.getType().getCategory(),
                            "electric"
                    );

                    target.kill();
                    plant.attack();
                }
            }
            return true;
        }

        if (name.equals("chomper")) {
            if (plant.getCooldownRemaining() == 0) {
                Zombie target = nearestZombie(collectCandidateZombies(plant, lane), plant, true);
                if (target != null && Math.abs(target.getX() - plant.getX()) <= MELEE_RANGE) {
                    target.recordDamageSource(
                            plant.getName(),
                            plant.getType() == null ? "" : plant.getType().getCategory(),
                            "chomp"
                    );

                    target.kill();
                    state.digestTicks = plant.getDigestTimeTicks() > 0
                            ? plant.getDigestTimeTicks()
                            : DEFAULT_CHOMPER_DIGEST_TICKS;
                    plant.attack();
                }
            }
            return true;
        }

        if (name.equals("phat beet")) {
            if (plant.getCooldownRemaining() == 0) {
                int damage = effectiveDamage(plant, 15);
                damageArea(new Position((int) Math.round(plant.getX()), lane.getLaneId()),
                        1, 1, damage, "sonic", plant, null);
                plant.attack();
            }
            return true;
        }

        String category = normalizeCategory(plant);
        return category.equals("sun producer")
                || category.equals("wall nut")
                || category.equals("modifier")
                || name.equals("lily pad")
                || name.equals("torchwood");
    }

    private void performPlantAttack(
            Lane lane,
            Tile tile,
            Plant plant,
            PlantRuntimeState state
    ) {
        String name = normalizeText(plant.getName());

        if (name.equals("threepeater")) {
            if (attackLanes(plant, lane.getLaneId() - 1, lane.getLaneId(), lane.getLaneId() + 1) > 0) {
                plant.attack();
                state.hasAttacked = true;
            }
            return;
        }
        if (name.equals("rotobaga")) {
            if (attackLanes(plant, lane.getLaneId() - 1, lane.getLaneId(), lane.getLaneId() + 1) > 0) {
                plant.attack();
                state.hasAttacked = true;
            }
            return;
        }
        if (name.equals("starfruit")) {
            List<Zombie> targets = closestTargets(plant, allLivingEnemyZombies(), 5, false);
            int damage = effectiveDamage(plant, 20);
            for (Zombie target : targets) {
                dealPlantDamage(plant, target, damage, "star", false);
            }
            if (!targets.isEmpty()) {
                plant.attack();
            }
            return;
        }
        if (name.equals("split pea")) {
            List<Zombie> candidates = collectCandidateZombies(plant, lane);
            Zombie front = nearestZombie(candidates, plant, false);
            Zombie back = nearestZombieBehind(candidates, plant);
            int damage = effectiveDamage(plant, 20);
            if (front != null) {
                dealPlantDamage(plant, front, damage, "pea", true);
            }
            if (back != null && back != front) {
                dealPlantDamage(plant, back, damage * 2, "pea", true);
            }
            if (front != null || back != null) {
                plant.attack();
                state.hasAttacked = true;
            }
            return;
        }

        List<Zombie> candidates = collectCandidateZombies(plant, lane);
        Zombie primaryTarget = selectPrimaryTarget(plant, candidates);
        if (primaryTarget == null) {
            return;
        }
        if (name.equals("citron") && !state.hasAttacked) {
            int chargeTicks = Math.max(1, plant.getChargeTimeTicks());
            if (state.ageTicks < chargeTicks) {
                return;
            }
        }

        int baseDamage = resolveBaseDamage(plant, state, tile);
        int damage = effectiveDamage(plant, baseDamage);
        boolean fireDamage = isFirePlant(plant)
                || hasTorchwoodBetween(plant, primaryTarget, lane);

        Tile blockingTerrain = findBlockingTerrain(lane, plant, primaryTarget);
        if (blockingTerrain != null) {
            blockingTerrain.damageTerrain(damage, fireDamage);
            plant.attack();
            state.hasAttacked = true;
            return;
        }
        if (hasTorchwoodBetween(plant, primaryTarget, lane)) {
            damage *= 2;
        }

        int shotCount = resolveShotCount(plant, tile);
        for (int shot = 0; shot < shotCount && primaryTarget.isAlive(); shot++) {
            dealPlantDamage(plant, primaryTarget, damage,
                    fireDamage ? "fire" : resolveDamageType(plant), fireDamage);
        }

        applyOnHitEffects(plant, primaryTarget);
        applySplashDamage(plant, primaryTarget, damage, fireDamage);
        applyExtraTargets(plant, primaryTarget, candidates, damage, fireDamage);

        if (name.equals("pepper pult") || plant.getWarmthRadius() > 0) {
            int radius = Math.max(1, plant.getWarmthRadius());
            if (board != null) {
                board.meltTerrainArea(
                        new Position((int) Math.round(plant.getX()), (int) Math.round(plant.getY())),
                        radius
                );
            }
        }

        state.shotCycle++;
        plant.attack();
        state.hasAttacked = true;
    }

    private int attackLanes(Plant plant, int... laneNumbers) {
        if (board == null) {
            return 0;
        }
        int hits = 0;
        int damage = effectiveDamage(plant, 20);
        for (int laneNumber : laneNumbers) {
            Lane targetLane = board.getLaneAt(laneNumber);
            if (targetLane == null) {
                continue;
            }
            Zombie target = selectPrimaryTarget(plant, new ArrayList<>(targetLane.getAllZombies()));
            if (target != null) {
                dealPlantDamage(plant, target, damage, resolveDamageType(plant), false);
                applyOnHitEffects(plant, target);
                hits++;
            }
        }
        return hits;
    }

    private int resolveBaseDamage(Plant plant, PlantRuntimeState state, Tile tile) {
        String name = normalizeText(plant.getName());
        if (name.equals("citron")) {
            return 400;
        }
        if (name.equals("bowling bulb")) {
            int cycle = state.shotCycle % 3;
            return cycle == 0 ? 40 : cycle == 1 ? 80 : 120;
        }
        if (name.equals("cabbage pult") || name.equals("kernel pult")) {
            return 40;
        }
        if (name.equals("melon pult") || name.equals("winter melon")) {
            return 80;
        }
        if (name.equals("pepper pult")) {
            return 50;
        }
        if (name.equals("wasabi whip")) {
            return 30;
        }
        if (name.equals("kiwibeast")) {
            double healthRatio = plant.getMaxHp() == 0 ? 1.0
                    : plant.getHp() / (double) plant.getMaxHp();
            int size = healthRatio <= 0.33 ? Math.max(3, plant.getMaxSize())
                    : healthRatio <= 0.66 ? Math.max(2, plant.getMaxSize())
                    : 1;
            return 15 * size;
        }
        if (name.equals("bonk choy")) {
            return 15;
        }
        if (name.equals("goo peashooter") || name.equals("snow pea")
                || name.equals("fire peashooter") || name.equals("mega gatling pea")
                || name.equals("repeater") || name.equals("pea pod")) {
            return 20;
        }
        return Math.max(0, plant.getAttackDamage());
    }

    private int resolveShotCount(Plant plant, Tile tile) {
        String name = normalizeText(plant.getName());
        if (name.equals("repeater") || name.equals("bonk choy")) {
            return 2;
        }
        if (name.equals("mega gatling pea")) {
            return 4;
        }
        if (name.equals("pea pod") && tile != null) {
            int count = 0;
            for (Plant layer : tile.getPlants()) {
                if (normalizeText(layer.getName()).equals("pea pod")) {
                    count++;
                }
            }
            return Math.max(1, count);
        }
        return 1;
    }

    private void applyOnHitEffects(Plant plant, Zombie target) {
        if (plant == null || target == null || !target.isAlive()) {
            return;
        }
        String name = normalizeText(plant.getName());

        if (name.equals("snow pea") || name.equals("winter melon")) {
            applyChill(target, Math.max(DEFAULT_CHILL_TICKS, plant.getChillDurationTicks()));
        }
        if (name.equals("goo peashooter") || plant.getDamagePerTick() > 0) {
            int damage = Math.max(5, plant.getDamagePerTick());
            applyPoison(target, damage, DEFAULT_POISON_TICKS, plant);
        }
        if (name.equals("kernel pult")) {
            int chance = Math.min(100, 25 + plant.getButterChancePercent());
            if (random.nextInt(100) < chance) {
                applyButterStun(target, DEFAULT_BUTTER_TICKS);
            }
        }
    }

    private void applySplashDamage(
            Plant plant,
            Zombie primary,
            int primaryDamage,
            boolean fireDamage
    ) {
        int splash = plant.getAreaDamage();
        String name = normalizeText(plant.getName());
        if (name.equals("melon pult") || name.equals("winter melon")) {
            splash = Math.max(splash, primaryDamage / 2);
        }
        if (splash <= 0 || board == null) {
            return;
        }
        Position center = new Position(
                Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(primary.getX()))),
                Math.max(1, Math.min(board.getHeight(), (int) Math.round(primary.getY())))
        );
        damageArea(center, 1, 1, splash,
                fireDamage ? "fire splash" : "splash", plant, primary);
    }

    private void applyExtraTargets(
            Plant plant,
            Zombie primary,
            List<Zombie> candidates,
            int damage,
            boolean fireDamage
    ) {
        int additional = Math.max(0, plant.getTargetCount() - 1)
                + Math.max(0, plant.getPierceCount())
                + Math.max(0, plant.getBounces());
        String category = normalizeCategory(plant);
        if (category.equals("strike through") && additional == 0) {
            additional = 2;
        }
        if (additional <= 0) {
            return;
        }

        List<Zombie> ordered = new ArrayList<>();
        for (Zombie zombie : candidates) {
            if (zombie != null && zombie != primary && zombie.isAlive()
                    && !isHypnotized(zombie)) {
                ordered.add(zombie);
            }
        }
        ordered.sort(Comparator.comparingDouble(zombie -> Math.abs(zombie.getX() - primary.getX())));

        for (int index = 0; index < ordered.size() && index < additional; index++) {
            Zombie zombie = ordered.get(index);
            int appliedDamage = index < plant.getBounces()
                    ? Math.max(1, damage / 2)
                    : damage;
            dealPlantDamage(plant, zombie, appliedDamage,
                    fireDamage ? "fire" : "multi target", fireDamage);
            applyOnHitEffects(plant, zombie);
        }
    }

    private void damageArea(
            Position center,
            int xRadius,
            int yRadius,
            int damage,
            String damageType,
            Plant source,
            Zombie excluded
    ) {
        if (board == null || center == null || damage <= 0) {
            return;
        }
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || zombie == excluded || !zombie.isAlive() || isHypnotized(zombie)) {
                continue;
            }
            int x = (int) Math.ceil(zombie.getX());
            int y = (int) Math.round(zombie.getY());
            if (Math.abs(x - center.getX()) <= xRadius
                    && Math.abs(y - center.getY()) <= yRadius) {
                dealPlantDamage(source, zombie, damage, damageType, damageType.contains("fire"));
            }
        }
    }

    private void dealPlantDamage(
            Plant source,
            Zombie target,
            int damage,
            String damageType,
            boolean fireDamage
    ) {
        if (target == null || !target.isAlive() || damage <= 0 || isHypnotized(target)) {
            return;
        }

        int adjusted = damage;

        if (source != null && isFamilyBoosted(source.getType().getCategory())) {
            adjusted *= 2;
        }

        if (source != null) {
            target.recordDamageSource(
                    source.getName(),
                    source.getType() == null ? "" : source.getType().getCategory(),
                    damageType
            );
        }

        target.takeDamage(new Damage(adjusted, damageType));

        if (fireDamage && board != null) {
            Position targetPosition = new Position(
                    Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(target.getX()))),
                    Math.max(1, Math.min(board.getHeight(), (int) Math.round(target.getY())))
            );

            board.damageTerrain(targetPosition, adjusted, true);
        }
    }

    private void updateZombies(Lane lane, List<Zombie> zombies) {
        List<Zombie> ordered = new ArrayList<>(zombies);
        ordered.sort(Comparator.comparingDouble(Zombie::getX));

        for (Zombie zombie : ordered) {
            if (!zombie.isAlive() || !processedZombiesThisBoardTick.add(zombie)) {
                continue;
            }

            ZombieRuntimeState state = stateOf(zombie);
            state.ageTicks++;
            if (state.poisonTicks > 0) {
                zombie.recordDamageSource(
                        state.poisonSourcePlantName,
                        state.poisonSourcePlantCategory,
                        "poison"
                );

                zombie.takeDamage(new Damage(state.poisonDamage, "poison"));
                state.poisonTicks--;

                if (!zombie.isAlive()) {
                    continue;
                }
            }

            Tile currentTile = tileForZombie(lane, zombie);
            if (currentTile != null && currentTile.isFrozenTerrain()) {
                zombie.setCurrentSpeed(0);
                continue;
            }

            if (state.frozenTicks > 0) {
                state.frozenTicks--;
                zombie.setCurrentSpeed(0);
                continue;
            }
            if (state.butterTicks > 0) {
                state.butterTicks--;
                zombie.setCurrentSpeed(0);
                continue;
            }

            applyZombieAbilityState(zombie, state);
            double speedMultiplier = state.chilledTicks > 0 ? 0.5 : 1.0;
            if (state.chilledTicks > 0) {
                state.chilledTicks--;
            }
            zombie.setCurrentSpeed(resolveAbilitySpeed(zombie) * speedMultiplier);

            if (!isBuiltInAbilityHandledByEngine(zombie) && !state.externalAbilityExecuted) {
                zombie.executeAbility();
                state.externalAbilityExecuted = true;
            }
            if (!zombie.isAlive()) {
                continue;
            }

            if (state.hypnotized) {
                updateHypnotizedZombie(lane, zombie);
                continue;
            }

            maybeAttractToSweetPotato(lane, zombie, state);
            currentTile = tileForZombie(lane, zombie);
            Plant plant = currentTile == null ? null : currentTile.getCurrentPlant();

            if (plant != null && plant.isAlive() && !fliesOverPlant(zombie, plant)) {
                zombie.attack(plant);
                if (plant.getReflectDamage() > 0 && zombie.isAlive()) {
                    zombie.recordDamageSource(
                            plant.getName(),
                            plant.getType() == null ? "" : plant.getType().getCategory(),
                            "reflected"
                    );

                    zombie.takeDamage(new Damage(plant.getReflectDamage(), "reflected"));
                }

                String plantName = normalizeText(plant.getName());
                if (plantName.equals("hypno shroom") && zombie.isAlive()) {
                    hypnotize(zombie);
                    plant.takeDamage(new Damage(plant.getMaxHp(), "consumed"));
                } else if (plantName.equals("garlic") && zombie.isAlive()) {
                    scheduleGarlicLaneShift(lane, zombie, state);
                }
            } else {
                zombie.move();
            }
        }
    }

    private void updateHypnotizedZombie(Lane lane, Zombie zombie) {
        Zombie target = null;
        double minimum = Double.MAX_VALUE;
        for (Zombie candidate : lane.getAllZombies()) {
            if (candidate == zombie || !candidate.isAlive() || isHypnotized(candidate)) {
                continue;
            }
            double distance = Math.abs(candidate.getX() - zombie.getX());
            if (distance < minimum) {
                minimum = distance;
                target = candidate;
            }
        }
        if (target != null && minimum <= MELEE_RANGE) {
            target.takeDamage(new Damage(zombie.getType().getDamagePerTick(), "hypnotized bite"));
        } else {
            zombie.moveBy(zombie.getCurrentSpeed(), 0);
        }
    }

    private void applyZombieAbilityState(Zombie zombie, ZombieRuntimeState state) {
        String name = normalizeText(zombie.getName());
        if (name.equals("king") && state.ageTicks % (5 * TICKS_PER_SECOND) == 0) {
            zombie.heal(50);
        }
    }

    private double resolveAbilitySpeed(Zombie zombie) {
        String name = normalizeText(zombie.getName());
        double base = zombie.getType().getSpeed();
        if (name.equals("gargantuar")) {
            return base * 0.75;
        }
        if (name.equals("imp")) {
            return base * 1.25;
        }
        if ((name.equals("news paper") || name.equals("newspaper"))
                && !zombie.hasArmor()) {
            return base * 1.5;
        }
        return base;
    }

    private boolean isBuiltInAbilityHandledByEngine(Zombie zombie) {
        String name = normalizeText(zombie.getName());
        return name.equals("gargantuar")
                || name.equals("imp")
                || name.equals("king")
                || name.equals("news paper")
                || name.equals("newspaper");
    }

    private void maybeAttractToSweetPotato(
            Lane currentLane,
            Zombie zombie,
            ZombieRuntimeState state
    ) {
        if (board == null || state.pendingLaneShift != 0) {
            return;
        }
        for (int delta : new int[]{-1, 1}) {
            Lane adjacent = board.getLaneAt(currentLane.getLaneId() + delta);
            if (adjacent == null) {
                continue;
            }
            for (Tile tile : adjacent.getTiles()) {
                if (!tile.hasPlantNamed("Sweet Potato")) {
                    continue;
                }
                if (Math.abs(tile.getPosition().getX() - zombie.getX()) <= 2.0) {
                    state.pendingLaneShift = delta;
                    return;
                }
            }
        }
    }

    private void scheduleGarlicLaneShift(
            Lane lane,
            Zombie zombie,
            ZombieRuntimeState state
    ) {
        if (board == null || state.pendingLaneShift != 0) {
            return;
        }
        boolean upAvailable = board.getLaneAt(lane.getLaneId() - 1) != null;
        boolean downAvailable = board.getLaneAt(lane.getLaneId() + 1) != null;
        if (!upAvailable && !downAvailable) {
            return;
        }
        if (upAvailable && downAvailable) {
            state.pendingLaneShift = random.nextBoolean() ? -1 : 1;
        } else {
            state.pendingLaneShift = upAvailable ? -1 : 1;
        }
    }

    private boolean fliesOverPlant(Zombie zombie, Plant plant) {
        String zombieName = normalizeText(zombie == null ? null : zombie.getName());
        if (!zombieName.contains("dodo")) {
            return false;
        }
        String plantName = normalizeText(plant == null ? null : plant.getName());
        if (plantName.equals("tall nut")) {
            return false;
        }
        String category = normalizeCategory(plant);
        return category.equals("wall nut")
                || category.equals("explosive")
                || plantName.equals("garlic")
                || plantName.equals("sweet potato");
    }

    private Zombie selectPrimaryTarget(Plant plant, List<Zombie> candidates) {
        if (plant == null) {
            return null;
        }
        if (plant.hasTargetPriorityUp()
                || normalizeText(plant.getName()).equals("electric blueberry")) {
            return strongestTarget(candidates, false);
        }
        return nearestZombie(candidates, plant, false);
    }

    private Zombie nearestZombie(List<Zombie> candidates, Plant plant, boolean allowSameTileOnly) {
        Zombie selected = null;
        double selectedDistance = Double.MAX_VALUE;
        String name = normalizeText(plant.getName());
        boolean canShootBehind = name.equals("starfruit") || name.equals("split pea");
        double maximumRange = resolveMaximumRange(plant);

        for (Zombie zombie : candidates) {
            if (zombie == null || !zombie.isAlive() || isHypnotized(zombie)) {
                continue;
            }
            double signedDistance = zombie.getX() - plant.getX();
            if (!canShootBehind && signedDistance < 0) {
                continue;
            }
            double distance = Math.abs(signedDistance);
            if (allowSameTileOnly && distance > 1.25) {
                continue;
            }
            if (distance > maximumRange) {
                continue;
            }
            if (distance < selectedDistance) {
                selected = zombie;
                selectedDistance = distance;
            }
        }
        return selected;
    }

    private Zombie nearestZombieBehind(List<Zombie> candidates, Plant plant) {
        Zombie selected = null;
        double selectedDistance = Double.MAX_VALUE;
        for (Zombie zombie : candidates) {
            if (zombie == null || !zombie.isAlive() || isHypnotized(zombie)
                    || zombie.getX() >= plant.getX()) {
                continue;
            }
            double distance = plant.getX() - zombie.getX();
            if (distance < selectedDistance) {
                selected = zombie;
                selectedDistance = distance;
            }
        }
        return selected;
    }

    private Zombie strongestTarget(List<Zombie> candidates, boolean includeHypnotized) {
        Zombie selected = null;
        int strongest = Integer.MIN_VALUE;
        for (Zombie zombie : candidates) {
            if (zombie == null || !zombie.isAlive()
                    || (!includeHypnotized && isHypnotized(zombie))) {
                continue;
            }
            int strength = zombie.getHp()
                    + (zombie.getArmor() == null ? 0 : zombie.getArmor().getHp());
            if (strength > strongest) {
                strongest = strength;
                selected = zombie;
            }
        }
        return selected;
    }

    private List<Zombie> closestTargets(
            Plant plant,
            List<Zombie> candidates,
            int limit,
            boolean onlyNear
    ) {
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : candidates) {
            if (zombie == null || !zombie.isAlive() || isHypnotized(zombie)) {
                continue;
            }
            if (onlyNear && Math.abs(zombie.getX() - plant.getX()) > 1.25) {
                continue;
            }
            result.add(zombie);
        }
        result.sort(Comparator.comparingDouble(zombie -> Math.abs(zombie.getX() - plant.getX())));
        if (result.size() > limit) {
            return new ArrayList<>(result.subList(0, limit));
        }
        return result;
    }

    private List<Zombie> collectCandidateZombies(Plant plant, Lane ownLane) {
        String name = normalizeText(plant.getName());
        String category = normalizeCategory(plant);
        if (board != null && (category.equals("homing")
                || name.equals("cat tail")
                || name.equals("electric blueberry")
                || name.equals("caulipower")
                || name.equals("starfruit"))) {
            return new ArrayList<>(board.getAllZombies());
        }
        return new ArrayList<>(ownLane.getAllZombies());
    }

    private List<Zombie> allLivingEnemyZombies() {
        if (board == null) {
            return Collections.emptyList();
        }
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie != null && zombie.isAlive() && !isHypnotized(zombie)) {
                result.add(zombie);
            }
        }
        return result;
    }

    private Zombie findArmoredTarget(Plant plant, Lane lane) {
        for (Zombie zombie : collectCandidateZombies(plant, lane)) {
            Armor armor = zombie.getArmor();
            if (armor == null || armor.isBroken()) {
                continue;
            }
            String type = normalizeText(armor.getArmorType());
            if (type.equals("metallic") || normalizeText(armor.getName()).contains("bucket")
                    || normalizeText(armor.getName()).contains("crown")
                    || normalizeText(armor.getName()).contains("shoulder")) {
                return zombie;
            }
        }
        return null;
    }

    private double resolveMaximumRange(Plant plant) {
        String name = normalizeText(plant.getName());
        int bonus = Math.max(0, plant.getRange() - 1);
        if (normalizeCategory(plant).equals("melee")) {
            return MELEE_RANGE + bonus;
        }
        if (name.equals("puff shroom") || name.equals("sea shroom")) {
            return 3 + bonus;
        }
        if (name.equals("wasabi whip")) {
            return 2 + bonus;
        }
        return Double.MAX_VALUE;
    }

    private boolean canPlantAttack(Plant plant) {
        String category = normalizeCategory(plant);
        return category.equals("shooter")
                || category.equals("strike through")
                || category.equals("homing")
                || category.equals("lobber")
                || category.equals("melee");
    }

    private boolean isLifespanExpired(Plant plant, PlantRuntimeState state) {
        int configured = plant.getLifespanTicks();
        String name = normalizeText(plant.getName());
        if (configured <= 0 && (name.equals("puff shroom") || name.equals("sea shroom"))) {
            configured = DEFAULT_SHROOM_LIFESPAN_TICKS;
        }
        return configured > 0 && state.ageTicks >= configured;
    }

    private boolean hasTorchwoodBetween(Plant plant, Zombie target, Lane lane) {
        if (lane == null || plant == null || target == null || !isPeaPlant(plant)) {
            return false;
        }
        int startX = (int) Math.floor(plant.getX()) + 1;
        int endX = (int) Math.ceil(target.getX()) - 1;
        for (int x = startX; x <= endX; x++) {
            Tile tile = lane.getTileAt(x);
            if (tile != null && tile.hasPlantNamed("Torchwood")) {
                return true;
            }
        }
        return false;
    }

    private boolean isPeaPlant(Plant plant) {
        String name = normalizeText(plant == null ? null : plant.getName());
        return name.contains("pea")
                || name.equals("peashooter")
                || name.equals("repeater")
                || name.equals("threepeater")
                || name.equals("split pea")
                || name.equals("mega gatling pea");
    }

    private Tile findBlockingTerrain(Lane lane, Plant plant, Zombie target) {
        int startX = Math.max(1, (int) Math.floor(plant.getX()) + 1);
        int endX = Math.min(lane.getWidth(), (int) Math.ceil(target.getX()));
        boolean lobber = normalizeCategory(plant).equals("lobber");
        if (lobber || normalizeCategory(plant).equals("homing")) {
            return null;
        }

        for (int x = startX; x <= endX; x++) {
            Tile tile = lane.getTileAt(x);
            if (tile != null && tile.hasDamageableTerrain()) {
                return tile;
            }
        }
        return null;
    }

    private List<Zombie> handleLaneEnd(Lane lane, List<Zombie> zombies) {
        if (!hasLivingZombieAtLaneEnd(zombies)) {
            return Collections.emptyList();
        }

        LawnMower mower = lane.getLawnMower();
        if (!mower.isReady()) {
            return Collections.emptyList();
        }

        return mower.destroyZombies(zombies);
    }

    private boolean hasLivingZombieAtLaneEnd(List<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (zombie.isAlive() && zombie.getX() <= 0) {
                return true;
            }
        }
        return false;
    }

    private int removeDeadPlants(
            Lane lane,
            Set<Plant> consumedPlants,
            Map<Plant, Position> plantPositions,
            List<GameEvent> events
    ) {
        int destroyed = 0;
        for (Tile tile : lane.getTiles()) {
            for (Plant plant : new ArrayList<>(tile.getPlants())) {
                if (plant == null || plant.isAlive()) {
                    continue;
                }
                Position position = plantPositions.getOrDefault(plant, tile.getPosition());
                triggerPlantDeathEffect(plant, position, events);
                if (!tile.removePlant(plant)) {
                    continue;
                }
                plantStates.remove(plant);
                if (!consumedPlants.contains(plant)) {
                    destroyed++;
                    events.add(GameEvent.plantDestroyed(plant.getName(), position));
                }
            }
        }
        return destroyed;
    }

    private void triggerPlantDeathEffect(Plant plant, Position position, List<GameEvent> events) {
        PlantRuntimeState state = plantStateOf(plant);
        if (state.deathEffectHandled) {
            return;
        }
        state.deathEffectHandled = true;
        String name = normalizeText(plant.getName());

        if (name.equals("explode o nut") || plant.getExplodeDamage() > 0) {
            int damage = Math.max(1800, plant.getExplodeDamage());
            damageArea(position, 1, 1, damage, "death explosion", plant, null);
        } else if (plant.hasAoeOnDeath()) {
            int damage = Math.max(20, plant.getAttackDamage());
            damageArea(position, 1, 1, damage, "death area", plant, null);
        }
    }

    private void redistributeLivingZombies(Lane lane, List<Zombie> zombies) {
        for (Tile tile : lane.getTiles()) {
            tile.clearZombies();
        }

        for (Zombie zombie : zombies) {
            if (!zombie.isAlive() || zombie.getX() <= 0) {
                continue;
            }
            ZombieRuntimeState state = stateOf(zombie);
            if (state.pendingLaneShift != 0 && board != null) {
                int targetLaneNumber = lane.getLaneId() + state.pendingLaneShift;
                Lane targetLane = board.getLaneAt(targetLaneNumber);
                if (targetLane != null) {
                    zombie.moveBy(0, state.pendingLaneShift);
                    Tile targetTile = targetLane.getTileAt(clampX(targetLane, zombie));
                    if (targetTile != null) {
                        targetTile.addZombie(zombie);
                    }
                    state.pendingLaneShift = 0;
                    continue;
                }
                state.pendingLaneShift = 0;
            }
            Tile tile = tileForZombie(lane, zombie);
            if (tile != null) {
                tile.addZombie(zombie);
            }
        }
    }

    private int clampX(Lane lane, Zombie zombie) {
        return Math.max(1, Math.min(lane.getWidth(), (int) Math.ceil(zombie.getX())));
    }

    private Tile tileForZombie(Lane lane, Zombie zombie) {
        return lane.getTileAt(clampX(lane, zombie));
    }

    private List<Zombie> collectLivingZombies(Lane lane) {
        List<Zombie> zombies = new ArrayList<>();
        Set<Zombie> seen = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Tile tile : lane.getTiles()) {
            for (Zombie zombie : tile.getZombies()) {
                if (zombie != null && zombie.isAlive() && seen.add(zombie)) {
                    zombies.add(zombie);
                }
            }
        }
        return zombies;
    }

    private void consumePlant(Tile tile, Plant plant, Set<Plant> consumedPlants) {
        if (tile.removePlant(plant)) {
            consumedPlants.add(plant);
            plantStates.remove(plant);
        }
    }

    private int effectiveDamage(Plant plant, int baseDamage) {
        int damage = Math.max(baseDamage, plant.getAttackDamage());
        if (plant.isBoosted()) {
            damage *= 2;
        }
        return damage;
    }

    private boolean isFirePlant(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return false;
        }
        String tags = normalizeText(plant.getType().getTags());
        if (tags.contains("fire")) {
            return true;
        }
        String name = normalizeText(plant.getName());
        return name.contains("fire")
                || name.contains("pepper")
                || name.contains("jalapeno")
                || name.contains("torchwood")
                || name.contains("wasabi")
                || name.contains("hot potato");
    }

    private String resolveDamageType(Plant plant) {
        String category = normalizeCategory(plant);
        if (category.equals("strike through")) {
            return "piercing";
        }
        if (category.equals("homing")) {
            return "homing";
        }
        if (category.equals("lobber")) {
            return "lobber";
        }
        if (category.equals("melee")) {
            return "melee";
        }
        return "normal";
    }

    private ZombieRuntimeState stateOf(Zombie zombie) {
        return zombieStates.computeIfAbsent(zombie, ignored -> new ZombieRuntimeState());
    }

    private PlantRuntimeState plantStateOf(Plant plant) {
        return plantStates.computeIfAbsent(plant, ignored -> new PlantRuntimeState());
    }

    private String normalizeCategory(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return "";
        }
        return normalizeText(plant.getType().getCategory());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}
