package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pvz.Main;
import controllers.features.NewsController;
import models.account.News;
import ui.BackButton;
import ui.MenuButton;
import ui.ModalWindow;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NewsScreen extends BaseMenuScreen {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final NewsController controller;
    private Table newsTable;

    public NewsScreen(Main game) {
        super(game);
        controller = game.getNewsController();
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshNews();
        refreshResourceBar();
    }

    private void buildUi() {
        Table root = createRoot();
        addResourceBar(root);
        Table panel = createPanel();
        panel.add(createTitle("News")).colspan(2).padBottom(14f).row();
        newsTable = new Table();
        ScrollPane scrollPane = new ScrollPane(newsTable, skin);
        scrollPane.setFadeScrollBars(false);
        panel.add(scrollPane).colspan(2).width(860f).height(370f).row();
        panel.add(new MenuButton("Mark All Read", skin, this::markAllRead)).width(190f).height(44f).padTop(10f);
        panel.add(new BackButton(skin, game.getScreenManager()::showMainMenu)).width(190f).height(44f).padTop(10f);
        root.add(panel).expand().center();
    }

    private void refreshNews() {
        newsTable.clearChildren();
        List<News> newsItems = controller.getAllNews();
        if (newsItems.isEmpty()) {
            newsTable.add(createLabel("No news available.")).pad(20f);
            return;
        }
        addHeader();
        for (News news : newsItems) {
            addNewsRow(news);
        }
    }

    private void addHeader() {
        newsTable.add(createNewsLabel("Status")).width(90f).left();
        newsTable.add(createNewsLabel("Title")).width(360f).left();
        newsTable.add(createNewsLabel("Date")).width(210f).left();
        newsTable.add(createNewsLabel("Action")).width(150f).row();
    }

    private void addNewsRow(News news) {
        String status = news.isUnread() ? "NEW" : "READ";
        String date = news.getCreatedAt() == null ? "-" : DATE_FORMAT.format(news.getCreatedAt());
        newsTable.add(createNewsLabel(status)).left().padTop(8f);
        newsTable.add(createNewsLabel(news.getTitle())).left().padTop(8f);
        newsTable.add(createNewsLabel(date)).left().padTop(8f);
        newsTable.add(new MenuButton("Open", skin, () -> openNews(news)))
                .width(120f).height(42f).padTop(8f).row();
    }

    private void openNews(News news) {
        controller.markAsRead(news);
        showNewsWindow(news);
        refreshNews();
    }

    private void showNewsWindow(News news) {
        ModalWindow window = new ModalWindow(news.getTitle(), skin);
        Label dateLabel = createLabel(news.getCreatedAt() == null ? "" : DATE_FORMAT.format(news.getCreatedAt()));
        Label contentLabel = createLabel(news.getContent());
        contentLabel.setWrap(true);
        Table content = window.getContentTable();
        content.add(dateLabel).left().padBottom(10f).row();
        content.add(contentLabel).width(620f).padBottom(16f).row();
        content.add(new MenuButton("Close", skin, "brown", window::close)).width(180f).height(46f);
        window.show(stage);
    }

    private Label createNewsLabel(String text) {
        Label label = createLabel(text);
        label.setColor(Color.BLACK);
        return label;
    }

    private void markAllRead() {
        controller.markAllAsRead();
        showControllerMessage(controller.getLastMessage());
        refreshNews();
    }
}
