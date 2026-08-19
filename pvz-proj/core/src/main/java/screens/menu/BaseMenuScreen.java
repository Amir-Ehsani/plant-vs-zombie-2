package screens.menu;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.pvz.Main;
import models.account.User;
import pvz.skin.BorderedTable;
import screens.BaseScreen;
import ui.MenuButton;
import ui.NotificationManager;
import ui.ResourceBar;

public abstract class BaseMenuScreen extends BaseScreen {
    protected final Skin skin;
    protected ResourceBar resourceBar;

    protected BaseMenuScreen(Main game) {
        super(game);
        skin = game.getSkin();
    }

    protected Table createRoot() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(24f);
        stage.addActor(root);
        return root;
    }

    protected Table createPanel() {
        BorderedTable panel = new BorderedTable();
        panel.pad(30f);
        return panel;
    }

    protected Label createTitle(String text) {
        Label label = new Label(text == null ? "" : text, skin, "big_outline");
        label.setAlignment(Align.center);
        return label;
    }

    protected Label createLabel(String text) {
        return new Label(text == null ? "" : text, skin, "default");
    }

    protected Label createSecondaryLabel(String text) {
        return new Label(text == null ? "" : text, skin, "secondary");
    }

    protected TextButton createButton(String text) {
        return new MenuButton(text, skin, "green", null);
    }

    protected TextField createField(String messageText) {
        TextField field = new TextField("", skin, "default");
        field.setMessageText(messageText == null ? "" : messageText);
        return field;
    }

    protected ResourceBar addResourceBar(Table root) {
        resourceBar = new ResourceBar(skin);
        refreshResourceBar();
        root.add(resourceBar).right().padBottom(12f).row();
        return resourceBar;
    }

    protected void refreshResourceBar() {
        if (resourceBar == null) {
            return;
        }
        User user = game.getAuthController().getLoggedInUser();
        resourceBar.refresh(user);
    }

    protected boolean requireLoggedIn() {
        if (game.getAuthController().isLoggedIn()) {
            return true;
        }
        game.getScreenManager().showLogin("ERROR: Please login first.");
        return false;
    }

    protected void showControllerMessage(String controllerMessage) {
        String message = controllerMessage == null ? "" : controllerMessage.trim();
        if (message.startsWith("ERROR:")) {
            NotificationManager.showError(stripPrefix(message));
            return;
        }
        if (message.startsWith("OK:")) {
            NotificationManager.showSuccess(stripPrefix(message));
            return;
        }
        NotificationManager.showInfo(message);
    }

    protected String stripPrefix(String message) {
        if (message == null) {
            return "";
        }
        if (message.startsWith("OK: ")) {
            return message.substring(4);
        }
        if (message.startsWith("ERROR: ")) {
            return message.substring(7);
        }
        return message;
    }
}
