package game.hud;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import game.animation.core.PvzAnimationService;
import models.core.zombie.Zombie;
import models.engine.session.GameSession;
import models.engine.session.GameState;
import models.level.wave.Wave;
import models.level.wave.WaveManager;
import models.minigame.ZombotanyGame;

public final class WaveProgressHud {
    private static final String METER_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER";
    private static final String FILL_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FILL";
    private static final String FLAG_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FLAG_DEFAULT";
    private static final String FLAG_POLE_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FLAG_POLE";
    private static final String ZOMBIE_HEAD_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_ZOMBIEHEAD";

    private static final float TOP_MARGIN = 12f;
    private static final float ZOMBOTANY_TOP_MARGIN = 72f;
    private static final float TRACK_LEFT_INSET = 10f;
    private static final float TRACK_RIGHT_INSET = 10f;
    private static final float FLAG_X_OFFSET = 1f;
    private static final float FLAG_Y_OFFSET = 7f;
    private static final float FLAG_POLE_X_OFFSET = -5f;
    private static final float FLAG_POLE_Y_OFFSET = -3f;
    private static final float HEAD_OVERLAP = 1f;

    private final TextureRegion meter;
    private final TextureRegion fill;
    private final TextureRegion flag;
    private final TextureRegion flagPole;
    private final TextureRegion zombieHead;
    private float displayedProgress;

    public WaveProgressHud(PvzAnimationService animations) {
        meter = region(animations, METER_ID);
        fill = region(animations, FILL_ID);
        flag = region(animations, FLAG_ID);
        flagPole = region(animations, FLAG_POLE_ID);
        zombieHead = region(animations, ZOMBIE_HEAD_ID);
        displayedProgress = 0f;
    }

    public void update(GameSession session) {
        float progress = calculateProgress(session);
        displayedProgress = Math.max(displayedProgress, progress);
        if (isVictory(session)) {
            displayedProgress = 1f;
        }
    }

    public void updateZombotany(ZombotanyGame game) {
        if (game == null) {
            return;
        }
        displayedProgress = Math.max(displayedProgress, game.getWaveProgress());
        if (game.isWon()) {
            displayedProgress = 1f;
        }
    }

    public void renderZombotany(Batch batch, ZombotanyGame game, float worldWidth, float worldHeight) {
        if (batch == null || game == null || meter == null || fill == null
                || flag == null || flagPole == null || zombieHead == null) {
            return;
        }
        float meterWidth = meter.getRegionWidth();
        float meterHeight = meter.getRegionHeight();
        float hudX = (worldWidth - meterWidth) * 0.5f;
        float hudY = worldHeight - meterHeight - ZOMBOTANY_TOP_MARGIN;
        float trackLeft = hudX + TRACK_LEFT_INSET;
        float trackRight = hudX + meterWidth - TRACK_RIGHT_INSET;
        float trackWidth = Math.max(1f, trackRight - trackLeft);
        float fillLeft = trackRight - trackWidth * MathUtils.clamp(displayedProgress, 0f, 1f);

        batch.draw(fill, fillLeft, fillY(hudY, meterHeight), trackRight - fillLeft, fill.getRegionHeight());
        batch.draw(meter, hudX, hudY);
        drawWaveFlags(batch, game.getTotalWaves(), trackLeft, trackWidth, hudY, meterHeight);
        drawZombieHead(batch, fillLeft, hudY, meterHeight);
    }

