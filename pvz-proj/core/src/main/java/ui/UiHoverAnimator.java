package ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

/** Adds the same lightweight hover/press motion to clickable UI actors that are not MenuButtons. */
public final class UiHoverAnimator {
    private static final float HOVER_SCALE = 1.035f;
    private static final float PRESS_SCALE = 0.97f;
    private static final float MOTION_SECONDS = 0.07f;

    private UiHoverAnimator() {
    }

    public static void attach(Stage stage) {
        if (stage == null) {
            return;
        }
        attachRecursively(stage.getRoot());
    }

    private static void attachRecursively(Actor actor) {
        if (actor == null) {
            return;
        }
        if (isEligible(actor) && !hasHoverListener(actor)) {
            actor.addListener(new HoverListener(actor));
        }
        if (actor instanceof Group group) {
            for (Actor child : group.getChildren()) {
                attachRecursively(child);
            }
        }
    }

    private static boolean isEligible(Actor actor) {
        if (!actor.isVisible() || actor.getTouchable() == Touchable.disabled) {
            return false;
        }
        if (actor instanceof MenuButton || actor instanceof ModalWindow
                || actor instanceof ScrollPane || actor instanceof TextField
                || actor instanceof SelectBox || actor instanceof List) {
            return false;
        }
        if (actor instanceof Button) {
            return true;
        }
        for (EventListener listener : actor.getListeners()) {
            if (listener instanceof ClickListener) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasHoverListener(Actor actor) {
        for (EventListener listener : actor.getListeners()) {
            if (listener instanceof HoverListener) {
                return true;
            }
        }
        return false;
    }

    private static final class HoverListener extends ClickListener {
        private final Actor actor;
        private final float baseScaleX;
        private final float baseScaleY;
        private ScaleToAction currentAction;

        private HoverListener(Actor actor) {
            this.actor = actor;
            baseScaleX = actor.getScaleX();
            baseScaleY = actor.getScaleY();
            if (actor instanceof Group group) {
                group.setTransform(true);
            }
            actor.setOrigin(Align.center);
        }

        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            super.enter(event, x, y, pointer, fromActor);
            if (pointer == -1 && isEnabled()) {
                animate(HOVER_SCALE);
            }
        }

        @Override
        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            super.exit(event, x, y, pointer, toActor);
            if (pointer == -1) {
                animate(1f);
            }
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            boolean handled = super.touchDown(event, x, y, pointer, button);
            if (handled && isEnabled()) {
                animate(PRESS_SCALE);
            }
            return handled;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            super.touchUp(event, x, y, pointer, button);
            if (isEnabled()) {
                animate(isOver() ? HOVER_SCALE : 1f);
            }
        }

        private boolean isEnabled() {
            return actor.isVisible() && actor.getTouchable() != Touchable.disabled
                    && (!(actor instanceof Button button) || !button.isDisabled());
        }

        private void animate(float factor) {
            if (currentAction != null) {
                actor.removeAction(currentAction);
            }
            actor.setOrigin(Align.center);
            currentAction = Actions.scaleTo(
                    baseScaleX * factor,
                    baseScaleY * factor,
                    MOTION_SECONDS,
                    Interpolation.sineOut
            );
            actor.addAction(currentAction);
        }
    }
}
