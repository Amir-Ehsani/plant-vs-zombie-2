package controllers.features;

import controllers.auth.AuthController;
import models.account.User;
import models.level.core.AdventureLevelCatalog;

public class ChapterLevelController {
    private final AuthController authController;
    private String lastMessage;

    public ChapterLevelController(AuthController authController) {
        this.authController = authController;
        this.lastMessage = "";
    }

    public void showLevels() {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }

        String chapterName = AdventureLevelCatalog.normalizeChapterName(user.getCurrentChapterName());
        if (!AdventureLevelCatalog.chapterExists(chapterName)) {
            fail("No adventure chapter is selected.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Chapter Levels - ")
                .append(AdventureLevelCatalog.displayChapterName(chapterName))
                .append("\n================================");

        for (int levelNumber = 1; levelNumber <= AdventureLevelCatalog.BOSS_LEVEL; levelNumber++) {
            builder.append("\n")
                    .append(levelNumber)
                    .append(". ")
                    .append(AdventureLevelCatalog.levelTitle(chapterName, levelNumber))
                    .append(" | ")
                    .append(AdventureLevelCatalog.levelKind(chapterName, levelNumber))
                    .append(" | ")
                    .append(levelStatus(user, chapterName, levelNumber));
        }

        builder.append("\n\nCommands:")
                .append("\nshow levels")
                .append("\nselect level -l <1|2|3|4>")
                .append("\nmenu show current")
                .append("\nmenu exit")
                .append("\n\nP2-09 boss battles are available for Ancient Egypt and Ice Cave.");

        success(builder.toString());
    }

    public void selectLevel(int levelNumber) {
        User user = getLoggedInUserOrFail();
        if (user == null) {
            return;
        }

        String chapterName = AdventureLevelCatalog.normalizeChapterName(user.getCurrentChapterName());
        if (!AdventureLevelCatalog.chapterExists(chapterName)) {
            fail("No adventure chapter is selected.");
            return;
        }

        if (!AdventureLevelCatalog.isPlayableLevel(levelNumber)) {
            fail("Level number must be between 1 and 4.");
            return;
        }

        if (levelNumber == AdventureLevelCatalog.BOSS_LEVEL
                && !AdventureLevelCatalog.isBossLevelAvailable(chapterName)) {
            fail("This chapter boss is not available.");
            return;
        }

        if (!user.isChapterLevelUnlocked(chapterName, levelNumber)) {
            fail("Level " + levelNumber + " is locked. Complete level " + (levelNumber - 1) + " first.");
            return;
        }

        user.setCurrentChapterLevel(levelNumber);
        authController.saveUsers();
        success("Selected " + AdventureLevelCatalog.displayChapterName(chapterName)
                + " level " + levelNumber + ": "
                + AdventureLevelCatalog.levelTitle(chapterName, levelNumber) + ".");
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void invalidCommand() {
        fail("Invalid command in chapter level menu.");
    }

    private String levelStatus(User user, String chapterName, int levelNumber) {
        if (levelNumber == AdventureLevelCatalog.BOSS_LEVEL
                && !AdventureLevelCatalog.isBossLevelAvailable(chapterName)) {
            return "LOCKED";
        }
        if (user.isChapterLevelCompleted(chapterName, levelNumber)) {
            return "COMPLETED";
        }
        return user.isChapterLevelUnlocked(chapterName, levelNumber) ? "UNLOCKED" : "LOCKED";
    }

    private User getLoggedInUserOrFail() {
        User user = authController == null ? null : authController.getLoggedInUser();
        if (user == null) {
            fail("No user is logged in.");
            return null;
        }
        return user;
    }

    private void success(String message) {
        lastMessage = "OK: " + message;
    }

    private void fail(String message) {
        lastMessage = "ERROR: " + message;
    }
}
