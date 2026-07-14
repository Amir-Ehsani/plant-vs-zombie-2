package controllers.auth;

import controllers.core.SaveManager;
import models.account.User;

import java.util.ArrayList;
import java.util.Locale;

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
    private User recoveryUser;
    private String lastMessage;

    public AuthController() {
        this.saveManager = new SaveManager();
        this.users = new ArrayList<>(saveManager.loadAllUsers());
        this.passwordValidator = new PasswordValidator();
        this.emailValidator = new EmailValidator();
        this.loggedInUser = findStayLoggedInUser();
        this.pendingUser = null;
        this.recoveryUser = null;
        this.lastMessage = "";
    }

    public void register(String username, String password, String passwordConfirm, String nickname, String email,
                         String gender) {
        if (!isValidUsername(username)) {
            fail("Invalid username.");
            return;
        }

        if (findUser(username) != null) {
            fail("Username already exists.");
            return;
        }

        if (password == null || passwordConfirm == null || !password.equals(passwordConfirm)) {
            fail("Password confirmation does not match.");
            return;
        }

        if (!checkPassword(password) || !checkNickname(nickname) || !checkEmail(email) || !checkGender(gender)) {
            return;
        }

        pendingUser = new User(username, password, nickname, email, normalizeGender(gender));
        success(securityQuestionText());
    }

    public void pickQuestion(int questionNumber, String answer, String answerConfirm) {
        if (pendingUser == null) {
            fail("No pending registration exists.");
            return;
        }

        if (questionNumber < 1 || questionNumber > SECURITY_QUESTIONS.length) {
            fail("Invalid security question number.");
            return;
        }

        if (answer == null || answer.isBlank()) {
            fail("Security answer cannot be empty.");
            return;
        }

        if (answerConfirm == null || !answer.equals(answerConfirm)) {
            fail("Security answer confirmation does not match.");
            return;
        }

        pendingUser.setSecurityQuestionNumber(questionNumber);
        pendingUser.setSecurityAnswer(answer);
        users.add(pendingUser);
        pendingUser = null;
        saveUsers();
        success("Registered successfully. You can login now.");
    }

    public void login(String username, String password, boolean stayLoggedIn) {
        User user = findUser(username);

        if (user == null) {
            fail("Username not found.");
            return;
        }

        if (password == null || !password.equals(user.getPassword())) {
            fail("Password is incorrect.");
            return;
        }

        clearStayLoggedInUsers();
        user.setStayLoggedIn(stayLoggedIn);
        loggedInUser = user;
        saveUsers();
        success("Logged in successfully.");
    }

    public void logout() {
        if (loggedInUser != null) {
            loggedInUser.setStayLoggedIn(false);
        }

        loggedInUser = null;
        saveUsers();
        success("Logged out successfully.");
    }

    public void forgetPassword(String username, String email) {
        User user = findUser(username);

        if (user == null) {
            fail("Username not found.");
            return;
        }

        if (email == null || !email.equals(user.getEmail())) {
            fail("Email does not match this user.");
            return;
        }

        if (user.getSecurityQuestionNumber() < 1 || user.getSecurityQuestionNumber() > SECURITY_QUESTIONS.length) {
            fail("This user has no security question.");
            return;
        }

        recoveryUser = user;
        success("Security question: " + SECURITY_QUESTIONS[user.getSecurityQuestionNumber() - 1]);
    }

    public void answerSecurityQuestion(String answer) {
        if (recoveryUser == null) {
            fail("No password recovery request exists.");
            return;
        }

        if (answer == null || !answer.equals(recoveryUser.getSecurityAnswer())) {
            recoveryUser = null;
            fail("Security answer is incorrect.");
            return;
        }

        success("Security answer accepted.");
    }

    public void changeUsername(String newUsername) {
        if (!requireLoggedIn()) {
            return;
        }

        if (!isValidUsername(newUsername)) {
            fail("Invalid username.");
            return;
        }

        if (loggedInUser.getUsername().equals(newUsername)) {
            fail("New username is the same as current username.");
            return;
        }

        if (findUser(newUsername) != null) {
            fail("Username already exists.");
            return;
        }

        loggedInUser.setUsername(newUsername);
        saveUsers();
        success("Username changed successfully.");
    }

    public void changeNickname(String newNickname) {
        if (!requireLoggedIn()) {
            return;
        }

        if (!checkNickname(newNickname)) {
            return;
        }

        if (loggedInUser.getNickname().equals(newNickname)) {
            fail("New nickname is the same as current nickname.");
            return;
        }

        loggedInUser.setNickname(newNickname);
        saveUsers();
        success("Nickname changed successfully.");
    }

    public void changeEmail(String newEmail) {
        if (!requireLoggedIn()) {
            return;
        }

        if (!checkEmail(newEmail)) {
            return;
        }

        if (loggedInUser.getEmail().equals(newEmail)) {
            fail("New email is the same as current email.");
            return;
        }

        loggedInUser.setEmail(newEmail);
        saveUsers();
        success("Email changed successfully.");
    }

    public void changePassword(String newPassword, String oldPassword) {
        if (!requireLoggedIn()) {
            return;
        }

        if (oldPassword == null || !oldPassword.equals(loggedInUser.getPassword())) {
            fail("Old password is incorrect.");
            return;
        }

        if (newPassword == null || newPassword.equals(loggedInUser.getPassword())) {
            fail("New password must be different from current password.");
            return;
        }

        if (!checkPassword(newPassword)) {
            return;
        }

        loggedInUser.setPassword(newPassword);
        saveUsers();
        success("Password changed successfully.");
    }

    public void changeDifficulty(int difficultyLevel) {
        if (!requireLoggedIn()) {
            return;
        }

        if (difficultyLevel < 1 || difficultyLevel > 5) {
            fail("Difficulty level must be between 1 and 5.");
            return;
        }

        loggedInUser.setDifficultyLevel(difficultyLevel);
        saveUsers();
        success("Difficulty changed successfully.");
    }

    public void addCheatCurrency(int amount, String currency) {
        if (!requireLoggedIn()) {
            return;
        }

        if (amount <= 0) {
            fail("Amount must be positive.");
            return;
        }

        String normalizedCurrency = normalize(currency);

        if ("coin".equals(normalizedCurrency) || "coins".equals(normalizedCurrency)) {
            loggedInUser.addCoins(amount);
            saveUsers();
            success(amount + " coins added.");
            return;
        }

        if ("diamond".equals(normalizedCurrency) || "diamonds".equals(normalizedCurrency)
                || "gem".equals(normalizedCurrency) || "gems".equals(normalizedCurrency)) {
            loggedInUser.addGems(amount);
            saveUsers();
            success(amount + " gems added.");
            return;
        }

        fail("Unknown currency.");
    }

    public void showCoinWallet() {
        if (!requireLoggedIn()) {
            return;
        }

        success("Coins: " + loggedInUser.getCoins());
    }

    public void showGemWallet() {
        if (!requireLoggedIn()) {
            return;
        }

        success("Gems: " + loggedInUser.getGems());
    }

    public void showProfileInfo() {
        if (!requireLoggedIn()) {
            return;
        }

        success(profileInfoText());
    }

    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean isLoggedIn() {
        return loggedInUser != null;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    public void saveUsers() {
        saveManager.saveAllUsers(users);
    }

    private boolean checkPassword(String password) {
        passwordValidator.isValid(password);

        if (!passwordValidator.wasSuccessful()) {
            lastMessage = passwordValidator.getLastMessage();
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

    private boolean checkNickname(String nickname) {
        if (nickname == null || nickname.length() < 3 || nickname.length() > 30) {
            fail("Nickname length must be between 3 and 30 characters.");
            return false;
        }

        return true;
    }

    private boolean checkGender(String gender) {
        if (!isValidGender(gender)) {
            fail("Gender must be male or female.");
            return false;
        }

        return true;
    }

    private boolean requireLoggedIn() {
        if (loggedInUser == null) {
            fail("No user is logged in.");
            return false;
        }

        return true;
    }

    private boolean isValidUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9-]+");
    }

    private boolean isValidGender(String gender) {
        String normalizedGender = normalize(gender);
        return "male".equals(normalizedGender) || "female".equals(normalizedGender)
                || "man".equals(normalizedGender) || "woman".equals(normalizedGender);
    }

    private String normalizeGender(String gender) {
        String normalizedGender = normalize(gender);

        if ("man".equals(normalizedGender)) {
            return "male";
        }

        if ("woman".equals(normalizedGender)) {
            return "female";
        }

        return normalizedGender;
    }

    private User findStayLoggedInUser() {
        for (User user : users) {
            if (user != null && user.isStayLoggedIn()) {
                return user;
            }
        }

        return null;
    }

    private User findUser(String username) {
        if (username == null) {
            return null;
        }

        for (User user : users) {
            if (user != null && username.equals(user.getUsername())) {
                return user;
            }
        }

        return null;
    }

    private void clearStayLoggedInUsers() {
        for (User user : users) {
            if (user != null) {
                user.setStayLoggedIn(false);
            }
        }
    }

    private String securityQuestionText() {
        StringBuilder text = new StringBuilder();
        text.append("Choose a security question:\n");

        for (int i = 0; i < SECURITY_QUESTIONS.length; i++) {
            text.append(i + 1).append(". ").append(SECURITY_QUESTIONS[i]).append("\n");
        }

        text.append("pick question -q <question_number> -a <answer> -c <answer_confirm>");
        return text.toString();
    }

    private String profileInfoText() {
        return "Profile Info\n"
                + "Username: " + loggedInUser.getUsername() + "\n"
                + "Nickname: " + loggedInUser.getNickname() + "\n"
                + "Games played: " + loggedInUser.getGamesPlayed() + "\n"
                + "Coins: " + loggedInUser.getCoins() + "\n"
                + "Gems: " + loggedInUser.getGems() + "\n"
                + "Passed levels: " + loggedInUser.getPassedLevels() + "\n"
                + "Best mio point: " + loggedInUser.getBestMioPoint();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }
}