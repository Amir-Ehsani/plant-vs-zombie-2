package controllers.auth;

import models.account.User;

public class AuthController {
    private User loggedInUser;

    private PasswordValidator passwordValidator;
    private EmailValidator emailValidator;

    public void register(String username, String password, String email) {}
    public void login(String username, String password) {}
}
