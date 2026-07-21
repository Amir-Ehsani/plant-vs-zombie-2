package controllers.features;

import controllers.auth.AuthController;

public class MainMenuController {
    private final AuthController authController;
    private String lastMessage;

    public MainMenuController(AuthController authController) {
        this.authController = authController;
        this.lastMessage = "";
    }

    public void logout() {
        authController.logout();
        lastMessage = authController.getLastMessage();
    }

    public boolean isLoggedIn() {
        return authController.isLoggedIn();
    }

    public int getUnreadNewsCount() {
        if (authController == null || authController.getLoggedInUser() == null) {
            return 0;
        }
        int count = 0;
        for (models.account.News news : authController.getLoggedInUser().getNewsList()) {
            if (news != null && news.isUnread()) {
                count++;
            }
        }
        return count;
    }

    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }
}   