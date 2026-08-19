package screens.menu;

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.pvz.Main;
import controllers.auth.AuthController;
import ui.BackButton;
import ui.MenuButton;
import ui.NotificationManager;

public class RegisterScreen extends BaseMenuScreen {
    private static final String[] SECURITY_QUESTIONS = {
            "What was the name of your first pet?",
            "What is your favorite plant?",
            "What city were you born in?"
    };
    private final AuthController controller;
    private TextField usernameField;
    private TextField passwordField;
    private TextField confirmPasswordField;
    private TextField nicknameField;
    private TextField emailField;
    private TextField answerField;
    private TextField confirmAnswerField;
    private SelectBox<String> genderBox;
    private SelectBox<String> questionBox;

    public RegisterScreen(Main game) {
        super(game);
        controller = game.getAuthController();
        buildUi();
    }

    private void buildUi() {
        Table root = createRoot();
        Table form = createPanel();
        createFields();
        fillForm(form);
        ScrollPane scrollPane = new ScrollPane(form, skin);
        scrollPane.setFadeScrollBars(false);
        root.add(scrollPane).width(800f).height(660f);
    }

    private void createFields() {
        usernameField = createField("Username");
        passwordField = passwordField("Password");
        confirmPasswordField = passwordField("Confirm password");
        nicknameField = createField("Nickname");
        emailField = createField("Email");
        answerField = createField("Security answer");
        confirmAnswerField = createField("Confirm security answer");
        genderBox = new SelectBox<>(skin);
        genderBox.setItems("male", "female");
        questionBox = new SelectBox<>(skin);
        questionBox.setItems(SECURITY_QUESTIONS);
    }

    private TextField passwordField(String message) {
        TextField field = createField(message);
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
        return field;
    }

    private void fillForm(Table form) {
        form.defaults().padBottom(10f);
        form.add(createTitle("Create Account")).colspan(2).padBottom(18f).row();
        addFieldRow(form, "Username", usernameField);
        addFieldRow(form, "Password", passwordField);
        addFieldRow(form, "Confirm Password", confirmPasswordField);
        addFieldRow(form, "Nickname", nicknameField);
        addFieldRow(form, "Email", emailField);
        addSelectRow(form, "Gender", genderBox);
        addSelectRow(form, "Security Question", questionBox);
        addFieldRow(form, "Security Answer", answerField);
        addFieldRow(form, "Confirm Answer", confirmAnswerField);
        addActions(form);
    }

    private void addFieldRow(Table table, String label, TextField field) {
        table.add(createLabel(label)).left().width(210f);
        table.add(field).width(430f).height(48f).row();
    }

    private void addSelectRow(Table table, String label, SelectBox<String> box) {
        table.add(createLabel(label)).left().width(210f);
        table.add(box).width(430f).height(48f).row();
    }

    private void addActions(Table form) {
        form.add(new MenuButton("Register", skin, this::register)).colspan(2).width(240f).height(52f).row();
        form.add(new BackButton(skin, game.getScreenManager()::showLogin))
                .colspan(2).width(240f).height(48f);
    }

    private void register() {
        if (!validateSecurityAnswer()) {
            return;
        }
        controller.register(
                usernameField.getText().trim(),
                passwordField.getText(),
                confirmPasswordField.getText(),
                nicknameField.getText().trim(),
                emailField.getText().trim(),
                genderBox.getSelected()
        );
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            return;
        }
        completeRegistration();
    }

    private boolean validateSecurityAnswer() {
        if (answerField.getText().isBlank()) {
            NotificationManager.showError("Security answer cannot be empty.");
            return false;
        }
        if (!answerField.getText().equals(confirmAnswerField.getText())) {
            NotificationManager.showError("Security answer confirmation does not match.");
            return false;
        }
        return true;
    }

    private void completeRegistration() {
        controller.pickQuestion(
                questionBox.getSelectedIndex() + 1,
                answerField.getText(),
                confirmAnswerField.getText()
        );
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            return;
        }
        game.getScreenManager().showLogin(controller.getLastMessage());
    }
}
