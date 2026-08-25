package game.input;

public final class InteractionValidation {
    private static final InteractionValidation VALID = new InteractionValidation(true, "Ready");

    private final boolean valid;
    private final String message;

    private InteractionValidation(boolean valid, String message) {
        this.valid = valid;
        this.message = message == null ? "" : message;
    }

    public static InteractionValidation valid() {
        return VALID;
    }

    public static InteractionValidation invalid(String message) {
        return new InteractionValidation(false, message);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }
}
