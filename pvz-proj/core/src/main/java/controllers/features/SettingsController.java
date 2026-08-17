package controllers.features;

import controllers.auth.AuthController;
import models.account.User;

public class SettingsController {
    private final AuthController authController;
    private String lastMessage;

    public SettingsController(AuthController authController) {
        this.authController = authController;
        this.lastMessage = "";
    }

    public void changeDifficulty(int difficultyLevel) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        if (difficultyLevel < 1 || difficultyLevel > 5) {
            fail("Difficulty level must be between 1 and 5.");
            return;
        }

        user.setDifficultyLevel(difficultyLevel);
        authController.saveUsers();
        success("Difficulty changed successfully.");
    }

    public boolean isLoggedIn() {
        return authController.isLoggedIn();
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

    private User getLoggedInUserOrFail() {
        User user = authController.getLoggedInUser();

        if (user == null) {
            fail("No user is logged in.");
            return null;
        }

        return user;
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }
}