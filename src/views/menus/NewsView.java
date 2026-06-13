package views.menus;

import models.account.News;
import views.core.BaseView;

import java.util.List;

public class NewsView extends BaseView {
    public NewsView(String viewName) {
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

    public void showNewsList(List<News> newsList) {}
}
