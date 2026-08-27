package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.pvz.Main;
import controllers.features.TravelLogController;
import ui.BackButton;
import ui.MenuButton;

public class MiniGameHubScreen extends BaseMenuScreen {
    private static final Color TEXT_COLOR = Color.valueOf("4A3A1F");
    private final TravelLogController controller;
    private final Table gameList;

    public MiniGameHubScreen(Main game) {
        super(game);
        controller = game.getTravelLogController();
        gameList = new Table();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshGames();
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        Table panel = createPanel();
        Label screenTitle = createTitle("Minigames");
        screenTitle.setColor(Color.WHITE);
        panel.add(screenTitle).padBottom(12f).row();
        panel.add(gameList).row();
        panel.add(new BackButton(skin, game.getScreenManager()::showQuests))
                .width(180f).height(44f).padTop(12f);
        root.add(panel).width(900f).height(540f);
    }

    private void refreshGames() {
        gameList.clearChildren();
        gameList.defaults().pad(8f);
        for (TravelLogController.MiniGameInfo gameInfo : controller.getMiniGames()) {
            gameList.add(createMiniGameCard(gameInfo)).width(250f).height(330f);
        }
    }

    private Table createMiniGameCard(TravelLogController.MiniGameInfo gameInfo) {
        Table card = createPanel();
        card.pad(14f);
        Label name = new Label(gameInfo.getDisplayName(), skin, "medium_outline");
        name.setColor(Color.WHITE);
        name.setAlignment(Align.center);
        card.add(name).width(210f).padBottom(10f).row();
        for (TravelLogController.MiniGameStageInfo stageInfo : gameInfo.getStages()) {
            String status = stageInfo.isCompleted() ? "Completed" : stageInfo.isUnlocked() ? "Unlocked" : "Locked";
            Label stageLabel = panelLabel("Stage " + stageInfo.getStage() + " - " + status);
            stageLabel.setAlignment(Align.center);
            card.add(stageLabel).width(210f).padTop(4f).row();
            MenuButton button = new MenuButton(
                    "Open Stage " + stageInfo.getStage(),
                    skin,
                    stageInfo.isUnlocked() ? "green_small" : "brown",
                    stageInfo.isUnlocked() ? () -> openMiniGame(gameInfo.getName(), stageInfo.getStage()) : null
            );
            button.setDisabled(!stageInfo.isUnlocked());
            card.add(button).width(175f).height(34f).padTop(3f).row();
        }
        return card;
    }

    private void openMiniGame(String gameName, int stage) {
        controller.enterMiniGame(gameName, stage);
        showControllerMessage(controller.getLastMessage());
        if (!controller.wasSuccessful()) {
            return;
        }
        game.getScreenManager().showActiveMiniGame();
    }

    private Label panelLabel(String text) {
        Label label = new Label(text == null ? "" : text, skin, "secondary");
        label.setColor(TEXT_COLOR);
        return label;
    }
}
