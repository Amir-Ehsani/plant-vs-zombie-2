package controllers.auth;

public class EmailValidator {
    private String lastMessage;

    public EmailValidator() {
        this.lastMessage = "";
    }

    public void isValid(String email) {
        if (email == null || email.isEmpty()) {
            fail("Email cannot be empty.");
            return;
        }

        int atIndex = email.indexOf('@');

        if (atIndex == -1) {
            fail("Email must contain @.");
            return;
        }

        if (atIndex != email.lastIndexOf('@')) {
            fail("Email must contain only one @.");
            return;
        }

        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        if (username.isEmpty()) {
            fail("Email username cannot be empty.");
            return;
        }

        if (domain.isEmpty()) {
            fail("Email domain cannot be empty.");
            return;
        }

        if (!Character.isLetterOrDigit(username.charAt(0))
                || !Character.isLetterOrDigit(username.charAt(username.length() - 1))) {
            fail("Email username must start and end with a letter or digit.");
            return;
        }

        if (username.contains("..")) {
            fail("Email username cannot contain consecutive dots.");
            return;
        }

        if (!username.matches("[a-zA-Z0-9._-]+")) {
            fail("Email username contains invalid characters.");
            return;
        }

        if (!Character.isLetterOrDigit(domain.charAt(0))
                || !Character.isLetterOrDigit(domain.charAt(domain.length() - 1))) {
            fail("Email domain must start and end with a letter or digit.");
            return;
        }

        if (!domain.contains(".")) {
            fail("Email domain must contain a dot.");
            return;
        }

        if (domain.contains("..")) {
            fail("Email domain cannot contain consecutive dots.");
            return;
        }

        if (!domain.matches("[a-zA-Z0-9.-]+")) {
            fail("Email domain contains invalid characters.");
            return;
        }

        String domainSuffix = domain.substring(domain.lastIndexOf('.') + 1);

        if (domainSuffix.length() < 2) {
            fail("Email domain suffix must be at least two letters.");
            return;
        }

        if (!domainSuffix.matches("[a-zA-Z]+")) {
            fail("Email domain suffix must only contain letters.");
            return;
        }

        success("Email is valid.");
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