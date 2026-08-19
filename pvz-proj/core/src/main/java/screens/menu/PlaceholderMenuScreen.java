package screens.menu;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pvz.Main;
import ui.BackButton;

public class PlaceholderMenuScreen extends BaseMenuScreen {
    private final String title;

    public PlaceholderMenuScreen(Main game, String title) {
        super(game);
        this.title = title == null ? "Menu" : title;
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshResourceBar();
    }

    private void buildUi() {
        Table root = createRoot();
        addResourceBar(root);
        Table panel = createPanel();
        panel.add(createTitle(title)).padBottom(20f).row();
        panel.add(createLabel("This screen belongs to the next assigned issue.")).padBottom(18f).row();
        panel.add(new BackButton(skin, game.getScreenManager()::showMainMenu)).width(220f).height(48f);
        root.add(panel).expand().center();
    }
}
