package controllers.auth;

import models.Result;

public class EmailValidator {
    public static Result isValid(String email) {
        if (email.isEmpty()) {
            return new Result(false, "Email cannot be empty.");
        }

        int atIndex = email.indexOf('@');

        if (atIndex == -1) {
            return new Result(false, "Email must contain @.");
        }

        if (atIndex != email.lastIndexOf('@')) {
            return new Result(false, "Email must contain only one @.");
        }

        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        if (username.isEmpty()) {
            return new Result(false, "Email username cannot be empty.");
        }

        if (domain.isEmpty()) {
            return new Result(false, "Email domain cannot be empty.");
        }

        if (!Character.isLetterOrDigit(username.charAt(0))
                || !Character.isLetterOrDigit(username.charAt(username.length() - 1))) {
            return new Result(false, "Email username must start and end with a letter or digit.");
        }

        if (username.contains("..")) {
            return new Result(false, "Email username cannot contain consecutive dots.");
        }

        if (!username.matches("[a-zA-Z0-9._-]+")) {
            return new Result(false, "Email username contains invalid characters.");
        }

        if (!Character.isLetterOrDigit(domain.charAt(0))
                || !Character.isLetterOrDigit(domain.charAt(domain.length() - 1))) {
            return new Result(false, "Email domain must start and end with a letter or digit.");
        }

        if (!domain.contains(".")) {
            return new Result(false, "Email domain must contain a dot.");
        }

        if (domain.contains("..")) {
            return new Result(false, "Email domain cannot contain consecutive dots.");
        }

        if (!domain.matches("[a-zA-Z0-9.-]+")) {
            return new Result(false, "Email domain contains invalid characters.");
        }

        String domainSuffix = domain.substring(domain.lastIndexOf('.') + 1);

        if (domainSuffix.length() < 2) {
            return new Result(false, "Email domain suffix must be at least two letters.");
        }

        if (!domainSuffix.matches("[a-zA-Z]+")) {
            return new Result(false, "Email domain suffix must only contain letters.");
        }

        return new Result(true, "");
    }
}