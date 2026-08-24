package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import models.account.User;

public class ResourceBar extends Table {
    private final Skin skin;
    private final CurrencyActor coinsActor;
    private final CurrencyActor diamondsActor;
    private final CurrencyActor sunActor;
    private final CurrencyActor plantFoodActor;

    public ResourceBar(Skin skin) {
        this.skin = skin;
        setBackground(skin.getDrawable("image_ui_mainmenu_name_field_10"));
        pad(12f, 18f, 12f, 18f);
        coinsActor = new CurrencyActor(skin, "Coins");
        diamondsActor = new CurrencyActor(skin, "Diamonds");
        sunActor = new CurrencyActor(skin, "Sun");
        plantFoodActor = new CurrencyActor(skin, "Plant Food");
        rebuild(false, false, null, null, null, null);
    }

    public void refresh(User user) {
        if (user == null) {
            coinsActor.setValue(0);
            diamondsActor.setValue(0);
            return;
        }
        coinsActor.setValue(user.getCoins());
        diamondsActor.setValue(user.getGems());
    }

    public void refreshGame(User user, int sun, int plantFood) {
        refresh(user);
        sunActor.setValue(sun);
        plantFoodActor.setValue(plantFood);
    }

    public void setDebugControls(boolean visible, Runnable addCoin, Runnable addDiamond) {
        rebuild(false, visible, addCoin, addDiamond, null, null);
    }

    public void setGameDebugControls(
            boolean visible,
            Runnable addCoin,
            Runnable addDiamond,
            Runnable addSun,
            Runnable addPlantFood
    ) {
        rebuild(true, visible, addCoin, addDiamond, addSun, addPlantFood);
    }

    private void rebuild(
            boolean gameResourcesVisible,
            boolean debugVisible,
            Runnable addCoin,
            Runnable addDiamond,
            Runnable addSun,
            Runnable addPlantFood
    ) {
        clearChildren();
        Table content = new Table();
        content.add(coinsActor).padRight(14f);
        content.add(diamondsActor);
        if (gameResourcesVisible) {
            content.add(sunActor).padLeft(14f);
            content.add(plantFoodActor).padLeft(14f);
        }
        if (debugVisible) {
            addDebugButton(content, "+Coin", 92f, addCoin);
            addDebugButton(content, "+Diamond", 110f, addDiamond);
            addOptionalGameDebugButton(content, "+Sun", 84f, addSun);
            addOptionalGameDebugButton(content, "+Plant Food", 124f, addPlantFood);
        }
        add(content);
    }

    private void addDebugButton(Table content, String text, float width, Runnable action) {
        content.add(new MenuButton(text, skin, "green_small", action))
                .width(width).height(34f).padLeft(6f);
    }

    private void addOptionalGameDebugButton(Table content, String text, float width, Runnable action) {
        if (action != null) {
            addDebugButton(content, text, width, action);
        }
    }
}
