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
        contentContainer.fill();
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
        root.pad(10f);
        addResourceBar(root);
        Table panel = createPanel();
        panel.pad(22f);
        panel.defaults().pad(4f);
        panel.add(createTitle("Collection")).colspan(3).padTop(6f).padBottom(12f).row();
        panel.add(new MenuButton("Plants", skin, "green", this::showPlants))
                .width(170f).height(44f).left();
        panel.add(new MenuButton("Zombies", skin, "purple", this::showZombies))
                .width(170f).height(44f).center();
        panel.add(new BackButton(skin, game.getScreenManager()::showMainMenu))
                .width(170f).height(44f).right().row();
        panel.add(contentContainer).colspan(3).width(1140f).height(530f).padTop(10f);
        root.add(panel).width(1215f).height(635f);
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
