package controllers.auth;

import models.Result;

public class PasswordValidator {
    private static final String ALLOWED_PASSWORD_REGEX =
            "^[a-zA-Z0-9!#$%^&*()=+{}\\[\\]|/\\\\:;'\",><?]+$";

    public static Result isValid(String password) {
        if (password.isEmpty()) {
            return new Result(false, "Password cannot be empty.");
        }

        if (!password.matches(ALLOWED_PASSWORD_REGEX)) {
            return new Result(false, "Password contains invalid characters.");
        }

        if (password.length() < 8) {
            return new Result(false, "Password is weak: it must be at least 8 characters long.");
        }

        if (!password.matches(".*[a-z].*")) {
            return new Result(false, "Password is weak: it must contain at least one lowercase letter.");
        }

        if (!password.matches(".*[A-Z].*")) {
            return new Result(false, "Password is weak: it must contain at least one uppercase letter.");
        }

        if (!password.matches(".*[0-9].*")) {
            return new Result(false, "Password is weak: it must contain at least one digit.");
        }

        if (!password.matches(".*[!#$%^&*()=+{}\\[\\]|/\\\\:;'\",><?].*")) {
            return new Result(false, "Password is weak: it must contain at least one special character.");
        }

        return new Result(true, "");
    }

}