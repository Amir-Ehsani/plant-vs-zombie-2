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

    // The Egypt PAM exposes a 3.3333s missile_start and a 0.3s missile effect.
    private static final int EGYPT_MISSILE_AIM_TICKS = 33;
    private static final int EGYPT_MISSILE_FLIGHT_TICKS = 3;
    private static final int EGYPT_MISSILE_EXPLOSION_TICKS = 17;

    // walk_forward / walk_backwards are 1.2333s each in the Egypt Zomboss PAM.
    private static final int EGYPT_CHARGE_FORWARD_TICKS = 12;
    private static final int EGYPT_CHARGE_IMPACT_HOLD_TICKS = 3;
    private static final int EGYPT_CHARGE_BACKWARD_TICKS = 12;
    private static final double EGYPT_CHARGE_FRONT_X = 2.25;

    // Frostbite boss PAM timings: slingshot=3.5s, wind=2.8333s, glacier_column=6.3s.
    private static final int FROST_MISSILE_LAUNCH_TICKS = 29;
    private static final int FROST_MISSILE_IMPACT_TICKS = 30;
    private static final int FROST_MISSILE_END_TICKS = 37;
    private static final int FROST_WIND_IMPACT_TICKS = 8;
    private static final int FROST_WIND_ACTION_TICKS = 28;
    private static final int FROST_GLACIER_RESOLVE_TICKS = 31;
    private static final int FROST_GLACIER_ACTION_TICKS = 63;

    private final Boss boss;
    private final Random random;
    private final ZombieFactory zombieFactory;
    private final List<BossHitbox> hitboxes;
    private final List<String> summonPool;
    private final List<String> frozenZombiePool;
    private final Map<Position, String> frozenZombieSpawns;

    private Board board;
    private boolean started;
    private int stateUntilTick;
    private int nextActionTick;
    private int observedSectionBreakSerial;
    private boolean deathStarted;
    private int moveDirection;

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
        started = false;
        stateUntilTick = 0;
        nextActionTick = Integer.MAX_VALUE;
        observedSectionBreakSerial = 0;
        deathStarted = false;
        moveDirection = 0;
        currentTick = 0;
        clearActionState();
    }

    public void start(Board board, List<String> allowedZombieNames, int currentTick) {
        if (started) {
            return;
        }
        if (board == null) {
            throw new IllegalArgumentException("Boss runtime requires a board.");
        }
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
        boss.setState(BossState.INTRO, BossAction.NONE);
        stateUntilTick = currentTick + boss.getIntroTicks();
        nextActionTick = stateUntilTick + nextActionDelay();
        observedSectionBreakSerial = boss.getHealth().getSectionBreakSerial();
        started = true;
    }

    public void update(int currentTick) {
        this.currentTick = currentTick;
        if (!started || boss.getState() == BossState.DEFEATED) {
            return;
        }
        releaseThawedFrozenZombies();
        observeHealth(currentTick);
        if (deathStarted) {
            if (currentTick >= stateUntilTick) {
                finishDefeat();
            }
            return;
        }
        if (boss.getState() == BossState.STUNNED) {
            if (currentTick >= stateUntilTick) {
                activate(currentTick);
            }
            return;
        }
        if (boss.getState() == BossState.INTRO) {
            if (currentTick >= stateUntilTick) {
                activate(currentTick);
            }
            return;
        }
        if (boss.getState() == BossState.ACTION) {
            updateCurrentAction(currentTick);
            if (currentTick >= stateUntilTick) {
                activate(currentTick);
            }
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
            default -> nextActionTick = currentTick + nextActionDelay();
        }
    }

    private void startLaneMove(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        actionStageStartTick = currentTick;
        moveToAnotherLanePair();
        boss.setState(BossState.ACTION, BossAction.MOVE_LANES);
        stateUntilTick = currentTick + COMMON_ACTION_TICKS;
    }

    private void startZombieSpawn(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        actionStageStartTick = currentTick;
        summonZombies();
        boss.setState(BossState.ACTION, BossAction.SPAWN_ZOMBIES);
        stateUntilTick = currentTick + COMMON_ACTION_TICKS;
    }

    private void startEgyptMissile(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        actionStageStartTick = currentTick;
        actionStage = 0;
        actionTarget = chooseRandomBoardTile();
        missileLaunchTick = currentTick + EGYPT_MISSILE_AIM_TICKS;
        missileImpactTick = missileLaunchTick + EGYPT_MISSILE_FLIGHT_TICKS;
        stateUntilTick = missileImpactTick + EGYPT_MISSILE_EXPLOSION_TICKS;
        boss.setState(BossState.ACTION, BossAction.MISSILE);
    }

    private void startEgyptCharge(int currentTick) {
        clearActionState();
        actionStartTick = currentTick;
        actionStageStartTick = currentTick;
        actionStage = 0;
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
    }

    private void updateCurrentAction(int currentTick) {
        switch (boss.getAction()) {
            case MISSILE -> updateEgyptMissile(currentTick);
            case CHARGE -> updateEgyptCharge(currentTick);
            case ICE_MISSILE -> updateFrostMissile(currentTick);
            case ICE_WIND -> updateFrostWind(currentTick);
            case FREEZE_COLUMN -> updateFrostFreezeColumn(currentTick);
            default -> {
            }
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
            float progress = fraction(currentTick - actionStartTick, EGYPT_CHARGE_FORWARD_TICKS);
            moveBossX(lerp(BOSS_X, EGYPT_CHARGE_FRONT_X, progress));
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
            if (actionStage != 2) {
                actionStage = 2;
                actionStageStartTick = chargeReturnTick;
            }
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
            }
        }
        board.removeDeadEntities();
        createEgyptMissileGraves();
    }

    private void createEgyptMissileGraves() {
        int capacity = Math.min(2, GraveSpawnRules.remainingCapacity(board));
        if (capacity <= 0) {
            return;
        }
        List<Tile> candidates = new ArrayList<>();
        for (int lane = 1; lane <= board.getHeight(); lane++) {
            for (int x = 2; x < board.getWidth(); x++) {
                Tile tile = board.getTileAt(new Position(x, lane));
                if (tile.getTileType() == TileType.NORMAL
                        && !tile.hasPlant()
                        && !tile.hasZombies()) {
                    candidates.add(tile);
                }
            }
        }
        Collections.shuffle(candidates, random);
        for (int index = 0; index < Math.min(capacity, candidates.size()); index++) {
            candidates.get(index).setTileType(TileType.GRAVE);
        }
    }

    private void destroyPlantsInBossLanes() {
        if (board == null) {
            return;
        }
        destroyPlantsInLane(boss.getFirstLane());
        destroyPlantsInLane(boss.getSecondLane());
        board.removeDeadEntities();
    }

    private void destroyPlantsInLane(int laneNumber) {
        if (laneNumber < 1 || laneNumber > board.getHeight()) {
            return;
        }
        for (Tile tile : board.getLaneAt(laneNumber).getTiles()) {
            for (Plant plant : new ArrayList<>(tile.getPlants())) {
                if (plant != null && plant.isAlive()) {
                    plant.kill();
                }
            }
        }
    }

    private Position chooseRandomBoardTile() {
        if (board == null) {
            return null;
        }
        return new Position(
                random.nextInt(board.getWidth()) + 1,
                random.nextInt(board.getHeight()) + 1
        );
    }

    private void moveToAnotherLanePair() {
        if (board == null || board.getHeight() < 2) {
            return;
        }
        int oldFirstLane = boss.getFirstLane();
        int maxFirstLane = board.getHeight() - 1;
        int nextFirstLane = oldFirstLane;
        if (maxFirstLane > 1) {
            while (nextFirstLane == oldFirstLane) {
                nextFirstLane = random.nextInt(maxFirstLane) + 1;
            }
        }
        moveDirection = Integer.compare(nextFirstLane, oldFirstLane);
        boss.setFirstLane(nextFirstLane);
        relocateHitboxes();
    }

    private void summonZombies() {
        if (board == null || summonPool.isEmpty()) {
            return;
        }
        int count = Math.min(2, Math.max(1, summonPool.size()));
        for (int i = 0; i < count; i++) {
            String name = summonPool.get(random.nextInt(summonPool.size()));
            int lane = i == 0 ? boss.getFirstLane() : boss.getSecondLane();
            try {
                Zombie zombie = zombieFactory.createZombie(name, SUMMON_X, lane);
                int tileX = Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(SUMMON_X)));
                board.getTileAt(new Position(tileX, lane)).addZombie(zombie);
            } catch (IllegalArgumentException ignored) {
            }
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
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return !normalized.contains("gargantuar")
                && !normalized.contains("king")
                && !normalized.contains("boss")
                && !normalized.contains("zomboss");
    }

    private boolean isEgyptBoss() {
        return boss.getChapterName().equals("ancient-egypt")
                || boss.getId().equals("zomboss-egypt");
    }

    private boolean isFrostbiteBoss() {
        return boss.getChapterName().equals("ice-cave")
                || boss.getId().equals("zomboss-frostbite");
    }

    private int nextActionDelay() {
        return ACTION_MIN_GAP_TICKS + random.nextInt(ACTION_GAP_SPREAD_TICKS);
    }

    private void attachHitboxes() {
        hitboxes.clear();
        hitboxes.add(new BossHitbox(boss, boss.getX(), boss.getFirstLane()));
        hitboxes.add(new BossHitbox(boss, boss.getX(), boss.getSecondLane()));
        for (BossHitbox hitbox : hitboxes) {
            addHitboxToTile(hitbox);
        }
    }

    private void moveBossX(double x) {
        boss.setX(x);
        relocateHitboxes();
    }

    private void restoreBossOrigin() {
        if (Math.abs(boss.getX() - BOSS_X) > 0.0001) {
            moveBossX(BOSS_X);
        }
    }

    private void relocateHitboxes() {
        if (board == null || hitboxes.size() < 2) {
            return;
        }
        for (BossHitbox hitbox : hitboxes) {
            Tile source = board.getTileContainingZombie(hitbox);
            if (source != null) {
                source.removeZombie(hitbox);
            }
        }
        hitboxes.get(0).moveTo(boss.getX(), boss.getFirstLane());
        hitboxes.get(1).moveTo(boss.getX(), boss.getSecondLane());
        for (BossHitbox hitbox : hitboxes) {
            addHitboxToTile(hitbox);
        }
    }

    private void addHitboxToTile(BossHitbox hitbox) {
        int tileX = Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(hitbox.getX())));
        int lane = Math.max(1, Math.min(board.getHeight(), (int) Math.round(hitbox.getY())));
        board.getTileAt(new Position(tileX, lane)).addZombie(hitbox);
    }

    private void cancelTransientAction() {
        restoreBossOrigin();
        clearActionState();
        moveDirection = 0;
    }

    private void clearActionState() {
        actionStartTick = currentTick;
        actionStageStartTick = currentTick;
        actionStage = 0;
        actionTarget = null;
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
    }

    private void finishDefeat() {
        restoreBossOrigin();
        for (BossHitbox hitbox : hitboxes) {
            Tile tile = board.getTileContainingZombie(hitbox);
            if (tile != null) {
                tile.removeZombie(hitbox);
            }
            hitbox.deactivate();
        }
        boss.setState(BossState.DEFEATED, BossAction.NONE);
    }

    private float fraction(int elapsed, int duration) {
        if (duration <= 0) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, elapsed / (float) duration));
    }

    private double lerp(double from, double to, float alpha) {
        return from + (to - from) * alpha;
    }

    public Boss getBoss() { return boss; }
    public boolean isStarted() { return started; }
    public boolean isDefeated() { return boss.getState() == BossState.DEFEATED; }
    public int getObservedSectionBreakSerial() { return observedSectionBreakSerial; }
    public int getMoveDirection() { return moveDirection; }
    public List<BossHitbox> getHitboxes() { return Collections.unmodifiableList(hitboxes); }
    public int getCurrentTick() { return currentTick; }
    public int getActionStartTick() { return actionStartTick; }
    public int getActionStageStartTick() { return actionStageStartTick; }
    public int getActionStage() { return actionStage; }
    public Position getActionTarget() { return actionTarget; }
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
    }
}
