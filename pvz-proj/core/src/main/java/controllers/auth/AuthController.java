package controllers.auth;

import controllers.core.SaveManager;
import models.account.User;
import network.client.NetworkAuthResult;
import network.client.NetworkManager;
import network.client.NetworkOperationResult;
import network.client.NetworkResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final NetworkManager networkManager;
    private User loggedInUser;
    private User pendingUser;
    private User recoveryUser;
    private String networkResetId;
    private String lastMessage;
    private int lastSubmittedMioPoint;

    /** Local-only constructor kept for model/controller tests and offline tooling. */
    public AuthController() {
        this(null);
    }

    /** Main game constructor: account state becomes server-authoritative. */
    public AuthController(NetworkManager networkManager) {
        this.saveManager = new SaveManager();
        this.users = new ArrayList<>(saveManager.loadAllUsers());
        this.passwordValidator = new PasswordValidator();
        this.emailValidator = new EmailValidator();
        this.networkManager = networkManager;
        this.pendingUser = null;
        this.recoveryUser = null;
        this.networkResetId = null;
        this.lastMessage = "";
        this.lastSubmittedMioPoint = 0;

        if (networkManager == null) {
            this.loggedInUser = findStayLoggedInUser();
        } else {
            this.loggedInUser = restoreServerSessionIfAvailable();
        }
        if (loggedInUser != null) {
            lastSubmittedMioPoint = Math.max(0, loggedInUser.getBestMioPoint());
        }
    }

    public void register(
            String username,
            String password,
            String passwordConfirm,
            String nickname,
            String email,
            String gender
    ) {
        if (pendingUser != null) {
            fail("Finish the current registration first.");
            return;
        }
        if (!isValidUsername(username)) {
            fail("Invalid username.");
            return;
        }
        if (networkManager == null && findUser(username) != null) {
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

        pendingUser = new User(username, "", nickname, email, normalizeGender(gender));
        setUserPassword(pendingUser, password);
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

        if (networkManager != null) {
            NetworkAuthResult result = networkManager.register(
                    pendingUser,
                    SECURITY_QUESTIONS[questionNumber - 1],
                    answer
            );
            if (!result.successful()) {
                pendingUser = null;
                fail(result.message());
                return;
            }
        }

        upsertLocalUser(pendingUser);
        pendingUser = null;
        saveLocalUsers();
        success("Registered successfully. You can login now.");
    }

    public void login(String username, String password, boolean stayLoggedIn) {
        if (networkManager != null) {
            NetworkAuthResult result = networkManager.login(username, password, stayLoggedIn);
            if (!result.successful() || result.user() == null) {
                fail(result.message());
                return;
            }
            clearStayLoggedInUsers();
            loggedInUser = result.user();
            loggedInUser.setStayLoggedIn(stayLoggedIn);
            upsertLocalUser(loggedInUser);
            lastSubmittedMioPoint = Math.max(0, loggedInUser.getBestMioPoint());
            saveLocalUsers();
            success("Logged in successfully.");
            return;
        }

        User user = findUser(username);
        if (user == null) {
            fail("Username not found.");
            return;
        }
        if (!passwordMatches(user, password)) {
            fail("Password is incorrect.");
            return;
        }
        clearStayLoggedInUsers();
        user.setStayLoggedIn(stayLoggedIn);
        loggedInUser = user;
        saveLocalUsers();
        success("Logged in successfully.");
    }

    public void logout() {
        NetworkOperationResult networkResult = null;
        if (networkManager != null) {
            networkResult = networkManager.logout();
        }
        if (loggedInUser != null) {
            loggedInUser.setStayLoggedIn(false);
        }
        loggedInUser = null;
        lastSubmittedMioPoint = 0;
        saveLocalUsers();
        if (networkResult != null && !networkResult.successful()) {
            fail(networkResult.message());
        } else {
            success("Logged out successfully.");
        }
    }

    public void forgetPassword(String username, String email) {
        if (networkManager != null) {
            NetworkResponse response = networkManager.beginPasswordReset(username, email);
            if (!response.wasSuccessful()) {
                networkResetId = null;
                fail(response.getMessage());
                return;
            }
            networkResetId = response.get("resetId");
            String question = response.get("securityQuestion");
            success("Security question: " + (question == null ? "" : question));
            return;
        }

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
        if (networkManager != null) {
            if (networkResetId == null || networkResetId.isBlank()) {
                fail("No password recovery request exists.");
                return;
            }
            NetworkResponse response = networkManager.verifyPasswordReset(networkResetId, answer);
            if (!response.wasSuccessful()) {
                networkResetId = null;
                fail(response.getMessage());
                return;
            }
            success("Security answer accepted. Enter your new password:");
            return;
        }

        if (recoveryUser == null) {
            fail("No password recovery request exists.");
            return;
        }
        if (answer == null || !answer.equals(recoveryUser.getSecurityAnswer())) {
            recoveryUser = null;
            fail("Security answer is incorrect.");
            return;
        }
        success("Security answer accepted. Enter your new password:");
    }

    public void resetForgottenPassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            fail("Password cannot be empty.");
            return;
        }
        if (!checkPassword(newPassword)) {
            return;
        }

        if (networkManager != null) {
            if (networkResetId == null || networkResetId.isBlank()) {
                fail("No verified password recovery request exists.");
                return;
            }
            NetworkOperationResult result = networkManager.completePasswordReset(networkResetId, newPassword);
            networkResetId = null;
            if (!result.successful()) {
                fail(result.message());
                return;
            }
            success("Password changed successfully.");
            return;
        }

        if (recoveryUser == null) {
            fail("No password recovery request exists.");
            return;
        }
        setUserPassword(recoveryUser, newPassword);
        recoveryUser = null;
        saveLocalUsers();
        success("Password changed successfully.");
    }

    public boolean passwordMatches(User user, String plainPassword) {
        if (user == null || plainPassword == null) {
            return false;
        }
        String storedHash = user.getPasswordHash();
        return storedHash != null && !storedHash.isBlank() && storedHash.equals(hashPassword(plainPassword));
    }

    public void setUserPassword(User user, String plainPassword) {
        if (user == null || plainPassword == null) {
            return;
        }
        user.setPassword("");
        user.setPasswordHash(hashPassword(plainPassword));
    }

    /** Network-aware username change used by ProfileController. */
    public boolean renameLoggedInUser(String newUsername) {
        User user = loggedInUser;
        if (user == null) {
            fail("No user is logged in.");
            return false;
        }
        String oldUsername = user.getUsername();
        user.setUsername(newUsername);
        if (networkManager != null) {
            NetworkOperationResult result = networkManager.rename(user, newUsername);
            if (!result.successful()) {
                user.setUsername(oldUsername);
                fail(result.message());
                return false;
            }
        }
        upsertLocalUser(user, oldUsername);
        saveLocalUsers();
        success("Username changed successfully.");
        return true;
    }

    /** Network-aware password change used by ProfileController. */
    public boolean changeLoggedInPassword(String oldPassword, String newPassword) {
        User user = loggedInUser;
        if (user == null) {
            fail("No user is logged in.");
            return false;
        }
        String oldHash = user.getPasswordHash();
        setUserPassword(user, newPassword);
        if (networkManager != null) {
            NetworkOperationResult result = networkManager.changePassword(user, oldPassword, newPassword);
            if (!result.successful()) {
                user.setPasswordHash(oldHash);
                fail(result.message());
                return false;
            }
        }
        saveLocalUsers();
        success("Password changed successfully.");
        return true;
    }

    public void invalidCommand(String menuName) { fail("Invalid command in " + menuName + "."); }
    public User getLoggedInUser() { return loggedInUser; }
    public boolean usernameExists(String username) { return findUser(username) != null; }
    public String getLastMessage() { return lastMessage; }
    public boolean isLoggedIn() { return loggedInUser != null; }
    public boolean isNetworkBacked() { return networkManager != null; }
    public boolean wasSuccessful() { return lastMessage != null && lastMessage.startsWith("OK:"); }

    /**
     * Keeps a local cache for fast startup/UI while making the server the source
     * of truth whenever the main game is running in network-backed mode.
     */
    public void saveUsers() {
        saveLocalUsers();
        if (networkManager != null && loggedInUser != null && networkManager.isAuthenticated()) {
            networkManager.synchronizeAsync(loggedInUser);
            int score = Math.max(0, loggedInUser.getBestMioPoint());
            if (score > lastSubmittedMioPoint) {
                lastSubmittedMioPoint = score;
                networkManager.submitScoredGameAsync(loggedInUser, score);
            }
        }
    }

    private User restoreServerSessionIfAvailable() {
        NetworkAuthResult result = networkManager.resumeSavedSession();
        if (!result.successful() || result.user() == null) {
            return null;
        }
        User user = result.user();
        user.setStayLoggedIn(true);
        clearStayLoggedInUsers();
        upsertLocalUser(user);
        saveLocalUsers();
        return user;
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

    private boolean isValidUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9-]+");
    }

    private boolean isValidGender(String gender) {
        String normalizedGender = normalize(gender);
        return "male".equals(normalizedGender)
                || "female".equals(normalizedGender)
                || "man".equals(normalizedGender)
                || "woman".equals(normalizedGender)
                || "مرد".equals(normalizedGender)
                || "زن".equals(normalizedGender);
    }

    private String normalizeGender(String gender) {
        String normalizedGender = normalize(gender);
        if ("man".equals(normalizedGender) || "مرد".equals(normalizedGender)) {
            return "male";
        }
        if ("woman".equals(normalizedGender) || "زن".equals(normalizedGender)) {
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
            if (user != null && username.equalsIgnoreCase(user.getUsername())) {
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

    private void upsertLocalUser(User user) { upsertLocalUser(user, user == null ? null : user.getUsername()); }

    private void upsertLocalUser(User user, String oldUsername) {
        if (user == null) {
            return;
        }
        for (int index = users.size() - 1; index >= 0; index--) {
            User existing = users.get(index);
            if (existing == user) {
                users.set(index, user);
                return;
            }
            if (existing != null && (equalsIgnoreCase(existing.getUsername(), oldUsername)
                    || equalsIgnoreCase(existing.getUsername(), user.getUsername()))) {
                users.set(index, user);
                return;
            }
        }
        users.add(user);
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private void saveLocalUsers() { saveManager.saveAllUsers(users); }

    private String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                builder.append(String.format("%02x", hashByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private String securityQuestionText() {
        StringBuilder text = new StringBuilder("Choose a security question:\n");
        for (int i = 0; i < SECURITY_QUESTIONS.length; i++) {
            text.append(i + 1).append(". ").append(SECURITY_QUESTIONS[i]).append("\n");
        }
        text.append("pick question -q <question_number> -a <answer> -c <answer_confirm>");
        return text.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void success(String message) { lastMessage = "OK: " + message; }
    private void fail(String message) { lastMessage = "ERROR: " + message; }
}
