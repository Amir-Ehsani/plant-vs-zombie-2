package models.engine.sun;

import models.engine.board.Position;

public class Sun {
    private final Position position;
    private SunType type;
    private int sunAmount;
    private int fallingTicksRemaining;
    private boolean falling;
    private final boolean producedByPlant;

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
        return falling ? fallingTicksRemaining : Integer.MAX_VALUE;
    }

    public int getFallingTicksRemaining() {
        return fallingTicksRemaining;
    }

    public boolean isFalling() {
        return falling;
    }

    public boolean isOnGround() {
        return !falling && !producedByPlant;
    }

    public boolean isProducedByPlant() {
        return producedByPlant;
    }

    public boolean isRadioactiveAndFalling() {
        return type == SunType.RADIOACTIVE && falling;
    }

    public boolean isExpired() {
        return false;
    }

    public boolean tick() {
        if (!falling) {
            return false;
        }

        if (fallingTicksRemaining > 0) {
            fallingTicksRemaining--;
        }

        if (fallingTicksRemaining == 0) {
            land();
            return true;
        }

        return false;
    }

    private void land() {
        falling = false;
        fallingTicksRemaining = 0;

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