    public void render(Batch batch, GameSession session, float worldWidth, float worldHeight) {
        if (!isDrawable(batch, session)) {
            return;
        }
        WaveManager waveManager = session.getCurrentLevel().getWaveManager();
        float meterWidth = meter.getRegionWidth();
        float meterHeight = meter.getRegionHeight();
        float hudX = (worldWidth - meterWidth) * 0.5f;
        float hudY = worldHeight - meterHeight - TOP_MARGIN;
        float trackLeft = hudX + TRACK_LEFT_INSET;
        float trackRight = hudX + meterWidth - TRACK_RIGHT_INSET;
        float trackWidth = Math.max(1f, trackRight - trackLeft);
        float fillLeft = trackRight - trackWidth * MathUtils.clamp(displayedProgress, 0f, 1f);

        batch.draw(fill, fillLeft, fillY(hudY, meterHeight), trackRight - fillLeft, fill.getRegionHeight());
        batch.draw(meter, hudX, hudY);
        drawWaveFlags(batch, waveManager.getTotalWaves(), trackLeft, trackWidth, hudY, meterHeight);
        drawZombieHead(batch, fillLeft, hudY, meterHeight);
    }

    private void drawWaveFlags(
            Batch batch,
            int totalWaves,
            float trackLeft,
            float trackWidth,
            float hudY,
            float meterHeight
    ) {
        for (int waveNumber = 1; waveNumber <= totalWaves; waveNumber++) {
            float fraction = waveNumber / (float) totalWaves;
            float markerX = trackLeft + trackWidth * (1f - fraction);
            batch.draw(
                    flagPole,
                    markerX + FLAG_POLE_X_OFFSET,
                    hudY + FLAG_POLE_Y_OFFSET
            );
            batch.draw(
                    flag,
                    markerX + FLAG_X_OFFSET,
                    hudY + (meterHeight - flag.getRegionHeight()) * 0.5f + FLAG_Y_OFFSET
            );
        }
    }

    private void drawZombieHead(Batch batch, float fillLeft, float hudY, float meterHeight) {
        float headX = fillLeft - zombieHead.getRegionWidth() + HEAD_OVERLAP;
        float headY = hudY + (meterHeight - zombieHead.getRegionHeight()) * 0.5f;
        batch.draw(zombieHead, headX, headY);
    }

    private float fillY(float hudY, float meterHeight) {
        return hudY + (meterHeight - fill.getRegionHeight()) * 0.5f;
    }

    private float calculateProgress(GameSession session) {
        if (session == null || session.getCurrentLevel() == null) {
            return 0f;
        }
        WaveManager waveManager = session.getCurrentLevel().getWaveManager();
        if (waveManager == null || waveManager.getTotalWaves() <= 0) {
            return 0f;
        }
        if (isVictory(session)) {
            return 1f;
        }
        Wave currentWave = waveManager.getCurrentWave();
        if (currentWave == null) {
            return 0f;
        }
        float completedWaves = currentWave.getWaveNumber() - 1f;
        float currentWaveProgress = healthLossFraction(currentWave);
        return MathUtils.clamp(
                (completedWaves + currentWaveProgress) / waveManager.getTotalWaves(),
                0f,
                1f
        );
    }

    private float healthLossFraction(Wave wave) {
        int initialHealth = wave.getInitialTotalHealth();
        if (initialHealth <= 0) {
            return wave.isSpawned() ? 1f : 0f;
        }
        int currentHealth = 0;
        for (Zombie zombie : wave.getZombiesList()) {
            if (zombie != null && zombie.isAlive()) {
                currentHealth += Math.max(0, zombie.getHp());
            }
        }
        return MathUtils.clamp(1f - currentHealth / (float) initialHealth, 0f, 1f);
    }

    private boolean isDrawable(Batch batch, GameSession session) {
        return batch != null
                && session != null
                && session.getCurrentLevel() != null
                && session.getCurrentLevel().getWaveManager() != null
                && session.getCurrentLevel().getWaveManager().getTotalWaves() > 0
                && meter != null
                && fill != null
                && flag != null
                && flagPole != null
                && zombieHead != null;
    }

    private boolean isVictory(GameSession session) {
        return session != null
                && session.getState() != null
                && session.getState().getStatus() == GameState.Status.WON;
    }

    private TextureRegion region(PvzAnimationService animations, String resourceId) {
        return animations == null ? null : animations.region(resourceId);
    }
}
