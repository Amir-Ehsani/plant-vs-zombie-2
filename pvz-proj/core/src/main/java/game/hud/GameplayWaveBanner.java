package game.hud;

import audio.AudioCue;
import audio.AudioManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import game.animation.core.PvzAnimationService;
import models.engine.session.GameSession;
import models.level.wave.Wave;
import models.level.wave.WaveManager;

public final class GameplayWaveBanner {
    private static final String NOTIFICATION_ICON_ID = "IMAGE_UI_HUD_INGAME_NOTIFICATION_ICON";
    private static final String ALERT_RING_ID = "IMAGE_UI_HUD_INGAME_ALERT_RING";
    private static final String WAVE_FLAG_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FLAG_DEFAULT";
    private static final String ZOMBIE_HEAD_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_ZOMBIEHEAD";

    private static final float READY_DURATION = 0.75f;
    private static final float SET_DURATION = 0.75f;
    private static final float PLANT_DURATION = 0.90f;
    private static final float HUGE_WAVE_DURATION = 2.50f;
    private static final int HUGE_WAVE_EARLY_TICKS = 25;
    private static final float ICON_GAP = 16f;

    private final TextureRegion notificationIcon;
    private final TextureRegion alertRing;
    private final TextureRegion waveFlag;
    private final TextureRegion zombieHead;
    private final BitmapFont font;
    private final GlyphLayout layout;

    private float introElapsed;
    private float hugeWaveRemaining;
    private boolean hugeWaveShown;

    public GameplayWaveBanner(PvzAnimationService animations, Skin skin) {
        notificationIcon = region(animations, NOTIFICATION_ICON_ID);
        alertRing = region(animations, ALERT_RING_ID);
        waveFlag = region(animations, WAVE_FLAG_ID);
        zombieHead = region(animations, ZOMBIE_HEAD_ID);
        font = resolveFont(skin);
        layout = new GlyphLayout();
        introElapsed = introDuration();
        hugeWaveRemaining = 0f;
        hugeWaveShown = false;
    }

    public void update(float delta, GameSession session) {
        float safeDelta = Math.max(0f, delta);
        if (introElapsed < introDuration()) {
            introElapsed = Math.min(introDuration(), introElapsed + safeDelta);
            return;
        }
        if (hugeWaveRemaining > 0f) {
            hugeWaveRemaining = Math.max(0f, hugeWaveRemaining - safeDelta);
            return;
        }
        if (!hugeWaveShown && shouldWarnAboutFinalWave(session)) {
            hugeWaveShown = true;
            hugeWaveRemaining = HUGE_WAVE_DURATION;
            AudioManager.playGlobal(AudioCue.ZOMBIES_COMING);
        }
    }

    public void render(Batch batch, float worldWidth, float worldHeight, float stateTime) {
        if (batch == null || font == null) {
            return;
        }
        String message = currentMessage();
        if (message == null) {
            return;
        }
        Color previousColor = new Color(font.getColor());
        font.setColor(isHugeWaveVisible() ? Color.SCARLET : Color.WHITE);
        layout.setText(font, message);
        float centerX = worldWidth * 0.5f;
        float centerY = worldHeight * 0.55f;
        float textX = centerX - layout.width * 0.5f;
        float textY = centerY + layout.height * 0.5f;
        drawBannerArt(batch, textX, centerY, stateTime);
        font.draw(batch, layout, textX, textY);
        font.setColor(previousColor);
    }

    private void drawBannerArt(Batch batch, float textX, float centerY, float stateTime) {
        if (isHugeWaveVisible()) {
            drawHugeWaveArt(batch, textX, centerY, stateTime);
            return;
        }
        if (notificationIcon == null) {
            return;
        }
        float x = textX - notificationIcon.getRegionWidth() - ICON_GAP;
        float y = centerY - notificationIcon.getRegionHeight() * 0.5f;
        batch.draw(notificationIcon, x, y);
    }

    private void drawHugeWaveArt(Batch batch, float textX, float centerY, float stateTime) {
        float iconCenterX = textX - 78f;
        if (alertRing != null) {
            float pulse = 0.88f + 0.08f * MathUtils.sin(stateTime * 8f);
            float width = alertRing.getRegionWidth() * pulse;
            float height = alertRing.getRegionHeight() * pulse;
            batch.draw(
                    alertRing,
                    iconCenterX - width * 0.5f,
                    centerY - height * 0.5f,
                    width,
                    height
            );
        }
        drawHugeWaveIcons(batch, iconCenterX, centerY);
    }

    private void drawHugeWaveIcons(Batch batch, float iconCenterX, float centerY) {
        if (waveFlag != null) {
            batch.draw(
                    waveFlag,
                    iconCenterX - waveFlag.getRegionWidth(),
                    centerY - waveFlag.getRegionHeight() * 0.5f
            );
        }
        if (zombieHead != null) {
            batch.draw(
                    zombieHead,
                    iconCenterX - zombieHead.getRegionWidth() * 0.15f,
                    centerY - zombieHead.getRegionHeight() * 0.5f
            );
        }
    }

    private String currentMessage() {
        if (introElapsed < READY_DURATION) {
            return "Ready...";
        }
        if (introElapsed < READY_DURATION + SET_DURATION) {
            return "Set...";
        }
        if (introElapsed < introDuration()) {
            return "PLANT!";
        }
        if (isHugeWaveVisible()) {
            return "A HUGE WAVE OF ZOMBIES IS APPROACHING!";
        }
        return null;
    }

    private boolean shouldWarnAboutFinalWave(GameSession session) {
        if (session == null || session.getCurrentLevel() == null || session.getTickManager() == null) {
            return false;
        }
        WaveManager waveManager = session.getCurrentLevel().getWaveManager();
        if (waveManager == null || waveManager.getTotalWaves() <= 0) {
            return false;
        }
        Wave nextWave = waveManager.getNextWave();
        if (nextWave == null || nextWave.getWaveNumber() != waveManager.getTotalWaves()) {
            return waveManager.getCurrentWaveNumber() == waveManager.getTotalWaves();
        }
        Wave currentWave = waveManager.getCurrentWave();
        if (currentWave != null) {
            return currentWave.hasLostSeventyFivePercentHealth();
        }
        int warningTick = Math.max(0, nextWave.getDelay() - HUGE_WAVE_EARLY_TICKS);
        return session.getTickManager().getCurrentTick() >= warningTick;
    }

    private boolean isHugeWaveVisible() {
        return hugeWaveRemaining > 0f;
    }

    private float introDuration() {
        return READY_DURATION + SET_DURATION + PLANT_DURATION;
    }

    private BitmapFont resolveFont(Skin skin) {
        if (skin == null) {
            return null;
        }
        try {
            return skin.get("big_outline", Label.LabelStyle.class).font;
        } catch (RuntimeException ignored) {
            return skin.getFont("default-font");
        }
    }

    private TextureRegion region(PvzAnimationService animations, String resourceId) {
        return animations == null ? null : animations.region(resourceId);
    }
}
