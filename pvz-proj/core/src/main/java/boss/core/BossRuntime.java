package boss.core;

import models.core.plant.Plant;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.GraveSpawnRules;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class BossRuntime {
    public static final int TICKS_PER_SECOND = 10;

    private static final int ACTION_MIN_GAP_TICKS = 45;
    private static final int ACTION_GAP_SPREAD_TICKS = 31;
    private static final int COMMON_ACTION_TICKS = 16;
    private static final double BOSS_X = 8.65;
    private static final double SUMMON_X = 8.95;
    private static final int DARK_SUMMON_IMPACT_TICKS = 10;
    private static final int DARK_SUMMON_END_TICKS = 22;
    private static final int BEACH_SUMMON_IMPACT_TICKS = 20;
    private static final int BEACH_SUMMON_END_TICKS = 30;

    private static final int EGYPT_MISSILE_AIM_TICKS = 33;
    private static final int EGYPT_MISSILE_FLIGHT_TICKS = 3;
    private static final int EGYPT_MISSILE_EXPLOSION_TICKS = 17;
    private static final int EGYPT_CHARGE_FORWARD_TICKS = 12;
    private static final int EGYPT_CHARGE_IMPACT_HOLD_TICKS = 3;
    private static final int EGYPT_CHARGE_BACKWARD_TICKS = 12;
    private static final double EGYPT_CHARGE_FRONT_X = 2.25;

    private static final int FROST_MISSILE_LAUNCH_TICKS = 29;
    private static final int FROST_MISSILE_IMPACT_TICKS = 30;
    private static final int FROST_MISSILE_END_TICKS = 37;
    private static final int FROST_WIND_IMPACT_TICKS = 8;
    private static final int FROST_WIND_ACTION_TICKS = 28;
    private static final int FROST_GLACIER_RESOLVE_TICKS = 31;
    private static final int FROST_GLACIER_ACTION_TICKS = 63;

    private static final int DARK_FIREBALL_IMPACT_TICKS = 18;
    private static final int DARK_BREATH_IMPACT_TICKS = 18;
    private static final int DARK_FIRE_LIFETIME_TICKS = 40;
    private static final int DARK_ACTION_END_TICKS = 34;

    private static final int BEACH_SUBMERGE_TICKS = 17;
    private static final int BEACH_SHARK_IMPACT_TICKS = 20;
    private static final int BEACH_SHARK_END_TICKS = 42;
    private static final int BEACH_TURBINE_CAPTURE_TICKS = 21;
    private static final int BEACH_TURBINE_RESOLVE_TICKS = 41;
    private static final int BEACH_TURBINE_END_TICKS = 66;

    private final Boss boss;
    private final Random random;
    private final ZombieFactory zombieFactory;
    private final List<BossHitbox> hitboxes;
    private final List<String> summonPool;
    private final List<String> frozenZombiePool;
    private final Map<Position, String> frozenZombieSpawns;

    private final List<Position> actionTargets;
    private final List<Position> summonTargets;
    private final List<String> summonNames;
    private final List<Position> sharkPositions;
    private final Map<Position, Integer> burningTiles;
    private final List<TurbineVictim> turbineVictims;

    private Board board;
    private boolean started;
    private int stateUntilTick;
    private int nextActionTick;
    private int observedSectionBreakSerial;
    private boolean deathStarted;
    private int moveDirection;
    private int moveStartFirstLane;
    private int pendingFirstLane;

    private int currentTick;
    private int actionStartTick;
    private int actionStageStartTick;
    private int actionStage;
    private Position actionTarget;
    private int missileLaunchTick;
    private int missileImpactTick;
    private boolean missileResolved;
    private int chargeImpactTick;
    private int chargeReturnTick;
    private boolean chargeImpactResolved;
    private int specialImpactTick;
    private int specialResolveTick;
    private boolean specialImpactResolved;
    private boolean specialResolveResolved;
    private int arrivalScorchUntilTick;
    private boolean beachTangleStun;

    private int frostMissileLaunchTick;
    private int frostMissileImpactTick;
    private boolean frostMissileResolved;
    private final List<Integer> frostWindLanes;
    private int frostWindImpactTick;
    private boolean frostWindResolved;
    private int frostFreezeColumn;
    private int frostFreezeResolveTick;
    private boolean frostFreezeResolved;
    private int frostVisualVariant;

    public BossRuntime(Boss boss, long seed) {
        if (boss == null) {
            throw new IllegalArgumentException("Boss runtime requires a boss.");
        }
        this.boss = boss;
        random = new Random(seed);
        zombieFactory = new ZombieFactory();
        hitboxes = new ArrayList<>();
        summonPool = new ArrayList<>();
        frozenZombiePool = new ArrayList<>();
        frozenZombieSpawns = new LinkedHashMap<>();
        frostWindLanes = new ArrayList<>();

        actionTargets = new ArrayList<>();
        summonTargets = new ArrayList<>();
        summonNames = new ArrayList<>();
        sharkPositions = new ArrayList<>();
        burningTiles = new LinkedHashMap<>();
        turbineVictims = new ArrayList<>();
        started = false;
        stateUntilTick = 0;
        nextActionTick = Integer.MAX_VALUE;
        observedSectionBreakSerial = 0;
        deathStarted = false;
        moveDirection = 0;
        moveStartFirstLane = boss.getFirstLane();
        currentTick = 0;
        arrivalScorchUntilTick = 0;
        beachTangleStun = false;
        clearActionState();
    }

    public void start(Board board, List<String> allowedZombieNames, int currentTick) {
        if (started) return;
        if (board == null) throw new IllegalArgumentException("Boss runtime requires a board.");
        this.board = board;
        this.currentTick = currentTick;
        summonPool.clear();
        frozenZombiePool.clear();
        frozenZombieSpawns.clear();
        if (allowedZombieNames != null) {
            for (String name : allowedZombieNames) {
                if (isSafeSummon(name)) {
                    summonPool.add(name.trim());
                }
                if (isSafeFrozenZombie(name)) {
                    frozenZombiePool.add(name.trim());
                }
            }
        }
        if (frozenZombiePool.isEmpty()) {
            frozenZombiePool.add("Default");
        }
        boss.setFirstLane(Math.min(2, Math.max(1, board.getHeight() - 1)));
        boss.setX(BOSS_X);
        attachHitboxes();
        initializeBossArenaVisualState(currentTick);
        boss.setState(BossState.INTRO, BossAction.NONE);
        stateUntilTick = currentTick + boss.getIntroTicks();
        nextActionTick = stateUntilTick + nextActionDelay();
        observedSectionBreakSerial = boss.getHealth().getSectionBreakSerial();
        started = true;
    }

    public void update(int currentTick) {
        this.currentTick = currentTick;
        burningTiles.entrySet().removeIf(entry -> currentTick >= entry.getValue());
        if (!started || boss.getState() == BossState.DEFEATED) {
            return;
        }
        releaseThawedFrozenZombies();
        observeHealth(currentTick);
        if (deathStarted) {
            if (currentTick >= stateUntilTick) finishDefeat();
            return;
        }
        if (boss.getState() == BossState.STUNNED || boss.getState() == BossState.INTRO) {
            if (currentTick >= stateUntilTick) activate(currentTick);
            return;
        }
        if (boss.getState() == BossState.ACTION) {
            updateCurrentAction(currentTick);
            if (currentTick >= stateUntilTick) activate(currentTick);
            return;
        }
        if (boss.getState() == BossState.ACTIVE && currentTick >= nextActionTick) {
            startRandomAction(currentTick);
        }
    }

    private void observeHealth(int currentTick) {
        if (boss.getHealth().isDepleted()) {
            if (!deathStarted) {
                deathStarted = true;
                cancelTransientAction();
                boss.setState(BossState.DYING, BossAction.NONE);
                stateUntilTick = currentTick + boss.getDeathTicks();
            }
            return;
        }
        int serial = boss.getHealth().getSectionBreakSerial();
        if (serial > observedSectionBreakSerial) {
            observedSectionBreakSerial = serial;
            cancelTransientAction();
            boss.setState(BossState.STUNNED, BossAction.NONE);
            stateUntilTick = currentTick + boss.getStunTicks();
        }
    }

    private void activate(int currentTick) {
        restoreBossOrigin();
        clearActionState();
        boss.setState(BossState.ACTIVE, BossAction.NONE);
        nextActionTick = currentTick + nextActionDelay();
        moveDirection = 0;
        beachTangleStun = false;
    }

    private void startRandomAction(int currentTick) {
        List<BossAction> choices = new ArrayList<>();
        if (boss.isLaneChangesAllowed()) {
            choices.add(BossAction.MOVE_LANES);
        }
        if (boss.isZombieSpawnsAllowed() && !summonPool.isEmpty()) {
            choices.add(BossAction.SPAWN_ZOMBIES);
        }
        if (isEgyptBoss()) {
            choices.add(BossAction.MISSILE);
            choices.add(BossAction.CHARGE);
        } else if (isFrostbiteBoss()) {
            choices.add(BossAction.ICE_MISSILE);
            choices.add(BossAction.ICE_WIND);
            choices.add(BossAction.FREEZE_COLUMN);
        } else if (isDarkBoss()) {
            choices.add(BossAction.DARK_FIREBALLS);
            choices.add(BossAction.DARK_FIRE_BREATH);
        } else if (isBeachBoss()) {
            choices.add(BossAction.BEACH_BABY_SHARKS);
            choices.add(BossAction.BEACH_TURBINE);
        }
        if (choices.isEmpty()) {
            nextActionTick = currentTick + nextActionDelay();
            return;
        }

        BossAction action = choices.get(random.nextInt(choices.size()));
        switch (action) {
            case MOVE_LANES -> startLaneMove(currentTick);
            case SPAWN_ZOMBIES -> startZombieSpawn(currentTick);
            case MISSILE -> startEgyptMissile(currentTick);
            case CHARGE -> startEgyptCharge(currentTick);
            case ICE_MISSILE -> startFrostMissile(currentTick);
            case ICE_WIND -> startFrostWind(currentTick);
            case FREEZE_COLUMN -> startFrostFreezeColumn(currentTick);
            case DARK_FIREBALLS -> startDarkFireballs(currentTick);
            case DARK_FIRE_BREATH -> startDarkFireBreath(currentTick);
            case BEACH_BABY_SHARKS -> startBeachBabySharks(currentTick);
            case BEACH_TURBINE -> startBeachTurbine(currentTick);
            default -> nextActionTick = currentTick + nextActionDelay();
        }
    }

    private void startLaneMove(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        actionStageStartTick = currentTick;
        int oldLane = boss.getFirstLane();
        moveStartFirstLane = oldLane;
        pendingFirstLane = isBeachBoss() ? chooseBeachSharkLanePair(oldLane) : chooseAnotherLanePair(oldLane);
        moveDirection = Integer.compare(pendingFirstLane, oldLane);
        boss.setState(BossState.ACTION, BossAction.MOVE_LANES);
        if (isBeachBoss()) {
            specialImpactTick = currentTick + BEACH_SUBMERGE_TICKS;
            stateUntilTick = specialImpactTick + 16;
        } else if (isDarkBoss()) {
            specialImpactTick = currentTick + COMMON_ACTION_TICKS;
            stateUntilTick = specialImpactTick;
        } else {
            applyPendingLaneMove();
            stateUntilTick = currentTick + COMMON_ACTION_TICKS;
        }
    }

    private void startZombieSpawn(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        actionStageStartTick = currentTick;
        boss.setState(BossState.ACTION, BossAction.SPAWN_ZOMBIES);
        if (isDarkBoss() || isBeachBoss()) {
            planZombieSummons();
            specialImpactTick = currentTick + (isDarkBoss()
                    ? DARK_SUMMON_IMPACT_TICKS : BEACH_SUMMON_IMPACT_TICKS);
            stateUntilTick = currentTick + (isDarkBoss()
                    ? DARK_SUMMON_END_TICKS : BEACH_SUMMON_END_TICKS);
            return;
        }
        summonZombies();
        stateUntilTick = currentTick + COMMON_ACTION_TICKS;
    }

    private void startEgyptMissile(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        actionTarget = chooseRandomBoardTile();
        missileLaunchTick = currentTick + EGYPT_MISSILE_AIM_TICKS;
        missileImpactTick = missileLaunchTick + EGYPT_MISSILE_FLIGHT_TICKS;
        stateUntilTick = missileImpactTick + EGYPT_MISSILE_EXPLOSION_TICKS;
        boss.setState(BossState.ACTION, BossAction.MISSILE);
    }

    private void startEgyptCharge(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        chargeImpactTick = currentTick + EGYPT_CHARGE_FORWARD_TICKS;
        chargeReturnTick = chargeImpactTick + EGYPT_CHARGE_IMPACT_HOLD_TICKS;
        stateUntilTick = chargeReturnTick + EGYPT_CHARGE_BACKWARD_TICKS;
        boss.setState(BossState.ACTION, BossAction.CHARGE);
    }

    private void startFrostMissile(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        actionStageStartTick = currentTick;
        actionTarget = chooseRandomBoardTile();
        frostMissileLaunchTick = currentTick + FROST_MISSILE_LAUNCH_TICKS;
        frostMissileImpactTick = currentTick + FROST_MISSILE_IMPACT_TICKS;
        stateUntilTick = currentTick + FROST_MISSILE_END_TICKS;
        boss.setState(BossState.ACTION, BossAction.ICE_MISSILE);
    }

    private void startFrostWind(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        actionStageStartTick = currentTick;
        frostWindLanes.addAll(chooseDistinctLanes(2));
        frostVisualVariant = random.nextInt(4) + 1;
        frostWindImpactTick = currentTick + FROST_WIND_IMPACT_TICKS;
        stateUntilTick = currentTick + FROST_WIND_ACTION_TICKS;
        boss.setState(BossState.ACTION, BossAction.ICE_WIND);
    }

    private void startFrostFreezeColumn(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        actionStageStartTick = currentTick;
        frostFreezeColumn = chooseFrostFreezeColumn();
        frostVisualVariant = random.nextInt(6) + 1;
        frostFreezeResolveTick = currentTick + FROST_GLACIER_RESOLVE_TICKS;
        stateUntilTick = currentTick + FROST_GLACIER_ACTION_TICKS;
        boss.setState(BossState.ACTION, BossAction.FREEZE_COLUMN);

    private void startDarkFireballs(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        int count = Math.min(3, 1 + boss.getHealth().getSectionBreakSerial());
        chooseDistinctTargets(count, false);
        specialImpactTick = currentTick + DARK_FIREBALL_IMPACT_TICKS;
        stateUntilTick = currentTick + DARK_ACTION_END_TICKS;
        boss.setState(BossState.ACTION, BossAction.DARK_FIREBALLS);
    }

    private void startDarkFireBreath(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        specialImpactTick = currentTick + DARK_BREATH_IMPACT_TICKS;
        stateUntilTick = currentTick + DARK_ACTION_END_TICKS;
        for (int lane : new int[]{boss.getFirstLane(), boss.getSecondLane()}) {
            for (int x = 1; x <= board.getWidth(); x++) actionTargets.add(new Position(x, lane));
        }
        boss.setState(BossState.ACTION, BossAction.DARK_FIRE_BREATH);
    }

    private void startBeachBabySharks(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        List<Position> waterPlants = new ArrayList<>();
        for (int y = 1; y <= board.getHeight(); y++) {
            for (int x = 1; x <= board.getWidth(); x++) {
                Position p = new Position(x, y);
                Tile tile = board.getTileAt(p);
                if (tile.getTileType() == TileType.WATER && tile.hasPlant()) waterPlants.add(p);
            }
        }
        Collections.shuffle(waterPlants, random);
        int wanted = Math.min(4, 2 + boss.getHealth().getSectionBreakSerial());
        if (!waterPlants.isEmpty()) {
            actionTargets.addAll(waterPlants.subList(0, Math.min(wanted, waterPlants.size())));
        } else {
            chooseDistinctTargets(wanted, true);
        }
        specialImpactTick = currentTick + BEACH_SHARK_IMPACT_TICKS;
        stateUntilTick = currentTick + BEACH_SHARK_END_TICKS;
        boss.setState(BossState.ACTION, BossAction.BEACH_BABY_SHARKS);
    }

    private void startBeachTurbine(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        specialImpactTick = currentTick + BEACH_TURBINE_CAPTURE_TICKS;
        specialResolveTick = currentTick + BEACH_TURBINE_RESOLVE_TICKS;
        captureTurbineVictims();
        stateUntilTick = currentTick + BEACH_TURBINE_END_TICKS;
        boss.setState(BossState.ACTION, BossAction.BEACH_TURBINE);
    }

    private void updateCurrentAction(int currentTick) {
        switch (boss.getAction()) {
            case MOVE_LANES -> updateLaneMove(currentTick);
            case SPAWN_ZOMBIES -> updateZombieSpawn(currentTick);
            case MISSILE -> updateEgyptMissile(currentTick);
            case CHARGE -> updateEgyptCharge(currentTick);
            case DARK_FIREBALLS -> updateDarkFireballs(currentTick);
            case DARK_FIRE_BREATH -> updateDarkFireBreath(currentTick);
            case BEACH_BABY_SHARKS -> updateBeachBabySharks(currentTick);
            case BEACH_TURBINE -> updateBeachTurbine(currentTick);
            case ICE_MISSILE -> updateFrostMissile(currentTick);
            case ICE_WIND -> updateFrostWind(currentTick);
            case FREEZE_COLUMN -> updateFrostFreezeColumn(currentTick);
            default -> { }
        }
    }

    private void updateZombieSpawn(int tick) {
        if (summonTargets.isEmpty() || specialImpactResolved || tick < specialImpactTick) {
            return;
        }
        specialImpactResolved = true;
        actionStage = 1;
        actionStageStartTick = tick;
        for (int index = 0; index < summonTargets.size(); index++) {
            spawnSummonedZombie(summonNames.get(index), summonTargets.get(index));
        }
    }

    private void updateLaneMove(int tick) {
        if ((isBeachBoss() || isDarkBoss()) && !specialImpactResolved && tick >= specialImpactTick) {
            specialImpactResolved = true;
            actionStage = 1;
            actionStageStartTick = tick;
            applyPendingLaneMove();
        }
    }

    private void updateEgyptMissile(int currentTick) {
        if (currentTick >= missileLaunchTick && actionStage == 0) {
            actionStage = 1;
            actionStageStartTick = missileLaunchTick;
        }
        if (!missileResolved && currentTick >= missileImpactTick) {
            missileResolved = true;
            resolveEgyptMissileImpact();
        }
    }

    private void updateEgyptCharge(int currentTick) {
        if (currentTick < chargeImpactTick) {
            moveBossX(lerp(BOSS_X, EGYPT_CHARGE_FRONT_X,
                    fraction(currentTick - actionStartTick, EGYPT_CHARGE_FORWARD_TICKS)));
            return;
        }
        moveBossX(EGYPT_CHARGE_FRONT_X);
        if (!chargeImpactResolved) {
            chargeImpactResolved = true;
            actionStage = 1;
            actionStageStartTick = chargeImpactTick;
            destroyPlantsInBossLanes();
        }
        if (currentTick >= chargeReturnTick) {
            actionStage = 2;
            float progress = fraction(currentTick - chargeReturnTick, EGYPT_CHARGE_BACKWARD_TICKS);
            moveBossX(lerp(EGYPT_CHARGE_FRONT_X, BOSS_X, progress));
        }
    }

    private void updateFrostMissile(int currentTick) {
        if (currentTick >= frostMissileLaunchTick && actionStage == 0) {
            actionStage = 1;
            actionStageStartTick = frostMissileLaunchTick;
        }
        if (!frostMissileResolved && currentTick >= frostMissileImpactTick) {
            frostMissileResolved = true;
            actionStage = 2;
            actionStageStartTick = frostMissileImpactTick;
            destroyPlantsAt(actionTarget);
        }
    }

    private void updateFrostWind(int currentTick) {
        if (!frostWindResolved && currentTick >= frostWindImpactTick) {
            frostWindResolved = true;
            actionStage = 1;
            actionStageStartTick = frostWindImpactTick;
            for (int lane : frostWindLanes) {
                applyFrostWindToLane(lane);
            }
        }
    }

    private void updateFrostFreezeColumn(int currentTick) {
        if (!frostFreezeResolved && currentTick >= frostFreezeResolveTick) {
            frostFreezeResolved = true;
            actionStage = 1;
            actionStageStartTick = frostFreezeResolveTick;
            resolveFrostFreezeColumn();
        }
    }

    private void destroyPlantsAt(Position position) {
        if (board == null || position == null || !board.isValidPosition(position)) {
            return;
        }
        Tile tile = board.getTileAt(position);
        for (Plant plant : new ArrayList<>(tile.getPlants())) {
            if (plant != null && plant.isAlive()) {
                plant.kill();
            }
        }
        board.removeDeadEntities();
    }

    private void applyFrostWindToLane(int laneNumber) {
        if (board == null || laneNumber < 1 || laneNumber > board.getHeight()) {
            return;
        }
        for (Plant plant : board.getAllPlants()) {
            if (plant == null || !plant.isAlive()
                    || (int) Math.round(plant.getY()) != laneNumber
                    || isFirePlant(plant)) {
                continue;
            }
            plant.addIceHit();
        }
    }

    private boolean isFirePlant(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return false;
        }
        String tags = plant.getType().getTags() == null
                ? "" : plant.getType().getTags().toLowerCase(Locale.ROOT);
        if (tags.contains("fire")) {
            return true;
        }
        String name = plant.getName() == null ? "" : plant.getName().toLowerCase(Locale.ROOT);
        return name.contains("fire")
                || name.contains("pepper")
                || name.contains("jalapeno")
                || name.contains("torchwood")
                || name.contains("wasabi")
                || name.contains("hot potato");
    }

    private List<Integer> chooseDistinctLanes(int count) {
        if (board == null || board.getHeight() <= 0 || count <= 0) {
            return Collections.emptyList();
        }
        List<Integer> lanes = new ArrayList<>();
        for (int lane = 1; lane <= board.getHeight(); lane++) {
            lanes.add(lane);
        }
        Collections.shuffle(lanes, random);
        return new ArrayList<>(lanes.subList(0, Math.min(count, lanes.size())));
    }

    private int chooseFrostFreezeColumn() {
        if (board == null) {
            return 1;
        }
        int maximumColumn = Math.max(1, board.getWidth() - 1);
        int bestOccupancy = Integer.MAX_VALUE;
        List<Integer> bestColumns = new ArrayList<>();
        for (int x = 1; x <= maximumColumn; x++) {
            int occupancy = 0;
            for (int lane = 1; lane <= board.getHeight(); lane++) {
                Tile tile = board.getTileAt(new Position(x, lane));
                for (Zombie zombie : tile.getZombies()) {
                    if (zombie != null && zombie.isAlive() && !(zombie instanceof BossHitbox)) {
                        occupancy++;
                    }
                }
            }
            if (occupancy < bestOccupancy) {
                bestOccupancy = occupancy;
                bestColumns.clear();
                bestColumns.add(x);
            } else if (occupancy == bestOccupancy) {
                bestColumns.add(x);
            }
        }
        return bestColumns.isEmpty() ? 1 : bestColumns.get(random.nextInt(bestColumns.size()));
    }

    private void resolveFrostFreezeColumn() {
        if (board == null || frostFreezeColumn < 1 || frostFreezeColumn > board.getWidth()) {
            return;
        }
        for (int lane = 1; lane <= board.getHeight(); lane++) {
            Position position = new Position(frostFreezeColumn, lane);
            Tile tile = board.getTileAt(position);
            for (Plant plant : new ArrayList<>(tile.getPlants())) {
                if (plant != null && plant.isAlive()) {
                    plant.kill();
                }
            }
            tile.setTileType(TileType.ICE);
            frozenZombieSpawns.put(position, chooseFrozenZombieName());
        }
        board.removeDeadEntities();
    }

    private String chooseFrozenZombieName() {
        return frozenZombiePool.get(random.nextInt(frozenZombiePool.size()));
    }

    private void releaseThawedFrozenZombies() {
        if (board == null || frozenZombieSpawns.isEmpty()) {
            return;
        }
        for (Map.Entry<Position, String> entry : new ArrayList<>(frozenZombieSpawns.entrySet())) {
            Position position = entry.getKey();
            Tile tile = board.getTileAt(position);
            if (tile == null || tile.getTileType() == TileType.ICE) {
                continue;
            }
            try {
                Zombie zombie = zombieFactory.createZombie(
                        entry.getValue(), position.getX(), position.getY()
                );
                zombie.setSeasonalIceImmune(true);
                tile.addZombie(zombie);
            } catch (IllegalArgumentException ignored) {
            }
            frozenZombieSpawns.remove(position);
        }
    }

    private void resolveEgyptMissileImpact() {
        if (board == null || actionTarget == null) {
            return;
        }
        Tile target = board.getTileAt(actionTarget);
        for (Plant plant : new ArrayList<>(target.getPlants())) {
            if (plant != null && plant.isAlive()) {
                plant.kill();

    private void updateDarkFireballs(int tick) {
        if (!specialImpactResolved && tick >= specialImpactTick) {
            specialImpactResolved = true;
            actionStage = 1;
            actionStageStartTick = tick;
            for (Position target : actionTargets) {
                killEverythingAt(target);
                burnTile(target, tick);
                spawnDragonImp(target);
            }
            board.removeDeadEntities();
        }
    }

    private void updateDarkFireBreath(int tick) {
        if (!specialImpactResolved && tick >= specialImpactTick) {
            specialImpactResolved = true;
            actionStage = 1;
            actionStageStartTick = tick;
            for (Position target : actionTargets) {
                killPlantsAt(target);
                burnTile(target, tick);
            }
            board.removeDeadEntities();
        }
    }

    private void updateBeachBabySharks(int tick) {
        if (!specialImpactResolved && tick >= specialImpactTick) {
            specialImpactResolved = true;
            actionStage = 1;
            actionStageStartTick = tick;
            for (Position target : actionTargets) killPlantsAt(target);
            board.removeDeadEntities();
        }
    }

    private void updateBeachTurbine(int tick) {
        if (tick >= specialImpactTick && actionStage == 0) {
            actionStage = 1;
            actionStageStartTick = specialImpactTick;
        }
        if (tick >= specialImpactTick && tick < specialResolveTick) {
            float progress = fraction(tick - specialImpactTick,
                    Math.max(1, specialResolveTick - specialImpactTick));
            moveTurbineVictims(progress * progress);
        }
        if (!specialResolveResolved && tick >= specialResolveTick) {
            specialResolveResolved = true;
            actionStage = 2;
            actionStageStartTick = tick;
            if (stunBeachBossWithTangleKelp(tick)) {
                return;
            }
            for (TurbineVictim victim : turbineVictims) {
                victim.kill();
            }
            board.removeDeadEntities();
        }
    }

    private void resolveEgyptMissileImpact() {
        if (board == null || actionTarget == null) return;
        killPlantsAt(actionTarget);
        board.removeDeadEntities();
        createEgyptMissileGraves();
    }

    private void createEgyptMissileGraves() {
        int capacity = Math.min(2, GraveSpawnRules.remainingCapacity(board));
        if (capacity <= 0) return;
        List<Tile> candidates = new ArrayList<>();
        for (int lane = 1; lane <= board.getHeight(); lane++) {
            for (int x = 2; x < board.getWidth(); x++) {
                Tile tile = board.getTileAt(new Position(x, lane));
                if (tile.getTileType() == TileType.NORMAL && !tile.hasPlant() && !tile.hasZombies()) {
                    candidates.add(tile);
                }
            }
        }
        Collections.shuffle(candidates, random);
        for (int i = 0; i < Math.min(capacity, candidates.size()); i++) candidates.get(i).setTileType(TileType.GRAVE);
    }

    private void destroyPlantsInBossLanes() {
        destroyPlantsInLane(boss.getFirstLane());
        destroyPlantsInLane(boss.getSecondLane());
        board.removeDeadEntities();
    }

    private void destroyPlantsInLane(int lane) {
        if (lane < 1 || lane > board.getHeight()) return;
        for (Tile tile : board.getLaneAt(lane).getTiles()) {
            for (Plant plant : new ArrayList<>(tile.getPlants())) if (plant != null && plant.isAlive()) plant.kill();
        }
    }

    private void killPlantsAt(Position p) {
        Tile tile = board.getTileAt(p);
        for (Plant plant : new ArrayList<>(tile.getPlants())) if (plant != null && plant.isAlive()) plant.kill();
    }

    private void killEverythingAt(Position p) {
        Tile tile = board.getTileAt(p);
        for (Plant plant : new ArrayList<>(tile.getPlants())) if (plant != null && plant.isAlive()) plant.kill();
        for (Zombie zombie : new ArrayList<>(tile.getZombies())) {
            if (zombie != null && zombie.isAlive() && !(zombie instanceof BossHitbox)) zombie.kill();
        }
    }

    private void burnTile(Position p, int tick) {
        if (isDarkBoss() && p != null && p.getX() >= board.getWidth() - 1) {
            burningTiles.put(p, Integer.MAX_VALUE);
            return;
        }
        burningTiles.put(p, tick + DARK_FIRE_LIFETIME_TICKS);
    }

    private void spawnDragonImp(Position p) {
        try {
            Zombie imp = zombieFactory.createZombie("Imp Dragon", p.getX(), p.getY());
            board.getTileAt(p).addZombie(imp);
        } catch (IllegalArgumentException ignored) { }
    }

    private void captureTurbineVictims() {
        turbineVictims.clear();
        for (int lane : new int[]{boss.getFirstLane(), boss.getSecondLane()}) {
            if (lane < 1 || lane > board.getHeight()) {
                continue;
            }
            for (int x = 1; x <= board.getWidth(); x++) {
                Tile tile = board.getTileAt(new Position(x, lane));
                for (Plant plant : tile.getPlants()) {
                    if (plant != null && plant.isAlive()) {
                        turbineVictims.add(new TurbineVictim(plant));
                    }
                }
                for (Zombie zombie : tile.getZombies()) {
                    if (zombie != null && zombie.isAlive() && !(zombie instanceof BossHitbox)) {
                        turbineVictims.add(new TurbineVictim(zombie));
                    }
                }
            }
        }
    }

    private void moveTurbineVictims(float progress) {
        double targetX = boss.getX() + 0.20;
        for (TurbineVictim victim : turbineVictims) {
            victim.moveTo(lerp(victim.startX, targetX, progress), victim.startY);
        }
    }

    private boolean stunBeachBossWithTangleKelp(int tick) {
        for (TurbineVictim victim : turbineVictims) {
            if (!victim.isTangleKelp()) {
                continue;
            }
            victim.kill();
            for (TurbineVictim other : turbineVictims) {
                if (other != victim) {
                    other.restore();
                }
            }
            board.removeDeadEntities();
            beachTangleStun = true;
            boss.setState(BossState.STUNNED, BossAction.NONE);
            stateUntilTick = tick + Math.max(boss.getStunTicks(), 40);
            return true;
        }
        return false;
    }

    private void chooseDistinctTargets(int count, boolean waterOnly) {
        List<Position> candidates = new ArrayList<>();
        for (int y = 1; y <= board.getHeight(); y++) {
            for (int x = 1; x <= board.getWidth(); x++) {
                Position p = new Position(x, y);
                Tile tile = board.getTileAt(p);
                if (waterOnly && tile.getTileType() != TileType.WATER) {
                    continue;
                }
                if (isDarkBoss() && tile.isGraveTerrain()) {
                    continue;
                }
                if (isDarkBoss() && isPlantingBlocked(p)) {
                    continue;
                }
                candidates.add(p);
            }
        }
        Collections.shuffle(candidates, random);
        actionTargets.addAll(candidates.subList(0, Math.min(count, candidates.size())));
    }

    private Position chooseRandomBoardTile() {
        return new Position(random.nextInt(board.getWidth()) + 1, random.nextInt(board.getHeight()) + 1);
    }

    private int chooseBeachSharkLanePair(int oldFirstLane) {
        if (sharkPositions.isEmpty()) {
            return chooseAnotherLanePair(oldFirstLane);
        }
        int next = oldFirstLane;
        for (int attempt = 0; attempt < 8 && next == oldFirstLane; attempt++) {
            Position shark = sharkPositions.get(random.nextInt(sharkPositions.size()));
            next = Math.max(1, Math.min(board.getHeight() - 1, shark.getY()));
        }
        return next;
    }

    private void planZombieSummons() {
        summonTargets.clear();
        summonNames.clear();
        int originalCount = Math.min(6, 4 + boss.getHealth().getSectionBreakSerial());
        int count = Math.max(1, Math.round(originalCount * 0.6f));
        List<Position> candidates = new ArrayList<>();
        if (isDarkBoss()) {
            for (int lane = 1; lane <= board.getHeight(); lane++) {
                for (int x = 3; x <= 5; x++) {
                    addSummonCandidate(candidates, x, lane);
                }
            }
        } else {
            for (int lane = 1; lane <= board.getHeight(); lane++) {
                for (int x = 6; x <= board.getWidth(); x++) {
                    addSummonCandidate(candidates, x, lane);
                }
            }
        }
        Collections.shuffle(candidates, random);
        int limit = Math.min(count, candidates.size());
        for (int index = 0; index < limit; index++) {
            summonTargets.add(candidates.get(index));
            summonNames.add(summonPool.get(random.nextInt(summonPool.size())));
        }
    }

    private void addSummonCandidate(List<Position> candidates, int x, int lane) {
        Position position = new Position(x, lane);
        Tile tile = board.getTileAt(position);
        if (tile == null || tile.isGraveTerrain() || tile.hasPlant() || tile.hasZombies()) {
            return;
        }
        if (isBeachBoss() && tile.getTileType() != TileType.WATER) {
            return;
        }
        candidates.add(position);
    }

    private void spawnSummonedZombie(String name, Position position) {
        try {
            Zombie zombie = zombieFactory.createZombie(name, position.getX(), position.getY());
            board.getTileAt(position).addZombie(zombie);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void initializeBossArenaVisualState(int tick) {
        if (isDarkBoss()) {
            arrivalScorchUntilTick = Integer.MAX_VALUE;
            for (int lane = 1; lane <= board.getHeight(); lane++) {
                burningTiles.put(new Position(board.getWidth(), lane), arrivalScorchUntilTick);
                burningTiles.put(new Position(board.getWidth() - 1, lane), arrivalScorchUntilTick);
            }
        }
        if (isBeachBoss()) {
            sharkPositions.clear();
            for (int lane = 1; lane <= board.getHeight(); lane++) {
                sharkPositions.add(new Position(board.getWidth(), lane));
            }
        }
    }

    private int chooseAnotherLanePair(int oldFirstLane) {
        int maxFirstLane = Math.max(1, board.getHeight() - 1);
        int next = oldFirstLane;
        while (maxFirstLane > 1 && next == oldFirstLane) next = random.nextInt(maxFirstLane) + 1;
        return next;
    }

    private void applyPendingLaneMove() {
        if (pendingFirstLane <= 0 || pendingFirstLane == boss.getFirstLane()) return;
        boss.setFirstLane(pendingFirstLane);
        relocateHitboxes();
    }

    private void summonZombies() {
        if (board == null || summonPool.isEmpty()) return;
        int section = boss.getHealth().getSectionBreakSerial();
        int originalCount = Math.min(4, 2 + section);
        int count = Math.max(1, Math.round(originalCount * 0.6f));
        for (int i = 0; i < count; i++) {
            String name = summonPool.get(random.nextInt(summonPool.size()));
            int lane = 1 + random.nextInt(board.getHeight());
            try {
                Zombie zombie = zombieFactory.createZombie(name, SUMMON_X, lane);
                board.getTileAt(new Position(board.getWidth(), lane)).addZombie(zombie);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private boolean isSafeFrozenZombie(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("default")
                || normalized.equals("cone head")
                || normalized.equals("bucket head")
                || normalized.equals("brick head")
                || normalized.equals("knight")
                || normalized.equals("imp")
                || normalized.equals("dodo")
                || normalized.equals("hunter")
                || normalized.equals("troglobite");
    }

    private boolean isSafeSummon(String name) {
        if (name == null || name.isBlank()) return false;
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return !normalized.contains("king")
                && !normalized.contains("boss") && !normalized.contains("zomboss");
    }

    private boolean isFrostbiteBoss() {
        return boss.getChapterName().equals("ice-cave")
                || boss.getId().equals("zomboss-frostbite");
    }

    private boolean isEgyptBoss() { return boss.getId().equals("zomboss-egypt"); }
    private boolean isDarkBoss() { return boss.getId().equals("zomboss-dark"); }
    private boolean isBeachBoss() { return boss.getId().equals("zomboss-beach"); }
    private int nextActionDelay() {
        if (isDarkBoss() || isBeachBoss()) {
            return 60 + random.nextInt(31);
        }
        return ACTION_MIN_GAP_TICKS + random.nextInt(ACTION_GAP_SPREAD_TICKS);
    }

    private void attachHitboxes() {
        hitboxes.clear();
        hitboxes.add(new BossHitbox(boss, boss.getX(), boss.getFirstLane()));
        hitboxes.add(new BossHitbox(boss, boss.getX(), boss.getSecondLane()));
        for (BossHitbox hitbox : hitboxes) addHitboxToTile(hitbox);
    }

    private void moveBossX(double x) { boss.setX(x); relocateHitboxes(); }
    private void restoreBossOrigin() { if (Math.abs(boss.getX() - BOSS_X) > 0.0001) moveBossX(BOSS_X); }

    private void relocateHitboxes() {
        if (board == null || hitboxes.size() < 2) return;
        for (BossHitbox hitbox : hitboxes) {
            Tile source = board.getTileContainingZombie(hitbox);
            if (source != null) source.removeZombie(hitbox);
        }
        hitboxes.get(0).moveTo(boss.getX(), boss.getFirstLane());
        hitboxes.get(1).moveTo(boss.getX(), boss.getSecondLane());
        for (BossHitbox hitbox : hitboxes) addHitboxToTile(hitbox);
    }

    private void addHitboxToTile(BossHitbox hitbox) {
        int tileX = Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(hitbox.getX())));
        int lane = Math.max(1, Math.min(board.getHeight(), (int) Math.round(hitbox.getY())));
        board.getTileAt(new Position(tileX, lane)).addZombie(hitbox);
    }

    private void cancelTransientAction() {
        restoreTurbineVictims();
        restoreBossOrigin();
        clearActionState();
        moveDirection = 0;
    }

    private void restoreTurbineVictims() {
        for (TurbineVictim victim : turbineVictims) {
            victim.restore();
        }
    }

    private void clearActionState() {
        actionStartTick = currentTick;
        actionStageStartTick = currentTick;
        actionStage = 0;
        actionTarget = null;
        actionTargets.clear();
        summonTargets.clear();
        summonNames.clear();
        turbineVictims.clear();
        missileLaunchTick = Integer.MAX_VALUE;
        missileImpactTick = Integer.MAX_VALUE;
        missileResolved = false;
        chargeImpactTick = Integer.MAX_VALUE;
        chargeReturnTick = Integer.MAX_VALUE;
        chargeImpactResolved = false;
        frostMissileLaunchTick = Integer.MAX_VALUE;
        frostMissileImpactTick = Integer.MAX_VALUE;
        frostMissileResolved = false;
        frostWindLanes.clear();
        frostWindImpactTick = Integer.MAX_VALUE;
        frostWindResolved = false;
        frostFreezeColumn = 0;
        frostFreezeResolveTick = Integer.MAX_VALUE;
        frostFreezeResolved = false;
        frostVisualVariant = 1;

        specialImpactTick = Integer.MAX_VALUE;
        specialResolveTick = Integer.MAX_VALUE;
        specialImpactResolved = false;
        specialResolveResolved = false;
        pendingFirstLane = -1;
    }

    private void finishDefeat() {
        restoreBossOrigin();
        for (BossHitbox hitbox : hitboxes) {
            Tile tile = board.getTileContainingZombie(hitbox);
            if (tile != null) tile.removeZombie(hitbox);
            hitbox.deactivate();
        }
        boss.setState(BossState.DEFEATED, BossAction.NONE);
    }

    private float fraction(int elapsed, int duration) {
        return duration <= 0 ? 1f : Math.max(0f, Math.min(1f, elapsed / (float) duration));
    }
    private double lerp(double from, double to, float alpha) { return from + (to - from) * alpha; }

    private static final class TurbineVictim {
        private final Plant plant;
        private final Zombie zombie;
        private final double startX;
        private final double startY;

        private TurbineVictim(Plant plant) {
            this.plant = plant;
            this.zombie = null;
            this.startX = plant.getX();
            this.startY = plant.getY();
        }

        private TurbineVictim(Zombie zombie) {
            this.plant = null;
            this.zombie = zombie;
            this.startX = zombie.getX();
            this.startY = zombie.getY();
        }

        private boolean isAlive() {
            return plant != null ? plant.isAlive() : zombie != null && zombie.isAlive();
        }

        private boolean isTangleKelp() {
            return plant != null && plant.isAlive() && plant.getName().equalsIgnoreCase("Tangle Kelp");
        }

        private void moveTo(double x, double y) {
            if (!isAlive()) {
                return;
            }
            if (plant != null) {
                plant.moveTo(x, y);
            } else {
                zombie.moveTo(x, y);
            }
        }

        private void restore() {
            moveTo(startX, startY);
        }

        private void kill() {
            if (plant != null && plant.isAlive()) {
                plant.kill();
            } else if (zombie != null && zombie.isAlive()) {
                zombie.kill();
            }
        }

        private Position position() {
            double x = plant != null ? plant.getX() : zombie.getX();
            double y = plant != null ? plant.getY() : zombie.getY();
            return new Position((int) Math.round(x), (int) Math.round(y));
        }
    }

    public Boss getBoss() { return boss; }
    public boolean isStarted() { return started; }
    public boolean isDefeated() { return boss.getState() == BossState.DEFEATED; }
    public int getObservedSectionBreakSerial() { return observedSectionBreakSerial; }
    public int getMoveDirection() { return moveDirection; }
    public int getMoveStartFirstLane() { return moveStartFirstLane; }
    public int getPendingFirstLane() { return pendingFirstLane; }
    public List<BossHitbox> getHitboxes() { return Collections.unmodifiableList(hitboxes); }
    public int getCurrentTick() { return currentTick; }
    public int getActionStartTick() { return actionStartTick; }
    public int getActionStageStartTick() { return actionStageStartTick; }
    public int getActionStage() { return actionStage; }
    public Position getActionTarget() { return actionTarget; }
    public List<Position> getActionTargets() { return Collections.unmodifiableList(actionTargets); }
    public int getMissileLaunchTick() { return missileLaunchTick; }
    public int getMissileImpactTick() { return missileImpactTick; }
    public int getFrostMissileLaunchTick() { return frostMissileLaunchTick; }
    public int getFrostMissileImpactTick() { return frostMissileImpactTick; }
    public List<Integer> getFrostWindLanes() {
        return Collections.unmodifiableList(new ArrayList<>(frostWindLanes));
    }
    public int getFrostFreezeColumn() { return frostFreezeColumn; }
    public int getFrostVisualVariant() { return frostVisualVariant; }
    public Map<Position, String> getPendingFrozenZombieSpawns() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(frozenZombieSpawns));

    public int getSpecialImpactTick() { return specialImpactTick; }
    public int getSpecialResolveTick() { return specialResolveTick; }
    public Map<Position, Integer> getBurningTiles() { return Collections.unmodifiableMap(burningTiles); }
    public List<Position> getSummonTargets() { return Collections.unmodifiableList(summonTargets); }
    public List<Position> getSharkPositions() { return Collections.unmodifiableList(sharkPositions); }
    public int getArrivalScorchUntilTick() { return arrivalScorchUntilTick; }
    public int getStateUntilTick() { return stateUntilTick; }
    public boolean isBeachTangleStun() { return beachTangleStun; }
    public double getSummonFocusLane() {
        if (summonTargets.isEmpty()) {
            return boss.getCenterLane();
        }
        double total = 0;
        for (Position target : summonTargets) {
            total += target.getY();
        }
        return total / summonTargets.size();
    }
    public List<Position> getTurbineVictimPositions() {
        List<Position> result = new ArrayList<>();
        for (TurbineVictim victim : turbineVictims) {
            if (victim.isAlive()) {
                result.add(victim.position());
            }
        }
        return Collections.unmodifiableList(result);
    }
    public boolean isPlantingBlocked(Position position) {
        Integer until = position == null ? null : burningTiles.get(position);
        return until != null && currentTick < until;
    }
}
