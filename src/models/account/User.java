package models.account;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class User extends UserProgress {
    public User(String username, String password, String nickname, String email, String gender) {
        super(username, password, nickname, email, gender);
    }

    public List<News> getNewsList() {
        if (newsList == null) {
            newsList = new ArrayList<>();
        }

        return new ArrayList<>(newsList);
    }

    public void setNewsList(List<News> newsList) {
        if (newsList == null) {
            this.newsList = new ArrayList<>();
            return;
        }

        this.newsList = newsList;
    }

    public void addNews(News news) {
        if (news != null) {
            if (newsList == null) {
                newsList = new ArrayList<>();
            }

            newsList.add(news);
        }
    }

}
