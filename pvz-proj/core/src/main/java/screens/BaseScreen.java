package screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz.Main;
import ui.NotificationManager;

public abstract class BaseScreen implements Screen {
    public static final float WORLD_WIDTH = 1280f;
    public static final float WORLD_HEIGHT = 720f;
    private static final float FADE_IN_SECONDS = 0.14f;

    protected final Main game;
    protected final Stage stage;
    protected final NotificationManager notificationManager;

    protected BaseScreen(Main game) {
        this.game = game;
        stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
        notificationManager = new NotificationManager(stage, game.getSkin());
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        notificationManager.activate();
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().clearActions();
        stage.getRoot().addAction(Actions.fadeIn(FADE_IN_SECONDS));
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
