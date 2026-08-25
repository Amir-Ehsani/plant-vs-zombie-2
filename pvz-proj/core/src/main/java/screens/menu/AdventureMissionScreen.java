package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.pvz.Main;
import controllers.core.GameController;
import models.account.User;
import ui.AdventureMissionCatalog;
import ui.BackButton;
import ui.MenuButton;

public class AdventureMissionScreen extends BaseMenuScreen {
    private static final Color TEXT_COLOR = Color.valueOf("4A3A1F");
    private final String chapterName;
    private final int levelNumber;

    public AdventureMissionScreen(Main game, String chapterName, int levelNumber) {
        super(game);
        this.chapterName = chapterName;
        this.levelNumber = levelNumber;
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        requireLoggedIn();
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        Table panel = createPanel();
        Label screenTitle = createTitle(AdventureMissionCatalog.title(chapterName, levelNumber));
        screenTitle.setColor(Color.WHITE);
        panel.add(screenTitle).padBottom(14f).row();
        Label missionTitle = new Label("Mission", skin, "medium_outline");
        missionTitle.setColor(Color.WHITE);
        panel.add(missionTitle).padBottom(8f).row();
        Label mission = new Label(AdventureMissionCatalog.mission(chapterName, levelNumber), skin, "medium");
        mission.setColor(TEXT_COLOR);
        mission.setWrap(true);
        mission.setAlignment(Align.center);
        panel.add(mission).width(650f).height(100f).padBottom(16f).row();
        Table actions = new Table();
        actions.add(new BackButton(skin, game.getScreenManager()::showAdventure))
                .width(180f).height(48f).padRight(10f);
        actions.add(new MenuButton("Continue", skin, "green", this::continueToLevel))
                .width(200f).height(48f);
        panel.add(actions);
        root.add(panel).width(760f).height(360f);
    }

    private void continueToLevel() {
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            game.getScreenManager().showLogin("ERROR: Please login first.");
            return;
        }
        user.setCurrentChapterName(chapterName);
        user.setCurrentChapterLevel(levelNumber);
        game.getAuthController().saveUsers();
        GameController controller = game.getGameController();
        controller.prepareChapterLevel(chapterName, levelNumber);
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            return;
        }
        if (controller.shouldAutoStartCurrentLevel()) {
            controller.startGame();
            if (!controller.wasSuccessful()) {
                showControllerMessage(controller.getLastMessage());
                return;
            }
            game.getScreenManager().showPreparedGame();
            return;
        }
        game.getScreenManager().showPlantSelection(chapterName, levelNumber);
    }
}
