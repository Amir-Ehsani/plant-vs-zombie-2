package ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import models.account.User;

public class ResourceBar extends Table {
    private final Skin skin;
    private final CurrencyActor coinsActor;
    private final CurrencyActor diamondsActor;
    private final CurrencyActor sunActor;
    private final CurrencyActor plantFoodActor;

    public ResourceBar(Skin skin, PvzAnimationService animations) {
        this.skin = skin;
        pad(0f);
        TextureRegion barBackground = animations == null
                ? null
                : animations.region("IMAGE_UI_GENERIC_PURPLEBUTTON_DOWN");
        TextureRegion coinIcon = animations == null
                ? null
                : animations.region("IMAGE_UI_THYMED_EVENTS_ECS_CONVRT_COIN");
        TextureRegion diamondIcon = animations == null
                ? null
                : animations.region("IMAGE_EFFECTS_COIN_DIAMOND_COIN_DIAMOND_141X146");
        coinsActor = new CurrencyActor(skin, "Coins", barBackground, coinIcon);
        diamondsActor = new CurrencyActor(skin, "Diamonds", barBackground, diamondIcon);
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
        content.add(coinsActor).width(112f).height(46f).padRight(8f);
        content.add(diamondsActor).width(112f).height(46f);
        if (gameResourcesVisible) {
            content.add(sunActor).padLeft(10f).padRight(8f);
            content.add(plantFoodActor).padLeft(4f).padRight(8f);
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
