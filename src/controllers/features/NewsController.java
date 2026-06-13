package controllers.features;

import models.account.News;

import java.util.List;

public class NewsController {
    private List<News> newsList;

    public void addNewNews(News news) {
        this.newsList.add(news);
    }

    public void applyNewsEffects(News news) {}
}
