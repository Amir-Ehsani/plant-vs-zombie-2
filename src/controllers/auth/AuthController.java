package controllers.auth;

import controllers.core.SaveManager;
import models.Result;
import models.account.User;

import java.util.ArrayList;

public class AuthController {
    private static final String[] SECURITY_QUESTIONS = {
            "What was the name of your first pet?",
            "What is your favorite plant?",
            "What city were you born in?"
    };

    private static final SaveManager saveManager = new SaveManager();
    private static final ArrayList<User> USERS = new ArrayList<>(saveManager.loadAllUsers());
    private static User loggedInUser;
    private static User pendingUser;

    public static Result register(String username, String password, String passwordConfirm, String nickname, String email, String gender) {
        if (!validUsername(username)) {
            return new Result(false, "invalid username");
        }
        if (findUser(username) != null) {
            return new Result(false, "username already exists");
        }
        if (!password.equals(passwordConfirm)) {
            return new Result(false, "password confirmation does not match");
        }
        if (!PasswordValidator.isValid(password).success()) {
            return new Result(false, PasswordValidator.isValid(password).message());
        }
        if (!validNickname(nickname)) {
            return new Result(false, "invalid nickname");
        }
        if (!EmailValidator.isValid(email).success()) {
            return new Result(false, EmailValidator.isValid(email).message());
        }
        if (!(gender.equals("male") || gender.equals("female"))) {
            return new Result(false, "invalid gender");
        }
        pendingUser = new User(username, password, nickname, email, gender);
        //if valid registeration return security questions text
        return new Result(true, securityQuestionText());
    }

    public static Result pickQuestion(int questionNumber, String answer, String answerConfirm) {
        if (questionNumber < 1 || questionNumber > SECURITY_QUESTIONS.length)
            return new Result(false, "invalid security question number");

        if (answer == null || answer.trim().isEmpty())
            return new Result(false, "security answer cannot be empty");

        if (!answer.equals(answerConfirm))
            return new Result(false, "security answer confirmation does not match");

        pendingUser.setSecurityQuestionNumber(questionNumber);
        pendingUser.setSecurityAnswer(answer);
        USERS.add(pendingUser);
        pendingUser = null;
        saveManager.saveAllUsers(USERS);
        return new Result(true, "registered successfully");
    }

    public static Result login(String username, String password) {
        User user = findUser(username);
        if (user == null)
            return new Result(false, "username not found");

        if (!user.getPassword().equals(password))
            return new Result(false, "password incorrect");

        loggedInUser = user;
        return new Result(true, "logged in successfully");
    }

    public static User getLoggedInUser() {
        return loggedInUser;
    }

    private static boolean validUsername(String username) {
        return username.matches("[A-Za-z0-9-]+");
    }

    private static boolean validNickname(String nickname) {
        return nickname.length() >= 3 && nickname.length() <= 30;
    }

    private static User findUser(String username) {
        for (User user : USERS) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    //methood that just  returns security questions text
    private static String securityQuestionText() {
        StringBuilder text = new StringBuilder();
        text.append("choose a security question:\n");

        int i = 1;
        for (String q : SECURITY_QUESTIONS) {
            text.append(i).append(". ").append(q).append("\n");
            i++;
        }
        text.append("pick question -q <question_number> -a <answer> -c <answer_confirm>");
        return text.toString();
    }

}
