package controllers.features;

import controllers.auth.AuthController;
import models.account.User;

import java.util.Locale;

public class GameMenuController {
    private final AuthController authController;
    private String lastMessage;

    public GameMenuController(AuthController authController) {
        this.authController = authController;
        this.lastMessage = "";
    }

    public void enterChapter(String chapterName) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        if (chapterName == null || chapterName.isBlank()) {
            fail("Chapter name is required.");
            return;
        }

        //add logic for enter chapter (in user)
        success("Entered chapter " + chapterName.trim() + ".");
    }

    public void showCoinWallet() {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        success("Coins: " + user.getCoins());
    }

    public void showGemWallet() {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        success("Gems: " + user.getGems());
    }

    public void addCheatCurrency(int amount, String currency) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        if (amount <= 0) {
            fail("Amount must be positive.");
            return;
        }

        String normalizedCurrency = normalize(currency);

        if ("coins".equals(normalizedCurrency)) {
            user.addCoins(amount);
            authController.saveUsers();
            success(amount + " coins added.");
            return;
        }

        if ("gems".equals(normalizedCurrency)) {
            user.addGems(amount);
            authController.saveUsers();
            success(amount + " gems added.");
            return;
        }

        fail("Unknown currency.");
    }

    public void enterGreenhouse() {
        //implement greenhouse menu
        fail("Greenhouse menu is not implemented yet.");
    }

    public void enterTravelLog() {
        //implement travel log menu
        fail("Travel log menu is not implemented yet.");
    }

    public boolean isLoggedIn() {
        return authController.isLoggedIn();
    }

    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    private User getLoggedInUserOrFail() {
        User user = authController.getLoggedInUser();

        if (user == null) {
            fail("No user is logged in.");
            return null;
        }

        return user;
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