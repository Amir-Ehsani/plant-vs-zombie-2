package screens.menu;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
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
        root.top();
        addResourceBar(root);
        Table panel = createPanel();
        panel.add(createTitle("Leaderboard")).padBottom(10f).row();
        rowsTable = new Table();
        rowsTable.top();
        ScrollPane scrollPane = new ScrollPane(rowsTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);
        panel.add(scrollPane).width(900f).height(315f).row();
        panel.add(new BackButton(skin, game.getScreenManager()::showMainMenu))
                .width(180f).height(42f).padTop(8f);
        root.add(panel).width(960f).height(490f).top().padTop(2f);
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
        addHeader("#", "rank", 42f, false);
        addHeader("Username", "username", 145f, true);
        addHeader("Progress", "progress", 175f, true);
        addHeader("Minigames", "minigames", 110f, true);
        addHeader("Daily Quests", "daily-quests", 120f, true);
        addHeader("Other Quests", "quests", 120f, true);
        addHeader("Best MioPoint", "best-score", 130f, true);
        rowsTable.row();
    }

    private void addHeader(String text, String column, float width, boolean sortable) {
        if (!sortable) {
            Label label = createRowLabel(text);
            rowsTable.add(label).width(width).height(40f).center();
            return;
        }
        String marker = sortColumn.equals(column) ? ascending ? " ^" : " v" : "";
        MenuButton button = new MenuButton(text + marker, skin, "brown", () -> sortBy(column));
        button.getLabel().setAlignment(Align.center);
        rowsTable.add(button).width(width).height(40f).center();
    }

    private void addUserRow(int rank, User user) {
        addValue(String.valueOf(rank), 42f);
        addValue(user.getUsername(), 145f);
        addValue(controller.getLastProgress(user), 175f);
        addValue(String.valueOf(controller.getCompletedMiniGameCount(user)), 110f);
        addValue(String.valueOf(controller.getDailyQuestCount(user)), 120f);
        addValue(String.valueOf(controller.getNonDailyQuestCount(user)), 120f);
        addValue(String.valueOf(user.getBestMioPoint()), 130f);
        rowsTable.row();
    }

    private void addValue(String text, float width) {
        rowsTable.add(createRowLabel(text)).width(width).height(34f).center().padTop(4f);
    }

    private Label createRowLabel(String text) {
        Label label = new Label(text == null ? "" : text, skin, "secondary");
        label.setAlignment(Align.center);
        label.setWrap(true);
        return label;
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
