package screens.menu;

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pvz.Main;
import controllers.features.LeaderboardController;
import models.account.User;
import ui.BackButton;
import ui.MenuButton;

import java.util.List;

public class LeaderboardScreen extends BaseMenuScreen {
    private final LeaderboardController controller;
    private Table rowsTable;
    private String sortColumn;
    private boolean ascending;

    public LeaderboardScreen(Main game) {
        super(game);
        controller = game.getLeaderboardController();
        sortColumn = "best-score";
        ascending = false;
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshRows();
        refreshResourceBar();
    }

    private void buildUi() {
        Table root = createRoot();
        addResourceBar(root);
        Table panel = createPanel();
        panel.add(createTitle("Leaderboard")).padBottom(14f).row();
        rowsTable = new Table();
        ScrollPane scrollPane = new ScrollPane(rowsTable, skin);
        scrollPane.setFadeScrollBars(false);
        panel.add(scrollPane).width(1080f).height(500f).row();
        panel.add(new BackButton(skin, game.getScreenManager()::showMainMenu))
                .width(220f).height(48f).padTop(12f);
        root.add(panel).expand().center();
    }

    private void refreshRows() {
        rowsTable.clearChildren();
        addHeaders();
        List<User> users = controller.getRankedUsers(sortColumn, ascending);
        int rank = 1;
        for (User user : users) {
            addUserRow(rank, user);
            rank++;
        }
    }

    private void addHeaders() {
        rowsTable.add(createLabel("#")).width(45f);
        addHeader("Username", "username", 180f);
        addHeader("Progress", "progress", 220f);
        addHeader("Minigames", "minigames", 130f);
        addHeader("Daily Quests", "daily-quests", 145f);
        addHeader("Other Quests", "quests", 145f);
        addHeader("Best MioPoint", "best-score", 150f);
        rowsTable.row();
    }

    private void addHeader(String text, String column, float width) {
        String marker = sortColumn.equals(column) ? ascending ? " ^" : " v" : "";
        rowsTable.add(new MenuButton(text + marker, skin, "brown", () -> sortBy(column))).width(width).height(44f);
    }

    private void addUserRow(int rank, User user) {
        rowsTable.add(createLabel(String.valueOf(rank))).width(45f).padTop(8f);
        rowsTable.add(createLabel(user.getUsername())).width(180f).left().padTop(8f);
        rowsTable.add(createLabel(controller.getLastProgress(user))).width(220f).left().padTop(8f);
        rowsTable.add(createLabel(String.valueOf(controller.getCompletedMiniGameCount(user))))
                .width(130f).padTop(8f);
        rowsTable.add(createLabel(String.valueOf(controller.getDailyQuestCount(user))))
                .width(145f).padTop(8f);
        rowsTable.add(createLabel(String.valueOf(controller.getNonDailyQuestCount(user))))
                .width(145f).padTop(8f);
        rowsTable.add(createLabel(String.valueOf(user.getBestMioPoint()))).width(150f).padTop(8f).row();
    }

    private void sortBy(String column) {
        if (sortColumn.equals(column)) {
            ascending = !ascending;
        } else {
            sortColumn = column;
            ascending = true;
        }
        refreshRows();
    }
}
