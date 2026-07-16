package controllers.core;

import models.account.User;

import java.util.ArrayList;
import java.util.List;

public class SaveManager {
    private static final ArrayList<User> SAVED_USERS = new ArrayList<>();

    public void saveAllUsers(List<User> users) {
        SAVED_USERS.clear();

        if (users == null) {
            return;
        }

        SAVED_USERS.addAll(users);
    }

    public List<User> loadAllUsers() {
        return new ArrayList<>(SAVED_USERS);
    }
}