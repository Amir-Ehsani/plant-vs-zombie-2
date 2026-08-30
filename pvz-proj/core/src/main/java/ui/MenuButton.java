package ui;

import audio.AudioCue;
import audio.AudioManager;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

/** Shared menu button with small, non-blocking hover/press feedback. */
public class MenuButton extends TextButton {
    private static final float HOVER_SCALE = 1.035f;
    private static final float PRESS_SCALE = 0.97f;
    private static final float MOTION_SECONDS = 0.07f;

    public MenuButton(String text, Skin skin, Runnable action) {
        this(text, skin, "green", action);
    }

    public MenuButton(String text, Skin skin, String styleName, Runnable action) {
        super(text == null ? "" : text, skin, styleName == null ? "green" : styleName);
        setTransform(true);
        addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1 && !isDisabled()) {
                    animateScale(HOVER_SCALE, Interpolation.sineOut);
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                if (pointer == -1) {
                    animateScale(1f, Interpolation.sineOut);
                }
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                boolean handled = super.touchDown(event, x, y, pointer, button);
                if (handled && !isDisabled()) {
                    animateScale(PRESS_SCALE, Interpolation.sineOut);
                }
                return handled;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (!isDisabled()) {
                    animateScale(isOver() ? HOVER_SCALE : 1f, Interpolation.sineOut);
                }
                super.touchUp(event, x, y, pointer, button);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isDisabled()) {
                    return;
                }
                AudioManager.playGlobal(AudioCue.BUTTON);
                if (action != null) {
                    action.run();
                }
            }
        });
    }

    private void animateScale(float scale, Interpolation interpolation) {
        setOrigin(Align.center);
        clearActions();
        addAction(Actions.scaleTo(scale, scale, MOTION_SECONDS, interpolation));
    }
}
