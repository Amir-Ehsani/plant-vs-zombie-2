package controllers.features;

import controllers.auth.AuthController;
import controllers.auth.EmailValidator;
import controllers.auth.PasswordValidator;
import models.account.User;

public class ProfileController {
    private final AuthController authController;
    private final EmailValidator emailValidator;
    private final PasswordValidator passwordValidator;
    private String lastMessage;

    public ProfileController(AuthController authController) {
        this.authController = authController;
        this.emailValidator = new EmailValidator();
        this.passwordValidator = new PasswordValidator();
        this.lastMessage = "";
    }

    public void showProfileInfo() {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        success(profileInfoText(user));
    }

    public void changeUsername(String newUsername) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        if (!isValidUsername(newUsername)) {
            fail("Invalid username.");
            return;
        }

        if (user.getUsername().equals(newUsername)) {
            fail("New username is the same as current username.");
            return;
        }

        if (authController.usernameExists(newUsername)) {
            fail("Username already exists.");
            return;
        }

        user.setUsername(newUsername);
        authController.saveUsers();
        success("Username changed successfully.");
    }

    public void changeNickname(String newNickname) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        if (!checkNickname(newNickname)) {
            return;
        }

        if (user.getNickname().equals(newNickname)) {
            fail("New nickname is the same as current nickname.");
            return;
        }

        user.setNickname(newNickname);
        authController.saveUsers();
        success("Nickname changed successfully.");
    }

    public void changeEmail(String newEmail) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        if (!checkEmail(newEmail)) {
            return;
        }

        if (user.getEmail().equals(newEmail)) {
            fail("New email is the same as current email.");
            return;
        }

        user.setEmail(newEmail);
        authController.saveUsers();
        success("Email changed successfully.");
    }

    public void changePassword(String newPassword, String oldPassword) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        if (oldPassword == null || !oldPassword.equals(user.getPassword())) {
            fail("Old password is incorrect.");
            return;
        }

        if (newPassword == null || newPassword.equals(user.getPassword())) {
            fail("New password must be different from current password.");
            return;
        }

        if (!checkPassword(newPassword)) {
            return;
        }

        user.setPassword(newPassword);
        authController.saveUsers();
        success("Password changed successfully.");
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

    private boolean isValidUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9-]+");
    }

    private boolean checkNickname(String nickname) {
        if (nickname == null || nickname.length() < 3 || nickname.length() > 30) {
            fail("Nickname length must be between 3 and 30 characters.");
            return false;
        }

        return true;
    }

    private boolean checkEmail(String email) {
        emailValidator.isValid(email);

        if (!emailValidator.wasSuccessful()) {
            lastMessage = emailValidator.getLastMessage();
            return false;
        }

        return true;
    }

    private boolean checkPassword(String password) {
        passwordValidator.isValid(password);

        if (!passwordValidator.wasSuccessful()) {
            lastMessage = passwordValidator.getLastMessage();
            return false;
        }

        return true;
    }

    private String profileInfoText(User user) {
        return "Profile Info\n"
                + "Username: " + user.getUsername() + "\n"
                + "Nickname: " + user.getNickname() + "\n"
                + "Games played: " + user.getGamesPlayed() + "\n"
                + "Coins: " + user.getCoins() + "\n"
                + "Gems: " + user.getGems() + "\n"
                + "Passed levels: " + user.getPassedLevels() + "\n"
                + "Best mio point: " + user.getBestMioPoint();
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }
}