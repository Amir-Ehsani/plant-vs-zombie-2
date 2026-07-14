package controllers.auth;

import controllers.core.SaveManager;
import models.account.User;

import java.util.ArrayList;
import java.util.List;

public class AuthController {
    private static final String[] SECURITY_QUESTIONS = {
            "What was the name of your first pet?",
            "What is your favorite plant?",
            "What city were you born in?"
    };

    private final SaveManager saveManager;
    private final ArrayList<User> users;
    private final PasswordValidator passwordValidator;
    private final EmailValidator emailValidator;

    private User loggedInUser;
    private User pendingUser;
    private String lastMessage;

    public AuthController() {
        this.saveManager = new SaveManager();
        this.users = loadUsers();
        this.passwordValidator = new PasswordValidator();
        this.emailValidator = new EmailValidator();
        this.loggedInUser = null;
        this.pendingUser = null;
        this.lastMessage = "";
    }

    public void register(String username, String password, String passwordConfirm, String nickname, String email, String gender) {
        if (!validUsername(username)) {
            fail("invalid username");
            return;
        }

        if (findUser(username) != null) {
            fail("username already exists");
            return;
        }

        if (password == null || passwordConfirm == null || !password.equals(passwordConfirm)) {
            fail("password confirmation does not match");
            return;
        }

        passwordValidator.isValid(password);

        if (!passwordValidator.wasSuccessful()) {
            lastMessage = passwordValidator.getLastMessage();
            return;
        }

        if (!validNickname(nickname)) {
            fail("invalid nickname");
            return;
        }

        emailValidator.isValid(email);

        if (!emailValidator.wasSuccessful()) {
            lastMessage = emailValidator.getLastMessage();
            return;
        }

        if (gender == null || !(gender.equals("male") || gender.equals("female"))) {
            fail("invalid gender");
            return;
        }

        pendingUser = new User(username, password, nickname, email, gender);
        success(securityQuestionText());
    }

    public void pickQuestion(int questionNumber, String answer, String answerConfirm) {
        if (pendingUser == null) {
            fail("no pending registration");
            return;
        }

        if (questionNumber < 1 || questionNumber > SECURITY_QUESTIONS.length) {
            fail("invalid security question number");
            return;
        }

        if (answer == null || answer.trim().isEmpty()) {
            fail("security answer cannot be empty");
            return;
        }

        if (answerConfirm == null || !answer.equals(answerConfirm)) {
            fail("security answer confirmation does not match");
            return;
        }

        pendingUser.setSecurityQuestionNumber(questionNumber);
        pendingUser.setSecurityAnswer(answer);

        users.add(pendingUser);
        pendingUser = null;

        saveManager.saveAllUsers(users);

        success("registered successfully");
    }

    public void login(String username, String password) {
        User user = findUser(username);

        if (user == null) {
            fail("username not found");
            return;
        }

        if (password == null || !user.getPassword().equals(password)) {
            fail("password incorrect");
            return;
        }

        loggedInUser = user;
        success("logged in successfully");
    }

    public void logout() {
        loggedInUser = null;
        success("logged out successfully");
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public boolean isLoggedIn() {
        return loggedInUser != null;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private boolean validUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9-]+");
    }

    private boolean validNickname(String nickname) {
        return nickname != null && nickname.length() >= 3 && nickname.length() <= 30;
    }

    private User findUser(String username) {
        if (username == null) {
            return null;
        }

        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }

        return null;
    }

    private String securityQuestionText() {
        StringBuilder text = new StringBuilder();

        text.append("choose a security question:\n");

        int i = 1;
        for (String question : SECURITY_QUESTIONS) {
            text.append(i).append(". ").append(question).append("\n");
            i++;
        }

        text.append("pick question -q <question_number> -a <answer> -c <answer_confirm>");

        return text.toString();
    }

    private ArrayList<User> loadUsers() {
        List<User> loadedUsers = saveManager.loadAllUsers();

        if (loadedUsers == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(loadedUsers);
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }
}