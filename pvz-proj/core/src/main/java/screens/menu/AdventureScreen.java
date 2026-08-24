package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.pvz.Main;
import models.account.User;
import models.level.core.AdventureLevelCatalog;
import ui.BackButton;
import ui.MenuButton;

public class AdventureScreen extends BaseMenuScreen {
    private static final Color TEXT_COLOR = Color.valueOf("4A3A1F");
    private final Table chapterList;

    public AdventureScreen(Main game) {
        super(game);
        chapterList = new Table();
        chapterList.top();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshChapters();
        refreshResourceBar();
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        addResourceBar(root);
        Table panel = createPanel();
        Label screenTitle = createTitle("Adventure");
        screenTitle.setColor(TEXT_COLOR);
        panel.add(screenTitle).padBottom(6f).row();
        Label subtitle = panelLabel("Choose a chapter and level");
        subtitle.setAlignment(Align.center);
        panel.add(subtitle).padBottom(12f).row();
        ScrollPane scrollPane = new ScrollPane(chapterList, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);
        panel.add(scrollPane).width(1080f).height(500f).row();
        panel.add(new BackButton(skin, game.getScreenManager()::showMainMenu))
                .width(180f).height(44f).padTop(10f);
        root.add(panel).width(1160f).height(650f);
    }

    private void refreshChapters() {
        chapterList.clearChildren();
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            return;
        }
        for (String chapterName : AdventureLevelCatalog.getChapterNames()) {
            chapterList.add(createChapterCard(user, chapterName)).width(1020f).padBottom(12f).row();
        }
    }

    private Table createChapterCard(User user, String chapterName) {
        Table card = createPanel();
        card.pad(18f);
        boolean unlocked = user.isChapterUnlocked(chapterName);
        Label name = new Label(AdventureLevelCatalog.displayChapterName(chapterName), skin, "medium_outline");
        name.setColor(TEXT_COLOR);
        Label progress = panelLabel(
                "Completed Levels: " + completedLevelCount(user, chapterName)
                        + " / " + AdventureLevelCatalog.BOSS_LEVEL
        );
        Label status = panelLabel(unlocked ? "UNLOCKED" : "LOCKED");
        Table header = new Table();
        header.add(name).left();
        header.add().expandX().fillX();
        header.add(progress).right().padRight(16f);
        header.add(status).right();
        card.add(header).growX().padBottom(10f).row();
        Table levels = new Table();
        levels.defaults().padRight(8f);
        for (int levelNumber = 1; levelNumber <= AdventureLevelCatalog.BOSS_LEVEL; levelNumber++) {
            levels.add(createLevelCard(user, chapterName, levelNumber)).width(235f).height(145f);
        }
        card.add(levels).left();
        return card;
    }

    private Table createLevelCard(User user, String chapterName, int levelNumber) {
        Table levelCard = createPanel();
        levelCard.pad(10f);
        boolean boss = levelNumber == AdventureLevelCatalog.BOSS_LEVEL;
        boolean unlocked = !boss && user.isChapterLevelUnlocked(chapterName, levelNumber);
        boolean completed = !boss && user.isChapterLevelCompleted(chapterName, levelNumber);
        Label number = new Label("Level " + levelNumber, skin, "medium_outline");
        number.setColor(TEXT_COLOR);
        Label type = panelLabel(AdventureLevelCatalog.levelKind(chapterName, levelNumber));
        Label title = panelLabel(AdventureLevelCatalog.levelTitle(chapterName, levelNumber));
        title.setWrap(true);
        title.setAlignment(Align.center);
        String statusText = boss ? "LOCKED | NOT IMPLEMENTED" : completed ? "COMPLETED" : unlocked ? "UNLOCKED" : "LOCKED";
        Label status = panelLabel(statusText);
        status.setAlignment(Align.center);
        levelCard.add(number).row();
        levelCard.add(type).padTop(2f).row();
        levelCard.add(title).width(205f).height(38f).padTop(3f).row();
        levelCard.add(status).padTop(2f).row();
        MenuButton button = new MenuButton(
                boss ? "Coming Soon" : completed ? "Play Again" : "Play",
                skin,
                unlocked ? "green_small" : "brown",
                unlocked ? () -> game.getScreenManager().showAdventureMission(chapterName, levelNumber) : null
        );
        button.setDisabled(!unlocked);
        levelCard.add(button).width(130f).height(32f).padTop(5f);
        return levelCard;
    }

    private int completedLevelCount(User user, String chapterName) {
        int completed = 0;
        for (int level = AdventureLevelCatalog.FIRST_PLAYABLE_LEVEL;
             level <= AdventureLevelCatalog.LAST_PLAYABLE_LEVEL;
             level++) {
            if (user.isChapterLevelCompleted(chapterName, level)) {
                completed++;
            }
        }
        return completed;
    }

    private Label panelLabel(String text) {
        Label label = new Label(text == null ? "" : text, skin, "secondary");
        label.setColor(TEXT_COLOR);
        return label;
    }
}
