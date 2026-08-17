package views.core;

public abstract class BaseView {
    private final String viewName;

    public BaseView(String viewName) {
        this.viewName = viewName;
    }

    public String getViewName() {
        return viewName;
    }

    public abstract void display();

    public abstract void handleInput(String input);

    protected void printControllerMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        System.out.println(message);
    }

    protected String cleanInput(String input) {
        if (input == null) {
            return "";
        }

        return input.trim();
    }

    protected Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}