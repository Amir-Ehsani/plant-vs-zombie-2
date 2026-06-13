package views.menus;

import models.account.User;
import views.core.BaseView;

public class ProfileView extends BaseView {
    public ProfileView(String viewName) {
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

    public void showProfileInfo(User user) {}
}
