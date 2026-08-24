package models.engine.sun;

import models.engine.board.Position;

public class Sun {
    public static final int DEFAULT_GROUND_LIFETIME_TICKS = 100;

    private final Position position;
    private final boolean producedByPlant;
    private SunType type;
    private int sunAmount;
    private int fallingTicksRemaining;
    private int groundTicksRemaining;
    private boolean falling;
    private boolean expired;

    public Sun(Position position, int sunAmount, int timeLeft) {
        this(
            position,
            resolveTypeFromAmount(sunAmount),
            sunAmount,
            timeLeft != Integer.MAX_VALUE && timeLeft > 0,
            timeLeft == Integer.MAX_VALUE ? 0 : Math.max(0, timeLeft),
            timeLeft == Integer.MAX_VALUE
        );
    }

    public Sun(
        Position position,
        SunType type,
        int sunAmount,
        boolean falling,
        int fallingTicksRemaining,
        boolean producedByPlant
    ) {
        if (position == null) {
            throw new IllegalArgumentException("Sun position cannot be null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Sun type cannot be null.");
        }
        if (sunAmount < 0) {
            throw new IllegalArgumentException("Sun amount cannot be negative.");
        }
        if (fallingTicksRemaining < 0) {
            throw new IllegalArgumentException("Falling ticks cannot be negative.");
        }

        this.position = position;
        this.type = type;
        this.sunAmount = sunAmount;
        this.falling = falling;
        this.fallingTicksRemaining = falling ? fallingTicksRemaining : 0;
        this.producedByPlant = producedByPlant;
        groundTicksRemaining = producedByPlant ? Integer.MAX_VALUE : DEFAULT_GROUND_LIFETIME_TICKS;
        expired = false;

        if (this.falling && this.fallingTicksRemaining == 0) {
            land();
        }
    }

    public static Sun skySun(Position position, SunType type, int fallingTicks) {
        return new Sun(
            position,
            type,
            type.getCollectionAmount(),
            true,
            fallingTicks,
            false
        );
    }

    public static Sun plantSun(Position position, int amount) {
        return new Sun(position, SunType.NORMAL, amount, false, 0, true);
    }

    public Position getPosition() {
        return position;
    }

    public SunType getType() {
        return type;
    }

    public int getSunAmount() {
        return sunAmount;
    }

    public int getTimeLeft() {
        if (falling) {
            return fallingTicksRemaining;
        }
        return producedByPlant ? Integer.MAX_VALUE : groundTicksRemaining;
    }

    public int getFallingTicksRemaining() {
        return fallingTicksRemaining;
    }

    public int getGroundTicksRemaining() {
        return groundTicksRemaining;
    }

    public boolean isFalling() {
        return falling;
    }

    public boolean isOnGround() {
        return !falling && !producedByPlant && !expired;
    }

    public boolean isProducedByPlant() {
        return producedByPlant;
    }

    public boolean isRadioactiveAndFalling() {
        return type == SunType.RADIOACTIVE && falling;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean tick() {
        if (expired) {
            return false;
        }
        if (falling) {
            return tickFalling();
        }
        tickGroundLifetime();
        return false;
    }

    private boolean tickFalling() {
        if (fallingTicksRemaining > 0) {
            fallingTicksRemaining--;
        }
        if (fallingTicksRemaining != 0) {
            return false;
        }
        land();
        return true;
    }

    private void tickGroundLifetime() {
        if (producedByPlant || groundTicksRemaining == Integer.MAX_VALUE) {
            return;
        }
        if (groundTicksRemaining > 0) {
            groundTicksRemaining--;
        }
        if (groundTicksRemaining == 0) {
            expired = true;
        }
    }

    private void land() {
        falling = false;
        fallingTicksRemaining = 0;
        groundTicksRemaining = DEFAULT_GROUND_LIFETIME_TICKS;
        if (type == SunType.RADIOACTIVE) {
            type = SunType.NORMAL;
            sunAmount = SunType.NORMAL.getCollectionAmount();
        }
    }

    private static SunType resolveTypeFromAmount(int amount) {
        if (amount == SunType.SPECIAL.getCollectionAmount()) {
            return SunType.SPECIAL;
        }
        return SunType.NORMAL;
    }
}
