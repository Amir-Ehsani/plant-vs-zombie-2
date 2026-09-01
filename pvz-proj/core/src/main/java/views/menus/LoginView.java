package views.menus;

import controllers.auth.AuthController;
import controllers.core.MenuManager;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginView extends BaseView {
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
    private boolean waitingForSecurityAnswer;
    private boolean waitingForNewPassword;

    public LoginView(String viewName, MenuManager menuManager, AuthController authController) {
        super(viewName);
        this.menuManager = menuManager;
        this.authController = authController;
        this.waitingForSecurityAnswer = false;
        this.waitingForNewPassword = false;
    }

    @Override
    public void display() {
        menuManager.showLoginMenuText();
        printControllerMessage(menuManager.getLastMessage());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);

        if (handleForgetPassword(command)) {
            return;
        }

        if (handleMenuCommand(command)) {
            return;
        }

        if (handleLogin(command)) {
            return;
        }

        authController.invalidCommand("login menu");
        printControllerMessage(authController.getLastMessage());
    }

    private boolean handleForgetPassword(String command) {
        if (waitingForNewPassword) {
            return handleNewPassword(command);
        }
        if (waitingForSecurityAnswer) {
            return handleSecurityAnswer(command);
        }
        Matcher matcher = FORGET_PASSWORD_PATTERN.matcher(command);
        if (!matcher.matches()) {
            return false;
        }
        authController.forgetPassword(matcher.group(1), matcher.group(2));
        printControllerMessage(authController.getLastMessage());
        if (authController.wasSuccessful()) {
            waitingForSecurityAnswer = true;
        }
        return true;
    }

    private boolean handleNewPassword(String command) {
        authController.resetForgottenPassword(command);
        printControllerMessage(authController.getLastMessage());
        if (authController.wasSuccessful()) {
            waitingForNewPassword = false;
            waitingForSecurityAnswer = false;
        }
        return true;
    }

    private boolean handleSecurityAnswer(String command) {
        Matcher matcher = ANSWER_PATTERN.matcher(command);
        if (!matcher.matches()) {
            authController.invalidCommand("login menu");
            printControllerMessage(authController.getLastMessage());
            return true;
        }
        authController.answerSecurityQuestion(matcher.group(1));
        printControllerMessage(authController.getLastMessage());
        waitingForNewPassword = authController.wasSuccessful();
        waitingForSecurityAnswer = false;
        return true;
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

    private boolean handleLogin(String command) {
        Matcher matcher = LOGIN_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        authController.login(matcher.group(1), matcher.group(2), matcher.group(3) != null);
        printControllerMessage(authController.getLastMessage());

        if (authController.wasSuccessful()) {
            menuManager.enterMainMenu();
            printControllerMessage(menuManager.getLastMessage());
        }

        return true;
    }
}
