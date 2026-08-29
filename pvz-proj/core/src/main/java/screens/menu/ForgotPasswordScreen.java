package screens.menu;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.pvz.Main;
import controllers.auth.AuthController;
import ui.BackButton;
import ui.MenuButton;
import ui.NotificationManager;

public class ForgotPasswordScreen extends BaseMenuScreen {
    private final AuthController controller;
    private TextField usernameField;
    private TextField emailField;
    private TextField answerField;
    private TextField newPasswordField;
    private TextField confirmPasswordField;
    private Label questionLabel;
    private MenuButton answerButton;
    private MenuButton resetButton;

    public ForgotPasswordScreen(Main game) {
        super(game);
        controller = game.getAuthController();
        buildUi();
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        Table panel = createPanel();
        createFields();
        fillPanel(panel);
        root.add(panel).width(700f);
    }

    private void createFields() {
        usernameField = createField("Username");
        emailField = createField("Email");
        answerField = createField("Security answer");
        newPasswordField = passwordField("New password");
        confirmPasswordField = passwordField("Confirm new password");
        answerField.setDisabled(true);
        newPasswordField.setDisabled(true);
        confirmPasswordField.setDisabled(true);
    }

    private TextField passwordField(String message) {
        TextField field = createField(message);
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
        return field;
    }

    private void fillPanel(Table panel) {
        panel.defaults().padBottom(10f);
        panel.add(createTitle("Password Recovery")).width(620f).padBottom(18f).row();
        panel.add(usernameField).width(430f).height(48f).row();
        panel.add(emailField).width(430f).height(48f).row();
        panel.add(new MenuButton("Find Account", skin, this::findAccount)).width(220f).height(48f).row();
        questionLabel = createLabel("Security question will appear here.");
        questionLabel.setWrap(true);
        panel.add(questionLabel).width(560f).padTop(8f).row();
        panel.add(answerField).width(430f).height(48f).row();
        answerButton = new MenuButton("Verify Answer", skin, this::verifyAnswer);
        answerButton.setDisabled(true);
        panel.add(answerButton).width(220f).height(48f).row();
        panel.add(newPasswordField).width(430f).height(48f).row();
        panel.add(confirmPasswordField).width(430f).height(48f).row();
        resetButton = new MenuButton("Reset Password", skin, this::resetPassword);
        resetButton.setDisabled(true);
        panel.add(resetButton).width(220f).height(48f).row();
        panel.add(new BackButton(skin, game.getScreenManager()::showLogin)).width(220f).height(46f);
    }

    private void findAccount() {
        controller.forgetPassword(usernameField.getText().trim(), emailField.getText().trim());
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            return;
        }
        questionLabel.setText(stripPrefix(controller.getLastMessage()));
        answerField.setDisabled(false);
        answerButton.setDisabled(false);
    }

    private void verifyAnswer() {
        controller.answerSecurityQuestion(answerField.getText());
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            resetRecoveryStep();
            return;
        }
        newPasswordField.setDisabled(false);
        confirmPasswordField.setDisabled(false);
        resetButton.setDisabled(false);
        showControllerMessage(controller.getLastMessage());
    }

    private void resetRecoveryStep() {
        answerField.setText("");
        answerField.setDisabled(true);
        answerButton.setDisabled(true);
        newPasswordField.setDisabled(true);
        confirmPasswordField.setDisabled(true);
        resetButton.setDisabled(true);
        questionLabel.setText("Security question will appear here.");
    }

    private void resetPassword() {
        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
            NotificationManager.showError("Password confirmation does not match.");
            return;
        }
        controller.resetForgottenPassword(newPasswordField.getText());
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            return;
        }
        game.getScreenManager().showLogin(controller.getLastMessage());
    }
}
