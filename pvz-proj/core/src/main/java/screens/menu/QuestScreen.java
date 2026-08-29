package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.pvz.Main;
import controllers.features.TravelLogController;
import models.account.Quest;
import ui.BackButton;
import ui.MenuButton;
import ui.QuestCard;

import java.util.List;

public class QuestScreen extends BaseMenuScreen {
    private static final Color TEXT_COLOR = Color.valueOf("4A3A1F");
    private final TravelLogController controller;
    private final Table questList;
    private final Table filters;
    private String pageName;

    public QuestScreen(Main game) {
        super(game);
        controller = game.getTravelLogController();
        questList = new Table();
        filters = new Table();
        pageName = "all";
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshQuests();
        refreshResourceBar();
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        addResourceBar(root);
        Table panel = createPanel();
        Label screenTitle = createTitle("Quests");
        screenTitle.setColor(Color.WHITE);
        panel.add(screenTitle).padBottom(8f).row();
        panel.add(filters).padBottom(8f).row();
        rebuildFilters();
        ScrollPane scrollPane = new ScrollPane(questList, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);
        panel.add(scrollPane).width(780f).height(455f).row();
        Table actions = new Table();
        actions.add(new MenuButton("Minigames", skin, "green", game.getScreenManager()::showMiniGames))
                .width(180f).height(44f).padRight(8f);
        actions.add(new MenuButton("Collect All", skin, "green_small", this::collectAll))
                .width(180f).height(44f).padRight(8f);
        actions.add(new BackButton(skin, game.getScreenManager()::showMainMenu))
                .width(180f).height(44f);
        panel.add(actions).padTop(10f);
        root.add(panel).width(880f).height(660f);
    }

    private void rebuildFilters() {
        filters.clearChildren();
        addFilter("All", "all");
        addFilter("Adventure", "adventure");
        addFilter("Special", "special");
        addFilter("Daily", "challenges");
        addFilter("Minigames", "minigames");
    }

    private void addFilter(String title, String page) {
        filters.add(new MenuButton(title, skin, pageName.equals(page) ? "green_small" : "brown", () -> switchPage(page)))
                .width(128f).height(36f).padRight(5f);
    }

    private void switchPage(String page) {
        pageName = page;
        rebuildFilters();
        refreshQuests();
    }

    private void refreshQuests() {
        questList.clearChildren();
        questList.top();
        List<Quest> quests = controller.showPage(pageName);
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            return;
        }
        int index = 1;
        for (Quest quest : quests) {
            int questNumber = index;
            questList.add(new QuestCard(skin, quest, () -> collectReward(questNumber)))
                    .width(680f).padBottom(9f).row();
            index++;
        }
        if (quests.isEmpty()) {
            Label empty = new Label("No quests on this page.", skin, "secondary");
            empty.setColor(TEXT_COLOR);
            questList.add(empty).pad(20f);
        }
        refreshResourceBar();
    }

    private void collectReward(int questNumber) {
        controller.collectQuestReward(pageName, questNumber);
        showControllerMessage(controller.getLastMessage());
        refreshQuests();
    }

    private void collectAll() {
        controller.collectAllDoneRewards(pageName);
        showControllerMessage(controller.getLastMessage());
        refreshQuests();
    }
}
