package boss.core;

import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.board.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class BossRuntime {
    private static final int ACTION_MIN_GAP_TICKS = 45;
    private static final int ACTION_GAP_SPREAD_TICKS = 31;
    private static final int COMMON_ACTION_TICKS = 16;
    private static final double BOSS_X = 8.65;
    private static final double SUMMON_X = 8.95;

    private final Boss boss;
    private final Random random;
    private final ZombieFactory zombieFactory;
    private final List<BossHitbox> hitboxes;
    private final List<String> summonPool;

    private Board board;
    private boolean started;
    private int stateUntilTick;
    private int nextActionTick;
    private int observedSectionBreakSerial;
    private boolean deathStarted;
    private int moveDirection;

    public BossRuntime(Boss boss, long seed) {
        if (boss == null) {
            throw new IllegalArgumentException("Boss runtime requires a boss.");
        }
        this.boss = boss;
        random = new Random(seed);
        zombieFactory = new ZombieFactory();
        hitboxes = new ArrayList<>();
        summonPool = new ArrayList<>();
        started = false;
        stateUntilTick = 0;
        nextActionTick = Integer.MAX_VALUE;
        observedSectionBreakSerial = 0;
        deathStarted = false;
        moveDirection = 0;
    }

    public void start(Board board, List<String> allowedZombieNames, int currentTick) {
        if (started) {
            return;
        }
        if (board == null) {
            throw new IllegalArgumentException("Boss runtime requires a board.");
        }
        this.board = board;
        summonPool.clear();
        if (allowedZombieNames != null) {
            for (String name : allowedZombieNames) {
                if (isSafeSummon(name)) {
                    summonPool.add(name.trim());
                }
            }
        }
        boss.setFirstLane(Math.min(2, Math.max(1, board.getHeight() - 1)));
        attachHitboxes();
        boss.setState(BossState.INTRO, BossAction.NONE);
        stateUntilTick = currentTick + boss.getIntroTicks();
        nextActionTick = stateUntilTick + nextActionDelay();
        observedSectionBreakSerial = boss.getHealth().getSectionBreakSerial();
        started = true;
    }

    public void update(int currentTick) {
        if (!started || boss.getState() == BossState.DEFEATED) {
            return;
        }
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
        if (boss.getState() == BossState.INTRO || boss.getState() == BossState.ACTION) {
            if (currentTick >= stateUntilTick) {
                activate(currentTick);
            }
            return;
        }
        if (boss.getState() == BossState.ACTIVE && currentTick >= nextActionTick) {
            startRandomCommonAction(currentTick);
        }
    }

    private void observeHealth(int currentTick) {
        if (boss.getHealth().isDepleted()) {
            if (!deathStarted) {
                deathStarted = true;
                boss.setState(BossState.DYING, BossAction.NONE);
                stateUntilTick = currentTick + boss.getDeathTicks();
            }
            return;
        }
        int serial = boss.getHealth().getSectionBreakSerial();
        if (serial > observedSectionBreakSerial) {
            observedSectionBreakSerial = serial;
            boss.setState(BossState.STUNNED, BossAction.NONE);
            stateUntilTick = currentTick + boss.getStunTicks();
        }
    }

    private void activate(int currentTick) {
        boss.setState(BossState.ACTIVE, BossAction.NONE);
        nextActionTick = currentTick + nextActionDelay();
        moveDirection = 0;
    }

    private void startRandomCommonAction(int currentTick) {
        List<BossAction> choices = new ArrayList<>();
        if (boss.isLaneChangesAllowed()) {
            choices.add(BossAction.MOVE_LANES);
        }
        if (boss.isZombieSpawnsAllowed() && !summonPool.isEmpty()) {
            choices.add(BossAction.SPAWN_ZOMBIES);
        }
        if (choices.isEmpty()) {
            nextActionTick = currentTick + nextActionDelay();
            return;
        }
        BossAction action = choices.get(random.nextInt(choices.size()));
        if (action == BossAction.MOVE_LANES) {
            moveToAnotherLanePair();
        } else if (action == BossAction.SPAWN_ZOMBIES) {
            summonZombies();
        }
        boss.setState(BossState.ACTION, action);
        stateUntilTick = currentTick + COMMON_ACTION_TICKS;
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

    private int nextActionDelay() {
        return ACTION_MIN_GAP_TICKS + random.nextInt(ACTION_GAP_SPREAD_TICKS);
    }

    private void attachHitboxes() {
        hitboxes.clear();
        hitboxes.add(new BossHitbox(boss, BOSS_X, boss.getFirstLane()));
        hitboxes.add(new BossHitbox(boss, BOSS_X, boss.getSecondLane()));
        for (BossHitbox hitbox : hitboxes) {
            addHitboxToTile(hitbox);
        }
    }

    private void relocateHitboxes() {
        for (BossHitbox hitbox : hitboxes) {
            Tile source = board.getTileContainingZombie(hitbox);
            if (source != null) {
                source.removeZombie(hitbox);
            }
        }
        hitboxes.get(0).moveTo(BOSS_X, boss.getFirstLane());
        hitboxes.get(1).moveTo(BOSS_X, boss.getSecondLane());
        for (BossHitbox hitbox : hitboxes) {
            addHitboxToTile(hitbox);
        }
    }

    private void addHitboxToTile(BossHitbox hitbox) {
        int tileX = Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(hitbox.getX())));
        int lane = Math.max(1, Math.min(board.getHeight(), (int) Math.round(hitbox.getY())));
        board.getTileAt(new Position(tileX, lane)).addZombie(hitbox);
    }

    private void finishDefeat() {
        for (BossHitbox hitbox : hitboxes) {
            Tile tile = board.getTileContainingZombie(hitbox);
            if (tile != null) {
                tile.removeZombie(hitbox);
            }
            hitbox.deactivate();
        }
        boss.setState(BossState.DEFEATED, BossAction.NONE);
    }

    public Boss getBoss() { return boss; }
    public boolean isStarted() { return started; }
    public boolean isDefeated() { return boss.getState() == BossState.DEFEATED; }
    public int getObservedSectionBreakSerial() { return observedSectionBreakSerial; }
    public int getMoveDirection() { return moveDirection; }
    public List<BossHitbox> getHitboxes() { return Collections.unmodifiableList(hitboxes); }
}
