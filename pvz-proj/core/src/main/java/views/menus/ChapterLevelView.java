package views.menus;

import controllers.core.MenuManager;
import controllers.features.ChapterLevelController;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChapterLevelView extends BaseView {
    private static final Pattern SELECT_LEVEL_PATTERN = Pattern.compile(
            "^(?:select|enter)\\s+level\\s+-l\\s+(\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private final MenuManager menuManager;
    private final ChapterLevelController controller;

    public ChapterLevelView(
            String viewName,
            MenuManager menuManager,
            ChapterLevelController controller
    ) {
        super(viewName);
        this.menuManager = menuManager;
        this.controller = controller;
    }

    @Override
    public void display() {
        controller.showLevels();
        printControllerMessage(controller.getLastMessage());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);

        if ("show levels".equalsIgnoreCase(command)) {
            controller.showLevels();
            printControllerMessage(controller.getLastMessage());
            return;
        }

        if ("menu show current".equalsIgnoreCase(command)) {
            menuManager.showCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return;
        }

        if ("menu exit".equalsIgnoreCase(command)) {
            menuManager.exitCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return;
        }

        Matcher matcher = SELECT_LEVEL_PATTERN.matcher(command);
        if (matcher.matches()) {
            Integer levelNumber = parseInteger(matcher.group(1));
            if (levelNumber == null) {
                controller.invalidCommand();
                printControllerMessage(controller.getLastMessage());
                return;
            }

            controller.selectLevel(levelNumber);
            printControllerMessage(controller.getLastMessage());
            if (controller.wasSuccessful()) {
                menuManager.enterGamePlayMenu();
                printControllerMessage(menuManager.getLastMessage());
            }
            return;
        }

        controller.invalidCommand();
        printControllerMessage(controller.getLastMessage());
    }
}
