package controllers.core;

import controllers.auth.AuthController;
import controllers.features.*;
import views.core.BaseView;
import views.menus.*;


public class MenuManager extends MenuManagerBase {
    public MenuManager() {
        super();
    }

    public void enterNamedMenu(String menuName) {
        if (menuName == null || menuName.isBlank()) {
            fail("Menu name is required.");
            return;
        }
        String targetMenu = menuName.trim().toLowerCase();
        String currentMenu = currentView == null ? "" : currentView.getViewName();
        switch (currentMenu) {
            case "Register Menu" -> enterFromRegister(targetMenu);
            case "Login Menu" -> enterFromLogin(targetMenu);
            case "Main Menu" -> enterFromMain(targetMenu);
            case "Game Menu" -> enterFromGame(targetMenu);
            default -> fail("You can't enter " + targetMenu + " menu from " + currentMenu + ".");
        }
    }

    private void enterFromRegister(String targetMenu) {
        if ("login".equals(targetMenu)) enterLoginMenu();
        else fail("You can't enter " + targetMenu + " menu from register menu.");
    }

    private void enterFromLogin(String targetMenu) {
        if ("register".equals(targetMenu)) enterRegisterMenu();
        else if ("main".equals(targetMenu) && authController.isLoggedIn()) enterMainMenu();
        else fail("You can't enter " + targetMenu + " menu from login menu.");
    }

    private void enterFromMain(String targetMenu) {
        switch (targetMenu) {
            case "game" -> enterGameMenu();
            case "settings" -> enterSettingsMenu();
            case "profile" -> enterProfileMenu();
            case "news" -> enterNewsMenu();
            case "network" -> fail("Network menu is not implemented yet.");
            default -> fail("You can't enter " + targetMenu + " menu from main menu.");
        }
    }

    private void enterFromGame(String targetMenu) {
        if ("collection".equals(targetMenu)) enterCollectionMenu();
        else fail("You can't enter " + targetMenu + " menu from game menu.");
    }


    public void exitCurrentMenu() {
        if (currentView == null) {
            success("Program closed.");
            return;
        }
        String viewName = currentView.getViewName();
        switch (viewName) {
            case "Register Menu" -> closeProgram();
            case "Login Menu" -> enterRegisterMenu();
            case "Main Menu" -> fail("Use menu logout to leave the main menu.");
            case "Game Play Menu" -> enterChapterLevelMenu();
            case "Chapter Level Menu", "Collection Menu", "Greenhouse Menu",
                    "Leaderboard Menu", "Travel Log Menu" -> enterGameMenu();
            case "Shop Menu" -> enterGreenhouseMenu();
            case "Mini-game Menu" -> exitMiniGame();
            case "News Menu", "Profile Menu", "Settings Menu" -> enterMainMenu();
            default -> enterMainMenu();
        }
    }

    private void exitMiniGame() {
        travelLogController.abandonMiniGame();
        enterTravelLogMenu();
    }


    public void closeProgram() {
        authController.saveUsers();
        changeView(null);
        success("Program closed.");
    }

    public void showRegisterMenuText() {
        success("""
                Register Menu
                register -u <username> -p <password> <password_confirm> -n <nickname> -e <email> -g <gender>
                pick question -q <question_number> -a <answer> -c <answer_confirm>
                menu enter login
                menu show current
                menu exit""");
    }

    public void showLoginMenuText() {
        success("""
                Login Menu
                login -u <username> -p <password> -stay-logged-in
                forget password -u <username> -e <email>
                answer -a <answer>
                menu enter register
                menu show current
                menu exit""");
    }

    public void showMainMenuText() {
        success("""
            Main Menu
            menu enter game
            menu enter settings
            menu enter profile
            menu enter news
            menu enter network
            menu logout
            menu exit program
            menu show current
            menu exit""");
    }

    public void showProfileMenuText() {
        success("""
                Profile Menu
                menu profile show-info
                menu profile change-username -u <username>
                menu profile change-nickname -u <nickname>
                menu profile change-email -e <email>
                menu profile change-password -p <new_password> -o <old_password>
                menu show current
                menu exit""");
    }

    public void showSettingsMenuText() {
        success("""
                Settings Menu
                menu settings change-difficulty -l <difficulty_level>
                menu show current
                menu exit""");
    }

    public void showGameMenuText() {
        success("""
                Game Menu
                menu enter collection
                menu enter chapter -c <chaptername>
                menu coin-wallet
                menu gem-wallet
                menu greenhouse
                menu travel-log
                menu leaderboard
                menu cheat add <n> <coin/diamond>
                menu cheat unlock-all-levels
                menu show current
                menu exit""");
    }

    public void showGamePlayMenuText() {
        success("""
                Game View

                Plant Selection Phase
                show all plants
                show available plants
                add plant -t <type>
                remove plant -t <type>
                boost plant -t <type>
                start game

                Game Play Phase
                show map
                show sun
                show plants
                zombies info
                show tile -l <x, y>
                plant -t <plant_type> -l <x, y>
                pluck -l <x, y>
                feed plant -l <x, y>
                collect sun -l <x, y>
                advance time -t <ticks>
                update game
                pause game
                start zombie waves
                cheat add sun -a <amount>
                cheat remove cooldown
                cheat add plant-food
                cheat spawn-zombie -t <zombie_type> -l <x, y>
                cheat nuke
                return to level menu

                menu show current
                menu exit""");
    }

    public void showTravelLogMenuText() {
        success("""
                Travel Log Menu
                travel log page all
                travel log page adventure
                travel log page special
                travel log page minigames
                travel log page community
                travel log page challenges
                travel log page mystery
                travel log collect -q <quest_number>
                travel log collect all
                show minigames
                enter minigame -n <mini_game_name> [-s <stage>]
                menu show current
                menu exit""");
    }

    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
    }




}
