package ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PamAnimationActor extends Actor {
    private final PvzAnimationService service;
    private final String pamPath;
    private final Matrix4 originalTransform;
    private final Matrix4 workingTransform;
    private final Vector2 stagePosition;
    private ClipRef clip;
    private Rectangle bounds;
    private float stateTime;
    private boolean loading;
    private boolean failed;
    private final List<String> armorVisibilityTokens;
    private Map<String, Boolean> visibilityOverrides;

    public PamAnimationActor(PvzAnimationService service, String pamPath) {
        this.service = service;
        this.pamPath = pamPath;
        originalTransform = new Matrix4();
        workingTransform = new Matrix4();
        stagePosition = new Vector2();
        bounds = new Rectangle();
        stateTime = 0f;
        loading = false;
        failed = false;
        armorVisibilityTokens = new ArrayList<>();
        visibilityOverrides = Collections.emptyMap();
        setSize(120f, 120f);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (clip != null) {
            stateTime += Math.max(0f, delta);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (clip == null) {
            startLoadingIfNeeded();
            return;
        }
        if (bounds.width <= 0f || bounds.height <= 0f) {
            return;
        }
        drawClip(batch);
    }

    public boolean isReady() {
        return clip != null;
    }

    public boolean hasFailed() {
        return failed;
    }

    public void restart() {
        stateTime = 0f;
    }

    public void setArmorVisibilityTokens(String... tokens) {
        armorVisibilityTokens.clear();
        if (tokens == null) {
            return;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank()) {
                armorVisibilityTokens.add(token);
            }
        }
    }

    private void startLoadingIfNeeded() {
        if (loading || failed || !isOnScreen() || !canLoad()) {
            return;
        }
        loading = true;
        try {
            service.getPamPlayer().loadAsync(pamPath, this::finishLoading);
        } catch (RuntimeException exception) {
            failed = true;
            loading = false;
        }
    }

    private void finishLoading() {
        try {
            PamPlayer player = service.getPamPlayer();
            String clipName = chooseClip(player.clips(pamPath));
            clip = player.getClip(pamPath, clipName);
            bounds = player.bounds(pamPath, clipName);
            visibilityOverrides = service.armorVisibility(pamPath, armorVisibilityTokens);
            failed = clip == null;
        } catch (RuntimeException exception) {
            clip = null;
            failed = true;
        }
        loading = false;
    }

    private String chooseClip(List<String> clips) {
        if (clips == null || clips.isEmpty()) {
            throw new IllegalStateException("Animation has no clips.");
        }
        String exactIdle = findClip(clips, "idle", 0);
        if (exactIdle != null) {
            return exactIdle;
        }
        String idle = findClip(clips, "idle", 1);
        if (idle != null) {
            return idle;
        }
        String loop = findClip(clips, "loop", 2);
        if (loop != null) {
            return loop;
        }
        String walk = findClip(clips, "walk", 2);
        if (walk != null) {
            return walk;
        }
        for (String value : clips) {
            String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
            if (!normalized.contains("attack") && !normalized.contains("plantfood")
                    && !normalized.contains("explosion") && !normalized.contains("special")) {
                return value;
            }
        }
        return clips.get(0);
    }

    private String findClip(List<String> clips, String token, int mode) {
        for (String value : clips) {
            String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
            if (matches(normalized, token, mode)) {
                return value;
            }
        }
        return null;
    }

    private boolean matches(String value, String token, int mode) {
        if (mode == 0) {
            return value.equals(token);
        }
        if (mode == 1) {
            return value.startsWith(token);
        }
        return value.contains(token);
    }

    private void drawClip(Batch batch) {
        originalTransform.set(batch.getTransformMatrix());
        workingTransform.set(originalTransform);
        float scale = calculateScale();
        workingTransform.translate(getX() + getWidth() / 2f, getY() + getHeight() / 2f, 0f);
        workingTransform.scale(scale, scale, 1f);
        batch.setTransformMatrix(workingTransform);
        float drawX = -(bounds.x + bounds.width / 2f);
        float drawY = -(bounds.y + bounds.height / 2f);
        service.getPamPlayer().draw(
                batch, clip, stateTime, drawX, drawY, true,
                visibilityOverrides == null || visibilityOverrides.isEmpty() ? null : visibilityOverrides
        );
        batch.setTransformMatrix(originalTransform);
    }

    private float calculateScale() {
        float widthScale = getWidth() / bounds.width;
        float heightScale = getHeight() / bounds.height;
        return Math.max(0.01f, Math.min(widthScale, heightScale) * 0.9f);
    }

    private boolean canLoad() {
        return service != null && service.isAvailable() && service.getPamPlayer() != null
                && pamPath != null && !pamPath.isBlank();
    }

    private boolean isOnScreen() {
        if (getStage() == null) {
            return false;
        }
        stagePosition.set(0f, 0f);
        localToStageCoordinates(stagePosition);
        return stagePosition.x + getWidth() >= 0f
                && stagePosition.x <= getStage().getWidth()
                && stagePosition.y + getHeight() >= 0f
                && stagePosition.y <= getStage().getHeight();
    }
}
