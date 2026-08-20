package screens.menu;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.pvz.Main;
import controllers.auth.AuthController;

public class LoginScreen extends BaseMenuScreen {
    private final AuthController controller;
    private final String initialMessage;
    private TextField usernameField;
    private TextField passwordField;
    private CheckBox stayLoggedInBox;

    public LoginScreen(Main game, String initialMessage) {
        super(game);
        controller = game.getAuthController();
        this.initialMessage = initialMessage == null ? "" : initialMessage;
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!initialMessage.isBlank()) {
            showControllerMessage(initialMessage);
        }
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        Table panel = createPanel();
        usernameField = createField("Username");
        passwordField = createPasswordField();
        stayLoggedInBox = new CheckBox(" Stay logged in", skin, "default");
        addForm(panel);
        root.add(panel).width(560f);
    }

    private TextField createPasswordField() {
        TextField field = createField("Password");
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
        return field;
    }

    private void addForm(Table panel) {
        panel.defaults().padBottom(12f);
        panel.add(createTitle("Plants vs. Zombies")).width(500f).padBottom(18f).row();
        panel.add(usernameField).width(420f).height(52f).row();
        panel.add(passwordField).width(420f).height(52f).row();
        panel.add(stayLoggedInBox).left().width(420f).row();
        panel.add(createLoginButton()).width(260f).height(54f).row();
        panel.add(createRegisterButton()).width(260f).height(50f).row();
        panel.add(createForgotButton()).width(260f).height(50f);
    }

    private TextButton createLoginButton() {
        return new ui.MenuButton("Login", skin, this::login);
    }

    private TextButton createRegisterButton() {
        return new ui.MenuButton("Register", skin, game.getScreenManager()::showRegister);
    }

    private TextButton createForgotButton() {
        return new ui.MenuButton("Forgot Password", skin, game.getScreenManager()::showForgotPassword);
    }

    private void login() {
        controller.login(usernameField.getText().trim(), passwordField.getText(), stayLoggedInBox.isChecked());
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            return;
        }
        game.getScreenManager().showMainMenu();
    }
}
