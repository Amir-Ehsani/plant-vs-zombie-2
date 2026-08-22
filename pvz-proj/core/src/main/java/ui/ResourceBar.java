package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import models.account.User;

public class ResourceBar extends Table {
    private final Skin skin;
    private final CurrencyActor coinsActor;
    private final CurrencyActor diamondsActor;

    public ResourceBar(Skin skin) {
        this.skin = skin;
        setBackground(skin.getDrawable("image_ui_mainmenu_name_field_10"));
        pad(12f, 18f, 12f, 18f);
        coinsActor = new CurrencyActor(skin, "Coins");
        diamondsActor = new CurrencyActor(skin, "Diamonds");
        rebuild(false, null, null, null, null);
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

    public void setDebugControls(boolean visible, Runnable addCoin, Runnable addDiamond) {
        rebuild(visible, addCoin, addDiamond, null, null);
    }

    public void setGameDebugControls(
            boolean visible,
            Runnable addCoin,
            Runnable addDiamond,
            Runnable addSun,
            Runnable addPlantFood
    ) {
        rebuild(visible, addCoin, addDiamond, addSun, addPlantFood);
    }

    private void rebuild(
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
