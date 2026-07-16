package views.menus;

import controllers.auth.AuthController;
import controllers.core.MenuManager;
import controllers.features.ProfileController;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileView extends BaseView {
    private static final Pattern CHANGE_USERNAME_PATTERN = Pattern.compile(
            "^menu\\s+profile\\s+change-username\\s+-u\\s+(\\S+)\\s*$"
    );
    private static final Pattern CHANGE_NICKNAME_PATTERN = Pattern.compile(
            "^menu\\s+profile\\s+change-nickname\\s+-u\\s+(.+)\\s*$"
    );
    private static final Pattern CHANGE_EMAIL_PATTERN = Pattern.compile(
            "^menu\\s+profile\\s+change-email\\s+-e\\s+(\\S+)\\s*$"
    );
    private static final Pattern CHANGE_PASSWORD_PATTERN = Pattern.compile(
            "^menu\\s+profile\\s+change-password\\s+-p\\s+(\\S+)\\s+-o\\s+(\\S+)\\s*$"
    );

    private final MenuManager menuManager;
    private final ProfileController profileController;

    public ProfileView(String viewName, MenuManager menuManager, ProfileController profileController) {
        super(viewName);
        this.menuManager = menuManager;
        this.profileController = profileController;
    }

    @Override
    public void display() {
        menuManager.showProfileMenuText();
        printControllerMessage(menuManager.getLastMessage());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);

        if (!profileController.isLoggedIn()) {
            profileController.invalidCommand("profile menu");
            printControllerMessage(profileController.getLastMessage());
            menuManager.enterLoginMenu();
            return;
        }

        if (handleMenuCommand(command)) {
            return;
        }

        if (handleShowInfo(command)) {
            return;
        }

        if (handleChangeUsername(command)) {
            return;
        }

        if (handleChangeNickname(command)) {
            return;
        }

        if (handleChangeEmail(command)) {
            return;
        }

        if (handleChangePassword(command)) {
            return;
        }

        profileController.invalidCommand("profile menu");
        printControllerMessage(profileController.getLastMessage());
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
        return false;
    }

    private boolean handleShowInfo(String command) {
        if (!"menu profile show-info".equals(command)) {
            return false;
        }

        profileController.showProfileInfo();
        printControllerMessage(profileController.getLastMessage());
        return true;
    }

    private boolean handleChangeUsername(String command) {
        Matcher matcher = CHANGE_USERNAME_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        profileController.changeUsername(matcher.group(1));
        printControllerMessage(profileController.getLastMessage());
        return true;
    }

    private boolean handleChangeNickname(String command) {
        Matcher matcher = CHANGE_NICKNAME_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        profileController.changeNickname(matcher.group(1));
        printControllerMessage(profileController.getLastMessage());
        return true;
    }

    private boolean handleChangeEmail(String command) {
        Matcher matcher = CHANGE_EMAIL_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        profileController.changeEmail(matcher.group(1));
        printControllerMessage(profileController.getLastMessage());
        return true;
    }

    private boolean handleChangePassword(String command) {
        Matcher matcher = CHANGE_PASSWORD_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return false;
        }

        profileController.changePassword(matcher.group(1), matcher.group(2));
        printControllerMessage(profileController.getLastMessage());
        return true;
    }
}