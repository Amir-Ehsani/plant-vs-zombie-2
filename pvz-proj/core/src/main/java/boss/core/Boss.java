package boss.core;

public final class Boss {
    private final String id;
    private final String displayName;
    private final String chapterName;
    private final String animationPath;
    private final float renderScale;
    private final BossHealth health;
    private final boolean laneChangesAllowed;
    private final boolean zombieSpawnsAllowed;
    private final String introClip;
    private final String idleClip;
    private final String moveUpClip;
    private final String moveDownClip;
    private final String spawnClip;
    private final String stunClip;
    private final String deathClip;
    private final int introTicks;
    private final int stunTicks;
    private final int deathTicks;

    private BossState state;
    private BossAction action;
    private int firstLane;
    private double x;
    private int visualRevision;

    public Boss(
            String id,
            String displayName,
            String chapterName,
            String animationPath,
            float renderScale,
            BossHealth health,
            boolean laneChangesAllowed,
            boolean zombieSpawnsAllowed,
            String introClip,
            String idleClip,
            String moveUpClip,
            String moveDownClip,
            String spawnClip,
            String stunClip,
            String deathClip,
            int introTicks,
            int stunTicks,
            int deathTicks
    ) {
        if (id == null || id.isBlank() || displayName == null || displayName.isBlank()
                || chapterName == null || chapterName.isBlank()
                || animationPath == null || animationPath.isBlank() || health == null) {
            throw new IllegalArgumentException("Boss identity, animation and health are required.");
        }
        this.id = id.trim();
        this.displayName = displayName.trim();
        this.chapterName = chapterName.trim();
        this.animationPath = animationPath.trim();
        this.renderScale = Math.max(0.1f, renderScale);
        this.health = health;
        this.laneChangesAllowed = laneChangesAllowed;
        this.zombieSpawnsAllowed = zombieSpawnsAllowed;
        this.introClip = cleanClip(introClip, "idle");
        this.idleClip = cleanClip(idleClip, "idle");
        this.moveUpClip = cleanClip(moveUpClip, this.idleClip);
        this.moveDownClip = cleanClip(moveDownClip, this.idleClip);
        this.spawnClip = cleanClip(spawnClip, this.idleClip);
        this.stunClip = cleanClip(stunClip, this.idleClip);
        this.deathClip = cleanClip(deathClip, this.idleClip);
        this.introTicks = Math.max(0, introTicks);
        this.stunTicks = Math.max(1, stunTicks);
        this.deathTicks = Math.max(1, deathTicks);
        state = BossState.INTRO;
        action = BossAction.NONE;
        firstLane = 2;
        x = 8.65;
        visualRevision = 0;
    }

    private String cleanClip(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    void setState(BossState nextState, BossAction nextAction) {
        BossState resolvedState = nextState == null ? BossState.ACTIVE : nextState;
        BossAction resolvedAction = nextAction == null ? BossAction.NONE : nextAction;
        if (state != resolvedState || action != resolvedAction) {
            state = resolvedState;
            action = resolvedAction;
            visualRevision++;
        }
    }

    void setFirstLane(int firstLane) {
        this.firstLane = Math.max(1, Math.min(4, firstLane));
    }

    void setX(double x) {
        this.x = x;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getChapterName() { return chapterName; }
    public String getAnimationPath() { return animationPath; }
    public float getRenderScale() { return renderScale; }
    public BossHealth getHealth() { return health; }
    public boolean isLaneChangesAllowed() { return laneChangesAllowed; }
    public boolean isZombieSpawnsAllowed() { return zombieSpawnsAllowed; }
    public BossState getState() { return state; }
    public BossAction getAction() { return action; }
    public int getFirstLane() { return firstLane; }
    public int getSecondLane() { return firstLane + 1; }
    public double getCenterLane() { return firstLane + 0.5; }
    public double getX() { return x; }
    public int getVisualRevision() { return visualRevision; }
    public int getIntroTicks() { return introTicks; }
    public int getStunTicks() { return stunTicks; }
    public int getDeathTicks() { return deathTicks; }

    public String getVisualClip() {
        return switch (state) {
            case INTRO -> introClip;
            case STUNNED -> stunClip;
            case DYING, DEFEATED -> deathClip;
            case ACTION -> switch (action) {
                case MOVE_LANES -> idleClip;
                case SPAWN_ZOMBIES -> spawnClip;
                case MISSILE -> "missile_start";
                case CHARGE -> "walk_forward";
                case ICE_MISSILE -> "slingshot";
                case ICE_WIND, FREEZE_COLUMN -> idleClip;
                default -> idleClip;
            };
            default -> idleClip;
        };
    }

    public String getMovementClip(boolean movingUp) {
        return movingUp ? moveUpClip : moveDownClip;
    }
}
