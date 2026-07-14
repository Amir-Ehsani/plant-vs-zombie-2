package views.menus;

import controllers.auth.AuthController;
import controllers.core.MenuManager;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginView extends BaseView {
    private static final Pattern REGISTER_PATTERN = Pattern.compile(
            "^register\\s+-u\\s+(\\S+)\\s+-p\\s+(\\S+)\\s+(\\S+)\\s+-n\\s+(\\S+)\\s+-e\\s+(\\S+)\\s+-g\\s+(\\S+)\\s*$"
    );
    private static final Pattern PICK_QUESTION_PATTERN = Pattern.compile(
            "^pick\\s+question\\s+-q\\s+(\\d+)\\s+-a\\s+(.+?)\\s+-c\\s+(.+)\\s*$"
    );
    private static final Pattern LOGIN_PATTERN = Pattern.compile(
            "^login\\s+-u\\s+(\\S+)\\s+-p\\s+(\\S+)(\\s+-stay-logged-in)?\\s*$"
    );
    private static final Pattern FORGET_PASSWORD_PATTERN = Pattern.compile(
            "^forget\\s+password\\s+-u\\s+(\\S+)\\s+-e\\s+(\\S+)\\s*$"
    );
    private static final Pattern ANSWER_PATTERN = Pattern.compile("^answer\\s+-a\\s+(.+)\\s*$");
    private static final Pattern MENU_ENTER_PATTERN = Pattern.compile("^menu\\s+enter\\s+(\\S+)\\s*$");

    private final MenuManager menuManager;
    private final AuthController authController;

    public LoginView(String viewName) {
        super(viewName);
        this.menuManager = null;
        this.authController = null;
    }

    public LoginView(String viewName, MenuManager menuManager, AuthController authController) {
        super(viewName);
        this.menuManager = menuManager;
        this.authController = authController;
    }

    @Override
    public void display() {
        if (menuManager == null) {
            return;
        }

        menuManager.showLoginMenuText();
        printControllerMessage(menuManager.getLastMessage());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);

        if (handleMenuCommand(command)) {
            return;
        }

        if (handleRegister(command)) {
            return;
        }

        if (handlePickQuestion(command)) {
            return;
        }

        if (handleLogin(command)) {
            return;
        }

        if (handleForgetPassword(command)) {
            return;
        }

        if (handleAnswer(command)) {
            return;
        }

        authController.invalidCommand("login menu");
        printControllerMessage(authController.getLastMessage());
    }

    private boolean handleMenuCommand(String command) {
        if ("menu show current".equals(command)) {
            menuManager.showCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        if ("menu exit".equals(command)) {
            menuManager.exitCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        Matcher matcher = MENU_ENTER_PATTERN.matcher(command);

        if (matcher.matches()) {
            menuManager.enterNamedMenu(matcher.group(1));
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }

        return false;
    }

    private boolean handleRegister(String command) {
        Matcher matcher = REGISTER_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        authController.register(
                matcher.group(1),
                matcher.group(2),
                matcher.group(3),
                matcher.group(4),
                matcher.group(5),
                matcher.group(6)
        );
        printControllerMessage(authController.getLastMessage());
        return true;
    }

    private boolean handlePickQuestion(String command) {
        Matcher matcher = PICK_QUESTION_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        Integer questionNumber = parseInteger(matcher.group(1));

        if (questionNumber == null) {
            authController.invalidCommand("login menu");
            printControllerMessage(authController.getLastMessage());
            return true;
        }

        authController.pickQuestion(questionNumber, matcher.group(2), matcher.group(3));
        printControllerMessage(authController.getLastMessage());
        return true;
    }

    private boolean handleLogin(String command) {
        Matcher matcher = LOGIN_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        authController.login(matcher.group(1), matcher.group(2), matcher.group(3) != null);
        printControllerMessage(authController.getLastMessage());

        if (authController.wasSuccessful()) {
            menuManager.enterMainMenu();
        }

        return true;
    }

    private boolean handleForgetPassword(String command) {
        Matcher matcher = FORGET_PASSWORD_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        authController.forgetPassword(matcher.group(1), matcher.group(2));
        printControllerMessage(authController.getLastMessage());
        return true;
    }

    private boolean handleAnswer(String command) {
        Matcher matcher = ANSWER_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        authController.answerSecurityQuestion(matcher.group(1));
        printControllerMessage(authController.getLastMessage());
        return true;
    }
}