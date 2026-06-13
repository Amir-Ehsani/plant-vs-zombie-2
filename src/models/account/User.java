package models.account;

import java.util.List;

public class User {
    private String username;
    private String passwordHash;
    private String nickname;
    private String email;
    private int coins;
    private int gems;
    private int score;

    private Collection collection;

    private Greenhouse greenhouse;

    private List<Quest> quests;



    public String getUsername() {
        return this.username;
    }

    public void addCoins(int coins) {
        this.coins += coins;
    }

}
