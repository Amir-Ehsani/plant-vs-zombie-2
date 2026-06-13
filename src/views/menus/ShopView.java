package views.menus;

import models.account.IPurchasable;
import views.core.BaseView;

import java.util.List;

public class ShopView extends BaseView {
    public ShopView(String viewName) {
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

    public void showCatalog(List<IPurchasable> permanent, List<IPurchasable> daily) {}
}
