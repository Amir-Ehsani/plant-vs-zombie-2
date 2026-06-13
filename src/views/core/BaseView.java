package views.core;

public abstract class BaseView {
    private String viewName;

    public BaseView(String viewName) {
        this.viewName = viewName;
    }

    public abstract void display();
    public abstract void showErrorMessage(String message);
    public abstract void showSuccessMessage(String message);
}
