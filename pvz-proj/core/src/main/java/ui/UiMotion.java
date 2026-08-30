package ui;

import audio.AudioCue;
import audio.AudioManager;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class UiMotion {
    private static final float HOVER_SCALE = 1.035f;
    private static final float PRESS_SCALE = 0.965f;
    private static final float ANIMATION_SECONDS = 0.08f;

    private UiMotion() {
    }

    public static void enhanceButton(Button button) {
        if (button == null || hasMotionListener(button)) {
            return;
        }
        button.setTransform(true);
        button.addListener(new MotionListener(button));
    }

    private static boolean hasMotionListener(Button button) {
        for (com.badlogic.gdx.scenes.scene2d.EventListener listener : button.getListeners()) {
            if (listener instanceof MotionListener) {
                return true;
            }
        }
        return false;
    }

    private static void animate(Actor actor, float scale) {
        actor.setOrigin(actor.getWidth() / 2f, actor.getHeight() / 2f);
        actor.addAction(Actions.scaleTo(scale, scale, ANIMATION_SECONDS, Interpolation.sineOut));
    }

    private static final class MotionListener extends ClickListener {
        private final Button button;

        private MotionListener(Button button) {
            this.button = button;
        }

        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (pointer == -1 && !button.isDisabled()) {
                animate(button, HOVER_SCALE);
            }
        }

        @Override
        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            if (!button.isDisabled()) {
                animate(button, 1f);
            }
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int buttonCode) {
            if (button.isDisabled()) {
                return false;
            }
            animate(button, PRESS_SCALE);
            AudioManager.playGlobal(AudioCue.BUTTON);
            return super.touchDown(event, x, y, pointer, buttonCode);
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int buttonCode) {
            super.touchUp(event, x, y, pointer, buttonCode);
            if (!button.isDisabled()) {
                animate(button, isOver() ? HOVER_SCALE : 1f);
            }
        }
    }
}
