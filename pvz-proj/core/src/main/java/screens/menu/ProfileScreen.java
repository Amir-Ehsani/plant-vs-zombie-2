package screens.menu;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.pvz.Main;
import controllers.features.ProfileController;
import models.account.User;
import ui.BackButton;
import ui.MenuButton;
import ui.NotificationManager;

public class ProfileScreen extends BaseMenuScreen {
    private final ProfileController controller;
    private Label usernameValue;
    private Label nicknameValue;
    private Label gamesValue;
    private Label coinsValue;
    private Label gemsValue;
    private Label levelsValue;
    private Label mioValue;

    public ProfileScreen(Main game) {
        super(game);
        controller = game.getProfileController();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshProfile();
    }

    private void buildUi() {
        Table root = createRoot();
        addResourceBar(root);
        Table content = createPanel();
        content.defaults().padBottom(8f);
        content.add(createTitle("Profile")).colspan(3).padBottom(16f).row();
        addInfoRows(content);
        addEditors(content);
        content.add(new BackButton(skin, game.getScreenManager()::showMainMenu))
                .colspan(3).width(220f).height(48f).padTop(12f);
        ScrollPane scrollPane = new ScrollPane(content, skin);
        scrollPane.setFadeScrollBars(false);
        root.add(scrollPane).width(940f).height(620f);
    }

    private void addInfoRows(Table table) {
        usernameValue = addInfoRow(table, "Username");
        nicknameValue = addInfoRow(table, "Nickname");
        gamesValue = addInfoRow(table, "Games Played");
        coinsValue = addInfoRow(table, "Coins");
        gemsValue = addInfoRow(table, "Diamonds");
        levelsValue = addInfoRow(table, "Completed Levels");
        mioValue = addInfoRow(table, "Best MioPoint");
    }

    private Label addInfoRow(Table table, String name) {
        Label value = createLabel("-");
        table.add(createLabel(name)).left().width(220f);
        table.add(value).left().colspan(2).width(580f).row();
        return value;
    }

    private void addEditors(Table table) {
        addTextEditor(table, "Change Username", "New username", this::changeUsername);
        addTextEditor(table, "Change Nickname", "New nickname", this::changeNickname);
        addTextEditor(table, "Change Email", "New email", this::changeEmail);
        addPasswordEditor(table);
    }

    private void addTextEditor(Table table, String label, String hint, TextAction action) {
        TextField field = createField(hint);
        table.add(createLabel(label)).left().width(220f);
        table.add(field).width(420f).height(46f);
        table.add(new MenuButton("Save", skin, () -> action.run(field.getText().trim())))
                .width(150f).height(46f).row();
    }

    private void addPasswordEditor(Table table) {
        TextField oldField = passwordField("Old password");
        TextField newField = passwordField("New password");
        TextField confirmField = passwordField("Confirm new password");
        Table fields = new Table();
        fields.defaults().padBottom(6f);
        fields.add(oldField).width(420f).height(46f).row();
        fields.add(newField).width(420f).height(46f).row();
        fields.add(confirmField).width(420f).height(46f);
        table.add(createLabel("Change Password")).left().width(220f);
        table.add(fields).width(440f);
        table.add(new MenuButton("Save", skin, () -> changePassword(oldField, newField, confirmField)))
                .width(150f).height(46f).row();
    }

    private TextField passwordField(String hint) {
        TextField field = createField(hint);
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
        return field;
    }

    private void changeUsername(String value) {
        controller.changeUsername(value);
        handleResult();
    }

    private void changeNickname(String value) {
        controller.changeNickname(value);
        handleResult();
    }

    private void changeEmail(String value) {
        controller.changeEmail(value);
        handleResult();
    }

    private void changePassword(TextField oldField, TextField newField, TextField confirmField) {
        if (!newField.getText().equals(confirmField.getText())) {
            NotificationManager.showError("Password confirmation does not match.");
            return;
        }
        controller.changePassword(newField.getText(), oldField.getText());
        handleResult();
        if (controller.wasSuccessful()) {
            oldField.setText("");
            newField.setText("");
            confirmField.setText("");
        }
    }

    private void handleResult() {
        showControllerMessage(controller.getLastMessage());
        if (controller.wasSuccessful()) {
            refreshProfile();
        }
    }

    private void refreshProfile() {
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            return;
        }
        usernameValue.setText(user.getUsername());
        nicknameValue.setText(user.getNickname());
        gamesValue.setText(String.valueOf(user.getGamesPlayed()));
        coinsValue.setText(String.valueOf(user.getCoins()));
        gemsValue.setText(String.valueOf(user.getGems()));
        levelsValue.setText(String.valueOf(user.getPassedLevels()));
        mioValue.setText(String.valueOf(user.getBestMioPoint()));
        refreshResourceBar();
    }

    private interface TextAction {
        void run(String value);
    }
}
