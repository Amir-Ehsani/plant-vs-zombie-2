package views.menus;

import models.account.User;
import views.core.BaseView;

import java.util.List;

public class LeaderboardView extends BaseView {
    public LeaderboardView(String viewName) {
        super(viewName);
    }

    @Override
    public void display() {

    }

    @Override
    public void showErrorMessage(String message) {

    }

    @Override
    public void showSuccessMessage(String message) {

    }

    public void showRankings(List<User> rankedUsers) {}
}
