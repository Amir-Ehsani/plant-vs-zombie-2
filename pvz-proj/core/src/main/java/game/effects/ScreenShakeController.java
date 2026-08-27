package game.effects;

import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ScreenShakeController {
    private static final float X_FREQUENCY = 73f;
    private static final float Y_FREQUENCY = 91f;
    private static final float Y_STRENGTH = 0.62f;

    private final List<ScheduledShake> scheduled = new ArrayList<>();
    private float amplitude;
    private float duration;
    private float remaining;
    private float elapsed;
    private float offsetX;
    private float offsetY;

    public void trigger(float strength, float seconds) {
        float safeStrength = Math.max(0f, strength);
        float safeDuration = Math.max(0f, seconds);
        if (safeStrength <= 0f || safeDuration <= 0f) {
            return;
        }
        amplitude = Math.max(amplitude, safeStrength);
        duration = Math.max(duration, safeDuration);
        remaining = Math.max(remaining, safeDuration);
        elapsed = 0f;
    }

    public void schedule(float delay, float strength, float seconds) {
        if (strength <= 0f || seconds <= 0f) {
            return;
        }
        if (delay <= 0f) {
            trigger(strength, seconds);
            return;
        }
        scheduled.add(new ScheduledShake(delay, strength, seconds));
    }

    public void update(float delta) {
        if (delta <= 0f) {
            return;
        }
        updateScheduled(delta);
        updateActive(delta);
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void clear() {
        scheduled.clear();
        amplitude = 0f;
        duration = 0f;
        remaining = 0f;
        elapsed = 0f;
        offsetX = 0f;
        offsetY = 0f;
    }

    private void updateScheduled(float delta) {
        Iterator<ScheduledShake> iterator = scheduled.iterator();
        while (iterator.hasNext()) {
            ScheduledShake shake = iterator.next();
            shake.delay -= delta;
            if (shake.delay > 0f) {
                continue;
            }
            trigger(shake.strength, shake.duration);
            iterator.remove();
        }
    }

    private void updateActive(float delta) {
        if (remaining <= 0f || duration <= 0f) {
            offsetX = 0f;
            offsetY = 0f;
            return;
        }
        remaining = Math.max(0f, remaining - delta);
        elapsed += delta;
        float envelope = MathUtils.clamp(remaining / duration, 0f, 1f);
        offsetX = MathUtils.sin(elapsed * X_FREQUENCY) * amplitude * envelope;
        offsetY = MathUtils.cos(elapsed * Y_FREQUENCY) * amplitude * Y_STRENGTH * envelope;
        if (remaining <= 0f) {
            amplitude = 0f;
            duration = 0f;
            offsetX = 0f;
            offsetY = 0f;
        }
    }

    private static final class ScheduledShake {
        private float delay;
        private final float strength;
        private final float duration;

        private ScheduledShake(float delay, float strength, float duration) {
            this.delay = delay;
            this.strength = strength;
            this.duration = duration;
        }
    }
}
