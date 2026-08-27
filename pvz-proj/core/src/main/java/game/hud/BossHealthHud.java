package game.hud;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import boss.core.Boss;
import boss.core.BossRuntime;
import game.animation.core.PvzAnimationService;

public final class BossHealthHud {
    private static final String METER_ID = "IMAGE_UI_HUD_INGAME_ZOMBOSS_PROGRESS_METER";
    private static final String FILL_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_ZOMBOSS_FILL";
    private static final String HEAD_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_ZOMBOSS_HEAD";
    private static final String NOTCH_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_ZOMBOSS_NOTCH";
    private static final String SKULL_ID = "IMAGE_UI_HUD_INGAME_ZOMBOSS_HEALTH_METER_SKULL_ICON";
    private static final float TOP_MARGIN = 12f;
    private static final float TRACK_LEFT_INSET = 31f;
    private static final float TRACK_RIGHT_INSET = 21f;

    private final BossRuntime runtime;
    private final TextureRegion meter;
    private final TextureRegion fill;
    private final TextureRegion head;
    private final TextureRegion notch;
    private final TextureRegion skull;

    public BossHealthHud(PvzAnimationService animations, BossRuntime runtime) {
        if (animations == null || runtime == null) {
            throw new IllegalArgumentException("Boss health HUD requires animations and runtime.");
        }
        this.runtime = runtime;
        meter = animations.region(METER_ID);
        fill = animations.region(FILL_ID);
        head = animations.region(HEAD_ID);
        notch = animations.region(NOTCH_ID);
        skull = animations.region(SKULL_ID);
    }

    public void render(Batch batch, float worldWidth, float worldHeight) {
        if (!drawable(batch)) {
            return;
        }
        Boss boss = runtime.getBoss();
        float meterWidth = meter.getRegionWidth();
        float meterHeight = meter.getRegionHeight();
        float x = (worldWidth - meterWidth) * 0.5f;
        float y = worldHeight - meterHeight - TOP_MARGIN;
        float trackLeft = x + TRACK_LEFT_INSET;
        float trackRight = x + meterWidth - TRACK_RIGHT_INSET;
        float trackWidth = Math.max(1f, trackRight - trackLeft);
        float fraction = MathUtils.clamp(boss.getHealth().getHealthFraction(), 0f, 1f);
        float fillWidth = trackWidth * fraction;
        float fillY = y + (meterHeight - fill.getRegionHeight()) * 0.5f;

        batch.draw(fill, trackLeft, fillY, fillWidth, fill.getRegionHeight());
        batch.draw(meter, x, y);
        int[] sections = boss.getHealth().getSectionMaximums();
        float total = Math.max(1f, boss.getHealth().getMaximumHp());
        float afterFirst = (sections[1] + sections[2]) / total;
        float afterSecond = sections[2] / total;
        drawNotch(batch, trackLeft + trackWidth * afterFirst, y, meterHeight);
        drawNotch(batch, trackLeft + trackWidth * afterSecond, y, meterHeight);
        batch.draw(head, x - head.getRegionWidth() * 0.45f,
                y + (meterHeight - head.getRegionHeight()) * 0.5f);
        batch.draw(skull, trackRight - skull.getRegionWidth() * 0.5f,
                y + (meterHeight - skull.getRegionHeight()) * 0.5f);
    }

    private void drawNotch(Batch batch, float x, float meterY, float meterHeight) {
        batch.draw(notch, x - notch.getRegionWidth() * 0.5f,
                meterY + (meterHeight - notch.getRegionHeight()) * 0.5f);
    }

    private boolean drawable(Batch batch) {
        return batch != null && runtime.isStarted()
                && meter != null && fill != null && head != null && notch != null && skull != null;
    }
}
