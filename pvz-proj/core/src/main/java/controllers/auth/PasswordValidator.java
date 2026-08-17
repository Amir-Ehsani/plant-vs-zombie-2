package controllers.auth;

public class PasswordValidator {
    private static final String ALLOWED_PASSWORD_REGEX =
            "^[a-zA-Z0-9!#$%^&*()=+{}\\[\\]|/\\\\:;'\",><?]+$";

    private String lastMessage;

    public PasswordValidator() {
        this.lastMessage = "";
    }

    public void isValid(String password) {
        if (password == null || password.isEmpty()) {
            fail("Password cannot be empty.");
            return;
        }

        if (!password.matches(ALLOWED_PASSWORD_REGEX)) {
            fail("Password contains invalid characters.");
            return;
        }

        if (password.length() < 8) {
            fail("Password is weak: it must be at least 8 characters long.");
            return;
        }

        if (!password.matches(".*[a-z].*")) {
            fail("Password is weak: it must contain at least one lowercase letter.");
            return;
        }

        if (!password.matches(".*[A-Z].*")) {
            fail("Password is weak: it must contain at least one uppercase letter.");
            return;
        }

        if (!password.matches(".*[0-9].*")) {
            fail("Password is weak: it must contain at least one digit.");
            return;
        }

        if (!password.matches(".*[!#$%^&*()=+{}\\[\\]|/\\\\:;'\",><?].*")) {
            fail("Password is weak: it must contain at least one special character.");
            return;
        }

        success("Password is valid.");
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }
}