import controllers.core.MenuManager;
import views.core.BaseView;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MenuManager menuManager = new MenuManager();

        if (menuManager.getCurrentView() != null) {
            menuManager.getCurrentView().display();
        }

        while (menuManager.getCurrentView() != null) {
            String input = scanner.nextLine();

            BaseView previousView = menuManager.getCurrentView();
            previousView.handleInput(input);

            BaseView currentView = menuManager.getCurrentView();

            if (currentView != null && currentView != previousView) {
                currentView.display();
            }
        }

        scanner.close();
    }
}