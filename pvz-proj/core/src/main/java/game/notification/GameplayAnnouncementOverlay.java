package game.notification;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayDeque;
import java.util.Queue;

public final class GameplayAnnouncementOverlay extends Label {
    private static final float DISPLAY_SECONDS = 1.85f;
    private static final float FADE_SECONDS = 0.20f;
    private static final float HEIGHT = 100f;

    private final Stage stage;
    private final Queue<String> queue = new ArrayDeque<>();
    private float remaining;

    public GameplayAnnouncementOverlay(Stage stage, Skin skin) {
        super("", skin, "big_outline");
        if (stage == null || skin == null) {
            throw new IllegalArgumentException("Announcement overlay requires a stage and skin.");
        }
        this.stage = stage;
        setTouchable(Touchable.disabled);
        setAlignment(Align.center);
        setWrap(true);
        setColor(Color.valueOf("D8281C"));
        setVisible(false);
        updateBounds();
        stage.addActor(this);
    }

    public void push(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String clean = message.trim();
        if (isVisible() && clean.equals(getText().toString())) {
            return;
        }
        if (queue.contains(clean)) {
            return;
        }
        queue.add(clean);
        if (!isVisible()) {
            showNext();
        }
        toFront();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        updateBounds();
        if (!isVisible()) {
            if (!queue.isEmpty()) {
                showNext();
            }
            return;
        }
        remaining -= Math.max(0f, delta);
        float elapsed = DISPLAY_SECONDS - remaining;
        float alpha = 1f;
        if (elapsed < FADE_SECONDS) {
            alpha = elapsed / FADE_SECONDS;
        } else if (remaining < FADE_SECONDS) {
            alpha = remaining / FADE_SECONDS;
        }
        Color color = getColor();
        setColor(color.r, color.g, color.b, Math.max(0f, Math.min(1f, alpha)));
        if (remaining <= 0f) {
            setVisible(false);
            setText("");
            showNext();
        }
    }

    private void updateBounds() {
        float width = stage.getViewport().getWorldWidth();
        float height = stage.getViewport().getWorldHeight();
        setBounds(0f, (height - HEIGHT) * 0.5f, width, HEIGHT);
    }

    private void showNext() {
        String next = queue.poll();
        if (next == null) {
            setVisible(false);
            return;
        }
        setText(next);
        Color color = getColor();
        setColor(color.r, color.g, color.b, 0f);
        setVisible(true);
        remaining = DISPLAY_SECONDS;
        updateBounds();
        toFront();
    }
}
