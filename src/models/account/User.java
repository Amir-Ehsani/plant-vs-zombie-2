package models.account;

import java.util.List;

public class User {
    private String username;
    private String password;
    private String passwordHash;
    private String nickname;
    private String email;
    private String gender;
    private int securityQuestionNumber;
    private String securityAnswer;

    public void setSecurityQuestionNumber(int securityQuestionNumber) {
        this.securityQuestionNumber = securityQuestionNumber;
    }

    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    private int coins;
    private int gems;
    private int score;

    public User(String username, String password, String nickname, String email, String gender) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
    }

    public String getPassword() {
        return password;
    }

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
