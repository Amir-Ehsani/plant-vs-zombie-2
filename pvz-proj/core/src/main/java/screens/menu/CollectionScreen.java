package screens.menu;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pvz.Main;
import controllers.features.CollectionController;
import ui.BackButton;
import ui.MenuButton;
import ui.PvzAnimationService;

public class CollectionScreen extends BaseMenuScreen {
    private final CollectionController controller;
    private final CollectionPlantPanel plantPanel;
    private final CollectionZombiePanel zombiePanel;
    private final Container<Actor> contentContainer;
    private boolean plantsVisible;

    public CollectionScreen(Main game) {
        super(game);
        controller = game.getCollectionController();
        PvzAnimationService animations = game.getAnimationService();
        plantPanel = new CollectionPlantPanel(
                skin,
                controller,
                game.getAuthController(),
                animations,
                this::showControllerMessage,
                this::refreshResourceBar
        );
        zombiePanel = new CollectionZombiePanel(skin, game.getAuthController(), animations);
        contentContainer = new Container<>();
        plantsVisible = true;
        buildUi();
        showPlants();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshResourceBar();
        plantPanel.refresh();
        zombiePanel.refresh();
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        addResourceBar(root);
        Table panel = createPanel();
        panel.pad(18f);
        panel.add(createTitle("Collection")).colspan(3).padBottom(8f).row();
        panel.add(new MenuButton("Plants", skin, "green", this::showPlants))
                .width(180f).height(44f).padRight(8f);
        panel.add(new MenuButton("Zombies", skin, "purple", this::showZombies))
                .width(180f).height(44f).padRight(8f);
        panel.add(new BackButton(skin, game.getScreenManager()::showMainMenu))
                .width(180f).height(44f).row();
        panel.add(contentContainer).colspan(3).width(1130f).height(495f).padTop(8f);
        root.add(panel).width(1180f).height(590f);
    }

    private void showPlants() {
        if (!plantsVisible) {
            plantPanel.refresh();
        }
        plantsVisible = true;
        contentContainer.setActor(plantPanel);
    }

    private void showZombies() {
        if (plantsVisible) {
            zombiePanel.refresh();
        }
        plantsVisible = false;
        contentContainer.setActor(zombiePanel);
    }
}
