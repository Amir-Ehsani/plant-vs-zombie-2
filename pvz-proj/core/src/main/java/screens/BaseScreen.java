package screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz.Main;
import ui.NotificationManager;
import ui.UiMotion;

public abstract class BaseScreen implements Screen {
    public static final float WORLD_WIDTH = 1280f;
    public static final float WORLD_HEIGHT = 720f;
    private static final float FADE_IN_SECONDS = 0.18f;
    private static final float FADE_OUT_SECONDS = 0.14f;

    protected final Main game;
    protected final Stage stage;
    protected final NotificationManager notificationManager;
    private boolean transitioning;

    protected BaseScreen(Main game) {
        this.game = game;
        stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
        notificationManager = new NotificationManager(stage, game.getSkin());
        transitioning = false;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        notificationManager.activate();
        UiMotion.enhanceTree(stage.getRoot());
        stage.getRoot().clearActions();
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(FADE_IN_SECONDS));
        transitioning = false;
    }

    public boolean transitionOut(Runnable afterTransition) {
        if (transitioning) {
            return false;
        }
        transitioning = true;
        stage.getRoot().clearActions();
        stage.getRoot().addAction(Actions.sequence(
                Actions.fadeOut(FADE_OUT_SECONDS),
                Actions.run(() -> {
                    if (afterTransition != null) {
                        afterTransition.run();
                    }
                })
        ));
        return true;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.06f, 0.09f, 0.08f, 1f);
        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        notificationManager.deactivate();
    }

    @Override
    public void dispose() {
        notificationManager.deactivate();
        stage.dispose();
    }
}
